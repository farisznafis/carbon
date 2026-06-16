package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true) 
@Schema(description = "プロジェクトドキュメント管理レスポンス (Response Management Documents for Project)")
public class ProjectUploadDocumentResponse extends BaseResponse {

    @Schema(description = "プロジェクト登録証のURL")
    private String registrationCertUrl;

    @Schema(description = "妥当性報告書のURL")
    private String validationReportUrl;

    @Schema(description = "検証報告書のURL")
    private String verificationReportUrl;

    @Schema(description = "プロジェクト登録証の受領ステータス")
    private Boolean isRegistrationCertReceived;

    @Schema(description = "妥当性報告書の受領ステータス")
    private Boolean isValidationReportReceived;

    @Schema(description = "検証報告書の受領ステータス")
    private Boolean isVerificationReportReceived;
}