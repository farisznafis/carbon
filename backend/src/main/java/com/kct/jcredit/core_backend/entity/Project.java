package com.kct.jcredit.core_backend.entity;

import java.util.List;

import com.kct.jcredit.core_backend.entity.base.BaseEntity;

import jakarta.persistence.*;
import jakarta.persistence.PrePersist;
import lombok.*;

@Entity
@Table(name = "project")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project extends BaseEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 32)
    private String publicId;

    @Column(name = "project_name", nullable = false, length = 100)
    private String projectName;

    @Column(name = "project_number", nullable = false, unique = true, length = 10)
    private String projectNumber;

    @Column(name = "project_status", nullable = false, length = 10)
    private String projectStatus;

    @Column(name = "rep_first_name", nullable = false, length = 50)
    private String repFirstName;

    @Column(name = "rep_first_name_kana", nullable = false, length = 50)
    private String repFirstNameKana;

    @Column(name = "rep_last_name", nullable = false, length = 50)
    private String repLastName;

    @Column(name = "rep_last_name_kana", nullable = false, length = 50)
    private String repLastNameKana;

    @Column(name = "rep_email", nullable = false, length = 254)
    private String repEmail;

    @Column(name = "rep_phone_number", nullable = false, length = 20)
    private String repPhoneNumber;

    @Column(name = "rep_position", nullable = false, length = 100)
    private String repPosition;

    @Column(name = "pic_first_name", nullable = false, length = 50)
    private String picFirstName;

    @Column(name = "pic_first_name_kana", nullable = false, length = 50)
    private String picFirstNameKana;

    @Column(name = "pic_last_name", nullable = false, length = 50)
    private String picLastName;

    @Column(name = "pic_last_name_kana", nullable = false, length = 50)
    private String picLastNameKana;

    @Column(name = "pic_email", nullable = false, length = 254)
    private String picEmail;

    @Column(name = "pic_phone_number", nullable = false, length = 20)
    private String picPhoneNumber;

    @Column(name = "pic_position", nullable = false, length = 100)
    private String picPosition;

    @Column(name = "data_management_pic", nullable = false, length = 50)
    private String dataManagementPic;

    @Column(name = "emission_reduction")
    private Double emissionReduction;

    @Column(name = "baseline_emission")
    private Double baselineEmission;

    @Column(name = "post_project_emission")
    private Double postProjectEmission;

    @Column(name = "regional_category", nullable = false, length = 10)
    private String regionalCategory;

    @Column(name = "new_members_total", nullable = false)
    private Integer newMembersTotal;

    @Column(name = "withdrawn_members", nullable = false)
    private Integer withdrawnMembers;

    @Column(name = "cumulative_members_total", nullable = false)
    private Integer cumulativeMembersTotal;

    @Column(name = "members_total")
    private Integer membersTotal;

    @Column(name = "methodology_version", nullable = false, length = 10)
    private String methodologyVersion;

    @Column(name = "method_category", nullable = false, length = 10)
    private String methodCategory;

    @Column(name = "method_number", nullable = false, length = 3)
    private String methodNumber;

    @Column(name = "method_name", nullable = false, length = 255)
    private String methodName;

    @Column(name = "method_main_emission", nullable = false, length = 255)
    private String methodMainEmission;

    @Column(name = "method_combi_feasibility", nullable = false)
    private Boolean methodCombiFeasibility;

    @Column(name = "method_details", nullable = false, length = 1000)
    private String methodDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_company_id", nullable = false)
    private OperatorCompany operatorCompany;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
    private List<SiteProjectRelation> siteRelations;
    @PrePersist
    private void generatePublicId() {
        if (this.publicId == null) {
            this.publicId = com.kct.jcredit.core_backend.util.PublicIdGenerator.generateProjectPublicId();
        }
    }

}