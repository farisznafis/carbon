package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.request.CreateUserRequest;
import com.kct.jcredit.core_backend.dto.request.DeleteUserSite;
import com.kct.jcredit.core_backend.dto.request.UpdateUserRequest;
import com.kct.jcredit.core_backend.dto.response.CreateUserResponse;
import com.kct.jcredit.core_backend.dto.response.DeleteUserSiteResponse;
import com.kct.jcredit.core_backend.dto.response.NotificationListResponse;
import com.kct.jcredit.core_backend.dto.response.UpdateUserResponse;
import com.kct.jcredit.core_backend.dto.response.UserListResponse;
import com.kct.jcredit.core_backend.dto.response.UserProfileResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
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
    private final JwtUtil jwtUtil;
    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "参加企業ユーザー一覧取得", description = "ログイン中の参加企業に所属する参加企業ユーザー一覧を取得します。")
    public ResponseEntity<UserListResponse> getUsers(Authentication authentication) {

        UserListResponse response = userService.getUsersByCompany(jwtUtil.extractCompanyId(authentication.getCredentials().toString()));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @DeleteMapping("/site")
    @PreAuthorize("hasAnyRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "参加企業ユーザー拠点紐付け削除", description = "指定された参加企業ユーザーと拠点の紐付けを削除します。")
    public ResponseEntity<DeleteUserSiteResponse> deleteUserSite(DeleteUserSite request) {

        DeleteUserSiteResponse response = userService.removeUserSiteConnection(request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "参加企業ユーザー作成", description = "ログイン中の参加企業に新しい参加企業ユーザーを作成します。")
    public ResponseEntity<CreateUserResponse> createUser(
            Authentication authentication,
            @RequestBody @Valid CreateUserRequest request) {
            String companyId = jwtUtil.extractCompanyId(
            authentication.getCredentials().toString()
        );
        String username = jwtUtil.extractUsername(authentication.getCredentials().toString());
        
        CreateUserResponse response = userService.createUser(companyId, username, request);
        
        // レスポンスのステータスコードに応じたHTTPステータスを返す
        return new ResponseEntity<>(
            response, 
            ResponseUtil.getHttpStatus(response.getStatusCode())
        );
    }

    @PostMapping("/validate-create")
    @PreAuthorize("hasAnyRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "参加企業ユーザー作成検証", description = "参加企業ユーザー作成リクエストを保存せずに検証します。")
    public ResponseEntity<BaseResponse> validateCreateUser(
            Authentication authentication,
            @RequestBody @Valid CreateUserRequest request) {
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());
        String username = jwtUtil.extractUsername(authentication.getCredentials().toString());

        BaseResponse response = userService.validateCreateUser(companyId, username, request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PatchMapping
    @SecurityRequirement(name = "bearerAuth")
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
    @PreAuthorize("hasAnyRole('master', 'operator')")
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

    @GetMapping("/notifications")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "参加企業ユーザー通知一覧取得", description = "ログイン中の参加企業ユーザーの通知一覧を取得します。")
    public ResponseEntity<NotificationListResponse> getNotifications(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Integer userId = jwtUtil.extractUserId(authentication.getCredentials().toString());
        boolean isOperatingUser = jwtUtil.extractRole(authentication.getCredentials().toString()).equals("operator");

        NotificationListResponse response = notificationService.getNotifications(userId,unreadOnly, page, size, isOperatingUser);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }  
}
