package com.adieser.conntest.configurations;

import com.adieser.conntest.models.CsvPingLogRepository;
import com.adieser.conntest.models.PingLogRepository;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class CsvPingLogRepositoryConfiguration {
    private final Logger logger;

    private final Environment env;

    public CsvPingLogRepositoryConfiguration(Logger logger, Environment env) {
        this.logger = logger;
        this.env = env;
    }

    @Bean
    public PingLogRepository csvPingLogRepository(){
        return new CsvPingLogRepository(logger, env.getProperty("conntest.pinglogs.path"));
    }
}
