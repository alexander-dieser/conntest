package com.adieser.conntest.configurations;

import com.adieser.conntest.models.CsvPingLogRepository;
import com.adieser.conntest.models.PingLogRepository;
import com.adieser.conntest.service.writer.FileWriterService;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
                                          AppProperties appProperties) {
        this.logger = logger;
        this.fileWriterService = fileWriterService;
        this.appProperties = appProperties;
    }

    /**
     * Makes a {@link CsvPingLogRepository} available for CDI.
     * @return {@link PingLogRepository} for handling persistence in CVS file located in conntest.pinglogs.path
     */
    @Bean
    public PingLogRepository csvPingLogRepository(){
        return new CsvPingLogRepository(appProperties.getPingLogsPath(), logger, fileWriterService);
    }
}
