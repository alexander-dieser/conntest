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

    /**
     * Clear all pings from the pinglog file
     */
    void clearPingLogFile() throws InterruptedException;

    /**
     * Retrieves a list of lost ping logs for a specific IP address.
     *
     * <p>This method queries the datasource for all ping logs associated with the given IP address
     * where the ping attempt was unsuccessful (i.e., the ping was "lost"). A lost ping typically
     * indicates that the target host did not respond within the expected time frame.</p>
     *
     * @param ipAddress The IP address to search for, in string format (e.g., "192.168.1.1").
     *                  This should be a valid IPv4 or IPv6 address.
     * @return A List of PingLog objects representing the lost pings for the specified IP address.
     *         The list will be empty if no lost pings are found for the given IP address.
     *
     * @see PingLog
     */
    List<PingLog> findLostPingsByIp(String ipAddress) throws IOException;

    /**
     * Retrieves a list of lost ping logs within a specific date and time range for a given IP address.
     *
     * @param start The start date and time of the range to search for lost pings.
     * @param end The end date and time of the range to search for lost pings.
     * @param ipAddress The IP address to search for, in string format (e.g., "192.168.1.1").
     *                  This should be a valid IPv4 or IPv6 address.
     * @return A List of PingLog objects representing the lost pings for the specified IP address
     *         and date and time range. The list will be empty if no lost pings are found for the
     *         given IP address within the specified date and time range.
     *
     * @see PingLog
     */
    List<PingLog> findLostPingsByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) throws IOException;
}
