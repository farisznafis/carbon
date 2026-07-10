package com.kct.jcredit.core_backend.dto.response.details;

import com.kct.jcredit.core_backend.enums.SuspensionReasonCode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Exposes an auditable suspension without exposing the company entity itself.
 */
@Data
@Builder
public class CompanySuspensionDetailResponse {
    private Integer id;
    private SuspensionReasonCode reasonCode;
    private String reasonDetail;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private String resolutionNote;
    private boolean active;
}
