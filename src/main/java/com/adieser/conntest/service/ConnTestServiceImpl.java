package com.adieser.conntest.service;

import com.adieser.conntest.controllers.responses.PingSessionExtract;
import com.adieser.conntest.models.ConnTest;
import com.adieser.conntest.models.PingLog;
import com.adieser.conntest.models.PingLogRepository;
import com.adieser.conntest.models.Pingable;
import com.adieser.conntest.models.utils.Reachable;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    List<ConnTest> tests = new ArrayList<>();
    private final ExecutorService threadPoolExecutor;

    private final TracertProvider tracertProvider;

    /**
     * Tracert IP regex (Windows) for extracting the IP addresses from tracert output
     */
    private static final String REGEX_PATTERN_TRACERT_WINDOWS = "(?<!\\[)(\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b)(?!])\\b(?<!8\\.8\\.8\\.8)";

    private final PingLogRepository pingLogRepository;

    private final Reachable pingUtils;

    public ConnTestServiceImpl(ExecutorService threadPoolExecutor, Logger logger,
                               TracertProvider tracertProvider,
                               PingLogRepository pingLogRepository,
                               Reachable pingUtils) {
        this.logger = logger;
        this.threadPoolExecutor = threadPoolExecutor;
        this.tracertProvider = tracertProvider;
        this.pingLogRepository = pingLogRepository;
        this.pingUtils = pingUtils;
    }

    @Override
    public void testLocalISPInternet(){
            List<String> ipAddresses = getLocalAndISPIpAddresses();
            ipAddresses.add(CLOUD_IP);
            testCustomIps(ipAddresses);
    }

    @Override
    public void testCustomIps(List<String> ipAddresses) {
        if(tests.isEmpty()){
            tests = getConnTestsFromIpAddresses(ipAddresses);
            tests.forEach(Pingable::startPingSession);
        }else
            tests.forEach(test -> logger.warn("Tests are already running (IP {})", test.getIpAddress()));
    }

    @Override
    public void stopTests(){
        if(tests.isEmpty())
            logger.warn("No tests to stop");
        else {
            tests.forEach(Pingable::stopPingSession);
            tests = new ArrayList<>();
        }
    }

    @Override
    public PingSessionExtract getPings() throws IOException {
        List<PingLog> pingLogs = pingLogRepository.findAllPingLogs();

        return createBasicResponse(pingLogs);
    }

    @Override
    public PingSessionExtract getPingsByDateTimeRange(LocalDateTime start, LocalDateTime end) throws IOException {
        List<PingLog> pingLogs = pingLogRepository.findPingLogsByDateTimeRange(start, end);

        PingSessionExtract response = createBasicResponse(pingLogs);
        response.setStartDate(start);
        response.setEndDate(end);

        return response;
    }

    @Override
    public PingSessionExtract getPingsByIp(String ipAddress) throws IOException {
        List<PingLog> pingLogs = pingLogRepository.findPingLogByIp(ipAddress);

        PingSessionExtract response = createBasicResponse(pingLogs);
        response.setIpAddress(ipAddress);

        return response;
    }

    @Override
    public PingSessionExtract getPingsByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) throws IOException {
        List<PingLog> pingLogs = pingLogRepository.findPingLogsByDateTimeRangeByIp(start, end, ipAddress);

        PingSessionExtract response = createBasicResponse(pingLogs);
        response.setStartDate(start);
        response.setEndDate(end);
        response.setIpAddress(ipAddress);

        return response;
    }

    @Override
    public PingSessionExtract getLostPingsByIp(String ipAddress) throws IOException {
        List<PingLog> pingLogs = pingLogRepository.findLostPingsByIp(ipAddress);

        PingSessionExtract response = createBasicResponse(pingLogs);
        response.setIpAddress(ipAddress);

        return response;
    }

    @Override
    public PingSessionExtract getLostPingsByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) throws IOException {
        List<PingLog> pingLogs = pingLogRepository.findLostPingsByDateTimeRangeByIp(start, end, ipAddress);

        PingSessionExtract response = createBasicResponse(pingLogs);
        response.setStartDate(start);
        response.setEndDate(end);
        response.setIpAddress(ipAddress);

        return response;
    }

    @Override
    public BigDecimal getPingsLostAvgByIp(String ipAddress) throws IOException {
        return pingLogRepository.findLostPingLogsAvgByIP(ipAddress);
    }

    @Override
    public BigDecimal getPingsLostAvgByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) throws IOException {
        return pingLogRepository.findLostPingLogsAvgByDateTimeRangeByIp(start, end, ipAddress);
    }

    @Override
    public BigDecimal getAvgLatencyByIp(String ipAddress) throws IOException {
        return pingLogRepository.findAvgLatencyByIp(ipAddress);
    }

    @Override
    public List<String> getIpAddressesFromActiveTests(){
        return tests.stream()
                .map(ConnTest::getIpAddress)
                .toList();
    }

    @Override
    public void clearPingLogFile() throws InterruptedException {
        pingLogRepository.clearPingLogFile();
    }

    @Override
    public PingSessionExtract getMaxMinPingLog(String ipAddress) throws IOException {
        PingSessionExtract response = createBasicResponse(pingLogRepository.findMaxMinPingLog(ipAddress));
        response.setIpAddress(ipAddress);

        return response;
    }

    /**
     * Create the ping session results formatted as {@link PingSessionExtract}
     * @param pingLogs List of ping sessions results
     */
    PingSessionExtract createBasicResponse(List<PingLog> pingLogs) {
        return PingSessionExtract.builder()
                .pingLogs(pingLogs)
                .amountOfPings(pingLogs.size())
                .build();
    }

    /**
     * Gather the necessary IP addresses
     * This method leverage the traceroute OS tool
     *
     * @return List of IP addresses, containing the first (local gateway) and the second hop (ISP)
     */
    List<String> getLocalAndISPIpAddresses() {
        List<String> ipAddresses = new ArrayList<>();

        try (BufferedReader reader = executeTracert()
                .orElseThrow(() -> new IOException("Error executing tracert in the OS"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = Pattern.compile(REGEX_PATTERN_TRACERT_WINDOWS).matcher(line);
                if (matcher.find())
                    ipAddresses.add(matcher.group());
            }
        }catch (IOException e) {
            logger.error("Traceroute error", e);
        }

        return ipAddresses;
    }

    /**
     * Get a list of ConnTests objects created from a list of IP addresses
     * @param ipAddresses IP address to create the ConnTest
     * @return A list of ConnTests
     */
    List<ConnTest> getConnTestsFromIpAddresses(List<String> ipAddresses) {
        return ipAddresses.stream()
                .map(s -> new ConnTest(threadPoolExecutor, s, logger, pingLogRepository, pingUtils))
                .toList();
    }

    /**
     * Execute OS tracert command and return the buffer reader containing the command output
     * @return the BufferReader containing the command output
     * @throws IOException throws by java.lang.Runtime exec command
     */
    Optional<BufferedReader> executeTracert() throws IOException {
        return tracertProvider.executeTracert();
    }

}
