package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import lombok.Builder;
import lombok.Getter;

@Getter 
@Builder
public class MyParticipationResponse extends BaseResponse {
    private Integer participationId;
    private String siteId;
    private String siteName;
    private String status;
    private String pdfStatus;
}
