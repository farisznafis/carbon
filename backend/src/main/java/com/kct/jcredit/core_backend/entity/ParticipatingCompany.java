package com.kct.jcredit.core_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kct.jcredit.core_backend.entity.base.BaseEntity;
import com.kct.jcredit.core_backend.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "participating_company")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParticipatingCompany extends BaseEntity {

    @Column(name = "company_id", nullable = false, unique = true, length = 12)
    private String companyId;

    @Column(name = "name_ja", nullable = false, length = 255)
    private String nameJa;

    @Column(name = "name_furigana", nullable = false, length = 255)
    private String nameFurigana;

    @Column(name = "name_en", nullable = false, length = 255)
    private String nameEn;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "corp_number", nullable = false, unique = true, length = 13)
    private String corpNumber;

    @Column(name = "postal_code", nullable = false, length = 8)
    private String postalCode;

    @Column(name = "prefecture", nullable = false, length = 100)
    private String prefecture;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "town", nullable = false, length = 100)
    private String town;

    @Column(name = "blocknum", nullable = false, length = 100)
    private String blocknum;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "fax", length = 20)
    private String fax;

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

    @Column(name = "reg_status", length = 20)
    private String regStatus;

    @Column(name = "contract_end_date", nullable = false)
    private LocalDate contractEndDate;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "last_renewal_date")
    private LocalDateTime lastRenewalDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", length = 20)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;

    @Column(name = "suspension_reason", length = 500)
    private String suspensionReason;

    @Column(name = "suspension_date")
    private LocalDateTime suspensionDate;

    public boolean isSuspended() {
        return subscriptionStatus == SubscriptionStatus.SUSPENDED;
    }

    public boolean isActiveSubscription() {
        return subscriptionStatus == SubscriptionStatus.ACTIVE && contractEndDate != null && !contractEndDate.isBefore(LocalDate.now());
    }

    
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SiteR002MemberData> methodologies = new ArrayList<>();
}
