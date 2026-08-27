package com.kct.jcredit.core_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.ApproveDeletionRequest;
import com.kct.jcredit.core_backend.dto.request.CreateDeletionRequest;
import com.kct.jcredit.core_backend.dto.request.EmailOtpStartRequest;
import com.kct.jcredit.core_backend.dto.request.OtpResendRequest;
import com.kct.jcredit.core_backend.dto.request.OtpVerifyRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyInfoValidateRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyMethodologyValidateRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyRegisterRequest;
import com.kct.jcredit.core_backend.dto.request.RejectDeletionRequest;
import com.kct.jcredit.core_backend.dto.request.UpdatePartiCompanyRequest;
import com.kct.jcredit.core_backend.dto.response.DeletionRequestResponse;
import com.kct.jcredit.core_backend.dto.response.OtpChallengeResponse;
import com.kct.jcredit.core_backend.dto.response.UpdatePartiCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.PartiCompanyResponseDetails;
import com.kct.jcredit.core_backend.service.CompanyDeletionService;
import com.kct.jcredit.core_backend.service.CompanySecurityFlowService;
import com.kct.jcredit.core_backend.service.PartiCompanyRegisterService;
import com.kct.jcredit.core_backend.service.PartiCompanyService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Exposes participating-company self-service and signup operations.
 * 参加企業のセルフサービスやサインアップ操作を公開します。
 */
@RestController
@RequestMapping("/company")
@Tag(name = "参加企業", description = "参加企業の登録、更新、削除申請を管理するAPIです。")
@RequiredArgsConstructor
public class PartiCompanyController {

    private final PartiCompanyService companyService;
    private final PartiCompanyRegisterService registerService;
    private final JwtUtil jwtUtil;
    private final CompanyDeletionService companyDeletionService;
    private final CompanySecurityFlowService companySecurityFlowService;

    /**
     * Updates the authenticated master user's company information.
     * 認証されたマスター ユーザーの会社情報を更新する
     */
    @PatchMapping
    @PreAuthorize("hasAnyRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "参加企業情報更新", description = "ログイン中の参加企業情報を更新します。")
    public ResponseEntity<UpdatePartiCompanyResponse> updateCompany(
            Authentication authentication,
            @RequestBody @Valid UpdatePartiCompanyRequest request) {
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());
        UpdatePartiCompanyResponse response = companyService.updateCompany(companyId, request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    /**
     * Registers a new company application or resubmits a rejected application.
     * 新しい会社の申請を登録するか、却下された申請を再提出します。
     * 
     * テスト新規追加
     */
    @PostMapping("/signup")
    @Operation(summary = "参加企業登録申請", description = "新しい参加企業の登録申請を作成します。")
    public ResponseEntity<PartiCompanyResponseDetails> signup(
            @Valid @RequestBody PartiCompanyRegisterRequest request) {
        PartiCompanyResponseDetails response = registerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    //ワンタイムパスワード送信
    @PostMapping("/signup/email-verification/start")
    public ResponseEntity<OtpChallengeResponse> startSignupEmailVerification(
            @Valid @RequestBody EmailOtpStartRequest request) {
        return ResponseEntity.ok(companySecurityFlowService.startSignupEmailVerification(request.getEmail()));
    }

    //ワンタイムパスワード再送信
    @PostMapping("/signup/email-verification/resend")
    public ResponseEntity<OtpChallengeResponse> resendSignupEmailVerification(
            @Valid @RequestBody OtpResendRequest request) {
        return ResponseEntity.ok(companySecurityFlowService.resendSignupEmailVerification(request.getChallengeId()));
    }

    //ワンタイムパスワード送信→認証完了
    @PostMapping("/signup/email-verification/verify")
    public ResponseEntity<BaseResponse> verifySignupEmail(
            @Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(companySecurityFlowService.verifySignupEmail(
                request.getChallengeId(), request.getCode()));
    }

    //差し戻された会社登録申請の修正用下書きデータを取得する
    @GetMapping("/signup/revision")
    public ResponseEntity<PartiCompanyRegisterRequest> getRejectedSignupRevision(
            @RequestHeader("X-Action-Session") String actionSession) {
        return ResponseEntity.ok(companySecurityFlowService.getRevisionDraft(actionSession));
    }

    //差し戻された会社登録申請を修正して再申請する
    @PutMapping("/signup/revision")
    public ResponseEntity<BaseResponse> reviseRejectedSignup(
            @RequestHeader("X-Action-Session") String actionSession,
            @Valid @RequestBody PartiCompanyRegisterRequest request) {
        return ResponseEntity.ok(companySecurityFlowService.reviseRejectedSignup(actionSession, request));
    }

    /**
     * Validates company identity fields before the final signup request.
     * 最終サインアップリクエストの前に、会社の身元情報を確認する。
     * 
     * テスト新規追加
     */
    @PostMapping("/signup/company-info/validate")
    @Operation(summary = "企業情報事前検証", description = "企業情報を保存せずに検証します。")
    public ResponseEntity<BaseResponse> validateSignupCompanyInfo(
            @Valid @RequestBody PartiCompanyInfoValidateRequest request) {
        registerService.validateCompanyInfo(request);
        return ResponseEntity.ok(BaseResponse.success(ResponseConstants.MSG_VALIDATION_SUCCESS));
    }

    /**
     * Validates methodology fields before the final signup request.
     * 最終サインアップのリクエスト前に、方法論のフィールドを確認する。
     * 
     * テスト新規追加
     */
    @PostMapping("/signup/methodology/validate")
    @Operation(summary = "方法論事前検証", description = "方法論情報を保存せずに検証します。")
    public ResponseEntity<BaseResponse> validateSignupMethodology(
            @Valid @RequestBody PartiCompanyMethodologyValidateRequest request) {
        return ResponseEntity.ok(BaseResponse.success(ResponseConstants.MSG_VALIDATION_SUCCESS));
    }

    /**
     * Creates a deletion request for the authenticated master user's company.
     * 認証済みのマスターユーザーの会社の削除リクエストを作る。
     */
    @PostMapping("/{companyId}/deletion-request")
    @PreAuthorize("hasAnyRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "企業削除申請", description = "参加企業の削除申請を作成します。")
    public ResponseEntity<DeletionRequestResponse> createDeletionRequest(
            Authentication authentication,
            @PathVariable String companyId,
            @RequestBody @Valid CreateDeletionRequest request) {
        String operatorId = jwtUtil.extractUsername(authentication.getCredentials().toString());
        DeletionRequestResponse response = companyDeletionService.createDeletionRequest(
                companyId,
                operatorId,
                request.getReason()
        );
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    /**
     * Approves a pending company deletion request.
     * 保留中の会社削除リクエストを承認する。
     */
    @PostMapping("/{companyId}/deletion-approve")
    @PreAuthorize("hasAnyRole('admin')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "企業削除申請承認", description = "指定した企業の削除申請を承認します。")
    public ResponseEntity<DeletionRequestResponse> approveDeletionRequest(
            Authentication authentication,
            @PathVariable String companyId,
            @RequestBody @Valid ApproveDeletionRequest request) {
        String operatorId = jwtUtil.extractUsername(authentication.getCredentials().toString());
        DeletionRequestResponse response = companyDeletionService.approveDeletionRequest(companyId, operatorId);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    /**
     * Rejects a pending company deletion request.
     * 保留中の会社削除リクエストを拒否する。
     */
    @PostMapping("/{companyId}/deletion-reject")
    @PreAuthorize("hasAnyRole('admin')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "企業削除申請却下", description = "指定した企業の削除申請を却下します。")
    public ResponseEntity<DeletionRequestResponse> rejectDeletionRequest(
            Authentication authentication,
            @PathVariable String companyId,
            @RequestBody @Valid RejectDeletionRequest request) {
        String operatorId = jwtUtil.extractUsername(authentication.getCredentials().toString());
        DeletionRequestResponse response = companyDeletionService.rejectDeletionRequest(
                companyId,
                operatorId,
                request.getRejectionReason()
        );
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }
}
