package com.kct.jcredit.core_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityEmailDeliveryService {
    private final NotificationService notificationService;
    private final SecurityActionLinkService actionLinkService;

    @Async
    public void deliverActionLink(UUID linkId, String email, String subject, String body) {
        boolean sent = false;
        try {
            notificationService.sendSecurityEmail(email, subject, body);
            sent = true;
        } catch (Exception ex) {
            log.error("Security email delivery failed for link {}", linkId, ex);
        }
        try {
            actionLinkService.markDelivery(linkId, sent);
        } catch (Exception ex) {
            log.error("Unable to record security email delivery for link {}", linkId, ex);
        }
    }
}
