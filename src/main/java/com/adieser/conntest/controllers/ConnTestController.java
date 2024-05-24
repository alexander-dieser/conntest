package com.adieser.conntest.controllers;

import com.adieser.conntest.controllers.responses.AverageResponse;
import com.adieser.conntest.controllers.responses.PingSessionExtract;
import com.adieser.conntest.controllers.responses.PingTestResponse;
import com.adieser.conntest.service.ConnTestService;
import com.adieser.conntest.utils.HATEOASLinkRelValueTemplates;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static com.adieser.conntest.controllers.responses.ErrorResponse.MAX_IP_ADDRESSES_EXCEEDED;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller to interact with pings
 */

/*
    produces={"application/json"} is used in some endpoints to avoid the HATEOAS
    default content-type: application/hal+json in the responses
 */
@RestController
@Tag(name = "Ping", description = "Ping endpoints")
public class ConnTestController {

    private final ConnTestService connTestService;

    public ConnTestController(ConnTestService connTestService) {
        this.connTestService = connTestService;
    }

    /**
     * Trigger 3 ping sessions to test connection against local gateway, the next hop, and the cloud (Google's 8.8.8.8)
     */
    @PostMapping(value = "/test-local-isp-cloud", produces = {"application/json"})
    @Operation(summary = "Test connection to local ISP, next hop, and cloud (Google's 8.8.8.8)")
    public ResponseEntity<PingTestResponse> testLocalIspCloud() {
        connTestService.testLocalISPInternet();

        PingTestResponse response = new PingTestResponse();
        response.add(linkTo(methodOn(ConnTestController.class).stopTests())
                .withRel(HATEOASLinkRelValueTemplates.STOP_TEST_SESSION));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Trigger up to 3 custom ip addresses ping sessions to test connection
     */
    @PostMapping(value = "/test-custom-ips", produces = {"application/json"})
    @Operation(summary = "Test connection to custom IP addresses")
    public ResponseEntity<Object> testCustomIps(
            @Parameter(
                    description = "IP addresses to ping",
                    required = true)
            @RequestBody List<String> ipAddresses) {
        HttpStatus httpStatus = HttpStatus.OK;

        if(ipAddresses.size() > 3)
            return ResponseEntity.badRequest().body(MAX_IP_ADDRESSES_EXCEEDED);

        connTestService.testCustomIps(ipAddresses);
        PingTestResponse pingResponse = new PingTestResponse();
        pingResponse.add(linkTo(methodOn(ConnTestController.class).stopTests())
                .withRel(HATEOASLinkRelValueTemplates.STOP_TEST_SESSION));

        return new ResponseEntity<>(pingResponse, httpStatus);
    }

    /**
     * stop all the test sessions
     */
    @PostMapping(value = "/stop-tests", produces={"application/json"})
    @Operation(summary = "Stop all the test sessions")
    public ResponseEntity<PingTestResponse> stopTests() {
        connTestService.stopTests();

        PingTestResponse response = new PingTestResponse();
        response.add(linkTo(methodOn(ConnTestController.class).testLocalIspCloud())
                .withRel(HATEOASLinkRelValueTemplates.START_TEST_SESSION));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Get all pings
     * @return If successful it returns a list of pings along with the total count
     */
    @ApiResponse(responseCode = "200", description = "a list of pings along with the total count.")
    @GetMapping("/pings")
    @Operation(summary = "Get all pings")
    public ResponseEntity<PingSessionExtract> getPings() {
        HttpStatus httpStatus = HttpStatus.OK;
        PingSessionExtract pings = connTestService.getPings();

        if(pings.getPingLogs().isEmpty())
            httpStatus = HttpStatus.NO_CONTENT;

        addHATEOASLinksForPingLogsReturningEndpoints(pings);

        return new ResponseEntity<>(pings, httpStatus);
    }

    /**
     * Get all the pings within a datetime range
     * @param start Start date and time of the range
     * @param end End date in the range
     * @return If successful it returns a list of pings along with the total count
     */
    @ApiResponse(responseCode = "200", description = "a list of pings along with the total count.")
    @GetMapping("/pings/{start}/{end}")
    @Operation(summary = "Get all the pings within a datetime range")
    public ResponseEntity<PingSessionExtract> getPingsByDateTimeRange(
            @Parameter(
                    description = "Start date and time of the range",
                    required = true)
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @Parameter(
                    description = "End date in the range",
                    required = true)
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        HttpStatus httpStatus = HttpStatus.OK;
        PingSessionExtract pings = connTestService.getPingsByDateTimeRange(start, end);

        if(pings.getPingLogs().isEmpty())
            httpStatus = HttpStatus.NO_CONTENT;

        addHATEOASLinksForPingLogsReturningEndpoints(pings);

        return new ResponseEntity<>(pings, httpStatus);
    }

    /**
     * Get all the pings filtered by IP
     * @param ipAddress IP address used to filter the results
     * @return If successful it returns a list of pings along with the total count
     */
    @ApiResponse(responseCode = "200", description = "a list of pings along with the total count.")
    @GetMapping("/pings/{ipAddress}")
    @Operation(summary = "Get all the pings filtered by IP")
    public ResponseEntity<PingSessionExtract> getPingsByIp(
            @Parameter(
                    description = "IP address used to filter the results",
                    required = true)
            @PathVariable String ipAddress) {
        HttpStatus httpStatus = HttpStatus.OK;
        PingSessionExtract pings = connTestService.getPingsByIp(ipAddress);

        if(pings.getPingLogs().isEmpty())
            httpStatus = HttpStatus.NO_CONTENT;

        addHATEOASLinksForPingLogsReturningEndpoints(pings);

        return new ResponseEntity<>(pings, httpStatus);
    }

    /**
     * Get all the pings within a datetime range filtered by IP
     * @param ipAddress IP address used to filter the results
     * @param start Start date and time of the range
     * @param end End date in the range
     * @return If successful it returns a list of pings along with the total count
     */
    @ApiResponse(responseCode = "200", description = "a list of pings along with the total count.")
    @GetMapping("/pings/{ipAddress}/{start}/{end}")
    @Operation(summary = "Get all the pings within a datetime range filtered by IP")
    public ResponseEntity<PingSessionExtract> getPingsByDateTimeRangeByIp(
            @Parameter(
                    description = "IP address used to filter the results",
                    required = true)
            @PathVariable String ipAddress,
            @Parameter(
                    description = "Start date and time of the range",
                    required = true)
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @Parameter(
                    description = "End date in the range",
                    required = true)
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        HttpStatus httpStatus = HttpStatus.OK;
        PingSessionExtract pings = connTestService.getPingsByDateTimeRangeByIp(start, end, ipAddress);

        if(pings.getPingLogs().isEmpty())
            httpStatus = HttpStatus.NO_CONTENT;

        addHATEOASLinksForPingLogsReturningEndpoints(pings);

        return new ResponseEntity<>(pings, httpStatus);
    }

    /**
     * Get the average of lost pings within a list of pings filtered by IP
     * @param ipAddress IP address used to filter the results
     * @return If successful it returns a number representing the average of lost pings
     */
    @ApiResponse(responseCode = "200", description = "a number representing the average of lost pings.")
    @GetMapping(value = "/pings/{ipAddress}/avg", produces = {"application/json"})
    @Operation(summary = "Get the average of lost pings within a list of pings filtered by IP")
    public ResponseEntity<AverageResponse> getPingsLostAvgByIp(
            @Parameter(
                    description = "IP address used to filter the results",
                    required = true)
            @PathVariable String ipAddress) {

        AverageResponse averageResult = AverageResponse.builder()
                .ipAddress(ipAddress)
                .average(connTestService.getPingsLostAvgByIp(ipAddress))
                .build();

        averageResult.add(
                linkTo(methodOn(ConnTestController.class).getPingsByIp(ipAddress))
                        .withRel(HATEOASLinkRelValueTemplates.GET_BY_IP.formatted(ipAddress))
        );

        return new ResponseEntity<>(
                averageResult,
                HttpStatus.OK
        );
    }

    /**
     * Get the average of lost pings within a datetime range filtered by IP
     * @param ipAddress IP address used to filter the results
     * @param start Start date and time of the range
     * @param end End date in the range
     * @return If successful it returns a number representing the average of lost pings
     */
    @ApiResponse(responseCode = "200", description = "a number representing the average of lost pings.")
    @GetMapping(value = "/pings/{ipAddress}/avg/{start}/{end}", produces = {"application/json"})
    @Operation(summary = "Get the average of lost pings within a datetime range filtered by IP")
    public ResponseEntity<AverageResponse> getPingsLostAvgByDateTimeRangeByIp(
            @Parameter(
                    description = "IP address used to filter the results",
                    required = true)
            @PathVariable String ipAddress,
            @Parameter(
                    description = "Start date and time of the range",
                    required = true)
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @Parameter(
                    description = "End date in the range",
                    required = true)
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {

        AverageResponse averageResult = AverageResponse.builder()
                .ipAddress(ipAddress)
                .average(connTestService.getPingsLostAvgByDateTimeRangeByIp(start, end, ipAddress))
                .build();

        averageResult.add(
                linkTo(methodOn(ConnTestController.class).getPingsByDateTimeRangeByIp(ipAddress, start, end))
                        .withRel(HATEOASLinkRelValueTemplates.GET_30_MINS_NEIGHBORHOOD_BY_IP.formatted(ipAddress))
        );

        return new ResponseEntity<>(
                averageResult,
                HttpStatus.OK
        );
    }

    private static void addHATEOASLinksForPingLogsReturningEndpoints(PingSessionExtract pings) {
        pings.getPingLogs().forEach(ping -> {
            LocalDateTime floor = ping.getDateTime().minusMinutes(30);
            LocalDateTime roof = ping.getDateTime().plusMinutes(30);
            String ipAddress = ping.getIpAddress();

            ping.add(
                    linkTo(methodOn(ConnTestController.class).getPingsByIp(ipAddress))
                            .withRel(HATEOASLinkRelValueTemplates.GET_BY_IP.formatted(ipAddress)),
                    linkTo(methodOn(ConnTestController.class).getPingsByDateTimeRange(floor, roof))
                            .withRel(HATEOASLinkRelValueTemplates.GET_30_MINS_NEIGHBORHOOD),
                    linkTo(methodOn(ConnTestController.class).getPingsByDateTimeRangeByIp(ipAddress, floor, roof))
                            .withRel(HATEOASLinkRelValueTemplates.GET_30_MINS_NEIGHBORHOOD_BY_IP.formatted(ipAddress)),
                    linkTo(methodOn(ConnTestController.class).getPingsLostAvgByIp(ping.getIpAddress()))
                            .withRel(HATEOASLinkRelValueTemplates.GET_LOST_AVG_BY_IP.formatted(ipAddress)),
                    linkTo(methodOn(ConnTestController.class).getPingsLostAvgByDateTimeRangeByIp(ipAddress, floor, roof))
                            .withRel(HATEOASLinkRelValueTemplates.GET_30_MINS_NEIGHBORHOOD_LOST_AVG_BY_IP.formatted(ipAddress))
            );
        });
    }
}
