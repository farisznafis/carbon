package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.request.ActionLinkExchangeRequest;
import com.kct.jcredit.core_backend.dto.request.CompletePasswordResetRequest;
import com.kct.jcredit.core_backend.dto.request.EmailOtpSettingChallengeRequest;
import com.kct.jcredit.core_backend.dto.request.ForgotPasswordRequest;
import com.kct.jcredit.core_backend.dto.request.MasterAccountSetupRequest;
import com.kct.jcredit.core_backend.dto.request.OtpResendRequest;
import com.kct.jcredit.core_backend.dto.request.OtpVerifyRequest;
import com.kct.jcredit.core_backend.dto.request.UserInvitationCompleteRequest;
import com.kct.jcredit.core_backend.dto.response.ActionLinkExchangeResponse;
import com.kct.jcredit.core_backend.dto.response.UserInvitationPrefillResponse;
import com.kct.jcredit.core_backend.dto.response.AdminLoginResponse;
import com.kct.jcredit.core_backend.dto.response.LoginResponse;
import com.kct.jcredit.core_backend.dto.response.OtpChallengeResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.service.SecurityAccountService;
import com.kct.jcredit.core_backend.service.UserInvitationService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import com.kct.jcredit.core_backend.enums.OtpPurpose;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "セキュリティ", description = "ログイン認証、パスワードリセット、メールOTPによる二段階認証、マスターアカウント設定を管理するAPIです。")
@RequiredArgsConstructor
public class SecurityFlowController {
    private final SecurityAccountService securityAccountService;
    private final UserInvitationService userInvitationService;
    private final JwtUtil jwtUtil;

    @PostMapping("/action-links/exchange")
    @Operation(summary = "アクションリンク検証", description = "メールで送信されたアクションリンクのトークンを検証し、後続処理用のアクションセッションを発行します。")
    public ResponseEntity<ActionLinkExchangeResponse> exchangeActionLink(
            @Valid @RequestBody ActionLinkExchangeRequest request) {
        return ResponseEntity.ok(securityAccountService.exchangeActionLink(request.getToken()));
    }

    @PostMapping("/master-account/setup")
    @Operation(summary = "マスターアカウント設定", description = "アクションセッションを用いて、企業のマスターアカウントのユーザー名とパスワードを設定します。")
    public ResponseEntity<BaseResponse> setupMasterAccount(
            @RequestHeader("X-Action-Session") String actionSession,
            @Valid @RequestBody MasterAccountSetupRequest request) {
        return ResponseEntity.ok(securityAccountService.setupMasterAccount(
                actionSession, request.getUsername(), request.getPassword()));
    }

    @PostMapping("/password-reset/request")
    @Operation(summary = "パスワードリセット申請", description = "登録されたメールアドレス宛にパスワードリセット用のアクションリンクを送信します。")
    public ResponseEntity<BaseResponse> requestPasswordReset(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(securityAccountService.requestPasswordReset(request));
    }

    @PostMapping("/password-reset/complete")
    @Operation(summary = "パスワードリセット完了", description = "アクションセッションを用いて新しいパスワードを設定し、パスワードリセットを完了します。")
    public ResponseEntity<BaseResponse> completePasswordReset(
            @RequestHeader("X-Action-Session") String actionSession,
            @Valid @RequestBody CompletePasswordResetRequest request) {
        BaseResponse response = securityAccountService.completePasswordReset(
                actionSession, request.getNewPassword());
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @GetMapping("/invitation")
    @Operation(summary = "招待内容の取得", description = "招待リンクから交換したアクションセッションを用いて、招待されたメールアドレスと所属先を取得します。")
    public ResponseEntity<UserInvitationPrefillResponse> getInvitation(
            @RequestHeader("X-Action-Session") String actionSession) {
        return ResponseEntity.ok(userInvitationService.getPrefill(actionSession));
    }

    @PostMapping("/invitation/complete")
    @Operation(summary = "招待によるアカウント作成", description = "招待されたユーザー本人が氏名・電話番号・ユーザー名・パスワードを入力し、アカウントを作成します。メールアドレスは招待時のものが使用されます。")
    public ResponseEntity<BaseResponse> completeInvitation(
            @RequestHeader("X-Action-Session") String actionSession,
            @Valid @RequestBody UserInvitationCompleteRequest request) {
        BaseResponse response = userInvitationService.complete(actionSession, request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/user/login/email-otp/verify")
    @Operation(summary = "参加企業ユーザーのログインOTP検証", description = "参加企業ユーザーのログイン時にメール送信されたOTPコードを検証し、ログインを完了します。")
    public ResponseEntity<LoginResponse> verifyParticipatingLogin(
            @Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(securityAccountService.verifyParticipatingLogin(
                request.getChallengeId(), request.getCode()));
    }

    @PostMapping("/admin/login/email-otp/verify")
    @Operation(summary = "運営ユーザーのログインOTP検証", description = "運営ユーザーのログイン時にメール送信されたOTPコードを検証し、ログインを完了します。")
    public ResponseEntity<AdminLoginResponse> verifyOperatingLogin(
            @Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(securityAccountService.verifyOperatingLogin(
                request.getChallengeId(), request.getCode()));
    }

    @PostMapping("/user/login/email-otp/resend")
    @Operation(summary = "参加企業ユーザーのログインOTP再送信", description = "参加企業ユーザーのログイン用メールOTPコードを再送信します。")
    public ResponseEntity<OtpChallengeResponse> resendParticipatingLogin(
            @Valid @RequestBody OtpResendRequest request) {
        return ResponseEntity.ok(securityAccountService.resendLoginOtp(
                request.getChallengeId(), OtpPurpose.PARTICIPATING_LOGIN));
    }

    @PostMapping("/admin/login/email-otp/resend")
    @Operation(summary = "運営ユーザーのログインOTP再送信", description = "運営ユーザーのログイン用メールOTPコードを再送信します。")
    public ResponseEntity<OtpChallengeResponse> resendOperatingLogin(
            @Valid @RequestBody OtpResendRequest request) {
        return ResponseEntity.ok(securityAccountService.resendLoginOtp(
                request.getChallengeId(), OtpPurpose.OPERATING_LOGIN));
    }

    @PostMapping("/email-otp/settings/challenge")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "メールOTP設定変更の開始", description = "現在のパスワードを検証したうえで、メールOTPの有効・無効を変更するためのチャレンジを発行します。")
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
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "メールOTP設定変更の確認", description = "発行されたチャレンジに対するOTPコードを検証し、メールOTP設定の変更を確定します。")
    public ResponseEntity<BaseResponse> confirmEmailOtpSetting(
            Authentication authentication,
            @Valid @RequestBody OtpVerifyRequest request) {
        String token = authentication.getCredentials().toString();
        return ResponseEntity.ok(securityAccountService.confirmEmailOtpSetting(
                jwtUtil.extractUserId(token), jwtUtil.extractRole(token),
                request.getChallengeId(), request.getCode()));
    }
}
