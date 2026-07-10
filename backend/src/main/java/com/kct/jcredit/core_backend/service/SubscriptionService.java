package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.SubscriptionLog;
import java.util.List;

/**
 * Manages billing and contract renewal independently from company access state.
 */
public interface SubscriptionService {

    /**
     * Processes companies whose active contracts have reached their end date.
     */
    void processExpiredContracts();

    /**
     * Attempts one renewal and creates a billing blocker when payment fails.
     */
    void renewContract(ParticipatingCompany company);

    /**
     * Returns the billing audit log for a company.
     */
    List<SubscriptionLog> getSubscriptionLogs(String companyId);
}
