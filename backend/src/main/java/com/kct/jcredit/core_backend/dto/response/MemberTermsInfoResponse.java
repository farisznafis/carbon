package com.kct.jcredit.core_backend.dto.response;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter 
@Builder
public class MemberTermsInfoResponse {
    private String statusCode;
    private String message;

    private String projectName;
    private String operatorNameJa;
    private String adminRepName;
    private String projectOverview;
    private String usagePurpose;
    private Instant startedAt;
}