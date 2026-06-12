package com.stubserver.backend.database.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "table")
@PropertySource(value = {"file:./db-tables.properties", "classpath:db-tables.properties"}, ignoreResourceNotFound = true)
public class TableNames {

    private String auditLogs;
    private String assignedServices;
    private String vsDetails;
    private String masterCatalog;
}
