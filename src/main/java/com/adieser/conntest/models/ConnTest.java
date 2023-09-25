package com.adieser.conntest.models;

import org.slf4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;

public abstract class ConnTest implements Pingable {
    static final int CUTOFF = 100;
    private static final int TIMEOUT = 3000;
    protected final ExecutorService threadPoolExecutor;
    protected final String ipAddress;
    protected boolean running = true;
    protected final Logger logger;

    ConnTest(ExecutorService threadPoolExecutor, String ipAddress, Logger logger) {
        this.threadPoolExecutor = threadPoolExecutor;
        this.ipAddress = ipAddress;
        this.logger = logger;
    }

    protected static BigDecimal getAvg(int lost) {
        return new BigDecimal(lost)
                .divide(BigDecimal.valueOf(CUTOFF), 2, RoundingMode.HALF_UP);
    }

    protected Ping ping(){
        Ping ping = Ping.builder()
                .ipAddress(ipAddress)
                .reachable(false)
                .build();

        long currentTime = System.currentTimeMillis();
        if(isReachable(ipAddress)) {
            long time = System.currentTimeMillis() - currentTime;
            ping.setTime(time);
            ping.setReachable(true);
        }

        return ping;
    }

    protected static boolean isReachable(String ip) {
        InetAddress inet;
        boolean ping=false;
        try {
            inet = InetAddress.getByName(ip);
            ping = inet.isReachable(TIMEOUT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ping;
    }
}
