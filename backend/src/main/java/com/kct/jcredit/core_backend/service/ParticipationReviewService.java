package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.ReviewParticipationRequest;
import com.kct.jcredit.core_backend.dto.request.ApproveReviewRequest;
import com.kct.jcredit.core_backend.dto.request.RejectReviewRequest;
import com.kct.jcredit.core_backend.dto.response.ParticipationReviewDetailResponse;
import com.kct.jcredit.core_backend.dto.response.ParticipationReviewHistoryResponse;
import com.kct.jcredit.core_backend.dto.response.PendingReviewParticipationResponse;
import com.kct.jcredit.core_backend.dto.response.ParticipationReviewDetailResponse;

import java.util.List;

public interface ParticipationReviewService {
    List<ParticipationReviewDetailResponse> getPendingReviews();
    List<ParticipationReviewDetailResponse> getPendingReviewsByUserId(Integer userId);
    ParticipationReviewDetailResponse getReviewDetail(Integer participationId);
    void approveReview(Integer participationId, Integer operatorUserId, ApproveReviewRequest request);
    void rejectReview(Integer participationId, Integer operatorUserId, RejectReviewRequest request);
    public List<ParticipationReviewHistoryResponse> getReviewHistory(Integer participationId);
}
