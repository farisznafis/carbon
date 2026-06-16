package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.response.ApproveCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.RejectCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.PendingCompanyListResponse;

public interface CompanyApprovalService {
    ApproveCompanyResponse approveCompany(String companyId);
    RejectCompanyResponse rejectCompany(String companyId, String rejectionReason);
    PendingCompanyListResponse getPendingCompanies();
}
