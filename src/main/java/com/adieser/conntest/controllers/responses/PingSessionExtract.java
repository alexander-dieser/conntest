package com.adieser.conntest.controllers.responses;

import com.adieser.conntest.models.PingLog;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Class to hold the result of a set of pings
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PingSessionExtract {
    private long amountOfPings;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String ipAddress;
    private List<PingLog> pingLogs;
}
