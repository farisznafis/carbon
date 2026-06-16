package com.kct.jcredit.core_backend.scheduler;

import com.kct.jcredit.core_backend.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void checkExpiredContracts() {
        log.info("Starting scheduled job: checkExpiredContracts");
        try {
            subscriptionService.processExpiredContracts();
        } catch (Exception e) {
            log.error("Error in scheduled job: checkExpiredContracts", e);
        }
        log.info("Completed scheduled job: checkExpiredContracts");
    }
}