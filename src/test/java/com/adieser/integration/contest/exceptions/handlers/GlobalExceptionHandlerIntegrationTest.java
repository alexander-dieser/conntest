package com.adieser.integration.contest.exceptions.handlers;

import com.adieser.conntest.ConntestApplication;
import com.adieser.conntest.exceptions.handlers.GlobalExceptionHandler;
import com.adieser.conntest.service.writer.FileWriterService;
import com.adieser.utils.ExceptionHandlerTestController;
import com.adieser.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ConntestApplication.class)
@ContextConfiguration(classes = {ConntestApplication.class, ExceptionHandlerTestController.class, GlobalExceptionHandler.class, FileWriterService.class})
@AutoConfigureMockMvc
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testHandleIOException() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder = TestUtils.addCustomSessionId(
                MockMvcRequestBuilders.get("/exceptionHandlerTest/simulateIOException")
        );

        // then assert
        mockMvc.perform(requestBuilder)
                .andExpect(status().is5xxServerError())
                .andExpect(content().string("Internal Server Error"));
    }

    @Test
    void testHandleMethodArgumentTypeMismatchException() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder = TestUtils.addCustomSessionId(
                MockMvcRequestBuilders.get("/exceptionHandlerTest/simulateMethodArgumentTypeMismatchException")
        );

        // then assert
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Bad Request"));
    }
}
