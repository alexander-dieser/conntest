package com.adieser.conntest.service;

import com.adieser.conntest.controllers.responses.PingSessionExtract;

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
     * Stop all the threads running ping sessions
     */
    void stopTests();

    /**
     * Retrieve all the ping logs
     * @return List of ping results and metadata {@link PingSessionExtract}
     */
    PingSessionExtract getPings();

    /**
     * Retrieve all the ping logs within a datetime range
     * @param start start date and time of the range
     * @param end end date in the range
     * @return List of ping results and metadata {@link PingSessionExtract}
     */
    PingSessionExtract getPingsByDateTimeRange(LocalDateTime start, LocalDateTime end);

    /**
     * Retrieve all the ping logs filtered by IP
     * @param ipAddress IP address use to filter the results
     * @return List of ping results and metadata {@link PingSessionExtract}
     */
    PingSessionExtract getPingsByIp(String ipAddress);

    /**
     * Retrieve all the ping logs within a datetime range filtered by IP
     * @param start start date and time of the range
     * @param end end date in the range
     * @param ipAddress IP address use to filter the results
     * @return List of ping results and metadata {@link PingSessionExtract}
     */
    PingSessionExtract getPingsByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress);

    /**
     * Get the average of lost pings within a list of pings filtered by IP
     * @param ipAddress IP address use to filter the results
     * @return average of pings lost
     */
    BigDecimal getPingsLostAvgByIp(String ipAddress);

    /**
     * Get the average of lost pings within a list of pings within a datetime range filtered by IP
     * @param start start date and time of the range
     * @param end end date in the range
     * @param ipAddress IP address use to filter the results
     * @return average of pings lost
     */
    BigDecimal getPingsLostAvgByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress);

    /**
     * Get the IP addresses from the active tests
     * @return list of IP addresses
     */
    List<String> getIpAddressesFromActiveTests();

}
