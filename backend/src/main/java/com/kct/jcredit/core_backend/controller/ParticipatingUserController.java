package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.request.DeleteUserSite;
import com.kct.jcredit.core_backend.dto.request.UpdateUserRequest;
import com.kct.jcredit.core_backend.dto.request.UserInvitationRequest;
import com.kct.jcredit.core_backend.dto.response.DeleteUserSiteResponse;
import com.kct.jcredit.core_backend.dto.response.NotificationListResponse;
import com.kct.jcredit.core_backend.dto.response.UpdateUserResponse;
import com.kct.jcredit.core_backend.dto.response.UserListResponse;
import com.kct.jcredit.core_backend.dto.response.UserProfileResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.service.UserInvitationService;
import com.kct.jcredit.core_backend.service.UserService;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Tag(name = "参加企業ユーザー", description = "参加企業ユーザーの作成、更新、一覧、プロフィール、通知を管理するAPIです。")
@RequiredArgsConstructor
public class ParticipatingUserController {

    private final UserService userService;
    private final UserInvitationService userInvitationService;
    private final JwtUtil jwtUtil;

    @GetMapping
    @PreAuthorize("hasAnyRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "参加企業ユーザー一覧取得", description = "ログイン中の参加企業に所属する参加企業ユーザー一覧を取得します。")
    public ResponseEntity<UserListResponse> getUsers(Authentication authentication) {

        UserListResponse response = userService.getUsersByCompany(jwtUtil.extractCompanyId(authentication.getCredentials().toString()));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/invitations")
    @PreAuthorize("hasAnyRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "参加企業ユーザー招待", description = "メールアドレスと拠点のみを指定して招待メールを送信します。ユーザー本人がリンクから残りの情報を入力してアカウントを作成します。")
    public ResponseEntity<BaseResponse> inviteUser(
            Authentication authentication,
            @RequestBody @Valid UserInvitationRequest request) {
        String token = authentication.getCredentials().toString();
        BaseResponse response = userInvitationService.invite(
                jwtUtil.extractCompanyId(token), jwtUtil.extractUsername(token),
                request.getEmail(), request.getSiteId());
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PatchMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('master')")
    @Operation(summary = "参加企業ユーザー情報更新", description = "指定された参加企業ユーザーの情報を更新します。")
    public ResponseEntity<UpdateUserResponse> updateUser(
            Authentication authentication,
            @RequestBody @Valid UpdateUserRequest request) {
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());
        String username = jwtUtil.extractUsername(authentication.getCredentials().toString());
        UpdateUserResponse response = userService.updateUser(companyId, username, request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }  

    @PostMapping("/validate-update")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "参加企業ユーザー更新検証", description = "参加企業ユーザー更新リクエストを保存せずに検証します。")
    public ResponseEntity<BaseResponse> validateUpdateUser(
            Authentication authentication,
            @RequestBody @Valid UpdateUserRequest request) {
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());
        String username = jwtUtil.extractUsername(authentication.getCredentials().toString());

        BaseResponse response = userService.validateUpdateUser(companyId, username, request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @GetMapping("/profile/{userId}")
    @PreAuthorize("hasAnyRole('admin', 'master') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PARTICIPATING_USER_READ.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "参加企業ユーザープロフィール取得", description = "指定された参加企業ユーザーIDのプロフィール情報を取得します。")
    public ResponseEntity<UserProfileResponse> getUserProfileById(
            @PathVariable Integer userId,
            Authentication authentication) {
        String role = jwtUtil.extractRole(authentication.getCredentials().toString());
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());

        UserProfileResponse response = userService.getUserProfileById(userId, role, companyId);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }
}
