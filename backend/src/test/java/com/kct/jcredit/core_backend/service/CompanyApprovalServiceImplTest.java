package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.response.ApproveCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.PendingCompanyListResponse;
import com.kct.jcredit.core_backend.dto.response.RejectCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.details.CompanyInfoResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.service.CompanyApprovalService;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.impl.CompanyApprovalServiceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.TransactionStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyApprovalServiceImpl のテスト")
class CompanyApprovalServiceImplTest {

    private static final String REG_STATUS_PENDING = "PENDING";
    private static final String REG_STATUS_APPROVED = "APPROVED";
    private static final String REG_STATUS_REJECTED = "REJECTED";

    @Mock
    private ParticipatingCompanyRepository companyRepository;

    @Mock
    private ParticipatingUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CompanyApprovalServiceImpl companyApprovalService;

    @Test
    @DisplayName("UT-AS-001: 全ての検証を通過し通知が成功した場合、会社が承認されマスターアカウントが作成される")
    void approveCompany_allValidationsPassAndNotificationSucceeds_returnsOkWithMasterAccount() {
        // Arrange
        String companyId = "001";
        Integer Id = 123;
        String role = "master";
        String repEmail = "rep@example.com";

        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getRegStatus()).thenReturn(REG_STATUS_PENDING);
        when(company.getRepEmail()).thenReturn(repEmail);
        when(company.getRepFirstName()).thenReturn("Taro");
        when(company.getRepFirstNameKana()).thenReturn("タロウ");
        when(company.getRepLastName()).thenReturn("Yamada");
        when(company.getRepLastNameKana()).thenReturn("ヤマダ");
        when(company.getRepPhoneNumber()).thenReturn("09012345678");
        when(company.getRepPosition()).thenReturn("代表取締役");

        when(companyRepository.findByCompanyId(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findByCompany_IdAndRole(Id, role))
                .thenReturn(Optional.empty());
        when(userRepository.existsByUsername(repEmail)).thenReturn(false);
        when(userRepository.existsByEmail(repEmail)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("ENCODED_PASSWORD");

        // Act
        ApproveCompanyResponse response = companyApprovalService.approveCompany(companyId);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getCompanyId()).isEqualTo(companyId);
        assertThat(response.getMasterUsername()).isEqualTo(repEmail);

        verify(company).setRegStatus(REG_STATUS_APPROVED);
        verify(companyRepository).save(company);

        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository).save(userCaptor.capture());
        ParticipatingUser savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo(repEmail);
        assertThat(savedUser.getEmail()).isEqualTo(repEmail);
        assertThat(savedUser.getRole()).isEqualTo(RoleConstants.MASTER);
        assertThat(savedUser.getCompany().getId()).isEqualTo(companyId);
        assertThat(savedUser.getPassword()).isEqualTo("ENCODED_PASSWORD");

        verify(notificationService).sendMasterAccountCredentials(eq(company), eq(repEmail), anyString());
    }

    @Test
    @DisplayName("UT-AS-001: regStatus が小文字 'pending' でも大文字小文字を区別せず処理が継続される")
    void approveCompany_regStatusPendingLowercase_proceedsSuccessfully() {
        // Arrange
        String companyId = "COMP002";
        Integer Id = 123;
        String role = "master";
        String repEmail = "rep2@example.com";

        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getRegStatus()).thenReturn("pending"); //verifikasi equalsIgnoreCase
        when(company.getRepEmail()).thenReturn(repEmail);

        when(companyRepository.findByCompanyId(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findByCompany_IdAndRole(Id, role))
                .thenReturn(Optional.empty());
        when(userRepository.existsByUsername(repEmail)).thenReturn(false);
        when(userRepository.existsByEmail(repEmail)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("ENCODED_PASSWORD");

        // Act
        ApproveCompanyResponse response = companyApprovalService.approveCompany(companyId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        verify(companyRepository).save(company);
        verify(notificationService).sendMasterAccountCredentials(any(), any(), any());
    }


    @Test
    @DisplayName("UT-AS-003: companyId に対応する会社が見つからない場合、NOT_FOUND を返す")
    void approveCompany_companyNotFound_returnsNotFound() {
        // Arrange
        String companyId = "COMP999";
        when(companyRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());

        // Act
        ApproveCompanyResponse response = companyApprovalService.approveCompany(companyId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);

        verifyNoInteractions(userRepository, passwordEncoder, notificationService);
        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-AS-004: regStatus が null の場合、BAD_REQUEST を返す")
    void approveCompany_regStatusNull_returnsBadRequest() {
        // Arrange
        String companyId = "COMP001";
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getRegStatus()).thenReturn(null);
        when(companyRepository.findByCompanyId(companyId)).thenReturn(Optional.of(company));

        // Act
        ApproveCompanyResponse response = companyApprovalService.approveCompany(companyId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_COMPANY_NOT_PENDING);

        verifyNoInteractions(userRepository, passwordEncoder, notificationService);
        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-AS-005: regStatus が PENDING ではない場合（既に処理済み）、BAD_REQUEST を返す")
    void approveCompany_regStatusNotPending_returnsBadRequest() {
        // Arrange
        String companyId = "COMP001";
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getRegStatus()).thenReturn(REG_STATUS_APPROVED);
        when(companyRepository.findByCompanyId(companyId)).thenReturn(Optional.of(company));

        // Act
        ApproveCompanyResponse response = companyApprovalService.approveCompany(companyId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_COMPANY_NOT_PENDING);

        verifyNoInteractions(userRepository, passwordEncoder, notificationService);
        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-AS-006: 既にマスターユーザーが存在する場合、BAD_REQUEST を返す")
    void approveCompany_masterUserAlreadyExists_returnsBadRequest() {
        // Arrange
        String companyId = "COMP001";
        Integer Id = 123;
        String role = "master";
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getRegStatus()).thenReturn(REG_STATUS_PENDING);
        when(companyRepository.findByCompanyId(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findByCompany_IdAndRole(Id, role))
                .thenReturn(Optional.of(mock(ParticipatingUser.class)));

        // Act
        ApproveCompanyResponse response = companyApprovalService.approveCompany(companyId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_MASTER_USER_EXISTS);

        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).existsByEmail(any());
        verifyNoInteractions(passwordEncoder, notificationService);
        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-AS-007: ユーザー名が既に使用されている場合、USED_USERNAME を返す")
    void approveCompany_usernameAlreadyUsed_returnsUsedUsername() {
        // Arrange
        String companyId = "COMP001";
        Integer Id = 123;
        String role = "master";
        String repEmail = "rep@example.com";
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getRegStatus()).thenReturn(REG_STATUS_PENDING);
        when(company.getRepEmail()).thenReturn(repEmail);
        when(companyRepository.findByCompanyId(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findByCompany_IdAndRole(Id, role))
                .thenReturn(Optional.empty());
        when(userRepository.existsByUsername(repEmail)).thenReturn(true);

        // Act
        ApproveCompanyResponse response = companyApprovalService.approveCompany(companyId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.USED_USERNAME);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_USED_USERNAME);

        verify(userRepository, never()).existsByEmail(any());
        verifyNoInteractions(passwordEncoder, notificationService);
        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-AS-008: メールアドレスが既に使用されている場合、USED_EMAIL を返す")
    void approveCompany_emailAlreadyUsed_returnsUsedEmail() {
        // Arrange
        String companyId = "COMP001";
        Integer Id = 123;
        String role = "master";
        String repEmail = "rep@example.com";
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getRegStatus()).thenReturn(REG_STATUS_PENDING);
        when(company.getRepEmail()).thenReturn(repEmail);
        when(companyRepository.findByCompanyId(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findByCompany_IdAndRole(Id, role))
                .thenReturn(Optional.empty());
        when(userRepository.existsByUsername(repEmail)).thenReturn(false);
        when(userRepository.existsByEmail(repEmail)).thenReturn(true);

        // Act
        ApproveCompanyResponse response = companyApprovalService.approveCompany(companyId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.USED_EMAIL);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_USED_EMAIL);

        verifyNoInteractions(passwordEncoder, notificationService);
        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-AS-009: 通知メール送信が失敗した場合、INTERNAL_SERVER_ERROR を返す（ロールバック対象）")
    void approveCompany_notificationFails_returnsInternalServerError() {
        // Arrange
        String companyId = "COMP001";
        Integer Id = 123;
        String role = "master";
        String repEmail = "rep@example.com";
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getRegStatus()).thenReturn(REG_STATUS_PENDING);
        when(company.getRepEmail()).thenReturn(repEmail);
        when(companyRepository.findByCompanyId(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findByCompany_IdAndRole(Id, role))
                .thenReturn(Optional.empty());
        when(userRepository.existsByUsername(repEmail)).thenReturn(false);
        when(userRepository.existsByEmail(repEmail)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("ENCODED_PASSWORD");
        doThrow(new RuntimeException("SES timeout"))
                .when(notificationService).sendMasterAccountCredentials(any(), any(), any());

        try (MockedStatic<TransactionAspectSupport> mockedTx = mockStatic(TransactionAspectSupport.class)) {
            TransactionStatus mockStatus = mock(TransactionStatus.class);
            mockedTx.when(TransactionAspectSupport::currentTransactionStatus).thenReturn(mockStatus);

            // Act
            ApproveCompanyResponse response = companyApprovalService.approveCompany(companyId);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_APPROVAL_EMAIL_FAILED);

            verify(mockStatus).setRollbackOnly();
            verify(companyRepository).save(company);
            verify(userRepository).save(any(ParticipatingUser.class)); 
        }
        
    }

    @Test
    @DisplayName("UT-AS-010: companyRepository.save() が失敗した場合、graceful に INTERNAL_SERVER_ERROR を返すべき")
    void approveCompany_companySaveThrows_shouldReturnInternalServerErrorGracefully() {
        // Arrange
        String companyId = "COMP001";
        Integer Id = 123;
        String role = "master";
        String repEmail = "rep@example.com";
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getRegStatus()).thenReturn(REG_STATUS_PENDING);
        when(company.getRepEmail()).thenReturn(repEmail);
        when(companyRepository.findByCompanyId(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findByCompany_IdAndRole(Id, role))
                .thenReturn(Optional.empty());
        when(userRepository.existsByUsername(repEmail)).thenReturn(false);
        when(userRepository.existsByEmail(repEmail)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("ENCODED_PASSWORD");
        when(companyRepository.save(company)).thenThrow(new DataAccessResourceFailureException("DB down"));

        try (MockedStatic<TransactionAspectSupport> mockedTx = mockStatic(TransactionAspectSupport.class)) {
            TransactionStatus mockStatus = mock(TransactionStatus.class);
            mockedTx.when(TransactionAspectSupport::currentTransactionStatus).thenReturn(mockStatus);
            ApproveCompanyResponse response = companyApprovalService.approveCompany(companyId);

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
            verify(notificationService, never()).sendMasterAccountCredentials(any(), any(), any());
        }
    }

    @Test
    @DisplayName("UT-AS-011: 会社が PENDING で通知が成功した場合、却下され通知が送信される")
    void rejectCompany_companyPendingAndNotificationSucceeds_returnsOk() {
        // Arrange
        String companyId = "COMP001";
        String rejectionReason = "申請内容に不備があります";
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getRegStatus()).thenReturn(REG_STATUS_PENDING);
        when(companyRepository.findByCompanyId(companyId)).thenReturn(Optional.of(company));

        // Act
        RejectCompanyResponse response = companyApprovalService.rejectCompany(companyId, rejectionReason);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getCompanyId()).isEqualTo(companyId);

        verify(company).setRegStatus(REG_STATUS_REJECTED);
        verify(companyRepository).save(company);
        verify(notificationService).sendCompanyRejectionNotification(company, rejectionReason);
    }

    @Test
    @DisplayName("UT-AS-012: companyId に対応する会社が見つからない場合、NOT_FOUND を返す")
    void rejectCompany_companyNotFound_returnsNotFound() {
        // Arrange
        String companyId = "COMP999";
        when(companyRepository.findByCompanyId(companyId)).thenReturn(Optional.empty());

        // Act
        RejectCompanyResponse response = companyApprovalService.rejectCompany(companyId, "");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);

        verifyNoInteractions(notificationService);
        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-AS-013: regStatus が PENDING ではない場合、BAD_REQUEST を返す")
    void rejectCompany_regStatusNotPending_returnsBadRequest() {
        // Arrange
        String companyId = "COMP001";
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getRegStatus()).thenReturn(REG_STATUS_APPROVED);
        when(companyRepository.findByCompanyId(companyId)).thenReturn(Optional.of(company));

        // Act
        RejectCompanyResponse response = companyApprovalService.rejectCompany(companyId, "");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_COMPANY_NOT_PENDING);

        verifyNoInteractions(notificationService);
        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-AS-014: companyRepository.save() が失敗した場合、INTERNAL_SERVER_ERROR を返す")
    void rejectCompany_saveThrows_returnsInternalServerError() {
        // Arrange
        String companyId = "COMP001";
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getRegStatus()).thenReturn(REG_STATUS_PENDING);
        when(companyRepository.findByCompanyId(companyId)).thenReturn(Optional.of(company));
        when(companyRepository.save(company)).thenThrow(new DataAccessResourceFailureException("DB down"));

        try (MockedStatic<TransactionAspectSupport> mockedTx = mockStatic(TransactionAspectSupport.class)) {
            TransactionStatus mockStatus = mock(TransactionStatus.class);
            mockedTx.when(TransactionAspectSupport::currentTransactionStatus).thenReturn(mockStatus);

            // Act
            RejectCompanyResponse response =
                    companyApprovalService.rejectCompany(companyId, "alasan apapun");

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_COMPANY_REJECT_FAILED);

            verify(mockStatus).setRollbackOnly();
            verifyNoInteractions(notificationService);
        }
    }

    @Test
    @DisplayName("UT-AS-015: save() は成功するが通知送信が失敗した場合、INTERNAL_SERVER_ERROR を返す")
    void rejectCompany_notificationThrows_returnsInternalServerError() {
        // Arrange
        String companyId = "COMP001";
        String rejectionReason = "申請内容に不備があります";
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getRegStatus()).thenReturn(REG_STATUS_PENDING);
        when(companyRepository.findByCompanyId(companyId)).thenReturn(Optional.of(company));
        doThrow(new RuntimeException("SES timeout"))
                .when(notificationService).sendCompanyRejectionNotification(company, rejectionReason);

        try (MockedStatic<TransactionAspectSupport> mockedTx = mockStatic(TransactionAspectSupport.class)) {
            TransactionStatus mockStatus = mock(TransactionStatus.class);
            mockedTx.when(TransactionAspectSupport::currentTransactionStatus).thenReturn(mockStatus);

            // Act
            RejectCompanyResponse response =
                    companyApprovalService.rejectCompany(companyId, rejectionReason);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_COMPANY_REJECT_FAILED);

            verify(mockStatus).setRollbackOnly();
            verify(companyRepository).save(company); // save SUDAH terjadi sebelum notifikasi gagal
        }
    }

    @Test
    @DisplayName("UT-AS-016: 承認待ちの会社が存在する場合、フィールドが正しくマッピングされる（id が null の場合も含む）")
    void getPendingCompanies_hasDataIncludingNullId_mapsCorrectly() {
        // Arrange
        ParticipatingCompany companyWithId = mock(ParticipatingCompany.class);
        when(companyWithId.getId()).thenReturn(10);
        when(companyWithId.getCompanyId()).thenReturn("COMP001");
        when(companyWithId.getNameJa()).thenReturn("株式会社サンプル");
        when(companyWithId.getNameFurigana()).thenReturn("カブシキガイシャサンプル");
        when(companyWithId.getNameEn()).thenReturn("Sample Co., Ltd.");
        when(companyWithId.getEmail()).thenReturn("info@sample.co.jp");
        when(companyWithId.getPhoneNumber()).thenReturn("0123456789");
        when(companyWithId.getFax()).thenReturn("0123456780");
        when(companyWithId.getCorpNumber()).thenReturn("1234567890123");
        when(companyWithId.getBuildingName()).thenReturn("熊本県熊本市");
        when(companyWithId.isSuspended()).thenReturn(false);

        ParticipatingCompany companyWithNullId = mock(ParticipatingCompany.class);
        when(companyWithNullId.getId()).thenReturn(null);
        when(companyWithNullId.getCompanyId()).thenReturn("COMP002");

        when(companyRepository.findByRegStatus(REG_STATUS_PENDING))
                .thenReturn(Arrays.asList(companyWithId, companyWithNullId));

        // Act
        PendingCompanyListResponse response = companyApprovalService.getPendingCompanies();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getCompanies()).hasSize(2);

        CompanyInfoResponse first = response.getCompanies().get(0);
        assertThat(first.getId()).isEqualTo("10");
        assertThat(first.getCompanyId()).isEqualTo("COMP001");
        assertThat(first.getNameJA()).isEqualTo("株式会社サンプル");
        assertThat(first.getNameFurigana()).isEqualTo("カブシキガイシャサンプル");
        assertThat(first.getNameEN()).isEqualTo("Sample Co., Ltd.");
        assertThat(first.getEmail()).isEqualTo("info@sample.co.jp");
        assertThat(first.getPhoneNumber()).isEqualTo("0123456789");
        assertThat(first.getFax()).isEqualTo("0123456780");
        assertThat(first.getCorpNumber()).isEqualTo("1234567890123");
        assertThat(first.getAddress()).isEqualTo("熊本県熊本市");
        assertThat(first.getIsSuspended()).isFalse();

        CompanyInfoResponse second = response.getCompanies().get(1);
        assertThat(second.getId()).isNull(); // null-safe mapping: id null tidak menyebabkan NPE
        assertThat(second.getCompanyId()).isEqualTo("COMP002");
    }

    @Test
    @DisplayName("UT-AS-017: 承認待ちの会社が存在しない場合、空リストを返す")
    void getPendingCompanies_noData_returnsEmptyList() {
        // Arrange
        when(companyRepository.findByRegStatus(REG_STATUS_PENDING)).thenReturn(List.of());

        // Act
        PendingCompanyListResponse response = companyApprovalService.getPendingCompanies();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getCompanies()).isNotNull();
        assertThat(response.getCompanies()).isEmpty();
    }
}