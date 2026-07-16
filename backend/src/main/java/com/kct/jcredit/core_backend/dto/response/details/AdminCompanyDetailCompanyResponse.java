package com.kct.jcredit.core_backend.dto.response.details;

import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminCompanyDetailCompanyResponse {
    private String companyId;
    private String corpNumber;
    private String nameJA;
    private String nameFurigana;
    private String nameEN;
    private String email;
    private String phoneNumber;
    private String faxNumber;
    private String address;
    private String postalCode;
    private String prefecture;
    private String municipality;
    private String blocknum;
    private String buildingName;
    private CompanyStatus companyStatus;
    private SubscriptionStatus subscriptionStatus;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
