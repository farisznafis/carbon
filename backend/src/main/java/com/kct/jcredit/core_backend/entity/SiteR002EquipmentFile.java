package com.kct.jcredit.core_backend.entity;

import java.time.LocalDateTime;

import com.kct.jcredit.core_backend.entity.base.BaseEntity;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "site_r002_equipment_file")
@SQLDelete(sql = "UPDATE site_r002_equipment_file SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteR002EquipmentFile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_data_id", nullable = false)
    private SiteR002MemberData memberData;

    @Column(name = "file_id", length = 100)
    private String fileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", length = 50, nullable = false)
    private EquipmentFileType fileType;

    // @Column(name = "field_reference", length = 255)
    // private String fieldReference;

    @Column(name = "s3_key", length = 1000, nullable = false)
    private String s3Key;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;
}