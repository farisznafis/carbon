package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.response.ApproveCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.RejectCompanyResponse;

public interface CompanyApprovalService {

    /**
     * Approves a pending registration and provisions its master account.
     */
    ApproveCompanyResponse approveCompany(String companyId);

    /**
     * Rejects a pending registration and records the supplied reason in its notification.
     */
    RejectCompanyResponse rejectCompany(String companyId, String rejectionReason);
}
