package com.adieser.conntest.controllers.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Class to hold the result of average of lost pings
 */
@Data
@AllArgsConstructor
public class AverageResponse {
    private BigDecimal average;
}
