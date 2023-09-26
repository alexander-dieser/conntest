package com.adieser.conntest.service;

import com.adieser.conntest.controllers.responses.PingLog;
import com.adieser.conntest.controllers.responses.PingLogFile;

import java.util.List;

public interface ConnTestService {
    void testLocalISPInternet(List<String> ipAddresses);
    void stopTests();
    List<PingLogFile> getPings();
    List<PingLogFile> getPingsLostAvg();
}
