package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.ApproveDeletionRequest;
import com.kct.jcredit.core_backend.dto.request.CreateDeletionRequest;
import com.kct.jcredit.core_backend.dto.request.ReactivationRequest;
import com.kct.jcredit.core_backend.dto.request.RejectDeletionRequest;
import com.kct.jcredit.core_backend.dto.request.SuspendCompanyRequest;
import com.kct.jcredit.core_backend.dto.request.UpdatePartiCompanyRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyInfoValidateRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyMethodologyValidateRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyRegisterRequest;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.DeleteCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.DeletionRequestResponse;
import com.kct.jcredit.core_backend.dto.response.ReactivationResponse;
import com.kct.jcredit.core_backend.dto.response.SuspendCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.UpdatePartiCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.UserListResponse;

import com.kct.jcredit.core_backend.dto.response.details.CompanyInfoResponse;
import com.kct.jcredit.core_backend.dto.response.details.PartiCompanyResponseDetails;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.enums.SubscriptionStatus;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.service.CompanyDeletionService;
import com.kct.jcredit.core_backend.service.PartiCompanyRegisterService;
import com.kct.jcredit.core_backend.service.PartiCompanyService;
import com.kct.jcredit.core_backend.service.PaymentGatewayService;
import com.kct.jcredit.core_backend.service.SubscriptionService;
import com.kct.jcredit.core_backend.service.UserService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
public class PartiCompanyController {

    private final PartiCompanyService companyService;
    private final ParticipatingCompanyRepository companyRepository;
    private final SubscriptionService subscriptionService;
    private final PaymentGatewayService paymentGatewayService;
    private final PartiCompanyRegisterService registerService;
    private final JwtUtil jwtUtil;
    private final CompanyDeletionService companyDeletionService;

    @PatchMapping
    @PreAuthorize("hasAnyRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "参加会社情報更新", description = "ログイン会社の参加会社情報を更新します。")
    public ResponseEntity<UpdatePartiCompanyResponse> updateCompany(
            @RequestBody @Valid UpdatePartiCompanyRequest request) {

        UpdatePartiCompanyResponse response = companyService.updateCompany(request.getCompanyId(), request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/signup")
    @Operation(summary = "参加会社サインアップ", description = "新しい参加会社の登録申請を作成します。")
    public ResponseEntity<PartiCompanyResponseDetails> signup(
            @Valid @RequestBody PartiCompanyRegisterRequest request) {
        PartiCompanyResponseDetails response = registerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/signup/company-info/validate")
    @Operation(summary = "会社情報入力検証", description = "サインアップ時の会社情報を保存せずに検証します。")
    public ResponseEntity<BaseResponse> validateSignupCompanyInfo(
            @Valid @RequestBody PartiCompanyInfoValidateRequest request) {
        registerService.validateCompanyInfo(request);
        return ResponseEntity.ok(BaseResponse.success(ResponseConstants.MSG_VALIDATION_SUCCESS));
    }

    @PostMapping("/signup/methodology/validate")
    @Operation(summary = "方法論入力検証", description = "サインアップ時の方法論情報を保存せずに検証します。")
    public ResponseEntity<BaseResponse> validateSignupMethodology(
            @Valid @RequestBody PartiCompanyMethodologyValidateRequest request) {
        return ResponseEntity.ok(BaseResponse.success(ResponseConstants.MSG_VALIDATION_SUCCESS));
    }

    @PostMapping("/suspend")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "参加会社停止", description = "指定された参加会社の利用を停止します。")
    public ResponseEntity<SuspendCompanyResponse> suspendCompany(
            @RequestBody @Valid SuspendCompanyRequest request) {

        Optional<ParticipatingCompany> companyOpt = companyRepository.findByCompanyId(request.getCompanyId());
        if (companyOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(SuspendCompanyResponse.builder()
                    .statusCode(ResponseConstants.NOT_FOUND)
                    .message(ResponseConstants.MSG_NOT_FOUND)
                    .build());
        }

        ParticipatingCompany company = companyOpt.get();
        if (company.getSubscriptionStatus() == SubscriptionStatus.SUSPENDED) {
            return ResponseEntity.badRequest().body(SuspendCompanyResponse.builder()
                    .statusCode(ResponseConstants.BAD_REQUEST)
                    .message(ResponseConstants.MSG_COMPANY_ALREADY_SUSPENDED)
                    .build());
        }

        subscriptionService.suspendCompanyManually(request.getCompanyId(), request.getReason());
        
        return ResponseEntity.ok(SuspendCompanyResponse.builder()
                .statusCode(ResponseConstants.OK)
                .message(ResponseConstants.MSG_SUSPEND_SUCCESS)
                .companyId(company.getCompanyId())
                .companyName(company.getNameJa())
                .build());
    }

    @PostMapping("/reactivate")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "参加会社再有効化", description = "停止中の参加会社を再有効化します。")
    public ResponseEntity<ReactivationResponse> reactivateCompany(
            @RequestBody @Valid ReactivationRequest request) {

        Optional<ParticipatingCompany> companyOpt = companyRepository.findByCompanyId(request.getCompanyId());
        if (companyOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(ReactivationResponse.builder()
                    .statusCode(ResponseConstants.NOT_FOUND)
                    .message(ResponseConstants.MSG_NOT_FOUND)
                    .build());
        }

        ParticipatingCompany company = companyOpt.get();
        if (company.getSubscriptionStatus() != SubscriptionStatus.SUSPENDED) {
            return ResponseEntity.badRequest().body(ReactivationResponse.builder()
                    .statusCode(ResponseConstants.BAD_REQUEST)
                    .message(ResponseConstants.MSG_COMPANY_NOT_SUSPENDED)
                    .build());
        }

        String paymentMethod = request.getPaymentMethod();
        if (paymentMethod == null || paymentMethod.isEmpty()) {
            paymentMethod = company.getPaymentMethod();
            if (paymentMethod == null || paymentMethod.isEmpty()) {
                paymentMethod = "CREDIT_CARD";
            }
        }

        BigDecimal amount = new BigDecimal("50000.00");
        PaymentGatewayService.PaymentResult paymentResult = paymentGatewayService.processPayment(
                company.getCompanyId(), paymentMethod, amount);

        if (paymentResult.success()) {
            subscriptionService.reactivateCompanyWithPayment(company, paymentMethod, paymentResult);
            return ResponseEntity.ok(ReactivationResponse.builder()
                    .statusCode(ResponseConstants.OK)
                    .message(ResponseConstants.MSG_REACTIVATION_SUCCESS)
                    .companyId(company.getCompanyId())
                    .companyName(company.getNameJa())
                    .isActive(true)
                    .isSuspended(false)
                    .build());
        } else {
            return ResponseEntity.status(402).body(ReactivationResponse.builder()
                    .statusCode(ResponseConstants.BAD_REQUEST)
                    .message(ResponseConstants.MSG_PAYMENT_FAILED + paymentResult.failureReason())
                    .companyId(company.getCompanyId())
                    .companyName(company.getNameJa())
                    .isActive(false)
                    .isSuspended(true)
                    .build());
        }
    }

    @PostMapping("/{companyId}/deletion-request")
    @PreAuthorize("hasAnyRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "会社削除申請作成", description = "指定された参加会社の削除申請を作成します。")
    public ResponseEntity<DeletionRequestResponse> createDeletionRequest(
            Authentication authentication,
            @PathVariable String companyId,
            @RequestBody @Valid CreateDeletionRequest request) {
        String operatorId = jwtUtil.extractUsername(authentication.getCredentials().toString());
        DeletionRequestResponse response = companyDeletionService.createDeletionRequest(
                companyId, operatorId, request.getReason());
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/{companyId}/deletion-approve")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "会社削除申請承認", description = "指定された参加会社の削除申請を承認します。")
    public ResponseEntity<DeletionRequestResponse> approveDeletionRequest(
            Authentication authentication,
            @PathVariable String companyId,
            @RequestBody @Valid ApproveDeletionRequest request) {
        String operatorId = jwtUtil.extractUsername(authentication.getCredentials().toString());
        DeletionRequestResponse response = companyDeletionService.approveDeletionRequest(
                companyId, operatorId);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/{companyId}/deletion-reject")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "会社削除申請却下", description = "指定された参加会社の削除申請を却下します。")
    public ResponseEntity<DeletionRequestResponse> rejectDeletionRequest(
            Authentication authentication,
            @PathVariable String companyId,
            @RequestBody @Valid RejectDeletionRequest request) {
        String operatorId = jwtUtil.extractUsername(authentication.getCredentials().toString());
        DeletionRequestResponse response = companyDeletionService.rejectDeletionRequest(
                companyId, operatorId, request.getRejectionReason());
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }
}
