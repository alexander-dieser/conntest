package com.adieser.conntest.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestLoggingInterceptorTest {

    @Test
    void preHandle() {
        // when
        Logger logger = mock(Logger.class);
        RequestLoggingInterceptor requestLoggingInterceptor = new RequestLoggingInterceptor(logger);
        HttpServletRequest request = mock(HttpServletRequest.class);

        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn("aaabbb1234567890");

        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");
        when(request.getQueryString()).thenReturn("param=value");
        when(request.getSession()).thenReturn(session);

        //then
        boolean result = requestLoggingInterceptor.preHandle(request, mock(HttpServletResponse.class), mock(Object.class));

        // assert
        assertTrue(result);
        verify(logger, times(1)).info("Incoming request: session={}, method={}, uri={}, params={}",
                "1234567890",
                "GET",
                "/test",
                "param=value");
    }
}