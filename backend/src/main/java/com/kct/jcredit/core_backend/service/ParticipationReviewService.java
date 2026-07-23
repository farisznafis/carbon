package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.ReviewParticipationRequest;
import com.kct.jcredit.core_backend.dto.response.ParticipationReviewDetailResponse;
import com.kct.jcredit.core_backend.dto.response.PendingReviewParticipationResponse;
import com.kct.jcredit.core_backend.dto.response.ParticipationReviewDetailResponse;

import java.util.List;

public interface ParticipationReviewService {

    // For controllers needing full detail (equipment, confirmation items, etc.)
    List<ParticipationReviewDetailResponse> getPendingReviews();
    List<ParticipationReviewDetailResponse> getPendingReviewsByUserId(Integer userId);

    // // For controllers needing lightweight summary (id, project, site, company, submittedAt)
    // List<PendingReviewParticipationResponse> getPendingReviewsSummary();
    // List<PendingReviewParticipationResponse> getPendingReviewsSummaryByUserId(Integer userId);

    ParticipationReviewDetailResponse getReviewDetail(Integer participationId);

    void submitReview(Integer participationId, ReviewParticipationRequest request);
}
