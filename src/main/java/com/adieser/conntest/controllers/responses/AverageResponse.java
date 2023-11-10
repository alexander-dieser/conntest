package com.adieser.conntest.controllers.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.hateoas.RepresentationModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Class to hold the result of average of lost pings
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AverageResponse extends RepresentationModel<AverageResponse> {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String ipAddress;
    private BigDecimal average;

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (o.getClass() != getClass()) return false;

        AverageResponse that = (AverageResponse) o;

        return super.equals(o) &&
                this.startDate.equals(that.startDate) &&
                this.endDate.equals(that.endDate) &&
                this.ipAddress.equals(that.ipAddress) &&
                this.average.equals(that.average);
    }

    @Override
    public int hashCode() {
        return super.hashCode() +
                this.startDate.hashCode() +
                this.endDate.hashCode() +
                this.ipAddress.hashCode() +
                this.average.hashCode();
    }
}
