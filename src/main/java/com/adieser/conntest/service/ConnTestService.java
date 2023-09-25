package com.adieser.conntest.service;

import com.adieser.conntest.controllers.responses.PingResponse;

import java.util.List;

public interface ConnTestService {
    void testLocalISPInternet(List<String> ipAddresses);
    void stopTests();
    List<PingResponse> getPings();
}
