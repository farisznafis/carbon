package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.service.PaymentGatewayService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    private static final BigDecimal RENEWAL_AMOUNT = new BigDecimal("50000.00");

    @Override
    public PaymentResult processPayment(String companyId, String paymentMethod, BigDecimal amount) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean success = Math.random() > 0.1;

        if (success) {
            String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            return new PaymentResult(true, transactionId, null);
        } else {
            return new PaymentResult(false, null, "Payment declined by gateway");
        }
    }
}



