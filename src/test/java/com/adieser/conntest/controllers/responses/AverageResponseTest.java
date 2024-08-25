package com.adieser.conntest.controllers.responses;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static com.adieser.utils.TestUtils.LOCAL_IP_ADDRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AverageResponseTest {

    private static final BigDecimal DEFAULT_AVERAGE = new BigDecimal(1);

    @ParameterizedTest
    @MethodSource("averageProvider")
    void testEquals(BigDecimal avg) {
        AverageResponse avgResponse1 = buildAvgResponse(DEFAULT_AVERAGE);
        AverageResponse avgResponse2 = buildAvgResponse(avg);

        if(avg.compareTo(DEFAULT_AVERAGE) == 0) {
            assertEquals(avgResponse1, avgResponse2);
            assertEquals(avgResponse1.hashCode(), avgResponse2.hashCode());
        }
        else {
            assertNotEquals(avgResponse1, avgResponse2);
            assertNotEquals(avgResponse1.hashCode(), avgResponse2.hashCode());
        }
    }

    private static AverageResponse buildAvgResponse(BigDecimal avg) {
        return AverageResponse.builder()
                .startDate(LocalDateTime.of(2023, 11, 10, 0, 0, 0))
                .endDate(LocalDateTime.of(2023, 11, 11, 0, 0, 0))
                .ipAddress(LOCAL_IP_ADDRESS)
                .average(avg)
                .build();
    }

    static Stream<BigDecimal> averageProvider() {
        return Stream.of(
                new BigDecimal(1),
                new BigDecimal("0.5")
        );
    }
}