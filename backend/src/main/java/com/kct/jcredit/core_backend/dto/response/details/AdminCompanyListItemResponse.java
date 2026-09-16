package com.kct.jcredit.core_backend.dto.response.details;

import com.kct.jcredit.core_backend.enums.CompanyStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents one company row in the administrator list without coupling it to profile DTOs.
 */
@Data
@Builder
public class AdminCompanyListItemResponse {
    private String id;
    private String companyId;
    private String nameJA;
    private String nameFurigana;
    private String nameEN;
    private String email;
    private String phoneNumber;
    private String fax;
    private String corpNumber;
    private String address;
    private String postalCode;
    private String prefecture;
    private String municipality;
    private String blocknum;
    private String buildingName;
    private String representativeFirstName;
    private String representativeLastName;
    private String representativeFirstNameKana;
    private String representativeLastNameKana;
    private String representativePosition;
    private String representativePhoneNumber;
    private List<String> requestedMethodologies;
    private String activityInformation;
    private CompanyStatus status;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
