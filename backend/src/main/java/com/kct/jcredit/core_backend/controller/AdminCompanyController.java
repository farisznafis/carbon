package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.request.ApproveReviewRequest;
import com.kct.jcredit.core_backend.dto.request.RejectCompanyRequest;
import com.kct.jcredit.core_backend.dto.request.RejectReviewRequest;
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
    
    // @GetMapping
    //     @PreAuthorize("hasRole('operator')")
    //     @SecurityRequirement(name = "bearerAuth")
    //     public ResponseEntity<List<ParticipationReviewDetailResponse>> getPendingReviews() {
    //         return ResponseEntity.ok(reviewService.getPendingReviews());
    //     }

    //     @GetMapping("/{participationId}")
    //     @PreAuthorize("hasRole('operator')")
    //     @SecurityRequirement(name = "bearerAuth")
    //     public ResponseEntity<ParticipationReviewDetailResponse> getReviewDetail(
    //             @PathVariable Integer participationId) {
    //         return ResponseEntity.ok(reviewService.getReviewDetail(participationId));
    //     }

    // LIST PENDING (status OPEN) ───────
    @GetMapping
    @PreAuthorize("hasRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "レビュー待ち一覧取得", description = "承認待ちの入会届一覧を取得します。")
    public ResponseEntity<List<ParticipationReviewDetailResponse>> getPendingReviews() {
        return ResponseEntity.ok(reviewService.getPendingReviews());
    }

    // LIST — review other operator login ──
    @GetMapping("/mine")
    @PreAuthorize("hasRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "自分がレビュー可能な一覧取得", description = "自分がまだレビューできる入会届一覧を取得します。")
    public ResponseEntity<List<ParticipationReviewDetailResponse>> getPendingReviewsForMe(
            Authentication authentication) {
        Integer operatorUserId = jwtUtil.extractUserId(authentication.getCredentials().toString());
        return ResponseEntity.ok(reviewService.getPendingReviewsByUserId(operatorUserId));
    }

    // DETAIL 
    @GetMapping("/{participationId}")
    @PreAuthorize("hasRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "レビュー詳細取得", description = "指定した参加情報の詳細を取得します。")
    public ResponseEntity<ParticipationReviewDetailResponse> getReviewDetail(
            @PathVariable Integer participationId) {
        return ResponseEntity.ok(reviewService.getReviewDetail(participationId));
    }

    //  APPROVE 
    @PostMapping("/{participationId}/approve")
    @PreAuthorize("hasRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "参加申請承認", description = "指定された参加申請を承認します（一次・二次確認共通）。")
    public ResponseEntity<BaseResponse> approveReview(
            Authentication authentication,
            @PathVariable Integer participationId,
            @RequestBody @Valid ApproveReviewRequest request) {

        Integer operatorUserId = jwtUtil.extractUserId(authentication.getCredentials().toString());
        reviewService.approveReview(participationId, operatorUserId, request);
        return ResponseEntity.ok(BaseResponse.success("承認しました。"));
    }

    //  REJECT 
    @PostMapping("/{participationId}/reject")
    @PreAuthorize("hasRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "参加申請却下", description = "指定された参加申請を却下します。")
    public ResponseEntity<BaseResponse> rejectReview(
            Authentication authentication,
            @PathVariable Integer participationId,
            @RequestBody @Valid RejectReviewRequest request) {

        Integer operatorUserId = jwtUtil.extractUserId(authentication.getCredentials().toString());
        reviewService.rejectReview(participationId, operatorUserId, request);
        return ResponseEntity.ok(BaseResponse.success("却下しました。"));
    }
}
