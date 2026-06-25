package com.kct.jcredit.core_backend.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.SubscriptionLog;
import com.kct.jcredit.core_backend.enums.SubscriptionStatus;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.SubscriptionLogRepository;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.PaymentGatewayService;
import com.kct.jcredit.core_backend.service.SubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final ParticipatingCompanyRepository companyRepository;
    private final SubscriptionLogRepository subscriptionLogRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final NotificationService notificationService;

    private static final BigDecimal RENEWAL_AMOUNT = new BigDecimal("50000.00");
    private static final int CONTRACT_EXTENSION_DAYS = 365;

    @Override
    @Transactional
    public void processExpiredContracts() {
        LocalDate today = LocalDate.now();
        List<ParticipatingCompany> expiringCompanies = companyRepository
                .findByContractEndDateBeforeAndSubscriptionStatus(today, SubscriptionStatus.ACTIVE);

        log.info("Processing {} expired contracts", expiringCompanies.size());

        for (ParticipatingCompany company : expiringCompanies) {
            try {
                renewContract(company);
            } catch (Exception e) {
                log.error("Failed to renew contract for company: {}", company.getCompanyId(), e);
                String reason = "Auto-renewal failed: " + e.getMessage();
                suspendCompany(company, reason);
            }
        }
    }

    @Override
    @Transactional
    public void renewContract(ParticipatingCompany company) {
        log.info("Processing renewal for company: {}", company.getCompanyId());

        String paymentMethod = company.getPaymentMethod();
        if (paymentMethod == null || paymentMethod.isEmpty()) {
            paymentMethod = "CREDIT_CARD";
        }

        PaymentGatewayService.PaymentResult result = paymentGatewayService.processPayment(
                company.getCompanyId(), paymentMethod, RENEWAL_AMOUNT);

        LocalDate previousEndDate = company.getContractEndDate();

        if (result.success()) {
            LocalDate newEndDate = previousEndDate.plusDays(CONTRACT_EXTENSION_DAYS);
            company.setContractEndDate(newEndDate);
            company.setLastRenewalDate(LocalDateTime.now());
            company.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            company.setIsActive(true);
            companyRepository.save(company);

            SubscriptionLog subscriptionLog = SubscriptionLog.builder()
                    .company(company)
                    .actionType("AUTO_RENEWAL")
                    .actionDate(LocalDateTime.now())
                    .amount(RENEWAL_AMOUNT)
                    .paymentMethod(paymentMethod)
                    .transactionId(result.transactionId())
                    .status("SUCCESS")
                    .previousEndDate(previousEndDate)
                    .newEndDate(newEndDate)
                    .build();
            subscriptionLogRepository.save(subscriptionLog);

            log.info("Renewal successful for company: {}, new end date: {}", company.getCompanyId(), newEndDate);
        } else {
            company.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
            company.setSuspensionReason(result.failureReason());
            company.setSuspensionDate(LocalDateTime.now());
            company.setIsActive(false);
            companyRepository.save(company);

            SubscriptionLog subscriptionLog = SubscriptionLog.builder()
                    .company(company)
                    .actionType("AUTO_RENEWAL")
                    .actionDate(LocalDateTime.now())
                    .amount(RENEWAL_AMOUNT)
                    .paymentMethod(paymentMethod)
                    .transactionId(result.transactionId())
                    .status("FAILED")
                    .failureReason(result.failureReason())
                    .previousEndDate(previousEndDate)
                    .build();
            subscriptionLogRepository.save(subscriptionLog);

            notificationService.sendFailureNotification(company, result.failureReason());
            notificationService.sendSuspensionNotification(company);

            log.warn("Renewal failed for company: {}, suspended", company.getCompanyId());
        }
    }

    @Override
    @Transactional
    public void suspendCompany(ParticipatingCompany company, String reason) {
        log.info("Suspending company: {}", company.getCompanyId());

        company.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
        company.setSuspensionReason(reason);
        company.setSuspensionDate(LocalDateTime.now());
        company.setIsActive(false);
        companyRepository.save(company);

        SubscriptionLog subscriptionLog = SubscriptionLog.builder()
                .company(company)
                .actionType("SUSPENSION")
                .actionDate(LocalDateTime.now())
                .status("SUSPENDED")
                .failureReason(reason)
                .build();
        subscriptionLogRepository.save(subscriptionLog);

        notificationService.sendSuspensionNotification(company);
        notificationService.createNotificationForParticipatingCompany(
                "一時利用停止のお知らせ",
                "停止されている理由: " + reason,
                company.getCompanyId(),
                null
        );
    }

    @Override
    @Transactional
    public void reactivateCompany(ParticipatingCompany company) {
        log.info("Reactivating company: {}", company.getCompanyId());

        company.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        company.setSuspensionReason(null);
        company.setSuspensionDate(null);
        company.setIsActive(true);
        companyRepository.save(company);

        SubscriptionLog subscriptionLog = SubscriptionLog.builder()
                .company(company)
                .actionType("REACTIVATION")
                .actionDate(LocalDateTime.now())
                .status("ACTIVE")
                .build();
        subscriptionLogRepository.save(subscriptionLog);

        notificationService.sendReactivationNotification(company);
                notificationService.createNotificationForParticipatingCompany(
                "ご利用再開のお知らせ",
                "利用再開されました。ご利用ありがとうございます。",
                company.getCompanyId(),
                null
        );
    }

    @Override
    public List<SubscriptionLog> getSubscriptionLogs(String companyId) {
        return subscriptionLogRepository.findByCompanyCompanyIdOrderByActionDateDesc(companyId);
    }

    @Override
    @Transactional
    public void suspendCompanyManually(String companyId, String reason) {
        log.info("Manually suspending company: {}", companyId);
        
        //企業IDで企業を検索し、nullの場合は"Company not found: " + companyIdを返す
        ParticipatingCompany company = companyRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found: " + companyId));
        //企業を停止状態にする
        company.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
        company.setSuspensionReason(reason != null ? reason : "Manual suspension");
        company.setSuspensionDate(LocalDateTime.now());
        company.setIsActive(false);
        companyRepository.save(company);

        //
        SubscriptionLog subscriptionLog = SubscriptionLog.builder()
                .company(company)
                .actionType("MANUAL_SUSPENSION")
                .actionDate(LocalDateTime.now())
                .status("SUSPENDED")
                .failureReason(reason)
                .build();
        subscriptionLogRepository.save(subscriptionLog);

        //
        notificationService.sendSuspensionNotification(company);
        
        log.info("Company suspended manually: {}", companyId);
    }

    @Override
    @Transactional
    public void reactivateCompanyWithPayment(ParticipatingCompany company, String paymentMethod, PaymentGatewayService.PaymentResult paymentResult) {
        log.info("Reactivating company with payment: {}", company.getCompanyId());

        LocalDate previousEndDate = company.getContractEndDate();

        if (paymentResult.success()) {
            LocalDate newEndDate = previousEndDate.plusDays(CONTRACT_EXTENSION_DAYS);
            company.setContractEndDate(newEndDate);
            company.setPaymentMethod(paymentMethod);
            company.setLastRenewalDate(LocalDateTime.now());
            company.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            company.setSuspensionReason(null);
            company.setSuspensionDate(null);
            company.setIsActive(true);
            companyRepository.save(company);

            SubscriptionLog subscriptionLog = SubscriptionLog.builder()
                    .company(company)
                    .actionType("MANUAL_REACTIVATION")
                    .actionDate(LocalDateTime.now())
                    .amount(RENEWAL_AMOUNT)
                    .paymentMethod(paymentMethod)
                    .transactionId(paymentResult.transactionId())
                    .status("SUCCESS")
                    .previousEndDate(previousEndDate)
                    .newEndDate(newEndDate)
                    .build();
            subscriptionLogRepository.save(subscriptionLog);

            notificationService.sendReactivationNotification(company);

            log.info("Reactivation with payment successful for company: {}", company.getCompanyId());
        } else {
            SubscriptionLog subscriptionLog = SubscriptionLog.builder()
                    .company(company)
                    .actionType("MANUAL_REACTIVATION")
                    .actionDate(LocalDateTime.now())
                    .amount(RENEWAL_AMOUNT)
                    .paymentMethod(paymentMethod)
                    .transactionId(paymentResult.transactionId())
                    .status("FAILED")
                    .failureReason(paymentResult.failureReason())
                    .build();
            subscriptionLogRepository.save(subscriptionLog);

            log.warn("Reactivation payment failed for company: {}", company.getCompanyId());
            throw new RuntimeException("Payment failed: " + paymentResult.failureReason());
        }
    }
}