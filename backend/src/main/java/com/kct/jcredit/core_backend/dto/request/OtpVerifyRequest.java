package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class OtpVerifyRequest {
    @NotNull
    private UUID challengeId;
    @NotBlank
    @Pattern(regexp = "\\d{6}")
    private String code;
}
