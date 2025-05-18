package com.adieser.conntest.models.utils;


import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
@ConditionalOnProperty(name = "conntest.simulator", havingValue = "disabled")
public class PingUtils implements Reachable {
    protected static final int TIMEOUT = 3000;

    private final Logger logger;

    public PingUtils(Logger logger) {
        this.logger = logger;
    }

    /**
     * Check if an ip address is reachable. It tries to reach the ip address during {@code TIMEOUT} ms, if it is not
     * possible it means the ip address is not reachable.
     * @param ip ip address to check connectivity
     * @return true if the ip address is reachable. False otherwise
     */
    public boolean isReachable(String ip) {
        InetAddress inet;
        boolean ping=false;
        try {
            inet = getInet(ip);
            ping = inet.isReachable(TIMEOUT);
        } catch (IOException e) {
            logger.error("IP address {} is not reachable", ip, e);
        }
        return ping;
    }

    InetAddress getInet(String ip) throws UnknownHostException {
        return InetAddress.getByName(ip);
    }
}
