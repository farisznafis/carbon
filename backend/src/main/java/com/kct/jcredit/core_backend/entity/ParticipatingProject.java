package com.kct.jcredit.core_backend.entity;

import com.kct.jcredit.core_backend.entity.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kct.jcredit.core_backend.converter.StringListJsonConverter;
import com.kct.jcredit.core_backend.entity.base.BaseEntity;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "participating_project")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ParticipatingProject extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private ParticipatingCompany company;

    @Column(name = "is_eligible", nullable = false)
    private Boolean isEligible;

    @Column(name = "status", nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private EnR002Equipment equipment;

    @Column(name = "s3_key_pdf_membership")
    private String s3KeyPdfMembership;

    // TIDAK ADA field company atau siteProjectRelation
    // TIDAK ADA applicantInkan — kolom ini belum ada di tabel,
    // perlu migration terpisah kalau mau ditambahkan nanti
}