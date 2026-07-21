package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.CreateCompanySuspensionRequest;
import com.kct.jcredit.core_backend.dto.request.ResolveCompanySuspensionRequest;
import com.kct.jcredit.core_backend.dto.response.CompanySuspensionResponse;
import com.kct.jcredit.core_backend.entity.CompanySuspension;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.enums.SuspensionReasonCode;
import com.kct.jcredit.core_backend.repository.postgres.CompanySuspensionRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.service.impl.CompanyLifecycleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyLifecycleServiceImplTest {

    @Mock
    private ParticipatingCompanyRepository companyRepository;

    @Mock
    private CompanySuspensionRepository suspensionRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CompanyLifecycleServiceImpl service;

    private ParticipatingCompany company;

    @BeforeEach
    void setUp() {
        company = new ParticipatingCompany();
        company.setId(1);
        company.setCompanyId("COMP00000001");
        company.setCompanyStatus(CompanyStatus.ACTIVE);
        company.setIsActive(true);
    }

    @Test
    void createSuspensionChangesCanonicalStatusAndCreatesAuditRecord() {
        CreateCompanySuspensionRequest request = new CreateCompanySuspensionRequest();
        request.setReasonCode(SuspensionReasonCode.POLICY_VIOLATION);
        request.setReasonDetail(" Policy violation ");
        when(companyRepository.findByCompanyIdForUpdate(company.getCompanyId()))
                .thenReturn(Optional.of(company));
        when(suspensionRepository.existsByCompany_IdAndReasonCodeAndResolvedAtIsNull(
                company.getId(), request.getReasonCode())).thenReturn(false);
        when(suspensionRepository.save(any(CompanySuspension.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(suspensionRepository.countByCompany_IdAndResolvedAtIsNull(company.getId()))
                .thenReturn(1L);

        CompanySuspensionResponse response = service.createSuspension(company.getCompanyId(), request);

        assertThat(response.getCompanyStatus()).isEqualTo(CompanyStatus.SUSPENDED);
        assertThat(response.getActiveSuspensionCount()).isEqualTo(1);
        assertThat(response.getSuspension().getReasonDetail()).isEqualTo("Policy violation");
        assertThat(company.getIsActive()).isFalse();
        verify(notificationService).sendSuspensionNotification(company);
    }

    @Test
    void resolvingOneOfMultipleBlockersKeepsCompanySuspended() {
        CompanySuspension suspension = activeSuspension(SuspensionReasonCode.SECURITY);
        ResolveCompanySuspensionRequest request = new ResolveCompanySuspensionRequest();
        request.setResolutionNote(" Security review completed ");
        company.setCompanyStatus(CompanyStatus.SUSPENDED);
        company.setIsActive(false);

        when(companyRepository.findByCompanyIdForUpdate(company.getCompanyId()))
                .thenReturn(Optional.of(company));
        when(suspensionRepository.findForUpdate(company.getCompanyId(), suspension.getId()))
                .thenReturn(Optional.of(suspension));
        when(suspensionRepository.countByCompany_IdAndResolvedAtIsNull(company.getId()))
                .thenReturn(1L);

        CompanySuspensionResponse response = service.resolveSuspension(
                company.getCompanyId(),
                suspension.getId(),
                request
        );

        assertThat(response.getCompanyStatus()).isEqualTo(CompanyStatus.SUSPENDED);
        assertThat(response.getActiveSuspensionCount()).isEqualTo(1);
        assertThat(suspension.getResolutionNote()).isEqualTo("Security review completed");
        verify(notificationService, never()).sendReactivationNotification(company);
    }

    @Test
    void resolvingLastBlockerReactivatesCompanyWithoutPayment() {
        CompanySuspension suspension = activeSuspension(SuspensionReasonCode.ADMINISTRATIVE);
        ResolveCompanySuspensionRequest request = new ResolveCompanySuspensionRequest();
        request.setResolutionNote(" Administrative review completed ");
        company.setCompanyStatus(CompanyStatus.SUSPENDED);
        company.setIsActive(false);
        when(companyRepository.findByCompanyIdForUpdate(company.getCompanyId()))
                .thenReturn(Optional.of(company));
        when(suspensionRepository.findForUpdate(company.getCompanyId(), suspension.getId()))
                .thenReturn(Optional.of(suspension));
        when(suspensionRepository.countByCompany_IdAndResolvedAtIsNull(company.getId()))
                .thenReturn(0L);

        CompanySuspensionResponse response = service.resolveSuspension(
                company.getCompanyId(),
                suspension.getId(),
                request
        );

        assertThat(response.getCompanyStatus()).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(response.getActiveSuspensionCount()).isZero();
        assertThat(company.getIsActive()).isTrue();
        assertThat(suspension.getResolutionNote()).isEqualTo("Administrative review completed");
        verify(notificationService).sendReactivationNotification(company);
    }

    private CompanySuspension activeSuspension(SuspensionReasonCode reasonCode) {
        return CompanySuspension.builder()
                .id(11)
                .company(company)
                .reasonCode(reasonCode)
                .reasonDetail("Reason")
                .build();
    }
}
