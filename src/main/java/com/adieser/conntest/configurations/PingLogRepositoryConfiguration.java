package com.adieser.conntest.configurations;

import com.adieser.conntest.models.CsvPingLogRepository;
import com.adieser.conntest.models.PingLogRepository;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Repository-related configurations
 */
@Configuration
public class PingLogRepositoryConfiguration {
    private static final String PING_LOGS_PATH_PROPERTY = "conntest.pinglogs.path";
    private final Environment env;
    private final Logger logger;

    public PingLogRepositoryConfiguration(Environment env, Logger logger) throws IOException {
        this.env = env;
        this.logger = logger;
        createPingLogsDirectory();
    }

    /**
     * Makes a {@link CsvPingLogRepository} available for CDI.
     * @return {@link PingLogRepository} for handling persistence in CVS file located in conntest.pinglogs.path
     */
    @Bean
    public PingLogRepository csvPingLogRepository(){
        return new CsvPingLogRepository(env.getProperty(PING_LOGS_PATH_PROPERTY), logger);
    }

    private void createPingLogsDirectory() throws IOException {
        String pingLogsPath = env.getProperty(PING_LOGS_PATH_PROPERTY);
        if (pingLogsPath != null) {
            try {
                Files.createDirectories(Paths.get(pingLogsPath));
            } catch (IOException e) {
                logger.error("Failed to create ping logs directory: " + pingLogsPath, e);
            }
        } else {
            logger.warn("Property '{}' is not set. Ping logs will not be saved.", PING_LOGS_PATH_PROPERTY);
            throw new IOException("Ping logs path not set");
        }
    }
}
