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
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static com.adieser.utils.PingLogUtils.CLOUD_IP_ADDRESS;
import static com.adieser.utils.PingLogUtils.LOCAL_IP_ADDRESS;
import static com.adieser.utils.PingLogUtils.getDefaultPingLog;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

    /**
     * Success ping save:
     * Propósito: Verificar si el método guarda correctamente un objeto PingLog.
     * Método: Proporciona un PingLog válido y asegúrate de que no se lancen excepciones.
     */
    @ParameterizedTest
    @MethodSource("successPingLogProvider")
    void testSavePingLogSuccess(PingLog pingLog) throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, IOException {
        // when
        CsvPingLogRepository underTestSpy = spy(new CsvPingLogRepository("test", logger));

        doReturn(fileWriterMock).when(underTestSpy).getFileWriter();
        when(underTestSpy.getCsvWriter(fileWriterMock)).thenReturn(cvsFileWriterMock);
        when(underTestSpy.getStatefulBeanToCsv(cvsFileWriterMock)).thenReturn(sbcMock);

        // then
        underTestSpy.savePingLog(pingLog);

        // assert
        verify(sbcMock, times(1)).write(pingLog);
    }

    /**
     * Prueba de Guardado con Ruta No Válida:
     * Propósito: Verificar si el método maneja adecuadamente una ruta de archivo no válida.
     * Método: Proporciona un PingLog válido y una ruta de archivo incorrecta, y asegúrate de que se maneje la excepción correctamente.
     */
    @Test
    void testSavePingLogInvalidPath() {
        // when
        PingLog mockPingLog = mock(PingLog.class);
        CsvPingLogRepository underTestSpy = spy(new CsvPingLogRepository("FileNotFound", logger));

        // then
        underTestSpy.savePingLog(mockPingLog);

        // assert
        verify(logger, times(1)).error(eq(CsvPingLogRepository.SAVE_PING_ERROR_MSG), any(FileNotFoundException.class));
    }

    /**
     * Prueba de Manejo de Excepción (IOException):
     * Propósito: Verificar si el método maneja adecuadamente una excepción de tipo IOException.
     * Método: Configura un escenario donde se provoque una excepción de tipo IOException al intentar escribir en el archivo.
     */
    @Test
    void testSavePingLogIOException() throws IOException {
        // when
        PingLog mockPingLog = mock(PingLog.class);
        CsvPingLogRepository underTestSpy = spy(new CsvPingLogRepository("test", logger));

        doThrow(new IOException()).when(underTestSpy).getFileWriter();

        // then
        underTestSpy.savePingLog(mockPingLog);

        // assert
        verify(logger, times(1)).error(eq(CsvPingLogRepository.SAVE_PING_ERROR_MSG), any(IOException.class));
    }

    /**
     * Prueba de Manejo de Excepción (CsvRequiredFieldEmptyException):
     * Propósito: Verificar si el método maneja adecuadamente una excepción de tipo CsvRequiredFieldEmptyException.
     * Método: Configura un escenario donde se provoque una excepción de tipo CsvRequiredFieldEmptyException al intentar escribir en el archivo.
     */
    @ParameterizedTest
    @MethodSource("statefulBeanToCsvExceptionsProvider")
    void testSavePingLogStatefulBeanToCsvExceptions(Class<? extends Throwable> clazz) throws IOException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        // when
        PingLog mockPingLog = mock(PingLog.class);
        CsvPingLogRepository underTestSpy = spy(new CsvPingLogRepository("test", logger));
        doReturn(fileWriterMock).when(underTestSpy).getFileWriter();
        when(underTestSpy.getCsvWriter(fileWriterMock)).thenReturn(cvsFileWriterMock);
        doThrow(clazz).when(sbcMock).write(mockPingLog);
        when(underTestSpy.getStatefulBeanToCsv(cvsFileWriterMock)).thenReturn(sbcMock);

        // then
        underTestSpy.savePingLog(mockPingLog);

        // assert
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

        // then
        List<PingLog> pingLogsResult = underTestSpy.readAll();

        // assert
        assertTrue(pingLogsResult.isEmpty());
        verify(logger, times(1)).error(eq(CsvPingLogRepository.PARSE_ERROR), any(IOException.class));
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
    void testGetPingLogsByDateTimeRangeByIp(LocalDateTime pingDate,
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
    void testGetLostPingLogsStreamSuccess(long pingTime, boolean pingFound){
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
