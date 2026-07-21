package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.enums.SuspensionReasonCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Carries the category and explanation for a new operational blocker.
 */
@Data
public class CreateCompanySuspensionRequest {

    @NotNull(message = "停止理由コードを指定してください。")
    private SuspensionReasonCode reasonCode;

    @NotBlank(message = "停止理由を入力してください。")
    @Size(max = 500, message = "停止理由は500文字以内で入力してください。")
    private String reasonDetail;
}
