package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.response.NotificationListResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.Notification;

public interface NotificationService {
    void sendSuspensionNotification(ParticipatingCompany company);
    void sendReactivationNotification(ParticipatingCompany company);
    void sendFailureNotification(ParticipatingCompany company, String failureReason);
    void sendMasterAccountCredentials(ParticipatingCompany company, String username, String plainPassword);
    void sendCompanyRejectionNotification(ParticipatingCompany company, String rejectionReason);
    NotificationListResponse getNotifications(
        Integer userId,
        Boolean unreadOnly,
        Integer page,
        Integer size,
        boolean isOperatingUser
    );

    Notification createNotification(String title, 
            String message, 
            Integer participatingUserId,
            Integer operatingUserId);

    void createNotificationForParticipatingCompany(
            String title, 
            String message, 
            Integer companyId,
            Integer operatingUserId);

    void createNotificationForOperatingCompany(
            String title,
            String message,
            String operatorCompanyId);

    void createNotificationForAllOperatingUsers(
            String title,
            String message);

    void sendEmailToAllOperatingUsers(
            String subject,
            String body);

    void sendEmailToAllOperatorCompanies(
            String subject,
            String body);
}