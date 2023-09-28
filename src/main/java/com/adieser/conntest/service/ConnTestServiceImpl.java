package com.adieser.conntest.service;

import com.adieser.conntest.controllers.responses.PingLogFile;
import com.adieser.conntest.models.ConnTestLogFileImpl;
import com.adieser.conntest.models.PingLog;
import com.adieser.conntest.models.PingLogRepository;
import com.adieser.conntest.models.Pingable;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
public class ConnTestServiceImpl implements ConnTestService {

    private final Logger logger;
    private List<ConnTestLogFileImpl> tests = new ArrayList<>();
    private final ExecutorService threadPoolExecutor;

    private final PingLogRepository pingLogRepository;

    public ConnTestServiceImpl(ExecutorService threadPoolExecutor, Logger logger,
                               @Qualifier("csvPingLogRepository") PingLogRepository pingLogRepository) {
        this.logger = logger;
        this.threadPoolExecutor = threadPoolExecutor;
        this.pingLogRepository = pingLogRepository;
    }

    @Override
    public void testLocalISPInternet(List<String> ipAddresses){
        tests = ipAddresses.stream()
                .map(s -> new ConnTestLogFileImpl(threadPoolExecutor, s, logger, pingLogRepository))
                .toList();

        tests.forEach(Pingable::startPingSession);
    }

    public void stopTests(){
        if(tests.isEmpty())
            logger.warn("No tests to stop");
        else {
            tests.forEach(Pingable::stopPingSession);
            threadPoolExecutor.shutdown();
        }
    }

    @Override
    public List<PingLogFile> getPings() {
        List<PingLogFile> pingResponses = new ArrayList<>();

        List<PingLog> pingLogs = pingLogRepository.findAllPingLogs();

        pingResponses.add(
                PingLogFile.builder()
                        .pingLogs(pingLogs)
                        .amountOfPings(pingLogs.size())
                        .build()
        );

        return pingResponses;
    }

    @Override
    public List<PingLogFile> getPingsByIp(String ipAddress) {
        List<PingLogFile> pingResponses = new ArrayList<>();

        List<PingLog> pingLogs = pingLogRepository.findPingLogByIp(ipAddress);

        pingResponses.add(
                PingLogFile.builder()
                        .pingLogs(pingLogs)
                        .amountOfPings(pingLogs.size())
                        .build()
        );

        return pingResponses;
    }

    @Override
    public List<PingLogFile> getPingsByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) {
        List<PingLogFile> pingResponses = new ArrayList<>();

        List<PingLog> pingLogs = pingLogRepository.findPingLogsByDateTimeRangeByIp(start, end, ipAddress);

        pingResponses.add(
                PingLogFile.builder()
                        .pingLogs(pingLogs)
                        .amountOfPings(pingLogs.size())
                        .build()
        );

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
}
