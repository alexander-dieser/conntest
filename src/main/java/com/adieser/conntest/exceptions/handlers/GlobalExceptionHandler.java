package com.adieser.conntest.exceptions.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * All the exceptions, checked and unchecked, are handled by this handler
     * @param ex exception to be handled
     * @return generic error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        return new ResponseEntity<>("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles HttpClientErrorException.BadRequest
     * @param ex exception to be handled
     * @return generic error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleBadRequestException(MethodArgumentTypeMismatchException ex) {
        return new ResponseEntity<>("Bad Request", HttpStatus.BAD_REQUEST);
    }
}

