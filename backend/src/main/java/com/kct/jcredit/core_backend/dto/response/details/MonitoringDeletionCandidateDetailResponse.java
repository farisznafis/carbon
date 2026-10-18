package com.kct.jcredit.core_backend.dto.response.details;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MonitoringDeletionCandidateDetailResponse {
    private Integer projectId;
    private String projectPublicId;
    private String projectName;
    private Integer siteId;
    private String sitePublicId;
    private LocalDateTime statusFinalizedAt;
    private long candidateRecordCount;
    private String oldestDateRecorded;
    private String newestDateRecorded;
}
