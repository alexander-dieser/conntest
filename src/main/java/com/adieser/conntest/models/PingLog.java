package com.adieser.conntest.models;

import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Data structure to represent a Ping
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PingLog extends RepresentationModel<PingLog> {

    /**
     * log datetime
     */
    @CsvDate(value = "yyyy-MM-dd HH:mm:ss")
    @CsvBindByPosition(position = 0)
    private LocalDateTime dateTime;

    /**
     * Ip address that was pinged
     */
    @CsvBindByPosition(position = 1)
    private String ipAddress;

    /**
     * time it took to ping the ip address (milliseconds)
     */
    @CsvBindByPosition(position = 2)
    private long pingTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PingLog pingLog)) return false;
        if (!super.equals(o)) return false;
        return pingTime == pingLog.pingTime && Objects.equals(dateTime, pingLog.dateTime) && Objects.equals(ipAddress, pingLog.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dateTime, ipAddress, pingTime);
    }
}
