package com.adieser.conntest.views;


import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PingRow {
    /**
     * log datetime
     */
    private LocalDateTime dateTime;
    /**
     * list of times it took to ping the ip address (milliseconds)
     */
    private List<String> pingTimes;

    public PingRow(LocalDateTime dateTime, List<String> pingTimes) {
        this.dateTime = dateTime;
        this.pingTimes = pingTimes;
    }
}


