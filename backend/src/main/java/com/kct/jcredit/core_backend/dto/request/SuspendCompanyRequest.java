package com.kct.jcredit.core_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuspendCompanyRequest {
    
    @NotBlank(message = "企業IDは必須です。")
    @Schema(description = "企業ID", example = "1", type = "integer")
    private String companyId;
    
    @Schema(description = "停止理由", example = "料金未払い")
    private String reason;
}