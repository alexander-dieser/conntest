package com.adieser.conntest.models.utils;

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
import java.util.stream.Stream;

import static com.adieser.utils.TestUtils.LOCAL_IP_ADDRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PingUtilsTest {
    @Mock
    Logger logger;

    @ParameterizedTest
    @MethodSource("testPingSuccess")
    void testIsReachable(boolean isReachable) throws IOException {
        // when
        PingUtils underTestSpy = spy(new PingUtils(logger));
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
        PingUtils underTestSpy = spy(new PingUtils(logger));
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
        PingUtils underTestSpy = spy(new PingUtils(logger));
        when(underTestSpy.getInet(LOCAL_IP_ADDRESS)).thenThrow(UnknownHostException.class);
        doNothing().when(logger).error(any(String.class), any(), any());

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