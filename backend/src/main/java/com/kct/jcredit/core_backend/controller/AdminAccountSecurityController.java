package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.service.SecurityAccountService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;
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
@RequiredArgsConstructor
public class AdminAccountSecurityController {
    private final SecurityAccountService securityAccountService;
    private final JwtUtil jwtUtil;

    @PostMapping("/{accountType}/{accountId}/password-reset-link")
    @PreAuthorize("hasRole('operator')")
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
