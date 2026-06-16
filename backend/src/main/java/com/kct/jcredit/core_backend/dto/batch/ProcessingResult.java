package com.kct.jcredit.core_backend.dto.batch;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class ProcessingResult {
    private Integer batchId;
    private Integer projectId;
    private Integer siteId;
    private Instant startTime;
    private Instant endTime;
    private String status; // SUCCESS, NO_DATA, FAILED
    private Integer recordsProcessed;
    private Integer monitoringItemId;
    private String errorMessage;
    
    // Factory methods for common scenarios
    public static ProcessingResult success(
            Integer batchId, 
            Integer projectId, 
            Integer siteId,
            Instant startTime,
            Instant endTime,
            Integer recordsProcessed,
            Integer monitoringItemId) {
        return ProcessingResult.builder()
            .batchId(batchId)
            .projectId(projectId)
            .siteId(siteId)
            .startTime(startTime)
            .endTime(endTime)
            .status("SUCCESS")
            .recordsProcessed(recordsProcessed)
            .monitoringItemId(monitoringItemId)
            .build();
    }
    
    public static ProcessingResult noData(
            Integer batchId, 
            Integer projectId, 
            Integer siteId) {
        Instant now = Instant.now();
        return ProcessingResult.builder()
            .batchId(batchId)
            .projectId(projectId)
            .siteId(siteId)
            .startTime(now)
            .endTime(now)
            .status("NO_DATA")
            .recordsProcessed(0)
            .errorMessage("No monitoring data found")
            .build();
    }
    
    public static ProcessingResult failed(
            Integer batchId, 
            Integer projectId, 
            Integer siteId,
            Instant startTime,
            Instant endTime,
            String errorMessage) {
        return ProcessingResult.builder()
            .batchId(batchId)
            .projectId(projectId)
            .siteId(siteId)
            .startTime(startTime)
            .endTime(endTime)
            .status("FAILED")
            .recordsProcessed(0)
            .errorMessage(errorMessage)
            .build();
    }
}