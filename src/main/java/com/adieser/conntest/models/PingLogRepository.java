package com.adieser.conntest.models;

import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository to persist and retrieve pings
 */
@Repository
public interface PingLogRepository {

    /**
     * Persist a ping
     * @param data ping to be persisted
     */
    void savePingLog(PingLog data) throws InterruptedException;

    /**
     * Retrieve all pings by IP address
     * @param ipAddress IP address to filter the pings
     * @return List of pings
     */
    List<PingLog> findPingLogByIp(String ipAddress) throws IOException;

    /**
     * Retrieve all pings in the database
     * @return List of pings
     */
    List<PingLog> findAllPingLogs() throws IOException;

    /**
     * Retrieve all pings within a datetime range
     * @param start start date and time of the range
     * @param end end date in the range
     * @return List of pings
     */
    List<PingLog> findPingLogsByDateTimeRange(LocalDateTime start, LocalDateTime end) throws IOException;

    /**
     * Retrieve all pings within a datetime range filtered by IP address
     * @param start start date and time of the range
     * @param end end date in the range
     * @param ipAddress IP address to filter the pings
     * @return List of pings
     */
    List<PingLog> findPingLogsByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) throws IOException;

    /**
     * Calculate and retrieve the average of lost pings within a list of pings filtered by IP address
     * @param ipAddress IP address use to filter the results
     * @return average of lost pings
     */
    BigDecimal findLostPingLogsAvgByIP(String ipAddress) throws IOException;

    /** Calculate and retrieve the average of lost pings within a list of pings within a datetime range filtered
     * by IP address
     * @param start start date and time of the range
     * @param end end date in the range
     * @param ipAddress IP address use to filter the results
     * @return average of lost pings
     */
    BigDecimal findLostPingLogsAvgByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) throws IOException;

}
