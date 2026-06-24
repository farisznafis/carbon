package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// FinalAgreementValidateRequest.java
@Getter
@Setter
public class FinalAgreementValidateRequest {

    @NotNull
    private Boolean isAgreeTerms;
}