package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.response.NotificationListResponse;
import com.kct.jcredit.core_backend.dto.response.NotificationCountResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Notification;
import com.kct.jcredit.core_backend.dto.request.InquiryRequest;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

public interface NotificationService {
    void sendSuspensionNotification(ParticipatingCompany company);
    void sendReactivationNotification(ParticipatingCompany company);
    void sendMasterAccountCredentials(ParticipatingCompany company, String username, String plainPassword);
    void sendCompanyRejectionNotification(ParticipatingCompany company, String rejectionReason);
    void sendSecurityEmail(String email, String subject, String body);
    void sendOtpEmail(String email, String code, long expiresInMinutes);
    NotificationListResponse getNotifications(
        Integer userId,
        Boolean unreadOnly,
        Integer page,
        Integer size,
        boolean isOperatingUser
    );

    NotificationCountResponse getNotificationCount(Integer userId, boolean isOperatingUser);

    NotificationListResponse readNotification(Integer userId, Integer notificationId, boolean isOperatingUser);

    NotificationListResponse deleteNotification(Integer userId, Integer notificationId, boolean isOperatingUser);

    Notification createNotification(String title,
            String message,
            Integer participatingUserId,
            Integer operatingUserId);

    void createNotificationForParticipatingCompany(
            String title,
            String message,
            Integer companyId,
            Integer operatingUserId);


    void createNotificationForAllOperatingUsers(
            String title,
            String message);

    void sendEmailToAllOperatingUsers(
            String subject,
            String body);

    void sendEmailToAllOperatorCompanies(
            String subject,
            String body);

    void sendParticipationRejectionNotification(ParticipatingCompany company, String rejectionReason);
    void sendParticipationApprovedNotification(ParticipatingCompany company);

    BaseResponse createInquiry(InquiryRequest request);
}
