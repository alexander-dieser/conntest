package com.adieser.conntest.models;

import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

/**
 * <pre>
 * ConnTest implementation where pings are stored in JUL log files.
 * Logs are in CSV format for easing the reporting
 * </pre>
 * <pre>
 * Format: timestamp,ipAddress,time
 *
 * Ex:
 * 2023-09-27 01:17:26,192.168.1.1,0
 * 2023-09-27 01:17:26,131.100.65.1,-1
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
