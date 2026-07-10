package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.service.PaymentGatewayService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    private final ConcurrentMap<String, PaymentResult> successfulPayments = new ConcurrentHashMap<>();

    /**
     * Processes a non-idempotent payment for existing renewal callers.
     */
    @Override
    public PaymentResult processPayment(String companyId, String paymentMethod, BigDecimal amount) {
        return performPayment();
    }

    /**
     * Reuses a successful result for retries with the same idempotency key.
     */
    @Override
    public PaymentResult processPayment(
            String companyId,
            String paymentMethod,
            BigDecimal amount,
            String idempotencyKey) {
        PaymentResult previousResult = successfulPayments.get(idempotencyKey);
        if (previousResult != null) {
            return previousResult;
        }

        PaymentResult currentResult = performPayment();
        if (currentResult.success()) {
            successfulPayments.putIfAbsent(idempotencyKey, currentResult);
            return successfulPayments.get(idempotencyKey);
        }
        return currentResult;
    }

    /**
     * Simulates the external gateway and preserves interruption state.
     */
    private PaymentResult performPayment() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean success = Math.random() > 0.1;

        if (success) {
            String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            return new PaymentResult(true, transactionId, null);
        }
        return new PaymentResult(false, null, "Payment declined by gateway");
    }
}



