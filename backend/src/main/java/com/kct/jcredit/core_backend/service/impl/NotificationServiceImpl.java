package com.kct.jcredit.core_backend.service.impl;
 
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.NotificationListResponse;
import com.kct.jcredit.core_backend.dto.response.details.NotificationDetailResponse;
import com.kct.jcredit.core_backend.dto.response.NotificationCountResponse;
import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;

import com.kct.jcredit.core_backend.entity.Notification;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.repository.postgres.NotificationRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatorCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;



@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;
    private final ParticipatingUserRepository participatingUserRepository;
    private final OperatingUserRepository operatingUserRepository;
    private final OperatorCompanyRepository operatorCompanyRepository;

    @Value("${app.mail.from:no-reply@gxcarbon.local}")
    private String fromAddress;
    @Value("${app.mail.override-to:}")
    private String overrideToAddress; 
    
    
    @Override
    public void sendSuspensionNotification(ParticipatingCompany company) {
        String subject = "アカウント停止のお知らせ";
        String body = "お客様のアカウントは現在停止されています。詳細は管理者へお問い合わせください。";
        sendMail(company.getEmail(), subject, body);
        log.info("[NOTIFICATION] Account Suspension - Company: {}, Email: {}",
            company.getCompanyId(), company.getEmail());
    }

    @Override
    public void sendReactivationNotification(ParticipatingCompany company) {
        String subject = "アカウント再開のお知らせ";
        String body = "企業 ID: " + company.getCompanyId() + " のアカウントが再開されました。";
        sendMail(company.getEmail(), subject, body);
        log.info("[NOTIFICATION] Account Reactivation - Company: {}, Email: {}",
            company.getCompanyId(), company.getEmail());
    }

    @Override
    public void sendMasterAccountCredentials(ParticipatingCompany company, String username, String plainPassword) {
        String subject = "マスターアカウント発行のお知らせ";
        String body = "企業 ID: " + company.getCompanyId() + "\nUsername: " + username + "\nPassword: " + plainPassword;
        sendMail(company.getEmail(), subject, body);
        log.info("[NOTIFICATION] Master Account Created - Company: {}, Email: {}",
                company.getCompanyId(), company.getEmail());
    }

    @Override
    public void sendCompanyRejectionNotification(ParticipatingCompany company, String rejectionReason) {
        String subject = "申請アカウントが却下されました";
        String companyName = company.getNameJa() != null ? company.getNameJa() : "";
        String body = companyName + " ご担当者様\n\n"
            + "このたびはご申請いただきありがとうございます。\n"
            + "申し訳ございませんが、貴社の申請は却下されました。\n\n"
            + "却下理由:\n"
            + rejectionReason + "\n\n"
            + "ご不明点がございましたら、お問い合わせください。";
        sendMail(company.getEmail(), subject, body);
        log.info("[NOTIFICATION] Company Rejected - Company: {}, Email: {}",
            company.getCompanyId(), company.getEmail());
    }

    @Override
    public void sendSecurityEmail(String email, String subject, String body) {
        sendMail(email, subject, body);
    }

    @Override
    public void sendOtpEmail(String email, String code, long expiresInMinutes) {
        String subject = "メール認証コードのお知らせ";
        String body = "認証コードは以下の通りです: " + code + "\n\n"
                + "このコードの有効期限は" + expiresInMinutes + "分です。"
                + "このコードは他人に共有しないでください。";
        sendMail(email, subject, body);
    }


    private void sendMail(String to, String subject, String body) {
        String resolvedTo = to;
        if (overrideToAddress != null && !overrideToAddress.isBlank()) {
            resolvedTo = overrideToAddress;
        }
        if (resolvedTo == null || resolvedTo.isBlank()) {
            log.warn("[NOTIFICATION] Missing . Subject: {}", subject);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(resolvedTo);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(
        Integer userId,
        Boolean unreadOnly,
        Integer page,
        Integer size,
        boolean isOperatingUser
    ) {
        log.info("Getting notifications - userId: {}, unreadOnly: {}, page: {}, size: {}", 
            userId, unreadOnly, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
           // 条件に応じてリポジトリメソッドを呼び分け
        Page<Notification> pageResult;
        
        if (Boolean.TRUE.equals(isOperatingUser)) {
            // 運営ユーザーの通知
            pageResult = Boolean.TRUE.equals(unreadOnly)
                ? notificationRepository.findByOperatingUserIdAndReadAndIsActive(userId, false, pageable)
                : notificationRepository.findByOperatingUserIdAndIsActive(userId, pageable);
        } else {
            // 参加企業ユーザーの通知
            pageResult = Boolean.TRUE.equals(unreadOnly)
                ? notificationRepository.findByParticipatingUserIdAndReadAndIsActive(userId, false, pageable)
                : notificationRepository.findByParticipatingUserIdAndIsActive(userId, pageable);
        }
        
        log.info("Found {} notifications", pageResult.getTotalElements());
        
        // Entity → DTO 変換
        List<NotificationDetailResponse> notificationDtos = pageResult.getContent().stream()
            .map(this::convertToNotificationDetailResponse)
            .toList();
        
        // ページネーション情報作成
        PaginationDetailResponse pagination = new PaginationDetailResponse(
            page,
            size,
            pageResult.getTotalElements(),
            pageResult.getTotalPages(),
            null,
            true
        );
        
        // レスポンス作成
        NotificationListResponse response = new NotificationListResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setNotifications(notificationDtos);
        response.setPagination(pagination);
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationCountResponse getNotificationCount(Integer userId, boolean isOperatingUser) {
        log.info("Counting unread notifications - userId: {}, isOperatingUser: {}", userId, isOperatingUser);

        long unreadCount = isOperatingUser
            ? notificationRepository.countUnreadByOperatingUserId(userId)
            : notificationRepository.countUnreadByParticipatingUserId(userId);

        NotificationCountResponse response = new NotificationCountResponse();
        response.setUnreadCount(unreadCount);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    @Override
    @Transactional
    public NotificationListResponse readNotification(Integer userId, Integer notificationId, boolean isOperatingUser) {
        log.info("Marking notification read - userId: {}, notificationId: {}, isOperatingUser: {}", userId, notificationId, isOperatingUser);

        NotificationListResponse response = new NotificationListResponse();

        Notification notification = notificationRepository.findById(notificationId)
            .filter(Notification::getIsActive)
            .orElse(null);

        if (notification == null || !isNotificationAccessible(notification, userId, isOperatingUser)) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage("Notification not found");
            return response;
        }

        if (!Boolean.TRUE.equals(notification.getRead())) {
            notification.markAsRead();
            notificationRepository.save(notification);
        }

        response.setStatusCode(ResponseConstants.OK);
        response.setNotifications(List.of(convertToNotificationDetailResponse(notification)));
        return response;
    }

    @Override
    @Transactional
    public NotificationListResponse deleteNotification(Integer userId, Integer notificationId, boolean isOperatingUser) {
        log.info("Deleting notification - userId: {}, notificationId: {}, isOperatingUser: {}", userId, notificationId, isOperatingUser);

        NotificationListResponse response = new NotificationListResponse();

        Notification notification = notificationRepository.findById(notificationId)
            .filter(Notification::getIsActive)
            .orElse(null);

        if (notification == null || !isNotificationAccessible(notification, userId, isOperatingUser)) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage("Notification not found");
            return response;
        }

        notification.setIsActive(false);
        notificationRepository.save(notification);

        response.setStatusCode(ResponseConstants.OK);
        response.setNotifications(List.of());
        return response;
    }

    private boolean isNotificationAccessible(Notification notification, Integer userId, boolean isOperatingUser) {
        if (Boolean.TRUE.equals(isOperatingUser)) {
            return notification.getOperatingUser() != null && notification.getOperatingUser().getId().equals(userId);
        }
        return notification.getParticipatingUser() != null && notification.getParticipatingUser().getId().equals(userId);
    }

    private NotificationDetailResponse convertToNotificationDetailResponse(Notification notification) {
        return NotificationDetailResponse.builder()
            .id(notification.getId())
            .title(notification.getTitle())
            .message(notification.getMessage())
            .read(notification.getRead())
            .operatingUserId(notification.getOperatingUser() != null 
                ? notification.getOperatingUser().getId() 
                : null)
            .participatingUserId(notification.getParticipatingUser() != null 
                ? notification.getParticipatingUser().getId() 
                : null)
            .createdAt(notification.getCreatedAt())
            .updatedAt(notification.getUpdatedAt())
            .build();
    }


        
    /**
     * 通知を作成してDBに保存（参加企業ユーザー向け）
     *
     * @param title タイトル
     * @param message メッセージ本文
     * @param participatingUserId 受信する参加企業ユーザーID
     * @param operatingUserId 送信する運営ユーザーID（オプション、システム通知の場合はnull）
     * @return 作成された通知
     */
    @Override
    @Transactional
public Notification createNotification(
        String title, 
        String message, 
        Integer ParticipatingUserId,
        Integer OperatingUserId) {
    
    // バリデーション: どちらか一方のみ指定されている必要がある
    if ((ParticipatingUserId == null && OperatingUserId == null) ||
        (ParticipatingUserId != null && OperatingUserId != null)) {
        throw new IllegalArgumentException(
            "Either ParticipatingUserId or OperatingUserId must be specified, but not both");
    }
    
    // 受信する参加企業ユーザーを取得（指定されている場合）
    ParticipatingUser ParticipatingUser = null;
    if (ParticipatingUserId != null) {
        ParticipatingUser = participatingUserRepository.findById(ParticipatingUserId)
            .orElseThrow(() -> new RuntimeException("Participating user not found: " + ParticipatingUserId));
    }
    
    // 受信する運営ユーザーを取得（指定されている場合）
    OperatingUser OperatingUser = null;
    if (OperatingUserId != null) {
        OperatingUser = operatingUserRepository.findById(OperatingUserId)
            .orElseThrow(() -> new RuntimeException("Operating user not found: " + OperatingUserId));
    }
    
    
    // 通知エンティティを作成
    Notification notification = Notification.builder()
        .title(title)
        .message(message)
        .read(false)
        .participatingUser(ParticipatingUser)  // 参加企業ユーザー向けならセット、運営ユーザー向けならnull
        .operatingUser(OperatingUser)              // 参加企業の通知ならnull
        .build();
    
    // DBに保存
    Notification savedNotification = notificationRepository.save(notification);
    log.info("Notification created successfully - id: {}", savedNotification.getId());
    
    return savedNotification;
}
    /**
     * 企業の全ユーザーに通知を送信
     */
    @Override
    @Transactional
    public void createNotificationForParticipatingCompany(
            String title, 
            String message, 
            Integer companyId,
            Integer operatingUserId) {
        
        log.info("Creating notifications for company - companyId: {}, title: {}", companyId, title);
        
        // 企業の全ユーザーを取得
        List<ParticipatingUser> users = participatingUserRepository.findByCompany_Id(companyId);
        
        if (users.isEmpty()) {
            log.warn("No users found for company: {}", companyId);
            return;
        }
        
        // 各ユーザーに通知を作成
        for (ParticipatingUser user : users) {
            createNotification(title, message, user.getId(), operatingUserId);
        }
        log.info("Created {} notifications for company {}", users.size(), companyId);
    }
    
    @Override
    @Transactional
    public void createNotificationForAllOperatingUsers(
            String title,
            String message) {

        List<OperatingUser> users = operatingUserRepository.findAll();
        if (users.isEmpty()) {
            return;
        }

        for (OperatingUser user : users) {
            createNotification(title, message, null, user.getId());
        }
    }

    @Override
    @Transactional
    public void sendEmailToAllOperatingUsers(
            String subject,
            String body) {

        List<OperatingUser> users = operatingUserRepository.findAll();
        if (users.isEmpty()) {
            log.warn("No operating users found to send email.");
            return;
        }

        for (OperatingUser user : users) {
            sendMail(user.getEmail(), subject, body);
            log.info("Sent email to operating user {} at {}",
                    user.getUsername(), user.getEmail());
        }
    }

    @Override
    @Transactional
    public void sendEmailToAllOperatorCompanies(
            String subject,
            String body) {

        List<OperatorCompany> companies = operatorCompanyRepository.findAll();
        if (companies.isEmpty()) {
            log.warn("No operator companies found to send email.");
            return;
        }

        for (OperatorCompany company : companies) {
            sendMail(company.getEmail(), subject, body);
            log.info("Sent email to operator company {} at {}",
                    company.getCompanyId(), company.getEmail());
        }
    }

    @Override
    public void sendParticipationRejectionNotification(ParticipatingUser user, String rejectionReason) {
        String subject = "入会届審査結果のお知らせ";
        String body = "この度はお申し込みいただきありがとうございます。\n"
            + "誠に申し訳ございませんが、入会届の審査の結果、却下となりました。\n\n"
            + "却下理由:\n" + rejectionReason + "\n\n"
            + "内容をご確認の上、再度お申し込みいただけます。";
        sendMail(user.getEmail(), subject, body);
        log.info("[NOTIFICATION] Participation Rejected - userId: {}, email: {}",
            user.getId(), user.getEmail());
    }

    @Override
    public void sendParticipationApprovedNotification(ParticipatingUser user) {
        String subject = "入会届審査完了のお知らせ";
        String body = "入会届の審査が完了し、承認されました。\n"
            + "書類の準備が整いましたら、マイページよりダウンロードいただけます。";
        sendMail(user.getEmail(), subject, body);
        log.info("[NOTIFICATION] Participation Approved - userId: {}, email: {}",
            user.getId(), user.getEmail());
    }
}
