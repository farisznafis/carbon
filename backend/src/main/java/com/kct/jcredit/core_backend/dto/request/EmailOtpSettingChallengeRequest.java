package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmailOtpSettingChallengeRequest {
    @NotNull
    private Boolean enabled;
    @NotBlank
    private String currentPassword;
}
