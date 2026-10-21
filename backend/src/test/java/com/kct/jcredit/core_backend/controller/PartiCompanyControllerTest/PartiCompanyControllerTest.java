package com.kct.jcredit.core_backend.controller.PartiCompanyControllerTest;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.controller.PartiCompanyController;
import com.kct.jcredit.core_backend.dto.request.ApproveDeletionRequest;
import com.kct.jcredit.core_backend.dto.request.CreateDeletionRequest;
import com.kct.jcredit.core_backend.dto.request.EmailOtpStartRequest;
import com.kct.jcredit.core_backend.dto.request.OtpResendRequest;
import com.kct.jcredit.core_backend.dto.request.OtpVerifyRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyInfoValidateRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyMethodologyValidateRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyRegisterRequest;
import com.kct.jcredit.core_backend.dto.request.RejectDeletionRequest;
import com.kct.jcredit.core_backend.dto.request.UpdatePartiCompanyRequest;
import com.kct.jcredit.core_backend.dto.response.DeletionRequestResponse;
import com.kct.jcredit.core_backend.dto.response.OtpChallengeResponse;
import com.kct.jcredit.core_backend.dto.response.UpdatePartiCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.PartiCompanyResponseDetails;
import com.kct.jcredit.core_backend.service.CompanyDeletionService;
import com.kct.jcredit.core_backend.service.CompanySecurityFlowService;
import com.kct.jcredit.core_backend.service.PartiCompanyRegisterService;
import com.kct.jcredit.core_backend.service.PartiCompanyService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * PartiCompanyController API and delegation test outline.
 *
 * Validation cases should use MockMvc so @Valid and required-header behavior
 * are exercised through Spring MVC rather than bypassed by direct method calls.
 */
@ExtendWith(MockitoExtension.class)
class PartiCompanyControllerTest {

    private static final String TOKEN = "parti-company-test-token";

    @Mock private PartiCompanyService companyService;
    @Mock private PartiCompanyRegisterService registerService;
    @Mock private JwtUtil jwtUtil;
    @Mock private CompanyDeletionService companyDeletionService;
    @Mock private CompanySecurityFlowService companySecurityFlowService;
    @Mock private Authentication authentication;

    @InjectMocks
    private PartiCompanyController controller;

    private MockMvc mockMvc() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(controller).setValidator(validator).build();
    }

    @Nested
    @DisplayName("PATCH /company のテスト")
    class UpdateCompanyTest {

        @Test
        @DisplayName("UT-PCC-001: JWTのcompanyIdと有効なリクエストがServiceへ渡り、更新結果が返る")
        void updateCompany_validRequest_returnsServiceResponse() {
            UpdatePartiCompanyRequest request = new UpdatePartiCompanyRequest();
            request.setNameJa("Sample Company");
            UpdatePartiCompanyResponse expected = new UpdatePartiCompanyResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP-001");
            when(companyService.updateCompany("COMP-001", request)).thenReturn(expected);

            var response = controller.updateCompany(authentication, request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(companyService).updateCompany("COMP-001", request);
            // Input: credentialsにJWT、jwtUtil.extractCompanyId(JWT)="COMP-001"
            //        companyService.updateCompany("COMP-001", request)がOKレスポンスを返す
            // Output: HTTP 200、レスポンスがServiceの結果と一致、同じrequestが渡される
        }

        @Test
        @DisplayName("UT-PCC-002: ServiceのエラーstatusCodeに対応するHTTPステータスが返る")
        void updateCompany_serviceReturnsError_convertsHttpStatus() {
            UpdatePartiCompanyRequest request = new UpdatePartiCompanyRequest();
            UpdatePartiCompanyResponse expected = new UpdatePartiCompanyResponse();
            expected.setStatusCode(ResponseConstants.NOT_FOUND);
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP-404");
            when(companyService.updateCompany("COMP-404", request)).thenReturn(expected);

            var response = controller.updateCompany(authentication, request);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: ServiceがNOT_FOUNDを返す
            // Output: HTTP 404、レスポンスボディはServiceの結果と一致
        }

        @Test
        @DisplayName("UT-PCC-003: リクエストの制約違反時は400となりServiceは呼ばれない")
        void updateCompany_invalidRequest_returnsValidationError() throws Exception {
            mockMvc().perform(patch("/company")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nameJa\":\"" + "x".repeat(256) + "\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(companyService);
            // Input: @Size上限を超えるnameJa等
            // Output: HTTP 400、companyService.updateCompanyは呼ばれない
        }
    }

    @Nested
    @DisplayName("POST /company/signup のテスト")
    class SignupTest {

        @Test
        @DisplayName("UT-PCC-004: 有効な登録申請がServiceへ渡り、201が返る")
        void signup_validRequest_returnsCreated() {
            PartiCompanyRegisterRequest request = new PartiCompanyRegisterRequest();
            PartiCompanyResponseDetails expected = new PartiCompanyResponseDetails(
                    "CREATED", "登録申請が完了しました。", "REG-001", "PENDING");
            when(registerService.register(request)).thenReturn(expected);

            var response = controller.signup(request);

            assertThat(response.getStatusCode().value()).isEqualTo(201);
            assertThat(response.getBody()).isSameAs(expected);
            verify(registerService).register(request);
            // Input: 有効なPartiCompanyRegisterRequest
            // Output: HTTP 201、registerService.registerに同じrequestが渡り、戻り値が返る
        }

        @Test
        @DisplayName("UT-PCC-005: 必須項目が欠落した登録申請は400となりServiceは呼ばれない")
        void signup_invalidRequest_returnsValidationError() throws Exception {
            mockMvc().perform(post("/company/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(registerService);
            // Input: 必須項目を欠落させたrequest
            // Output: HTTP 400、registerService.registerは呼ばれない
        }
    }

    @Nested
    @DisplayName("POST /company/signup/email-verification/start のテスト")
    class StartSignupEmailVerificationTest {

        @Test
        @DisplayName("UT-PCC-006: 有効なemailがSecurityFlowServiceへ渡り、チャレンジ結果が返る")
        void startSignupEmailVerification_validEmail_returnsChallenge() {
            EmailOtpStartRequest request = new EmailOtpStartRequest();
            request.setEmail("user@example.com");
            OtpChallengeResponse expected = new OtpChallengeResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(companySecurityFlowService.startSignupEmailVerification("user@example.com")).thenReturn(expected);

            var response = controller.startSignupEmailVerification(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(companySecurityFlowService).startSignupEmailVerification("user@example.com");
            // Input: email="user@example.com"
            // Output: HTTP 200、startSignupEmailVerificationにemailが渡り、結果が返る
        }

        @Test
        @DisplayName("UT-PCC-007: emailが空または不正な形式の場合は400となりServiceは呼ばれない")
        void startSignupEmailVerification_invalidEmail_returnsValidationError() throws Exception {
            mockMvc().perform(post("/company/signup/email-verification/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"invalid-email\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(companySecurityFlowService);
            // Input: 空またはValidEmail違反のemail
            // Output: HTTP 400、SecurityFlowServiceは呼ばれない
        }
    }

    @Nested
    @DisplayName("POST /company/signup/email-verification/resend のテスト")
    class ResendSignupEmailVerificationTest {

        @Test
        @DisplayName("UT-PCC-008: challengeIdがSecurityFlowServiceへ渡り、再送結果が返る")
        void resendSignupEmailVerification_validRequest_returnsChallenge() {
            UUID challengeId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            OtpResendRequest request = new OtpResendRequest();
            request.setChallengeId(challengeId);
            OtpChallengeResponse expected = new OtpChallengeResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(companySecurityFlowService.resendSignupEmailVerification(challengeId)).thenReturn(expected);

            var response = controller.resendSignupEmailVerification(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(companySecurityFlowService).resendSignupEmailVerification(challengeId);
            // Input: challengeIdを持つrequest
            // Output: HTTP 200、resendSignupEmailVerificationに同じchallengeIdが渡る
        }

        @Test
        @DisplayName("UT-PCC-009: challengeIdが欠落した場合は400となりServiceは呼ばれない")
        void resendSignupEmailVerification_missingChallengeId_returnsValidationError() throws Exception {
            mockMvc().perform(post("/company/signup/email-verification/resend")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(companySecurityFlowService);
            // Input: challengeId=null
            // Output: HTTP 400、SecurityFlowServiceは呼ばれない
        }
    }

    @Nested
    @DisplayName("POST /company/signup/email-verification/verify のテスト")
    class VerifySignupEmailTest {

        @Test
        @DisplayName("UT-PCC-010: challengeIdと6桁codeがSecurityFlowServiceへ渡る")
        void verifySignupEmail_validRequest_returnsServiceResponse() {
            UUID challengeId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            OtpVerifyRequest request = new OtpVerifyRequest();
            request.setChallengeId(challengeId);
            request.setCode("123456");
            BaseResponse expected = BaseResponse.success("Email verified.");
            when(companySecurityFlowService.verifySignupEmail(challengeId, "123456")).thenReturn(expected);

            var response = controller.verifySignupEmail(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(companySecurityFlowService).verifySignupEmail(challengeId, "123456");
            // Input: challengeIdとcode="123456"
            // Output: HTTP 200、verifySignupEmailに両値が渡り、BaseResponseが返る
        }

        @Test
        @DisplayName("UT-PCC-011: challengeIdまたは6桁codeが不正な場合は400となる")
        void verifySignupEmail_invalidRequest_returnsValidationError() throws Exception {
            mockMvc().perform(post("/company/signup/email-verification/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"12345x\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(companySecurityFlowService);
            // Input: challengeId=nullまたはcodeが空/6桁数字以外
            // Output: HTTP 400、SecurityFlowServiceは呼ばれない
        }
    }

    @Nested
    @DisplayName("GET /company/signup/revision のテスト")
    class GetRejectedSignupRevisionTest {

        @Test
        @DisplayName("UT-PCC-012: X-Action-SessionがServiceへ渡り、下書きが返る")
        void getRejectedSignupRevision_validSession_returnsDraft() {
            PartiCompanyRegisterRequest expected = new PartiCompanyRegisterRequest();
            when(companySecurityFlowService.getRevisionDraft("action-session")).thenReturn(expected);

            var response = controller.getRejectedSignupRevision("action-session");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(companySecurityFlowService).getRevisionDraft("action-session");
            // Input: X-Action-Session="action-session"
            // Output: HTTP 200、getRevisionDraftに同じtokenが渡り、requestが返る
        }

        @Test
        @DisplayName("UT-PCC-013: X-Action-Sessionが欠落した場合は400となる")
        void getRejectedSignupRevision_missingSession_returnsBadRequest() throws Exception {
            mockMvc().perform(get("/company/signup/revision"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(companySecurityFlowService);
            // Input: 必須ヘッダーなし
            // Output: HTTP 400、getRevisionDraftは呼ばれない
        }
    }

    @Nested
    @DisplayName("PUT /company/signup/revision のテスト")
    class ReviseRejectedSignupTest {

        @Test
        @DisplayName("UT-PCC-014: sessionと修正申請がServiceへ渡り、再申請結果が返る")
        void reviseRejectedSignup_validRequest_returnsServiceResponse() {
            PartiCompanyRegisterRequest request = new PartiCompanyRegisterRequest();
            BaseResponse expected = BaseResponse.success("申請内容を再提出しました。");
            when(companySecurityFlowService.reviseRejectedSignup("action-session", request)).thenReturn(expected);

            var response = controller.reviseRejectedSignup("action-session", request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(companySecurityFlowService).reviseRejectedSignup("action-session", request);
            // Input: X-Action-Sessionと有効なPartiCompanyRegisterRequest
            // Output: HTTP 200、同じsession/requestがServiceへ渡り、BaseResponseが返る
        }

        @Test
        @DisplayName("UT-PCC-015: 修正申請の必須項目違反時は400となりServiceは呼ばれない")
        void reviseRejectedSignup_invalidRequest_returnsValidationError() throws Exception {
            mockMvc().perform(put("/company/signup/revision")
                            .header("X-Action-Session", "action-session")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(companySecurityFlowService);
            // Input: 必須項目を欠落させたrequest
            // Output: HTTP 400、reviseRejectedSignupは呼ばれない
        }

        @Test
        @DisplayName("UT-PCC-016: X-Action-Sessionが欠落した場合は400となる")
        void reviseRejectedSignup_missingSession_returnsBadRequest() throws Exception {
            mockMvc().perform(put("/company/signup/revision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(companySecurityFlowService);
            // Input: 必須ヘッダーなし
            // Output: HTTP 400、reviseRejectedSignupは呼ばれない
        }
    }

    @Nested
    @DisplayName("POST /company/signup/company-info/validate のテスト")
    class ValidateSignupCompanyInfoTest {

        @Test
        @DisplayName("UT-PCC-017: 有効な企業情報が検証Serviceへ渡り、成功レスポンスが返る")
        void validateSignupCompanyInfo_validRequest_returnsSuccess() {
            PartiCompanyInfoValidateRequest request = new PartiCompanyInfoValidateRequest();
            var response = controller.validateSignupCompanyInfo(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getBody().getMessage()).isEqualTo(ResponseConstants.MSG_VALIDATION_SUCCESS);
            verify(registerService).validateCompanyInfo(request);
            // Input: 有効なPartiCompanyInfoValidateRequest
            // Output: HTTP 200、registerService.validateCompanyInfoにrequestが渡り、成功BaseResponse
        }

        @Test
        @DisplayName("UT-PCC-018: 企業情報が制約違反の場合は400となりServiceは呼ばれない")
        void validateSignupCompanyInfo_invalidRequest_returnsValidationError() throws Exception {
            mockMvc().perform(post("/company/signup/company-info/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(registerService);
            // Input: 法人番号、email等の必須項目が欠落したrequest
            // Output: HTTP 400、validateCompanyInfoは呼ばれない
        }
    }

    @Nested
    @DisplayName("POST /company/signup/methodology/validate のテスト")
    class ValidateSignupMethodologyTest {

        @Test
        @DisplayName("UT-PCC-019: 有効な方法論情報で成功レスポンスが返る")
        void validateSignupMethodology_validRequest_returnsSuccess() {
            PartiCompanyMethodologyValidateRequest request = new PartiCompanyMethodologyValidateRequest();
            request.setMethodology(java.util.List.of("METHODOLOGY-001"));
            request.setActivityInformation("活動内容");

            var response = controller.validateSignupMethodology(request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getBody().getMessage()).isEqualTo(ResponseConstants.MSG_VALIDATION_SUCCESS);
            verifyNoInteractions(registerService, companySecurityFlowService, companyService, companyDeletionService);
            // Input: activityInformation等の必須項目を満たすrequest
            // Output: HTTP 200、statusCode=OK、検証成功message
            //         このcontroller methodはServiceを呼ばない
        }

        @Test
        @DisplayName("UT-PCC-020: 必須項目が欠落した場合は400となる")
        void validateSignupMethodology_invalidRequest_returnsValidationError() throws Exception {
            mockMvc().perform(post("/company/signup/methodology/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(registerService, companySecurityFlowService, companyService, companyDeletionService);
            // Input: @NotBlank項目を欠落させたrequest
            // Output: HTTP 400
        }
    }

    @Nested
    @DisplayName("POST /company/{companyId}/deletion-request のテスト")
    class CreateDeletionRequestTest {

        @Test
        @DisplayName("UT-PCC-021: JWT usernameとcompanyId・reasonがServiceへ渡り、成功結果が返る")
        void createDeletionRequest_validRequest_returnsServiceResponse() {
            CreateDeletionRequest request = new CreateDeletionRequest();
            request.setReason("契約終了");
            DeletionRequestResponse expected = new DeletionRequestResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn("master001");
            when(companyDeletionService.createDeletionRequest("COMP-001", "master001", "契約終了"))
                    .thenReturn(expected);

            var response = controller.createDeletionRequest(authentication, "COMP-001", request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(companyDeletionService).createDeletionRequest("COMP-001", "master001", "契約終了");
            // Input: credentialsにJWT、extractUsername(JWT)="master001"、reasonを持つrequest
            // Output: HTTP 200、createDeletionRequest(companyId, "master001", reason)が呼ばれ、結果が返る
        }

        @Test
        @DisplayName("UT-PCC-022: ServiceがNOT_FOUNDを返した場合、HTTP 404に変換される")
        void createDeletionRequest_serviceReturnsNotFound_convertsHttpStatus() {
            CreateDeletionRequest request = new CreateDeletionRequest();
            request.setReason("契約終了");
            DeletionRequestResponse expected = new DeletionRequestResponse();
            expected.setStatusCode(ResponseConstants.NOT_FOUND);
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn("master001");
            when(companyDeletionService.createDeletionRequest("COMP-404", "master001", "契約終了"))
                    .thenReturn(expected);

            var response = controller.createDeletionRequest(authentication, "COMP-404", request);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: ServiceがstatusCode=NOT_FOUNDを返す
            // Output: HTTP 404、レスポンスボディはServiceの結果と一致
        }

        @Test
        @DisplayName("UT-PCC-023: reason未指定時の挙動を確認する")
        void createDeletionRequest_missingReason_passesThroughCurrentContract() {
            CreateDeletionRequest request = new CreateDeletionRequest();
            DeletionRequestResponse expected = new DeletionRequestResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn("master001");
            when(companyDeletionService.createDeletionRequest("COMP-001", "master001", null))
                    .thenReturn(expected);

            var response = controller.createDeletionRequest(authentication, "COMP-001", request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(companyDeletionService).createDeletionRequest("COMP-001", "master001", null);
            // CreateDeletionRequestに現在Bean Validation制約はない
            // Output: 現状の契約どおりrequest.getReason()がServiceへ渡る
        }
    }

    @Nested
    @DisplayName("POST /company/{companyId}/deletion-approve のテスト")
    class ApproveDeletionRequestTest {

        @Test
        @DisplayName("UT-PCC-024: JWT usernameとcompanyIdがServiceへ渡り、承認結果が返る")
        void approveDeletionRequest_validRequest_returnsServiceResponse() {
            ApproveDeletionRequest request = new ApproveDeletionRequest();
            DeletionRequestResponse expected = new DeletionRequestResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn("admin001");
            when(companyDeletionService.approveDeletionRequest("COMP-001", "admin001")).thenReturn(expected);

            var response = controller.approveDeletionRequest(authentication, "COMP-001", request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(companyDeletionService).approveDeletionRequest("COMP-001", "admin001");
            // Input: credentialsにJWT、extractUsername(JWT)="admin001"
            // Output: HTTP 200、approveDeletionRequest(companyId, "admin001")が呼ばれる
            //         request bodyはservice引数に含まれない
        }

        @Test
        @DisplayName("UT-PCC-025: ServiceがNOT_FOUNDを返した場合、HTTP 404に変換される")
        void approveDeletionRequest_serviceReturnsNotFound_convertsHttpStatus() {
            ApproveDeletionRequest request = new ApproveDeletionRequest();
            DeletionRequestResponse expected = new DeletionRequestResponse();
            expected.setStatusCode(ResponseConstants.NOT_FOUND);
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn("admin001");
            when(companyDeletionService.approveDeletionRequest("COMP-404", "admin001")).thenReturn(expected);

            var response = controller.approveDeletionRequest(authentication, "COMP-404", request);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: ServiceがstatusCode=NOT_FOUNDを返す
            // Output: HTTP 404、レスポンスボディはServiceの結果と一致
        }
    }

    @Nested
    @DisplayName("POST /company/{companyId}/deletion-reject のテスト")
    class RejectDeletionRequestTest {

        @Test
        @DisplayName("UT-PCC-026: JWT username・companyId・rejectionReasonがServiceへ渡る")
        void rejectDeletionRequest_validRequest_returnsServiceResponse() {
            RejectDeletionRequest request = new RejectDeletionRequest();
            request.setRejectionReason("書類不備");
            DeletionRequestResponse expected = new DeletionRequestResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn("admin001");
            when(companyDeletionService.rejectDeletionRequest("COMP-001", "admin001", "書類不備"))
                    .thenReturn(expected);

            var response = controller.rejectDeletionRequest(authentication, "COMP-001", request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(companyDeletionService).rejectDeletionRequest("COMP-001", "admin001", "書類不備");
            // Input: credentialsにJWT、extractUsername(JWT)="admin001"、rejectionReason
            // Output: HTTP 200、rejectDeletionRequest(companyId, "admin001", rejectionReason)が呼ばれる
        }

        @Test
        @DisplayName("UT-PCC-027: ServiceがNOT_FOUNDを返した場合、HTTP 404に変換される")
        void rejectDeletionRequest_serviceReturnsNotFound_convertsHttpStatus() {
            RejectDeletionRequest request = new RejectDeletionRequest();
            request.setRejectionReason("書類不備");
            DeletionRequestResponse expected = new DeletionRequestResponse();
            expected.setStatusCode(ResponseConstants.NOT_FOUND);
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn("admin001");
            when(companyDeletionService.rejectDeletionRequest("COMP-404", "admin001", "書類不備"))
                    .thenReturn(expected);

            var response = controller.rejectDeletionRequest(authentication, "COMP-404", request);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: ServiceがstatusCode=NOT_FOUNDを返す
            // Output: HTTP 404、レスポンスボディはServiceの結果と一致
        }

        @Test
        @DisplayName("UT-PCC-028: rejectionReasonが空または欠落した場合は400となりServiceは呼ばれない")
        void rejectDeletionRequest_blankReason_returnsValidationError() throws Exception {
            mockMvc().perform(post("/company/COMP-001/deletion-reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rejectionReason\":\"\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(companyDeletionService);
            // Input: rejectionReason=""またはnull
            // Output: HTTP 400、rejectDeletionRequestは呼ばれない
        }
    }
}
