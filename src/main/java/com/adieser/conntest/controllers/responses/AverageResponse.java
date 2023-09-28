package com.adieser.conntest.controllers.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AverageResponse {
    private BigDecimal average;
}
