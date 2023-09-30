package com.adieser.conntest.models;

import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;

public abstract class ConnTest implements Pingable {
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

    protected long ping(){
        long time = -1L;

        long currentTime = System.currentTimeMillis();
        if(isReachable(ipAddress))
            time = System.currentTimeMillis() - currentTime;

        return time;
    }

    protected boolean isReachable(String ip) {
        InetAddress inet;
        boolean ping=false;
        try {
            inet = InetAddress.getByName(ip);
            ping = inet.isReachable(TIMEOUT);
        } catch (IOException e) {
            logger.error("isReachable error", e);
        }
        return ping;
    }
}
