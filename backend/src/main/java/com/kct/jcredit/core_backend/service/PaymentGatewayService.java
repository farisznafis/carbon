package com.kct.jcredit.core_backend.service;

import java.math.BigDecimal;

/**
 * Abstracts payment processing from company lifecycle operations.
 */
public interface PaymentGatewayService {

    /**
     * Processes a payment for legacy callers that do not supply an idempotency key.
     */
    PaymentResult processPayment(String companyId, String paymentMethod, BigDecimal amount);

    /**
     * Processes a retry-safe payment identified by a stable business key.
     */
    default PaymentResult processPayment(
            String companyId,
            String paymentMethod,
            BigDecimal amount,
            String idempotencyKey) {
        return processPayment(companyId, paymentMethod, amount);
    }
    
    record PaymentResult(boolean success, String transactionId, String failureReason) {}
}
