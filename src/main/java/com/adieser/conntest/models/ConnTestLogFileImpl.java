package com.adieser.conntest.models;

import com.adieser.conntest.models.utils.JsonLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

@SuppressWarnings("BusyWait")
public class ConnTestLogFileImpl extends ConnTest {
    private final java.util.logging.Logger pingLogger;

    public ConnTestLogFileImpl(ExecutorService threadPoolExecutor, String ipAddress, Logger logger) {
        super(threadPoolExecutor, ipAddress, logger);
        this.pingLogger = java.util.logging.Logger.getLogger(ipAddress);

        setupLogger(ipAddress, pingLogger);
    }

    private static void setupLogger(String ipAddress, java.util.logging.Logger logger) {
        FileHandler fh;
        try {
            fh = new FileHandler("C:\\pingLogs\\pingTest"+ ipAddress +".log",10 * 1024 * 1024, 10 , true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.addHandler(fh);
        logger.setUseParentHandlers(false);
        SimpleFormatter formatter = new SimpleFormatter(){
            @Override
            public String format(LogRecord logRecord) {
                ObjectMapper objectMapper = new ObjectMapper();

                try {
                    return objectMapper.writeValueAsString(
                            JsonLog.builder()
                                    .dateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(logRecord.getMillis())))
                                    .logLevel(logRecord.getLevel().getName())
                                    .message(formatMessage(logRecord))
                                    .build()
                    );
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

//                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(logRecord.getMillis())) +
//                        " " +
//                        logRecord.getLevel() +
//                        ": " +
//                        formatMessage(logRecord) +
//                        "\n";
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
            ObjectMapper objectMapper = new ObjectMapper();

            while(running) {
//                ping().ifPresentOrElse(
//                        ping -> pingLogger.log(Level.INFO,
//                                objectMapper.writeValueAsString(ping)),
//                        () -> {
//                            pingLogger.log(Level.SEVERE,
//                                   "{" +
//                                    "\"ipAddress\":\"{0}\"," +
//                                    "\"time\":\"{1}ms\"" +
//                                    "\"reachable\":false" +
//                                    "}",
//                                    ipAddress);
//                            lost.getAndIncrement();
//                        }
//                );

                Ping ping = ping();
                String msg = objectMapper.writeValueAsString(ping);
                if(ping.isReachable())
                    pingLogger.log(Level.INFO, msg);
                else {
                    pingLogger.log(Level.SEVERE, msg);
                    lost++;
                }

                count++;
                if(count == CUTOFF) {
                    BigDecimal avg = getAvg(lost);
                    pingLogger.log(Level.INFO, "Avg lost package: {0}", avg);
                    count = 0;
                    lost = 0;
                }

                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            pingLogger.log(Level.SEVERE, "Ping error: {0}", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
