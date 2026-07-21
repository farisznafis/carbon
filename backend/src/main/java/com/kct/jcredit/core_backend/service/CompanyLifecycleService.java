package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.CreateCompanySuspensionRequest;
import com.kct.jcredit.core_backend.dto.request.ResolveCompanySuspensionRequest;
import com.kct.jcredit.core_backend.dto.response.CompanySuspensionListResponse;
import com.kct.jcredit.core_backend.dto.response.CompanySuspensionResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;

/**
 * Enforces every participating-company lifecycle transition and suspension invariant.
 */
public interface CompanyLifecycleService {

    /**
     * Changes a pending company to active after approval succeeds.
     */
    void activatePendingCompany(ParticipatingCompany company);

    void activateApprovedCompany(ParticipatingCompany company);

    /**
     * Changes a pending company to rejected.
     */
    void rejectPendingCompany(ParticipatingCompany company);

    /**
     * Creates an administrator-requested operational blocker.
     */
    CompanySuspensionResponse createSuspension(
            String companyId,
            CreateCompanySuspensionRequest request);

    /**
     * Returns active or historical suspension records.
     */
    CompanySuspensionListResponse findSuspensions(
            String companyId,
            boolean activeOnly,
            int page,
            int limit);

    /**
     * Resolves one blocker and activates the company only when no blockers remain.
     */
    CompanySuspensionResponse resolveSuspension(
            String companyId,
            Integer suspensionId,
            ResolveCompanySuspensionRequest request);
}
