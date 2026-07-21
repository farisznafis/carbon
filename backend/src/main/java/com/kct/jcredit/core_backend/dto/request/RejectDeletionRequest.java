package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectDeletionRequest {
    @NotBlank(message = "rejectionReason is required")
    private String rejectionReason;
}
