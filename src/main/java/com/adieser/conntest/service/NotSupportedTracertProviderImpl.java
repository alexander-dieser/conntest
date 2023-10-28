package com.adieser.conntest.service;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.util.Optional;

public class NotSupportedTracertProviderImpl implements TracertProvider {
    private final Logger logger;

    public NotSupportedTracertProviderImpl(Logger logger) {
        this.logger = logger;
    }

    @Override
    public Optional<BufferedReader> executeTracert() {
        String os = System.getProperty("os.name");
        logger.warn("TracertProvider not supported for {}", os);

        return Optional.empty();
    }
}
