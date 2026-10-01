package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.config.MonitoringDataRetentionProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Runs the automatic monitoring data deletion (削除) once a day.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringDataDeletionScheduler {

    // Arbitrary but fixed: every instance must ask Postgres for the same advisory lock.
    private static final long ADVISORY_LOCK_KEY = 7_302_011_925L;

    private final MonitoringDataDeletionService monitoringDataDeletionService;
    private final MonitoringDataRetentionProperties retentionProperties;
    private final DataSource dataSource;

    @Scheduled(cron = "${app.monitoring.retention.auto-deletion-cron:0 0 2 * * *}")
    public void runDaily() {
        if (!retentionProperties.isAutoDeletionEnabled()) {
            return;
        }
        // A session-level advisory lock keeps a second backend instance from running the same
        // pass and sending every notice twice. It lives on this connection, so it is released
        // even if the instance dies mid-run.
        try (Connection connection = dataSource.getConnection()) {
            if (!tryLock(connection)) {
                log.info("[MONITORING DELETION] Another instance is already running the job");
                return;
            }
            try {
                monitoringDataDeletionService.runAutomaticDeletion(LocalDate.now());
            } finally {
                unlock(connection);
            }
        } catch (Exception e) {
            log.error("[MONITORING DELETION] Scheduled run failed", e);
        }
    }

    private boolean tryLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
            statement.setLong(1, ADVISORY_LOCK_KEY);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }

    private void unlock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
            statement.setLong(1, ADVISORY_LOCK_KEY);
            statement.execute();
        }
    }
}
