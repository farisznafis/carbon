package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.entity.ParticipatingCompany;

/**
 * Owns billing-only state changes and subscription audit logging.
 */
public interface SubscriptionBillingService {

    /**
     * Applies a successful recovery payment without changing company lifecycle status.
     */
    void completeRecovery(
            ParticipatingCompany company,
            String paymentMethod,
            PaymentGatewayService.PaymentResult paymentResult);
}
