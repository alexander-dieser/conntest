package com.adieser.conntest.configurations;

import com.adieser.conntest.models.CsvPingLogRepository;
import com.adieser.conntest.models.PingLogRepository;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Repository-related configurations
 */
@Configuration
public class PingLogRepositoryConfiguration {
    private final Environment env;
    private final Logger logger;

    public PingLogRepositoryConfiguration(Environment env, Logger logger) {
        this.env = env;
        this.logger = logger;
    }

    /**
     * Makes a {@link CsvPingLogRepository} available for CDI.
     * @return {@link PingLogRepository} for handling persistence in CVS file located in conntest.pinglogs.path
     */
    @Bean
    public PingLogRepository csvPingLogRepository(){
        return new CsvPingLogRepository(env.getProperty("conntest.pinglogs.path"), logger);
    }
}
