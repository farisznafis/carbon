package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.validation.ValidSecurityPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompletePasswordResetRequest {
    @NotBlank
    @ValidSecurityPassword
    private String newPassword;
}
