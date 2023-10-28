package com.adieser.conntest.controllers.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Class to hold the result of average of lost pings
 */
@Getter
@AllArgsConstructor
public class AverageResponse {
    private BigDecimal average;
}
