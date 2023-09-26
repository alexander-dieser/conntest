package com.adieser.conntest.models;

import org.slf4j.Logger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

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

    private final java.util.logging.Logger pingLogger;
    private final String pingLogsPath;

    private static final String SEPARATOR = ",";

    public ConnTestLogFileImpl(ExecutorService threadPoolExecutor, String ipAddress, Logger logger, String pingLogsPath) {
        super(threadPoolExecutor, ipAddress, logger);
        this.pingLogger = java.util.logging.Logger.getLogger(ipAddress);
        this.pingLogsPath = pingLogsPath;
        pingLogger.setLevel(Level.FINE);
        setupLogger(ipAddress, pingLogger);
    }

    private void setupLogger(String ipAddress, java.util.logging.Logger logger) {
        FileHandler fh;
        try {
            fh = new FileHandler(pingLogsPath + "/pingTest"+ ipAddress +".log",10 * 1024 * 1024, 10 , false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.addHandler(fh);
        logger.setUseParentHandlers(false);
        SimpleFormatter formatter = new SimpleFormatter(){
            @Override
            public String format(LogRecord logRecord) {

                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(logRecord.getMillis())) +
                        SEPARATOR +
                        logRecord.getLevel() +
                        SEPARATOR +
                        formatMessage(logRecord) +
                        "\n";
            }
        };

        fh.setFormatter(formatter);
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
            int lost = 0;
            int count = 0;

            while(running) {
                Ping ping = ping();
                if(ping.isReachable())
                    pingLogger.log(Level.INFO, "{0},{1}", new Object[]{ping.getIpAddress(), ping.getTime()});
                else {
                    pingLogger.log(Level.WARNING, "{0},-1", ping.getIpAddress());
                    lost++;
                }

                count++;
                if(count== CUTOFF) {
                    pingLogger.log(Level.FINE, "{0},{1}", new Object[]{ping.getIpAddress(), getAvg(lost)});
                    count = 0;
                    lost = 0;
                }

                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            pingLogger.log(Level.SEVERE, "Thread error: {0}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
