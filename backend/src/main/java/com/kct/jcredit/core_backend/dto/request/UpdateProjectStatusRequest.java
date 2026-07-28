package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.dto.request.base.BaseRequest;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectStatusRequest{
    @NotNull(message = "projectStatus is required")
    @Pattern(
        regexp = "^(作成済み|準備中|変更中|妥当性確認処理|却下|実施中|検証中|クレジット申請中|終了)$",
        message = "ステータスはリストから選んでください: 作成済み, 準備中, 変更中, 妥当性確認処理, 却下, 実施中, 検証中, クレジット申請中, 終了"
    )
    private String projectStatus;
}
