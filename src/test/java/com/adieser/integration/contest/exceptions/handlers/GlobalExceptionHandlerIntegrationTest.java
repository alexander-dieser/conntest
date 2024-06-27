package com.adieser.integration.contest.exceptions.handlers;

import com.adieser.conntest.ConntestApplication;
import com.adieser.conntest.exceptions.handlers.GlobalExceptionHandler;
import com.adieser.utils.ExceptionHandlerTestController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//@WebMvcTest(ExceptionHandlerTestController.class)
@SpringBootTest(classes = ConntestApplication.class)
@ContextConfiguration(classes = {ConntestApplication.class, ExceptionHandlerTestController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testHandleIOException() throws Exception {
        mockMvc.perform(get("/exceptionHandlerTest/simulateIOException"))
                .andExpect(status().is5xxServerError())
                .andExpect(content().string("Internal Server Error"));
    }

    @Test
    void testHandleMethodArgumentTypeMismatchException() throws Exception {
        mockMvc.perform(get("/exceptionHandlerTest/simulateMethodArgumentTypeMismatchException"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Bad Request"));
    }
}
