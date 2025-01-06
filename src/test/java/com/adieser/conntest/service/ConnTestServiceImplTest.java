package com.adieser.conntest.service;

import com.adieser.conntest.controllers.responses.PingSessionExtract;
import com.adieser.conntest.models.ConnTest;
import com.adieser.conntest.models.PingLog;
import com.adieser.conntest.models.PingLogRepository;
import com.adieser.conntest.models.utils.Reachable;
import com.adieser.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static com.adieser.utils.TestUtils.CLOUD_IP_ADDRESS;
import static com.adieser.utils.TestUtils.DEFAULT_LOG_DATE_TIME;
import static com.adieser.utils.TestUtils.DEFAULT_PING_TIME;
import static com.adieser.utils.TestUtils.ISP_IP_ADDRESS;
import static com.adieser.utils.TestUtils.LOCAL_IP_ADDRESS;
import static com.adieser.utils.TestUtils.getDefaultPingLog;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnTestServiceImplTest {
    private static final LocalDateTime START = DEFAULT_LOG_DATE_TIME.minusMinutes(1);
    private static final LocalDateTime END = DEFAULT_LOG_DATE_TIME.plusMinutes(1);

    @Mock
    Logger logger;
    @Mock
    ExecutorService executorService;
    @Mock
    PingLogRepository pingLogRepository;
    @Mock
    TracertProvider tracertProvider;
    @Mock
    Reachable pingUtils;

    @Test
    void testTestLocalISPInternet() {
        // when
        ConnTestServiceImpl underTest =
                spy(new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils));
        List<String> localAndISPIpAddresses = new ArrayList<>(Arrays.asList(LOCAL_IP_ADDRESS, ISP_IP_ADDRESS));
        List<ConnTest> mockConnTests = List.of(mock(ConnTest.class),mock(ConnTest.class),mock(ConnTest.class));

        when(underTest.getConnTestsFromIpAddresses(localAndISPIpAddresses)).thenReturn(mockConnTests);
        doReturn(localAndISPIpAddresses).when(underTest).getLocalAndISPIpAddresses();

        // then
        underTest.testLocalISPInternet();

        // assert
        localAndISPIpAddresses.add(CLOUD_IP_ADDRESS);
        underTest.tests.forEach(mockConnTest ->
                verify(underTest).testCustomIps(localAndISPIpAddresses)
        );
    }

    @ParameterizedTest
    @MethodSource("areTestsRunningProvider")
    void testTestCustomIps(boolean areTestRunning) {
        // when
        String ip1 = "1.1.1.1";
        String ip2 = "2.2.2.2";
        String ip3 = "3.3.3.3";

        ConnTestServiceImpl underTest =
                spy(new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils));
        List<String> ipAddresses = new ArrayList<>(Arrays.asList(ip1, ip2, ip3));

        ConnTest localConnMock = mock(ConnTest.class);
        ConnTest ispConnMock = mock(ConnTest.class);
        ConnTest cloudConnMock = mock(ConnTest.class);

        List<ConnTest> mockConnTests = List.of(localConnMock,ispConnMock,cloudConnMock);

        if(areTestRunning){
            when(localConnMock.getIpAddress()).thenReturn(ip1);
            when(ispConnMock.getIpAddress()).thenReturn(ip2);
            when(cloudConnMock.getIpAddress()).thenReturn(ip3);

            underTest.tests = mockConnTests;
        }else
            when(underTest.getConnTestsFromIpAddresses(ipAddresses)).thenReturn(mockConnTests);

        // then
        underTest.testCustomIps(ipAddresses);

        // assert
        if(areTestRunning) {
            verify(logger).warn("Tests are already running (IP {})", ip1);
            verify(logger).warn("Tests are already running (IP {})", ip2);
            verify(logger).warn("Tests are already running (IP {})", ip3);
        } else{
            underTest.tests.forEach(mockConnTest ->
                    verify(mockConnTest).startPingSession()
            );
        }
    }

    @ParameterizedTest
    @MethodSource("areTestsRunningProvider")
    void testStopTests(boolean areTestRunning) {
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        List<ConnTest> mockConnTests = List.of(mock(ConnTest.class),mock(ConnTest.class),mock(ConnTest.class));

        if(areTestRunning)
            underTest.tests = mockConnTests;

        // then
        underTest.stopTests();

        // assert
        if(!areTestRunning)
            verify(logger).warn("No tests to stop");
        else{
            mockConnTests.forEach(mockConnTest ->
                    verify(mockConnTest).stopPingSession()
            );
            assertTrue(underTest.tests.isEmpty());
        }
    }

    @ParameterizedTest
    @MethodSource("pingLogsProvider")
    void testGetPings(List<PingLog> pingLogs) throws IOException {
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        doReturn(pingLogs).when(pingLogRepository).findAllPingLogs();

        // then
        PingSessionExtract pings = underTest.getPings();

        // assert
        assertPingSessionResponseEntities(pingLogs, pings);
    }

    @Test
    void testGetPingsIOException() throws IOException {
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        doThrow(IOException.class).when(pingLogRepository).findAllPingLogs();

        // then assert
        assertThrows(IOException.class, underTest::getPings);
    }

    @ParameterizedTest
    @MethodSource("pingLogsProvider")
    void testGetPingsByDateTimeRange(List<PingLog> pingLogs) throws IOException {
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        doReturn(pingLogs).when(pingLogRepository).findPingLogsByDateTimeRange(START, END);

        // then
        PingSessionExtract pings = underTest.getPingsByDateTimeRange(
                START,
                END
        );

        // assert
        assertPingSessionResponseEntities(pingLogs, pings);
    }

    @Test
    void testGetPingsByDateTimeRangeIOException() throws IOException {
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        doThrow(IOException.class).when(pingLogRepository).findPingLogsByDateTimeRange(START, END);

        // then assert
        assertThrows(IOException.class, () -> underTest.getPingsByDateTimeRange(START,END));
    }

    @ParameterizedTest
    @MethodSource("pingLogsProvider")
    void testGetPingsByIp(List<PingLog> pingLogs) throws IOException {
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        doReturn(pingLogs).when(pingLogRepository).findPingLogByIp(LOCAL_IP_ADDRESS);

        // then
        PingSessionExtract pings = underTest.getPingsByIp(LOCAL_IP_ADDRESS);

        // assert
        assertPingSessionResponseEntities(pingLogs, pings);
    }

    @Test
    void testGetPingsByIpIOException() throws IOException {
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        doThrow(IOException.class).when(pingLogRepository).findPingLogByIp(LOCAL_IP_ADDRESS);

        // then assert
        assertThrows(IOException.class, () -> underTest.getPingsByIp(LOCAL_IP_ADDRESS));
    }

    @ParameterizedTest
    @MethodSource("pingLogsProvider")
    void testGetPingsByDateTimeRangeByIp(List<PingLog> pingLogs) throws IOException {
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        doReturn(pingLogs).when(pingLogRepository).findPingLogsByDateTimeRangeByIp(
                START,
                END,
                LOCAL_IP_ADDRESS
        );

        // then
        PingSessionExtract pings = underTest.getPingsByDateTimeRangeByIp(
                START,
                END,
                LOCAL_IP_ADDRESS
        );

        // assert
        assertPingSessionResponseEntities(pingLogs, pings);
    }

    @Test
    void testGetPingsByDateTimeRangeByIpIOException() throws IOException {
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        doThrow(IOException.class).when(pingLogRepository).findPingLogsByDateTimeRangeByIp(START,END,LOCAL_IP_ADDRESS);

        // then assert
        assertThrows(IOException.class, () -> underTest.getPingsByDateTimeRangeByIp(START,END,LOCAL_IP_ADDRESS));
    }


    @Test
    void testGetLostPingsByIp() throws IOException {
        // when
        List<PingLog> lostPingLogs = List.of(TestUtils.getDefaultLostPingLog());

        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        doReturn(lostPingLogs).when(pingLogRepository).findLostPingsByIp(LOCAL_IP_ADDRESS);

        // then
        PingSessionExtract pings = underTest.getLostPingsByIp(LOCAL_IP_ADDRESS);

        // assert
        assertEquals(1, pings.getAmountOfPings());
        PingLog pingLog = pings.getPingLogs().get(0);
        assertEquals(-1L, pingLog.getPingTime());
        assertEquals(DEFAULT_LOG_DATE_TIME, pingLog.getDateTime());
        assertEquals(LOCAL_IP_ADDRESS, pingLog.getIpAddress());
    }

    @Test
    void testGetLostPingsByDateTimeRangeByIp() throws IOException {
        // when
        List<PingLog> lostPingLogs = List.of(TestUtils.getDefaultLostPingLog());

        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        doReturn(lostPingLogs).when(pingLogRepository).findLostPingsByDateTimeRangeByIp(
                START,
                END,
                LOCAL_IP_ADDRESS
        );

        // then
        PingSessionExtract pings = underTest.getLostPingsByDateTimeRangeByIp(
                START,
                END,
                LOCAL_IP_ADDRESS
        );

        // assert
        assertEquals(1, pings.getAmountOfPings());
        PingLog pingLog = pings.getPingLogs().get(0);
        assertEquals(-1L, pingLog.getPingTime());
        assertEquals(DEFAULT_LOG_DATE_TIME, pingLog.getDateTime());
        assertEquals(LOCAL_IP_ADDRESS, pingLog.getIpAddress());
    }

    @Test
    void testGetPingsLostAvgByIp() throws IOException {
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        BigDecimal avg = new BigDecimal("0.2");
        doReturn(avg).when(pingLogRepository).findLostPingLogsAvgByIP(LOCAL_IP_ADDRESS);

        // then
        BigDecimal resultAvg = underTest.getPingsLostAvgByIp(LOCAL_IP_ADDRESS);

        // assert
        assertEquals(avg, resultAvg);
    }

    @Test
    void testGetPingsLostAvgByIpIOException() throws IOException {
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        doThrow(IOException.class).when(pingLogRepository).findLostPingLogsAvgByIP(LOCAL_IP_ADDRESS);

        // then assert
        assertThrows(IOException.class, () -> underTest.getPingsLostAvgByIp(LOCAL_IP_ADDRESS));
    }

    @Test
    void testGetPingsLostAvgByDateTimeRangeByIp() throws IOException {
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        BigDecimal avg = new BigDecimal("0.2");
        doReturn(avg).when(pingLogRepository).findLostPingLogsAvgByDateTimeRangeByIp(
                START,
                END,
                LOCAL_IP_ADDRESS
        );

        // then
        BigDecimal resultAvg = underTest.getPingsLostAvgByDateTimeRangeByIp(
                START,
                END,
                LOCAL_IP_ADDRESS
        );

        // assert
        assertEquals(avg, resultAvg);
    }

    @Test
    void testGetPingsLostAvgByDateTimeRangeByIpIOException() throws IOException {
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        doThrow(IOException.class).when(pingLogRepository).findLostPingLogsAvgByDateTimeRangeByIp(START,
                END,
                LOCAL_IP_ADDRESS);

        // then assert
        assertThrows(IOException.class, () -> underTest.getPingsLostAvgByDateTimeRangeByIp(START,
                END,
                LOCAL_IP_ADDRESS));
    }

    @Test
    void testCreateResponse(){
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        List<PingLog> pingLogs = List.of(getDefaultPingLog());

        // then
        PingSessionExtract response = underTest.createBasicResponse(pingLogs);

        // assert
        PingSessionExtract expectedResponse = PingSessionExtract.builder()
                .pingLogs(pingLogs)
                .amountOfPings(pingLogs.size())
                .build();

        assertEquals(1, response.getPingLogs().size());
        assertEquals(expectedResponse, response);
    }

    @Test
    void testGetLocalAndISPIpAddresses() throws IOException {
        // when
        ConnTestServiceImpl underTest =
                spy(new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils));
        BufferedReader bufferedReader = mock(BufferedReader.class);
        when(bufferedReader.readLine())
                .thenReturn("")
                .thenReturn("xxxxx x xx xxxxxxxxx dns.google [8.8.8.8]")
                .thenReturn("xxxxx xx xxxxxx xx 2 xxxxxx")
                .thenReturn("")
                .thenReturn("1    <1 ms    <1 ms    <1 ms  " + LOCAL_IP_ADDRESS)
                .thenReturn("2     1 ms     2 ms     1 ms  " + ISP_IP_ADDRESS)
                .thenReturn("")
                .thenReturn("xxxxx xxxxxxxx.")
                .thenReturn(null);

        doReturn(Optional.of(bufferedReader)).when(underTest).executeTracert();

        // then
        List<String> localAndISPIpAddresses = underTest.getLocalAndISPIpAddresses();

        // assert
        assertEquals(2, localAndISPIpAddresses.size());
        assertEquals(LOCAL_IP_ADDRESS, localAndISPIpAddresses.get(0));
        assertEquals(ISP_IP_ADDRESS, localAndISPIpAddresses.get(1));
    }

    @Test
    void testGetLocalAndISPIpAddressesIOException() throws IOException {
        // when
        ConnTestServiceImpl underTest =
                spy(new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils));
        BufferedReader bufferedReader = mock(BufferedReader.class);
        when(bufferedReader.readLine()).thenThrow(IOException.class);

        doReturn(Optional.of(bufferedReader)).when(underTest).executeTracert();

        // then
        underTest.getLocalAndISPIpAddresses();

        // assert
        verify(logger).error(eq("Traceroute error"), any(IOException.class));
    }

    @Test
    void testGetConnTestsFromIpAddresses() {
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        List<String> ipAddresses = List.of(LOCAL_IP_ADDRESS, ISP_IP_ADDRESS);

        // then
        List<ConnTest> connTestsFromIpAddresses = underTest.getConnTestsFromIpAddresses(ipAddresses);

        // assert
        assertEquals(LOCAL_IP_ADDRESS, connTestsFromIpAddresses.get(0).getIpAddress());
        assertEquals(ISP_IP_ADDRESS, connTestsFromIpAddresses.get(1).getIpAddress());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testExecuteTracert() throws IOException {
        // when
        ConnTestServiceImpl underTest =
                spy(new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils));
        Optional<BufferedReader> mockReader = mock(Optional.class);
        doReturn(mockReader).when(tracertProvider).executeTracert();

        // then
        Optional<BufferedReader> reader = underTest.executeTracert();

        // assert
        verify(tracertProvider).executeTracert();
        assertEquals(mockReader, reader);
    }

    @Test
    void testGetIpAddressesFromActiveTests(){
        // when
        ConnTestServiceImpl underTest =
                spy(new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils));
        List<String> ipAddresses = List.of("IP_1", "IP_2", "IP_3");
        underTest.tests = ipAddresses.stream()
                .map(ip -> new ConnTest(executorService, ip, logger, pingLogRepository, pingUtils))
                .toList();

        // then
        List<String> result = underTest.getIpAddressesFromActiveTests();

        // assert
        // convert the lists to set to compare ignoring the ordering
        Set<String> expected = new HashSet<>(ipAddresses);
        Set<String> testResult = new HashSet<>(result);

        assertEquals(expected, testResult);
    }

    @Test
    void testClearPinglogFile() throws InterruptedException {
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);

        // then
        underTest.clearPingLogFile();

        // assert
        verify(pingLogRepository).clearPingLogFile();
    }

    @Test
    void testGetMaxMinPingLog() throws IOException {
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        doReturn(
                List.of(getDefaultPingLog(),
                        getDefaultPingLog()))
                .when(pingLogRepository).findMaxMinPingLog(LOCAL_IP_ADDRESS);

        // then
        PingSessionExtract pings = underTest.getMaxMinPingLog(LOCAL_IP_ADDRESS);

        // assert
        assertEquals(DEFAULT_PING_TIME, pings.getPingLogs().get(0).getPingTime());
        assertEquals(DEFAULT_PING_TIME, pings.getPingLogs().get(1).getPingTime());
        assertEquals(2, pings.getAmountOfPings());
        assertEquals(LOCAL_IP_ADDRESS, pings.getIpAddress());
    }

    @Test
    void testGetAvgLatencyByIp() throws IOException {
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        BigDecimal avg = new BigDecimal("0.2");
        doReturn(avg).when(pingLogRepository).findAvgLatencyByIp(LOCAL_IP_ADDRESS);

        // then
        BigDecimal resultAvg = underTest.getAvgLatencyByIp(LOCAL_IP_ADDRESS);

        // assert
        assertEquals(avg, resultAvg);
    }

    @Test
    void testGetAvgLatencyByDateTimeRangeByIp() throws IOException {
        // when
        ConnTestServiceImpl underTest =
                new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository, pingUtils);
        BigDecimal avg = new BigDecimal("0.2");
        doReturn(avg).when(pingLogRepository).findAvgLatencyByDateTimeRangeByIp(
                START,
                END,
                LOCAL_IP_ADDRESS
        );

        // then
        BigDecimal resultAvg = underTest.getAvgLatencyByDateTimeRangeByIp(
                START,
                END,
                LOCAL_IP_ADDRESS
        );

        // assert
        verify(pingLogRepository).findAvgLatencyByDateTimeRangeByIp(START, END, LOCAL_IP_ADDRESS);
        assertEquals(avg, resultAvg);
    }

    static Stream<Boolean> areTestsRunningProvider() {
        return Stream.of(
                true,
                false
        );
    }

    static Stream<List<PingLog>> pingLogsProvider() {
        return Stream.of(
                List.of(),
                List.of(getDefaultPingLog())
        );
    }

    private static void assertPingSessionResponseEntities(List<PingLog> pingLogs, PingSessionExtract pings) {
        if(pingLogs.isEmpty()) {
            assertEquals(0, pings.getAmountOfPings());
            assertTrue(pings.getPingLogs().isEmpty());
        }else{
            assertEquals(1, pings.getAmountOfPings());
            PingLog pingLog = pings.getPingLogs().get(0);
            assertEquals(DEFAULT_PING_TIME, pingLog.getPingTime());
            assertEquals(DEFAULT_LOG_DATE_TIME, pingLog.getDateTime());
            assertEquals(LOCAL_IP_ADDRESS, pingLog.getIpAddress());
        }
    }
}