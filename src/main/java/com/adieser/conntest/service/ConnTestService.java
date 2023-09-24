package com.adieser.conntest.service;

import java.util.List;

public interface ConnTestService {
    void testLocalISPInternet(List<String> ipAddresses);
    void stopTests();
}
