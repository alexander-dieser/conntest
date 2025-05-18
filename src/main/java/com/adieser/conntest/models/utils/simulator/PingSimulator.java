package com.adieser.conntest.models.utils.simulator;

import com.adieser.conntest.models.utils.Reachable;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
@ConditionalOnProperty(name = "conntest.simulator", havingValue = "enabled")
public class PingSimulator implements Reachable {
    private final Logger logger;

    public PingSimulator(Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean isReachable(String ip) {
        boolean isReachable = true;
        try {
            int randomNum = ThreadLocalRandom.current().nextInt(101);
            if(ip.equals("8.8.8.8")) {
                if (randomNum < 40) {
                    Thread.sleep(3000);
                    isReachable = false;
                }else if (randomNum < 60)
                    Thread.sleep(1000 + randomNum * 3L);
                else
                    Thread.sleep(randomNum);
            }else
                Thread.sleep(randomNum);

        } catch (Exception e) {
            logger.error("Error", e);
            Thread.currentThread().interrupt();
        }

        return isReachable;
    }
}
