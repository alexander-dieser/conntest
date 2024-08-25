package com.adieser.conntest.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private final Logger logger;

    public RequestLoggingInterceptor(Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler){
        String sessionIdLast10 = request.getSession().getId().substring(
                request.getSession().getId().length() - 10
        );

        logger.info("Incoming request: session={}, method={}, uri={}, params={}",
                sessionIdLast10,
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString());
        return true;
    }
}

