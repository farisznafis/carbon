package com.kct.jcredit.core_backend.entity;

import com.kct.jcredit.core_backend.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", referencedColumnName = "id")
    private ParticipatingCompany company;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "action_date", nullable = false)
    private LocalDateTime actionDate;

    @Column(name = "amount", precision = 10, scale = 2)
    private java.math.BigDecimal amount;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "previous_end_date")
    private java.time.LocalDate previousEndDate;

    @Column(name = "new_end_date")
    private java.time.LocalDate newEndDate;
}