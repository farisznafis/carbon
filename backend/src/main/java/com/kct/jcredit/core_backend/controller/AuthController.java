package com.kct.jcredit.core_backend.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.kct.jcredit.core_backend.dto.request.AdminLoginRequest;
import com.kct.jcredit.core_backend.dto.request.CreateDummyUserRequest;
import com.kct.jcredit.core_backend.dto.request.LoginRequest;
import com.kct.jcredit.core_backend.dto.request.PasswordChangeRequest;
import com.kct.jcredit.core_backend.dto.response.AdminLoginResponse;
import com.kct.jcredit.core_backend.dto.response.LoginResponse;
import com.kct.jcredit.core_backend.dto.response.PasswordChangeResponse;
import com.kct.jcredit.core_backend.dto.response.UserProfileResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.service.AuthService;
import com.kct.jcredit.core_backend.service.UserService;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import com.kct.jcredit.core_backend.util.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Tag(name = "認証", description = "ログイン、プロフィール取得、パスワード変更などの認証関連APIです。")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/user/login")
    @Operation(summary = "参加企業ユーザーログイン", description = "参加企業ユーザーの認証を行い、ログイン情報を返します。")
    public ResponseEntity<LoginResponse> userLogin(@RequestBody LoginRequest request) {     
        LoginResponse response = authService.userLogin(request);
        
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/admin/login")
    @Operation(summary = "管理者ログイン", description = "管理者の認証を行い、ログイン情報を返します。")
    public ResponseEntity<AdminLoginResponse> adminLogin(@RequestBody AdminLoginRequest request) {
        AdminLoginResponse response = authService.operatorLogin(request);

        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }


    @GetMapping("/user/profile")
    @PreAuthorize("hasRole('user')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "ログイン参加企業ユーザー情報取得", description = "ログイン中の参加企業ユーザーのプロフィール情報を取得します。")
    public ResponseEntity<UserProfileResponse> getUserProfile(Authentication authentication) {
        UserProfileResponse response = new UserProfileResponse();

        String username = authentication.getName();
        response = userService.getUserProfile(username);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/user/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "パスワード変更", description = "ログイン中のユーザーのパスワードを変更します。")
    public ResponseEntity<PasswordChangeResponse> changePassword(Authentication authentication, @RequestHeader("Authorization") String authHeader,
                                                                @Valid @RequestBody PasswordChangeRequest request) {
        String username = authentication.getName();
        String token = authHeader.substring(7); 
        PasswordChangeResponse response = authService.changePassword(username, token, request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
   }


    @PostMapping("/dummyUser")
    @Operation(summary = "ダミー参加企業ユーザー作成", description = "検証用の参加企業ユーザーを作成します。")
    public ResponseEntity<ParticipatingUser> createDummyUser(@RequestBody CreateDummyUserRequest req) {
        ParticipatingUser user = authService.createDummyUser(req);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/dummyOperator")
    @Operation(summary = "ダミー管理者作成", description = "検証用の管理者を作成します。")
    public ResponseEntity<OperatingUser> createDummyOperator(@RequestBody CreateDummyUserRequest req) {
        OperatingUser user = authService.createDummyOperatorUser(req);
        return ResponseEntity.ok(user);
    }
}
