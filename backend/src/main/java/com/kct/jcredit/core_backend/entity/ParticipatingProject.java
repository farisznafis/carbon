package com.kct.jcredit.core_backend.entity;

import com.kct.jcredit.core_backend.entity.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kct.jcredit.core_backend.converter.StringListJsonConverter;
import com.kct.jcredit.core_backend.entity.base.BaseEntity;
import com.kct.jcredit.core_backend.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "participating_project",
       uniqueConstraints = 
       @UniqueConstraint(columnNames = {"site_project_relation_id"}))
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder

public class ParticipatingProject extends BaseEntity{
    // Link ke relasi site-project
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_project_relation_id", nullable = false)
    private SiteProjectRelation siteProjectRelation;

    // Equipment pertama — dasar generate PDF
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private EnR002Equipment equipment;

    // Master user yang melakukan participation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private ParticipatingUser user;

    // Denormalized dari site.company — untuk query cepat
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", referencedColumnName = "company_id", nullable = false)
    private ParticipatingCompany company;

    // Hasil evaluasi eligibility — diisi oleh proses terpisah, default false
    @Column(name = "is_eligible", nullable = false)
    private Boolean isEligible = false;

    // TODO: Digital inkan — nullable
    // S3 key path or CloudSign/DocuSign
    // Referensi: 別紙9【確認者記名・押印欄】
    @Column(name = "applicant_inkan", length = 500)
    private String applicantInkan;

    // S3 key PDF 別紙9 — diisi setelah PDF berhasil di-generate
    @Column(name = "s3_key_pdf_membership", length = 500)
    private String s3KeyPdfMembership;  
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    public Optional<Site> findById(Integer participationId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findById'");
    }  
}
