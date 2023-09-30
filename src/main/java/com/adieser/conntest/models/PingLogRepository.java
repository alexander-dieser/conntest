package com.adieser.conntest.models;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PingLogRepository {
    void savePingLog(PingLog data);
    List<PingLog> findPingLogByIp(String ipAddress);
    List<PingLog> findAllPingLogs();
    List<PingLog> findPingLogsByDateTimeRange(LocalDateTime start, LocalDateTime end);
    List<PingLog> findPingLogsByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress);
    BigDecimal findLostPingLogsAvgByIP(String ipAddress);
    BigDecimal findLostPingLogsAvgByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress);

}
