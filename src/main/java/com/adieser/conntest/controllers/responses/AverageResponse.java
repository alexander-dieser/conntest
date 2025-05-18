package com.adieser.conntest.controllers.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.hateoas.RepresentationModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Class to hold the result of average of lost pings
 */
@EqualsAndHashCode(callSuper = false)
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AverageResponse extends RepresentationModel<AverageResponse> {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String ipAddress;
    private BigDecimal average;
}
