package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// JigyoushaValidateRequest.java
@Getter
@Setter
public class JigyoushaValidateRequest {

    @NotBlank
    private String companyName;

    @NotBlank
    private String repName;

    @NotBlank
    private String repRole;

    @NotNull
    private Boolean hasConsent;
}