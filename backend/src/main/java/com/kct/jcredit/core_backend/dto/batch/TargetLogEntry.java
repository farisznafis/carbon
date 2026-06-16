package com.kct.jcredit.core_backend.dto.batch;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class TargetLogEntry {
    private Integer batchId;
    private Integer projectId;
    private Integer siteId;
    private Instant expectedStartTime;
    private String status; // Always "PENDING"
}