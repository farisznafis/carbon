package com.kct.jcredit.core_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectUploadDocumentRequest {
    @NotNull(message = "プロジェクト登録証の受領ステータスは必須です。")
    @Schema(description = "プロジェクト登録証の受領ステータス", example = "true")
    private Boolean isRegistrationCertReceived;

    @NotNull(message = "妥当性報告書の受領ステータスは必須です。")
    @Schema(description = "妥当性報告書の受領ステータス", example = "true")
    private Boolean isValidationReportReceived;

    @NotNull(message = "検証報告書の受領ステータスは必須です。")
    @Schema(description = "検証報告書の受領ステータス", example = "false")
    private Boolean isVerificationReportReceived;
}