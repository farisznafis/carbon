package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import lombok.Builder;
import lombok.Getter;

@Getter 
@Builder
public class ParticipationReviewHistoryResponse extends BaseResponse{
    private Integer attemptNumber;
    private String  decision;
    private String  rejectReason;
    private String  primaryReviewerName;
    private String  secondaryReviewerName;
    private String  reviewedAt;
}