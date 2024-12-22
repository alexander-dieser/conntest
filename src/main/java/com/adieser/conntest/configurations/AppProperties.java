package com.adieser.conntest.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * This class is used to read the properties from the application.properties file.
 * It is annotated with @ConfigurationProperties to bind the properties with the prefix "conntest".
 * The properties are then injected into the fields of this class using the @Data annotation from Lombok.
 */
@Data
@Component
@ConfigurationProperties(prefix = "conntest")
public class AppProperties {
    private String pingLogsPath;
    private Long fileMaxSize;
    private String simulator;
}

