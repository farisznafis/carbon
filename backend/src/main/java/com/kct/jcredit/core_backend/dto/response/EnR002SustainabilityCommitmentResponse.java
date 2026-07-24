package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.Column;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnR002SustainabilityCommitmentResponse extends BaseResponse {
    private String companyName;
    private String repName;
    private String repRole;
    private Boolean hasConsent;
    private Boolean isAgreeTerms;
}
