package com.adieser.conntest.controllers.responses;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PingLog {
    private LocalDateTime date;
    private String logLevel;
    private String ipAddress;
    private long time;
}
