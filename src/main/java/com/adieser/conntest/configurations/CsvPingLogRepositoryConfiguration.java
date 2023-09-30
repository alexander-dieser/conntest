package com.adieser.conntest.configurations;

import com.adieser.conntest.models.CsvPingLogRepository;
import com.adieser.conntest.models.PingLogRepository;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class CsvPingLogRepositoryConfiguration {
    private final Environment env;
    private final Logger logger;

    public CsvPingLogRepositoryConfiguration(Environment env, Logger logger) {
        this.env = env;
        this.logger = logger;
    }

    @Bean
    public PingLogRepository csvPingLogRepository(){
        return new CsvPingLogRepository(env.getProperty("conntest.pinglogs.path"), logger);
    }
}
