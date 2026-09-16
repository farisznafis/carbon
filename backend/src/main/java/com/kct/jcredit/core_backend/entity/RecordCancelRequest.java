package com.kct.jcredit.core_backend.entity;

import java.time.Instant;
import java.time.LocalDateTime;

import com.kct.jcredit.core_backend.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * モニタリングレコード取消申請エンティティ
 * 
 * ステータス:
 * - PENDING: 申請中
 * - APPROVED: 適用済
 * - REJECTED: 却下
 */
@Entity
@Table(name = "record_cancel_request")
@SQLDelete(sql = "UPDATE record_cancel_request SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordCancelRequest extends BaseEntity {
    
    /**
     * 対象モニタリングレコードID (NoSQL側のID)
     */
    @Column(name = "record_pk", nullable = false)
    private String recordPk;

        /**
     * 対象モニタリングレコードID (NoSQL側のID)
     */
    @Column(name = "record_sk", nullable = false)
    private String recordSk;
    
    /**
     * 拠点ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false, 
                foreignKey = @ForeignKey(name = "fk_cancel_request_site"))
    private Site site;
    
    /**
     * ステータス: PENDING, APPROVED, REJECTED
     */
    @Column(name = "status", nullable = false, length = 50)
    private String status;
    
    /**
     * 申請者ユーザーID
     */
    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy;
    
    /**
     * 申請日時
     */
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;
    
    /**
     * 取消理由
     */
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;
    
    /**
     * 承認/却下者ユーザーID
     */
    @Column(name = "decided_by", length = 100)
    private String decidedBy;
    
    /**
     * 承認/却下日時
     */
    @Column(name = "decided_at")
    private Instant decidedAt;
    
    
    /**
     * 相殺レコードID（却下時NULL）
     */
    @Column(name = "offset_record_pk")
    private String offsetRecordPk;

      /**
     * 相殺レコードID（却下時NULL）
     */
    @Column(name = "offset_record_sk")
    private String offsetRecordSk;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;
}