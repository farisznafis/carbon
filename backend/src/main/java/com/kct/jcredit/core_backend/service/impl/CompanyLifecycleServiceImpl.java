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
import com.kct.jcredit.core_backend.enums.SubscriptionStatus;
import com.kct.jcredit.core_backend.enums.SuspensionReasonCode;
import com.kct.jcredit.core_backend.exception.BusinessRuleViolationException;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.exception.ResourceNotFoundException;
import com.kct.jcredit.core_backend.repository.postgres.CompanySuspensionRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.service.CompanyLifecycleService;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.PaymentGatewayService;
import com.kct.jcredit.core_backend.service.SubscriptionBillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Serializes company state transitions and keeps active blockers auditable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyLifecycleServiceImpl implements CompanyLifecycleService {

    private static final BigDecimal RENEWAL_AMOUNT = new BigDecimal("50000.00");

    private final ParticipatingCompanyRepository companyRepository;
    private final CompanySuspensionRepository suspensionRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final SubscriptionBillingService subscriptionBillingService;
    private final NotificationService notificationService;
    private final PlatformTransactionManager transactionManager;

    /**
     * Activates a pending company and synchronizes the inherited active flag.
     */
    @Override
    public void activatePendingCompany(ParticipatingCompany company) {
        requireStatus(company, CompanyStatus.PENDING);
        company.setCompanyStatus(CompanyStatus.ACTIVE);
        company.setIsActive(true);
        company.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
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
     * Creates the idempotent billing blocker used by renewal failures and expiry processing.
     */
    @Override
    @Transactional
    public void createBillingSuspension(String companyId, String reasonDetail) {
        ParticipatingCompany company = getCompanyForUpdate(companyId);
        if (company.getCompanyStatus() == CompanyStatus.PENDING
                || company.getCompanyStatus() == CompanyStatus.REJECTED) {
            return;
        }
        if (suspensionRepository.existsByCompany_IdAndReasonCodeAndResolvedAtIsNull(
                company.getId(), SuspensionReasonCode.BILLING)) {
            suspendCompany(company);
            return;
        }

        CompanySuspension suspension = CompanySuspension.builder()
                .company(company)
                .reasonCode(SuspensionReasonCode.BILLING)
                .reasonDetail(reasonDetail)
                .build();
        suspensionRepository.save(suspension);
        suspendCompany(company);
        registerAfterCommit(() -> sendSuspensionNotification(company));
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
     * Performs conditional billing before atomically resolving the selected blocker.
     */
    @Override
    public CompanySuspensionResponse resolveSuspension(
            String companyId,
            Integer suspensionId,
            ResolveCompanySuspensionRequest request) {
        CompanySuspension snapshot = suspensionRepository.findDetails(companyId, suspensionId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseConstants.MSG_SUSPENSION_NOT_FOUND));
        validateResolveRequest(snapshot, request);

        PaymentGatewayService.PaymentResult paymentResult = null;
        String paymentMethod = null;
        if (snapshot.getReasonCode() == SuspensionReasonCode.BILLING) {
            paymentMethod = resolvePaymentMethod(snapshot.getCompany(), request.getPaymentMethod());
            paymentResult = paymentGatewayService.processPayment(
                    companyId,
                    paymentMethod,
                    RENEWAL_AMOUNT,
                    "SUSPENSION-" + suspensionId
            );
            if (!paymentResult.success()) {
                throw new CustomException(
                        ResponseConstants.PAYMENT_REQUIRED,
                        ResponseConstants.MSG_BILLING_PAYMENT_FAILED
                );
            }
        }

        PaymentGatewayService.PaymentResult successfulPayment = paymentResult;
        String selectedPaymentMethod = paymentMethod;
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return transactionTemplate.execute(status -> finalizeResolution(
                companyId,
                suspensionId,
                request,
                selectedPaymentMethod,
                successfulPayment
        ));
    }

    /**
     * Resolves a blocker inside a transaction and derives the resulting company status.
     */
    private CompanySuspensionResponse finalizeResolution(
            String companyId,
            Integer suspensionId,
            ResolveCompanySuspensionRequest request,
            String paymentMethod,
            PaymentGatewayService.PaymentResult paymentResult) {
        ParticipatingCompany company = getCompanyForUpdate(companyId);
        CompanySuspension suspension = suspensionRepository.findForUpdate(companyId, suspensionId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseConstants.MSG_SUSPENSION_NOT_FOUND));
        if (!suspension.isActive()) {
            throw new BusinessRuleViolationException(
                    ResponseConstants.CONFLICT,
                    ResponseConstants.MSG_SUSPENSION_ALREADY_RESOLVED
            );
        }

        if (suspension.getReasonCode() == SuspensionReasonCode.BILLING) {
            subscriptionBillingService.completeRecovery(company, paymentMethod, paymentResult);
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
     * Validates payment-field usage against the blocker category.
     */
    private void validateResolveRequest(
            CompanySuspension suspension,
            ResolveCompanySuspensionRequest request) {
        if (!suspension.isActive()) {
            throw new BusinessRuleViolationException(
                    ResponseConstants.CONFLICT,
                    ResponseConstants.MSG_SUSPENSION_ALREADY_RESOLVED
            );
        }
        if (suspension.getReasonCode() != SuspensionReasonCode.BILLING
                && StringUtils.hasText(request.getPaymentMethod())) {
            throw new BusinessRuleViolationException(ResponseConstants.MSG_PAYMENT_METHOD_NOT_ALLOWED);
        }
    }

    /**
     * Selects an explicit or stored payment method and rejects missing billing data.
     */
    private String resolvePaymentMethod(ParticipatingCompany company, String requestedPaymentMethod) {
        String paymentMethod = StringUtils.hasText(requestedPaymentMethod)
                ? requestedPaymentMethod.trim()
                : company.getPaymentMethod();
        if (!StringUtils.hasText(paymentMethod)) {
            throw new BusinessRuleViolationException(ResponseConstants.MSG_PAYMENT_METHOD_REQUIRED);
        }
        return paymentMethod;
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
