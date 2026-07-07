package com.kct.jcredit.core_backend.dto.response.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyInfoResponse {
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
    private String faxNumber;
    private Boolean isSuspended;
    private List<String> requestedMethodologies;
    private String activityInformation;
    private List<MethodologyInfoResponse> methodologies;
}
