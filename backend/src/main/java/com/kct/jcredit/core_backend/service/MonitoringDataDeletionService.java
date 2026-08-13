package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.response.MonitoringDataDeletionHistoryListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringDeletionCandidateListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringDeletionCandidateRecordListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringDeletionExecutionResponse;

import java.util.List;

public interface MonitoringDataDeletionService {

    /**
     * Lists monitoring data deletion candidates, grouped by project/site.
     */
    MonitoringDeletionCandidateListResponse previewDeletionCandidates();

    /**
     * Lists the individual candidate records for one project/site combination,
     * for operators drilling into a single row of previewDeletionCandidates().
     */
    MonitoringDeletionCandidateRecordListResponse previewDeletionCandidateRecords(
            String projectPublicId,
            String sitePublicId
    );

    /**
     * Deletes monitoring data for the given eligible projects and records the outcome per record.
     */
    MonitoringDeletionExecutionResponse executeDeletion(String actor, List<String> projectPublicIds);

    /**
     * Returns paginated deletion history, optionally filtered by project.
     */
    MonitoringDataDeletionHistoryListResponse findDeletionHistory(Integer projectId, int page, int limit);
}
