package com.kct.jcredit.core_backend.controller.AdminCompanyControllerTest;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.controller.AdminCompanyController;
import com.kct.jcredit.core_backend.dto.request.ApproveReviewRequest;
import com.kct.jcredit.core_backend.dto.request.CompanyListQuery;
import com.kct.jcredit.core_backend.dto.request.CreateCompanySuspensionRequest;
import com.kct.jcredit.core_backend.dto.request.CreateOperatorUserRequest;
import com.kct.jcredit.core_backend.dto.request.RejectCompanyRequest;
import com.kct.jcredit.core_backend.dto.request.RejectReviewRequest;
import com.kct.jcredit.core_backend.dto.request.ResolveCompanySuspensionRequest;
import com.kct.jcredit.core_backend.dto.request.UpdateUserRequest;
import com.kct.jcredit.core_backend.dto.response.AdminCompanyDetailResponse;
import com.kct.jcredit.core_backend.dto.response.AdminCompanyListResponse;
import com.kct.jcredit.core_backend.dto.response.ApproveCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.CompanySuspensionListResponse;
import com.kct.jcredit.core_backend.dto.response.CompanySuspensionResponse;
import com.kct.jcredit.core_backend.dto.response.CreateUserResponse;
import com.kct.jcredit.core_backend.dto.response.ParticipationReviewDetailResponse;
import com.kct.jcredit.core_backend.dto.response.RejectCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.UpdateUserResponse;
import com.kct.jcredit.core_backend.dto.response.UserListResponse;
import com.kct.jcredit.core_backend.dto.response.UserProfileResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.enums.SuspensionReasonCode;
import com.kct.jcredit.core_backend.exception.ResourceNotFoundException;
import com.kct.jcredit.core_backend.service.AdminCompanyQueryService;
import com.kct.jcredit.core_backend.service.CompanyApprovalService;
import com.kct.jcredit.core_backend.service.CompanyLifecycleService;
import com.kct.jcredit.core_backend.service.DocuSignService;
import com.kct.jcredit.core_backend.service.OperatorUserService;
import com.kct.jcredit.core_backend.service.ParticipationReviewService;
import com.kct.jcredit.core_backend.service.SecurityAccountService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminCompanyControllerTest {

    private static final String TOKEN = "test-jwt";

    @Mock private OperatorUserService operatorUserService;
    @Mock private CompanyApprovalService companyApprovalService;
    @Mock private AdminCompanyQueryService adminCompanyQueryService;
    @Mock private CompanyLifecycleService companyLifecycleService;
    @Mock private ParticipationReviewService reviewService;
    @Mock private SecurityAccountService securityAccountService;
    @Mock private JwtUtil jwtUtil;
    @Mock private DocuSignService docuSignService;
    @Mock private Authentication authentication;

    @InjectMocks
    private AdminCompanyController controller;

    private MockMvc mockMvc() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(controller).setValidator(validator).build();
    }

    private void assertInvalidBody(String path, String json) throws Exception {
        mockMvc().perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    private static ApproveReviewRequest validApproveReviewRequest() {
        ApproveReviewRequest request = new ApproveReviewRequest();
        request.setReviewerInkan("印");
        request.setReviewerName("山田太郎");
        request.setIsSolarContractDoc(true);
        request.setIsPcsSpecDoc(true);
        request.setIsBatterySpecDoc(true);
        request.setIsOperationStartDoc(true);
        request.setIsGenerationDataDoc(true);
        request.setIsReversePowerDoc(true);
        request.setIsInvestmentProofDoc(true);
        request.setIsApplicationCompleteCheck(true);
        request.setIsFormsAttachCheck(true);
        request.setIsEmissionsReductionActivityListCheck(true);
        request.setIsOriginalSpecCheck(true);
        request.setIsSubsidyCheck(true);
        return request;
    }

    private static RejectReviewRequest validRejectReviewRequest() {
        RejectReviewRequest request = new RejectReviewRequest();
        request.setReviewerInkan("印");
        request.setReviewerName("山田太郎");
        request.setRejectReason("書類不備");
        request.setIsSolarContractDoc(true);
        request.setIsPcsSpecDoc(true);
        request.setIsBatterySpecDoc(true);
        request.setIsOperationStartDoc(true);
        request.setIsGenerationDataDoc(true);
        request.setIsReversePowerDoc(true);
        request.setIsInvestmentProofDoc(true);
        request.setIsApplicationCompleteCheck(true);
        request.setIsFormsAttachCheck(true);
        request.setIsEmissionsReductionActivityListCheck(true);
        request.setIsOriginalSpecCheck(true);
        request.setIsSubsidyCheck(true);
        return request;
    }

    @Nested
    @DisplayName("GET /admin/companies のテスト")
    class GetCompaniesTest {

        @Test
        @DisplayName("UT-ACC-001: 有効な検索条件がServiceへ渡り、レスポンスがそのまま返る")
        void getCompanies_validQuery_returnsServiceResponseAsIs() {
            CompanyListQuery query = new CompanyListQuery();
            query.setStatus(Set.of(CompanyStatus.ACTIVE));
            query.setSearch("テスト");
            query.setPage(1);
            query.setLimit(20);
            AdminCompanyListResponse expected = new AdminCompanyListResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(adminCompanyQueryService.findCompanies(query,null)).thenReturn(expected);

            ResponseEntity<AdminCompanyListResponse> response = controller.getCompanies(query);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(adminCompanyQueryService).findCompanies(query,null);
        }

        @Test
        @DisplayName("UT-ACC-002: Serviceがエラー系statusCodeを返した場合のHTTPステータス変換確認")
        void getCompanies_serviceReturnsError_convertsHttpStatus() {
            CompanyListQuery query = new CompanyListQuery();
            AdminCompanyListResponse expected = new AdminCompanyListResponse();
            expected.setStatusCode(ResponseConstants.BAD_REQUEST);
            when(adminCompanyQueryService.findCompanies(query,null)).thenReturn(expected);

            ResponseEntity<AdminCompanyListResponse> response = controller.getCompanies(query);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isSameAs(expected);
        }
    }

    // ------------------------------------------------------------------------
    // POST /admin/companies/{companyId}/approve
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/companies/{companyId}/approve のテスト")
    class ApproveCompanyTest {

        @Test
        @DisplayName("UT-ACC-003: 申請中企業のcompanyIdがServiceへ渡り、承認成功結果が返る")
        void approveCompany_validCompanyId_returnsServiceSuccessResponse() {
            ApproveCompanyResponse expected = new ApproveCompanyResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(companyApprovalService.approveCompany("COMP-001")).thenReturn(expected);

            ResponseEntity<ApproveCompanyResponse> response = controller.approveCompany("COMP-001");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(companyApprovalService).approveCompany("COMP-001");
        }

        @Test
        @DisplayName("UT-ACC-004: ServiceがNOT_FOUND等のエラーレスポンスを返した場合のステータス変換確認")
        void approveCompany_serviceReturnsError_convertsHttpStatus() {
            ApproveCompanyResponse expected = new ApproveCompanyResponse();
            expected.setStatusCode(ResponseConstants.NOT_FOUND);
            when(companyApprovalService.approveCompany("COMP-404")).thenReturn(expected);

            ResponseEntity<ApproveCompanyResponse> response = controller.approveCompany("COMP-404");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isSameAs(expected);
        }
    }

    // ------------------------------------------------------------------------
    // POST /admin/companies/{companyId}/reject
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/companies/{companyId}/reject のテスト")
    class RejectCompanyTest {

        @Test
        @DisplayName("UT-ACC-005: 有効な却下理由がServiceへ渡り、却下成功結果が返る")
        void rejectCompany_validRequest_returnsServiceSuccessResponse() {
            RejectCompanyRequest request = new RejectCompanyRequest();
            request.setRejectionReason("書類不備");
            RejectCompanyResponse expected = RejectCompanyResponse.builder()
                    .statusCode(ResponseConstants.OK).companyId("COMP-001").build();
            when(companyApprovalService.rejectCompany("COMP-001", "書類不備")).thenReturn(expected);

            ResponseEntity<RejectCompanyResponse> response = controller.rejectCompany("COMP-001", request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(companyApprovalService).rejectCompany("COMP-001", "書類不備");
        }

        @Test
        @DisplayName("UT-ACC-006: rejectionReasonが不正/欠落し@Validでバリデーションエラーとなる")
        void rejectCompany_invalidRejectionReason_returnsValidationError() throws Exception {
            assertInvalidBody("/admin/companies/COMP-001/reject", "{\"rejectionReason\":\"\"}");
            verify(companyApprovalService, never()).rejectCompany(any(), any());
        }
    }

    // ------------------------------------------------------------------------
    // POST /admin/companies/{companyId}/master-setup-link/resend
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/companies/{companyId}/master-setup-link/resend のテスト")
    class ResendMasterSetupLinkTest {

        @Test
        @DisplayName("UT-ACC-007: 有効なcompanyIdがServiceへ渡り、再送成功結果が返る")
        void resendMasterSetupLink_validCompanyId_returnsServiceSuccessResponse() {
            ApproveCompanyResponse expected = new ApproveCompanyResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(companyApprovalService.resendMasterSetupLink("COMP-001")).thenReturn(expected);

            ResponseEntity<ApproveCompanyResponse> response = controller.resendMasterSetupLink("COMP-001");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(companyApprovalService).resendMasterSetupLink("COMP-001");
        }

        @Test
        @DisplayName("UT-ACC-008: ServiceがNOT_FOUND等のエラーレスポンスを返した場合のステータス変換確認")
        void resendMasterSetupLink_serviceReturnsError_convertsHttpStatus() {
            ApproveCompanyResponse expected = new ApproveCompanyResponse();
            expected.setStatusCode(ResponseConstants.NOT_FOUND);
            when(companyApprovalService.resendMasterSetupLink("COMP-404")).thenReturn(expected);

            ResponseEntity<ApproveCompanyResponse> response = controller.resendMasterSetupLink("COMP-404");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isSameAs(expected);
        }
    }

    // ------------------------------------------------------------------------
    // POST /admin/companies/{companyId}/password-reset-link
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/companies/{companyId}/password-reset-link のテスト")
    class RequestMasterPasswordResetTest {

        @Test
        @DisplayName("UT-ACC-009: JWTのusernameとcompanyIdがServiceへ渡り、成功結果が返る")
        void requestMasterPasswordReset_validRequest_returnsServiceSuccessResponse() {
            BaseResponse expected = new BaseResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn("admin@example.com");
            when(securityAccountService.operatorRequestMasterPasswordReset("COMP-001", "admin@example.com"))
                    .thenReturn(expected);

            ResponseEntity<BaseResponse> response = controller.requestMasterPasswordReset(authentication, "COMP-001");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(securityAccountService).operatorRequestMasterPasswordReset("COMP-001", "admin@example.com");
        }

        @Test
        @DisplayName("UT-ACC-010: ServiceがNOT_FOUND等のエラーレスポンスを返した場合のステータス変換確認")
        void requestMasterPasswordReset_serviceReturnsError_convertsHttpStatus() {
            BaseResponse expected = new BaseResponse();
            expected.setStatusCode(ResponseConstants.NOT_FOUND);
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn("admin@example.com");
            when(securityAccountService.operatorRequestMasterPasswordReset("COMP-404", "admin@example.com"))
                    .thenReturn(expected);

            ResponseEntity<BaseResponse> response = controller.requestMasterPasswordReset(authentication, "COMP-404");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isSameAs(expected);
        }
    }

    // ------------------------------------------------------------------------
    // GET /admin/companies/{companyId}
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies/{companyId} のテスト")
    class GetCompanyDetailTest {

        @Test
        @DisplayName("UT-ACC-011: 有効なcompanyIdがServiceへ渡り、企業詳細がそのまま返る")
        void getCompanyDetail_validCompanyId_returnsServiceResponseAsIs() {
            AdminCompanyDetailResponse expected = new AdminCompanyDetailResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(adminCompanyQueryService.findCompanyDetail("COMP-001")).thenReturn(expected);

            ResponseEntity<AdminCompanyDetailResponse> response = controller.getCompanyDetail("COMP-001");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(adminCompanyQueryService).findCompanyDetail("COMP-001");
        }

        @Test
        @DisplayName("UT-ACC-012: ServiceがNOT_FOUNDレスポンスを返した場合のステータス変換確認")
        void getCompanyDetail_serviceReturnsNotFound_convertsHttpStatus404() {
            AdminCompanyDetailResponse expected = new AdminCompanyDetailResponse();
            expected.setStatusCode(ResponseConstants.NOT_FOUND);
            when(adminCompanyQueryService.findCompanyDetail("COMP-404")).thenReturn(expected);

            ResponseEntity<AdminCompanyDetailResponse> response = controller.getCompanyDetail("COMP-404");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isSameAs(expected);
        }
    }

    // ------------------------------------------------------------------------
    // POST /admin/companies/{companyId}/suspensions
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/companies/{companyId}/suspensions のテスト")
    class CreateSuspensionTest {

        @Test
        @DisplayName("UT-ACC-013: 有効なリクエストがServiceへ渡り、登録成功結果が返る")
        void createSuspension_validRequest_returnsServiceSuccessResponse() {
            CreateCompanySuspensionRequest request = new CreateCompanySuspensionRequest();
            request.setReasonCode(SuspensionReasonCode.POLICY_VIOLATION);
            request.setReasonDetail("規約違反");
            CompanySuspensionResponse expected = new CompanySuspensionResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(companyLifecycleService.createSuspension("COMP-001", request)).thenReturn(expected);

            ResponseEntity<CompanySuspensionResponse> response = controller.createSuspension("COMP-001", request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(companyLifecycleService).createSuspension("COMP-001", request);
        }

        @Test
        @DisplayName("UT-ACC-014: 必須項目（reason等）が欠落し@Validでバリデーションエラーとなる")
        void createSuspension_invalidRequest_returnsValidationError() throws Exception {
            assertInvalidBody("/admin/companies/COMP-001/suspensions", "{\"reasonDetail\":\"\"}");
            verify(companyLifecycleService, never()).createSuspension(any(), any());
        }
    }

    // ------------------------------------------------------------------------
    // GET /admin/companies/{companyId}/suspensions
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies/{companyId}/suspensions のテスト")
    class GetSuspensionsTest {

        @Test
        @DisplayName("UT-ACC-015: フィルターとページ情報がServiceへ渡り、一覧結果が返る")
        void getSuspensions_validParams_returnsServiceResponseAsIs() {
            CompanySuspensionListResponse expected = new CompanySuspensionListResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(companyLifecycleService.findSuspensions("COMP-001", false, 2, 10)).thenReturn(expected);

            ResponseEntity<CompanySuspensionListResponse> response =
                    controller.getSuspensions("COMP-001", false, 2, 10);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(companyLifecycleService).findSuspensions("COMP-001", false, 2, 10);
        }

        @Test
        @DisplayName("UT-ACC-016: page/limitが範囲外の場合@Min/@Maxでバリデーションエラーとなる")
        void getSuspensions_outOfRangePageOrLimit_returnsValidationError() throws Exception {
            Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
            ExecutableValidator executableValidator = validator.forExecutables();
            Method method = AdminCompanyController.class.getMethod(
                    "getSuspensions", String.class, boolean.class, int.class, int.class);
            Set<ConstraintViolation<AdminCompanyController>> violations = executableValidator.validateParameters(
                    controller, method, new Object[] {"COMP-001", true, 0, 200});

            assertThat(violations).hasSize(2);
            verify(companyLifecycleService, never()).findSuspensions(any(), eq(true), eq(0), eq(200));
        }
    }

    // ------------------------------------------------------------------------
    // POST /admin/companies/{companyId}/suspensions/{suspensionId}/resolve
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/companies/{companyId}/suspensions/{suspensionId}/resolve のテスト")
    class ResolveSuspensionTest {

        @Test
        @DisplayName("UT-ACC-017: 有効なリクエストがServiceへ渡り、解除成功結果が返る")
        void resolveSuspension_validRequest_returnsServiceSuccessResponse() {
            ResolveCompanySuspensionRequest request = new ResolveCompanySuspensionRequest();
            request.setResolutionNote("対応完了");
            CompanySuspensionResponse expected = new CompanySuspensionResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(companyLifecycleService.resolveSuspension("COMP-001", 10, request)).thenReturn(expected);

            ResponseEntity<CompanySuspensionResponse> response =
                    controller.resolveSuspension("COMP-001", 10, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(companyLifecycleService).resolveSuspension("COMP-001", 10, request);
        }

        @Test
        @DisplayName("UT-ACC-018: ServiceがNOT_FOUND等のエラーレスポンスを返した場合のステータス変換確認")
        void resolveSuspension_serviceReturnsError_convertsHttpStatus() {
            ResolveCompanySuspensionRequest request = new ResolveCompanySuspensionRequest();
            request.setResolutionNote("対応完了");
            CompanySuspensionResponse expected = new CompanySuspensionResponse();
            expected.setStatusCode(ResponseConstants.NOT_FOUND);
            when(companyLifecycleService.resolveSuspension("COMP-001", 99999, request)).thenReturn(expected);

            ResponseEntity<CompanySuspensionResponse> response =
                    controller.resolveSuspension("COMP-001", 99999, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isSameAs(expected);
        }
    }

    // ------------------------------------------------------------------------
    // GET /admin/companies/participate/project/{projectId}
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies/participate/project/{projectId} のテスト")
    class GetParticipationsByProjectTest {

        @Test
        @DisplayName("UT-ACC-019: 有効なprojectIdがServiceへ渡り、一覧結果が返る")
        void getParticipationsByProject_validProjectId_returnsServiceListAsIs() {
            List<ParticipationReviewDetailResponse> expected = List.of();
            when(reviewService.getParticipationsByProject("PROJECT-001")).thenReturn(expected);

            ResponseEntity<List<ParticipationReviewDetailResponse>> response =
                    controller.getParticipationsByProject("PROJECT-001");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(reviewService).getParticipationsByProject("PROJECT-001");
        }

        @Test
        @DisplayName("UT-ACC-020: 対象プロジェクトの申請が存在しない場合、空リストが返る")
        void getParticipationsByProject_noMatchingParticipations_returnsEmptyList() {
            when(reviewService.getParticipationsByProject("PROJECT-999")).thenReturn(List.of());

            ResponseEntity<List<ParticipationReviewDetailResponse>> response =
                    controller.getParticipationsByProject("PROJECT-999");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    // ------------------------------------------------------------------------
    // GET /admin/companies/participate/pending
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies/participate/pending のテスト")
    class GetPendingReviewsTest {

        @Test
        @DisplayName("UT-ACC-021: Serviceのレビュー待ち一覧がそのまま返る")
        void getPendingReviews_returnsServiceListAsIs() {
            List<ParticipationReviewDetailResponse> expected = List.of();
            when(reviewService.getPendingReviews()).thenReturn(expected);

            ResponseEntity<List<ParticipationReviewDetailResponse>> response = controller.getPendingReviews();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(reviewService).getPendingReviews();
        }

        @Test
        @DisplayName("UT-ACC-022: レビュー待ちが0件の場合、空リストが返る")
        void getPendingReviews_noPendingReviews_returnsEmptyList() {
            when(reviewService.getPendingReviews()).thenReturn(List.of());

            ResponseEntity<List<ParticipationReviewDetailResponse>> response = controller.getPendingReviews();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    // ------------------------------------------------------------------------
    // GET /admin/companies/participate/open
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies/participate/open のテスト")
    class GetPendingReviewsForMeTest {

        @Test
        @DisplayName("UT-ACC-023: JWTの運営者IDがServiceへ渡り、レビュー可能一覧が返る")
        void getPendingReviewsForMe_validOperatorUserId_returnsServiceListAsIs() {
            List<ParticipationReviewDetailResponse> expected = List.of();
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(7);
            when(reviewService.getPendingReviewsByUserId(7)).thenReturn(expected);

            ResponseEntity<List<ParticipationReviewDetailResponse>> response =
                    controller.getPendingReviewsForMe(authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(reviewService).getPendingReviewsByUserId(7);
        }

        @Test
        @DisplayName("UT-ACC-024: 対象運営者にレビュー可能な申請が無い場合、空リストが返る")
        void getPendingReviewsForMe_noAssignableReviews_returnsEmptyList() {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(8);
            when(reviewService.getPendingReviewsByUserId(8)).thenReturn(List.of());

            ResponseEntity<List<ParticipationReviewDetailResponse>> response =
                    controller.getPendingReviewsForMe(authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    // ------------------------------------------------------------------------
    // GET /admin/companies/participations/approved
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies/participations/approved のテスト")
    class GetApprovedParticipationsTest {

        @Test
        @DisplayName("UT-ACC-025: Serviceの承認済み一覧がそのまま返る")
        void getApprovedParticipations_returnsServiceListAsIs() {
            List<ParticipationReviewDetailResponse> expected = List.of();
            when(reviewService.getApprovedParticipations()).thenReturn(expected);

            ResponseEntity<List<ParticipationReviewDetailResponse>> response = controller.getApprovedParticipations();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(reviewService).getApprovedParticipations();
        }

        @Test
        @DisplayName("UT-ACC-026: 承認済み申請が0件の場合、空リストが返る")
        void getApprovedParticipations_noApprovedParticipations_returnsEmptyList() {
            when(reviewService.getApprovedParticipations()).thenReturn(List.of());

            ResponseEntity<List<ParticipationReviewDetailResponse>> response = controller.getApprovedParticipations();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    // ------------------------------------------------------------------------
    // GET /admin/companies/participations/rejected
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies/participations/rejected のテスト")
    class GetRejectedParticipationsTest {

        @Test
        @DisplayName("UT-ACC-027: Serviceの却下済み一覧がそのまま返る")
        void getRejectedParticipations_returnsServiceListAsIs() {
            List<ParticipationReviewDetailResponse> expected = List.of();
            when(reviewService.getRejectedParticipations()).thenReturn(expected);

            ResponseEntity<List<ParticipationReviewDetailResponse>> response = controller.getRejectedParticipations();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(reviewService).getRejectedParticipations();
        }

        @Test
        @DisplayName("UT-ACC-028: 却下済み申請が0件の場合、空リストが返る")
        void getRejectedParticipations_noRejectedParticipations_returnsEmptyList() {
            when(reviewService.getRejectedParticipations()).thenReturn(List.of());

            ResponseEntity<List<ParticipationReviewDetailResponse>> response = controller.getRejectedParticipations();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    // ------------------------------------------------------------------------
    // GET /admin/companies/participate/{participationId}
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies/participate/{participationId} のテスト")
    class GetReviewDetailTest {

        @Test
        @DisplayName("UT-ACC-029: 有効なparticipationIdがServiceへ渡り、詳細結果が返る")
        void getReviewDetail_validParticipationId_returnsServiceResponseAsIs() {
            ParticipationReviewDetailResponse expected = new ParticipationReviewDetailResponse();
            when(reviewService.getReviewDetail(12)).thenReturn(expected);

            ResponseEntity<ParticipationReviewDetailResponse> response = controller.getReviewDetail(12);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(reviewService).getReviewDetail(12);
        }

        @Test
        @DisplayName("UT-ACC-030: 対象participationIdが存在しない場合、Serviceの例外がハンドラで処理される")
        void getReviewDetail_notFoundParticipationId_propagatesServiceException() {
            when(reviewService.getReviewDetail(99999)).thenThrow(new ResourceNotFoundException("not found"));

            assertThrows(ResourceNotFoundException.class, () -> controller.getReviewDetail(99999));
            verify(reviewService).getReviewDetail(99999);
        }
    }

    // ------------------------------------------------------------------------
    // POST /admin/companies/participation/{participationId}/approve
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/companies/participation/{participationId}/approve のテスト")
    class ApproveReviewTest {

        @Test
        @DisplayName("UT-ACC-031: JWT由来の運営者IDと有効なリクエストがServiceへ渡り、承認成功結果が返る")
        void approveReview_validRequest_returnsSuccessMessage() {
            ApproveReviewRequest request = validApproveReviewRequest();
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(7);

            ResponseEntity<BaseResponse> response = controller.approveReview(authentication, 12, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getMessage()).isEqualTo("承認しました。");
            verify(reviewService).approveReview(12, 7, request);
        }

        @Test
        @DisplayName("UT-ACC-032: 必須項目が欠落し@Validでバリデーションエラーとなる")
        void approveReview_invalidRequest_returnsValidationError() throws Exception {
            assertInvalidBody("/admin/companies/participation/12/approve", "{}");
            verify(reviewService, never()).approveReview(eq(12), any(), any());
        }
    }

    // ------------------------------------------------------------------------
    // POST /admin/companies/participation/{participationId}/reject
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/companies/participation/{participationId}/reject のテスト")
    class RejectReviewTest {

        @Test
        @DisplayName("UT-ACC-033: JWT由来の運営者IDと有効なリクエストがServiceへ渡り、却下成功結果が返る")
        void rejectReview_validRequest_returnsSuccessMessage() {
            RejectReviewRequest request = validRejectReviewRequest();
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(7);

            ResponseEntity<BaseResponse> response = controller.rejectReview(authentication, 12, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getMessage()).isEqualTo("却下しました。");
            verify(reviewService).rejectReview(12, 7, request);
        }

        @Test
        @DisplayName("UT-ACC-034: 必須項目（rejectionReason）が欠落し@Validでバリデーションエラーとなる")
        void rejectReview_invalidRequest_returnsValidationError() throws Exception {
            assertInvalidBody("/admin/companies/participation/12/reject", "{}");
            verify(reviewService, never()).rejectReview(eq(12), any(), any());
        }
    }

    // ------------------------------------------------------------------------
    // GET /admin/companies/operator-user
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies/operator-user のテスト")
    class GetOperatorUsersTest {

        @Test
        @DisplayName("UT-ACC-035: JWTのcompanyIdがServiceへ渡り、運用者一覧がそのまま返る")
        void getOperatorUsers_validCompanyId_returnsServiceResponseAsIs() {
            UserListResponse expected = new UserListResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP-001");
            when(operatorUserService.getOperatorUsersByCompany("COMP-001")).thenReturn(expected);

            ResponseEntity<UserListResponse> response = controller.getOperatorUsers(authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(operatorUserService).getOperatorUsersByCompany("COMP-001");
        }

        @Test
        @DisplayName("UT-ACC-036: Serviceがエラー系statusCodeを返した場合のHTTPステータス変換確認")
        void getOperatorUsers_serviceReturnsError_convertsHttpStatus() {
            UserListResponse expected = new UserListResponse();
            expected.setStatusCode(ResponseConstants.NOT_FOUND);
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP-404");
            when(operatorUserService.getOperatorUsersByCompany("COMP-404")).thenReturn(expected);

            ResponseEntity<UserListResponse> response = controller.getOperatorUsers(authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isSameAs(expected);
        }
    }

    // ------------------------------------------------------------------------
    // POST /admin/companies/operator-user
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/companies/operator-user のテスト")
    class CreateUserTest {

        @Test
        @DisplayName("UT-ACC-037: JWTのcompanyId・usernameと有効なリクエストがServiceへ渡り、作成成功結果が返る")
        void createUser_validRequest_returnsServiceSuccessResponse() {
            CreateOperatorUserRequest request = new CreateOperatorUserRequest();
            request.setEmail("yamada@example.com");
            CreateUserResponse expected = new CreateUserResponse();
            expected.setStatusCode("OK");
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP-001");
            when(jwtUtil.extractUsername(TOKEN)).thenReturn("admin@example.com");
            when(operatorUserService.createOperatorUser("COMP-001", "admin@example.com", request))
                    .thenReturn(expected);

            ResponseEntity<CreateUserResponse> response = controller.createUser(authentication, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(operatorUserService).createOperatorUser("COMP-001", "admin@example.com", request);
        }

        @Test
        @DisplayName("UT-ACC-038: 必須項目（email等）が欠落し@Validでバリデーションエラーとなる")
        void createUser_invalidRequest_returnsValidationError() throws Exception {
            assertInvalidBody("/admin/companies/operator-user", "{\"email\":\"\"}");
            verify(operatorUserService, never()).createOperatorUser(any(), any(), any());
        }
    }

    // ------------------------------------------------------------------------
    // PATCH /admin/companies/operator-user/{userId}
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("PATCH /admin/companies/operator-user/{userId} のテスト")
    class UpdateOperatorUserTest {

        @Test
        @DisplayName("UT-ACC-039: adminロールが他ユーザーを更新した場合、Serviceへ委譲され成功結果が返る")
        void updateOperatorUser_adminUpdatingOtherUser_returnsServiceSuccessResponse() {
            UpdateUserRequest request = new UpdateUserRequest();
            UpdateUserResponse expected = new UpdateUserResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP-001");
            when(jwtUtil.extractUsername(TOKEN)).thenReturn("admin@example.com");
            when(jwtUtil.extractRole(TOKEN)).thenReturn("admin");
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(1);
            when(operatorUserService.updateUser("COMP-001", "admin@example.com", request, "admin", 1))
                    .thenReturn(expected);

            ResponseEntity<UpdateUserResponse> response = controller.updateOperatorUser(authentication, 5, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            assertThat(request.getUserId()).isEqualTo(5);
            verify(operatorUserService).updateUser("COMP-001", "admin@example.com", request, "admin", 1);
        }

        @Test
        @DisplayName("UT-ACC-040: operatorロールが他ユーザー（自分以外）を更新しようとした場合、403が返りServiceが呼ばれない")
        void updateOperatorUser_operatorUpdatingOtherUser_returnsForbidden() {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP-001");
            when(jwtUtil.extractUsername(TOKEN)).thenReturn("operator@example.com");
            when(jwtUtil.extractRole(TOKEN)).thenReturn("operator");
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(1);

            ResponseEntity<UpdateUserResponse> response =
                    controller.updateOperatorUser(authentication, 5, new UpdateUserRequest());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNull();
            verify(operatorUserService, never()).updateUser(any(), any(), any(), any(), any());
        }
    }

    // ------------------------------------------------------------------------
    // GET /admin/companies/operator-user/{userId}
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies/operator-user/{userId} のテスト")
    class GetOperatorUserProfileTest {

        @Test
        @DisplayName("UT-ACC-041: operatorロールが自分自身のプロファイルを取得した場合、Serviceへ委譲され成功結果が返る")
        void getOperatorUserProfile_operatorRequestingSelf_returnsServiceSuccessResponse() {
            UserProfileResponse expected = new UserProfileResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractRole(TOKEN)).thenReturn("operator");
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(1);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP-001");
            when(operatorUserService.getOperatorUserProfileById(1, 1, "operator", "COMP-001"))
                    .thenReturn(expected);

            ResponseEntity<UserProfileResponse> response = controller.getOperatorUserProfile(authentication, 1);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(operatorUserService).getOperatorUserProfileById(1, 1, "operator", "COMP-001");
        }

        @Test
        @DisplayName("UT-ACC-042: operatorロールが他人（自分以外）のプロファイルを取得しようとした場合、403が返りServiceが呼ばれない")
        void getOperatorUserProfile_operatorRequestingOtherUser_returnsForbidden() {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractRole(TOKEN)).thenReturn("operator");
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(1);

            ResponseEntity<UserProfileResponse> response = controller.getOperatorUserProfile(authentication, 5);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNull();
            verify(operatorUserService, never()).getOperatorUserProfileById(any(), any(), any(), any());
        }
    }
}