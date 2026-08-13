package com.kct.jcredit.core_backend.dto.response.details;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonitoringDeletionCandidateRecordDetailResponse {
    private String monitoringRecordId;
    private String recordTimeUpdated;
    private String dateRecorded;
}
