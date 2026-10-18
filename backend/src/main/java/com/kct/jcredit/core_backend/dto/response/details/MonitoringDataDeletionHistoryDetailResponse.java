package com.kct.jcredit.core_backend.dto.response.details;

import com.kct.jcredit.core_backend.enums.DeletionResult;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MonitoringDataDeletionHistoryDetailResponse {
    private Integer id;
    private String monitoringRecordId;
    private String recordTimeUpdated;
    private String dateRecorded;
    private Integer projectId;
    private Integer siteId;
    private DeletionResult result;
    private String failureReason;
    private LocalDateTime deletedAt;
    private String deletedBy;
}
