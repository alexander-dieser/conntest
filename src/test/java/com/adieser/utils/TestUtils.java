package com.adieser.utils;

import com.adieser.conntest.models.PingLog;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;

public class TestUtils {
    public static final String LOCAL_IP_ADDRESS = "192.168.1.1";
    public static final String CLOUD_IP_ADDRESS = "8.8.8.8";
    public static final String ISP_IP_ADDRESS = "130.101.75.1";
    public static final LocalDateTime DEFAULT_LOG_DATE_TIME = LocalDateTime.of(2023, 10, 6, 2, 8, 35);
    public static final Long DEFAULT_PING_TIME = 13L;

    public static PingLog getDefaultPingLog() {
        return PingLog.builder()
                .ipAddress(LOCAL_IP_ADDRESS)
                .dateTime(DEFAULT_LOG_DATE_TIME)
                .pingTime(DEFAULT_PING_TIME)
                .build();
    }

    public static PingLog getDefaultLostPingLog() {
        return PingLog.builder()
                .ipAddress(LOCAL_IP_ADDRESS)
                .dateTime(DEFAULT_LOG_DATE_TIME)
                .pingTime(-1L)
                .build();
    }

    /**
     * This method takes a MockHttpServletRequestBuilder object as input and returns a new MockHttpServletRequestBuilder
     * object with a custom session ID added to it.
     * @param requestBuilder the MockHttpServletRequestBuilder object to add the custom session ID to
     * @return requestBuilder with the custom session ID added to it
     * @see CustomSessionIdMockHttpSession
     */
    public static MockHttpServletRequestBuilder addCustomSessionId(MockHttpServletRequestBuilder requestBuilder){
        MockHttpSession session = new CustomSessionIdMockHttpSession("aabb1234567890");
        requestBuilder.session(session);

        return requestBuilder;
    }

    /**
     * recursively delete directories, subdirectories and files
     */
    public static void deleteDir(File fileOrDir) throws IOException {
        if (fileOrDir.isDirectory()) {
            File[] files = fileOrDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDir(file);
                }
            }
        }

        Files.deleteIfExists(fileOrDir.toPath());
    }
}
