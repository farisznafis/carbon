package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.SubscriptionLog;
import com.kct.jcredit.core_backend.enums.SubscriptionStatus;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.SubscriptionLogRepository;
import com.kct.jcredit.core_backend.service.PaymentGatewayService;
import com.kct.jcredit.core_backend.service.SubscriptionBillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Persists subscription recovery independently from operational suspension resolution.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionBillingServiceImpl implements SubscriptionBillingService {

    private static final BigDecimal RENEWAL_AMOUNT = new BigDecimal("50000.00");
    private static final int CONTRACT_EXTENSION_DAYS = 365;

    private final ParticipatingCompanyRepository companyRepository;
    private final SubscriptionLogRepository subscriptionLogRepository;

    /**
     * Extends the contract from the later of today or the previous contract end date.
     */
    @Override
    public void completeRecovery(
            ParticipatingCompany company,
            String paymentMethod,
            PaymentGatewayService.PaymentResult paymentResult) {
        LocalDate previousEndDate = company.getContractEndDate();
        LocalDate extensionBase = previousEndDate == null || previousEndDate.isBefore(LocalDate.now())
                ? LocalDate.now()
                : previousEndDate;
        LocalDate newEndDate = extensionBase.plusDays(CONTRACT_EXTENSION_DAYS);

        company.setContractEndDate(newEndDate);
        company.setPaymentMethod(paymentMethod);
        company.setLastRenewalDate(LocalDateTime.now());
        company.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        companyRepository.save(company);

        SubscriptionLog log = SubscriptionLog.builder()
                .company(company)
                .actionType("BILLING_RECOVERY")
                .actionDate(LocalDateTime.now())
                .amount(RENEWAL_AMOUNT)
                .paymentMethod(paymentMethod)
                .transactionId(paymentResult.transactionId())
                .status("SUCCESS")
                .previousEndDate(previousEndDate)
                .newEndDate(newEndDate)
                .build();
        subscriptionLogRepository.save(log);
    }
}
