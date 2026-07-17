package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActionLinkExchangeRequest {
    @NotBlank
    private String token;
}
