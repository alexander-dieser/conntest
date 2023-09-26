package com.adieser.conntest.controllers.responses;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PingLogFile {
    private String fileName;
    private long amountOfPings;
    private List<PingLog> pingLogs;
}
