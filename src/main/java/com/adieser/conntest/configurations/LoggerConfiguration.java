package com.adieser.conntest.configurations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Logger configuration
 */
@Configuration
public class LoggerConfiguration {

    /**
     * Makes a SL4J logger available for CDI
     * @return logger
     */
    @Bean
    public Logger logger() {
        return LoggerFactory.getLogger("AppLogger");
    }
}
