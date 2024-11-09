package com.adieser.conntest.models;

import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvPingLogRepositoryTest {
    @Mock
    Logger logger;

    @Mock
    StatefulBeanToCsv<PingLog> sbcMock;

    @Mock
    FileWriter fileWriterMock;

    @Mock
    CSVWriter cvsFileWriterMock;

    @ParameterizedTest
    @MethodSource("successPingLogProvider")
    void testSavePingLogSuccess(PingLog pingLog)
            throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, IOException, InterruptedException {
        // when
        CsvPingLogRepository underTestSpy = spy(new CsvPingLogRepository("test", logger));

        doReturn(fileWriterMock).when(underTestSpy).getFileWriter(anyBoolean());
        when(underTestSpy.getCsvWriter(fileWriterMock)).thenReturn(cvsFileWriterMock);
        when(underTestSpy.getStatefulBeanToCsv(cvsFileWriterMock)).thenReturn(sbcMock);

        // then
        underTestSpy.savePingLog(pingLog);

        // assert
        verify(sbcMock, times(1)).write(pingLog);
    }

    @Test
    void testSavePingLogInvalidPath() {
        // when
        PingLog mockPingLog = mock(PingLog.class);
        CsvPingLogRepository underTestSpy = spy(new CsvPingLogRepository("FileNotFound", logger));

        // then assert
        assertThrows(InterruptedException.class, () -> underTestSpy.savePingLog(mockPingLog));
        verify(logger, times(1)).error(eq(CsvPingLogRepository.SAVE_PING_ERROR_MSG), any(FileNotFoundException.class));
    }

    @Test
    void testSavePingLogIOException() throws IOException {
        // when
        PingLog mockPingLog = mock(PingLog.class);
        CsvPingLogRepository underTestSpy = spy(new CsvPingLogRepository("test", logger));

        doThrow(new IOException()).when(underTestSpy).getFileWriter(anyBoolean());

        // then assert
        assertThrows(InterruptedException.class, () -> underTestSpy.savePingLog(mockPingLog));
        verify(logger, times(1)).error(eq(CsvPingLogRepository.SAVE_PING_ERROR_MSG), any(IOException.class));
    }

    @ParameterizedTest
    @MethodSource("statefulBeanToCsvExceptionsProvider")
    void testSavePingLogStatefulBeanToCsvExceptions(Class<? extends Throwable> clazz) throws IOException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        // when
        PingLog mockPingLog = mock(PingLog.class);
        CsvPingLogRepository underTestSpy = spy(new CsvPingLogRepository("test", logger));
        doReturn(fileWriterMock).when(underTestSpy).getFileWriter(anyBoolean());
        when(underTestSpy.getCsvWriter(fileWriterMock)).thenReturn(cvsFileWriterMock);
        doThrow(clazz).when(sbcMock).write(mockPingLog);
        when(underTestSpy.getStatefulBeanToCsv(cvsFileWriterMock)).thenReturn(sbcMock);

        // assert
        assertThrows(InterruptedException.class, () -> underTestSpy.savePingLog(mockPingLog));
        verify(logger, times(1)).error(eq(CsvPingLogRepository.SAVE_PING_ERROR_MSG), any(clazz));
    }

    /**
     * Success case
     */
    @SuppressWarnings("unchecked")
    @Test
    void testReadAllSuccess() throws IOException {
        // when
        CsvPingLogRepository underTestSpy = spy(new CsvPingLogRepository("test", logger));
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
        CsvPingLogRepository underTestSpy = spy(new CsvPingLogRepository("test", logger));
        doThrow(IOException.class).when(underTestSpy).getReader();

        // then assert
        assertThrows(IOException.class, underTestSpy::readAll);
    }

    @ParameterizedTest
    @MethodSource("getPingLogsByIpStreamProvider")
    void testGetPingLogsByIpStreamSuccess(String pingIpAddress, String searchedIpAddress, boolean pingFound){
        // when
        CsvPingLogRepository underTest = new CsvPingLogRepository("test", logger);
        PingLog pingLog = getDefaultPingLog();
        pingLog.setIpAddress(pingIpAddress);
        Stream<PingLog> stream = Stream.of(pingLog);

        // then
        Stream<PingLog> pingLogsByIpStream = underTest.getPingLogsByIpStream(stream, searchedIpAddress);

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
        CsvPingLogRepository underTest = new CsvPingLogRepository("test", logger);
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
        CsvPingLogRepository underTest = new CsvPingLogRepository("test", logger);
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
        CsvPingLogRepository underTest = new CsvPingLogRepository("test", logger);
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
        CsvPingLogRepository underTestSpy = spy(new CsvPingLogRepository("test", logger));
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
        CsvPingLogRepository underTestSpy = spy(new CsvPingLogRepository("test", logger));
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
        CsvPingLogRepository underTestSpy = spy(new CsvPingLogRepository("test", logger));

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

    static Stream<Class<? extends Throwable>> statefulBeanToCsvExceptionsProvider() {
        return Stream.of(
                CsvRequiredFieldEmptyException.class,
                CsvDataTypeMismatchException.class
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
