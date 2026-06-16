package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.ReactivationRequest;
import com.kct.jcredit.core_backend.dto.request.SuspendCompanyRequest;
import com.kct.jcredit.core_backend.dto.request.UpdatePartiCompanyRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyRegisterRequest;
import com.kct.jcredit.core_backend.dto.response.ReactivationResponse;
import com.kct.jcredit.core_backend.dto.response.SuspendCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.UpdatePartiCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.UserListResponse;

import com.kct.jcredit.core_backend.dto.response.details.CompanyInfoResponse;
import com.kct.jcredit.core_backend.dto.response.details.PartiCompanyResponseDetails;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.enums.SubscriptionStatus;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.service.PartiCompanyRegisterService;
import com.kct.jcredit.core_backend.service.PartiCompanyService;
import com.kct.jcredit.core_backend.service.PaymentGatewayService;
import com.kct.jcredit.core_backend.service.SubscriptionService;
import com.kct.jcredit.core_backend.service.UserService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;
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

    @PatchMapping
    @PreAuthorize("hasAnyRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UpdatePartiCompanyResponse> updateCompany(
            @RequestBody @Valid UpdatePartiCompanyRequest request) {

        UpdatePartiCompanyResponse response = companyService.updateCompany(request.getCompanyId(), request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/signup")
    public ResponseEntity<PartiCompanyResponseDetails> signup(
            @Valid @RequestBody PartiCompanyRegisterRequest request) {
        PartiCompanyResponseDetails response = registerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/suspend")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
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
}
