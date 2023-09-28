package com.adieser.conntest.models;

import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

/**
 * <pre>
 * ConnTest implementation where pings are stored in JUL log files.
 * Logs are in CSV format for easing the reporting
 * The following log levels represent:
 * - INFO: normal ping
 * - WARNING: unreachable ping
 * - FINE: average of lost pings within the last 100 pings
 * - SEVERE: Indicates an exception
 * </pre>
 * <pre>
 * Format: timestamp,loglevel,ipAddress,time/avg
 *
 * Ex:
 * 2023-09-24 17:33:45,INFO,8.8.8.8,13
 * 2023-09-24 17:33:45,FINE,8.8.8.8,0
 * 2023-09-24 17:48:48,WARNING,8.8.8.8,-1
 * </pre>
 */
@SuppressWarnings("BusyWait")
public class ConnTestLogFileImpl extends ConnTest {


    private final PingLogRepository pingLogRepository;

    public ConnTestLogFileImpl(ExecutorService threadPoolExecutor, String ipAddress, Logger logger,
                               PingLogRepository pingLogRepository) {
        super(threadPoolExecutor, ipAddress, logger);
        this.pingLogRepository = pingLogRepository;
    }

    @Override
    public void startPingSession() {
        threadPoolExecutor.execute(this::pingSession);
        logger.info("Ping session for IP {} started...", ipAddress);
    }

    @Override
    public void stopPingSession() {
        running = false;
        logger.info("Ping session for IP {} stopped.", ipAddress);
    }

    private void pingSession() {

        try {
            while(running) {
                long ping = ping();
                pingLogRepository.savePingLog(PingLog.builder()
                        .dateTime(LocalDateTime.now())
                        .ipAddress(ipAddress)
                        .pingTime(ping)
                        .build());

                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
