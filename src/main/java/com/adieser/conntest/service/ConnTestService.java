package com.adieser.conntest.service;

import com.adieser.conntest.controllers.responses.PingLogFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ConnTestService {
    void testLocalISPInternet();
    void stopTests();
    List<PingLogFile> getPings();
    List<PingLogFile> getPingsByIp(String ipAddress);
    List<PingLogFile> getPingsByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress);
    BigDecimal getPingsLostAvgByIp(String ipAddress);
    BigDecimal getPingsLostAvgByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress);

}
