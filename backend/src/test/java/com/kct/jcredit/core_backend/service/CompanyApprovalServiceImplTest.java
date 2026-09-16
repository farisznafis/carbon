package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.ApproveCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.RejectCompanyResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.service.impl.CompanyApprovalServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompanyApprovalServiceImplTest {

    @Mock
    private ParticipatingCompanyRepository companyRepository;

    @Mock
    private ParticipatingUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CompanyLifecycleService companyLifecycleService;

    @InjectMocks
    private CompanyApprovalServiceImpl service;

    @Test
    void approveCompanyActivatesCanonicalCompanyStatus() {
        ParticipatingCompany company = pendingCompany();
        when(companyRepository.findByCompanyId(company.getCompanyId())).thenReturn(Optional.of(company));
        when(userRepository.findByCompany_IdAndRole(company.getId(), "master")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername(company.getEmail())).thenReturn(false);
        when(userRepository.existsByEmail(company.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");

        ApproveCompanyResponse response = service.approveCompany(company.getCompanyId());

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        verify(companyLifecycleService).activatePendingCompany(company);
        verify(userRepository).save(any());
    }

    @Test
    void rejectCompanyRejectsCanonicalPendingStatus() {
        ParticipatingCompany company = pendingCompany();
        when(companyRepository.findByCompanyId(company.getCompanyId())).thenReturn(Optional.of(company));

        RejectCompanyResponse response = service.rejectCompany(company.getCompanyId(), "不足情報");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        verify(companyLifecycleService).rejectPendingCompany(company);
        verify(notificationService).sendCompanyRejectionNotification(company, "不足情報");
    }

    @Test
    void approveCompanyRejectsNonPendingState() {
        ParticipatingCompany company = pendingCompany();
        company.setCompanyStatus(CompanyStatus.SUSPENDED);
        when(companyRepository.findByCompanyId(company.getCompanyId())).thenReturn(Optional.of(company));

        ApproveCompanyResponse response = service.approveCompany(company.getCompanyId());

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
        verify(companyLifecycleService, never()).activatePendingCompany(any());
    }

    private ParticipatingCompany pendingCompany() {
        ParticipatingCompany company = new ParticipatingCompany();
        company.setId(10);
        company.setCompanyId("COMP00000001");
        company.setCompanyStatus(CompanyStatus.PENDING);
        company.setEmail("master@example.com");
        company.setRepFirstName("太郎");
        company.setRepLastName("山田");
        company.setRepFirstNameKana("タロウ");
        company.setRepLastNameKana("ヤマダ");
        company.setRepPhoneNumber("09012345678");
        company.setRepPosition("代表取締役");
        return company;
    }
}
