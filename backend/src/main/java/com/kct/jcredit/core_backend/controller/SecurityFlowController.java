package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.request.ActionLinkExchangeRequest;
import com.kct.jcredit.core_backend.dto.request.CompletePasswordResetRequest;
import com.kct.jcredit.core_backend.dto.request.EmailOtpSettingChallengeRequest;
import com.kct.jcredit.core_backend.dto.request.ForgotPasswordRequest;
import com.kct.jcredit.core_backend.dto.request.MasterAccountSetupRequest;
import com.kct.jcredit.core_backend.dto.request.OtpResendRequest;
import com.kct.jcredit.core_backend.dto.request.OtpVerifyRequest;
import com.kct.jcredit.core_backend.dto.response.ActionLinkExchangeResponse;
import com.kct.jcredit.core_backend.dto.response.AdminLoginResponse;
import com.kct.jcredit.core_backend.dto.response.LoginResponse;
import com.kct.jcredit.core_backend.dto.response.OtpChallengeResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.service.SecurityAccountService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import com.kct.jcredit.core_backend.enums.OtpPurpose;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class SecurityFlowController {
    private final SecurityAccountService securityAccountService;
    private final JwtUtil jwtUtil;

    @PostMapping("/action-links/exchange")
    public ResponseEntity<ActionLinkExchangeResponse> exchangeActionLink(
            @Valid @RequestBody ActionLinkExchangeRequest request) {
        return ResponseEntity.ok(securityAccountService.exchangeActionLink(request.getToken()));
    }

    @PostMapping("/master-account/setup")
    public ResponseEntity<BaseResponse> setupMasterAccount(
            @RequestHeader("X-Action-Session") String actionSession,
            @Valid @RequestBody MasterAccountSetupRequest request) {
        return ResponseEntity.ok(securityAccountService.setupMasterAccount(
                actionSession, request.getUsername(), request.getPassword()));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<BaseResponse> requestPasswordReset(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(securityAccountService.requestPasswordReset(request));
    }

    @PostMapping("/password-reset/complete")
    public ResponseEntity<BaseResponse> completePasswordReset(
            @RequestHeader("X-Action-Session") String actionSession,
            @Valid @RequestBody CompletePasswordResetRequest request) {
        BaseResponse response = securityAccountService.completePasswordReset(
                actionSession, request.getNewPassword());
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/user/login/email-otp/verify")
    public ResponseEntity<LoginResponse> verifyParticipatingLogin(
            @Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(securityAccountService.verifyParticipatingLogin(
                request.getChallengeId(), request.getCode()));
    }

    @PostMapping("/admin/login/email-otp/verify")
    public ResponseEntity<AdminLoginResponse> verifyOperatingLogin(
            @Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(securityAccountService.verifyOperatingLogin(
                request.getChallengeId(), request.getCode()));
    }

    @PostMapping("/user/login/email-otp/resend")
    public ResponseEntity<OtpChallengeResponse> resendParticipatingLogin(
            @Valid @RequestBody OtpResendRequest request) {
        return ResponseEntity.ok(securityAccountService.resendLoginOtp(
                request.getChallengeId(), OtpPurpose.PARTICIPATING_LOGIN));
    }

    @PostMapping("/admin/login/email-otp/resend")
    public ResponseEntity<OtpChallengeResponse> resendOperatingLogin(
            @Valid @RequestBody OtpResendRequest request) {
        return ResponseEntity.ok(securityAccountService.resendLoginOtp(
                request.getChallengeId(), OtpPurpose.OPERATING_LOGIN));
    }

    @PostMapping("/email-otp/settings/challenge")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OtpChallengeResponse> startEmailOtpSetting(
            Authentication authentication,
            @Valid @RequestBody EmailOtpSettingChallengeRequest request) {
        String token = authentication.getCredentials().toString();
        return ResponseEntity.ok(securityAccountService.startEmailOtpSetting(
                jwtUtil.extractUserId(token), jwtUtil.extractRole(token),
                request.getEnabled(), request.getCurrentPassword()));
    }

    @PostMapping("/email-otp/settings/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse> confirmEmailOtpSetting(
            Authentication authentication,
            @Valid @RequestBody OtpVerifyRequest request) {
        String token = authentication.getCredentials().toString();
        return ResponseEntity.ok(securityAccountService.confirmEmailOtpSetting(
                jwtUtil.extractUserId(token), jwtUtil.extractRole(token),
                request.getChallengeId(), request.getCode()));
    }
}
