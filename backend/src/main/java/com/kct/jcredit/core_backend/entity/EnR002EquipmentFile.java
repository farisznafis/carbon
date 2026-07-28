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
@Table(name = "en_r002_equipment_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnR002EquipmentFile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private EnR002Equipment equipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", length = 50, nullable = false)
    private EquipmentFileType fileType;

    @Column(name = "file_id", length = 100, nullable = false)
    private String fileId;

    @Column(name = "s3_key", length = 500, nullable = false)
    private String s3Key;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;
}