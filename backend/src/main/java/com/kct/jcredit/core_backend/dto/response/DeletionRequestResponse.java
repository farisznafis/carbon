package com.kct.jcredit.core_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeletionRequestResponse extends BaseResponse{

    private Integer requestId;
    private String companyId;
    private String companyName;
    private String requestedBy;
    private LocalDateTime requestedAt;
    private String reason;
    private String status;
    private String decidedBy;
    private LocalDateTime decidedAt;
    private String rejectionReason;
}
