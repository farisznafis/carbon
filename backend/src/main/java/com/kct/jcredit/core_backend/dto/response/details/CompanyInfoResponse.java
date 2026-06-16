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
    private Boolean isSuspended;
    private List<MethodologyInfoResponse> methodologies;
}
