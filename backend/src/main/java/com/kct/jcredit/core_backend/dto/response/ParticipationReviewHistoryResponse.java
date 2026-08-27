package com.kct.jcredit.core_backend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter 
@Builder
public class ParticipationReviewHistoryResponse {
    private Integer attemptNumber;
    private String  decision;
    private String  rejectReason;
    private String  primaryReviewerName;
    private String  secondaryReviewerName;
    private String  reviewedAt;
}