package com.kct.jcredit.core_backend.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kct.jcredit.core_backend.entity.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "operator_company")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OperatorCompany extends BaseEntity {

    @Column(name = "company_id", nullable = false, length = 12)
    private String companyId;

    @Column(name = "name_ja", nullable = false, length = 255)
    private String nameJa;

    @Column(name = "name_furigana", nullable = false, length = 255)
    private String nameFurigana;

    @Column(name = "name_en", nullable = false, length = 255)
    private String nameEn;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "corp_number", nullable = false, unique = true, length = 13)
    private String corpNumber;

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

}
