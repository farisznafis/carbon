package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.request.CompanyListQuery;
import com.kct.jcredit.core_backend.dto.request.CreateCompanySuspensionRequest;
import com.kct.jcredit.core_backend.dto.request.CreateUserRequest;
import com.kct.jcredit.core_backend.dto.request.RejectCompanyRequest;
import com.kct.jcredit.core_backend.dto.request.ResolveCompanySuspensionRequest;
import com.kct.jcredit.core_backend.dto.request.ReviewParticipationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.ApproveReviewRequest;
import com.kct.jcredit.core_backend.dto.request.RejectReviewRequest;
import com.kct.jcredit.core_backend.dto.request.UpdateUserRequest;
import com.kct.jcredit.core_backend.dto.response.AdminCompanyListResponse;
import com.kct.jcredit.core_backend.dto.response.AdminCompanyDetailResponse;
import com.kct.jcredit.core_backend.dto.response.ApproveCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.CompanySuspensionListResponse;
import com.kct.jcredit.core_backend.dto.response.CompanySuspensionResponse;
import com.kct.jcredit.core_backend.dto.response.CreateUserResponse;
import com.kct.jcredit.core_backend.dto.response.DocuSignEnvelopeResponse;
import com.kct.jcredit.core_backend.dto.response.UpdateUserResponse;
import com.kct.jcredit.core_backend.dto.response.UserListResponse;
import com.kct.jcredit.core_backend.dto.response.UserProfileResponse;
import com.kct.jcredit.core_backend.dto.response.RejectCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.ParticipationReviewDetailResponse;
import com.kct.jcredit.core_backend.dto.response.DocuSignEnvelopeResult;
import com.kct.jcredit.core_backend.service.AdminCompanyQueryService;
import com.kct.jcredit.core_backend.service.CompanyApprovalService;
import com.kct.jcredit.core_backend.service.CompanyLifecycleService;
import com.kct.jcredit.core_backend.service.DocuSignService;
import com.kct.jcredit.core_backend.service.DocuSignService;
import com.kct.jcredit.core_backend.service.OperatorUserService;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import javax.crypto.spec.SecretKeySpec;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    private final OperatorUserService operatorUserService;
    private final CompanyApprovalService companyApprovalService;
    private final AdminCompanyQueryService adminCompanyQueryService;
    private final CompanyLifecycleService companyLifecycleService;
    private final ParticipationReviewService reviewService;
    private final SecurityAccountService securityAccountService;
    private final JwtUtil jwtUtil;
    private final DocuSignService docuSignService;


    /**
     * Returns a unified, filtered, and paginated company list.
     */
    @GetMapping
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PARTICIPATING_COMPANY_READ.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "企業一覧取得", description = "企業ステータス、検索条件、日付範囲で企業一覧を取得します。")
    public ResponseEntity<AdminCompanyListResponse> getCompanies(
            @Valid @ModelAttribute CompanyListQuery query) {
        AdminCompanyListResponse response = adminCompanyQueryService.findCompanies(query);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/{companyId}/master-setup-link/resend")
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PARTICIPATING_COMPANY_APPLICATION_REVIEW.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "マスターアカウントセットアップリンク再送信", description = "指定した企業のマスターアカウントに対して、セットアップ用のアクションリンクを再送信します。")
    public ResponseEntity<ApproveCompanyResponse> resendMasterSetupLink(@PathVariable String companyId) {
        ApproveCompanyResponse response = companyApprovalService.resendMasterSetupLink(companyId);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/{companyId}/password-reset-link")
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).MASTER_USER_PASSWORD_RESET.name()))")
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
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PARTICIPATING_COMPANY_READ.name()))")
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
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PARTICIPATING_COMPANY_SUSPEND.name()))")
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
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PARTICIPATING_COMPANY_SUSPEND.name()))")
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
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PARTICIPATING_COMPANY_SUSPEND.name()))")
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

    // ── LIST ALL — (status OPEN) ───────
    @GetMapping("participate/pending")
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PARTICIPATING_COMPANY_APPLICATION_REVIEW.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "レビュー待ち一覧取得", description = "承認待ちの入会届一覧を取得します。")
    public ResponseEntity<List<ParticipationReviewDetailResponse>> getPendingReviews() {
        return ResponseEntity.ok(reviewService.getPendingReviews());
    }

    // ── LIST — 
    @GetMapping("participate/open")
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PARTICIPATING_COMPANY_APPLICATION_REVIEW.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "自分がレビュー可能な一覧取得", description = "自分がまだレビューできる入会届一覧を取得します。")
    public ResponseEntity<List<ParticipationReviewDetailResponse>> getPendingReviewsForMe(
            Authentication authentication) {
        Integer operatorUserId = jwtUtil.extractUserId(authentication.getCredentials().toString());
        return ResponseEntity.ok(reviewService.getPendingReviewsByUserId(operatorUserId));
    }

    @GetMapping("participations/approved")
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PARTICIPATING_COMPANY_APPLICATION_REVIEW.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "承認済み入会届一覧取得",
            description = "審査承認済み（入会届PDF生成済み）の一覧を取得します。")
    public ResponseEntity<List<ParticipationReviewDetailResponse>> getApprovedParticipations() {
        return ResponseEntity.ok(reviewService.getApprovedParticipations());
    }

    @GetMapping("participations/rejected")
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PARTICIPATING_COMPANY_APPLICATION_REVIEW.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "却下済み入会届一覧取得",
            description = "却下され、まだ再申請されていない入会届の一覧を取得します。")
    public ResponseEntity<List<ParticipationReviewDetailResponse>> getRejectedParticipations() {
        return ResponseEntity.ok(reviewService.getRejectedParticipations());
    }

    // ── DETAIL ─────────────────────────────────────────────────
    @GetMapping("participate/{participationId}")
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PARTICIPATING_COMPANY_APPLICATION_REVIEW.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "レビュー詳細取得", description = "指定した参加情報の詳細を取得します。")
    public ResponseEntity<ParticipationReviewDetailResponse> getReviewDetail(
            @PathVariable Integer participationId) {
        return ResponseEntity.ok(reviewService.getReviewDetail(participationId));
    }

    // ── APPROVE ────────────────────────────────────────────────
    @PostMapping("participation/{participationId}/approve")
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PARTICIPATING_COMPANY_APPLICATION_REVIEW.name()))")
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
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PARTICIPATING_COMPANY_APPLICATION_REVIEW.name()))")
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
    
    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> testToken() {
        try {
            String token = docuSignService.getAccessToken();
            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "tokenPreview", token.substring(0, 20) + "..."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/send-envelope")
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PARTICIPATING_COMPANY_APPLICATION_REVIEW.name()))")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, String>> testSendEnvelope() throws Exception {

        byte[] dummyPdf = Files.readAllBytes(Paths.get("C:/secrets/docusign/test.pdf"));

        DocuSignEnvelopeResult result = docuSignService.sendEnvelopeForSignature(
                dummyPdf,
                "yejacox735@prodbits.com", 
                "Test Sankasha",
                "test_document.pdf"
        );

        return ResponseEntity.ok(Map.of(
                "envelopeId", result.getEnvelopeId(),
                "status", result.getStatus()
        ));
    }

    // @PostMapping("/envelope-completed")
    // public ResponseEntity<Void> handleWebhook(
    //         @RequestBody String rawPayload,
    //         @RequestHeader(value = "X-DocuSign-Signature-1", required = false) String signature) {

    //     // ⚠️ HMAC検証 — 本番運用では必須。今は開発中のため一旦ログ出力のみ
    //     if (hmacSecret != null && !hmacSecret.isBlank()) {
    //         boolean valid = verifyHmacSignature(rawPayload, signature, hmacSecret);
    //         if (!valid) {
    //             log.warn("[DocuSign Webhook] HMAC検証失敗、リクエストを拒否");
    //             return ResponseEntity.status(401).build();
    //         }
    //     }

    //     try {
    //         ObjectMapper mapper = new ObjectMapper();
    //         Map<String, Object> payload = mapper.readValue(rawPayload, Map.class);

    //         String envelopeId = (String) payload.get("envelopeId");
    //         String status = (String) payload.get("status");

    //         log.info("[DocuSign Webhook] 受信。envelopeId={}, status={}", envelopeId, status);

    //         if ("completed".equalsIgnoreCase(status)) {
    //             pdfService.handleEnvelopeCompleted(envelopeId);
    //         }

    //     } catch (Exception e) {
    //         log.error("[DocuSign Webhook] 処理失敗。error={}", e.getMessage(), e);
    //         // Tetap return 200 supaya DocuSign tidak retry berkali-kali untuk error yang sama
    //     }

    //     return ResponseEntity.ok().build();
    // }

    // private boolean verifyHmacSignature(String payload, String signature, String secret) {
    //     try {
    //         Mac mac = Mac.getInstance("HmacSHA256");
    //         mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    //         byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    //         String computed = Base64.getEncoder().encodeToString(hash);
    //         return computed.equals(signature);
    //     } catch (Exception e) {
    //         log.error("HMAC verification error: {}", e.getMessage());
    //         return false;
    //     }
    // }

    // Operator User Management

    // ── Operator User List ─────────────────────────────────────────────────
    //get a list of operator users that are associated with the current users operating companyiD
    // admin user has superiority and can make operators that should be able to access the admin tool and manage companies, users, and sites
    // only admin can accept new users and requests to join projects 


    // ── Operator Create ─────────────────────────────────────────────────
    //create a new operating user and set their permissions to different parts of the admin tool page
    //permissions set through many to many table relation

    @GetMapping("/operator-user")
    @PreAuthorize("hasRole('admin') or (hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).OPERATOR_USER_READ.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "運用者一覧取得", description = "ログイン中の管理企業に所属する運用者ユーザー一覧を取得します。")
    public ResponseEntity<UserListResponse> getOperatorUsers(Authentication authentication) {
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());
        UserListResponse response = operatorUserService.getOperatorUsersByCompany(companyId);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/operator-user")
    @PreAuthorize("hasRole('admin') or (hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).OPERATOR_USER_CREATE.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "運用者ユーザー作成", description = "ログイン中の管理企業に新しい運用者ユーザーを作成します。")
    public ResponseEntity<CreateUserResponse> createUser(
            Authentication authentication,
            @RequestBody @Valid CreateUserRequest request) {
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());
        String username = jwtUtil.extractUsername(authentication.getCredentials().toString());

        CreateUserResponse response = operatorUserService.createOperatorUser(companyId, username, request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PatchMapping("/operator-user/{userId}")
    @PreAuthorize("hasRole('admin') or (hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).OPERATOR_USER_UPDATE.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "運用者ユーザー更新", description = "運用者ユーザーの基本情報と権限を更新します。")
    public ResponseEntity<UpdateUserResponse> updateOperatorUser(
            Authentication authentication,
            @PathVariable Integer userId,
            @RequestBody @Valid UpdateUserRequest request) {
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());
        String username = jwtUtil.extractUsername(authentication.getCredentials().toString());
        request.setUserId(userId);

        UpdateUserResponse response = operatorUserService.updateUser(companyId, username, request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @GetMapping("operator-user/{userId}")
    @PreAuthorize("hasAnyRole('operator', 'admin')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "運用者ユーザープロファイル取得", description = "運用者ユーザーの基本情報と権限を取得します。")
    public ResponseEntity<UserProfileResponse> getOperatorUserProfile(
            Authentication authentication,
            @PathVariable Integer userId) {
    String role = jwtUtil.extractRole(authentication.getCredentials().toString());
    Integer currentId = jwtUtil.extractUserId(authentication.getCredentials().toString());

    boolean isAdmin = "admin".equalsIgnoreCase(role);
    boolean isSelf = currentId.equals(userId);

    if (!isAdmin && !isSelf) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
    }

    UserProfileResponse response = operatorUserService.getOperatorUserProfileById(
            userId,
            currentId,
            role,
            jwtUtil.extractCompanyId(authentication.getCredentials().toString())
    );
    return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @RestController
    @RequestMapping("/test/docusign")
    @RequiredArgsConstructor
    public class DocuSignTestController {

        private final DocuSignService docuSignService;

        @GetMapping("/token")
        public ResponseEntity<Map<String, String>> testToken() {
            try {
                String token = docuSignService.getAccessToken();
                return ResponseEntity.ok(Map.of(
                        "status", "OK",
                        "tokenPreview", token.substring(0, 20) + "..."
                ));
            } catch (Exception e) {
                return ResponseEntity.status(500).body(Map.of(
                        "status", "ERROR",
                        "message", e.getMessage()
                ));
            }
        }

    //     @PostMapping("/send-envelope")
    //     public ResponseEntity<Map<String, String>> testSendEnvelope() throws Exception {
    //         byte[] dummyPdf = Files.readAllBytes(Paths.get("C:/シャーナズ/【03】 Projek/docusign/template.pdf"));

    //         DocuSignEnvelopeResponse result = docuSignService.sendEnvelopeForSignature(
    //                 dummyPdf,
    //                 "wqzdzb5506@minitts.net", 
    //                 "Test Sankasha",
    //                 "test_document.pdf"
    //         );

    //         return ResponseEntity.ok(Map.of(
    //                 "envelopeId", result.getEnvelopeId(),
    //                 "status", result.getStatus()
    //         ));
    //     }
     }
}
