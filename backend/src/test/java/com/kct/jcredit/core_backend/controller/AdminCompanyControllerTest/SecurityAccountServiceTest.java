package com.kct.jcredit.core_backend.controller.AdminCompanyControllerTest;

import com.kct.jcredit.core_backend.config.SecurityFlowProperties;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.repository.postgres.*;
import com.kct.jcredit.core_backend.service.*;
import com.kct.jcredit.core_backend.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityAccountServiceTest {

    @Mock private SecurityFlowProperties properties;
    @Mock private SecurityFlowProperties.Features features;
    @Mock private SecurityActionLinkService actionLinkService;
    @Mock private OtpChallengeService otpChallengeService;
    @Mock private NotificationService notificationService;
    @Mock private ParticipatingCompanyRepository companyRepository;
    @Mock private ParticipatingUserRepository participatingUserRepository;
    @Mock private OperatingUserRepository operatingUserRepository;
    @Mock private UsernameAvailabilityService usernameAvailabilityService;
    @Mock private CompanyLifecycleService companyLifecycleService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthVersionService authVersionService;
    @Mock private JwtUtil jwtUtil;
    @Mock private SecurityRateLimitService rateLimitService;
    @Mock private SecurityEmailDeliveryService securityEmailDeliveryService;

    @InjectMocks
    private SecurityAccountService securityAccountService;

    // ------------------------------------------------------------------------
    // operatorRequestMasterPasswordReset
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/companies/{companyId}/password-reset-link のテスト")
    class OperatorRequestMasterPasswordResetTest {

        @Test
        @DisplayName("UT-SAS-001: companyIdが存在しない場合、CustomException(NOT_FOUND)がスローされる")
        void operatorRequestMasterPasswordReset_companyNotFound_throwsCustomException() {
            // Input: companyId="COMP-404", actor="operator"
            //        properties.getFeatures() が features を返すようモック
            //        features.isPasswordReset() が true を返すようモック
            //        companyRepository.findByCompanyId("COMP-404") が Optional.empty を返すようモック
            // Output: CustomExceptionがスローされること
            //         statusCode=ResponseConstants.NOT_FOUND, message=ResponseConstants.MSG_COMPANY_NOT_FOUND
            // 検証: participatingUserRepository.findByCompany_IdAndRole が一度も呼ばれないこと
            //       actionLinkService.issue が呼ばれないこと
            givenPasswordResetEnabled();
            when(companyRepository.findByCompanyId("COMP-404")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> securityAccountService.operatorRequestMasterPasswordReset("COMP-404", "operator"))
                .isInstanceOf(CustomException.class)
                .hasMessage(ResponseConstants.MSG_COMPANY_NOT_FOUND);
            verifyNoInteractions(participatingUserRepository, actionLinkService);
        }

        @Test
        @DisplayName("UT-SAS-002: companyIdは存在するがmasterユーザーが存在しない場合、CustomException(NOT_FOUND)がスローされる")
        void operatorRequestMasterPasswordReset_masterUserNotFound_throwsCustomException() {
            // Input: companyId="COMP-001", actor="operator"
            //        properties.getFeatures() が features を返すようモック
            //        features.isPasswordReset() が true を返すようモック
            //        companyRepository.findByCompanyId("COMP-001") が ParticipatingCompany(id=1) を返すようモック
            //        participatingUserRepository.findByCompany_IdAndRole(1, RoleConstants.MASTER)
            //        が Optional.empty を返すようモック
            // Output: CustomExceptionがスローされること
            //         statusCode=ResponseConstants.NOT_FOUND, message=ResponseConstants.MSG_MASTER_ACCOUNT_NOT_FOUND
            // 検証: actionLinkService.issue が呼ばれないこと
            givenPasswordResetEnabled();
            ParticipatingCompany company = mock(ParticipatingCompany.class);
            when(company.getId()).thenReturn(1);
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            when(participatingUserRepository.findByCompany_IdAndRole(1, RoleConstants.MASTER))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> securityAccountService.operatorRequestMasterPasswordReset("COMP-001", "operator"))
                .isInstanceOf(CustomException.class)
                .hasMessage(ResponseConstants.MSG_MASTER_ACCOUNT_NOT_FOUND);
            verifyNoInteractions(actionLinkService);
        }

        @Test
        @DisplayName("UT-SAS-003: 正常系（companyId存在・master存在）の場合、パスワードリセットリンクが発行され成功レスポンスが返る")
        void operatorRequestMasterPasswordReset_validRequest_issuesResetLinkAndReturnsSuccess() {
            // Input: companyId="COMP-001", actor="operator"
            //        properties.getFeatures() が features を返すようモック
            //        features.isPasswordReset() が true を返すようモック
            //        companyRepository.findByCompanyId("COMP-001") が ParticipatingCompany(id=1) を返すようモック
            //        participatingUserRepository.findByCompany_IdAndRole(1, RoleConstants.MASTER)
            //        が ParticipatingUser(id=10, email="master@example.com") を返すようモック
            //        actionLinkService.issue(PASSWORD_RESET, PARTICIPATING_USER, 10, "operator")
            //        が IssuedActionLink（linkId, url等ダミー値）を返すようモック
            // Output: BaseResponse.success(ResponseConstants.MSG_PASSWORD_RESET_LINK_QUEUED) と同等の内容が返る
            //         statusCode=OK, message=MSG_PASSWORD_RESET_LINK_QUEUED
            // 検証: actionLinkService.issue(PASSWORD_RESET, PARTICIPATING_USER, 10, "operator") が呼ばれること
            //       ※ afterCommit内のdeliverResetLink呼び出しはトランザクション同期のコールバックのため、
            //         単体テストではTransactionSynchronizationManagerが登録されていない環境だと
            //         即時実行されるか未実行かが実装依存になる。securityEmailDeliveryService.deliverActionLink
            //         の呼び出し検証は行わない（対象外）か、afterCommitの実装詳細を確認してから追加するか要相談
            givenPasswordResetEnabled();
            ParticipatingCompany company = mock(ParticipatingCompany.class);
            ParticipatingUser master = mock(ParticipatingUser.class);
            when(company.getId()).thenReturn(1);
            when(master.getId()).thenReturn(10);
            when(master.getEmail()).thenReturn("master@example.com");
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            when(participatingUserRepository.findByCompany_IdAndRole(1, RoleConstants.MASTER))
                .thenReturn(Optional.of(master));
            when(actionLinkService.issue(any(), any(), eq(10), eq("operator")))
                .thenReturn(new SecurityActionLinkService.IssuedActionLink(
                    java.util.UUID.randomUUID(), "token", java.time.LocalDateTime.now().plusHours(1), "url"));

            BaseResponse response = securityAccountService.operatorRequestMasterPasswordReset("COMP-001", "operator");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_PASSWORD_RESET_LINK_QUEUED);
            verify(actionLinkService).issue(any(), any(), eq(10), eq("operator"));
            verify(companyRepository).findByCompanyId("COMP-001");
            verify(participatingUserRepository).findByCompany_IdAndRole(1, RoleConstants.MASTER);
        }

        private void givenPasswordResetEnabled() {
            when(properties.getFeatures()).thenReturn(features);
            when(features.isPasswordReset()).thenReturn(true);
        }
    }
}