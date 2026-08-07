package com.kct.jcredit.core_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.monitoring.retention")
public class MonitoringDataRetentionProperties {
    private int retentionYears = 2;
}
