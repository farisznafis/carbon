package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.SubscriptionLog;
import com.kct.jcredit.core_backend.enums.SubscriptionStatus;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.SubscriptionLogRepository;
import com.kct.jcredit.core_backend.service.CompanyLifecycleService;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.PaymentGatewayService;
import com.kct.jcredit.core_backend.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles contract renewal while delegating operational blocking to the lifecycle service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final BigDecimal RENEWAL_AMOUNT = new BigDecimal("50000.00");
    private static final int CONTRACT_EXTENSION_DAYS = 365;

    private final ParticipatingCompanyRepository companyRepository;
    private final SubscriptionLogRepository subscriptionLogRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final CompanyLifecycleService companyLifecycleService;
    private final NotificationService notificationService;
    private final PlatformTransactionManager transactionManager;

    /**
     * Attempts renewal for every active subscription whose contract has expired.
     */
    @Override
    public void processExpiredContracts() {
        LocalDate today = LocalDate.now();
        List<ParticipatingCompany> companies = companyRepository
                .findByContractEndDateBeforeAndSubscriptionStatus(today, SubscriptionStatus.ACTIVE);

        log.info("Processing {} expired participating-company contracts", companies.size());
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        companies.forEach(company -> transactionTemplate.executeWithoutResult(
                status -> renewContract(company)
        ));
    }

    /**
     * Updates only billing state and creates a BILLING blocker when renewal cannot complete.
     */
    @Override
    @Transactional
    public void renewContract(ParticipatingCompany company) {
        String paymentMethod = company.getPaymentMethod();
        if (!StringUtils.hasText(paymentMethod)) {
            handleRenewalFailure(company, "支払方法が登録されていません。", null);
            return;
        }

        company.setSubscriptionStatus(SubscriptionStatus.PENDING_RENEWAL);
        companyRepository.save(company);

        PaymentGatewayService.PaymentResult result = paymentGatewayService.processPayment(
                company.getCompanyId(),
                paymentMethod,
                RENEWAL_AMOUNT,
                "AUTO-RENEWAL-" + company.getCompanyId() + "-" + company.getContractEndDate()
        );
        if (!result.success()) {
            handleRenewalFailure(company, "契約更新の支払いに失敗しました。", result.failureReason());
            return;
        }

        LocalDate previousEndDate = company.getContractEndDate();
        LocalDate extensionBase = previousEndDate == null || previousEndDate.isBefore(LocalDate.now())
                ? LocalDate.now()
                : previousEndDate;
        LocalDate newEndDate = extensionBase.plusDays(CONTRACT_EXTENSION_DAYS);

        company.setContractEndDate(newEndDate);
        company.setLastRenewalDate(LocalDateTime.now());
        company.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        companyRepository.save(company);
        subscriptionLogRepository.save(buildRenewalLog(
                company,
                paymentMethod,
                result,
                previousEndDate,
                newEndDate
        ));
    }

    /**
     * Returns billing history ordered by the most recent action first.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionLog> getSubscriptionLogs(String companyId) {
        return subscriptionLogRepository.findByCompanyCompanyIdOrderByActionDateDesc(companyId);
    }

    /**
     * Persists a recoverable billing failure and delegates access suspension.
     */
    private void handleRenewalFailure(
            ParticipatingCompany company,
            String blockerReason,
            String gatewayReason) {
        company.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
        companyRepository.save(company);

        SubscriptionLog failureLog = SubscriptionLog.builder()
                .company(company)
                .actionType("AUTO_RENEWAL")
                .actionDate(LocalDateTime.now())
                .amount(RENEWAL_AMOUNT)
                .paymentMethod(company.getPaymentMethod())
                .status("FAILED")
                .failureReason(gatewayReason)
                .previousEndDate(company.getContractEndDate())
                .build();
        subscriptionLogRepository.save(failureLog);
        companyLifecycleService.createBillingSuspension(company.getCompanyId(), blockerReason);

        registerAfterCommit(() -> sendFailureNotification(company, blockerReason));
    }

    /**
     * Builds the immutable audit row for a successful automatic renewal.
     */
    private SubscriptionLog buildRenewalLog(
            ParticipatingCompany company,
            String paymentMethod,
            PaymentGatewayService.PaymentResult result,
            LocalDate previousEndDate,
            LocalDate newEndDate) {
        return SubscriptionLog.builder()
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
    }

    /**
     * Defers the billing notification until its database changes are committed.
     */
    private void registerAfterCommit(Runnable notificationTask) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notificationTask.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationTask.run();
            }
        });
    }

    /**
     * Sends a renewal failure notification without exposing gateway details in logs.
     */
    private void sendFailureNotification(ParticipatingCompany company, String blockerReason) {
        try {
            notificationService.sendFailureNotification(company, blockerReason);
        } catch (Exception exception) {
            log.error("Failed to send renewal failure notification. companyId={}", company.getCompanyId(), exception);
        }
    }
}
