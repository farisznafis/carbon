package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.SubscriptionLog;
import com.kct.jcredit.core_backend.enums.SubscriptionStatus;
import com.kct.jcredit.core_backend.service.PaymentGatewayService;

import java.util.List;

public interface SubscriptionService {
    void processExpiredContracts();
    void renewContract(ParticipatingCompany company);
    void suspendCompany(ParticipatingCompany company, String reason);
    void reactivateCompany(ParticipatingCompany company);
    void reactivateCompanyWithPayment(ParticipatingCompany company, String paymentMethod, PaymentGatewayService.PaymentResult paymentResult);
    List<SubscriptionLog> getSubscriptionLogs(String companyId);
    void suspendCompanyManually(String companyId, String reason);
}