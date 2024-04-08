package com.adieser.conntest.models;

import lombok.Getter;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

/**
 * Handles the connection test to a specific IP address. It has the capability of triggering and stopping a
 * ping session.
 */
@SuppressWarnings("BusyWait")
public class ConnTest implements Pingable {
    protected static final int TIMEOUT = 3000;
    public static final String PING_SESSION_INTERRUPTED_MSG = "Ping session was interrupted";
    private final ExecutorService threadPoolExecutor;
    protected boolean running = false;
    private final Logger logger;
    private final PingLogRepository pingLogRepository;

    @Getter
    private final String ipAddress;

    public ConnTest(ExecutorService threadPoolExecutor, String ipAddress, Logger logger,
                    PingLogRepository pingLogRepository) {
        this.threadPoolExecutor = threadPoolExecutor;
        this.ipAddress = ipAddress;
        this.logger = logger;
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

    /**
     * Ping task running in a loop only stoppable by changing the value of {@code running} variable to false.
     * Each ping is triggered once per second
     */
    void pingSession() {
        running = true;
        try {
            while(running) {
                long ping = ping();
                pingLogRepository.savePingLog(buildPingLog(ping));

                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn(PING_SESSION_INTERRUPTED_MSG);
        }
    }

    PingLog buildPingLog(long ping) {
        return PingLog.builder()
                .dateTime(LocalDateTime.now())
                .ipAddress(ipAddress)
                .pingTime(ping)
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
        if(isReachable(ipAddress))
            time = System.currentTimeMillis() - currentTime;

        return time;
    }

    /**
     * Check if an ip address is reachable. It tries to reach the ip address during {@code TIMEOUT} ms, if it is not
     * possible it means the ip address is not reachable.
     * @param ip ip address to check connectivity
     * @return true if the ip address is reachable. False otherwise
     */
    boolean isReachable(String ip) {
        InetAddress inet;
        boolean ping=false;
        try {
            inet = getInet(ip);
            ping = inet.isReachable(TIMEOUT);
        } catch (IOException e) {
            logger.error("IP address {} is not reachable", ipAddress, e);
        }
        return ping;
    }

    InetAddress getInet(String ip) throws UnknownHostException {
        return InetAddress.getByName(ip);
    }
}
