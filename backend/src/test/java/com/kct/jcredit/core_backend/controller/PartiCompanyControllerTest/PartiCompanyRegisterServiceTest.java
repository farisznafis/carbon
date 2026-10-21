package com.kct.jcredit.core_backend.controller.PartiCompanyControllerTest;

import com.kct.jcredit.core_backend.config.SecurityFlowProperties;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyInfoValidateRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyRegisterRequest;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.enums.OtpPurpose;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRegisterRepository;
import com.kct.jcredit.core_backend.service.NotificationMessages;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.OtpChallengeService;
import com.kct.jcredit.core_backend.service.PartiCompanySignupValidator;
import com.kct.jcredit.core_backend.service.impl.PartiCompanyRegisterServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * PartiCompanyRegisterServiceImpl#register / validateCompanyInfo test outline.
 */
class PartiCompanyRegisterServiceTest {
    private NotificationService notificationService;
    private ParticipatingCompanyRegisterRepository repository;
    private PartiCompanySignupValidator signupValidator;
    private OtpChallengeService otpChallengeService;
    private SecurityFlowProperties securityFlowProperties;
    private NotificationMessages messages;
    private PartiCompanyRegisterServiceImpl service;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        repository = mock(ParticipatingCompanyRegisterRepository.class);
        signupValidator = mock(PartiCompanySignupValidator.class);
        otpChallengeService = mock(OtpChallengeService.class);
        securityFlowProperties = new SecurityFlowProperties();
        messages = mock(NotificationMessages.class);
        when(messages.of(anyString(), any(Object[].class)))
                .thenReturn(new NotificationMessages.Content("subject", "body"));
        when(messages.text(anyString(), any(Object[].class))).thenReturn("（未入力）");
        when(repository.save(any(ParticipatingCompany.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findByCorpNumberAndCompanyStatus(anyString(), any()))
                .thenReturn(Optional.empty());
        when(repository.findByEmailAndCompanyStatus(anyString(), any()))
                .thenReturn(Optional.empty());
        service = new PartiCompanyRegisterServiceImpl(notificationService, repository,
                signupValidator, otpChallengeService, securityFlowProperties, messages);
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private PartiCompanyRegisterRequest validRequest() {
        return PartiCompanyRegisterRequest.builder()
                .corporateNumber("1234567890123")
                .corporateName("株式会社サンプル")
                .corporateNameKana("カブシキガイシャサンプル")
                .companyNameEnglish("Sample Corporation")
                .corporatePhoneNumber("03-1234-5678")
                .postalCode("1234567")
                .prefecture("東京都")
                .municipality("渋谷区桜丘町")
                .blocknum("1-1")
                .buildingName("サンプルビル 3F")
                .lastName("山田")
                .firstName("太郎")
                .lastNameKana("ヤマダ")
                .firstNameKana("タロウ")
                .position("会長")
                .phoneNumber("03-1234-5678")
                .faxNumber("03-1234-5679")
                .email("yamada@company.com")
                .methodology(List.of("太陽光発電"))
                .activityInformation("lorem ipsum")
                .build();
    }

    @Nested
    @DisplayName("POST /company/signup/company-info/validate のテスト")
    class ValidateCompanyInfoTest {

        @Test
        @DisplayName("UT-PCRS-001: signupValidatorへrequestが渡り、正常にバリデーションが完了する")
        void validateCompanyInfo_validRequest_delegatesToValidator() {
            PartiCompanyInfoValidateRequest request = new PartiCompanyInfoValidateRequest();

            service.validateCompanyInfo(request);

            verify(signupValidator).validateCompanyInfo(request);
        }

        @Test
        @DisplayName("UT-PCRS-002: signupValidatorがバリデーション例外をthrowした場合、そのまま伝播する")
        void validateCompanyInfo_invalidRequest_propagatesException() {
            PartiCompanyInfoValidateRequest request = new PartiCompanyInfoValidateRequest();
            CustomException expected = new CustomException(
                    ResponseConstants.VALIDATION_ERROR, ResponseConstants.MSG_VALIDATION_ERROR);
            doThrow(expected).when(signupValidator).validateCompanyInfo(request);

            assertThatThrownBy(() -> service.validateCompanyInfo(request))
                    .isSameAs(expected);
        }
    }

    @Nested
    @DisplayName("POST /company/signup のテスト")
    class RegisterTest {

        @Test
        @DisplayName("UT-PCRS-003: signupValidatorがバリデーション例外をthrowした場合、登録処理が行われず例外が伝播する")
        void register_validationFails_propagatesExceptionWithoutSaving() {
            PartiCompanyRegisterRequest request = validRequest();
            CustomException expected = new CustomException(
                    ResponseConstants.VALIDATION_ERROR, ResponseConstants.MSG_VALIDATION_ERROR);
            doThrow(expected).when(signupValidator).validateCompanyInfo(request);

            assertThatThrownBy(() -> service.register(request)).isSameAs(expected);

            verifyNoInteractions(repository, notificationService, otpChallengeService);
        }

        @Test
        @DisplayName("UT-PCRS-004: isRejectedSignupRevision=trueかつ該当企業がREJECTED状態で既存の場合、CONFLICT例外をthrowする")
        void register_rejectedRevisionFeatureOnAndExistingRejected_throwsConflict() {
            PartiCompanyRegisterRequest request = validRequest();
            securityFlowProperties.getFeatures().setRejectedSignupRevision(true);
            when(repository.findByCorpNumberAndCompanyStatus(
                    request.getCorporateNumber(), CompanyStatus.REJECTED))
                    .thenReturn(Optional.of(new ParticipatingCompany()));

            assertThatThrownBy(() -> service.register(request))
                    .isInstanceOfSatisfying(CustomException.class, exception -> {
                        assertThat(exception.getErrorCode()).isEqualTo(ResponseConstants.CONFLICT);
                        assertThat(exception).hasMessage(
                                "Rejected registrations must be revised through the emailed action link.");
                    });

            verify(repository, never()).save(any());
            verifyNoInteractions(notificationService, otpChallengeService);
        }

        @Test
        @DisplayName("UT-PCRS-005: isSignupEmailVerification=trueかつemailVerificationChallengeIdがnullの場合、OTP_REQUIRED例外をthrowする")
        void register_emailVerificationRequiredButChallengeIdNull_throwsOtpRequired() {
            PartiCompanyRegisterRequest request = validRequest();
            securityFlowProperties.getFeatures().setSignupEmailVerification(true);

            assertThatThrownBy(() -> service.register(request))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(ResponseConstants.OTP_REQUIRED));

            verifyNoInteractions(otpChallengeService);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("UT-PCRS-006: isSignupEmailVerification=trueかつchallengeIdが未検証の場合、otpChallengeServiceの例外が伝播する")
        void register_emailVerificationChallengeNotVerified_propagatesException() {
            PartiCompanyRegisterRequest request = validRequest();
            UUID challengeId = UUID.randomUUID();
            request.setEmailVerificationChallengeId(challengeId);
            securityFlowProperties.getFeatures().setSignupEmailVerification(true);
            CustomException expected = new CustomException(
                    ResponseConstants.INVALID_OR_EXPIRED_TOKEN, ResponseConstants.MSG_INVALID_OR_EXPIRED_TOKEN);
            doThrow(expected).when(otpChallengeService).requireVerified(
                    challengeId, OtpPurpose.SIGNUP_EMAIL_VERIFICATION, request.getEmail());

            assertThatThrownBy(() -> service.register(request)).isSameAs(expected);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("UT-PCRS-007: 新規登録（isRejectedSignupRevision=false、REJECTED企業なし）の場合、新規entityが作成されCREATEDが返る")
        void register_newEntityNoReactivation_returnsCreated() {
            PartiCompanyRegisterRequest request = validRequest();

            var response = service.register(request);

            assertThat(response.getStatusCode()).isEqualTo("CREATED");
            assertThat(response.getMessage()).isEqualTo("登録申請が完了しました。");
            assertThat(response.getRegistrationId()).isNotBlank();
            assertThat(response.getStatus()).isEqualTo("PENDING");
            ArgumentCaptor<ParticipatingCompany> companyCaptor =
                    ArgumentCaptor.forClass(ParticipatingCompany.class);
            verify(repository).save(companyCaptor.capture());
            assertThat(companyCaptor.getValue().getCompanyStatus()).isEqualTo(CompanyStatus.PENDING);
            assertThat(companyCaptor.getValue().getIsActive()).isFalse();
            verify(notificationService).createNotificationForAllOperatingUsers(anyString(), anyString());
            verify(notificationService).sendEmailToAllOperatorCompanies(anyString(), anyString());
            verify(otpChallengeService, never()).consumeVerified(any(), any());
        }

        @Test
        @DisplayName("UT-PCRS-008: isRejectedSignupRevision=trueかつ既存REJECTED企業が見つかる場合、既存entityを再利用（再申請）してCREATEDが返る")
        void register_rejectedRevisionFeatureOnAndEntityFoundByCorpNumber_reactivatesEntity() {
            PartiCompanyRegisterRequest request = validRequest();
            request.setEmail("new-email@company.com");
            securityFlowProperties.getFeatures().setRejectedSignupRevision(true);

            var response = service.register(request);

            assertThat(response.getStatusCode()).isEqualTo("CREATED");
            assertThat(response.getRegistrationId()).isNotBlank();
            ArgumentCaptor<ParticipatingCompany> companyCaptor =
                    ArgumentCaptor.forClass(ParticipatingCompany.class);
            verify(repository).save(companyCaptor.capture());
            assertThat(companyCaptor.getValue().getId()).isNull();
            assertThat(companyCaptor.getValue().getEmail()).isEqualTo("new-email@company.com");
            assertThat(companyCaptor.getValue().getCompanyStatus()).isEqualTo(CompanyStatus.PENDING);
        }

        @Test
        @DisplayName("UT-PCRS-009: isSignupEmailVerification=trueかつchallengeIdが検証済みの場合、emailVerifiedAtが設定されcommit後にconsumeVerifiedが呼ばれる")
        void register_emailVerificationVerified_setsVerifiedAtAndConsumesAfterCommit() {
            PartiCompanyRegisterRequest request = validRequest();
            UUID challengeId = UUID.randomUUID();
            request.setEmailVerificationChallengeId(challengeId);
            securityFlowProperties.getFeatures().setSignupEmailVerification(true);
            TransactionSynchronizationManager.initSynchronization();

            var response = service.register(request);

            assertThat(response.getStatusCode()).isEqualTo("CREATED");
            ArgumentCaptor<ParticipatingCompany> companyCaptor =
                    ArgumentCaptor.forClass(ParticipatingCompany.class);
            verify(repository).save(companyCaptor.capture());
            assertThat(companyCaptor.getValue().getEmailVerifiedAt()).isNotNull();
            verify(otpChallengeService, never()).consumeVerified(
                    challengeId, OtpPurpose.SIGNUP_EMAIL_VERIFICATION);
            TransactionSynchronization synchronization =
                    TransactionSynchronizationManager.getSynchronizations().get(0);
            synchronization.afterCommit();
            verify(otpChallengeService).consumeVerified(
                    challengeId, OtpPurpose.SIGNUP_EMAIL_VERIFICATION);
        }

        @Test
        @DisplayName("UT-PCRS-010: generateUniqueCompanyIdが重複companyIdを検知した場合、再生成して一意なIDを設定する")
        void register_companyIdCollision_regeneratesUntilUnique() {
            PartiCompanyRegisterRequest request = validRequest();
            when(repository.existsByCompanyId(anyString())).thenReturn(true, false);

            var response = service.register(request);

            assertThat(response.getStatusCode()).isEqualTo("CREATED");
            ArgumentCaptor<String> companyIdCaptor = ArgumentCaptor.forClass(String.class);
            verify(repository, times(2)).existsByCompanyId(companyIdCaptor.capture());
            assertThat(companyIdCaptor.getAllValues()).hasSize(2);
            assertThat(companyIdCaptor.getAllValues().get(0))
                    .isNotEqualTo(companyIdCaptor.getAllValues().get(1));
            ArgumentCaptor<ParticipatingCompany> companyCaptor =
                    ArgumentCaptor.forClass(ParticipatingCompany.class);
            verify(repository).save(companyCaptor.capture());
            assertThat(companyCaptor.getValue().getCompanyId())
                    .isEqualTo(companyIdCaptor.getAllValues().get(1));
        }

        @Test
        @DisplayName("UT-PCRS-011: methodologyがnullの場合、通知メール本文の技術リスト項目が空値表示になる")
        void register_methodologyNull_usesEmptyValueInNotificationMail() {
            PartiCompanyRegisterRequest request = validRequest();
            request.setMethodology(null);
            when(messages.of(eq("signup.operator.mail"), any(Object[].class)))
                    .thenAnswer(invocation -> new NotificationMessages.Content("subject",
                            java.util.Arrays.stream(invocation.getArguments())
                                    .map(String::valueOf)
                                    .collect(java.util.stream.Collectors.joining("|"))));

            var response = service.register(request);

            assertThat(response.getStatusCode()).isEqualTo("CREATED");
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(notificationService).sendEmailToAllOperatorCompanies(anyString(), bodyCaptor.capture());
            assertThat(bodyCaptor.getValue()).contains("（未入力）");
        }
    }
}