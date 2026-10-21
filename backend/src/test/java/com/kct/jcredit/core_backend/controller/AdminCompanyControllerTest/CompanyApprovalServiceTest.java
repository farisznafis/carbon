package com.kct.jcredit.core_backend.controller.AdminCompanyControllerTest;

import com.kct.jcredit.core_backend.config.SecurityFlowProperties;
import com.kct.jcredit.core_backend.constant.*;
import com.kct.jcredit.core_backend.entity.*;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.repository.postgres.*;
import com.kct.jcredit.core_backend.service.*;
import com.kct.jcredit.core_backend.service.impl.CompanyApprovalServiceImpl;
import com.kct.jcredit.core_backend.dto.response.ApproveCompanyResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(MockitoExtension.class)
class CompanyApprovalServiceTest {

    @Mock private ParticipatingCompanyRepository companyRepository;
    @Mock private ParticipatingUserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private NotificationService notificationService;
    @Mock private CompanyLifecycleService companyLifecycleService;
    @Mock private CompanySecurityFlowService companySecurityFlowService;
    @Mock private SecurityFlowProperties securityFlowProperties;
    @Mock private SecurityFlowProperties.Features securityFlowFeatures;

    @InjectMocks
    private CompanyApprovalServiceImpl companyApprovalService;

    // ------------------------------------------------------------------------
    // approveCompany
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/companies/{companyId}/approve のテスト")
    class ApproveCompanyTest {

        @Test
        @DisplayName("UT-ACA-001: companyIdが存在しない場合、NOT_FOUNDを返し他の処理は実行されない")
        void approveCompany_nonExistingCompanyId_returnsNotFound() {
            when(companyRepository.findByCompanyId("COMP-404")).thenReturn(Optional.empty());
            ApproveCompanyResponse response = companyApprovalService.approveCompany("COMP-404");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            verifyNoInteractions(userRepository, companyLifecycleService, notificationService, companySecurityFlowService);
            // Input: companyId="COMP-404"
            //        companyRepository.findByCompanyId("COMP-404") が Optional.empty を返すようモック
            // Output: statusCode=NOT_FOUND, message=MSG_NOT_FOUND
            //         userRepository / companyLifecycleService / notificationService /
            //         companySecurityFlowService が一度も呼ばれないこと（verify(never())）
        }

        @Test
        @DisplayName("UT-ACA-002: companyStatusがPENDING以外の場合、BAD_REQUESTを返し他の処理は実行されない")
        void approveCompany_statusNotPending_returnsBadRequest() {
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company(CompanyStatus.ACTIVE)));
            assertThat(companyApprovalService.approveCompany("COMP-001").getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
            verifyNoInteractions(userRepository, companyLifecycleService);
            // Input: companyId="COMP-001"
            //        companyRepository.findByCompanyId(...) が companyStatus=ACTIVE の
            //        ParticipatingCompany を返すようモック
            // Output: statusCode=BAD_REQUEST, message=MSG_COMPANY_NOT_PENDING
            //         userRepository.findByCompany_IdAndRole 等が呼ばれないこと
        }

        @Test
        @DisplayName("UT-ACA-003: 既にMASTERロールユーザーが存在する場合、BAD_REQUESTを返す")
        void approveCompany_masterUserAlreadyExists_returnsBadRequest() {
            ParticipatingCompany company = company(CompanyStatus.PENDING);
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            when(userRepository.findByCompany_IdAndRole(any(), eq(RoleConstants.MASTER))).thenReturn(Optional.of(mock(ParticipatingUser.class)));
            assertThat(companyApprovalService.approveCompany("COMP-001").getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
            verifyNoInteractions(companyLifecycleService);
            // Input: companyId="COMP-001"
            //        companyRepository.findByCompanyId(...) が companyStatus=PENDING の company を返すようモック
            //        userRepository.findByCompany_IdAndRole(company.getId(), MASTER) が
            //        既存のParticipatingUserを返すようモック（Optional.of）
            // Output: statusCode=BAD_REQUEST, message=MSG_MASTER_USER_EXISTS
            //         companyLifecycleService.activatePendingCompany が呼ばれないこと
        }

        @Test
        @DisplayName("UT-ACA-004: masterSetupLink機能が有効な場合、companyをAPPROVED_PENDING_SETUPに変更しリンクを発行する")
        void approveCompany_masterSetupLinkFeatureEnabled_setsPendingSetupAndIssuesLink() {
            ParticipatingCompany company = company(CompanyStatus.PENDING);
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            when(userRepository.findByCompany_IdAndRole(any(), eq(RoleConstants.MASTER))).thenReturn(Optional.empty());
            when(securityFlowProperties.getFeatures()).thenReturn(securityFlowFeatures);
            when(securityFlowFeatures.isMasterSetupLink()).thenReturn(true);
            ApproveCompanyResponse response = companyApprovalService.approveCompany("COMP-001");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            verify(companyRepository).save(company);
            verify(companySecurityFlowService).issueMasterSetupLink(company, "operator");
            verify(userRepository, never()).save(any());
            verify(notificationService, never()).sendMasterAccountCredentials(any(), anyString(), anyString());
            // Input: companyId="COMP-001"
            //        companyRepository.findByCompanyId(...) が companyStatus=PENDING の company を返すようモック
            //        userRepository.findByCompany_IdAndRole(...) が Optional.empty を返すようモック
            //        securityFlowProperties.getFeatures().isMasterSetupLink() が true を返すようモック
            // Output: statusCode=OK, message="Company approved; master account setup is pending."
            //         companyId=companyId, masterUsername=null
            //         company.getCompanyStatus()がAPPROVED_PENDING_SETUP、isActive=falseに変更された状態で
            //         companyRepository.save(company) が呼ばれること
            //         companySecurityFlowService.issueMasterSetupLink(company, "operator") が呼ばれること
            //         userRepository.save / notificationService.sendMasterAccountCredentials が
            //         呼ばれないこと（通常フローに入らないこと）
        }

        @Test
        @DisplayName("UT-ACA-005: masterSetupLink機能が無効かつmasterUsername（email）が既存usernameと重複する場合、USED_USERNAMEを返す")
        void approveCompany_usernameAlreadyExists_returnsUsedUsername() {
            ParticipatingCompany company = company(CompanyStatus.PENDING);
            company.setEmail("taken@example.com");
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            when(userRepository.findByCompany_IdAndRole(any(), eq(RoleConstants.MASTER))).thenReturn(Optional.empty());
            when(securityFlowProperties.getFeatures()).thenReturn(securityFlowFeatures);
            when(securityFlowFeatures.isMasterSetupLink()).thenReturn(false);
            when(userRepository.existsByUsername("taken@example.com")).thenReturn(true);
            assertThat(companyApprovalService.approveCompany("COMP-001").getStatusCode()).isEqualTo(ResponseConstants.USED_USERNAME);
            // Input: companyId="COMP-001"
            //        companyRepository.findByCompanyId(...) が companyStatus=PENDING、email="taken@example.com" の
            //        company を返すようモック
            //        userRepository.findByCompany_IdAndRole(...) が Optional.empty を返すようモック
            //        securityFlowProperties.getFeatures().isMasterSetupLink() が false を返すようモック
            //        userRepository.existsByUsername("taken@example.com") が true を返すようモック
            // Output: statusCode=USED_USERNAME, message=MSG_USED_USERNAME
            //         userRepository.existsByEmail が呼ばれないこと
            //         companyLifecycleService.activatePendingCompany が呼ばれないこと
        }

        @Test
        @DisplayName("UT-ACA-006: masterUsername（email）は未使用だがemailが既存ユーザーのemailと重複する場合、USED_EMAILを返す")
        void approveCompany_emailAlreadyExists_returnsUsedEmail() {
            ParticipatingCompany company = company(CompanyStatus.PENDING);
            company.setEmail("dup@example.com");
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            when(userRepository.findByCompany_IdAndRole(any(), eq(RoleConstants.MASTER))).thenReturn(Optional.empty());
            when(securityFlowProperties.getFeatures()).thenReturn(securityFlowFeatures);
            when(securityFlowFeatures.isMasterSetupLink()).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);
            assertThat(companyApprovalService.approveCompany("COMP-001").getStatusCode()).isEqualTo(ResponseConstants.USED_EMAIL);
            // Input: companyId="COMP-001"
            //        companyRepository.findByCompanyId(...) が companyStatus=PENDING、email="dup@example.com" の
            //        company を返すようモック
            //        userRepository.findByCompany_IdAndRole(...) が Optional.empty を返すようモック
            //        securityFlowProperties.getFeatures().isMasterSetupLink() が false を返すようモック
            //        userRepository.existsByUsername("dup@example.com") が false を返すようモック
            //        userRepository.existsByEmail("dup@example.com") が true を返すようモック
            // Output: statusCode=USED_EMAIL, message=MSG_USED_EMAIL
            //         companyLifecycleService.activatePendingCompany が呼ばれないこと
        }

        @Test
        @DisplayName("UT-ACA-007: 正常系（masterSetupLink機能無効、重複なし）の場合、masterユーザーが作成され会社がactivate、通知が送信される")
        void approveCompany_validRequestWithoutFeatureFlag_createsMasterUserAndActivatesCompany() {
            ParticipatingCompany company = company(CompanyStatus.PENDING);
            company.setEmail("new@example.com");
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            when(userRepository.findByCompany_IdAndRole(any(), eq(RoleConstants.MASTER))).thenReturn(Optional.empty());
            when(securityFlowProperties.getFeatures()).thenReturn(securityFlowFeatures);
            when(securityFlowFeatures.isMasterSetupLink()).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("ENCODED");
            ApproveCompanyResponse response = companyApprovalService.approveCompany("COMP-001");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            verify(companyLifecycleService).activatePendingCompany(company);
            verify(userRepository).save(any(ParticipatingUser.class));
            verify(notificationService).sendMasterAccountCredentials(eq(company), eq("new@example.com"), anyString());
            // Input: companyId="COMP-001"
            //        companyRepository.findByCompanyId(...) が companyStatus=PENDING、
            //        email="new@example.com"、rep系フィールドが設定された company を返すようモック
            //        userRepository.findByCompany_IdAndRole(...) が Optional.empty を返すようモック
            //        securityFlowProperties.getFeatures().isMasterSetupLink() が false を返すようモック
            //        userRepository.existsByUsername / existsByEmail が両方 false を返すようモック
            //        passwordEncoder.encode(any()) が固定文字列"ENCODED"を返すようモック
            // Output: statusCode=OK, message=MSG_COMPANY_APPROVED
            //         companyId=companyId, masterUsername="new@example.com"
            //         companyLifecycleService.activatePendingCompany(company) が呼ばれること
            //         userRepository.save(argThat(user ->
            //             role==MASTER && username=="new@example.com" && password=="ENCODED"))が呼ばれること
            //         notificationService.sendMasterAccountCredentials(company, "new@example.com", 生成された平文パスワード)
            //         が呼ばれること（引数のパスワードは長さ12、大文字/小文字/数字/記号を各1文字以上含むこと）
        }

        @Test
        @DisplayName("UT-ACA-008: 正常系フロー中にnotificationServiceが例外をthrowした場合、ロールバックされINTERNAL_SERVER_ERRORを返す")
        void approveCompany_notificationServiceThrowsException_rollsBackAndReturnsInternalServerError() {
            ParticipatingCompany company = company(CompanyStatus.PENDING);
            company.setEmail("new@example.com");
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            when(userRepository.findByCompany_IdAndRole(any(), eq(RoleConstants.MASTER))).thenReturn(Optional.empty());
            when(securityFlowProperties.getFeatures()).thenReturn(securityFlowFeatures);
            when(securityFlowFeatures.isMasterSetupLink()).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("ENCODED");
            doThrow(new RuntimeException("mail server down"))
                .when(notificationService).sendMasterAccountCredentials(any(), anyString(), anyString());

            try (MockedStatic<TransactionAspectSupport> mocked = mockStatic(TransactionAspectSupport.class)) {
                mocked.when(TransactionAspectSupport::currentTransactionStatus).thenReturn(mock(TransactionStatus.class));

                assertThat(companyApprovalService.approveCompany("COMP-001").getStatusCode())
                    .isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
            }
            // Input: companyId="COMP-001"
            //        companyRepository.findByCompanyId(...) が companyStatus=PENDING の company を返すようモック
            //        userRepository.findByCompany_IdAndRole(...) が Optional.empty を返すようモック
            //        securityFlowProperties.getFeatures().isMasterSetupLink() が false を返すようモック
            //        userRepository.existsByUsername / existsByEmail が両方 false を返すようモック
            //        notificationService.sendMasterAccountCredentials(any(), any(), any())が
            //        RuntimeException("mail server down")をthrowするようモック
            // Output: statusCode=INTERNAL_SERVER_ERROR, message=MSG_APPROVAL_EMAIL_FAILED
            //         companyLifecycleService.activatePendingCompany および userRepository.save は
            //         例外発生前に呼ばれていること（verifyで確認）
            //         ※ TransactionAspectSupport.currentTransactionStatus() 呼び出しは
            //           実トランザクション文脈が無いテストでは例外になる可能性があるため、
            //           実装側でトランザクション文脈を用意する必要があるかを別途検討する
        }
    }

    // ------------------------------------------------------------------------
    // rejectCompany
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/companies/{companyId}/reject のテスト")
    class RejectCompanyTest {

        @Test
        @DisplayName("UT-ACA-009: companyIdが存在しない場合、NOT_FOUNDを返し他の処理は実行されない")
        void rejectCompany_nonExistingCompanyId_returnsNotFound() {
            when(companyRepository.findByCompanyId("COMP-404")).thenReturn(Optional.empty());
            assertThat(companyApprovalService.rejectCompany("COMP-404", "不備あり").getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            // Input: companyId="COMP-404", rejectionReason="不備あり"
            //        companyRepository.findByCompanyId(...) が Optional.empty を返すようモック
            // Output: statusCode=NOT_FOUND, message=MSG_NOT_FOUND
            //         companyLifecycleService / notificationService / companySecurityFlowService
            //         が一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-ACA-010: companyStatusがPENDING以外の場合、BAD_REQUESTを返し他の処理は実行されない")
        void rejectCompany_statusNotPending_returnsBadRequest() {
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company(CompanyStatus.REJECTED)));
            assertThat(companyApprovalService.rejectCompany("COMP-001", "不備あり").getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
            // Input: companyId="COMP-001", rejectionReason="不備あり"
            //        companyRepository.findByCompanyId(...) が companyStatus=REJECTED の company を返すようモック
            // Output: statusCode=BAD_REQUEST, message=MSG_COMPANY_NOT_PENDING
            //         companyLifecycleService.rejectPendingCompany が呼ばれないこと
        }

        @Test
        @DisplayName("UT-ACA-011: rejectedSignupRevision機能が有効な場合、修正リンクを発行し通知メールは送信されない")
        void rejectCompany_revisionFeatureEnabled_issuesRevisionLinkInsteadOfNotification() {
            ParticipatingCompany company = company(CompanyStatus.PENDING);
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            when(securityFlowProperties.getFeatures()).thenReturn(securityFlowFeatures);
            when(securityFlowFeatures.isRejectedSignupRevision()).thenReturn(true);
            assertThat(companyApprovalService.rejectCompany("COMP-001", "内容不備").getStatusCode()).isEqualTo(ResponseConstants.OK);
            verify(companySecurityFlowService).issueRevisionLink(company, "内容不備", "operator");
            verify(companyLifecycleService).rejectPendingCompany(company);
            verify(notificationService, never()).sendCompanyRejectionNotification(any(), anyString());
            // Input: companyId="COMP-001", rejectionReason="内容不備のため"
            //        companyRepository.findByCompanyId(...) が companyStatus=PENDING の company を返すようモック
            //        securityFlowProperties.getFeatures().isRejectedSignupRevision() が true を返すようモック
            // Output: statusCode=OK, message=MSG_COMPANY_REJECTED, companyId=companyId
            //         companyLifecycleService.rejectPendingCompany(company) が呼ばれること
            //         companySecurityFlowService.issueRevisionLink(company, "内容不備のため", "operator") が呼ばれること
            //         notificationService.sendCompanyRejectionNotification は呼ばれないこと
        }

        @Test
        @DisplayName("UT-ACA-012: rejectedSignupRevision機能が無効な場合、通知メールが送信され修正リンクは発行されない")
        void rejectCompany_revisionFeatureDisabled_sendsNotificationInsteadOfRevisionLink() {
            ParticipatingCompany company = company(CompanyStatus.PENDING);
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            when(securityFlowProperties.getFeatures()).thenReturn(securityFlowFeatures);
            when(securityFlowFeatures.isRejectedSignupRevision()).thenReturn(false);
            assertThat(companyApprovalService.rejectCompany("COMP-001", "内容不備").getStatusCode()).isEqualTo(ResponseConstants.OK);
            verify(notificationService).sendCompanyRejectionNotification(company, "内容不備");
            verify(companyLifecycleService).rejectPendingCompany(company);
            // Input: companyId="COMP-001", rejectionReason="内容不備のため"
            //        companyRepository.findByCompanyId(...) が companyStatus=PENDING の company を返すようモック
            //        securityFlowProperties.getFeatures().isRejectedSignupRevision() が false を返すようモック
            // Output: statusCode=OK, message=MSG_COMPANY_REJECTED, companyId=companyId
            //         companyLifecycleService.rejectPendingCompany(company) が呼ばれること
            //         notificationService.sendCompanyRejectionNotification(company, "内容不備のため") が呼ばれること
            //         companySecurityFlowService.issueRevisionLink は呼ばれないこと
        }

        @Test
        @DisplayName("UT-ACA-013: securityFlowPropertiesがnullの場合、通知メール送信フローにフォールバックする")
        void rejectCompany_securityFlowPropertiesNull_fallsBackToNotificationFlow() {
            ParticipatingCompany company = company(CompanyStatus.PENDING);
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));

            try (MockedStatic<TransactionAspectSupport> mocked = mockStatic(TransactionAspectSupport.class)) {
                mocked.when(TransactionAspectSupport::currentTransactionStatus).thenReturn(mock(TransactionStatus.class));

                assertThat(companyApprovalService.rejectCompany("COMP-001", "不備あり").getStatusCode())
                    .isEqualTo(ResponseConstants.OK);
                verify(notificationService).sendCompanyRejectionNotification(company, "不備あり");
            }
            // Input: companyId="COMP-001", rejectionReason="不備あり"
            //        companyApprovalServiceのsecurityFlowPropertiesフィールドをnullにセット（テスト内で再構築 or setterが無い場合はReflection）
            //        companyRepository.findByCompanyId(...) が companyStatus=PENDING の company を返すようモック
            // Output: statusCode=OK, message=MSG_COMPANY_REJECTED
            //         notificationService.sendCompanyRejectionNotification(company, "不備あり") が呼ばれること
            //         companySecurityFlowService.issueRevisionLink は呼ばれないこと
        }

        @Test
        @DisplayName("UT-ACA-014: 処理中にcompanyLifecycleServiceが例外をthrowした場合、ロールバックされINTERNAL_SERVER_ERRORを返す")
        void rejectCompany_lifecycleServiceThrowsException_rollsBackAndReturnsInternalServerError() {
            ParticipatingCompany company = company(CompanyStatus.PENDING);
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            doThrow(new RuntimeException("db error")).when(companyLifecycleService).rejectPendingCompany(company);

            try (MockedStatic<TransactionAspectSupport> mocked = mockStatic(TransactionAspectSupport.class)) {
                mocked.when(TransactionAspectSupport::currentTransactionStatus).thenReturn(mock(TransactionStatus.class));

                assertThat(companyApprovalService.rejectCompany("COMP-001", "不備あり").getStatusCode())
                    .isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
                    verifyNoInteractions(notificationService);
            }

                    // Input: companyId="COMP-001", rejectionReason="不備あり"
                    //        companyRepository.findByCompanyId(...) が companyStatus=PENDING の company を返すようモック
                    //        companyLifecycleService.rejectPendingCompany(company) が
                    //        RuntimeException("db error")をthrowするようモック
                    // Output: statusCode=INTERNAL_SERVER_ERROR, message=MSG_COMPANY_REJECT_FAILED
                    //         notificationService / companySecurityFlowService のリンク発行・通知メソッドは
                    //         呼ばれないこと（例外がrejectPendingCompany呼び出し直後に発生するため）
        }
    }

    // ------------------------------------------------------------------------
    // resendMasterSetupLink
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/companies/{companyId}/master-setup-link/resend のテスト")
    class ResendMasterSetupLinkTest {

        @Test
        @DisplayName("UT-ACA-015: masterSetupLink機能が無効な場合、NOT_FOUNDを返しRepositoryは呼ばれない")
        void resendMasterSetupLink_featureDisabled_returnsNotFoundWithoutRepositoryAccess() {
            when(securityFlowProperties.getFeatures()).thenReturn(securityFlowFeatures);
            when(securityFlowFeatures.isMasterSetupLink()).thenReturn(false);
            assertThat(companyApprovalService.resendMasterSetupLink("COMP-001").getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            verifyNoInteractions(companyRepository);
            // Input: companyId="COMP-001"
            //        securityFlowProperties.getFeatures().isMasterSetupLink() が false を返すようモック
            // Output: statusCode=NOT_FOUND, message=MSG_NOT_FOUND
            //         companyRepository.findByCompanyId が一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-ACA-016: securityFlowPropertiesがnullの場合、NOT_FOUNDを返す")
        void resendMasterSetupLink_securityFlowPropertiesNull_returnsNotFound() {
            ReflectionTestUtils.setField(companyApprovalService, "securityFlowProperties", null);

            assertThat(companyApprovalService.resendMasterSetupLink("COMP-001").getStatusCode())
                .isEqualTo(ResponseConstants.NOT_FOUND);
            verifyNoInteractions(companyRepository);
            // Input: companyId="COMP-001"
            //        companyApprovalServiceのsecurityFlowPropertiesフィールドをnullにセット
            // Output: statusCode=NOT_FOUND, message=MSG_NOT_FOUND
            //         companyRepository.findByCompanyId が一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-ACA-017: 機能有効だがcompanyIdが存在しない場合、NOT_FOUNDを返す")
        void resendMasterSetupLink_featureEnabledButCompanyNotFound_returnsNotFound() {
            when(securityFlowProperties.getFeatures()).thenReturn(securityFlowFeatures);
            when(securityFlowFeatures.isMasterSetupLink()).thenReturn(true);
            when(companyRepository.findByCompanyId("COMP-404")).thenReturn(Optional.empty());
            assertThat(companyApprovalService.resendMasterSetupLink("COMP-404").getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            // Input: companyId="COMP-404"
            //        securityFlowProperties.getFeatures().isMasterSetupLink() が true を返すようモック
            //        companyRepository.findByCompanyId("COMP-404") が Optional.empty を返すようモック
            // Output: statusCode=NOT_FOUND, message=MSG_NOT_FOUND
            //         companySecurityFlowService.issueMasterSetupLink が呼ばれないこと
        }

        @Test
        @DisplayName("UT-ACA-018: companyStatusがAPPROVED_PENDING_SETUP以外の場合、CONFLICTを返す")
        void resendMasterSetupLink_statusNotApprovedPendingSetup_returnsConflict() {
            when(securityFlowProperties.getFeatures()).thenReturn(securityFlowFeatures);
            when(securityFlowFeatures.isMasterSetupLink()).thenReturn(true);
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company(CompanyStatus.ACTIVE)));
            assertThat(companyApprovalService.resendMasterSetupLink("COMP-001").getStatusCode()).isEqualTo(ResponseConstants.CONFLICT);
            // Input: companyId="COMP-001"
            //        securityFlowProperties.getFeatures().isMasterSetupLink() が true を返すようモック
            //        companyRepository.findByCompanyId(...) が companyStatus=ACTIVE の company を返すようモック
            // Output: statusCode=CONFLICT, message=MSG_COMPANY_STATUS_CONFLICT
            //         companySecurityFlowService.issueMasterSetupLink が呼ばれないこと
        }

        @Test
        @DisplayName("UT-ACA-019: 正常系（機能有効・companyIdが存在・status=APPROVED_PENDING_SETUP）の場合、リンクが再送される")
        void resendMasterSetupLink_validRequest_reissuesLink() {
            ParticipatingCompany company = company(CompanyStatus.APPROVED_PENDING_SETUP);
            when(securityFlowProperties.getFeatures()).thenReturn(securityFlowFeatures);
            when(securityFlowFeatures.isMasterSetupLink()).thenReturn(true);
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            assertThat(companyApprovalService.resendMasterSetupLink("COMP-001").getStatusCode()).isEqualTo(ResponseConstants.OK);
            verify(companySecurityFlowService).issueMasterSetupLink(company, "operator");
            // Input: companyId="COMP-001"
            //        securityFlowProperties.getFeatures().isMasterSetupLink() が true を返すようモック
            //        companyRepository.findByCompanyId(...) が
            //        companyStatus=APPROVED_PENDING_SETUP の company を返すようモック
            // Output: statusCode=OK, message="Master account setup link resent.", companyId=companyId
            //         companySecurityFlowService.issueMasterSetupLink(company, "operator") が呼ばれること
        }
    }

    private ParticipatingCompany company(CompanyStatus status) {
        ParticipatingCompany company = new ParticipatingCompany();
        company.setId(1);
        company.setCompanyId("COMP-001");
        company.setCompanyStatus(status);
        return company;
    }
}