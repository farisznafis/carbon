package com.kct.jcredit.core_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import com.kct.jcredit.core_backend.entity.base.BaseEntity;

@Entity
@Table(name = "project_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectFiles extends BaseEntity{

    @Column(name = "certificate_registration")
    private String certificateRegistration;

    @Column(name = "report_validation")
    private String reportValidation;

    @Column(name = "report_verification")
    private String reportVerification;

}