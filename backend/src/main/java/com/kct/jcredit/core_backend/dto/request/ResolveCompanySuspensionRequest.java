package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Carries the audit note used to resolve a blocker.
 */
@Data
public class ResolveCompanySuspensionRequest {

    @NotBlank(message = "解除理由を入力してください。")
    @Size(max = 500, message = "解除理由は500文字以内で入力してください。")
    private String resolutionNote;
}
