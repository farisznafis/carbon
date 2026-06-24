package com.kct.jcredit.core_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kct.jcredit.core_backend.entity.base.BaseEntity;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "site")
@SQLDelete(sql = "UPDATE site SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Site extends BaseEntity {

    @Column(name = "site_name", nullable = false, length = 255)
    private String siteName;

    @Column(name = "postal_code", nullable = false, length = 8)
    private String postalCode;

    @Column(name = "prefecture", nullable = false, length = 10)
    private String prefecture;

    @Column(name = "municipality", nullable = false, length = 150)
    private String municipality;

    @Column(name = "blocknum", nullable = false, length = 100)
    private String blocknum;

    @Column(name = "building_name", length = 255)
    private String buildingName;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "participating_company_id", nullable = false)
    private ParticipatingCompany company;

    @OneToMany(mappedBy = "site", fetch = FetchType.LAZY)
    private List<SiteProjectRelation> siteProjectRelations;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;
}