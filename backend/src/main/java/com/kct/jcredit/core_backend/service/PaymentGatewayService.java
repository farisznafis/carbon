package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.entity.ParticipatingCompany;

import java.math.BigDecimal;

public interface PaymentGatewayService {
    PaymentResult processPayment(String companyId, String paymentMethod, BigDecimal amount);
    
    record PaymentResult(boolean success, String transactionId, String failureReason) {}
}