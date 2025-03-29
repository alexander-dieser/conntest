package com.adieser.conntest.service;

import com.adieser.conntest.controllers.responses.PingSessionExtract;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for testing connections in parallel based on a ping tool
 */
public interface ConnTestService {
    /**
     * Trigger a ping session for 3 IP addresses in parallel: local gateway, ISP and Cloud
     */
    void testLocalISPInternet();

    /**
     * Trigger a 3 custom Ip addresses in parallel
     * @param ipAddresses ip addresses to test connection to
     */
    void testCustomIps(List<String> ipAddresses);

    /**
     * Stop all the threads running ping sessions
     */
    void stopTests();

    /**
     * Retrieve all the ping logs
     * @return List of ping results and metadata {@link PingSessionExtract}
     */
    PingSessionExtract getPings() throws IOException;

    /**
     * Retrieve all the ping logs within a datetime range
     * @param start start date and time of the range
     * @param end end date in the range
     * @return List of ping results and metadata {@link PingSessionExtract}
     */
    PingSessionExtract getPingsByDateTimeRange(LocalDateTime start, LocalDateTime end) throws IOException;

    /**
     * Retrieve all the ping logs filtered by IP
     * @param ipAddress IP address use to filter the results
     * @return List of ping results and metadata {@link PingSessionExtract}
     */
    PingSessionExtract getPingsByIp(String ipAddress) throws IOException;

    /**
     * Retrieve all the ping logs within a datetime range filtered by IP
     * @param start start date and time of the range
     * @param end end date in the range
     * @param ipAddress IP address use to filter the results
     * @return List of ping results and metadata {@link PingSessionExtract}
     */
    PingSessionExtract getPingsByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) throws IOException;

    /**
     * Retrieve all the lost ping logs
     * @param ipAddress IP address use to filter the results
     * @return List of ping results and metadata {@link PingSessionExtract}
     */
    PingSessionExtract getLostPingsByIp(String ipAddress) throws IOException;

    /**
     * Retrieve all the lost ping logs within a datetime range
     * @param start start date and time of the range
     * @param end end date in the range
     * @param ipAddress IP address use to filter the results
     * @return List of ping results and metadata {@link PingSessionExtract}
     */
    PingSessionExtract getLostPingsByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) throws IOException;

    /**
     * Get the average of lost pings within a list of pings filtered by IP
     * @param ipAddress IP address use to filter the results
     * @return average of pings lost
     */
    BigDecimal getPingsLostAvgByIp(String ipAddress) throws IOException;

    /**
     * Get the average of lost pings within a list of pings within a datetime range filtered by IP
     * @param start start date and time of the range
     * @param end end date in the range
     * @param ipAddress IP address use to filter the results
     * @return average of pings lost
     */
    BigDecimal getPingsLostAvgByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) throws IOException;

    /**
     * Get the IP addresses from the active tests
     * @return list of IP addresses
     */
    List<String> getIpAddressesFromActiveTests();

    /**
     * Remove all the entries from the current pinglog file
     */
    void clearPingLogFile() throws InterruptedException;

    /**
     * Retrieve the lowest latency pinglog and the highest latency pinglog
     * @param ipAddress IP address use to filter the results
     * @return List of ping results and metadata {@link PingSessionExtract}
     */
    PingSessionExtract getMaxMinPingLog(String ipAddress) throws IOException;

    /**
     * Retrieve the lowest latency pinglog and the highest latency pinglog within a datetime range
     * @param start start date and time of the range
     * @param end end date in the range
     * @param ipAddress IP address use to filter the results
     * @return List of ping results and metadata {@link PingSessionExtract}
     */
    PingSessionExtract getMaxMinPingLogByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) throws IOException;

    /**
     * Get the average latency of all the ping logs filtered by IP
     * @param ipAddress IP address use to filter the results
     * @return average latency of all the ping logs
     */
    BigDecimal getAvgLatencyByIp(String ipAddress) throws IOException;

    /**
     * Get the average latency of all the ping logs within a datetime range filtered by IP
     * @param start start date and time of the range
     * @param end end date in the range
     * @param ipAddress IP address use to filter the results
     * @return average latency of all the ping logs
     */
    BigDecimal getAvgLatencyByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) throws IOException;

    /**
     * Change the datasource where the pinglogs are located
     * @param newDatasource new datasource to be used
     */
    void changeDataSource(Object newDatasource) throws IOException;
}
