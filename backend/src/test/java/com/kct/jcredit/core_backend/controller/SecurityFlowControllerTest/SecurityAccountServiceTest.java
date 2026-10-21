package com.kct.jcredit.core_backend.controller.SecurityFlowControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import com.kct.jcredit.core_backend.config.SecurityFlowProperties;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.ForgotPasswordRequest;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.enums.OtpPurpose;
import com.kct.jcredit.core_backend.enums.SecurityActionPurpose;
import com.kct.jcredit.core_backend.enums.SecurityActionSubjectType;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.service.AuthVersionService;
import com.kct.jcredit.core_backend.service.CompanyLifecycleService;
import com.kct.jcredit.core_backend.service.NotificationMessages;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.OtpChallengeService;
import com.kct.jcredit.core_backend.service.SecurityAccountService;
import com.kct.jcredit.core_backend.service.SecurityActionLinkService;
import com.kct.jcredit.core_backend.service.SecurityEmailDeliveryService;
import com.kct.jcredit.core_backend.service.SecurityRateLimitService;
import com.kct.jcredit.core_backend.service.UsernameAvailabilityService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SecurityAccountService business logic, validation, and state-transition outline.
 *
 * Use Mockito for repository/service collaborators and verify side effects
 * (save/update calls, notification dispatch, exception propagation).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SecurityAccountServiceTest {

    private static final UUID CHALLENGE_ID = UUID.fromString("8a5b5572-d593-4358-8c98-4a90c35df012");
    private static final String SESSION_TOKEN = "session-token";
    private static final String PASSWORD = "NewPass123";

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
    @Spy private NotificationMessages messages = new NotificationMessages();
    @Mock private AuthVersionService authVersionService;
    @Mock private JwtUtil jwtUtil;
    @Mock private SecurityRateLimitService rateLimitService;
    @Mock private SecurityEmailDeliveryService securityEmailDeliveryService;
    @InjectMocks private SecurityAccountService service;

    @BeforeEach
    void setUp() {
        when(properties.getFeatures()).thenReturn(features);
        when(properties.getOtpTtl()).thenReturn(Duration.ofMinutes(5));
    }

    private SecurityActionLinkService.ActionSessionData sessionData(
            SecurityActionPurpose purpose, SecurityActionSubjectType subjectType, int subjectId) {
        return new SecurityActionLinkService.ActionSessionData(UUID.randomUUID(), purpose, subjectType, subjectId);
    }

    private OtpChallengeService.OtpData otpData(OtpPurpose purpose, String reference) {
        return new OtpChallengeService.OtpData(purpose, reference, "person@example.com", "hash", 0, 1, 0L, true);
    }

    private ParticipatingUser participatingUser(int id, String role) {
        ParticipatingUser user = ParticipatingUser.builder().username("person").firstName("Taro").firstNameKana("タロウ")
                .lastName("Yamada").lastNameKana("ヤマダ").email("person@example.com").phoneNumber("09012345678")
                .password("encoded-old").role(role).userPosition("staff").loginAttempts(4)
                .emailOtpLoginEnabled(false).authVersion(0).build();
        user.setId(id);
        user.setIsActive(true);
        return user;
    }

    private OperatingUser operatingUser(int id) {
        OperatingUser user = OperatingUser.builder().username("operator").firstName("Taro")
                .firstNameKana("タロウ").lastName("Yamada").lastNameKana("ヤマダ")
                .email("person@example.com").phoneNumber("09012345678").password("encoded-old")
                .role("operator").loginAttempts(4).emailOtpLoginEnabled(true).authVersion(0)
                .company(OperatorCompany.builder().companyId("OP-1").build()).build();
        user.setId(id);
        user.setIsActive(true);
        user.getCompany().setIsActive(true);
        return user;
    }

    private OtpChallengeService.IssuedOtp issuedOtp() {
        return new OtpChallengeService.IssuedOtp(CHALLENGE_ID, "123456", 300L, "p***@example.com",
                "person@example.com");
    }

    private ForgotPasswordRequest forgotPasswordRequest(String email) {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(email);
        return request;
    }

    private CustomException exceptionFrom(Runnable action) {
        try {
            action.run();
        } catch (CustomException exception) {
            return exception;
        }
        throw new AssertionError("Expected CustomException");
    }

    private Object invokePrivate(String name, Class<?>[] types, Object... args) {
        try {
            Method method = SecurityAccountService.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method.invoke(service, args);
        } catch (java.lang.reflect.InvocationTargetException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(exception.getCause());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Nested
    @DisplayName("POST /auth/action-links/exchange のテスト")
    class ExchangeActionLinkTest {

        @Test
        @DisplayName("UT-SAS-001: 有効なtokenでセッションに交換できる")
        void exchangeActionLink_validToken_returnsSession() {
            when(actionLinkService.exchange("raw-token")).thenReturn(new SecurityActionLinkService.ActionSession(
                    SESSION_TOKEN, SecurityActionPurpose.PASSWORD_RESET, 900L));
            var response = service.exchangeActionLink("raw-token");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_ACTION_LINK_OPENED);
            assertThat(response.getActionSessionToken()).isEqualTo(SESSION_TOKEN);
            assertThat(response.getPurpose()).isEqualTo(SecurityActionPurpose.PASSWORD_RESET);
            assertThat(response.getExpiresInSeconds()).isEqualTo(900L);
            verify(actionLinkService).exchange("raw-token");
            // Input: rawToken（actionLinkService.exchangeが正常ActionSessionを返すようモック）
            // Procedure: exchangeActionLinkを実行
            // Output: ActionLinkExchangeResponseが返却される
            // Verify: statusCode=OK、message=MSG_ACTION_LINK_OPENED、actionSessionToken/purpose/expiresInSecondsがsessionの値と一致していること
        }

        @Test
        @DisplayName("UT-SAS-002: actionLinkService.exchangeが例外を送出した場合そのまま伝播する")
        void exchangeActionLink_serviceThrows_propagatesException() {
            CustomException failure = new CustomException(ResponseConstants.INVALID_OR_EXPIRED_TOKEN,
                    ResponseConstants.MSG_INVALID_OR_EXPIRED_TOKEN);
            when(actionLinkService.exchange("bad-token")).thenThrow(failure);
            assertThatThrownBy(() -> service.exchangeActionLink("bad-token")).isSameAs(failure);
            verify(actionLinkService).exchange("bad-token");
            // Input: actionLinkService.exchangeがCustomException(INVALID_OR_EXPIRED_TOKEN)を送出するようモック
            // Procedure: exchangeActionLinkを実行
            // Output: CustomExceptionがそのまま伝播する
            // Verify: 例外のstatusCode/messageが変換されずそのまま伝播していること
        }
    }

    @Nested
    @DisplayName("POST /auth/master-account/setup のテスト")
    class SetupMasterAccountTest {

        @Test
        @DisplayName("UT-SAS-003: 設定待ち企業にマスターアカウントが作成され企業が有効化される")
        void setupMasterAccount_pendingCompany_createsMasterAndActivates() {
            when(features.isMasterSetupLink()).thenReturn(true);
            when(actionLinkService.requireSession(SESSION_TOKEN, SecurityActionPurpose.MASTER_ACCOUNT_SETUP))
                    .thenReturn(sessionData(SecurityActionPurpose.MASTER_ACCOUNT_SETUP,
                            SecurityActionSubjectType.PARTICIPATING_USER, 71));
            ParticipatingCompany company = ParticipatingCompany.builder()
                    .companyStatus(CompanyStatus.APPROVED_PENDING_SETUP).repFirstName("Taro").repFirstNameKana("タロウ")
                    .repLastName("Yamada").repLastNameKana("ヤマダ").repPhoneNumber("09012345678")
                    .repPosition("CEO").email("rep@example.com").build();
            company.setId(71);
            when(companyRepository.findByIdForUpdate(71)).thenReturn(Optional.of(company));
            when(participatingUserRepository.findByCompany_IdAndRole(71, "master")).thenReturn(Optional.empty());
            when(usernameAvailabilityService.lockAndRequireAvailable("yamada")).thenReturn("yamada");
            when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-new");
            BaseResponse response = service.setupMasterAccount(SESSION_TOKEN, "yamada", PASSWORD);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_MASTER_ACCOUNT_SETUP_COMPLETED);
            ArgumentCaptor<ParticipatingUser> captor = ArgumentCaptor.forClass(ParticipatingUser.class);
            verify(participatingUserRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo("master");
            assertThat(captor.getValue().getAuthVersion()).isZero();
            assertThat(captor.getValue().getEmailOtpLoginEnabled()).isTrue();
            assertThat(captor.getValue().getSite()).isNull();
            verify(companyLifecycleService).activateApprovedCompany(company);
            verify(actionLinkService).complete(SESSION_TOKEN, SecurityActionPurpose.MASTER_ACCOUNT_SETUP);
            // Input: sessionToken、username="yamada"、password="Abcd1234"（company.companyStatus=APPROVED_PENDING_SETUP、master未存在、usernameAvailable）
            // Procedure: setupMasterAccountを実行
            // Output: BaseResponse成功（MSG_MASTER_ACCOUNT_SETUP_COMPLETED）が返却される
            // Verify: participatingUserRepository.saveにrole=master、authVersion=0、emailOtpLoginEnabled=true、site=nullで保存されること／companyLifecycleService.activateApprovedCompanyが呼ばれること／actionLinkService.completeがMASTER_ACCOUNT_SETUPで呼ばれること
        }

        @Test
        @DisplayName("UT-SAS-004: 機能フラグがOFFの場合NOT_FOUND例外となる")
        void setupMasterAccount_featureDisabled_throwsNotFound() {
            when(features.isMasterSetupLink()).thenReturn(false);
            assertThat(exceptionFrom(() -> service.setupMasterAccount(SESSION_TOKEN, "yamada", PASSWORD)).getErrorCode())
                    .isEqualTo(ResponseConstants.NOT_FOUND);
            verifyNoInteractions(actionLinkService, companyRepository);
            // Input: properties.features.masterSetupLink=false
            // Procedure: setupMasterAccountを実行
            // Output: CustomException(NOT_FOUND)がスローされる
            // Verify: actionLinkService.requireSessionおよびcompanyRepositoryが一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-005: 企業が存在しない場合INVALID_OR_EXPIRED_TOKENとなる")
        void setupMasterAccount_companyNotFound_throwsInvalidToken() {
            when(features.isMasterSetupLink()).thenReturn(true);
            when(actionLinkService.requireSession(SESSION_TOKEN, SecurityActionPurpose.MASTER_ACCOUNT_SETUP))
                    .thenReturn(sessionData(SecurityActionPurpose.MASTER_ACCOUNT_SETUP,
                            SecurityActionSubjectType.PARTICIPATING_USER, 71));
            when(companyRepository.findByIdForUpdate(71)).thenReturn(Optional.empty());
            assertThat(exceptionFrom(() -> service.setupMasterAccount(SESSION_TOKEN, "yamada", PASSWORD)).getErrorCode())
                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verifyNoInteractions(usernameAvailabilityService);
            verify(participatingUserRepository, never()).save(any());
            // Input: session.subjectIdに対応する企業がcompanyRepositoryに存在しない
            // Procedure: setupMasterAccountを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: usernameAvailabilityServiceおよびparticipatingUserRepository.saveが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-006: 企業ステータスがAPPROVED_PENDING_SETUPでない場合INVALID_OR_EXPIRED_TOKENとなる")
        void setupMasterAccount_wrongCompanyStatus_throwsInvalidToken() {
            when(features.isMasterSetupLink()).thenReturn(true);
            when(actionLinkService.requireSession(SESSION_TOKEN, SecurityActionPurpose.MASTER_ACCOUNT_SETUP))
                    .thenReturn(sessionData(SecurityActionPurpose.MASTER_ACCOUNT_SETUP,
                            SecurityActionSubjectType.PARTICIPATING_USER, 71));
            ParticipatingCompany company = new ParticipatingCompany();
            company.setId(71);
            company.setCompanyStatus(CompanyStatus.ACTIVE);
            when(companyRepository.findByIdForUpdate(71)).thenReturn(Optional.of(company));
            assertThat(exceptionFrom(() -> service.setupMasterAccount(SESSION_TOKEN, "yamada", PASSWORD)).getErrorCode())
                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verifyNoInteractions(usernameAvailabilityService);
            // Input: company.companyStatus=ACTIVE
            // Procedure: setupMasterAccountを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: usernameAvailabilityServiceが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-007: 企業にマスターが既に存在する場合INVALID_OR_EXPIRED_TOKENとなる")
        void setupMasterAccount_masterAlreadyExists_throwsInvalidToken() {
            when(features.isMasterSetupLink()).thenReturn(true);
            when(actionLinkService.requireSession(SESSION_TOKEN, SecurityActionPurpose.MASTER_ACCOUNT_SETUP))
                    .thenReturn(sessionData(SecurityActionPurpose.MASTER_ACCOUNT_SETUP,
                            SecurityActionSubjectType.PARTICIPATING_USER, 71));
            ParticipatingCompany company = new ParticipatingCompany();
            company.setId(71);
            company.setCompanyStatus(CompanyStatus.APPROVED_PENDING_SETUP);
            when(companyRepository.findByIdForUpdate(71)).thenReturn(Optional.of(company));
            when(participatingUserRepository.findByCompany_IdAndRole(71, "master"))
                    .thenReturn(Optional.of(participatingUser(72, "master")));
            assertThat(exceptionFrom(() -> service.setupMasterAccount(SESSION_TOKEN, "yamada", PASSWORD)).getErrorCode())
                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verifyNoInteractions(usernameAvailabilityService);
            // Input: company.companyStatus=APPROVED_PENDING_SETUP、participatingUserRepository.findByCompany_IdAndRoleがmasterユーザーを返す
            // Procedure: setupMasterAccountを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: usernameAvailabilityServiceが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-008: ユーザー名が使用済みの場合USED_USERNAME例外が伝播する")
        void setupMasterAccount_usernameUsed_propagatesException() {
            when(features.isMasterSetupLink()).thenReturn(true);
            when(actionLinkService.requireSession(SESSION_TOKEN, SecurityActionPurpose.MASTER_ACCOUNT_SETUP))
                    .thenReturn(sessionData(SecurityActionPurpose.MASTER_ACCOUNT_SETUP,
                            SecurityActionSubjectType.PARTICIPATING_USER, 71));
            ParticipatingCompany company = new ParticipatingCompany();
            company.setId(71);
            company.setCompanyStatus(CompanyStatus.APPROVED_PENDING_SETUP);
            when(companyRepository.findByIdForUpdate(71)).thenReturn(Optional.of(company));
            when(participatingUserRepository.findByCompany_IdAndRole(71, "master")).thenReturn(Optional.empty());
            CustomException failure = new CustomException(ResponseConstants.USED_USERNAME, "used");
            when(usernameAvailabilityService.lockAndRequireAvailable("yamada")).thenThrow(failure);
            assertThatThrownBy(() -> service.setupMasterAccount(SESSION_TOKEN, "yamada", PASSWORD)).isSameAs(failure);
            verify(participatingUserRepository, never()).save(any());
            verify(companyLifecycleService, never()).activateApprovedCompany(any());
            // Input: usernameAvailabilityService.lockAndRequireAvailableがCustomException(USED_USERNAME)を送出
            // Procedure: setupMasterAccountを実行
            // Output: CustomException(USED_USERNAME)がスローされる
            // Verify: participatingUserRepository.saveおよびcompanyLifecycleService.activateApprovedCompanyが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-009: 作成されるマスターの属性が企業の代表者情報と一致する")
        void setupMasterAccount_createdMaster_matchesCompanyRepInfo() {
            when(features.isMasterSetupLink()).thenReturn(true);
            when(actionLinkService.requireSession(SESSION_TOKEN, SecurityActionPurpose.MASTER_ACCOUNT_SETUP))
                    .thenReturn(sessionData(SecurityActionPurpose.MASTER_ACCOUNT_SETUP,
                            SecurityActionSubjectType.PARTICIPATING_USER, 71));
            ParticipatingCompany company = ParticipatingCompany.builder()
                    .companyStatus(CompanyStatus.APPROVED_PENDING_SETUP).repFirstName("Taro").repFirstNameKana("タロウ")
                    .repLastName("Yamada").repLastNameKana("ヤマダ").repPhoneNumber("09012345678")
                    .repPosition("CEO").email("rep@example.com").build();
            company.setId(71);
            when(companyRepository.findByIdForUpdate(71)).thenReturn(Optional.of(company));
            when(participatingUserRepository.findByCompany_IdAndRole(71, "master")).thenReturn(Optional.empty());
            when(usernameAvailabilityService.lockAndRequireAvailable("yamada")).thenReturn("yamada");
            when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded");
            service.setupMasterAccount(SESSION_TOKEN, "yamada", PASSWORD);
            ArgumentCaptor<ParticipatingUser> captor = ArgumentCaptor.forClass(ParticipatingUser.class);
            verify(participatingUserRepository).save(captor.capture());
            ParticipatingUser master = captor.getValue();
            assertThat(master.getFirstName()).isEqualTo("Taro");
            assertThat(master.getFirstNameKana()).isEqualTo("タロウ");
            assertThat(master.getLastName()).isEqualTo("Yamada");
            assertThat(master.getLastNameKana()).isEqualTo("ヤマダ");
            assertThat(master.getEmail()).isEqualTo("rep@example.com");
            assertThat(master.getPhoneNumber()).isEqualTo("09012345678");
            assertThat(master.getUserPosition()).isEqualTo("CEO");
            // Input: company.repFirstName/repLastName/repPhoneNumber/repPosition/emailに値を設定
            // Procedure: setupMasterAccountを実行
            // Output: 保存されるParticipatingUserの各属性が企業の代表者情報と一致する
            // Verify: firstName/firstNameKana/lastName/lastNameKana/email/phoneNumber/userPositionがcompanyの値と一致していること
        }
    }

    @Nested
    @DisplayName("POST /auth/password-reset/request のテスト")
    class RequestPasswordResetTest {

        @Test
        @DisplayName("UT-SAS-010: 参加企業ユーザーのメールにのみ一致した場合リセットリンクが発行される")
        void requestPasswordReset_participatingEmailMatch_issuesResetLink() {
            when(features.isPasswordReset()).thenReturn(true);
            ParticipatingUser user = participatingUser(10, "user");
            when(participatingUserRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
            when(operatingUserRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.empty());
            when(actionLinkService.issue(SecurityActionPurpose.PASSWORD_RESET,
                    SecurityActionSubjectType.PARTICIPATING_USER, 10, "self-service"))
                    .thenReturn(new SecurityActionLinkService.IssuedActionLink(UUID.randomUUID(), "raw",
                            LocalDateTime.now().plusMinutes(5), "https://example/reset"));
            BaseResponse response = service.requestPasswordReset(forgotPasswordRequest("person@example.com"));
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_PASSWORD_RESET_EMAIL_SENT);
            verify(actionLinkService).issue(SecurityActionPurpose.PASSWORD_RESET,
                    SecurityActionSubjectType.PARTICIPATING_USER, 10, "self-service");
            verify(operatingUserRepository).findByEmailIgnoreCase("person@example.com");
            // Input: email一致するParticipatingUserのみ存在（OperatingUserは存在しない）
            // Procedure: requestPasswordResetを実行
            // Output: BaseResponse成功（MSG_PASSWORD_RESET_EMAIL_SENT）が返却される
            // Verify: actionLinkService.issueがPARTICIPATING_USER種別で1回呼ばれること／operatingUser側は呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-011: 運営ユーザーのメールにのみ一致した場合リセットリンクが発行される")
        void requestPasswordReset_operatingEmailMatch_issuesResetLink() {
            when(features.isPasswordReset()).thenReturn(true);
            when(participatingUserRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.empty());
            OperatingUser user = operatingUser(11);
            when(operatingUserRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
            when(actionLinkService.issue(SecurityActionPurpose.PASSWORD_RESET,
                    SecurityActionSubjectType.OPERATING_USER, 11, "self-service"))
                    .thenReturn(new SecurityActionLinkService.IssuedActionLink(UUID.randomUUID(), "raw",
                            LocalDateTime.now().plusMinutes(5), "https://example/reset"));
            assertThat(service.requestPasswordReset(forgotPasswordRequest("person@example.com")).getStatusCode())
                    .isEqualTo(ResponseConstants.OK);
            verify(actionLinkService).issue(SecurityActionPurpose.PASSWORD_RESET,
                    SecurityActionSubjectType.OPERATING_USER, 11, "self-service");
            // Input: email一致するOperatingUserのみ存在（ParticipatingUserは存在しない）
            // Procedure: requestPasswordResetを実行
            // Output: BaseResponse成功が返却される
            // Verify: actionLinkService.issueがOPERATING_USER種別で1回呼ばれること
        }

        @Test
        @DisplayName("UT-SAS-012: どちらのテーブルにもメールが存在しない場合でも同一の成功応答を返す（列挙対策）")
        void requestPasswordReset_noMatch_returnsGenericSuccess() {
            when(features.isPasswordReset()).thenReturn(true);
            when(participatingUserRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.empty());
            when(operatingUserRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.empty());
            BaseResponse response = service.requestPasswordReset(forgotPasswordRequest("person@example.com"));
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_PASSWORD_RESET_EMAIL_SENT);
            verify(actionLinkService, never()).issue(any(), any(), any(), any());
            // Input: emailがParticipatingUser/OperatingUserどちらにも存在しない
            // Procedure: requestPasswordResetを実行
            // Output: BaseResponse成功（MSG_PASSWORD_RESET_EMAIL_SENT）が返却される
            // Verify: actionLinkService.issueが一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-013: 機能フラグOFFの場合NOT_FOUND例外となる")
        void requestPasswordReset_featureDisabled_throwsNotFound() {
            when(features.isPasswordReset()).thenReturn(false);
            assertThat(exceptionFrom(() -> service.requestPasswordReset(forgotPasswordRequest("person@example.com"))
                    ).getErrorCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            verifyNoInteractions(rateLimitService, participatingUserRepository, operatingUserRepository);
            // Input: properties.features.passwordReset=false
            // Procedure: requestPasswordResetを実行
            // Output: CustomException(NOT_FOUND)がスローされる
            // Verify: rateLimitServiceおよびリポジトリ検索が一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-014: 同一メールへの時間あたり上限を超えた場合TOO_MANY_REQUESTSが伝播する")
        void requestPasswordReset_rateLimitExceeded_propagatesException() {
            when(features.isPasswordReset()).thenReturn(true);
            CustomException failure = new CustomException(ResponseConstants.TOO_MANY_REQUESTS, "too many");
            org.mockito.Mockito.doThrow(failure).when(rateLimitService).require(
                    "password-reset-account", "person@example.com", 3, Duration.ofHours(1));
            assertThatThrownBy(() -> service.requestPasswordReset(forgotPasswordRequest("person@example.com")))
                    .isSameAs(failure);
            verify(participatingUserRepository, never()).findByEmailIgnoreCase(anyString());
            verify(operatingUserRepository, never()).findByEmailIgnoreCase(anyString());
            // Input: rateLimitService.requireがCustomException(TOO_MANY_REQUESTS)を送出
            // Procedure: requestPasswordResetを実行
            // Output: CustomException(TOO_MANY_REQUESTS)がスローされる
            // Verify: リポジトリのfindByEmailIgnoreCaseが一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-015: メールアドレスが大文字小文字混在でも正規化されて検索される")
        void requestPasswordReset_mixedCaseEmail_normalizesBeforeLookup() {
            when(features.isPasswordReset()).thenReturn(true);
            when(participatingUserRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.empty());
            when(operatingUserRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.empty());
            service.requestPasswordReset(forgotPasswordRequest(" User@Example.COM "));
            verify(rateLimitService).require("password-reset-account", "user@example.com", 3, Duration.ofHours(1));
            verify(participatingUserRepository).findByEmailIgnoreCase("user@example.com");
            verify(operatingUserRepository).findByEmailIgnoreCase("user@example.com");
            // Input: email="User@Example.COM"
            // Procedure: requestPasswordResetを実行
            // Output: 正規化（trim+lowercase）された値でfindByEmailIgnoreCaseが呼ばれる
            // Verify: rateLimitServiceおよびリポジトリ呼び出しに"user@example.com"が渡されていること
        }
    }

    @Nested
    @DisplayName("運営操作:マスターパスワードリセット代行申請 のテスト")
    class OperatorRequestMasterPasswordResetTest {

        @Test
        @DisplayName("UT-SAS-016: 運営ユーザーが企業IDを指定してマスターのパスワードリセットを代行申請できる")
        void operatorRequestMasterPasswordReset_validCompany_issuesResetLink() {
            when(features.isPasswordReset()).thenReturn(true);
            ParticipatingCompany company = new ParticipatingCompany();
            company.setId(71);
            ParticipatingUser master = participatingUser(72, "master");
            when(companyRepository.findByCompanyId("COMP-1")).thenReturn(Optional.of(company));
            when(participatingUserRepository.findByCompany_IdAndRole(71, "master")).thenReturn(Optional.of(master));
            when(actionLinkService.issue(SecurityActionPurpose.PASSWORD_RESET,
                    SecurityActionSubjectType.PARTICIPATING_USER, 72, "operator-1"))
                    .thenReturn(new SecurityActionLinkService.IssuedActionLink(UUID.randomUUID(), "raw",
                            LocalDateTime.now().plusMinutes(5), "https://example/reset"));
            BaseResponse response = service.operatorRequestMasterPasswordReset("COMP-1", "operator-1");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_PASSWORD_RESET_LINK_QUEUED);
            verify(actionLinkService).issue(SecurityActionPurpose.PASSWORD_RESET,
                    SecurityActionSubjectType.PARTICIPATING_USER, 72, "operator-1");
            // Input: companyId、actor="operator-1"（企業・マスター存在）
            // Procedure: operatorRequestMasterPasswordResetを実行
            // Output: BaseResponse成功（MSG_PASSWORD_RESET_LINK_QUEUED）が返却される
            // Verify: actionLinkService.issueがmaster.getId()、actor="operator-1"で呼ばれること
        }

        @Test
        @DisplayName("UT-SAS-017: 指定した企業IDが存在しない場合NOT_FOUND例外となる")
        void operatorRequestMasterPasswordReset_companyNotFound_throwsNotFound() {
            when(features.isPasswordReset()).thenReturn(true);
            when(companyRepository.findByCompanyId("missing")).thenReturn(Optional.empty());
            CustomException exception = exceptionFrom(() -> service.operatorRequestMasterPasswordReset("missing", "operator"));
            assertThat(exception.getErrorCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(exception.getMessage()).isEqualTo(ResponseConstants.MSG_COMPANY_NOT_FOUND);
            verifyNoInteractions(participatingUserRepository);
            // Input: companyRepository.findByCompanyIdが空を返す
            // Procedure: operatorRequestMasterPasswordResetを実行
            // Output: CustomException(NOT_FOUND, MSG_COMPANY_NOT_FOUND)がスローされる
            // Verify: participatingUserRepositoryが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-018: 企業にマスターユーザーが存在しない場合NOT_FOUND例外となる")
        void operatorRequestMasterPasswordReset_masterNotFound_throwsNotFound() {
            when(features.isPasswordReset()).thenReturn(true);
            ParticipatingCompany company = new ParticipatingCompany();
            company.setId(71);
            when(companyRepository.findByCompanyId("COMP-1")).thenReturn(Optional.of(company));
            when(participatingUserRepository.findByCompany_IdAndRole(71, "master")).thenReturn(Optional.empty());
            CustomException exception = exceptionFrom(() -> service.operatorRequestMasterPasswordReset("COMP-1", "operator"));
            assertThat(exception.getErrorCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(exception.getMessage()).isEqualTo(ResponseConstants.MSG_MASTER_ACCOUNT_NOT_FOUND);
            verify(actionLinkService, never()).issue(any(), any(), any(), any());
            // Input: company存在、participatingUserRepository.findByCompany_IdAndRoleが空を返す
            // Procedure: operatorRequestMasterPasswordResetを実行
            // Output: CustomException(NOT_FOUND, MSG_MASTER_ACCOUNT_NOT_FOUND)がスローされる
            // Verify: actionLinkService.issueが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-019: 機能フラグOFFの場合NOT_FOUND例外となる")
        void operatorRequestMasterPasswordReset_featureDisabled_throwsNotFound() {
            when(features.isPasswordReset()).thenReturn(false);
            assertThat(exceptionFrom(() -> service.operatorRequestMasterPasswordReset("COMP-1", "operator")
                    ).getErrorCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            verifyNoInteractions(companyRepository, participatingUserRepository);
            // Input: properties.features.passwordReset=false
            // Procedure: operatorRequestMasterPasswordResetを実行
            // Output: CustomException(NOT_FOUND)がスローされる
            // Verify: companyRepositoryが呼ばれないこと
        }
    }

    @Nested
    @DisplayName("POST /auth/password-reset/complete のテスト")
    class CompletePasswordResetTest {

        @Test
        @DisplayName("UT-SAS-020: 参加企業ユーザーが新しいパスワードでリセット完了できる")
        void completePasswordReset_participatingUser_completesReset() {
            when(features.isPasswordReset()).thenReturn(true);
            when(actionLinkService.requireSession(SESSION_TOKEN, SecurityActionPurpose.PASSWORD_RESET))
                    .thenReturn(sessionData(SecurityActionPurpose.PASSWORD_RESET,
                            SecurityActionSubjectType.PARTICIPATING_USER, 10));
            ParticipatingUser user = participatingUser(10, "user");
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(5));
            when(participatingUserRepository.findById(10)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, "encoded-old")).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-new");
            BaseResponse response = service.completePasswordReset(SESSION_TOKEN, PASSWORD);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_PASSWORD_RESET_COMPLETED);
            assertThat(user.getPassword()).isEqualTo("encoded-new");
            assertThat(user.getLoginAttempts()).isZero();
            assertThat(user.getAccountLockedUntil()).isNull();
            verify(authVersionService).increment(user);
            verify(actionLinkService).complete(SESSION_TOKEN, SecurityActionPurpose.PASSWORD_RESET);
            // Input: sessionToken（subjectType=PARTICIPATING_USER）、newPassword="NewPass123"（現パスワードと異なる）
            // Procedure: completePasswordResetを実行
            // Output: BaseResponse成功（MSG_PASSWORD_RESET_COMPLETED）が返却される
            // Verify: user.passwordが新パスワードのハッシュに更新、loginAttempts=0、accountLockedUntil=null、authVersionService.incrementが呼ばれ、actionLinkService.completeがPASSWORD_RESETで呼ばれること
        }

        @Test
        @DisplayName("UT-SAS-021: 運営ユーザーが新しいパスワードでリセット完了できる")
        void completePasswordReset_operatingUser_completesReset() {
            when(features.isPasswordReset()).thenReturn(true);
            when(actionLinkService.requireSession(SESSION_TOKEN, SecurityActionPurpose.PASSWORD_RESET))
                    .thenReturn(sessionData(SecurityActionPurpose.PASSWORD_RESET,
                            SecurityActionSubjectType.OPERATING_USER, 11));
            OperatingUser user = operatingUser(11);
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(5));
            when(operatingUserRepository.findById(11)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, "encoded-old")).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-new");
            BaseResponse response = service.completePasswordReset(SESSION_TOKEN, PASSWORD);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(user.getPassword()).isEqualTo("encoded-new");
            assertThat(user.getLoginAttempts()).isZero();
            assertThat(user.getAccountLockedUntil()).isNull();
            verify(authVersionService).increment(user);
            verify(actionLinkService).complete(SESSION_TOKEN, SecurityActionPurpose.PASSWORD_RESET);
            // Input: sessionToken（subjectType=OPERATING_USER）、newPassword="NewPass123"
            // Procedure: completePasswordResetを実行
            // Output: BaseResponse成功が返却される
            // Verify: OperatingUserのpassword/loginAttempts/accountLockedUntilが更新され、authVersionService.incrementが呼ばれること
        }

        @Test
        @DisplayName("UT-SAS-022: 新パスワードが現在のパスワードと同一の場合SAME_PASSWORD例外となる")
        void completePasswordReset_samePassword_throwsSamePassword() {
            when(features.isPasswordReset()).thenReturn(true);
            when(actionLinkService.requireSession(SESSION_TOKEN, SecurityActionPurpose.PASSWORD_RESET))
                    .thenReturn(sessionData(SecurityActionPurpose.PASSWORD_RESET,
                            SecurityActionSubjectType.PARTICIPATING_USER, 10));
            ParticipatingUser user = participatingUser(10, "user");
            when(participatingUserRepository.findById(10)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, "encoded-old")).thenReturn(true);
            assertThat(exceptionFrom(() -> service.completePasswordReset(SESSION_TOKEN, PASSWORD)).getErrorCode())
                    .isEqualTo(ResponseConstants.SAME_PASSWORD);
            verify(passwordEncoder, never()).encode(anyString());
            verify(authVersionService, never()).increment(any(ParticipatingUser.class));
            // Input: newPasswordがpasswordEncoder.matchesでtrueを返す（既存パスワードと同一）
            // Procedure: completePasswordResetを実行
            // Output: CustomException(SAME_PASSWORD)がスローされる
            // Verify: user.setPasswordおよびauthVersionService.incrementが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-023: subjectTypeがPARTICIPATING_USER/OPERATING_USER以外の場合INVALID_OR_EXPIRED_TOKENとなる")
        void completePasswordReset_unknownSubjectType_throwsInvalidToken() {
            when(features.isPasswordReset()).thenReturn(true);
            when(actionLinkService.requireSession(SESSION_TOKEN, SecurityActionPurpose.PASSWORD_RESET))
                    .thenReturn(sessionData(SecurityActionPurpose.PASSWORD_RESET, SecurityActionSubjectType.COMPANY, 10));
            assertThat(exceptionFrom(() -> service.completePasswordReset(SESSION_TOKEN, PASSWORD)).getErrorCode())
                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verify(actionLinkService, never()).complete(anyString(), any());
            // Input: session.subjectTypeが未定義の値
            // Procedure: completePasswordResetを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: actionLinkService.completeが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-024: subjectIdに対応する参加企業ユーザーが存在しない場合INVALID_OR_EXPIRED_TOKENとなる")
        void completePasswordReset_participatingUserNotFound_throwsInvalidToken() {
            when(features.isPasswordReset()).thenReturn(true);
            when(actionLinkService.requireSession(SESSION_TOKEN, SecurityActionPurpose.PASSWORD_RESET))
                    .thenReturn(sessionData(SecurityActionPurpose.PASSWORD_RESET,
                            SecurityActionSubjectType.PARTICIPATING_USER, 10));
            when(participatingUserRepository.findById(10)).thenReturn(Optional.empty());
            assertThat(exceptionFrom(() -> service.completePasswordReset(SESSION_TOKEN, PASSWORD)).getErrorCode())
                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verify(actionLinkService, never()).complete(anyString(), any());
            // Input: subjectType=PARTICIPATING_USER、participatingUserRepository.findByIdが空を返す
            // Procedure: completePasswordResetを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: actionLinkService.completeが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-025: 機能フラグOFFの場合NOT_FOUND例外となる")
        void completePasswordReset_featureDisabled_throwsNotFound() {
            when(features.isPasswordReset()).thenReturn(false);
            assertThat(exceptionFrom(() -> service.completePasswordReset(SESSION_TOKEN, PASSWORD)).getErrorCode())
                    .isEqualTo(ResponseConstants.NOT_FOUND);
            verifyNoInteractions(actionLinkService);
            // Input: properties.features.passwordReset=false
            // Procedure: completePasswordResetを実行
            // Output: CustomException(NOT_FOUND)がスローされる
            // Verify: actionLinkService.requireSessionが呼ばれないこと
        }
    }

    @Nested
    @DisplayName("メールOTP要否判定(参加企業) のテスト")
    class RequiresParticipatingOtpTest {

        @Test
        @DisplayName("UT-SAS-026: roleがmasterの場合は常にOTPが必須と判定される")
        void requiresParticipatingOtp_masterRole_alwaysTrue() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            ParticipatingUser user = participatingUser(1, "master");
            user.setEmailOtpLoginEnabled(false);
            assertThat(service.requiresParticipatingOtp(user)).isTrue();
            // Input: properties.features.loginEmailOtp=true、user.role="master"、emailOtpLoginEnabled=false
            // Procedure: requiresParticipatingOtpを実行
            // Output: trueが返却される
            // Verify: role=masterであればemailOtpLoginEnabledの値に関わらずtrueになること
        }

        @Test
        @DisplayName("UT-SAS-027: 一般ユーザーでemailOtpLoginEnabled=trueの場合OTPが必須と判定される")
        void requiresParticipatingOtp_userEnabled_returnsTrue() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            ParticipatingUser user = participatingUser(1, "user");
            user.setEmailOtpLoginEnabled(true);
            assertThat(service.requiresParticipatingOtp(user)).isTrue();
            // Input: user.role="user"、emailOtpLoginEnabled=true、機能フラグON
            // Procedure: requiresParticipatingOtpを実行
            // Output: trueが返却される
            // Verify: 判定結果がtrueであること
        }

        @Test
        @DisplayName("UT-SAS-028: 一般ユーザーでemailOtpLoginEnabled=falseの場合OTPが不要と判定される")
        void requiresParticipatingOtp_userDisabled_returnsFalse() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            assertThat(service.requiresParticipatingOtp(participatingUser(1, "user"))).isFalse();
            // Input: user.role="user"、emailOtpLoginEnabled=false、機能フラグON
            // Procedure: requiresParticipatingOtpを実行
            // Output: falseが返却される
            // Verify: 判定結果がfalseであること
        }

        @Test
        @DisplayName("UT-SAS-029: 機能フラグがOFFの場合roleに関わらずOTPが不要と判定される")
        void requiresParticipatingOtp_featureDisabled_returnsFalse() {
            when(features.isLoginEmailOtp()).thenReturn(false);
            assertThat(service.requiresParticipatingOtp(participatingUser(1, "master"))).isFalse();
            // Input: properties.features.loginEmailOtp=false、user.role="master"
            // Procedure: requiresParticipatingOtpを実行
            // Output: falseが返却される
            // Verify: 機能フラグがroleより優先して判定されること
        }
    }

    @Nested
    @DisplayName("メールOTP要否判定(運営) のテスト")
    class RequiresOperatingOtpTest {

        @Test
        @DisplayName("UT-SAS-030: 機能フラグがONの場合OTPが必須と判定される")
        void requiresOperatingOtp_featureEnabled_returnsTrue() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            assertThat(service.requiresOperatingOtp()).isTrue();
            // Input: properties.features.loginEmailOtp=true
            // Procedure: requiresOperatingOtpを実行
            // Output: trueが返却される
            // Verify: 判定結果がtrueであること
        }

        @Test
        @DisplayName("UT-SAS-031: 機能フラグがOFFの場合OTPが不要と判定される")
        void requiresOperatingOtp_featureDisabled_returnsFalse() {
            when(features.isLoginEmailOtp()).thenReturn(false);
            assertThat(service.requiresOperatingOtp()).isFalse();
            // Input: properties.features.loginEmailOtp=false
            // Procedure: requiresOperatingOtpを実行
            // Output: falseが返却される
            // Verify: 判定結果がfalseであること
        }
    }

    @Nested
    @DisplayName("参加企業ユーザーログイン(1段階目) のテスト")
    class BeginParticipatingLoginTest {

        @Test
        @DisplayName("UT-SAS-032: OTPチャレンジが発行されLoginResponseが返却される")
        void beginParticipatingLogin_validUser_issuesOtpChallenge() {
            ParticipatingUser user = participatingUser(1, "user");
            user.setAuthVersion(4);
            when(otpChallengeService.create(OtpPurpose.PARTICIPATING_LOGIN, "P:1:4", "person@example.com"))
                    .thenReturn(issuedOtp());
            var response = service.beginParticipatingLogin(user,
                    ParticipatingCompany.builder().nameJa("Example").build());
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OTP_REQUIRED);
            assertThat(response.getRole()).isEqualTo("user");
            assertThat(response.getCompanyName()).isEqualTo("Example");
            assertThat(response.getChallengeId()).isEqualTo(CHALLENGE_ID);
            assertThat(response.getMaskedEmail()).isEqualTo("p***@example.com");
            assertThat(response.getExpiresInSeconds()).isEqualTo(300L);
            verify(otpChallengeService).create(OtpPurpose.PARTICIPATING_LOGIN, "P:1:4", "person@example.com");
            verify(notificationService).sendOtpEmail("person@example.com", "123456", 5L);
            // Input: user（id, authVersion, email, role）、company（nameJa）
            // Procedure: beginParticipatingLoginを実行
            // Output: LoginResponseが返却される
            // Verify: statusCode=OTP_REQUIRED、subjectReferenceが"P:{userId}:{authVersion}"形式で生成されること、role/companyName/challengeId/maskedEmail/expiresInSecondsが設定され、notificationService.sendOtpEmailが呼ばれること
        }

        @Test
        @DisplayName("UT-SAS-033: authVersionがnullの場合0として扱われる")
        void beginParticipatingLogin_nullAuthVersion_defaultsToZero() {
            ParticipatingUser user = participatingUser(1, "user");
            user.setAuthVersion(null);
            when(otpChallengeService.create(OtpPurpose.PARTICIPATING_LOGIN, "P:1:0", "person@example.com"))
                    .thenReturn(issuedOtp());
            service.beginParticipatingLogin(user, ParticipatingCompany.builder().nameJa("Example").build());
            verify(otpChallengeService).create(OtpPurpose.PARTICIPATING_LOGIN, "P:1:0", "person@example.com");
            // Input: user.authVersion=null
            // Procedure: beginParticipatingLoginを実行
            // Output: subjectReferenceの末尾が"0"になる
            // Verify: otpChallengeService.createに渡るsubjectReferenceにauthVersion=0相当の文字列が含まれること
        }
    }

    @Nested
    @DisplayName("運営ユーザーログイン(1段階目) のテスト")
    class BeginOperatingLoginTest {

        @Test
        @DisplayName("UT-SAS-034: OTPチャレンジが発行されAdminLoginResponseが返却される")
        void beginOperatingLogin_validUser_issuesOtpChallenge() {
            OperatingUser user = operatingUser(1);
            user.setAuthVersion(3);
            when(otpChallengeService.create(OtpPurpose.OPERATING_LOGIN, "O:1:3", "person@example.com"))
                    .thenReturn(issuedOtp());
            var response = service.beginOperatingLogin(user);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OTP_REQUIRED);
            assertThat(response.getChallengeId()).isEqualTo(CHALLENGE_ID);
            assertThat(response.getMaskedEmail()).isEqualTo("p***@example.com");
            assertThat(response.getExpiresInSeconds()).isEqualTo(300L);
            verify(otpChallengeService).create(OtpPurpose.OPERATING_LOGIN, "O:1:3", "person@example.com");
            verify(notificationService).sendOtpEmail("person@example.com", "123456", 5L);
            // Input: user（id, authVersion, email）
            // Procedure: beginOperatingLoginを実行
            // Output: AdminLoginResponseが返却される
            // Verify: statusCode=OTP_REQUIRED、subjectReferenceが"O:{userId}:{authVersion}"形式で生成され、challengeId/maskedEmail/expiresInSecondsが設定されること
        }
    }

    @Nested
    @DisplayName("参加企業ユーザーログインOTP検証 のテスト")
    class VerifyParticipatingLoginTest {

        @Test
        @DisplayName("UT-SAS-035: コード検証成功でJWTを含むLoginResponseが返却される")
        void verifyParticipatingLogin_validOtp_returnsLoginResponse() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.PARTICIPATING_LOGIN, "P:1:0"));
            ParticipatingCompany company = ParticipatingCompany.builder().companyStatus(CompanyStatus.ACTIVE)
                    .nameJa("Example").companyId("COMP-1").build();
            ParticipatingUser user = participatingUser(1, "user");
            user.setCompany(company);
            when(participatingUserRepository.findById(1)).thenReturn(Optional.of(user));
            when(jwtUtil.generateToken(1, "person", "COMP-1", "user", 0, java.util.List.of())).thenReturn("jwt");
            var response = service.verifyParticipatingLogin(CHALLENGE_ID, "123456");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getToken()).isEqualTo("jwt");
            assertThat(response.getRole()).isEqualTo("user");
            assertThat(response.getCompanyName()).isEqualTo("Example");
            verify(otpChallengeService).consumeVerified(CHALLENGE_ID, OtpPurpose.PARTICIPATING_LOGIN);
            // Input: challengeId、code（subjectReference="P:1:0"、user.authVersion=0、isActive=true、company.status=ACTIVE）
            // Procedure: verifyParticipatingLoginを実行
            // Output: statusCode=OKのLoginResponseが返却される
            // Verify: token/role/companyName/siteIdが設定され、otpChallengeService.consumeVerifiedが呼ばれること
        }

        @Test
        @DisplayName("UT-SAS-036: 拠点に紐づかないユーザーの場合siteIdがnullになる")
        void verifyParticipatingLogin_noSite_siteIdIsNull() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.PARTICIPATING_LOGIN, "P:1:0"));
            ParticipatingCompany company = ParticipatingCompany.builder().companyStatus(CompanyStatus.ACTIVE)
                    .nameJa("Example").companyId("COMP-1").build();
            ParticipatingUser user = participatingUser(1, "user");
            user.setCompany(company);
            when(participatingUserRepository.findById(1)).thenReturn(Optional.of(user));
            when(jwtUtil.generateToken(any(), anyString(), anyString(), anyString(), any(), any())).thenReturn("jwt");
            assertThat(service.verifyParticipatingLogin(CHALLENGE_ID, "123456").getSiteId()).isNull();
            // Input: user.site=null
            // Procedure: verifyParticipatingLoginを実行
            // Output: LoginResponse.siteId=nullが返却される
            // Verify: siteIdがnullとして設定されていること
        }

        @Test
        @DisplayName("UT-SAS-037: subjectReferenceのprefixがPでない場合INVALID_OR_EXPIRED_TOKENとなる")
        void verifyParticipatingLogin_wrongPrefix_throwsInvalidToken() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.PARTICIPATING_LOGIN, "O:1:0"));
            assertThat(exceptionFrom(() -> service.verifyParticipatingLogin(CHALLENGE_ID, "123456")).getErrorCode())
                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verifyNoInteractions(participatingUserRepository);
            // Input: subjectReference="O:1:0"
            // Procedure: verifyParticipatingLoginを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: participatingUserRepositoryが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-038: subjectReferenceの要素数が不正な場合INVALID_OR_EXPIRED_TOKENとなる")
        void verifyParticipatingLogin_wrongPartsCount_throwsInvalidToken() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.PARTICIPATING_LOGIN, "P:1"));
            assertThat(exceptionFrom(() -> service.verifyParticipatingLogin(CHALLENGE_ID, "123456")).getErrorCode())
                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verify(otpChallengeService, never()).consumeVerified(any(), any());
            // Input: subjectReference="P:1"（要素数2）
            // Procedure: verifyParticipatingLoginを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: otpChallengeService.consumeVerifiedが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-039: userId/authVersion部分が数値変換できない場合INVALID_OR_EXPIRED_TOKENとなる")
        void verifyParticipatingLogin_nonNumericParts_throwsInvalidToken() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.PARTICIPATING_LOGIN, "P:abc:0"));
            assertThat(exceptionFrom(() -> service.verifyParticipatingLogin(CHALLENGE_ID, "123456")).getErrorCode())
                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            // Input: subjectReference="P:abc:0"
            // Procedure: verifyParticipatingLoginを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: NumberFormatExceptionがキャッチされinvalidTokenに変換されること
        }

        @Test
        @DisplayName("UT-SAS-040: subjectReferenceがnullの場合の例外伝播を確認する")
        void verifyParticipatingLogin_nullSubjectReference_checksExceptionPropagation() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.PARTICIPATING_LOGIN, null));
            assertThatThrownBy(() -> service.verifyParticipatingLogin(CHALLENGE_ID, "123456"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ResponseConstants.MSG_INVALID_OR_EXPIRED_TOKEN);
            // Input: challenge.subjectReference()=null
            // Procedure: verifyParticipatingLoginを実行
            // Output: NullPointerExceptionまたは適切な例外が発生する
            // Verify: splitメソッド呼び出し時のnull未処理による想定外の例外伝播有無を確認すること
        }

        @Test
        @DisplayName("UT-SAS-041: ユーザーIDに対応する参加企業ユーザーが存在しない場合INVALID_OR_EXPIRED_TOKENとなる")
        void verifyParticipatingLogin_userNotFound_throwsInvalidToken() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.PARTICIPATING_LOGIN, "P:1:0"));
            when(participatingUserRepository.findById(1)).thenReturn(Optional.empty());
            assertThat(exceptionFrom(() -> service.verifyParticipatingLogin(CHALLENGE_ID, "123456")).getErrorCode())
                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verify(otpChallengeService, never()).consumeVerified(any(), any());
            // Input: participatingUserRepository.findByIdが空を返す
            // Procedure: verifyParticipatingLoginを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: otpChallengeService.consumeVerifiedが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-042: authVersionがチャレンジ発行時と現在で異なる場合INVALID_OR_EXPIRED_TOKENとなる")
        void verifyParticipatingLogin_authVersionMismatch_throwsInvalidToken() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.PARTICIPATING_LOGIN, "P:1:0"));
            ParticipatingCompany company = ParticipatingCompany.builder().companyStatus(CompanyStatus.ACTIVE).build();
            ParticipatingUser user = participatingUser(1, "user");
            user.setAuthVersion(1);
            user.setCompany(company);
            when(participatingUserRepository.findById(1)).thenReturn(Optional.of(user));
            assertThat(exceptionFrom(() -> service.verifyParticipatingLogin(CHALLENGE_ID, "123456")).getErrorCode())
                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verify(otpChallengeService, never()).consumeVerified(any(), any());
            // Input: subjectのauthVersion=0、user.authVersion=1（パスワード変更等で更新済み）
            // Procedure: verifyParticipatingLoginを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: otpChallengeService.consumeVerifiedが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-043: ユーザーが無効化されている場合INVALID_OR_EXPIRED_TOKENとなる")
        void verifyParticipatingLogin_userInactive_throwsInvalidToken() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.PARTICIPATING_LOGIN, "P:1:0"));
            ParticipatingCompany company = ParticipatingCompany.builder().companyStatus(CompanyStatus.ACTIVE).build();
            ParticipatingUser user = participatingUser(1, "user");
            user.setIsActive(false);
            user.setCompany(company);
            when(participatingUserRepository.findById(1)).thenReturn(Optional.of(user));
            assertThat(exceptionFrom(() -> service.verifyParticipatingLogin(CHALLENGE_ID, "123456")).getErrorCode())
                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verify(otpChallengeService, never()).consumeVerified(any(), any());
            // Input: user.isActive=false
            // Procedure: verifyParticipatingLoginを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: otpChallengeService.consumeVerifiedが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-044: 所属企業がACTIVEでない場合INVALID_OR_EXPIRED_TOKENとなる")
        void verifyParticipatingLogin_companyInactive_throwsInvalidToken() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.PARTICIPATING_LOGIN, "P:1:0"));
            ParticipatingCompany company = ParticipatingCompany.builder().companyStatus(CompanyStatus.SUSPENDED).build();
            ParticipatingUser user = participatingUser(1, "user");
            user.setCompany(company);
            when(participatingUserRepository.findById(1)).thenReturn(Optional.of(user));
            assertThat(exceptionFrom(() -> service.verifyParticipatingLogin(CHALLENGE_ID, "123456")).getErrorCode())
                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verify(otpChallengeService, never()).consumeVerified(any(), any());
            // Input: company.companyStatus=SUSPENDED等
            // Procedure: verifyParticipatingLoginを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: otpChallengeService.consumeVerifiedが呼ばれないこと
        }
    }

    @Nested
    @DisplayName("運営ユーザーログインOTP検証 のテスト")
    class VerifyOperatingLoginTest {

        @Test
        @DisplayName("UT-SAS-045: コード検証成功でJWTを含むAdminLoginResponseが返却される")
        void verifyOperatingLogin_validOtp_returnsAdminLoginResponse() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.OPERATING_LOGIN, "O:1:0"));
            OperatingUser user = operatingUser(1);
            when(operatingUserRepository.findById(1)).thenReturn(Optional.of(user));
            when(jwtUtil.generateToken(1, "operator", "OP-1", "operator", 0, java.util.List.of())).thenReturn("jwt");
            var response = service.verifyOperatingLogin(CHALLENGE_ID, "123456");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getToken()).isEqualTo("jwt");
            assertThat(response.getAdmin().getName()).isEqualTo("Yamada Taro");
            verify(otpChallengeService).consumeVerified(CHALLENGE_ID, OtpPurpose.OPERATING_LOGIN);
            // Input: challengeId、code（subjectReference="O:1:0"、user.authVersion=0、isActive=true、company.isActive=true）
            // Procedure: verifyOperatingLoginを実行
            // Output: statusCode=OKのAdminLoginResponseが返却される
            // Verify: token/adminが設定され、admin.name=lastName+" "+firstName、otpChallengeService.consumeVerifiedが呼ばれること
        }

        @Test
        @DisplayName("UT-SAS-046: subjectReferenceのprefixがOでない場合INVALID_OR_EXPIRED_TOKENとなる")
        void verifyOperatingLogin_wrongPrefix_throwsInvalidToken() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.OPERATING_LOGIN, "P:1:0"));
            assertThat(exceptionFrom(() -> service.verifyOperatingLogin(CHALLENGE_ID, "123456")).getErrorCode())
                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verifyNoInteractions(operatingUserRepository);
            // Input: subjectReference="P:1:0"
            // Procedure: verifyOperatingLoginを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: operatingUserRepositoryが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-047: 運営ユーザーが存在しない場合INVALID_OR_EXPIRED_TOKENとなる")
        void verifyOperatingLogin_userNotFound_throwsInvalidToken() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.OPERATING_LOGIN, "O:1:0"));
            when(operatingUserRepository.findById(1)).thenReturn(Optional.empty());
            assertThat(exceptionFrom(() -> service.verifyOperatingLogin(CHALLENGE_ID, "123456")).getErrorCode())
                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verify(otpChallengeService, never()).consumeVerified(any(), any());
            // Input: operatingUserRepository.findByIdが空を返す
            // Procedure: verifyOperatingLoginを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: otpChallengeService.consumeVerifiedが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-048: authVersionが不一致の場合INVALID_OR_EXPIRED_TOKENとなる")
        void verifyOperatingLogin_authVersionMismatch_throwsInvalidToken() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.OPERATING_LOGIN, "O:1:0"));
            OperatingUser user = operatingUser(1);
            user.setAuthVersion(2);
            when(operatingUserRepository.findById(1)).thenReturn(Optional.of(user));
            assertThat(exceptionFrom(() -> service.verifyOperatingLogin(CHALLENGE_ID, "123456")).getErrorCode())
                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verify(otpChallengeService, never()).consumeVerified(any(), any());
            // Input: subjectのauthVersion=0、user.authVersion=2
            // Procedure: verifyOperatingLoginを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: otpChallengeService.consumeVerifiedが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-049: 運営ユーザーが無効化されている場合INVALID_OR_EXPIRED_TOKENとなる")
        void verifyOperatingLogin_userInactive_throwsInvalidToken() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.OPERATING_LOGIN, "O:1:0"));
            OperatingUser user = operatingUser(1);
            user.setIsActive(false);
            when(operatingUserRepository.findById(1)).thenReturn(Optional.of(user));
            assertThat(exceptionFrom(() -> service.verifyOperatingLogin(CHALLENGE_ID, "123456")).getErrorCode())
                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verify(otpChallengeService, never()).consumeVerified(any(), any());
            // Input: user.isActive=false
            // Procedure: verifyOperatingLoginを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: otpChallengeService.consumeVerifiedが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-050: 所属企業がnullの場合INVALID_OR_EXPIRED_TOKENとなる")
        void verifyOperatingLogin_companyNull_throwsInvalidToken() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.OPERATING_LOGIN, "O:1:0"));
            OperatingUser user = operatingUser(1);
            user.setCompany(null);
            when(operatingUserRepository.findById(1)).thenReturn(Optional.of(user));
            assertThat(exceptionFrom(() -> service.verifyOperatingLogin(CHALLENGE_ID, "123456")).getErrorCode())
                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verify(otpChallengeService, never()).consumeVerified(any(), any());
            // Input: user.company=null
            // Procedure: verifyOperatingLoginを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: NullPointerExceptionが発生せずINVALID_OR_EXPIRED_TOKENに正しく変換されること
        }

        @Test
        @DisplayName("UT-SAS-051: 所属企業が無効化されている場合INVALID_OR_EXPIRED_TOKENとなる")
        void verifyOperatingLogin_companyInactive_throwsInvalidToken() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.OPERATING_LOGIN, "O:1:0"));
            OperatingUser user = operatingUser(1);
            user.getCompany().setIsActive(false);
            when(operatingUserRepository.findById(1)).thenReturn(Optional.of(user));
            assertThat(exceptionFrom(() -> service.verifyOperatingLogin(CHALLENGE_ID, "123456")).getErrorCode())
                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verify(otpChallengeService, never()).consumeVerified(any(), any());
            // Input: user.company.isActive=false
            // Procedure: verifyOperatingLoginを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: otpChallengeService.consumeVerifiedが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-052: AdminResponseのid/firstNameKana/lastNameKanaが未設定でnullのまま返る")
        void verifyOperatingLogin_toAdmin_leavesUnmappedFieldsNull() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.OPERATING_LOGIN, "O:1:0"));
            when(operatingUserRepository.findById(1)).thenReturn(Optional.of(operatingUser(1)));
            var response = service.verifyOperatingLogin(CHALLENGE_ID, "123456");
            assertThat(response.getAdmin().getId()).isNull();
            assertThat(response.getAdmin().getFirstNameKana()).isNull();
            assertThat(response.getAdmin().getLastNameKana()).isNull();
            // Input: user情報設定済み（toAdmin経由）
            // Procedure: verifyOperatingLoginを実行
            // Output: admin.id/firstNameKana/lastNameKanaがnullである
            // Verify: toAdminがidとKana系フィールドを一切設定していないこと
        }
    }

    @Nested
    @DisplayName("ログインOTP再送信（共通ロジック） のテスト")
    class ResendLoginOtpTest {

        @Test
        @DisplayName("UT-SAS-053: 再送信成功時に新しいコードがメール送信される")
        void resendLoginOtp_validChallenge_sendsNewCode() {
            when(otpChallengeService.resend(CHALLENGE_ID, OtpPurpose.PARTICIPATING_LOGIN)).thenReturn(issuedOtp());
            var response = service.resendLoginOtp(CHALLENGE_ID, OtpPurpose.PARTICIPATING_LOGIN);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getChallengeId()).isEqualTo(CHALLENGE_ID);
            assertThat(response.getMaskedEmail()).isEqualTo("p***@example.com");
            assertThat(response.getExpiresInSeconds()).isEqualTo(300L);
            verify(notificationService).sendOtpEmail("person@example.com", "123456", 5L);
            // Input: challengeId、expectedPurpose、otpChallengeService.resendが新しいIssuedOtpを返す
            // Procedure: resendLoginOtpを実行
            // Output: OtpChallengeResponse（statusCode=OK）が返却される
            // Verify: notificationService.sendOtpEmailが新しいissued.code()で呼ばれ、challengeId/maskedEmail/expiresInSecondsが設定されること
        }

        @Test
        @DisplayName("UT-SAS-054: otpChallengeService.resendがnullを返す場合INVALID_OR_EXPIRED_TOKENとなる")
        void resendLoginOtp_nullIssued_throwsInvalidToken() {
            when(otpChallengeService.resend(CHALLENGE_ID, OtpPurpose.PARTICIPATING_LOGIN)).thenReturn(null);
            assertThat(exceptionFrom(() -> service.resendLoginOtp(CHALLENGE_ID, OtpPurpose.PARTICIPATING_LOGIN))
                    .getErrorCode()).isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verifyNoInteractions(notificationService);
            // Input: otpChallengeService.resendがnullを返す（チャレンジ不正・期限切れ・用途不一致等）
            // Procedure: resendLoginOtpを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: notificationService.sendOtpEmailが呼ばれないこと
        }
    }

    @Nested
    @DisplayName("POST /auth/email-otp/settings/challenge のテスト")
    class StartEmailOtpSettingTest {

        @Test
        @DisplayName("UT-SAS-055: 一般ユーザー(participating)がOTP設定変更を開始できる")
        void startEmailOtpSetting_userRoleDisable_startsChallenge() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            ParticipatingUser user = participatingUser(1, "user");
            when(participatingUserRepository.findById(1)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("current", "encoded-old")).thenReturn(true);
            when(otpChallengeService.create(OtpPurpose.EMAIL_LOGIN_SETTING, "S:user:1:false", "person@example.com"))
                    .thenReturn(issuedOtp());
            var response = service.startEmailOtpSetting(1, "user", false, "current");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            verify(otpChallengeService).create(OtpPurpose.EMAIL_LOGIN_SETTING, "S:user:1:false", "person@example.com");
            verify(notificationService).sendOtpEmail("person@example.com", "123456", 5L);
            // Input: userId、role="user"、enabled=false、currentPassword（一致）
            // Procedure: startEmailOtpSettingを実行
            // Output: OtpChallengeResponse（statusCode=OK）が返却される
            // Verify: subjectReferenceが"S:user:{userId}:false"形式で生成され、notificationService.sendOtpEmailが呼ばれること
        }

        @Test
        @DisplayName("UT-SAS-056: 運営ユーザー(operator)がenabled=trueでOTP設定変更を開始できる")
        void startEmailOtpSetting_operatorRoleEnable_startsChallenge() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            when(operatingUserRepository.findById(1)).thenReturn(Optional.of(operatingUser(1)));
            when(passwordEncoder.matches("current", "encoded-old")).thenReturn(true);
            when(otpChallengeService.create(OtpPurpose.EMAIL_LOGIN_SETTING, "S:operator:1:true", "person@example.com"))
                    .thenReturn(issuedOtp());
            assertThat(service.startEmailOtpSetting(1, "operator", true, "current").getStatusCode())
                    .isEqualTo(ResponseConstants.OK);
            verify(operatingUserRepository).findById(1);
            verify(otpChallengeService).create(OtpPurpose.EMAIL_LOGIN_SETTING, "S:operator:1:true", "person@example.com");
            // Input: userId、role="operator"、enabled=true、currentPassword（一致）
            // Procedure: startEmailOtpSettingを実行
            // Output: OtpChallengeResponseが返却される
            // Verify: operatingUserRepositoryから検索され、subjectReferenceが"S:operator:{userId}:true"になること
        }

        @Test
        @DisplayName("UT-SAS-057: 運営ユーザーがenabled=falseを指定した場合FORBIDDENとなる")
        void startEmailOtpSetting_operatorDisable_throwsForbidden() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            assertThat(exceptionFrom(() -> service.startEmailOtpSetting(1, "operator", false, "current"))
                    .getErrorCode()).isEqualTo(ResponseConstants.FORBIDDEN);
            verifyNoInteractions(operatingUserRepository);
            // Input: role="operator"、enabled=false
            // Procedure: startEmailOtpSettingを実行
            // Output: CustomException(FORBIDDEN)がスローされる
            // Verify: operatingUserRepository.findByIdが一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-058: マスターユーザーがenabled=falseを指定した場合FORBIDDENとなる")
        void startEmailOtpSetting_masterDisable_throwsForbidden() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            assertThat(exceptionFrom(() -> service.startEmailOtpSetting(1, "master", false, "current"))
                    .getErrorCode()).isEqualTo(ResponseConstants.FORBIDDEN);
            verifyNoInteractions(participatingUserRepository);
            // Input: role="master"、enabled=false
            // Procedure: startEmailOtpSettingを実行
            // Output: CustomException(FORBIDDEN)がスローされる
            // Verify: participatingUserRepository.findByIdが一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-059: 現在のパスワードが一致しない場合INVALID_CURRENT_PASSWORDとなる")
        void startEmailOtpSetting_wrongCurrentPassword_throwsInvalidPassword() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            when(participatingUserRepository.findById(1)).thenReturn(Optional.of(participatingUser(1, "user")));
            when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);
            assertThat(exceptionFrom(() -> service.startEmailOtpSetting(1, "user", true, "wrong"))
                    .getErrorCode()).isEqualTo(ResponseConstants.INVALID_CURRENT_PASSWORD);
            verifyNoInteractions(otpChallengeService);
            // Input: currentPasswordが不一致
            // Procedure: startEmailOtpSettingを実行
            // Output: CustomException(INVALID_CURRENT_PASSWORD)がスローされる
            // Verify: otpChallengeService.createが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-060: 機能フラグOFFの場合NOT_FOUND例外となる")
        void startEmailOtpSetting_featureDisabled_throwsNotFound() {
            when(features.isLoginEmailOtp()).thenReturn(false);
            assertThat(exceptionFrom(() -> service.startEmailOtpSetting(1, "operator", true, "current"))
                    .getErrorCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            verifyNoInteractions(operatingUserRepository, participatingUserRepository);
            // Input: properties.features.loginEmailOtp=false
            // Procedure: startEmailOtpSettingを実行
            // Output: CustomException(NOT_FOUND)がスローされる
            // Verify: operatingUserRepository/participatingUserRepositoryが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-061: 対象の運営ユーザーが存在しない場合INVALID_OR_EXPIRED_TOKENとなる")
        void startEmailOtpSetting_operatorNotFound_throwsInvalidToken() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            when(operatingUserRepository.findById(1)).thenReturn(Optional.empty());
            assertThat(exceptionFrom(() -> service.startEmailOtpSetting(1, "operator", true, "current"))
                    .getErrorCode()).isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verifyNoInteractions(passwordEncoder);
            // Input: role="operator"、operatingUserRepository.findByIdが空を返す
            // Procedure: startEmailOtpSettingを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: passwordEncoder.matchesが呼ばれないこと
        }
    }

    @Nested
    @DisplayName("POST /auth/email-otp/settings/confirm のテスト")
    class ConfirmEmailOtpSettingTest {

        @Test
        @DisplayName("UT-SAS-062: 一般ユーザーが認証コード検証成功でOTP設定が確定する")
        void confirmEmailOtpSetting_userRole_confirmsSetting() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.EMAIL_LOGIN_SETTING, "S:user:1:true"));
            ParticipatingUser user = participatingUser(1, "user");
            when(participatingUserRepository.findById(1)).thenReturn(Optional.of(user));
            BaseResponse response = service.confirmEmailOtpSetting(1, "user", CHALLENGE_ID, "123456");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_EMAIL_OTP_SETTING_UPDATED);
            assertThat(user.getEmailOtpLoginEnabled()).isTrue();
            verify(authVersionService).increment(user);
            verify(otpChallengeService).consumeVerified(CHALLENGE_ID, OtpPurpose.EMAIL_LOGIN_SETTING);
            // Input: userId、role="user"、challengeId、code（subjectReference="S:user:{userId}:true"）
            // Procedure: confirmEmailOtpSettingを実行
            // Output: BaseResponse成功（MSG_EMAIL_OTP_SETTING_UPDATED）が返却される
            // Verify: user.emailOtpLoginEnabled=trueに更新、authVersionService.incrementが呼ばれ、otpChallengeService.consumeVerifiedが呼ばれること
        }

        @Test
        @DisplayName("UT-SAS-063: 運営ユーザーが認証コード検証成功でOTP設定が確定する")
        void confirmEmailOtpSetting_operatorRole_confirmsSetting() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.EMAIL_LOGIN_SETTING, "S:operator:1:true"));
            OperatingUser user = operatingUser(1);
            when(operatingUserRepository.findById(1)).thenReturn(Optional.of(user));
            assertThat(service.confirmEmailOtpSetting(1, "operator", CHALLENGE_ID, "123456").getStatusCode())
                    .isEqualTo(ResponseConstants.OK);
            assertThat(user.getEmailOtpLoginEnabled()).isTrue();
            verify(authVersionService).increment(user);
            // Input: userId、role="operator"、subjectReference="S:operator:{userId}:true"
            // Procedure: confirmEmailOtpSettingを実行
            // Output: BaseResponse成功が返却される
            // Verify: OperatingUserのemailOtpLoginEnabledが更新され、authVersionService.incrementが呼ばれること
        }

        @Test
        @DisplayName("UT-SAS-064: challenge.purposeがEMAIL_LOGIN_SETTINGでない場合INVALID_OR_EXPIRED_TOKENとなる")
        void confirmEmailOtpSetting_wrongPurpose_throwsInvalidToken() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.PARTICIPATING_LOGIN, "S:user:1:true"));
            assertThat(exceptionFrom(() -> service.confirmEmailOtpSetting(1, "user", CHALLENGE_ID, "123456"))
                    .getErrorCode()).isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verifyNoInteractions(participatingUserRepository);
            // Input: challenge.purpose=PARTICIPATING_LOGIN
            // Procedure: confirmEmailOtpSettingを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: リポジトリ更新処理が呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-065: subjectReferenceの要素数が4でない場合INVALID_OR_EXPIRED_TOKENとなる")
        void confirmEmailOtpSetting_wrongPartsCount_throwsInvalidToken() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.EMAIL_LOGIN_SETTING, "S:user:1"));
            assertThat(exceptionFrom(() -> service.confirmEmailOtpSetting(1, "user", CHALLENGE_ID, "123456"))
                    .getErrorCode()).isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verify(otpChallengeService, never()).consumeVerified(any(), any());
            // Input: subjectReference="S:user:1"（要素数3）
            // Procedure: confirmEmailOtpSettingを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: otpChallengeService.consumeVerifiedが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-066: リクエストのroleがsubjectReferenceのroleと異なる場合INVALID_OR_EXPIRED_TOKENとなる")
        void confirmEmailOtpSetting_roleMismatch_throwsInvalidToken() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.EMAIL_LOGIN_SETTING, "S:user:1:true"));
            assertThat(exceptionFrom(() -> service.confirmEmailOtpSetting(1, "operator", CHALLENGE_ID, "123456"))
                    .getErrorCode()).isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verifyNoInteractions(participatingUserRepository, operatingUserRepository);
            // Input: 呼び出し時role="operator"、subjectReferenceのrole部分="user"
            // Procedure: confirmEmailOtpSettingを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: どちらのリポジトリも更新されないこと
        }

        @Test
        @DisplayName("UT-SAS-067: リクエストのuserIdがsubjectReferenceのuserIdと異なる場合INVALID_OR_EXPIRED_TOKENとなる")
        void confirmEmailOtpSetting_userIdMismatch_throwsInvalidToken() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.EMAIL_LOGIN_SETTING, "S:user:1:true"));
            assertThat(exceptionFrom(() -> service.confirmEmailOtpSetting(2, "user", CHALLENGE_ID, "123456"))
                    .getErrorCode()).isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verify(otpChallengeService, never()).consumeVerified(any(), any());
            // Input: 呼び出し時userId=2、subjectReferenceのuserId部分=1
            // Procedure: confirmEmailOtpSettingを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: otpChallengeService.consumeVerifiedが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-068: マスターユーザーを無効化しようとした場合FORBIDDENとなる（role=user経路）")
        void confirmEmailOtpSetting_masterDisableViaUserRole_throwsForbidden() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.EMAIL_LOGIN_SETTING, "S:user:1:false"));
            ParticipatingUser master = participatingUser(1, "master");
            when(participatingUserRepository.findById(1)).thenReturn(Optional.of(master));
            assertThat(exceptionFrom(() -> service.confirmEmailOtpSetting(1, "user", CHALLENGE_ID, "123456"))
                    .getErrorCode()).isEqualTo(ResponseConstants.FORBIDDEN);
            verify(authVersionService, never()).increment(any(ParticipatingUser.class));
            // Input: role="user"、subjectReferenceのenabled="false"、DB上のuser.role="master"
            // Procedure: confirmEmailOtpSettingを実行
            // Output: CustomException(FORBIDDEN)がスローされる
            // Verify: user.setEmailOtpLoginEnabledおよびauthVersionService.incrementが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-069【欠陥候補】: 運営ユーザー(operator)がenabled=falseで確定しようとした場合に拒否されるかを確認する")
        void confirmEmailOtpSetting_operatorDisable_checksRejection() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.EMAIL_LOGIN_SETTING, "S:operator:1:false"));
            OperatingUser operator = operatingUser(1);
            when(operatingUserRepository.findById(1)).thenReturn(Optional.of(operator));
            assertThat(exceptionFrom(() -> service.confirmEmailOtpSetting(1, "operator", CHALLENGE_ID, "123456"))
                    .getErrorCode()).isEqualTo(ResponseConstants.FORBIDDEN);
            assertThat(operator.getEmailOtpLoginEnabled()).isTrue();
            verify(authVersionService, never()).increment(any(OperatingUser.class));
            // Input: role="operator"、subjectReferenceのenabled="false"
            // Procedure: confirmEmailOtpSettingを実行
            // Output: FORBIDDEN等で拒否されることを期待（startEmailOtpSettingとの整合性）
            // Verify: operatorブロックにenabled=false拒否ロジックが存在しないため、実際はemailOtpLoginEnabled=falseで更新されてしまう実装漏れを検出すること
        }

        @Test
        @DisplayName("UT-SAS-070: roleがnullの場合の例外発生有無を確認する")
        void confirmEmailOtpSetting_nullRole_checksExceptionBehavior() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.EMAIL_LOGIN_SETTING, "S:user:1:true"));
            assertThatThrownBy(() -> service.confirmEmailOtpSetting(1, null, CHALLENGE_ID, "123456"))
                    .isInstanceOf(NullPointerException.class);
            // Input: role=null、subjectReferenceのrole部分="user"
            // Procedure: confirmEmailOtpSettingを実行
            // Output: NullPointerExceptionまたは適切な例外が発生する
            // Verify: role.equals(parts[1])呼び出し時にNullPointerExceptionが発生し想定外の例外となることを確認すること
        }

        @Test
        @DisplayName("UT-SAS-071: 機能フラグOFFの場合NOT_FOUND例外となる")
        void confirmEmailOtpSetting_featureDisabled_throwsNotFound() {
            when(features.isLoginEmailOtp()).thenReturn(false);
            assertThat(exceptionFrom(() -> service.confirmEmailOtpSetting(1, "user", CHALLENGE_ID, "123456"))
                    .getErrorCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            verifyNoInteractions(otpChallengeService);
            // Input: properties.features.loginEmailOtp=false
            // Procedure: confirmEmailOtpSettingを実行
            // Output: CustomException(NOT_FOUND)がスローされる
            // Verify: otpChallengeService.verifyが呼ばれないこと
        }

        @Test
        @DisplayName("UT-SAS-072: 対象の参加企業ユーザーが存在しない場合INVALID_OR_EXPIRED_TOKENとなる")
        void confirmEmailOtpSetting_participatingUserNotFound_throwsInvalidToken() {
            when(features.isLoginEmailOtp()).thenReturn(true);
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.EMAIL_LOGIN_SETTING, "S:user:1:true"));
            when(participatingUserRepository.findById(1)).thenReturn(Optional.empty());
            assertThat(exceptionFrom(() -> service.confirmEmailOtpSetting(1, "user", CHALLENGE_ID, "123456"))
                    .getErrorCode()).isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
            verify(otpChallengeService, never()).consumeVerified(any(), any());
            // Input: role="user"、participatingUserRepository.findByIdが空を返す
            // Procedure: confirmEmailOtpSettingを実行
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN)がスローされる
            // Verify: otpChallengeService.consumeVerifiedが呼ばれないこと
        }
    }

    @Nested
    @DisplayName("内部処理:パスワードリセットリンク発行 のテスト")
    class IssuePasswordResetTest {

        @Test
        @DisplayName("UT-SAS-073: issuePasswordResetがトランザクションコミット後にメール配信をトリガーする")
        void issuePasswordReset_syncActive_deliversAfterCommit() {
            when(features.isPasswordReset()).thenReturn(true);
            when(participatingUserRepository.findByEmailIgnoreCase("person@example.com"))
                    .thenReturn(Optional.of(participatingUser(10, "user")));
            when(operatingUserRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.empty());
            UUID linkId = UUID.randomUUID();
            when(actionLinkService.issue(SecurityActionPurpose.PASSWORD_RESET,
                    SecurityActionSubjectType.PARTICIPATING_USER, 10, "self-service"))
                    .thenReturn(new SecurityActionLinkService.IssuedActionLink(
                            linkId, "raw", LocalDateTime.now().plusMinutes(5), "https://example/reset"));
            org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
            try {
                service.requestPasswordReset(forgotPasswordRequest("person@example.com"));
                assertThat(org.springframework.transaction.support.TransactionSynchronizationManager
                        .getSynchronizations()).hasSize(1);
                verifyNoInteractions(securityEmailDeliveryService);
                org.springframework.transaction.support.TransactionSynchronizationManager
                        .getSynchronizations().get(0).afterCommit();
                verify(securityEmailDeliveryService).deliverActionLink(
                        org.mockito.ArgumentMatchers.eq(linkId), org.mockito.ArgumentMatchers.eq("person@example.com"),
                        anyString(), anyString());
            } finally {
                org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
            }
            // Input: type=PARTICIPATING_USER、id、email、actor
            // Procedure: requestPasswordReset経由でissuePasswordResetが実行される（トランザクション同期がアクティブ）
            // Output: afterCommitコールバックとしてdeliverResetLinkが登録される
            // Verify: TransactionSynchronizationManagerにコールバックが登録され、コミット後にsecurityEmailDeliveryService.deliverActionLinkが呼ばれること
        }

        @Test
        @DisplayName("UT-SAS-074: トランザクション同期が非アクティブな場合は即時にメール配信される")
        void issuePasswordReset_syncInactive_deliversImmediately() {
            when(features.isPasswordReset()).thenReturn(true);
            when(participatingUserRepository.findByEmailIgnoreCase("person@example.com"))
                    .thenReturn(Optional.of(participatingUser(10, "user")));
            when(operatingUserRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.empty());
            UUID linkId = UUID.randomUUID();
            when(actionLinkService.issue(SecurityActionPurpose.PASSWORD_RESET,
                    SecurityActionSubjectType.PARTICIPATING_USER, 10, "self-service"))
                    .thenReturn(new SecurityActionLinkService.IssuedActionLink(
                            linkId, "raw", LocalDateTime.now().plusMinutes(5), "https://example/reset"));
            service.requestPasswordReset(forgotPasswordRequest("person@example.com"));
            verify(securityEmailDeliveryService).deliverActionLink(
                    org.mockito.ArgumentMatchers.eq(linkId), org.mockito.ArgumentMatchers.eq("person@example.com"),
                    anyString(), anyString());
            // Input: TransactionSynchronizationManager.isSynchronizationActive()=false
            // Procedure: afterCommitを含む処理を実行
            // Output: 即時にdeliverResetLink相当の処理が実行される
            // Verify: actionが同期登録されず直接runされること
        }
    }

    @Nested
    @DisplayName("内部処理:安全なバージョン取得 のテスト")
    class SafeVersionTest {

        @Test
        @DisplayName("UT-SAS-075: authVersionがnullの場合0を返す")
        void safeVersion_nullVersion_returnsZero() {
            assertThat(invokePrivate("safeVersion", new Class<?>[] { Integer.class }, new Object[] { null }))
                    .isEqualTo(0);
            // Input: version=null
            // Procedure: safeVersionを実行
            // Output: 0が返却される
            // Verify: nullの場合に0へフォールバックしていること
        }

        @Test
        @DisplayName("UT-SAS-076: authVersionが設定済みの場合その値を返す")
        void safeVersion_setVersion_returnsSameValue() {
            assertThat(invokePrivate("safeVersion", new Class<?>[] { Integer.class }, 5)).isEqualTo(5);
            // Input: version=5
            // Procedure: safeVersionを実行
            // Output: 5が返却される
            // Verify: 設定値がそのまま返却されること
        }
    }

    @Nested
    @DisplayName("内部処理:管理者情報変換 のテスト")
    class ToAdminTest {

        @Test
        @DisplayName("UT-SAS-077: toAdminが姓名を正しく結合する")
        void toAdmin_lastAndFirstName_combinesCorrectly() {
            when(otpChallengeService.verify(CHALLENGE_ID, "123456"))
                    .thenReturn(otpData(OtpPurpose.OPERATING_LOGIN, "O:1:0"));
            when(operatingUserRepository.findById(1)).thenReturn(Optional.of(operatingUser(1)));
            var response = service.verifyOperatingLogin(CHALLENGE_ID, "123456");
            assertThat(response.getAdmin().getName()).isEqualTo("Yamada Taro");
            // Input: user.lastName="山田"、user.firstName="太郎"
            // Procedure: toAdminを実行（verifyOperatingLogin経由）
            // Output: name="山田 太郎"（姓+半角スペース+名）が設定される
            // Verify: nameフィールドが契約通りの連結順序・区切り文字になっていること
        }
    }
}