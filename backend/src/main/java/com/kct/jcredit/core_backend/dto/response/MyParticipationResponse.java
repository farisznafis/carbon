package com.kct.jcredit.core_backend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter 
@Builder
public class MyParticipationResponse {
    private Integer participationId;
    private String siteId;
    private String siteName;
    private String status;
    private String pdfStatus;
}
