package com.kct.jcredit.core_backend.controller.PartiCompanyControllerTest;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CompanySecurityFlowService test outline.
 * 対象メソッド: startSignupEmailVerification, resendSignupEmailVerification,
 *             verifySignupEmail, getRevisionDraft(getRejectedSignupRevision), reviseRejectedSignup
 */
class CompanySecurityFlowServiceTest {

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
        }

        @Test
        @DisplayName("UT-CSFS-004: challengeIdが存在しない場合、otpChallengeServiceの例外が伝播する")
        void resendSignupEmailVerification_challengeNotFound_propagatesException() {
            // Input: challengeId=未知のUUID
            //        properties.getFeatures().isSignupEmailVerification()=true
            //        otpChallengeService.resend(challengeId, SIGNUP_EMAIL_VERIFICATION)がCustomException(INVALID_OR_EXPIRED_TOKEN)をthrowするようモック
            // Output: CustomExceptionがそのままthrowされること
            //         notificationService.sendOtpEmailが呼ばれないこと
        }

        @Test
        @DisplayName("UT-CSFS-005: 正常にOTPが再発行された場合、メール送信され正常なレスポンスが返る")
        void resendSignupEmailVerification_valid_returnsOtpChallengeResponse() {
            // Input: challengeId=UUID
            //        properties.getFeatures().isSignupEmailVerification()=true
            //        otpChallengeService.resend(challengeId, SIGNUP_EMAIL_VERIFICATION)がIssuedOtp(challengeId=同UUID, code="654321", email="yamada@company.com", maskedEmail="y****@company.com", expiresInSeconds=300)を返すようモック
            // Output: statusCode=OK, message="Verification code sent.", challengeId=同UUID, maskedEmail="y****@company.com"
            //         notificationService.sendOtpEmail(email, "654321", 分数)が呼ばれたことを検証
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
        }

        @Test
        @DisplayName("UT-CSFS-007: codeが不正な場合、otpChallengeServiceの例外が伝播する")
        void verifySignupEmail_invalidCode_propagatesException() {
            // Input: challengeId=UUID, code="000000"
            //        properties.getFeatures().isSignupEmailVerification()=true
            //        otpChallengeService.verify(challengeId, code)がCustomException(INVALID_OR_EXPIRED_TOKEN)をthrowするようモック
            // Output: CustomExceptionがそのままthrowされること
        }

        @Test
        @DisplayName("UT-CSFS-008: 検証成功したOTPのpurposeがSIGNUP_EMAIL_VERIFICATION以外の場合、INVALID_OR_EXPIRED_TOKEN例外をthrowする")
        void verifySignupEmail_purposeMismatch_throwsInvalidToken() {
            // Input: challengeId=UUID, code="123456"
            //        properties.getFeatures().isSignupEmailVerification()=true
            //        otpChallengeService.verify(challengeId, code)がOtpData(purpose=MASTER_ACCOUNT_SETUPなど別purpose)を返すようモック
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN, MSG_INVALID_OR_EXPIRED_TOKEN)がthrowされること
        }

        @Test
        @DisplayName("UT-CSFS-009: 正常にメール検証が完了した場合、成功レスポンスが返る")
        void verifySignupEmail_valid_returnsSuccess() {
            // Input: challengeId=UUID, code="123456"
            //        properties.getFeatures().isSignupEmailVerification()=true
            //        otpChallengeService.verify(challengeId, code)がOtpData(purpose=SIGNUP_EMAIL_VERIFICATION)を返すようモック
            // Output: statusCode=OK, message="Email verified."
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
        }

        @Test
        @DisplayName("UT-CSFS-011: sessionTokenが無効な場合、actionLinkServiceの例外が伝播する")
        void getRevisionDraft_invalidSession_propagatesException() {
            // Input: sessionToken="invalid-token"
            //        properties.getFeatures().isRejectedSignupRevision()=true
            //        actionLinkService.requireSession(sessionToken, REJECTED_SIGNUP_REVISION)がCustomException(INVALID_OR_EXPIRED_TOKEN)をthrowするようモック
            // Output: CustomExceptionがそのままthrowされること
            //         companyRepository.findByIdが呼ばれないこと
        }

        @Test
        @DisplayName("UT-CSFS-012: sessionに紐づくcompanyIdの会社が存在しない場合、INVALID_OR_EXPIRED_TOKEN例外をthrowする")
        void getRevisionDraft_companyNotFound_throwsInvalidToken() {
            // Input: sessionToken="session-token-123"
            //        properties.getFeatures().isRejectedSignupRevision()=true
            //        actionLinkService.requireSessionがActionSessionData(subjectId=999L)を返すようモック
            //        companyRepository.findById(999L)がOptional.emptyを返すようモック
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN, MSG_INVALID_OR_EXPIRED_TOKEN)がthrowされること
        }

        @Test
        @DisplayName("UT-CSFS-013: 会社のステータスがREJECTED以外の場合、INVALID_OR_EXPIRED_TOKEN例外をthrowする")
        void getRevisionDraft_companyNotRejected_throwsInvalidToken() {
            // Input: sessionToken="session-token-123"
            //        properties.getFeatures().isRejectedSignupRevision()=true
            //        actionLinkService.requireSessionがActionSessionData(subjectId=5678L)を返すようモック
            //        companyRepository.findById(5678L)がCompanyStatus.PENDINGのcompanyを返すようモック
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN, MSG_INVALID_OR_EXPIRED_TOKEN)がthrowされること
        }

        @Test
        @DisplayName("UT-CSFS-014: 正常にREJECTED状態の会社情報が取得できた場合、PartiCompanyRegisterRequestに変換されて返る")
        void getRevisionDraft_valid_returnsMappedRequest() {
            // Input: sessionToken="session-token-123"
            //        properties.getFeatures().isRejectedSignupRevision()=true
            //        actionLinkService.requireSessionがActionSessionData(subjectId=5678L)を返すようモック
            //        companyRepository.findById(5678L)がCompanyStatus.REJECTEDのcompany（nameJa="株式会社サンプル", email="yamada@company.com", lastRejectionReason="書類不備", ...）を返すようモック
            // Output: PartiCompanyRegisterRequestのcorporateName="株式会社サンプル", email="yamada@company.com", rejectionReason="書類不備"等が元companyの値と一致すること
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
        }

        @Test
        @DisplayName("UT-CSFS-016: sessionに紐づくcompanyIdの会社が存在しない場合、INVALID_OR_EXPIRED_TOKEN例外をthrowする")
        void reviseRejectedSignup_companyNotFound_throwsInvalidToken() {
            // Input: sessionToken="session-token-123", request={corporateNumber:"1234567890123", email:"yamada@company.com", ...}
            //        properties.getFeatures().isRejectedSignupRevision()=true
            //        actionLinkService.requireSessionがActionSessionData(subjectId=999L)を返すようモック
            //        companyRepository.findById(999L)がOptional.emptyを返すようモック
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN, MSG_INVALID_OR_EXPIRED_TOKEN)がthrowされること
        }

        @Test
        @DisplayName("UT-CSFS-017: 会社のステータスがREJECTED以外の場合、INVALID_OR_EXPIRED_TOKEN例外をthrowする")
        void reviseRejectedSignup_companyNotRejected_throwsInvalidToken() {
            // Input: sessionToken="session-token-123", request={corporateNumber:"1234567890123", email:"yamada@company.com", ...}
            //        properties.getFeatures().isRejectedSignupRevision()=true
            //        actionLinkService.requireSessionがActionSessionData(subjectId=5678L)を返すようモック
            //        companyRepository.findById(5678L)がCompanyStatus.PENDINGのcompanyを返すようモック
            // Output: CustomException(INVALID_OR_EXPIRED_TOKEN, MSG_INVALID_OR_EXPIRED_TOKEN)がthrowされること
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
        }
    }
}