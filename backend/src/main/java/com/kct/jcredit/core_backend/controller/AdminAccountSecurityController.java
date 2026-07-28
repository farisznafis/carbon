package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.service.SecurityAccountService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/accounts")
@Tag(name = "管理者アカウントセキュリティ", description = "運営ユーザーによる参加企業アカウントのパスワードリセットリンク発行を管理するAPIです。")
@RequiredArgsConstructor
public class AdminAccountSecurityController {
    private final SecurityAccountService securityAccountService;
    private final JwtUtil jwtUtil;

    @PostMapping("/{accountType}/{accountId}/password-reset-link")
    @PreAuthorize("hasRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "アカウントパスワードリセットリンク発行", description = "運営ユーザーが指定したアカウント種別・IDに対して、パスワードリセット用のアクションリンクを発行します。")
    public ResponseEntity<BaseResponse> requestPasswordResetForAccount(
            Authentication authentication,
            @PathVariable String accountType,
            @PathVariable Integer accountId) {
        String actor = jwtUtil.extractUsername(authentication.getCredentials().toString());
        BaseResponse response = securityAccountService.operatorRequestPasswordReset(
                accountType, accountId, actor);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }
}
