package com.adieser.conntest.configurations;

import com.adieser.conntest.service.NotSupportedTracertProviderImpl;
import com.adieser.conntest.service.TracertProvider;
import com.adieser.conntest.service.WindowsTracertProviderImpl;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracertProviderConfiguration {

    private final Logger logger;

    public TracertProviderConfiguration(Logger logger) {
        this.logger = logger;
    }

    @Bean
    public TracertProvider tracertProvider() {

        TracertProvider tracertProvider = new NotSupportedTracertProviderImpl(logger);

        if (System.getProperty("os.name").toLowerCase().contains("win"))
            tracertProvider =  new WindowsTracertProviderImpl();

        return tracertProvider;
    }
}
