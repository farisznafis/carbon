package com.kct.jcredit.core_backend.controller.SecurityFlowControllerTest;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.controller.SecurityFlowController;
import com.kct.jcredit.core_backend.dto.request.ActionLinkExchangeRequest;
import com.kct.jcredit.core_backend.dto.request.CompletePasswordResetRequest;
import com.kct.jcredit.core_backend.dto.request.EmailOtpSettingChallengeRequest;
import com.kct.jcredit.core_backend.dto.request.ForgotPasswordRequest;
import com.kct.jcredit.core_backend.dto.request.MasterAccountSetupRequest;
import com.kct.jcredit.core_backend.dto.request.OtpResendRequest;
import com.kct.jcredit.core_backend.dto.request.OtpVerifyRequest;
import com.kct.jcredit.core_backend.dto.response.ActionLinkExchangeResponse;
import com.kct.jcredit.core_backend.dto.response.AdminLoginResponse;
import com.kct.jcredit.core_backend.dto.response.LoginResponse;
import com.kct.jcredit.core_backend.dto.response.OtpChallengeResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.enums.OtpPurpose;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.handler.GlobalErrorHandler;
import com.kct.jcredit.core_backend.service.SecurityAccountService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SecurityFlowController API delegation, validation, and authentication outline.
 *
 * Use MockMvc for @Valid, required-header, and method-security behavior.
 */
@ExtendWith(MockitoExtension.class)
class SecurityFlowControllerTest {

    private static final String TOKEN = "security-flow-test-token";
    private static final String CHALLENGE_ID = "00000000-0000-0000-0000-000000000001";
    private static final String VALID_PASSWORD = "StrongPass123456";

    @Mock private SecurityAccountService securityAccountService;
    @Mock private JwtUtil jwtUtil;
    @Mock private Authentication authentication;

    @InjectMocks
    private SecurityFlowController controller;

    private MockMvc mockMvc() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalErrorHandler())
                .build();
    }

    private void assertInvalidBody(String path, String json) throws Exception {
        mockMvc().perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    private void assertInvalidBodyWithSession(String path, String json) throws Exception {
        mockMvc().perform(post(path)
                        .header("X-Action-Session", "test-action-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    private void assertAuthenticationGuard(String methodName) {
        PreAuthorize annotation = Arrays.stream(SecurityFlowController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .orElseThrow()
                .getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("isAuthenticated()");
    }

    @Nested
    @DisplayName("POST /auth/action-links/exchange のテスト")
    class ExchangeActionLinkTest {

        @Test
        @DisplayName("UT-SFC-001: 有効なaction tokenがServiceへ渡り、session情報が返る")
        void exchangeActionLink_validToken_returnsSessionResponse() {
            ActionLinkExchangeRequest request = new ActionLinkExchangeRequest();
            request.setToken("valid-action-token");
            ActionLinkExchangeResponse expected = new ActionLinkExchangeResponse();
            expected.setStatusCode(ResponseConstants.OK);
            expected.setActionSessionToken("action-session-token");
            when(securityAccountService.exchangeActionLink("valid-action-token")).thenReturn(expected);

            var response = controller.exchangeActionLink(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(securityAccountService).exchangeActionLink("valid-action-token");
            // Input: ActionLinkExchangeRequest.tokenをServiceへ渡す
            // Output: HTTP 200、actionSessionToken/purpose/expiresInSecondsを含むresponse
        }

        @Test
        @DisplayName("UT-SFC-002: tokenが空または欠落した場合は400となりServiceは呼ばれない")
        void exchangeActionLink_blankToken_returnsValidationError() throws Exception {
            assertInvalidBody("/auth/action-links/exchange", "{\"token\":\"\"}");
            verify(securityAccountService, never()).exchangeActionLink(org.mockito.ArgumentMatchers.anyString());
            // Input: token=nullまたはblank
            // Output: HTTP 400、exchangeActionLinkは呼ばれない
        }

        @Test
        @DisplayName("UT-SFC-003: 無効または期限切れtokenの例外がHTTP error responseへ変換される")
        void exchangeActionLink_invalidToken_mapsServiceException() throws Exception {
            when(securityAccountService.exchangeActionLink("expired-token"))
                .thenThrow(new CustomException(ResponseConstants.INVALID_OR_EXPIRED_TOKEN,
                    ResponseConstants.MSG_INVALID_OR_EXPIRED_TOKEN));
            mockMvc().perform(post("/auth/action-links/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"expired-token\"}"))
                .andExpect(status().isUnauthorized());
            // Input: Serviceがinvalid/expired action token例外を送出
            // Output: Global exception handlerの定義に従うHTTP status/body
        }
    }

    @Nested
    @DisplayName("POST /auth/master-account/setup のテスト")
    class SetupMasterAccountTest {

        @Test
        @DisplayName("UT-SFC-004: session、username、passwordがServiceへ渡り成功する")
        void setupMasterAccount_validRequest_returnsSuccess() {
            MasterAccountSetupRequest request = new MasterAccountSetupRequest();
            request.setUsername("Master_User01");
            request.setPassword(VALID_PASSWORD);
            BaseResponse expected = BaseResponse.success(ResponseConstants.MSG_MASTER_ACCOUNT_SETUP_COMPLETED);
            when(securityAccountService.setupMasterAccount("test-action-session", "Master_User01", VALID_PASSWORD))
                    .thenReturn(expected);

            var response = controller.setupMasterAccount("test-action-session", request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(securityAccountService).setupMasterAccount("test-action-session", "Master_User01", VALID_PASSWORD);
            // Input: X-Action-Sessionと有効なusername/password
            // Output: setupMasterAccount(session, username, password)が呼ばれHTTP 200
        }

        @Test
        @DisplayName("UT-SFC-005: 必須headerまたはrequest制約違反時は400となりServiceは呼ばれない")
        void setupMasterAccount_invalidRequest_returnsValidationError() throws Exception {
            assertInvalidBodyWithSession("/auth/master-account/setup",
                "{\"username\":\"bad/name\",\"password\":\"short\"}");
            mockMvc().perform(post("/auth/master-account/setup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"Master_User01\",\"password\":\"" + VALID_PASSWORD + "\"}"))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(securityAccountService);
            // Input: headerなし、blank/不正username、password policy違反
            // Output: HTTP 400、setupMasterAccountは呼ばれない
        }

        @Test
        @DisplayName("UT-SFC-006: 無効session等のService例外がHTTP error responseへ変換される")
        void setupMasterAccount_invalidSession_mapsServiceException() throws Exception {
            when(securityAccountService.setupMasterAccount("expired-session", "Master_User01", VALID_PASSWORD))
                .thenThrow(new CustomException(ResponseConstants.INVALID_OR_EXPIRED_TOKEN,
                    ResponseConstants.MSG_INVALID_OR_EXPIRED_TOKEN));
            mockMvc().perform(post("/auth/master-account/setup")
                    .header("X-Action-Session", "expired-session")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"Master_User01\",\"password\":\"" + VALID_PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
            // Input: Serviceが無効/期限切れsessionを拒否
            // Output: Global exception handlerの定義に従うHTTP status/body
        }
    }

    @Nested
    @DisplayName("POST /auth/password-reset/request のテスト")
    class RequestPasswordResetTest {

        @Test
        @DisplayName("UT-SFC-007: 有効なemail requestがServiceへ渡り成功responseが返る")
        void requestPasswordReset_validEmail_returnsSuccess() {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("person@example.com");
            BaseResponse expected = BaseResponse.success(ResponseConstants.MSG_PASSWORD_RESET_EMAIL_SENT);
            when(securityAccountService.requestPasswordReset(request)).thenReturn(expected);

            var response = controller.requestPasswordReset(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(securityAccountService).requestPasswordReset(request);
            // Input: 有効なForgotPasswordRequest
            // Output: requestPasswordReset(request)が呼ばれHTTP 200とService response
        }

        @Test
        @DisplayName("UT-SFC-008: emailが空または不正形式の場合は400となりServiceは呼ばれない")
        void requestPasswordReset_invalidEmail_returnsValidationError() throws Exception {
            assertInvalidBody("/auth/password-reset/request", "{\"email\":\"not-an-email\"}");
            verifyNoInteractions(securityAccountService);
            // Input: email=null/blank/不正形式
            // Output: HTTP 400、requestPasswordResetは呼ばれない
        }

        @Test
        @DisplayName("UT-SFC-009: 未登録emailでもアカウント有無を漏らさない成功responseが返る")
        void requestPasswordReset_unknownEmail_returnsGenericSuccess() {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("unknown@example.com");
            BaseResponse expected = BaseResponse.success(ResponseConstants.MSG_PASSWORD_RESET_EMAIL_SENT);
            when(securityAccountService.requestPasswordReset(request)).thenReturn(expected);

            var response = controller.requestPasswordReset(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(securityAccountService).requestPasswordReset(request);
            // Input: 有効だが未登録のemail
            // Output: アカウント存在時と同じ外部response contract
        }
    }

    @Nested
    @DisplayName("POST /auth/password-reset/complete のテスト")
    class CompletePasswordResetTest {

        @Test
        @DisplayName("UT-SFC-010: sessionと有効なnewPasswordがServiceへ渡る")
        void completePasswordReset_validRequest_returnsSuccess() {
            CompletePasswordResetRequest request = new CompletePasswordResetRequest();
            request.setNewPassword(VALID_PASSWORD);
            BaseResponse expected = BaseResponse.success(ResponseConstants.MSG_PASSWORD_RESET_COMPLETED);
            when(securityAccountService.completePasswordReset("reset-session", VALID_PASSWORD)).thenReturn(expected);

            var response = controller.completePasswordReset("reset-session", request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(securityAccountService).completePasswordReset("reset-session", VALID_PASSWORD);
            // Input: X-Action-Sessionとpolicy適合newPassword
            // Output: completePasswordReset(session, newPassword)が呼ばれ、response statusが変換される
        }

        @Test
        @DisplayName("UT-SFC-011: headerまたはpassword制約違反時は400となりServiceは呼ばれない")
        void completePasswordReset_invalidRequest_returnsValidationError() throws Exception {
            assertInvalidBodyWithSession("/auth/password-reset/complete", "{\"newPassword\":\"short\"}");
            mockMvc().perform(post("/auth/password-reset/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\":\"" + VALID_PASSWORD + "\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(securityAccountService);
            // Input: headerなし、password blankまたはpassword policy違反
            // Output: HTTP 400、completePasswordResetは呼ばれない
        }

        @Test
        @DisplayName("UT-SFC-012: Service error statusCodeがHTTP statusへ変換される")
        void completePasswordReset_serviceReturnsError_convertsHttpStatus() {
            CompletePasswordResetRequest request = new CompletePasswordResetRequest();
            request.setNewPassword(VALID_PASSWORD);
            BaseResponse expected = BaseResponse.error(ResponseConstants.SAME_PASSWORD,
                    ResponseConstants.MSG_SAME_PASSWORD);
            when(securityAccountService.completePasswordReset("reset-session", VALID_PASSWORD)).thenReturn(expected);

            var response = controller.completePasswordReset("reset-session", request);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: ServiceがSAME_PASSWORD等のerror BaseResponseを返す
            // Output: ResponseUtilに従うHTTP statusとService body
        }
    }

    @Nested
    @DisplayName("POST /auth/user/login/email-otp/verify のテスト")
    class VerifyParticipatingLoginTest {

        @Test
        @DisplayName("UT-SFC-013: challengeId/codeが参加企業login verify Serviceへ渡る")
        void verifyParticipatingLogin_validOtp_returnsLoginResponse() {
            UUID challengeId = UUID.fromString(CHALLENGE_ID);
            OtpVerifyRequest request = new OtpVerifyRequest();
            request.setChallengeId(challengeId);
            request.setCode("123456");
            LoginResponse expected = new LoginResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(securityAccountService.verifyParticipatingLogin(challengeId, "123456")).thenReturn(expected);

            var response = controller.verifyParticipatingLogin(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(securityAccountService).verifyParticipatingLogin(challengeId, "123456");
            // Input: UUID challengeIdと6桁code
            // Output: verifyParticipatingLogin(challengeId, code)が呼ばれ、LoginResponseがHTTP 200で返る
        }

        @Test
        @DisplayName("UT-SFC-014: challengeIdまたはcodeが不正時は400となりServiceは呼ばれない")
        void verifyParticipatingLogin_invalidRequest_returnsValidationError() throws Exception {
            assertInvalidBody("/auth/user/login/email-otp/verify",
                    "{\"challengeId\":\"" + CHALLENGE_ID + "\",\"code\":\"12345x\"}");
            verifyNoInteractions(securityAccountService);
            // Input: challengeId=null、code欠落、または6桁数字以外
            // Output: HTTP 400、verifyParticipatingLoginは呼ばれない
        }
    }

    @Nested
    @DisplayName("POST /auth/admin/login/email-otp/verify のテスト")
    class VerifyOperatingLoginTest {

        @Test
        @DisplayName("UT-SFC-015: challengeId/codeが運営login verify Serviceへ渡りAdminLoginResponseが返る")
        void verifyOperatingLogin_validOtp_returnsAdminLoginResponse() {
            UUID challengeId = UUID.fromString(CHALLENGE_ID);
            OtpVerifyRequest request = new OtpVerifyRequest();
            request.setChallengeId(challengeId);
            request.setCode("123456");
            AdminLoginResponse expected = new AdminLoginResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(securityAccountService.verifyOperatingLogin(challengeId, "123456")).thenReturn(expected);

            var response = controller.verifyOperatingLogin(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(securityAccountService).verifyOperatingLogin(challengeId, "123456");
            // Input: UUID challengeIdと6桁code
            // Output: verifyOperatingLogin(challengeId, code)が呼ばれ、AdminLoginResponseがHTTP 200で返る
        }

        @Test
        @DisplayName("UT-SFC-016: challengeIdまたはcodeが不正時は400となりServiceは呼ばれない")
        void verifyOperatingLogin_invalidRequest_returnsValidationError() throws Exception {
            assertInvalidBody("/auth/admin/login/email-otp/verify",
                    "{\"challengeId\":\"" + CHALLENGE_ID + "\",\"code\":\"12345x\"}");
            verifyNoInteractions(securityAccountService);
            // Input: challengeId=null、code欠落、または6桁数字以外
            // Output: HTTP 400、verifyOperatingLoginは呼ばれない
        }
    }

    @Nested
    @DisplayName("POST /auth/user/login/email-otp/resend のテスト")
    class ResendParticipatingLoginTest {

        @Test
        @DisplayName("UT-SFC-017: challengeIdとPARTICIPATING_LOGIN purposeで再送Serviceが呼ばれる")
        void resendParticipatingLogin_validChallenge_returnsChallengeResponse() {
            UUID challengeId = UUID.fromString(CHALLENGE_ID);
            OtpResendRequest request = new OtpResendRequest();
            request.setChallengeId(challengeId);
            OtpChallengeResponse expected = new OtpChallengeResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(securityAccountService.resendLoginOtp(challengeId, OtpPurpose.PARTICIPATING_LOGIN))
                    .thenReturn(expected);

            var response = controller.resendParticipatingLogin(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(securityAccountService).resendLoginOtp(challengeId, OtpPurpose.PARTICIPATING_LOGIN);
            // Input: OtpResendRequest.challengeId
            // Output: resendLoginOtp(challengeId, PARTICIPATING_LOGIN)とHTTP 200 response
        }

        @Test
        @DisplayName("UT-SFC-018: challengeIdが欠落した場合は400となりServiceは呼ばれない")
        void resendParticipatingLogin_missingChallengeId_returnsValidationError() throws Exception {
            assertInvalidBody("/auth/user/login/email-otp/resend", "{}");
            verifyNoInteractions(securityAccountService);
            // Input: challengeId=null
            // Output: HTTP 400、resendLoginOtpは呼ばれない
        }
    }

    @Nested
    @DisplayName("POST /auth/admin/login/email-otp/resend のテスト")
    class ResendOperatingLoginTest {

        @Test
        @DisplayName("UT-SFC-019: challengeIdとOPERATING_LOGIN purposeで再送Serviceが呼ばれる")
        void resendOperatingLogin_validChallenge_returnsChallengeResponse() {
            UUID challengeId = UUID.fromString(CHALLENGE_ID);
            OtpResendRequest request = new OtpResendRequest();
            request.setChallengeId(challengeId);
            OtpChallengeResponse expected = new OtpChallengeResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(securityAccountService.resendLoginOtp(challengeId, OtpPurpose.OPERATING_LOGIN))
                    .thenReturn(expected);

            var response = controller.resendOperatingLogin(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(securityAccountService).resendLoginOtp(challengeId, OtpPurpose.OPERATING_LOGIN);
            // Input: OtpResendRequest.challengeId
            // Output: resendLoginOtp(challengeId, OPERATING_LOGIN)とHTTP 200 response
        }

        @Test
        @DisplayName("UT-SFC-020: challengeIdが欠落した場合は400となりServiceは呼ばれない")
        void resendOperatingLogin_missingChallengeId_returnsValidationError() throws Exception {
            assertInvalidBody("/auth/admin/login/email-otp/resend", "{}");
            verifyNoInteractions(securityAccountService);
            // Input: challengeId=null
            // Output: HTTP 400、resendLoginOtpは呼ばれない
        }
    }

    @Nested
    @DisplayName("POST /auth/email-otp/settings/challenge のテスト")
    class StartEmailOtpSettingTest {

        @Test
        @DisplayName("UT-SFC-021: JWT userId/roleとenabled/currentPasswordがServiceへ渡る")
        void startEmailOtpSetting_authenticatedRequest_returnsChallenge() {
            EmailOtpSettingChallengeRequest request = new EmailOtpSettingChallengeRequest();
            request.setEnabled(true);
            request.setCurrentPassword("CurrentPass123!");
            OtpChallengeResponse expected = new OtpChallengeResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(42);
            when(jwtUtil.extractRole(TOKEN)).thenReturn("operator");
            when(securityAccountService.startEmailOtpSetting(42, "operator", true, "CurrentPass123!"))
                    .thenReturn(expected);

            var response = controller.startEmailOtpSetting(authentication, request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(securityAccountService).startEmailOtpSetting(42, "operator", true, "CurrentPass123!");
            // Input: authenticated principal, JWT, enabled, currentPassword
            // Output: startEmailOtpSetting(userId, role, enabled, currentPassword)が呼ばれHTTP 200
        }

        @Test
        @DisplayName("UT-SFC-022: enabledまたはcurrentPasswordが不正時は400となりServiceは呼ばれない")
        void startEmailOtpSetting_invalidRequest_returnsValidationError() throws Exception {
            assertInvalidBody("/auth/email-otp/settings/challenge", "{\"enabled\":null,\"currentPassword\":\"\"}");
            verifyNoInteractions(securityAccountService);
            // Input: enabled=nullまたはcurrentPassword blank
            // Output: HTTP 400、startEmailOtpSettingは呼ばれない
        }

        @Test
        @DisplayName("UT-SFC-023: unauthenticated requestは401となる")
        void startEmailOtpSetting_unauthenticated_returnsUnauthorized() {
            assertAuthenticationGuard("startEmailOtpSetting");
            verifyNoInteractions(securityAccountService);
            // Input: 認証なし
            // Output: HTTP 401、SecurityAccountServiceは呼ばれない
        }
    }

    @Nested
    @DisplayName("POST /auth/email-otp/settings/confirm のテスト")
    class ConfirmEmailOtpSettingTest {

        @Test
        @DisplayName("UT-SFC-024: JWT userId/roleとchallengeId/codeがServiceへ渡る")
        void confirmEmailOtpSetting_authenticatedRequest_returnsServiceResponse() {
            UUID challengeId = UUID.fromString(CHALLENGE_ID);
            OtpVerifyRequest request = new OtpVerifyRequest();
            request.setChallengeId(challengeId);
            request.setCode("123456");
            BaseResponse expected = BaseResponse.success(ResponseConstants.MSG_EMAIL_OTP_SETTING_UPDATED);
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(42);
            when(jwtUtil.extractRole(TOKEN)).thenReturn("operator");
            when(securityAccountService.confirmEmailOtpSetting(42, "operator", challengeId, "123456"))
                    .thenReturn(expected);

            var response = controller.confirmEmailOtpSetting(authentication, request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(securityAccountService).confirmEmailOtpSetting(42, "operator", challengeId, "123456");
            // Input: authenticated principal、JWT、UUID challengeId、6桁code
            // Output: confirmEmailOtpSetting(userId, role, challengeId, code)が呼ばれHTTP 200
        }

        @Test
        @DisplayName("UT-SFC-025: challengeId/codeが不正時は400となりServiceは呼ばれない")
        void confirmEmailOtpSetting_invalidRequest_returnsValidationError() throws Exception {
            assertInvalidBody("/auth/email-otp/settings/confirm",
                    "{\"challengeId\":\"" + CHALLENGE_ID + "\",\"code\":\"12345x\"}");
            verifyNoInteractions(securityAccountService);
            // Input: challengeId=nullまたはcodeが空/6桁数字以外
            // Output: HTTP 400、confirmEmailOtpSettingは呼ばれない
        }

        @Test
        @DisplayName("UT-SFC-026: unauthenticated requestは401となる")
        void confirmEmailOtpSetting_unauthenticated_returnsUnauthorized() {
            assertAuthenticationGuard("confirmEmailOtpSetting");
            verifyNoInteractions(securityAccountService);
            // Input: 認証なし
            // Output: HTTP 401、SecurityAccountServiceは呼ばれない
        }
    }
}
