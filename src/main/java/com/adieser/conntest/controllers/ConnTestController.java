package com.adieser.conntest.controllers;

import com.adieser.conntest.controllers.responses.AverageResponse;
import com.adieser.conntest.controllers.responses.PingLogFile;
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

@RestController
public class ConnTestController {

    private final ConnTestService connTestService;

    public ConnTestController(ConnTestServiceImpl connTestService) {
        this.connTestService = connTestService;
    }

    @PostMapping("/test-local-isp-cloud")
    public void testLocalIspCloud() {
        connTestService.testLocalISPInternet();
    }

    @PostMapping("/stop-tests")
    public void stopTests() {
        connTestService.stopTests();
    }

    @GetMapping("/pings")
    public ResponseEntity<List<PingLogFile>> getPings() {
        return new ResponseEntity<>(connTestService.getPings(), HttpStatus.OK);
    }

    @GetMapping("/pings/{ipAddress}")
    public ResponseEntity<List<PingLogFile>> getPingsByIp(@PathVariable String ipAddress) {
        return new ResponseEntity<>(connTestService.getPingsByIp(ipAddress), HttpStatus.OK);
    }

    @GetMapping("/pings/{ipAddress}/{start}/{end}")
    public ResponseEntity<List<PingLogFile>> getPingsByDateTimeRangeByIp(
            @PathVariable String ipAddress,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {

        return new ResponseEntity<>(connTestService.getPingsByDateTimeRangeByIp(start, end, ipAddress), HttpStatus.OK);
    }

    @GetMapping("/pings/{ipAddress}/avg")
    public ResponseEntity<AverageResponse> getPingsLostAvgByIp(@PathVariable String ipAddress) {

        return new ResponseEntity<>(
                new AverageResponse(
                        connTestService.getPingsLostAvgByIp(ipAddress)
                ),
                HttpStatus.OK
        );
    }

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
