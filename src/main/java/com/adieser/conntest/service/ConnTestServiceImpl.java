package com.adieser.conntest.service;

import com.adieser.conntest.controllers.responses.PingSessionResponseEntity;
import com.adieser.conntest.models.ConnTest;
import com.adieser.conntest.models.PingLog;
import com.adieser.conntest.models.PingLogRepository;
import com.adieser.conntest.models.Pingable;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ConnTestService implementation. It uses {@link ExecutorService} for multithreading.
 * To automatically gather the local and ISP IP addresses it leverages the OS traceroute-like command
 */
@Service
public class ConnTestServiceImpl implements ConnTestService {

    public static final String CLOUD_IP = "8.8.8.8";
    private final Logger logger;
    private List<ConnTest> tests = new ArrayList<>();
    private final ExecutorService threadPoolExecutor;

    /**
     * Tracert IP regex (Windows) for extracting the IP addresses from tracert output
     */
    private static final String REGEX_PATTERN_TRACERT_WINDOWS = "(?<!\\[)(\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b)(?!])";

    private final PingLogRepository pingLogRepository;

    public ConnTestServiceImpl(ExecutorService threadPoolExecutor, Logger logger,
                               @Qualifier("csvPingLogRepository") PingLogRepository pingLogRepository) {
        this.logger = logger;
        this.threadPoolExecutor = threadPoolExecutor;
        this.pingLogRepository = pingLogRepository;
    }

    @Override
    public void testLocalISPInternet(){

        List<String> ipAddresses = getLocalAndISPIpAddresses();
        ipAddresses.add(CLOUD_IP);

        tests = ipAddresses.stream()
                .map(s -> new ConnTest(threadPoolExecutor, s, logger, pingLogRepository))
                .toList();

        tests.forEach(Pingable::startPingSession);
    }

    @Override
    public void stopTests(){
        if(tests.isEmpty())
            logger.warn("No tests to stop");
        else {
            tests.forEach(Pingable::stopPingSession);
            threadPoolExecutor.shutdown();
        }
    }

    @Override
    public List<PingSessionResponseEntity> getPings() {
        List<PingSessionResponseEntity> pingResponses = new ArrayList<>();

        List<PingLog> pingLogs = pingLogRepository.findAllPingLogs();

        createResponse(pingResponses, pingLogs);

        return pingResponses;
    }

    @Override
    public List<PingSessionResponseEntity> getPingsByDateTimeRange(LocalDateTime start, LocalDateTime end) {
        List<PingSessionResponseEntity> pingResponses = new ArrayList<>();

        List<PingLog> pingLogs = pingLogRepository.findPingLogsByDateTimeRange(start, end);

        createResponse(pingResponses, pingLogs);

        return pingResponses;
    }

    @Override
    public List<PingSessionResponseEntity> getPingsByIp(String ipAddress) {
        List<PingSessionResponseEntity> pingResponses = new ArrayList<>();

        List<PingLog> pingLogs = pingLogRepository.findPingLogByIp(ipAddress);

        createResponse(pingResponses, pingLogs);

        return pingResponses;
    }

    @Override
    public List<PingSessionResponseEntity> getPingsByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) {
        List<PingSessionResponseEntity> pingResponses = new ArrayList<>();

        List<PingLog> pingLogs = pingLogRepository.findPingLogsByDateTimeRangeByIp(start, end, ipAddress);

        createResponse(pingResponses, pingLogs);

        return pingResponses;
    }

    @Override
    public BigDecimal getPingsLostAvgByIp(String ipAddress) {
        return pingLogRepository.findLostPingLogsAvgByIP(ipAddress);
    }

    @Override
    public BigDecimal getPingsLostAvgByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) {
        return pingLogRepository.findLostPingLogsAvgByDateTimeRangeByIp(start, end, ipAddress);
    }

    /**
     * Create the ping session results formatted as {@link PingSessionResponseEntity}
     * @param pingResponses List carrying the {@link PingSessionResponseEntity}
     * @param pingLogs List of ping sessions results
     */
    private static void createResponse(List<PingSessionResponseEntity> pingResponses, List<PingLog> pingLogs) {
        pingResponses.add(
                PingSessionResponseEntity.builder()
                        .pingLogs(pingLogs)
                        .amountOfPings(pingLogs.size())
                        .build()
        );
    }

    /**
     * Gather the necessary IP addresses
     * This method leverage the traceroute OS tool
     *
     * @return List of IP addresses, containing the first (local gateway) and the second hop (ISP)
     */
    private List<String> getLocalAndISPIpAddresses() {
        List<String> ipAddresses = new ArrayList<>();

        try {
            Process tracertProcess = Runtime.getRuntime().exec("tracert -h 2 8.8.8.8");

            BufferedReader reader = new BufferedReader(new InputStreamReader(tracertProcess.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = Pattern.compile(REGEX_PATTERN_TRACERT_WINDOWS).matcher(line);
                if (matcher.find())
                    ipAddresses.add(matcher.group());
            }

        } catch (IOException e) {
            logger.error("Tracerout error", e);
        }

        return ipAddresses;
    }
}
