package com.kct.jcredit.core_backend.entity;

import com.kct.jcredit.core_backend.entity.base.BaseEntity;
import com.kct.jcredit.core_backend.enums.DeletionResult;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "monitoring_data_deletion_history")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringDataDeletionHistory extends BaseEntity {

    @Column(name = "monitoring_record_id", nullable = false, length = 100)
    private String monitoringRecordId;

    @Column(name = "record_time_updated", nullable = false, length = 50)
    private String recordTimeUpdated;

    @Column(name = "date_recorded", length = 10)
    private String dateRecorded;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "site_id")
    private Integer siteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 10)
    private DeletionResult result;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;
}
