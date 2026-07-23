package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank
    @Pattern(regexp = "PARTICIPATING|OPERATING")
    private String accountType;
    private String companyId;
    @NotBlank
    private String identifier;
}
