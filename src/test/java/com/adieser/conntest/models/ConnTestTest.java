package com.adieser.conntest.models;

import com.adieser.utils.PingLogUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static com.adieser.conntest.models.ConnTest.PING_SESSION_INTERRUPTED_MSG;
import static com.adieser.utils.PingLogUtils.DEFAULT_PING_TIME;
import static com.adieser.utils.PingLogUtils.LOCAL_IP_ADDRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnTestTest {
    @Mock
    Logger logger;

    @Mock
    ExecutorService executorService;

    @Mock
    PingLogRepository pingLogRepository;

    @Test
    void testStartPingSession() {
        // when
        ConnTest underTest = new ConnTest(executorService, LOCAL_IP_ADDRESS, logger, pingLogRepository);
        doNothing().when(executorService).execute(any());

        // then
        underTest.startPingSession();

        // assert
        verify(executorService, times(1)).execute(any());
    }

    @Test
    void testStopPingSession() {
        // when
        ConnTest underTest = new ConnTest(executorService, LOCAL_IP_ADDRESS, logger, pingLogRepository);

        // then
        underTest.stopPingSession();

        // assert
        assertFalse(underTest.running);
    }

    @Test
    @SuppressWarnings("squid:S2925")
    void testPingSessionSuccess() throws InterruptedException {
        PingLog defaultPingLog = PingLogUtils.getDefaultPingLog();

        // when
        ConnTest underTestSpy = spy(new ConnTest(executorService, LOCAL_IP_ADDRESS, logger, pingLogRepository));
        doReturn(DEFAULT_PING_TIME).when(underTestSpy).ping();
        when(underTestSpy.buildPingLog(DEFAULT_PING_TIME)).thenReturn(defaultPingLog);
        doNothing().when(pingLogRepository).savePingLog(any());

        // then
        new Thread(underTestSpy::pingSession).start();
        Thread.sleep(2000);
        underTestSpy.running = false;

        // assert
        verify(underTestSpy, atLeastOnce()).ping();
        verify(pingLogRepository, atLeastOnce()).savePingLog(defaultPingLog);
    }

    @Test
    @SuppressWarnings("squid:S2925")
    void testPingSessionInterruptedException() throws InterruptedException {
        // when
        ConnTest underTestSpy = spy(new ConnTest(executorService, LOCAL_IP_ADDRESS, logger, pingLogRepository));

        // then
        Thread thread = new Thread(underTestSpy::pingSession);
        thread.start();
        while(!underTestSpy.running)
            Thread.sleep(50); // give time to start the thread before interrupting it
        thread.interrupt();

        // assert
        verify(logger, times(1)).warn(PING_SESSION_INTERRUPTED_MSG);
    }

    @ParameterizedTest
    @MethodSource("testPingSuccess")
    @SuppressWarnings("squid:S2925")
    void testPing(boolean isReachable) {
        // when
        ConnTest underTestSpy = spy(new ConnTest(executorService, LOCAL_IP_ADDRESS, logger, pingLogRepository));
        when(underTestSpy.isReachable(LOCAL_IP_ADDRESS)).thenReturn(isReachable);

        // then
        long ping = underTestSpy.ping();

        /* assert
            ping != -1 means ping success, i.e. it is reachable
         */
        assertEquals(isReachable, ping != -1L);
    }

    @ParameterizedTest
    @MethodSource("testPingSuccess")
    void testIsReachable(boolean isReachable) throws IOException {
        // when
        ConnTest underTestSpy = spy(new ConnTest(executorService, LOCAL_IP_ADDRESS, logger, pingLogRepository));
        InetAddress inet = mock(InetAddress.class);
        when(inet.isReachable(anyInt())).thenReturn(isReachable);
        when(underTestSpy.getInet(LOCAL_IP_ADDRESS)).thenReturn(inet);

        // then
        boolean reachable = underTestSpy.isReachable(LOCAL_IP_ADDRESS);

        // assert
        assertEquals(isReachable, reachable);
    }

    @Test
    void testIsReachableIOException() throws IOException {
        // when
        ConnTest underTestSpy = spy(new ConnTest(executorService, LOCAL_IP_ADDRESS, logger, pingLogRepository));
        InetAddress inet = mock(InetAddress.class);
        when(inet.isReachable(anyInt())).thenThrow(IOException.class);
        when(underTestSpy.getInet(LOCAL_IP_ADDRESS)).thenReturn(inet);

        // then
        underTestSpy.isReachable(LOCAL_IP_ADDRESS);

        // assert
        verify(logger, times(1)).error(anyString(), anyString(), any(IOException.class));
    }

    @Test
    void testIsReachableUnknownHostException() throws UnknownHostException {
        // when
        ConnTest underTestSpy = spy(new ConnTest(executorService, LOCAL_IP_ADDRESS, logger, pingLogRepository));
        when(underTestSpy.getInet(LOCAL_IP_ADDRESS)).thenThrow(UnknownHostException.class);

        // then
        underTestSpy.isReachable(LOCAL_IP_ADDRESS);

        // assert
        verify(logger, times(1)).error(anyString(), anyString(), any(UnknownHostException.class));
    }

    static Stream<Boolean> testPingSuccess() {
        return Stream.of(
                true,
                false
        );
    }
}
