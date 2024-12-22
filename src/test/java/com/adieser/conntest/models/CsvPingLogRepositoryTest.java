package com.adieser.conntest.models;

import com.adieser.conntest.service.writer.FileWriterService;
import com.opencsv.bean.CsvToBean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static com.adieser.utils.TestUtils.CLOUD_IP_ADDRESS;
import static com.adieser.utils.TestUtils.LOCAL_IP_ADDRESS;
import static com.adieser.utils.TestUtils.getDefaultPingLog;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvPingLogRepositoryTest {
    @Mock
    Logger logger;

    @Mock
    FileWriter fileWriterMock;

    @Mock
    FileWriterService  fileWriterServiceMock;

    @InjectMocks
    @Spy
    CsvPingLogRepository underTestSpy;

    @ParameterizedTest
    @MethodSource("successPingLogProvider")
    void testSavePingLogSuccess(PingLog pingLog) {
        // then
        underTestSpy.savePingLog(pingLog);

        // assert
        verify(fileWriterServiceMock, times(1)).submit(pingLog);
    }

    /**
     * Success case
     */
    @SuppressWarnings("unchecked")
    @Test
    void testReadAllSuccess() throws IOException {
        // when
        BufferedReader mockReader = mock(BufferedReader.class);
        doReturn(mockReader).when(underTestSpy).getReader();
        CsvToBean<PingLog> cb = mock(CsvToBean.class);
        List<PingLog> pingLogs = List.of(getDefaultPingLog());
        when(cb.parse()).thenReturn(pingLogs);
        when(underTestSpy.getCsvToBean(mockReader)).thenReturn(cb);

        // then
        List<PingLog> pingLogsResult = underTestSpy.readAll();

        // assert
        assertEquals(1, pingLogsResult.size());
        assertEquals(pingLogs.get(0), pingLogsResult.get(0));
    }

    /**
     * Test IOException case
     */
    @Test
    void testReadAllIoException() throws IOException {
        // when
        doThrow(IOException.class).when(underTestSpy).getReader();

        // then assert
        assertThrows(IOException.class, underTestSpy::readAll);
    }

    @ParameterizedTest
    @MethodSource("getPingLogsByIpStreamProvider")
    void testGetPingLogsByIpStreamSuccess(String pingIpAddress, String searchedIpAddress, boolean pingFound){
        // when
        PingLog pingLog = getDefaultPingLog();
        pingLog.setIpAddress(pingIpAddress);
        Stream<PingLog> stream = Stream.of(pingLog);

        // then
        Stream<PingLog> pingLogsByIpStream = underTestSpy.getPingLogsByIpStream(stream, searchedIpAddress);

        // assert
        if(pingFound)
            assertEquals(pingLog, pingLogsByIpStream.toList().get(0));
        else
            assertTrue(pingLogsByIpStream.toList().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getPingLogsByDateTimeRangeByIpStreamProvider")
    void testGetPingLogsByDateTimeRangeByIpStream(LocalDateTime pingDate,
                                            LocalDateTime start,
                                            LocalDateTime end,
                                            String ipAddress,
                                            boolean pingFound){
        // when
        CsvPingLogRepository underTest = new CsvPingLogRepository("test", logger, fileWriterServiceMock);
        PingLog pingLog = getDefaultPingLog();
        pingLog.setDateTime(pingDate);
        Stream<PingLog> stream = Stream.of(pingLog);

        // then
        Stream<PingLog> pingLogsByIpStream = underTest.getPingLogsByDateTimeRangeByIpStream(stream,
                start,
                end,
                ipAddress);

        // assert
        if(pingFound)
            assertEquals(pingLog, pingLogsByIpStream.toList().get(0));
        else
            assertTrue(pingLogsByIpStream.toList().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getLostPingLogsStreamProvider")
    void testGetLostPingLogsStream(long pingTime, boolean pingFound){
        // when
        CsvPingLogRepository underTest = new CsvPingLogRepository("test", logger, fileWriterServiceMock);
        PingLog pingLog = getDefaultPingLog();
        pingLog.setPingTime(pingTime);
        Stream<PingLog> stream = Stream.of(pingLog);

        // then
        Stream<PingLog> pingLogsByIpStream = underTest.getLostPingLogsStream(stream);

        // assert
        if(pingFound)
            assertEquals(pingLog, pingLogsByIpStream.toList().get(0));
        else
            assertTrue(pingLogsByIpStream.toList().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getPingLogsByDateTimeRangeStreamProvider")
    void testGetPingLogsByDateTimeRangeStream(LocalDateTime pingDate,
                                               LocalDateTime start,
                                               LocalDateTime end,
                                               boolean pingFound){
        // when
        CsvPingLogRepository underTest = new CsvPingLogRepository("test", logger, fileWriterServiceMock);
        PingLog pingLog = getDefaultPingLog();
        pingLog.setDateTime(pingDate);
        Stream<PingLog> stream = Stream.of(pingLog);

        // then
        Stream<PingLog> pingLogsByIpStream = underTest.getPingLogsByDateTimeRangeStream(stream, start, end);

        // assert
        if(pingFound)
            assertEquals(pingLog, pingLogsByIpStream.toList().get(0));
        else
            assertTrue(pingLogsByIpStream.toList().isEmpty());
    }

    @Test
    void clearPingLogFile_Success() throws Exception {
        // when
        doReturn(fileWriterMock).when(underTestSpy).getFileWriter(false);

        //then
        assertDoesNotThrow(underTestSpy::clearPingLogFile);

        // assert
        verify(fileWriterMock).write("");
        verify(fileWriterMock).flush();
        verify(fileWriterMock).close();
    }

    @Test
    void clearPingLogFile_IOExceptionThrown() throws Exception {
        // when
        doReturn(fileWriterMock).when(underTestSpy).getFileWriter(false);
        doThrow(new IOException("Test exception")).when(fileWriterMock).write("");

        InterruptedException exception = assertThrows(InterruptedException.class,
                underTestSpy::clearPingLogFile);

        assertEquals(CsvPingLogRepository.CLEAN_PINGLOG_FILE_MSG, exception.getMessage());
        verify(logger).error(eq(CsvPingLogRepository.CLEAN_PINGLOG_FILE_MSG), any(IOException.class));
    }

    @ParameterizedTest
    @MethodSource("findAvgLatencyByIpPingLogProvider")
    void testFindAvgLatencyByIp(List<PingLog> pingLogs, BigDecimal expectedAvg) throws IOException {

        // When
        doReturn(mock(List.class))
                .when(underTestSpy)
                .readAll();

        doReturn(pingLogs.stream())
                .when(underTestSpy)
                .getPingLogsByIpStream(any(), eq(LOCAL_IP_ADDRESS));

        // Then
        BigDecimal result = underTestSpy.findAvgLatencyByIp(LOCAL_IP_ADDRESS);

        // Assert
        assertEquals(expectedAvg, result);
    }

    static Stream<Arguments> findAvgLatencyByIpPingLogProvider() {

        return Stream.of(
                Arguments.of(
                        Arrays.asList(
                                PingLog.builder().ipAddress(LOCAL_IP_ADDRESS).pingTime(10L).build(),
                                PingLog.builder().ipAddress(LOCAL_IP_ADDRESS).pingTime(20L).build(),
                                PingLog.builder().ipAddress(LOCAL_IP_ADDRESS).pingTime(30L).build()
                        ),
                        BigDecimal.valueOf(20.00).setScale(2, RoundingMode.HALF_UP)
                ),
                Arguments.of(
                        List.of(),
                        BigDecimal.valueOf(0).setScale(2, RoundingMode.HALF_UP)
                ),
                Arguments.of(
                        Collections.singletonList(
                                PingLog.builder().ipAddress(LOCAL_IP_ADDRESS).pingTime(50L).build()
                        ),
                        BigDecimal.valueOf(50.00).setScale(2, RoundingMode.HALF_UP)
                )
        );
    }

    static Stream<PingLog> successPingLogProvider() {
        return Stream.of(
                PingLog.builder().build(),
                null
        );
    }

    /**
     * LocalDateTime - date of the ping in the list
     * LocalDateTime - start date of the range
     * LocalDateTime - end date of the range
     * String - IP address to search by
     * boolean - is ping found
     */
    static Stream<Arguments> getPingLogsByDateTimeRangeByIpStreamProvider() {
        return Stream.of(
                // SUCCESS
                Arguments.of(
                        LocalDateTime.of(2023, 10, 6, 0, 0, 30),
                        LocalDateTime.of(2023, 10, 6, 0, 0, 0),
                        LocalDateTime.of(2023, 10, 6, 0, 0, 50),
                        LOCAL_IP_ADDRESS,
                        true),
                // EMPTY DATE RANGE
                Arguments.of(
                        LocalDateTime.of(2023, 10, 6, 0, 0, 30),
                        LocalDateTime.of(2023, 10, 6, 0, 0, 40),
                        LocalDateTime.of(2023, 10, 6, 0, 0, 50),
                        LOCAL_IP_ADDRESS,
                        false),
                // DIFFERENT IP IN DATE RANGE
                Arguments.of(
                        LocalDateTime.of(2023, 10, 6, 0, 0, 30),
                        LocalDateTime.of(2023, 10, 6, 0, 0, 0),
                        LocalDateTime.of(2023, 10, 6, 0, 0, 50),
                        CLOUD_IP_ADDRESS,
                        false)
        );
    }

    /**
     * String - IP address of the ping in the list
     * String - IP address to search by
     * boolean - is ping found
     */
    static Stream<Arguments> getPingLogsByIpStreamProvider() {
        return Stream.of(
                // SUCCESS
                Arguments.of(
                        LOCAL_IP_ADDRESS,
                        LOCAL_IP_ADDRESS,
                        true),
                // NO PING FOR THE SEARCHED IP
                Arguments.of(
                        LOCAL_IP_ADDRESS,
                        CLOUD_IP_ADDRESS,
                        false)
        );
    }

    /**
     * String - IP address to search by
     * boolean - is ping found
     */
    static Stream<Arguments> getLostPingLogsStreamProvider() {
        return Stream.of(
                // SUCCESS
                Arguments.of(
                        -1,
                        true),
                // NO LOST PING
                Arguments.of(
                        13,
                        false)
        );
    }

    /**
     * LocalDateTime - date of the ping in the list
     * LocalDateTime - start date of the range
     * LocalDateTime - end date of the range
     * boolean - is ping found
     */
    static Stream<Arguments> getPingLogsByDateTimeRangeStreamProvider() {
        return Stream.of(
                // SUCCESS
                Arguments.of(
                        LocalDateTime.of(2023, 10, 6, 0, 0, 30),
                        LocalDateTime.of(2023, 10, 6, 0, 0, 0),
                        LocalDateTime.of(2023, 10, 6, 0, 0, 50),
                        true),
                Arguments.of(
                        LocalDateTime.of(2023, 10, 6, 0, 0, 0),
                        LocalDateTime.of(2023, 10, 6, 0, 0, 0),
                        LocalDateTime.of(2023, 10, 6, 0, 0, 50),
                        true),
                Arguments.of(
                        LocalDateTime.of(2023, 10, 6, 0, 0, 50),
                        LocalDateTime.of(2023, 10, 6, 0, 0, 0),
                        LocalDateTime.of(2023, 10, 6, 0, 0, 50),
                        true),
                // EMPTY DATE RANGE
                Arguments.of(
                        LocalDateTime.of(2023, 10, 6, 0, 0, 30),
                        LocalDateTime.of(2023, 10, 6, 0, 0, 40),
                        LocalDateTime.of(2023, 10, 6, 0, 0, 50),
                        false)
        );
    }


}
