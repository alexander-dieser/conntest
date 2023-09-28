package com.adieser.conntest.models;

import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PingLog {

    /**
     * log datetime
     */
    @CsvDate(value = "yyyy-MM-dd HH:mm:ss")
    @CsvBindByPosition(position = 0)
    private LocalDateTime dateTime;

    @CsvBindByPosition(position = 1)
    private String ipAddress;

    /**
     * time in milliseconds
     */
    @CsvBindByPosition(position = 2)
    private long pingTime;
}
