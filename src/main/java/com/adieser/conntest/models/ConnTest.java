package com.adieser.conntest.models;

import com.adieser.conntest.models.utils.Reachable;
import lombok.Getter;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

/**
 * Handles the connection test to a specific IP address. It has the capability of triggering and stopping a
 * ping session.
 */
@SuppressWarnings("BusyWait")
public class ConnTest implements Pingable {
    public static final String PING_SESSION_INTERRUPTED_MSG = "Ping session was interrupted";
    private final ExecutorService threadPoolExecutor;
    protected boolean running = false;
    private final Logger logger;
    private final PingLogRepository pingLogRepository;
    private final Reachable pingUtils;

    @Getter
    private final String ipAddress;

    public ConnTest(ExecutorService threadPoolExecutor, String ipAddress, Logger logger,
                    PingLogRepository pingLogRepository, Reachable pingUtils) {
        this.threadPoolExecutor = threadPoolExecutor;
        this.ipAddress = ipAddress;
        this.logger = logger;
        this.pingLogRepository = pingLogRepository;
        this.pingUtils = pingUtils;
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

    /**
     * Ping task running in a loop only stoppable by changing the value of {@code running} variable to false.
     * Each ping is triggered once per second if the ping takes less than 1 second
     */
    void pingSession() {
        running = true;
        try {
            while(running) {
                long startTime = System.currentTimeMillis();

                PingLog ping = buildPingLog();
                long pingTime = ping();
                ping.setPingTime(pingTime);

                pingLogRepository.savePingLog(ping);
                long elapsedTime = System.currentTimeMillis() - startTime;

                long sleepTime = 1000 - elapsedTime;

                if (sleepTime > 0)
                    Thread.sleep(sleepTime); // sleep only if the ping took less than 1 second
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn(PING_SESSION_INTERRUPTED_MSG);
        }
    }

    PingLog buildPingLog() {
        return PingLog.builder()
                .dateTime(LocalDateTime.now())
                .ipAddress(ipAddress)
                .build();
    }

    /**
     * Check if a connection is reachable and save the time it takes to do it.
     * @return the time (milliseconds) it takes to check if an ip address is reachable.
     * If it's not reachable it returns -1
     */
    long ping(){
        long time = -1L;

        long currentTime = System.currentTimeMillis();
        if(pingUtils.isReachable(ipAddress))
            time = System.currentTimeMillis() - currentTime;

        return time;
    }
}
