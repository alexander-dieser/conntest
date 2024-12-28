package com.adieser.integration.contest.exceptions.handlers;

import com.adieser.conntest.ConntestApplication;
import com.adieser.conntest.exceptions.handlers.GlobalExceptionHandler;
import com.adieser.conntest.service.writer.FileWriterService;
import com.adieser.utils.ExceptionHandlerTestController;
import com.adieser.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.adieser.utils.TestUtils.deleteDir;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ConntestApplication.class)
@ContextConfiguration(classes = {ConntestApplication.class, ExceptionHandlerTestController.class, GlobalExceptionHandler.class, FileWriterService.class})
@AutoConfigureMockMvc
class GlobalExceptionHandlerIntegrationTest {
    private static final String PINGLOGS_DIR = "src/test/resources/pingLogs";

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(Paths.get(PINGLOGS_DIR));
    }

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

    @AfterEach
    public void clean() throws IOException {
        deleteDir(Paths.get(PINGLOGS_DIR).toFile());
    }
}
