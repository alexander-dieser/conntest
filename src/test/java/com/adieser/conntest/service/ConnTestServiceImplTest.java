package com.adieser.conntest.service;

import com.adieser.conntest.controllers.responses.PingSessionResponseEntity;
import com.adieser.conntest.models.ConnTest;
import com.adieser.conntest.models.PingLog;
import com.adieser.conntest.models.PingLogRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static com.adieser.utils.PingLogUtils.DEFAULT_LOG_DATE_TIME;
import static com.adieser.utils.PingLogUtils.DEFAULT_PING_TIME;
import static com.adieser.utils.PingLogUtils.ISP_IP_ADDRESS;
import static com.adieser.utils.PingLogUtils.LOCAL_IP_ADDRESS;
import static com.adieser.utils.PingLogUtils.getDefaultPingLog;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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

    @Test
    void testTestLocalISPInternet() {
        // when
        ConnTestServiceImpl underTest = spy(new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository));
        List<String> localAndISPIpAddresses = new ArrayList<>(Arrays.asList(LOCAL_IP_ADDRESS, ISP_IP_ADDRESS));

        List<ConnTest> mockConnTests = List.of(mock(ConnTest.class),mock(ConnTest.class),mock(ConnTest.class));
        when(underTest.getConnTestsFromIpAddresses(localAndISPIpAddresses)).thenReturn(mockConnTests);
        doReturn(localAndISPIpAddresses).when(underTest).getLocalAndISPIpAddresses();

        // then
        underTest.testLocalISPInternet();

        // assert
        mockConnTests.forEach(mockConnTest ->
                verify(mockConnTest, times(1)).startPingSession()
        );
    }

    @ParameterizedTest
    @MethodSource("connTestsProvider")
    void testStopTests(List<ConnTest> mockConnTests) {
        // when
        ConnTestServiceImpl underTest = new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository);
        underTest.tests = mockConnTests;

        // then
        underTest.stopTests();

        // assert
        if(mockConnTests.isEmpty())
            verify(logger, times(1)).warn("No tests to stop");
        else{
            mockConnTests.forEach(mockConnTest ->
                    verify(mockConnTest, times(1)).stopPingSession()
            );
            verify(executorService, times(1)).shutdown();
        }
    }

    @ParameterizedTest
    @MethodSource("pingLogsProvider")
    void testGetPings(List<PingLog> pingLogs) {
        // when
        ConnTestServiceImpl underTest = new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository);
        doReturn(pingLogs).when(pingLogRepository).findAllPingLogs();

        // then
        List<PingSessionResponseEntity> pings = underTest.getPings();

        // assert
        assertPingSessionResponseEntities(pingLogs, pings);
    }

    @ParameterizedTest
    @MethodSource("pingLogsProvider")
    void testGetPingsByDateTimeRange(List<PingLog> pingLogs) {
        // when
        ConnTestServiceImpl underTest =new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository);
        doReturn(pingLogs).when(pingLogRepository).findPingLogsByDateTimeRange(START, END);

        // then
        List<PingSessionResponseEntity> pings = underTest.getPingsByDateTimeRange(
                START,
                END
        );

        // assert
        assertPingSessionResponseEntities(pingLogs, pings);
    }

    @ParameterizedTest
    @MethodSource("pingLogsProvider")
    void testGetPingsByIp(List<PingLog> pingLogs) {
        // when
        ConnTestServiceImpl underTest = new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository);
        doReturn(pingLogs).when(pingLogRepository).findPingLogByIp(LOCAL_IP_ADDRESS);

        // then
        List<PingSessionResponseEntity> pings = underTest.getPingsByIp(LOCAL_IP_ADDRESS);

        // assert
        assertPingSessionResponseEntities(pingLogs, pings);
    }

    @ParameterizedTest
    @MethodSource("pingLogsProvider")
    void testGetPingsByDateTimeRangeByIp(List<PingLog> pingLogs) {
        // when
        ConnTestServiceImpl underTest = new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository);
        doReturn(pingLogs).when(pingLogRepository).findPingLogsByDateTimeRangeByIp(
                START,
                END,
                LOCAL_IP_ADDRESS
        );

        // then
        List<PingSessionResponseEntity> pings = underTest.getPingsByDateTimeRangeByIp(
                START,
                END,
                LOCAL_IP_ADDRESS
        );

        // assert
        assertPingSessionResponseEntities(pingLogs, pings);
    }

    @Test
    void testGetPingsLostAvgByIp() {
        // when
        ConnTestServiceImpl underTest = new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository);
        BigDecimal avg = new BigDecimal("0.2");
        doReturn(avg).when(pingLogRepository).findLostPingLogsAvgByIP(LOCAL_IP_ADDRESS);

        // then
        BigDecimal resultAvg = underTest.getPingsLostAvgByIp(LOCAL_IP_ADDRESS);

        // assert
        assertEquals(avg, resultAvg);
    }

    @Test
    void testGetPingsLostAvgByDateTimeRangeByIp() {
        // when
        ConnTestServiceImpl underTest = new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository);
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
    void testCreateResponse(){
        // when
        ConnTestServiceImpl underTest = new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository);
        List<PingLog> pingLogs = List.of(getDefaultPingLog());

        // then
        List<PingSessionResponseEntity> responses = new ArrayList<>();
        underTest.createResponse(responses, pingLogs);

        // assert
        PingSessionResponseEntity expectedResponse = PingSessionResponseEntity.builder()
                .pingLogs(pingLogs)
                .amountOfPings(pingLogs.size())
                .build();

        assertEquals(1, responses.size());
        assertEquals(expectedResponse, responses.get(0));
    }

    @Test
    void testGetLocalAndISPIpAddresses() throws IOException {
        // when
        ConnTestServiceImpl underTest = spy(new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository));
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
        ConnTestServiceImpl underTest = spy(new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository));
        BufferedReader bufferedReader = mock(BufferedReader.class);
        when(bufferedReader.readLine()).thenThrow(IOException.class);

        doReturn(Optional.of(bufferedReader)).when(underTest).executeTracert();

        // then
        underTest.getLocalAndISPIpAddresses();

        // assert
        verify(logger, times(1)).error(eq("Traceroute error"), any(IOException.class));
    }

    @Test
    void testGetConnTestsFromIpAddresses() {
        // when
        ConnTestServiceImpl underTest = new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository);
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
        ConnTestServiceImpl underTest = spy(new ConnTestServiceImpl(executorService, logger, tracertProvider, pingLogRepository));
        Optional<BufferedReader> mockReader = mock(Optional.class);
        doReturn(mockReader).when(tracertProvider).executeTracert();

        // then
        Optional<BufferedReader> reader = underTest.executeTracert();

        // assert
        verify(tracertProvider, times(1)).executeTracert();
        assertEquals(mockReader, reader);
    }

    static Stream<List<ConnTest>> connTestsProvider() {
        return Stream.of(
                List.of(mock(ConnTest.class)),
                List.of()
        );
    }

    static Stream<List<PingLog>> pingLogsProvider() {
        return Stream.of(
                List.of(),
                List.of(getDefaultPingLog())
        );
    }

    private static void assertPingSessionResponseEntities(List<PingLog> pingLogs, List<PingSessionResponseEntity> pings) {
        if(pingLogs.isEmpty()) {
            assertEquals(0, pings.get(0).getAmountOfPings());
            assertTrue(pings.get(0).getPingLogs().isEmpty());
        }else{
            assertEquals(1, pings.get(0).getAmountOfPings());
            PingLog pingLog = pings.get(0).getPingLogs().get(0);
            assertEquals(DEFAULT_PING_TIME, pingLog.getPingTime());
            assertEquals(DEFAULT_LOG_DATE_TIME, pingLog.getDateTime());
            assertEquals(LOCAL_IP_ADDRESS, pingLog.getIpAddress());
        }
    }
}