package com.kct.jcredit.core_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kct.jcredit.core_backend.entity.base.BaseEntity;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "en_r002_reduction_result_total")
@SQLDelete(sql = "UPDATE en_r002_reduction_result_total SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnR002ReductionResultTotal extends BaseEntity {
    
    @Column(name = "batch_id", nullable = false, unique = true)
    private Integer batchId;
    
    @Column(name = "processing_date", nullable = false)
    private LocalDate processingDate;
    
    @Column(name = "project_id", nullable = false, unique = true)
    private Integer projectId;
    
    @Column(name = "start_time", nullable = false)
    private Instant startTime;
    
    @Column(name = "end_time", nullable = false)
    private Instant endTime;
    
    @Column(name = "empj", nullable = false, precision = 12, scale = 3)
    private BigDecimal empj;
    
    @Column(name = "empj_m", nullable = false, precision = 12, scale = 3)
    private BigDecimal empjM;
    
    @Column(name = "empj_s", precision = 12, scale = 3)
    private BigDecimal empjS;
    
    @Column(name = "empj_s_control", precision = 12, scale = 3)
    private BigDecimal empjSControl;
    
    @Column(name = "empj_s_battery", precision = 12, scale = 3)
    private BigDecimal empjSBattery;

    @Column(name = "elpj", precision = 12, scale = 3)
    private BigDecimal elpj;

    @Column(name = "embl", precision = 12, scale = 3)
    private BigDecimal embl;

    @Column(name = "elbl", precision = 12, scale = 3)
    private BigDecimal elbl;

    @Column(name = "er", precision = 12, scale = 3)
    private BigDecimal er;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;
}