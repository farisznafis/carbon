package com.kct.jcredit.core_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.monitoring.retention")
public class MonitoringDataRetentionProperties {
    private int retentionYears = 2;

    /** Master switch for the scheduled deletion job. */
    private boolean autoDeletionEnabled = false;

    /** How many days before the deletion date the advance notice is sent. */
    private int advanceNoticeDays = 7;

    /**
     * No project is deleted before this date (ISO yyyy-MM-dd). Set it to the go-live date plus
     * advanceNoticeDays so projects already past retention still get both notices.
     * The job refuses to run while this is empty.
     */
    private String earliestDeletionDate;

    /** Base wait between retries of items DynamoDB returned as unprocessed. */
    private long retryBackoffMillis = 200;

    public LocalDate resolveEarliestDeletionDate() {
        if (earliestDeletionDate == null || earliestDeletionDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(earliestDeletionDate.trim());
    }
}
