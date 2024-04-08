package com.adieser.conntest.models;

import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;

/**
 * Data structure to represent a Ping
 */
@EqualsAndHashCode(callSuper = true)
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
}
