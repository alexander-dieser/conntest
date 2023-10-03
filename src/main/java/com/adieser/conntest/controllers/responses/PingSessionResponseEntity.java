package com.adieser.conntest.controllers.responses;

import com.adieser.conntest.models.PingLog;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Class to hold the result of a set of pings
 */
@Data
@Builder
public class PingSessionResponseEntity {
    private long amountOfPings;
    private List<PingLog> pingLogs;
}
