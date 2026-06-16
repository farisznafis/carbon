package com.kct.jcredit.core_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactivationResponse {
    private String statusCode;
    private String message;
    private String companyId;
    private String companyName;
    private Boolean isActive;
    private Boolean isSuspended;
}