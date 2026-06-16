package com.kct.jcredit.core_backend.dto.response;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class CancelRequestResponse extends BaseResponse{
    private Integer cancelRequestId;
    private String recordPk;
    private String recordSk;
    private String status;
    private String requestedBy;
    private String requestedAt;  // ISO 8601 format
    private Integer siteId;
    private String reason;
}
