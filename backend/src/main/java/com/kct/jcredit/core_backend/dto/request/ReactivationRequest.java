package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReactivationRequest {
    @NotBlank(message = "企业IDは必須です。")
    private String companyId;
    
    private String paymentMethod;
}