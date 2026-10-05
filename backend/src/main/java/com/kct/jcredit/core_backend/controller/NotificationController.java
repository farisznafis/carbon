package com.kct.jcredit.core_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;


import com.kct.jcredit.core_backend.dto.response.NotificationListResponse;
import com.kct.jcredit.core_backend.dto.request.InquiryRequest;
import com.kct.jcredit.core_backend.dto.response.NotificationCountResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/notification")
@Tag(name = "通知", description = "通知の一覧、未読数取得、既読処理を管理するAPIです。")
@RequiredArgsConstructor
public class NotificationController {

    private final JwtUtil jwtUtil;
    private final NotificationService notificationService;

    @GetMapping("/list")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "ユーザー通知一覧取得", description = "ログイン中の参加企業ユーザーの通知一覧を取得します。")
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

    @GetMapping("/count")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "ユーザー通知数取得", description = "ログイン中の参加企業ユーザーの未読通知数を取得します。")
    public ResponseEntity<NotificationCountResponse> getNotificationCount(Authentication authentication) {
        Integer userId = jwtUtil.extractUserId(authentication.getCredentials().toString());
        boolean isOperatingUser = jwtUtil.extractRole(authentication.getCredentials().toString()).equals("operator");

        NotificationCountResponse response = notificationService.getNotificationCount(userId, isOperatingUser);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }  
    
    @PatchMapping("/{notificationId}/read")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "ユーザー通知既読", description = "指定された通知を既読にします。")
    public ResponseEntity<NotificationListResponse> readNotification(
            Authentication authentication,
            @PathVariable Integer notificationId) {
        Integer userId = jwtUtil.extractUserId(authentication.getCredentials().toString());
        boolean isOperatingUser = jwtUtil.extractRole(authentication.getCredentials().toString()).equals("operator");

        NotificationListResponse response = notificationService.readNotification(userId, notificationId, isOperatingUser);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }  

    @PatchMapping("/{notificationId}/delete")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "ユーザー通知削除", description = "指定された通知を削除します。")
    public ResponseEntity<NotificationListResponse> deleteNotification(
            Authentication authentication,
            @PathVariable Integer notificationId) {
        Integer userId = jwtUtil.extractUserId(authentication.getCredentials().toString());
        boolean isOperatingUser = jwtUtil.extractRole(authentication.getCredentials().toString()).equals("operator");

        NotificationListResponse response = notificationService.deleteNotification(userId, notificationId, isOperatingUser);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }  

    @PostMapping("/inquiry")
    @Operation(summary = "問い合わせ受付", description = "お問い合わせ内容を受け付け、担当オペレーターへ通知・メール送信を行います。")
    public ResponseEntity<BaseResponse> createInquiry(
            @Valid @RequestBody InquiryRequest request) {
                
        BaseResponse response = notificationService.createInquiry(request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }
}
