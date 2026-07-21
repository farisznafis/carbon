package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.CreateCompanySuspensionRequest;
import com.kct.jcredit.core_backend.dto.request.ResolveCompanySuspensionRequest;
import com.kct.jcredit.core_backend.dto.response.CompanySuspensionListResponse;
import com.kct.jcredit.core_backend.dto.response.CompanySuspensionResponse;
import com.kct.jcredit.core_backend.dto.response.details.CompanySuspensionDetailResponse;
import com.kct.jcredit.core_backend.entity.CompanySuspension;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.exception.BusinessRuleViolationException;
import com.kct.jcredit.core_backend.exception.ResourceNotFoundException;
import com.kct.jcredit.core_backend.repository.postgres.CompanySuspensionRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.service.CompanyLifecycleService;
import com.kct.jcredit.core_backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;

/**
 * Serializes company state transitions and keeps active blockers auditable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyLifecycleServiceImpl implements CompanyLifecycleService {

    private final ParticipatingCompanyRepository companyRepository;
    private final CompanySuspensionRepository suspensionRepository;
    private final NotificationService notificationService;

    /**
     * Activates a pending company and synchronizes the inherited active flag.
     */
    @Override
    public void activatePendingCompany(ParticipatingCompany company) {
        requireStatus(company, CompanyStatus.PENDING);
        company.setCompanyStatus(CompanyStatus.ACTIVE);
        company.setIsActive(true);
        companyRepository.save(company);
    }

    @Override
    public void activateApprovedCompany(ParticipatingCompany company) {
        requireStatus(company, CompanyStatus.APPROVED_PENDING_SETUP);
        company.setCompanyStatus(CompanyStatus.ACTIVE);
        company.setIsActive(true);
        companyRepository.save(company);
    }

    /**
     * Rejects a pending company and prevents it from receiving operational access.
     */
    @Override
    public void rejectPendingCompany(ParticipatingCompany company) {
        requireStatus(company, CompanyStatus.PENDING);
        company.setCompanyStatus(CompanyStatus.REJECTED);
        company.setIsActive(false);
        companyRepository.save(company);
    }

    /**
     * Creates a manual blocker under a locked company row.
     */
    @Override
    @Transactional
    public CompanySuspensionResponse createSuspension(
            String companyId,
            CreateCompanySuspensionRequest request) {
        ParticipatingCompany company = getCompanyForUpdate(companyId);
        validateSuspendable(company);

        if (suspensionRepository.existsByCompany_IdAndReasonCodeAndResolvedAtIsNull(
                company.getId(), request.getReasonCode())) {
            throw new BusinessRuleViolationException(
                    ResponseConstants.CONFLICT,
                    ResponseConstants.MSG_SUSPENSION_ALREADY_ACTIVE
            );
        }

        CompanySuspension suspension = CompanySuspension.builder()
                .company(company)
                .reasonCode(request.getReasonCode())
                .reasonDetail(request.getReasonDetail().trim())
                .build();
        suspensionRepository.save(suspension);

        suspendCompany(company);
        registerAfterCommit(() -> sendSuspensionNotification(company));
        return buildMutationResponse(
                company,
                suspension,
                ResponseConstants.MSG_SUSPENSION_CREATED
        );
    }

    /**
     * Returns deterministic suspension history with bounded pagination.
     */
    @Override
    @Transactional(readOnly = true)
    public CompanySuspensionListResponse findSuspensions(
            String companyId,
            boolean activeOnly,
            int page,
            int limit) {
        if (companyRepository.findByCompanyId(companyId).isEmpty()) {
            throw new ResourceNotFoundException(ResponseConstants.MSG_COMPANY_NOT_FOUND);
        }

        PageRequest pageRequest = PageRequest.of(
                page - 1,
                limit,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );
        Page<CompanySuspension> suspensionPage = activeOnly
                ? suspensionRepository.findByCompany_CompanyIdAndResolvedAtIsNull(companyId, pageRequest)
                : suspensionRepository.findByCompany_CompanyId(companyId, pageRequest);

        CompanySuspensionListResponse response = new CompanySuspensionListResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(ResponseConstants.MSG_SUSPENSION_LIST_RETRIEVED);
        response.setCompanyId(companyId);
        response.setSuspensions(suspensionPage.getContent().stream().map(this::toDetail).toList());
        response.setPagination(new PaginationDetailResponse(
                page,
                limit,
                suspensionPage.getTotalElements(),
                suspensionPage.getTotalPages(),
                null,
                suspensionPage.hasNext()
        ));
        return response;
    }

    /**
     * Resolves one blocker and derives the resulting company status atomically.
     */
    @Override
    @Transactional
    public CompanySuspensionResponse resolveSuspension(
            String companyId,
            Integer suspensionId,
            ResolveCompanySuspensionRequest request) {
        ParticipatingCompany company = getCompanyForUpdate(companyId);
        CompanySuspension suspension = suspensionRepository.findForUpdate(companyId, suspensionId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseConstants.MSG_SUSPENSION_NOT_FOUND));
        if (!suspension.isActive()) {
            throw new BusinessRuleViolationException(
                    ResponseConstants.CONFLICT,
                    ResponseConstants.MSG_SUSPENSION_ALREADY_RESOLVED
            );
        }

        suspension.setResolvedAt(LocalDateTime.now());
        suspension.setResolvedBy(currentActor());
        suspension.setResolutionNote(request.getResolutionNote().trim());
        suspensionRepository.save(suspension);

        long remainingCount = suspensionRepository.countByCompany_IdAndResolvedAtIsNull(company.getId());
        String message = ResponseConstants.MSG_SUSPENSION_RESOLVED_COMPANY_STILL_SUSPENDED;
        if (remainingCount == 0) {
            company.setCompanyStatus(CompanyStatus.ACTIVE);
            company.setIsActive(true);
            companyRepository.save(company);
            message = ResponseConstants.MSG_SUSPENSION_RESOLVED;
            registerAfterCommit(() -> sendReactivationNotification(company));
        }

        CompanySuspensionResponse response = buildMutationResponse(company, suspension, message);
        response.setActiveSuspensionCount(remainingCount);
        return response;
    }

    /**
     * Rejects lifecycle transitions that do not originate from the expected state.
     */
    private void requireStatus(ParticipatingCompany company, CompanyStatus expectedStatus) {
        if (company.getCompanyStatus() != expectedStatus) {
            throw new BusinessRuleViolationException(
                    ResponseConstants.CONFLICT,
                    ResponseConstants.MSG_COMPANY_STATUS_CONFLICT
            );
        }
    }

    /**
     * Ensures only approved operational companies can receive suspension blockers.
     */
    private void validateSuspendable(ParticipatingCompany company) {
        if (company.getCompanyStatus() != CompanyStatus.ACTIVE
                && company.getCompanyStatus() != CompanyStatus.SUSPENDED) {
            throw new BusinessRuleViolationException(
                    ResponseConstants.CONFLICT,
                    ResponseConstants.MSG_COMPANY_STATUS_CONFLICT
            );
        }
    }

    /**
     * Loads and locks the canonical company row for a state transition.
     */
    private ParticipatingCompany getCompanyForUpdate(String companyId) {
        return companyRepository.findByCompanyIdForUpdate(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseConstants.MSG_COMPANY_NOT_FOUND));
    }

    /**
     * Derives inactive access from the canonical suspended status.
     */
    private void suspendCompany(ParticipatingCompany company) {
        company.setCompanyStatus(CompanyStatus.SUSPENDED);
        company.setIsActive(false);
        companyRepository.save(company);
    }

    /**
     * Builds the shared mutation response and current blocker count.
     */
    private CompanySuspensionResponse buildMutationResponse(
            ParticipatingCompany company,
            CompanySuspension suspension,
            String message) {
        CompanySuspensionResponse response = new CompanySuspensionResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(message);
        response.setCompanyId(company.getCompanyId());
        response.setCompanyStatus(company.getCompanyStatus());
        response.setSuspension(toDetail(suspension));
        response.setActiveSuspensionCount(
                suspensionRepository.countByCompany_IdAndResolvedAtIsNull(company.getId())
        );
        return response;
    }

    /**
     * Maps a persistence record to the stable suspension response contract.
     */
    private CompanySuspensionDetailResponse toDetail(CompanySuspension suspension) {
        return CompanySuspensionDetailResponse.builder()
                .id(suspension.getId())
                .reasonCode(suspension.getReasonCode())
                .reasonDetail(suspension.getReasonDetail())
                .createdAt(suspension.getCreatedAt())
                .createdBy(suspension.getCreatedBy())
                .resolvedAt(suspension.getResolvedAt())
                .resolvedBy(suspension.getResolvedBy())
                .resolutionNote(suspension.getResolutionNote())
                .active(suspension.isActive())
                .build();
    }

    /**
     * Returns the authenticated actor used for explicit resolution auditing.
     */
    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || !authentication.isAuthenticated()
                ? "system"
                : authentication.getName();
    }

    /**
     * Defers notification side effects until the database transaction commits.
     */
    private void registerAfterCommit(Runnable notificationTask) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notificationTask.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationTask.run();
            }
        });
    }

    /**
     * Sends suspension notification without exposing reason detail in logs.
     */
    private void sendSuspensionNotification(ParticipatingCompany company) {
        try {
            notificationService.sendSuspensionNotification(company);
        } catch (Exception exception) {
            log.error("Failed to send company suspension notification. companyId={}", company.getCompanyId(), exception);
        }
    }

    /**
     * Sends reactivation notification without rolling back the completed transition.
     */
    private void sendReactivationNotification(ParticipatingCompany company) {
        try {
            notificationService.sendReactivationNotification(company);
        } catch (Exception exception) {
            log.error("Failed to send company reactivation notification. companyId={}", company.getCompanyId(), exception);
        }
    }
}
