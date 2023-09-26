package com.adieser.conntest.controllers;

import com.adieser.conntest.controllers.responses.PingLogFile;
import com.adieser.conntest.service.ConnTestService;
import com.adieser.conntest.service.ConnTestServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
public class ConnTestController {

    private final ConnTestService connTestService;

    public ConnTestController(ConnTestServiceImpl connTestService) {
        this.connTestService = connTestService;
    }

    @GetMapping("/test-local-isp-cloud")
    public String testLocalIspCloud() {

        connTestService.testLocalISPInternet(
                Arrays.asList("192.168.1.1", "131.100.65.1", "8.8.8.8")
        );

        return "Connection tests running";
    }

    @GetMapping("/stop-tests")
    public String stopTests() {
        connTestService.stopTests();

        return "Connection tests stopped";
    }

    @GetMapping("/pings")
    public ResponseEntity<List<PingLogFile>> getPings() {
        return new ResponseEntity<>(connTestService.getPings(), HttpStatus.OK);
    }

    @GetMapping("/pings/avg")
    public ResponseEntity<List<PingLogFile>> getPingsLostAvg() {
        return new ResponseEntity<>(connTestService.getPingsLostAvg(), HttpStatus.OK);
    }
}
