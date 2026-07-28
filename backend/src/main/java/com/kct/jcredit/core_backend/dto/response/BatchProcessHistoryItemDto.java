package com.kct.jcredit.core_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchProcessHistoryItemDto {
    
    private Integer id;
    
    @JsonProperty("is_active")
    private Boolean isActive;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("created_by")
    private String createdBy;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @JsonProperty("updated_by")
    private String updatedBy;
    
    @JsonProperty("start_time")
    private Instant startTime;
    
    @JsonProperty("end_time")
    private Instant endTime;
    
    private String status;
    
    @JsonProperty("claimed_by")
    private String claimedBy;
    
    @JsonProperty("claimed_at")
    private Instant claimedAt;
}
