package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.ReviewParticipationRequest;
import com.kct.jcredit.core_backend.dto.response.ParticipationReviewDetailResponse;
import com.kct.jcredit.core_backend.dto.response.ParticipationReviewDetailResponse;

import java.util.List;

public interface ParticipationReviewService {
    List<ParticipationReviewDetailResponse> getPendingReviews();
    ParticipationReviewDetailResponse getReviewDetail(Integer participationId);
    void submitReview(Integer participationId, Integer operatorUserId,
                      String operatorName, ReviewParticipationRequest request);

    List<ParticipationReviewDetailResponse> getPendingReviewsByUserId(Integer userId);
}
