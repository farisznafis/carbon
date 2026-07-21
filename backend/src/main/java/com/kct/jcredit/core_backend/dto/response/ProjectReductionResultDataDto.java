package com.kct.jcredit.core_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectReductionResultDataDto {
    
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
    
    @JsonProperty("batch_id")
    private Integer batchId;
    
    @JsonProperty("processing_date")
    private LocalDate processingDate;
    
    @JsonProperty("project_id")
    private Integer projectId;
    
    @JsonProperty("start_time")
    private Instant startTime;
    
    @JsonProperty("end_time")
    private Instant endTime;
    
    @JsonProperty("site_id")
    private Integer siteId;
    
    private BigDecimal empj;
    
    @JsonProperty("empj_m")
    private BigDecimal empjM;
    
    @JsonProperty("empj_s")
    private BigDecimal empjS;
    
    @JsonProperty("empj_s_control")
    private BigDecimal empjSControl;
    
    @JsonProperty("empj_s_battery")
    private BigDecimal empjSBattery;
    
    @JsonProperty("deleted_at")
    private LocalDateTime deletedAt;
    
    @JsonProperty("deleted_by")
    private String deletedBy;

    @JsonProperty("embl")
    private BigDecimal embl;
}
