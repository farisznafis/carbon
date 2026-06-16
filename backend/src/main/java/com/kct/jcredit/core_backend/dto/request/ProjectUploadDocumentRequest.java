package com.kct.jcredit.core_backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ProjectUploadDocumentRequest {
    @Schema(description = "プロジェクト登録証の受領ステータス", example = "true")
    private Boolean isRegistrationCertReceived;

    @Schema(description = "妥当性報告書の受領ステータス", example = "true")
    private Boolean isValidationReportReceived;

    @Schema(description = "検証報告書の受領ステータス", example = "false")
    private Boolean isVerificationReportReceived;
}