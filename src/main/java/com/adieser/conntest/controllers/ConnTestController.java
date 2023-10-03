package com.adieser.conntest.controllers;

import com.adieser.conntest.controllers.responses.AverageResponse;
import com.adieser.conntest.controllers.responses.PingSessionResponseEntity;
import com.adieser.conntest.service.ConnTestService;
import com.adieser.conntest.service.ConnTestServiceImpl;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller to interact with pings
 */
@RestController
public class ConnTestController {

    private final ConnTestService connTestService;

    public ConnTestController(ConnTestServiceImpl connTestService) {
        this.connTestService = connTestService;
    }

    /**
     * Trigger 3 ping sessions to test connection against local gateway, the next hop, and the cloud (Google's 8.8.8.8)
     */
    @PostMapping("/test-local-isp-cloud")
    public void testLocalIspCloud() {
        connTestService.testLocalISPInternet();
    }

    /**
     * stop all the test sessions
     */
    @PostMapping("/stop-tests")
    public void stopTests() {
        connTestService.stopTests();
    }

    /**
     * Get all pings
     * @return If successful it returns a list of pings along with the total count
     */
    @GetMapping("/pings")
    public ResponseEntity<List<PingSessionResponseEntity>> getPings() {
        return new ResponseEntity<>(connTestService.getPings(), HttpStatus.OK);
    }

    /**
     * Get all the pings within a datetime range
     * @param start Start date and time of the range
     * @param end End date in the range
     * @return If successful it returns a list of pings along with the total count
     */
    @GetMapping("/pings/{start}/{end}")
    public ResponseEntity<List<PingSessionResponseEntity>> getPingsByDateTimeRange(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        return new ResponseEntity<>(connTestService.getPingsByDateTimeRange(start, end), HttpStatus.OK);
    }

    /**
     * Get all the pings filtered by IP
     * @param ipAddress IP address used to filter the results
     * @return If successful it returns a list of pings along with the total count
     */
    @GetMapping("/pings/{ipAddress}")
    public ResponseEntity<List<PingSessionResponseEntity>> getPingsByIp(@PathVariable String ipAddress) {
        return new ResponseEntity<>(connTestService.getPingsByIp(ipAddress), HttpStatus.OK);
    }

    /**
     * Get all the pings within a datetime range filtered by IP
     * @param ipAddress IP address used to filter the results
     * @param start Start date and time of the range
     * @param end End date in the range
     * @return If successful it returns a list of pings along with the total count
     */
    @GetMapping("/pings/{ipAddress}/{start}/{end}")
    public ResponseEntity<List<PingSessionResponseEntity>> getPingsByDateTimeRangeByIp(
            @PathVariable String ipAddress,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {

        return new ResponseEntity<>(connTestService.getPingsByDateTimeRangeByIp(start, end, ipAddress), HttpStatus.OK);
    }

    /**
     * Get the average of lost pings within a list of pings filtered by IP
     * @param ipAddress IP address used to filter the results
     * @return If successful it returns a number representing the average of lost pings
     */
    @GetMapping("/pings/{ipAddress}/avg")
    public ResponseEntity<AverageResponse> getPingsLostAvgByIp(@PathVariable String ipAddress) {

        return new ResponseEntity<>(
                new AverageResponse(
                        connTestService.getPingsLostAvgByIp(ipAddress)
                ),
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
    @GetMapping("/pings/{ipAddress}/avg/{start}/{end}")
    public ResponseEntity<AverageResponse> getPingsLostAvgByDateTimeRangeByIp(
            @PathVariable String ipAddress,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {

        return new ResponseEntity<>(
                new AverageResponse(
                        connTestService.getPingsLostAvgByDateTimeRangeByIp(start, end, ipAddress)
                ),
                HttpStatus.OK
        );
    }
}
