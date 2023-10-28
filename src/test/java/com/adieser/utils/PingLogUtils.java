package com.adieser.utils;

import com.adieser.conntest.models.PingLog;

import java.time.LocalDateTime;

public class PingLogUtils {
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
}
