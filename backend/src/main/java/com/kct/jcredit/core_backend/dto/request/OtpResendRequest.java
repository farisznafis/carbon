package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class OtpResendRequest {
    @NotNull
    private UUID challengeId;
}
