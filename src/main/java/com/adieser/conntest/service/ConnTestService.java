package com.adieser.conntest.service;

import com.adieser.conntest.controllers.responses.PingLogFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalDouble;

public interface ConnTestService {
    void testLocalISPInternet(List<String> ipAddresses);
    void stopTests();
    List<PingLogFile> getPings();
    List<PingLogFile> getPingsByIp(String ipAddress);
    List<PingLogFile> getPingsByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress);
    BigDecimal getPingsLostAvgByIp(String ipAddress);
    BigDecimal getPingsLostAvgByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress);

}
