package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.request.RejectCompanyRequest;
import com.kct.jcredit.core_backend.dto.request.ReviewParticipationRequest;
import com.kct.jcredit.core_backend.dto.response.ApproveCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.PendingCompanyListResponse;
import com.kct.jcredit.core_backend.dto.response.RejectCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.ParticipationReviewDetailResponse;
import com.kct.jcredit.core_backend.service.CompanyApprovalService;
import com.kct.jcredit.core_backend.service.ParticipationReviewService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.service.AuthService;
import com.kct.jcredit.core_backend.service.UserService;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/companies")
@Tag(name = "管理者", description = "管理者による参加企業の承認、却下、承認待ち一覧を管理するAPIです。")
@RequiredArgsConstructor
public class AdminCompanyController {

    private final CompanyApprovalService companyApprovalService;
    private final ParticipationReviewService reviewService;
    private final JwtUtil jwtUtil;

    @PostMapping("/{companyId}/approve")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "参加企業登録承認", description = "指定された参加企業の登録申請を承認します。")
    public ResponseEntity<ApproveCompanyResponse> approveCompany(@PathVariable String companyId) {
        ApproveCompanyResponse response = companyApprovalService.approveCompany(companyId);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/{companyId}/reject")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "参加企業登録却下", description = "指定された参加企業の登録申請を却下します。")
    public ResponseEntity<RejectCompanyResponse> rejectCompany(
            @PathVariable String companyId,
            @RequestBody @Valid RejectCompanyRequest request) {
        RejectCompanyResponse response = companyApprovalService.rejectCompany(companyId, request.getRejectionReason());
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "承認待ち参加企業一覧取得", description = "承認待ちの参加企業一覧を取得します。")
    public ResponseEntity<PendingCompanyListResponse> getPendingCompanies() {
        PendingCompanyListResponse response = companyApprovalService.getPendingCompanies();
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }
    
    @GetMapping("/participation")
    @PreAuthorize("hasRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ParticipationReviewDetailResponse>> getPendingReviews() {
        return ResponseEntity.ok(reviewService.getPendingReviews());
    }

    @GetMapping("/{participationId}")
    @PreAuthorize("hasRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ParticipationReviewDetailResponse> getReviewDetail(
            @PathVariable Integer participationId) {
        return ResponseEntity.ok(reviewService.getReviewDetail(participationId));
    }

    @PostMapping("/{participationId}/review")
    @PreAuthorize("hasRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BaseResponse> submitReview(
            Authentication authentication,
            @PathVariable Integer participationId,
            @RequestBody @Valid ReviewParticipationRequest request) {
        
        reviewService.submitReview(participationId, request);
        return ResponseEntity.ok(BaseResponse.success("レビューを受け付けました。"));
    }
}
