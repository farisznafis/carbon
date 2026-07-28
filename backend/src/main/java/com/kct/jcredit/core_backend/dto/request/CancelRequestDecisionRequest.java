package com.kct.jcredit.core_backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 取消申請承認/却下リクエスト
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelRequestDecisionRequest {
    
    /**
     * 決定: APPROVE または REJECT
     */
    @NotBlank(message = "決定は必須です")
    @Pattern(regexp = "APPROVE|REJECT", message = "決定はAPPROVEまたはREJECTである必要があります")
    private String decision;
    
    /**
     * 管理者コメント（却下理由など）
     */
    private String managerRemarks;
}