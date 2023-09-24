package com.adieser.conntest.models;

import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class ConnTestFileDbImpl extends ConnTest {
    ConnTestFileDbImpl(ExecutorService threadPoolExecutor, String ipAddress, Logger logger) {
        super(threadPoolExecutor, ipAddress, logger);
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

//        try {
//            AtomicInteger lost= new AtomicInteger();
//            int count=0;
//
//            while(running) {
//                ping().ifPresentOrElse(
//                        time -> ,
//                        () -> {
//
//                            lost.getAndIncrement();
//                        }
//                );
//
//                count++;
//                if(count== CUTOFF) {
//                    BigDecimal avg = getAvg(lost.get());
//                    pingLogger.log(Level.INFO, "Avg lost package: {0}", avg);
//                    count=0;
//                }
//
//                Thread.sleep(1000);
//            }
//        } catch (InterruptedException e) {
//            pingLogger.log(Level.SEVERE, "Ping error: {0}", e.getMessage());
//            Thread.currentThread().interrupt();
//        }
    }
}
