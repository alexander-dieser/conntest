package com.adieser.conntest.exceptions.handlers;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    @Test
    void testHandleIOException() {
        // when
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        // then
        ResponseEntity<String> response = handler.handleException(mock(Exception.class));

        // assert
        assertEquals("Internal Server Error", response.getBody());
        assertEquals(500, response.getStatusCode().value());
    }


    @Test
    void testHandleMethodArgumentTypeMismatchExceptionException() {
        // when
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        // then
        ResponseEntity<String> response = handler.handleBadRequestException(mock(MethodArgumentTypeMismatchException.class));

        // assert
        assertEquals("Bad Request", response.getBody());
        assertEquals(400, response.getStatusCode().value());
    }
}