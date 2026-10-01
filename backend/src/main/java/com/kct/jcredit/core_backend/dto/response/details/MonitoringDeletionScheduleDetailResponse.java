package com.kct.jcredit.core_backend.dto.response.details;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MonitoringDeletionScheduleDetailResponse {
    private Integer projectId;
    private String projectPublicId;
    private String projectName;
    private LocalDateTime statusFinalizedAt;
    private LocalDate scheduledDeletionDate;
    private long daysUntilDeletion;
}
