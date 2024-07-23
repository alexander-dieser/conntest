package com.adieser.utils;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.IOException;

import static org.mockito.Mockito.mock;

@RestController
@RequestMapping("/exceptionHandlerTest")
public class ExceptionHandlerTestController {

    @GetMapping("/simulateIOException")
    public String simulateIOException() throws IOException {
        throw mock(IOException.class);
    }

    @GetMapping("/simulateMethodArgumentTypeMismatchException")
    public String simulateMethodArgumentTypeMismatchException() {
        throw mock(MethodArgumentTypeMismatchException.class);
    }
}
