package com.kct.jcredit.core_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectCompanyRequest {

    @NotBlank(message = "却下理由は必須です。")
    @Schema(description = "却下理由", example = "必要書類が不足しています。")
    private String rejectionReason;
}
