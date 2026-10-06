package com.kct.jcredit.core_backend.controller.PartiCompanyControllerTest;

import com.kct.jcredit.core_backend.config.SecurityFlowProperties;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyRegisterRequest;
import com.kct.jcredit.core_backend.dto.response.OtpChallengeResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.enums.OtpPurpose;
import com.kct.jcredit.core_backend.enums.SecurityActionPurpose;
import com.kct.jcredit.core_backend.enums.SecurityActionSubjectType;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.service.CompanySecurityFlowService;
import com.kct.jcredit.core_backend.service.NotificationMessages;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.OtpChallengeService;
import com.kct.jcredit.core_backend.service.PartiCompanySignupValidator;
import com.kct.jcredit.core_backend.service.SecurityActionLinkService;
import com.kct.jcredit.core_backend.service.SecurityEmailDeliveryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CompanySecurityFlowService test outline.
 * 対象メソッド: startSignupEmailVerification, resendSignupEmailVerification,
 *             verifySignupEmail, getRevisionDraft(getRejectedSignupRevision), reviseRejectedSignup
 */
class CompanySecurityFlowServiceTest {
    private static final String SESSION_TOKEN = "session-token-123";
    private static final Integer COMPANY_ID = 5678;

    private SecurityFlowProperties properties;
    private OtpChallengeService otpChallengeService;
    private SecurityActionLinkService actionLinkService;
    private NotificationService notificationService;
    private SecurityEmailDeliveryService securityEmailDeliveryService;
    private ParticipatingCompanyRepository companyRepository;
    private PartiCompanySignupValidator signupValidator;
    private NotificationMessages messages;
    private CompanySecurityFlowService service;

    @BeforeEach
    void setUp() {
        properties = new SecurityFlowProperties();
        otpChallengeService = mock(OtpChallengeService.class);
        actionLinkService = mock(SecurityActionLinkService.class);
        notificationService = mock(NotificationService.class);
        securityEmailDeliveryService = mock(SecurityEmailDeliveryService.class);
        companyRepository = mock(ParticipatingCompanyRepository.class);
        signupValidator = mock(PartiCompanySignupValidator.class);
        messages = mock(NotificationMessages.class);
        service = new CompanySecurityFlowService(properties, otpChallengeService,
                actionLinkService, notificationService, securityEmailDeliveryService,
                companyRepository, signupValidator, messages);
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private SecurityActionLinkService.ActionSessionData actionSession(int subjectId) {
        return new SecurityActionLinkService.ActionSessionData(UUID.randomUUID(),
                SecurityActionPurpose.REJECTED_SIGNUP_REVISION,
                SecurityActionSubjectType.COMPANY, subjectId);
    }

    private ParticipatingCompany rejectedCompany() {
        ParticipatingCompany company = companyWithStatus(CompanyStatus.REJECTED);
        company.setId(COMPANY_ID);
        company.setCompanyId("COMP12345678");
        company.setNameJa("株式会社サンプル");
        company.setNameFurigana("カブシキガイシャサンプル");
        company.setNameEn("Sample Corporation");
        company.setEmail("yamada@company.com");
        company.setPhoneNumber("03-1234-5678");
        company.setCorpNumber("1234567890123");
        company.setPostalCode("1234567");
        company.setPrefecture("東京都");
        company.setMunicipality("渋谷区桜丘町");
        company.setBlocknum("1-1");
        company.setBuildingName("サンプルビル 3F");
        company.setRepLastName("山田");
        company.setRepFirstName("太郎");
        company.setRepLastNameKana("ヤマダ");
        company.setRepFirstNameKana("タロウ");
        company.setRepPosition("会長");
        company.setRepPhoneNumber("03-1234-5678");
        company.setFax("03-1234-5679");
        company.setRequestedMethodologies(List.of("太陽光発電"));
        company.setActivityInformation("lorem ipsum");
        company.setLastRejectionReason("書類不備");
        company.setRejectedAt(LocalDateTime.of(2025, 1, 2, 3, 4));
        return company;
    }

    private ParticipatingCompany companyWithStatus(CompanyStatus status) {
        ParticipatingCompany company = new ParticipatingCompany();
        company.setCompanyStatus(status);
        return company;
    }

    private PartiCompanyRegisterRequest validRequest() {
        return PartiCompanyRegisterRequest.builder()
                .corporateNumber("1234567890123")
                .corporateName("株式会社サンプル改")
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

    private void stubRejectedCompany(ParticipatingCompany company) {
        when(actionLinkService.requireSession(SESSION_TOKEN,
                SecurityActionPurpose.REJECTED_SIGNUP_REVISION)).thenReturn(actionSession((int) COMPANY_ID));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(companyRepository.findByCorpNumber(anyString())).thenReturn(Optional.empty());
        when(companyRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(companyRepository.save(any(ParticipatingCompany.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("POST /company/signup/email-verification/start")
    class StartSignupEmailVerificationTest {

        @Test
        @DisplayName("UT-CSFS-001: isSignupEmailVerification=falseの場合、NOT_FOUND例外をthrowする")
        void startSignupEmailVerification_featureDisabled_throwsNotFound() {
            // Input: email="yamada@company.com"
            //        properties.getFeatures().isSignupEmailVerification()=false
            // Output: CustomException(NOT_FOUND, MSG_NOT_FOUND)がthrowされること
            //         otpChallengeService.create、notificationService.sendOtpEmailが一度も呼ばれないこと
            assertThatThrownBy(() -> service.startSignupEmailVerification("yamada@company.com"))
                    .isInstanceOfSatisfying(CustomException.class, exception -> {
                        assertThat(exception.getErrorCode()).isEqualTo(ResponseConstants.NOT_FOUND);
                        assertThat(exception).hasMessage(ResponseConstants.MSG_NOT_FOUND);
                    });
            verifyNoInteractions(otpChallengeService, notificationService);
        }

        @Test
        @DisplayName("UT-CSFS-002: isSignupEmailVerification=trueの場合、OTPが発行されメール送信され正常なレスポンスが返る")
        void startSignupEmailVerification_featureEnabled_returnsOtpChallengeResponse() {
            // Input: email="yamada@company.com"
            //        properties.getFeatures().isSignupEmailVerification()=true
            //        otpChallengeService.create(SIGNUP_EMAIL_VERIFICATION, "public-signup", email)がIssuedOtp(challengeId=UUID, code="123456", email="yamada@company.com", maskedEmail="y****@company.com", expiresInSeconds=300)を返すようモック
            //        properties.getOtpTtl()が5分を返すようモック
            // Output: statusCode=OK, message="Verification code sent.", challengeId=発行されたUUID, maskedEmail="y****@company.com", expiresInSeconds=300
            //         notificationService.sendOtpEmail(email, "123456", 5)が呼ばれたことを検証
            properties.getFeatures().setSignupEmailVerification(true);
            UUID challengeId = UUID.randomUUID();
            when(otpChallengeService.create(OtpPurpose.SIGNUP_EMAIL_VERIFICATION,
                    "public-signup", "yamada@company.com"))
                    .thenReturn(new OtpChallengeService.IssuedOtp(challengeId, "123456",
                            300, "y****@company.com", "yamada@company.com"));

            OtpChallengeResponse response = service.startSignupEmailVerification("yamada@company.com");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getMessage()).isEqualTo("Verification code sent.");
            assertThat(response.getChallengeId()).isEqualTo(challengeId);
            assertThat(response.getMaskedEmail()).isEqualTo("y****@company.com");
            assertThat(response.getExpiresInSeconds()).isEqualTo(300L);
            verify(notificationService).sendOtpEmail("yamada@company.com", "123456", 5);
        }
    }

    @Nested
    @DisplayName("POST /company/signup/email-verification/resend のテスト")
    class ResendSignupEmailVerificationTest {

        @Test
        @DisplayName("UT-CSFS-003: isSignupEmailVerification=falseの場合、NOT_FOUND例外をthrowする")
        void resendSignupEmailVerification_featureDisabled_throwsNotFound() {
            // Input: challengeId=UUID
            //        properties.getFeatures().isSignupEmailVerification()=false
            // Output: CustomException(NOT_FOUND, MSG_NOT_FOUND)がthrowされること
            //         otpChallengeService.resendが一度も呼ばれないこと
            UUID challengeId = UUID.randomUUID();
            assertThatThrownBy(() -> service.resendSignupEmailVerification(challengeId))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(ResponseConstants.NOT_FOUND));
            verifyNoInteractions(otpChallengeService);
        }

        @Test
        @DisplayName("UT-CSFS-004: challengeIdが存在しない場合、otpChallengeServiceの例外が伝播する")
        void resendSignupEmailVerification_challengeNotFound_propagatesException() {
            // Input: challengeId=未知のUUID
            //        properties.getFeatures().isSignupEmailVerification()=true
            //        otpChallengeService.resend(challengeId, SIGNUP_EMAIL_VERIFICATION)がCustomException(INVALID_OR_EXPIRED_TOKEN)をthrowするようモック
            // Output: CustomExceptionがそのままthrowされること
            //         notificationService.sendOtpEmailが呼ばれないこと
            properties.getFeatures().setSignupEmailVerification(true);
            UUID challengeId = UUID.randomUUID();
            CustomException expected = new CustomException(
                    ResponseConstants.INVALID_OR_EXPIRED_TOKEN, ResponseConstants.MSG_INVALID_OR_EXPIRED_TOKEN);
            when(otpChallengeService.resend(challengeId, OtpPurpose.SIGNUP_EMAIL_VERIFICATION))
                    .thenThrow(expected);
            assertThatThrownBy(() -> service.resendSignupEmailVerification(challengeId)).isSameAs(expected);
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("UT-CSFS-005: 正常にOTPが再発行された場合、メール送信され正常なレスポンスが返る")
        void resendSignupEmailVerification_valid_returnsOtpChallengeResponse() {
            // Input: challengeId=UUID
            //        properties.getFeatures().isSignupEmailVerification()=true
            //        otpChallengeService.resend(challengeId, SIGNUP_EMAIL_VERIFICATION)がIssuedOtp(challengeId=同UUID, code="654321", email="yamada@company.com", maskedEmail="y****@company.com", expiresInSeconds=300)を返すようモック
            // Output: statusCode=OK, message="Verification code sent.", challengeId=同UUID, maskedEmail="y****@company.com"
            //         notificationService.sendOtpEmail(email, "654321", 分数)が呼ばれたことを検証
            properties.getFeatures().setSignupEmailVerification(true);
            UUID challengeId = UUID.randomUUID();
            when(otpChallengeService.resend(challengeId, OtpPurpose.SIGNUP_EMAIL_VERIFICATION))
                    .thenReturn(new OtpChallengeService.IssuedOtp(challengeId, "654321",
                            300, "y****@company.com", "yamada@company.com"));
            OtpChallengeResponse response = service.resendSignupEmailVerification(challengeId);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getMessage()).isEqualTo("Verification code sent.");
            assertThat(response.getChallengeId()).isEqualTo(challengeId);
            assertThat(response.getMaskedEmail()).isEqualTo("y****@company.com");
            verify(notificationService).sendOtpEmail("yamada@company.com", "654321", 5);
        }
    }

    @Nested
    @DisplayName("POST /company/signup/email-verification/verify のテスト")
    class VerifySignupEmailTest {

        @Test
        @DisplayName("UT-CSFS-006: isSignupEmailVerification=falseの場合、NOT_FOUND例外をthrowする")
        void verifySignupEmail_featureDisabled_throwsNotFound() {
            // Input: challengeId=UUID, code="123456"
            //        properties.getFeatures().isSignupEmailVerification()=false
            // Output: CustomException(NOT_FOUND, MSG_NOT_FOUND)がthrowされること
            //         otpChallengeService.verifyが一度も呼ばれないこと
            assertThatThrownBy(() -> service.verifySignupEmail(UUID.randomUUID(), "123456"))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(ResponseConstants.NOT_FOUND));
            verifyNoInteractions(otpChallengeService);
        }

        @Test
        @DisplayName("UT-CSFS-007: codeが不正な場合、otpChallengeServiceの例外が伝播する")
        void verifySignupEmail_invalidCode_propagatesException() {
            // Input: challengeId=UUID, code="000000"
            //        properties.getFeatures().isSignupEmailVerification()=true
            //        otpChallengeService.verify(challengeId, code)がCustomException(INVALID_OR_EXPIRED_TOKEN)をthrowするようモック
            // Output: CustomExceptionがそのままthrowされること
            properties.getFeatures().setSignupEmailVerification(true);
            UUID challengeId = UUID.randomUUID();
            CustomException expected = new CustomException(
                    ResponseConstants.INVALID_OR_EXPIRED_TOKEN, ResponseConstants.MSG_INVALID_OR_EXPIRED_TOKEN);
            when(otpChallengeService.verify(challengeId, "000000")).thenThrow(expected);
            assertThatThrownBy(() -> service.verifySignupEmail(challengeId, "000000")).isSameAs(expected);
        }

        @Test
        @DisplayName("UT-CSFS-008: 検証成功したOTPのpurposeがSIGNUP_EMAIL_VERIFICATION以外の場合、INVALID_OR_EXPIRED_TOKEN例外をthrowする")
        void verifySignupEmail_purposeMismatch_throwsInvalidToken() {
            // Input: challengeId=UUID, code="123456"
            //        properties.getFeatures().isSignupEmailVerification()=true
            //        otpChallengeService.verify(challengeId, code)がOtpData(purpose=MASTER_ACCOUNT_SETUPなど別purpose)を返すようモック
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN, MSG_INVALID_OR_EXPIRED_TOKEN)がthrowされること
            properties.getFeatures().setSignupEmailVerification(true);
            UUID challengeId = UUID.randomUUID();
            when(otpChallengeService.verify(challengeId, "123456"))
                    .thenReturn(new OtpChallengeService.OtpData(OtpPurpose.OPERATING_LOGIN,
                            "company", "yamada@company.com", "hash", 0, 1, 0, false));
            assertThatThrownBy(() -> service.verifySignupEmail(challengeId, "123456"))
                    .isInstanceOfSatisfying(CustomException.class, exception -> {
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
                        assertThat(exception).hasMessage(ResponseConstants.MSG_INVALID_OR_EXPIRED_TOKEN);
                    });
        }

        @Test
        @DisplayName("UT-CSFS-009: 正常にメール検証が完了した場合、成功レスポンスが返る")
        void verifySignupEmail_valid_returnsSuccess() {
            // Input: challengeId=UUID, code="123456"
            //        properties.getFeatures().isSignupEmailVerification()=true
            //        otpChallengeService.verify(challengeId, code)がOtpData(purpose=SIGNUP_EMAIL_VERIFICATION)を返すようモック
            // Output: statusCode=OK, message="Email verified."
            properties.getFeatures().setSignupEmailVerification(true);
            UUID challengeId = UUID.randomUUID();
            when(otpChallengeService.verify(challengeId, "123456"))
                    .thenReturn(new OtpChallengeService.OtpData(OtpPurpose.SIGNUP_EMAIL_VERIFICATION,
                            "public-signup", "yamada@company.com", "hash", 0, 1, 0, true));
            var response = service.verifySignupEmail(challengeId, "123456");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getMessage()).isEqualTo("Email verified.");
        }
    }

    @Nested
    @DisplayName("getRevisionDraft（GET /company/signup/revision）のテスト")
    class GetRevisionDraftTest {

        @Test
        @DisplayName("UT-CSFS-010: isRejectedSignupRevision=falseの場合、NOT_FOUND例外をthrowする")
        void getRevisionDraft_featureDisabled_throwsNotFound() {
            // Input: sessionToken="session-token-123"
            //        properties.getFeatures().isRejectedSignupRevision()=false
            // Output: CustomException(NOT_FOUND, MSG_NOT_FOUND)がthrowされること
            //         actionLinkService.requireSessionが一度も呼ばれないこと
            assertThatThrownBy(() -> service.getRevisionDraft(SESSION_TOKEN))
                    .isInstanceOfSatisfying(CustomException.class, exception -> {
                        assertThat(exception.getErrorCode()).isEqualTo(ResponseConstants.NOT_FOUND);
                        assertThat(exception).hasMessage(ResponseConstants.MSG_NOT_FOUND);
                    });
            verifyNoInteractions(actionLinkService);
        }

        @Test
        @DisplayName("UT-CSFS-011: sessionTokenが無効な場合、actionLinkServiceの例外が伝播する")
        void getRevisionDraft_invalidSession_propagatesException() {
            // Input: sessionToken="invalid-token"
            //        properties.getFeatures().isRejectedSignupRevision()=true
            //        actionLinkService.requireSession(sessionToken, REJECTED_SIGNUP_REVISION)がCustomException(INVALID_OR_EXPIRED_TOKEN)をthrowするようモック
            // Output: CustomExceptionがそのままthrowされること
            //         companyRepository.findByIdが呼ばれないこと
            properties.getFeatures().setRejectedSignupRevision(true);
            CustomException expected = new CustomException(
                    ResponseConstants.INVALID_OR_EXPIRED_TOKEN, ResponseConstants.MSG_INVALID_OR_EXPIRED_TOKEN);
            when(actionLinkService.requireSession("invalid-token",
                    SecurityActionPurpose.REJECTED_SIGNUP_REVISION)).thenThrow(expected);
            assertThatThrownBy(() -> service.getRevisionDraft("invalid-token")).isSameAs(expected);
            verifyNoInteractions(companyRepository);
        }

        @Test
        @DisplayName("UT-CSFS-012: sessionに紐づくcompanyIdの会社が存在しない場合、INVALID_OR_EXPIRED_TOKEN例外をthrowする")
        void getRevisionDraft_companyNotFound_throwsInvalidToken() {
            // Input: sessionToken="session-token-123"
            //        properties.getFeatures().isRejectedSignupRevision()=true
            //        actionLinkService.requireSessionがActionSessionData(subjectId=999L)を返すようモック
            //        companyRepository.findById(999L)がOptional.emptyを返すようモック
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN, MSG_INVALID_OR_EXPIRED_TOKEN)がthrowされること
            properties.getFeatures().setRejectedSignupRevision(true);
            when(actionLinkService.requireSession(SESSION_TOKEN,
                    SecurityActionPurpose.REJECTED_SIGNUP_REVISION)).thenReturn(actionSession(999));
            when(companyRepository.findById(999)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getRevisionDraft(SESSION_TOKEN))
                    .isInstanceOfSatisfying(CustomException.class, exception -> {
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN);
                        assertThat(exception).hasMessage(ResponseConstants.MSG_INVALID_OR_EXPIRED_TOKEN);
                    });
        }

        @Test
        @DisplayName("UT-CSFS-013: 会社のステータスがREJECTED以外の場合、INVALID_OR_EXPIRED_TOKEN例外をthrowする")
        void getRevisionDraft_companyNotRejected_throwsInvalidToken() {
            // Input: sessionToken="session-token-123"
            //        properties.getFeatures().isRejectedSignupRevision()=true
            //        actionLinkService.requireSessionがActionSessionData(subjectId=5678L)を返すようモック
            //        companyRepository.findById(5678L)がCompanyStatus.PENDINGのcompanyを返すようモック
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN, MSG_INVALID_OR_EXPIRED_TOKEN)がthrowされること
            properties.getFeatures().setRejectedSignupRevision(true);
            when(actionLinkService.requireSession(SESSION_TOKEN,
                    SecurityActionPurpose.REJECTED_SIGNUP_REVISION)).thenReturn(actionSession((int) COMPANY_ID));
            when(companyRepository.findById(COMPANY_ID))
                    .thenReturn(Optional.of(companyWithStatus(CompanyStatus.PENDING)));
            assertThatThrownBy(() -> service.getRevisionDraft(SESSION_TOKEN))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN));
        }

        @Test
        @DisplayName("UT-CSFS-014: 正常にREJECTED状態の会社情報が取得できた場合、PartiCompanyRegisterRequestに変換されて返る")
        void getRevisionDraft_valid_returnsMappedRequest() {
            // Input: sessionToken="session-token-123"
            //        properties.getFeatures().isRejectedSignupRevision()=true
            //        actionLinkService.requireSessionがActionSessionData(subjectId=5678L)を返すようモック
            //        companyRepository.findById(5678L)がCompanyStatus.REJECTEDのcompany（nameJa="株式会社サンプル", email="yamada@company.com", lastRejectionReason="書類不備", ...）を返すようモック
            // Output: PartiCompanyRegisterRequestのcorporateName="株式会社サンプル", email="yamada@company.com", rejectionReason="書類不備"等が元companyの値と一致すること
            properties.getFeatures().setRejectedSignupRevision(true);
            ParticipatingCompany company = rejectedCompany();
            when(actionLinkService.requireSession(SESSION_TOKEN,
                    SecurityActionPurpose.REJECTED_SIGNUP_REVISION)).thenReturn(actionSession((int) COMPANY_ID));
            when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));

            PartiCompanyRegisterRequest result = service.getRevisionDraft(SESSION_TOKEN);

            assertThat(result.getCorporateNumber()).isEqualTo(company.getCorpNumber());
            assertThat(result.getCorporateName()).isEqualTo(company.getNameJa());
            assertThat(result.getCorporateNameKana()).isEqualTo(company.getNameFurigana());
            assertThat(result.getCompanyNameEnglish()).isEqualTo(company.getNameEn());
            assertThat(result.getCorporatePhoneNumber()).isEqualTo(company.getPhoneNumber());
            assertThat(result.getEmail()).isEqualTo(company.getEmail());
            assertThat(result.getPostalCode()).isEqualTo(company.getPostalCode());
            assertThat(result.getLastName()).isEqualTo(company.getRepLastName());
            assertThat(result.getMethodology()).isEqualTo(company.getRequestedMethodologies());
            assertThat(result.getRejectionReason()).isEqualTo(company.getLastRejectionReason());
            assertThat(result.getRejectedAt()).isEqualTo(company.getRejectedAt());
        }
    }

    @Nested
    @DisplayName("PUT /company/signup/revision のテスト")
    class ReviseRejectedSignupTest {

        @Test
        @DisplayName("UT-CSFS-015: isRejectedSignupRevision=falseの場合、NOT_FOUND例外をthrowする")
        void reviseRejectedSignup_featureDisabled_throwsNotFound() {
            // Input: sessionToken="session-token-123", request={corporateNumber:"1234567890123", email:"yamada@company.com", ...}
            //        properties.getFeatures().isRejectedSignupRevision()=false
            // Output: CustomException(NOT_FOUND, MSG_NOT_FOUND)がthrowされること
            //         companyRepository.save、signupValidatorが呼ばれないこと
            assertThatThrownBy(() -> service.reviseRejectedSignup(SESSION_TOKEN, validRequest()))
                    .isInstanceOfSatisfying(CustomException.class, exception -> {
                        assertThat(exception.getErrorCode()).isEqualTo(ResponseConstants.NOT_FOUND);
                        assertThat(exception).hasMessage(ResponseConstants.MSG_NOT_FOUND);
                    });
            verifyNoInteractions(companyRepository, signupValidator);
        }

        @Test
        @DisplayName("UT-CSFS-016: sessionに紐づくcompanyIdの会社が存在しない場合、INVALID_OR_EXPIRED_TOKEN例外をthrowする")
        void reviseRejectedSignup_companyNotFound_throwsInvalidToken() {
            // Input: sessionToken="session-token-123", request={corporateNumber:"1234567890123", email:"yamada@company.com", ...}
            //        properties.getFeatures().isRejectedSignupRevision()=true
            //        actionLinkService.requireSessionがActionSessionData(subjectId=999L)を返すようモック
            //        companyRepository.findById(999L)がOptional.emptyを返すようモック
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN, MSG_INVALID_OR_EXPIRED_TOKEN)がthrowされること
            properties.getFeatures().setRejectedSignupRevision(true);
            when(actionLinkService.requireSession(SESSION_TOKEN,
                    SecurityActionPurpose.REJECTED_SIGNUP_REVISION)).thenReturn(actionSession(999));
            when(companyRepository.findById(999)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.reviseRejectedSignup(SESSION_TOKEN, validRequest()))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN));
        }

        @Test
        @DisplayName("UT-CSFS-017: 会社のステータスがREJECTED以外の場合、INVALID_OR_EXPIRED_TOKEN例外をthrowする")
        void reviseRejectedSignup_companyNotRejected_throwsInvalidToken() {
            // Input: sessionToken="session-token-123", request={corporateNumber:"1234567890123", email:"yamada@company.com", ...}
            //        properties.getFeatures().isRejectedSignupRevision()=true
            //        actionLinkService.requireSessionがActionSessionData(subjectId=5678L)を返すようモック
            //        companyRepository.findById(5678L)がCompanyStatus.PENDINGのcompanyを返すようモック
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN, MSG_INVALID_OR_EXPIRED_TOKEN)がthrowされること
            properties.getFeatures().setRejectedSignupRevision(true);
            when(actionLinkService.requireSession(SESSION_TOKEN,
                    SecurityActionPurpose.REJECTED_SIGNUP_REVISION)).thenReturn(actionSession((int) COMPANY_ID));
            when(companyRepository.findById(COMPANY_ID))
                    .thenReturn(Optional.of(companyWithStatus(CompanyStatus.PENDING)));
            assertThatThrownBy(() -> service.reviseRejectedSignup(SESSION_TOKEN, validRequest()))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(ResponseConstants.INVALID_OR_EXPIRED_TOKEN));
        }

        @Test
        @DisplayName("UT-CSFS-018: 入力された法人番号が自分以外の会社で既に使用されている場合、CONFLICT例外をthrowする")
        void reviseRejectedSignup_corpNumberUsedByOther_throwsConflict() {
            // Input: sessionToken="session-token-123", request={corporateNumber:"9999999999999", email:"yamada@company.com", ...}
            //        properties.getFeatures().isRejectedSignupRevision()=true
            //        actionLinkService.requireSessionがActionSessionData(subjectId=5678L)を返すようモック
            //        companyRepository.findById(5678L)がCompanyStatus.REJECTEDのcompany(id=5678L)を返すようモック
            //        companyRepository.findByCorpNumber("9999999999999")がid=9999Lの別会社を返すようモック
            // Output: CustomException(CONFLICT, MSG_CORP_NUMBER_ALREADY_USED)がthrowされること
            //         companyRepository.save、signupValidatorが呼ばれないこと
            properties.getFeatures().setRejectedSignupRevision(true);
            PartiCompanyRegisterRequest request = validRequest();
            request.setCorporateNumber("9999999999999");
            stubRejectedCompany(rejectedCompany());
            ParticipatingCompany otherCompany = new ParticipatingCompany();
            otherCompany.setId(9999);
            when(companyRepository.findByCorpNumber("9999999999999"))
                    .thenReturn(Optional.of(otherCompany));

            assertThatThrownBy(() -> service.reviseRejectedSignup(SESSION_TOKEN, request))
                    .isInstanceOfSatisfying(CustomException.class, exception -> {
                        assertThat(exception.getErrorCode()).isEqualTo(ResponseConstants.CONFLICT);
                        assertThat(exception).hasMessage(ResponseConstants.MSG_CORP_NUMBER_ALREADY_USED);
                    });
            verify(companyRepository, never()).save(any());
            verifyNoInteractions(signupValidator);
        }

        @Test
        @DisplayName("UT-CSFS-019: 入力されたメールアドレスが自分以外の会社で既に使用されている場合、CONFLICT例外をthrowする")
        void reviseRejectedSignup_emailUsedByOther_throwsConflict() {
            // Input: sessionToken="session-token-123", request={corporateNumber:"1234567890123", email:"other@company.com", ...}
            //        properties.getFeatures().isRejectedSignupRevision()=true
            //        actionLinkService.requireSessionがActionSessionData(subjectId=5678L)を返すようモック
            //        companyRepository.findById(5678L)がCompanyStatus.REJECTEDのcompany(id=5678L, corpNumber="1234567890123")を返すようモック
            //        companyRepository.findByCorpNumber("1234567890123")が同id=5678Lのcompanyを返すようモック（重複なし）
            //        companyRepository.findByEmailIgnoreCase("other@company.com")がid=4321Lの別会社を返すようモック
            // Output: CustomException(CONFLICT, MSG_EMAIL_ALREADY_USED)がthrowされること
            //         companyRepository.save、signupValidatorが呼ばれないこと
            properties.getFeatures().setRejectedSignupRevision(true);
            PartiCompanyRegisterRequest request = validRequest();
            request.setEmail("other@company.com");
            ParticipatingCompany company = rejectedCompany();
            stubRejectedCompany(company);
            when(companyRepository.findByCorpNumber(request.getCorporateNumber()))
                    .thenReturn(Optional.of(company));
            ParticipatingCompany otherCompany = new ParticipatingCompany();
            otherCompany.setId(4321);
            when(companyRepository.findByEmailIgnoreCase("other@company.com"))
                    .thenReturn(Optional.of(otherCompany));

            assertThatThrownBy(() -> service.reviseRejectedSignup(SESSION_TOKEN, request))
                    .isInstanceOfSatisfying(CustomException.class, exception -> {
                        assertThat(exception.getErrorCode()).isEqualTo(ResponseConstants.CONFLICT);
                        assertThat(exception).hasMessage(ResponseConstants.MSG_EMAIL_ALREADY_USED);
                    });
            verify(companyRepository, never()).save(any());
            verifyNoInteractions(signupValidator);
        }

        @Test
        @DisplayName("UT-CSFS-020: メール変更ありかつisSignupEmailVerification=trueでchallengeIdがnullの場合、OTP_REQUIRED例外をthrowする")
        void reviseRejectedSignup_emailChangedNoChallengeId_throwsOtpRequired() {
            // Input: sessionToken="session-token-123", request={corporateNumber:"1234567890123", email:"new-email@company.com", emailVerificationChallengeId:null, ...}
            //        properties.getFeatures().isRejectedSignupRevision()=true, isSignupEmailVerification()=true
            //        companyRepository.findById(5678L)がCompanyStatus.REJECTEDのcompany(id=5678L, email="old-email@company.com", corpNumber="1234567890123")を返すようモック
            //        companyRepository.findByCorpNumber/findByEmailIgnoreCaseが重複なし（Optional.empty）を返すようモック
            // Output: CustomException(OTP_REQUIRED, MSG_OTP_REQUIRED)がthrowされること
            //         otpChallengeService.requireVerified、signupValidator、companyRepository.saveが呼ばれないこと
            properties.getFeatures().setRejectedSignupRevision(true);
            properties.getFeatures().setSignupEmailVerification(true);
            ParticipatingCompany company = rejectedCompany();
            company.setEmail("old-email@company.com");
            stubRejectedCompany(company);
            PartiCompanyRegisterRequest request = validRequest();
            request.setEmail("new-email@company.com");

            assertThatThrownBy(() -> service.reviseRejectedSignup(SESSION_TOKEN, request))
                    .isInstanceOfSatisfying(CustomException.class, exception -> {
                        assertThat(exception.getErrorCode()).isEqualTo(ResponseConstants.OTP_REQUIRED);
                        assertThat(exception).hasMessage(ResponseConstants.MSG_OTP_REQUIRED);
                    });
            verifyNoInteractions(otpChallengeService, signupValidator);
            verify(companyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UT-CSFS-021: メール変更なしかつemailVerifiedAtがnullの場合でisSignupEmailVerification=trueの場合、OTP検証が要求される")
        void reviseRejectedSignup_emailUnchangedButNotVerified_requiresOtp() {
            // Input: sessionToken="session-token-123", request={corporateNumber:"1234567890123", email:"same-email@company.com", emailVerificationChallengeId:UUID, ...}
            //        properties.getFeatures().isRejectedSignupRevision()=true, isSignupEmailVerification()=true
            //        companyRepository.findById(5678L)がCompanyStatus.REJECTEDのcompany(id=5678L, email="same-email@company.com", emailVerifiedAt=null, corpNumber="1234567890123")を返すようモック
            //        companyRepository.findByCorpNumber/findByEmailIgnoreCaseが重複なしを返すようモック
            //        otpChallengeService.requireVerifiedが例外をthrowしないようモック
            //        signupValidator.validateCompanyInfoが例外をthrowしないようモック
            // Output: BaseResponse成功（message="Registration revision submitted."）
            //         otpChallengeService.requireVerified(challengeId, SIGNUP_EMAIL_VERIFICATION, email)が呼ばれたことを検証
            //         companyRepository.saveに渡されたcompanyのemailVerifiedAtが設定されていることを検証
            properties.getFeatures().setRejectedSignupRevision(true);
            properties.getFeatures().setSignupEmailVerification(true);
            ParticipatingCompany company = rejectedCompany();
            company.setEmail("same-email@company.com");
            stubRejectedCompany(company);
            PartiCompanyRegisterRequest request = validRequest();
            request.setEmail("same-email@company.com");
            UUID challengeId = UUID.randomUUID();
            request.setEmailVerificationChallengeId(challengeId);

            var response = service.reviseRejectedSignup(SESSION_TOKEN, request);

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getMessage()).isEqualTo("Registration revision submitted.");
            verify(otpChallengeService).requireVerified(
                    challengeId, OtpPurpose.SIGNUP_EMAIL_VERIFICATION, request.getEmail());
            verify(companyRepository).save(company);
            assertThat(company.getEmailVerifiedAt()).isNotNull();
        }

        @Test
        @DisplayName("UT-CSFS-022: signupValidatorがバリデーション例外をthrowした場合、更新処理が行われず例外が伝播する")
        void reviseRejectedSignup_validationFails_propagatesExceptionWithoutSaving() {
            // Input: sessionToken="session-token-123", request={corporateNumber:"", email:"yamada@company.com", ...}（不正値）
            //        properties.getFeatures().isRejectedSignupRevision()=true, isSignupEmailVerification()=false
            //        companyRepository.findById(5678L)がCompanyStatus.REJECTEDのcompany(id=5678L)を返すようモック
            //        companyRepository.findByCorpNumber/findByEmailIgnoreCaseが重複なしを返すようモック
            //        signupValidator.validateCompanyInfoがCustomException(VALIDATION_ERROR)をthrowするようモック
            // Output: CustomExceptionがそのままthrowされること
            //         companyRepository.save、actionLinkService.completeが呼ばれないこと
            properties.getFeatures().setRejectedSignupRevision(true);
            ParticipatingCompany company = rejectedCompany();
            stubRejectedCompany(company);
            PartiCompanyRegisterRequest request = validRequest();
            request.setCorporateNumber("");
            CustomException expected = new CustomException(
                    ResponseConstants.VALIDATION_ERROR, ResponseConstants.MSG_VALIDATION_ERROR);
            doThrow(expected).when(signupValidator).validateCompanyInfo(request);

            assertThatThrownBy(() -> service.reviseRejectedSignup(SESSION_TOKEN, request))
                    .isSameAs(expected);
            verify(companyRepository, never()).save(any());
            verify(actionLinkService, never()).complete(anyString(), any());
        }

        @Test
        @DisplayName("UT-CSFS-023: isSignupEmailVerification=falseの場合、OTP検証なしで正常に再申請が完了する")
        void reviseRejectedSignup_emailVerificationFeatureOff_updatesWithoutOtp() {
            // Input: sessionToken="session-token-123", request={corporateNumber:"1234567890123", corporateName:"株式会社サンプル改", email:"yamada@company.com", emailVerificationChallengeId:null, methodology:["太陽光発電"], activityInformation:"lorem ipsum", ...}
            //        properties.getFeatures().isRejectedSignupRevision()=true, isSignupEmailVerification()=false
            //        companyRepository.findById(5678L)がCompanyStatus.REJECTEDのcompany(id=5678L, email="yamada@company.com", lastRejectionReason="書類不備")を返すようモック
            //        companyRepository.findByCorpNumber/findByEmailIgnoreCaseが重複なしを返すようモック
            //        signupValidator.validateCompanyInfoが例外をthrowしないようモック
            // Output: BaseResponse成功（message="Registration revision submitted."）
            //         companyRepository.saveに渡されたcompanyのcompanyStatus=PENDING, isActive=false, lastRejectionReason=null, rejectedAt=nullであることを検証
            //         actionLinkService.complete(sessionToken, REJECTED_SIGNUP_REVISION)が呼ばれたことを検証
            //         otpChallengeService.requireVerified、consumeVerifiedが呼ばれないことを検証
            properties.getFeatures().setRejectedSignupRevision(true);
            ParticipatingCompany company = rejectedCompany();
            stubRejectedCompany(company);
            PartiCompanyRegisterRequest request = validRequest();

            var response = service.reviseRejectedSignup(SESSION_TOKEN, request);

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getMessage()).isEqualTo("Registration revision submitted.");
            assertThat(company.getCompanyStatus()).isEqualTo(CompanyStatus.PENDING);
            assertThat(company.getIsActive()).isFalse();
            assertThat(company.getLastRejectionReason()).isNull();
            assertThat(company.getRejectedAt()).isNull();
            verify(actionLinkService).complete(
                    SESSION_TOKEN, SecurityActionPurpose.REJECTED_SIGNUP_REVISION);
            verifyNoInteractions(otpChallengeService);
        }

        @Test
        @DisplayName("UT-CSFS-024: メール変更ありかつOTP検証済みの場合、emailVerifiedAtが更新されcommit後にconsumeVerifiedが呼ばれる")
        void reviseRejectedSignup_emailChangedAndVerified_consumesOtpAfterCommit() {
            // Input: sessionToken="session-token-123", request={corporateNumber:"1234567890123", email:"new-email@company.com", emailVerificationChallengeId:UUID, ...}
            //        properties.getFeatures().isRejectedSignupRevision()=true, isSignupEmailVerification()=true
            //        companyRepository.findById(5678L)がCompanyStatus.REJECTEDのcompany(id=5678L, email="old-email@company.com", emailVerifiedAt=過去日時)を返すようモック
            //        companyRepository.findByCorpNumber/findByEmailIgnoreCaseが重複なしを返すようモック
            //        otpChallengeService.requireVerifiedが例外をthrowしないようモック
            //        signupValidator.validateCompanyInfoが例外をthrowしないようモック
            //        TransactionSynchronizationManager.isSynchronizationActive()=trueの状態
            // Output: BaseResponse成功（message="Registration revision submitted."）
            //         companyRepository.saveに渡されたcompanyのemailVerifiedAtが新しい日時に更新されていることを検証
            //         otpChallengeService.consumeVerified(challengeId, SIGNUP_EMAIL_VERIFICATION)がトランザクションコミット後に呼ばれたことを検証
            properties.getFeatures().setRejectedSignupRevision(true);
            properties.getFeatures().setSignupEmailVerification(true);
            ParticipatingCompany company = rejectedCompany();
            company.setEmail("old-email@company.com");
            LocalDateTime previouslyVerified = LocalDateTime.of(2020, 1, 1, 0, 0);
            company.setEmailVerifiedAt(previouslyVerified);
            stubRejectedCompany(company);
            PartiCompanyRegisterRequest request = validRequest();
            request.setEmail("new-email@company.com");
            UUID challengeId = UUID.randomUUID();
            request.setEmailVerificationChallengeId(challengeId);
            TransactionSynchronizationManager.initSynchronization();

            var response = service.reviseRejectedSignup(SESSION_TOKEN, request);

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(company.getEmailVerifiedAt()).isAfter(previouslyVerified);
            verify(otpChallengeService).requireVerified(
                    challengeId, OtpPurpose.SIGNUP_EMAIL_VERIFICATION, request.getEmail());
            verify(otpChallengeService, never()).consumeVerified(
                    challengeId, OtpPurpose.SIGNUP_EMAIL_VERIFICATION);
            TransactionSynchronization synchronization =
                    TransactionSynchronizationManager.getSynchronizations().get(0);
            synchronization.afterCommit();
            verify(otpChallengeService).consumeVerified(
                    challengeId, OtpPurpose.SIGNUP_EMAIL_VERIFICATION);
        }
    }
}