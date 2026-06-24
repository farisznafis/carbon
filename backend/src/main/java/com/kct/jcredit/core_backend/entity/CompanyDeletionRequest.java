package com.kct.jcredit.core_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import com.kct.jcredit.core_backend.entity.base.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_deletion_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyDeletionRequest extends BaseEntity {

    @Column(name = "company_id", nullable = false, length = 12)
    private String companyId;

    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "decided_by", length = 100)
    private String decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
}
