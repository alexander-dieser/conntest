package com.adieser.conntest.service;

import com.adieser.conntest.models.Pingable;
import com.adieser.conntest.models.ConnTestLogFileImpl;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
public class ConnTestServiceImpl implements ConnTestService {

    private final Logger logger;
    private List<ConnTestLogFileImpl> tests = new ArrayList<>();
    private final ExecutorService threadPoolExecutor;

    public ConnTestServiceImpl(ExecutorService threadPoolExecutor, Logger logger) {
        this.logger = logger;
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @Override
    public void testLocalISPInternet(List<String> ipAddresses){
        tests = ipAddresses.stream()
                .map(s -> new ConnTestLogFileImpl(threadPoolExecutor, s, logger))
                .toList();

        tests.forEach(Pingable::startPingSession);
    }

    public void stopTests(){
        if(tests.isEmpty())
            logger.warn("No tests to stop");
        else
            tests.forEach(Pingable::stopPingSession);
    }
}
