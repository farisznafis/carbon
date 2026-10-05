package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.response.MonitoringDataDeletionHistoryListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringDeletionScheduleListResponse;

import java.time.LocalDate;

public interface MonitoringDataDeletionService {

    /**
     * Lists finalized projects whose monitoring data is still waiting for its deletion date.
     */
    MonitoringDeletionScheduleListResponse findDeletionSchedule();

    /**
     * One pass of the automatic deletion job: sends the advance and same-day notices, then
     * deletes the monitoring data of every project whose deletion date has been reached.
     * Safe to run repeatedly; whatever a previous pass left behind is picked up again.
     */
    void runAutomaticDeletion(LocalDate today);

    /**
     * Returns paginated deletion history, optionally filtered by project.
     */
    MonitoringDataDeletionHistoryListResponse findDeletionHistory(Integer projectId, int page, int limit);
}
