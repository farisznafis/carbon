package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.validation.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailOtpStartRequest {
    @NotBlank
    @ValidEmail
    private String email;
}
