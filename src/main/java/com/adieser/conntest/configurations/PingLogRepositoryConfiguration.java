package com.adieser.conntest.configurations;

import com.adieser.conntest.models.CsvPingLogRepository;
import com.adieser.conntest.models.PingLogRepository;
import com.adieser.conntest.service.writer.FileWriterService;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Repository-related configurations
 */
@Configuration
public class PingLogRepositoryConfiguration {
    private final Logger logger;
    private final FileWriterService fileWriterService;
    private final AppProperties appProperties;

    public PingLogRepositoryConfiguration(Logger logger,
                                          FileWriterService fileWriterService,
                                          AppProperties appProperties) throws IOException {
        this.logger = logger;
        this.fileWriterService = fileWriterService;
        this.appProperties = appProperties;
        createPingLogsDirectory();
    }

    /**
     * Makes a {@link CsvPingLogRepository} available for CDI.
     * @return {@link PingLogRepository} for handling persistence in CVS file located in conntest.pinglogs.path
     */
    @Bean
    public PingLogRepository csvPingLogRepository(){
        return new CsvPingLogRepository(appProperties.getPingLogsPath(), logger, fileWriterService);
    }

    private void createPingLogsDirectory() throws IOException {
        String pingLogsPath = appProperties.getPingLogsPath();
        if (pingLogsPath != null) {
            try {
                Files.createDirectories(Paths.get(pingLogsPath));
            } catch (IOException e) {
                logger.error("Failed to create ping logs directory: {}", pingLogsPath, e);
            }
        } else {
            logger.warn("Property 'conntest.pinglogs-path' is not set. Ping logs will not be saved.");
            throw new IOException("Ping logs path not set");
        }
    }
}
