package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.request.CompanyListQuery;
import com.kct.jcredit.core_backend.dto.request.CreateCompanySuspensionRequest;
import com.kct.jcredit.core_backend.dto.request.RejectCompanyRequest;
import com.kct.jcredit.core_backend.dto.request.ResolveCompanySuspensionRequest;
import com.kct.jcredit.core_backend.dto.request.ReviewParticipationRequest;
import com.kct.jcredit.core_backend.dto.request.ApproveReviewRequest;
import com.kct.jcredit.core_backend.dto.request.RejectReviewRequest;
import com.kct.jcredit.core_backend.dto.response.AdminCompanyListResponse;
import com.kct.jcredit.core_backend.dto.response.AdminCompanyDetailResponse;
import com.kct.jcredit.core_backend.dto.response.ApproveCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.CompanySuspensionListResponse;
import com.kct.jcredit.core_backend.dto.response.CompanySuspensionResponse;
import com.kct.jcredit.core_backend.dto.response.RejectCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.ParticipationReviewDetailResponse;
import com.kct.jcredit.core_backend.service.AdminCompanyQueryService;
import com.kct.jcredit.core_backend.service.CompanyApprovalService;
import com.kct.jcredit.core_backend.service.CompanyLifecycleService;
import com.kct.jcredit.core_backend.service.ParticipationReviewService;
import com.kct.jcredit.core_backend.service.SecurityAccountService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes operator-only company lifecycle and search operations.
 */
@RestController
@RequestMapping("/admin/companies")
@Tag(name = "企業管理", description = "企業の承認、却下、一覧取得、利用停止、参加審査を管理するAPIです。")
@RequiredArgsConstructor
@Validated
public class AdminCompanyController {

    private final CompanyApprovalService companyApprovalService;
    private final AdminCompanyQueryService adminCompanyQueryService;
    private final CompanyLifecycleService companyLifecycleService;
    private final ParticipationReviewService reviewService;
    private final SecurityAccountService securityAccountService;
    private final JwtUtil jwtUtil;

    /**
     * Approves a pending company and provisions its master account.
     */
    @PostMapping("/{companyId}/approve")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "企業登録承認", description = "申請中の企業を承認し、マスターユーザーを作成します。")
    public ResponseEntity<ApproveCompanyResponse> approveCompany(@PathVariable String companyId) {
        ApproveCompanyResponse response = companyApprovalService.approveCompany(companyId);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    /**
     * Rejects a pending company registration with an auditable explanation.
     */
    @PostMapping("/{companyId}/reject")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "企業登録却下", description = "申請中の企業登録を却下します。")
    public ResponseEntity<RejectCompanyResponse> rejectCompany(
            @PathVariable String companyId,
            @RequestBody @Valid RejectCompanyRequest request) {
        RejectCompanyResponse response = companyApprovalService.rejectCompany(
                companyId,
                request.getRejectionReason()
        );
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    /**
     * Returns a unified, filtered, and paginated company list.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "企業一覧取得", description = "企業ステータス、検索条件、日付範囲で企業一覧を取得します。")
    public ResponseEntity<AdminCompanyListResponse> getCompanies(
            @Valid @ModelAttribute CompanyListQuery query) {
        AdminCompanyListResponse response = adminCompanyQueryService.findCompanies(query);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/{companyId}/master-setup-link/resend")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApproveCompanyResponse> resendMasterSetupLink(@PathVariable String companyId) {
        ApproveCompanyResponse response = companyApprovalService.resendMasterSetupLink(companyId);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/{companyId}/password-reset-link")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "マスターアカウントパスワードリセットリンク発行", description = "指定した企業のマスターアカウントに対して、パスワードリセット用のアクションリンクを発行します。")
    public ResponseEntity<BaseResponse> requestMasterPasswordReset(
            Authentication authentication,
            @PathVariable String companyId) {
        String actor = jwtUtil.extractUsername(authentication.getCredentials().toString());
        BaseResponse response = securityAccountService.operatorRequestMasterPasswordReset(companyId, actor);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    /**
     * Returns master-account, company, user, and site details for one participating company.
     */
    @GetMapping("/{companyId}")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "企業詳細取得", description = "指定した企業のマスターアカウント、ユーザー、拠点情報を取得します。")
    public ResponseEntity<AdminCompanyDetailResponse> getCompanyDetail(
            @PathVariable String companyId) {
        AdminCompanyDetailResponse response = adminCompanyQueryService.findCompanyDetail(companyId);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    /**
     * Creates one operational blocker for an active or already suspended company.
     */
    @PostMapping("/{companyId}/suspensions")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "企業利用停止登録", description = "指定した理由で企業の利用停止を登録します。")
    public ResponseEntity<CompanySuspensionResponse> createSuspension(
            @PathVariable String companyId,
            @Valid @RequestBody CreateCompanySuspensionRequest request) {
        CompanySuspensionResponse response = companyLifecycleService.createSuspension(companyId, request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    /**
     * Returns active blockers or complete suspension history for a company.
     */
    @GetMapping("/{companyId}/suspensions")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "企業停止履歴取得", description = "企業の有効な停止理由または停止履歴を取得します。")
    public ResponseEntity<CompanySuspensionListResponse> getSuspensions(
            @PathVariable String companyId,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "ページ番号は1以上で指定してください。") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "表示件数は1以上で指定してください。")
            @Max(value = 100, message = "表示件数は100以下で指定してください。") int limit) {
        CompanySuspensionListResponse response = companyLifecycleService.findSuspensions(
                companyId,
                activeOnly,
                page,
                limit
        );
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    /**
     * Resolves one blocker and conditionally reactivates the company.
     */
    @PostMapping("/{companyId}/suspensions/{suspensionId}/resolve")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "企業停止解除", description = "指定した停止理由を解除し、残りの停止理由を確認します。")
    public ResponseEntity<CompanySuspensionResponse> resolveSuspension(
            @PathVariable String companyId,
            @PathVariable Integer suspensionId,
            @Valid @RequestBody ResolveCompanySuspensionRequest request) {
        CompanySuspensionResponse response = companyLifecycleService.resolveSuspension(
                companyId,
                suspensionId,
                request
        );
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    // ─────────────────────────────────────────────────────────────
    // PARTICIPATION REVIEW
    // Note: namespaced under /participation to avoid colliding with the
    // /{companyId} single-segment mapping above.
    // ─────────────────────────────────────────────────────────────

    // ── LIST — semua item yang masih PENDING (status OPEN) ───────
    @GetMapping("participate/pending")
    @PreAuthorize("hasRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "レビュー待ち一覧取得", description = "承認待ちの入会届一覧を取得します。")
    public ResponseEntity<List<ParticipationReviewDetailResponse>> getPendingReviews() {
        return ResponseEntity.ok(reviewService.getPendingReviews());
    }

    // ── LIST — item yang MASIH BISA di-review oleh operator yang login ──
    // (exclude item yang dia sendiri sudah jadi primary reviewer)
    @GetMapping("participate/mine")
    @PreAuthorize("hasRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "自分がレビュー可能な一覧取得", description = "自分がまだレビューできる入会届一覧を取得します。")
    public ResponseEntity<List<ParticipationReviewDetailResponse>> getPendingReviewsForMe(
            Authentication authentication) {
        Integer operatorUserId = jwtUtil.extractUserId(authentication.getCredentials().toString());
        return ResponseEntity.ok(reviewService.getPendingReviewsByUserId(operatorUserId));
    }

    // ── DETAIL ─────────────────────────────────────────────────
    @GetMapping("participate/{participationId}")
    @PreAuthorize("hasRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "レビュー詳細取得", description = "指定した参加情報の詳細を取得します。")
    public ResponseEntity<ParticipationReviewDetailResponse> getReviewDetail(
            @PathVariable Integer participationId) {
        return ResponseEntity.ok(reviewService.getReviewDetail(participationId));
    }

    // ── APPROVE ────────────────────────────────────────────────
    @PostMapping("participation/{participationId}/approve")
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

    // ── REJECT ─────────────────────────────────────────────────
    @PostMapping("participation/{participationId}/reject")
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
