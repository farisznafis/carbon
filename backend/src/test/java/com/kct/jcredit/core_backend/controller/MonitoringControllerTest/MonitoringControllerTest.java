package com.kct.jcredit.core_backend.controller.MonitoringControllerTest;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.controller.MonitoringController;
import com.kct.jcredit.core_backend.dto.request.CancelRequestDecisionRequest;
import com.kct.jcredit.core_backend.dto.request.CreateCancelRequestRequest;
import com.kct.jcredit.core_backend.dto.request.RegisterMonitoringRecordSolarRequest;
import com.kct.jcredit.core_backend.dto.response.CancelRequestDecisionResponse;
import com.kct.jcredit.core_backend.dto.response.CancelRequestListResponse;
import com.kct.jcredit.core_backend.dto.response.CancelRequestResponse;
import com.kct.jcredit.core_backend.dto.response.FileUploadResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringEvidenceItemsResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringRecordSolarListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringRecordSolarResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.service.CancelRequestService;
import com.kct.jcredit.core_backend.service.MonitoringService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.handler.GlobalErrorHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MonitoringController API and delegation test outline.
 *
 * Use MockMvc for @Valid, request-parameter, multipart, and authorization
 * behavior. Direct controller calls do not run MVC argument validation.
 */
@ExtendWith(MockitoExtension.class)
class MonitoringControllerTest {

    private static final String TOKEN = "monitoring-test-token";
    private static final String USERNAME = "operator01";
    private static final String COMPANY_ID = "COMP-001";
    private static final String PROJECT_ID = "PROJECT-001";
    private static final String SITE_ID = "SITE-001";

    @Mock private MonitoringService monitoringService;
    @Mock private CancelRequestService cancelRequestService;
    @Mock private JwtUtil jwtUtil;
    @Mock private Authentication authentication;

    @InjectMocks
    private MonitoringController controller;

    private MockMvc mockMvc() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalErrorHandler())
                .build();
    }

    private void prepareIdentity() {
        when(authentication.getCredentials()).thenReturn(TOKEN);
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
    }

    private RegisterMonitoringRecordSolarRequest validMonitoringRequest() {
        RegisterMonitoringRecordSolarRequest request = new RegisterMonitoringRecordSolarRequest();
        request.setProjectId(PROJECT_ID);
        request.setSiteId(SITE_ID);
        request.setEquipmentId(1);
        request.setProjectType("SOLAR");
        request.setDateRecorded("2024-01-15");
        request.setElPv(new BigDecimal("123.456"));
        request.setElPvr(new BigDecimal("78.900"));
        request.setChangedBattery(false);
        request.setElpjControl(new BigDecimal("12.300"));
        request.setElpjAccuracy(new BigDecimal("99.50"));
        request.setElPvEvidenceUrl("https://example.test/elpv.pdf");
        request.setElPvrEvidenceUrl("https://example.test/elpvr.pdf");
        request.setElpjControlEvidenceUrl("https://example.test/control.pdf");
        return request;
    }

    @Nested
    @DisplayName("POST /monitoring のテスト")
    class RegisterMonitoringRecordSolarTest {

        @Test
        @DisplayName("UT-MC-001: 有効な記録とJWT由来のusername/companyIdがServiceへ渡る")
        void registerMonitoringRecordSolar_validRequest_returnsServiceResponse() {
            prepareIdentity();
            RegisterMonitoringRecordSolarRequest request = validMonitoringRequest();
            MonitoringRecordSolarResponse expected = new MonitoringRecordSolarResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(monitoringService.registerMonitoringRecordSolar(USERNAME, COMPANY_ID, request)).thenReturn(expected);

            var response = controller.RegisterMonitoringRecordSolar(authentication, request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(monitoringService).registerMonitoringRecordSolar(USERNAME, COMPANY_ID, request);
            // Input: JWT、必須フィールドと証跡URLを満たすrequest
            // Output: HTTP statusがResponseUtilで変換され、Serviceの結果が返る
            //         registerMonitoringRecordSolar(username, companyId, request)を検証
        }

        @Test
        @DisplayName("UT-MC-002: Bean Validation違反時は400となりServiceは呼ばれない")
        void registerMonitoringRecordSolar_invalidRequest_returnsValidationError() throws Exception {
            mockMvc().perform(post("/monitoring")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(monitoringService);
            // Input: 必須値null、範囲外の数値、または不正なdateRecorded
            // Output: HTTP 400、monitoringService.registerMonitoringRecordSolarは呼ばれない
        }

        @Test
        @DisplayName("UT-MC-003: ServiceがエラーstatusCodeを返した場合HTTP statusが変換される")
        void registerMonitoringRecordSolar_serviceReturnsError_convertsHttpStatus() {
            prepareIdentity();
            RegisterMonitoringRecordSolarRequest request = validMonitoringRequest();
            MonitoringRecordSolarResponse expected = new MonitoringRecordSolarResponse();
            expected.setStatusCode(ResponseConstants.NOT_FOUND);
            when(monitoringService.registerMonitoringRecordSolar(USERNAME, COMPANY_ID, request)).thenReturn(expected);

            var response = controller.RegisterMonitoringRecordSolar(authentication, request);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: ServiceがNOT_FOUND等のレスポンスを返す
            // Output: ResponseUtil.getHttpStatusに従うHTTP statusと同じレスポンスbody
        }
    }

    @Nested
    @DisplayName("POST /monitoring/validate のテスト")
    class ValidateMonitoringRecordSolarTest {

        @Test
        @DisplayName("UT-MC-004: 有効なrequestとJWT情報が検証Serviceへ渡る")
        void validateMonitoringRecordSolar_validRequest_returnsServiceResponse() {
            prepareIdentity();
            RegisterMonitoringRecordSolarRequest request = validMonitoringRequest();
            BaseResponse expected = BaseResponse.success("Valid");
            when(monitoringService.validateMonitoringRecordSolar(USERNAME, COMPANY_ID, request)).thenReturn(expected);

            var response = controller.validateMonitoringRecordSolar(authentication, request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(monitoringService).validateMonitoringRecordSolar(USERNAME, COMPANY_ID, request);
            // Input: JWTと有効なRegisterMonitoringRecordSolarRequest
            // Output: HTTP statusがService responseのstatusCodeから変換される
            //         validateMonitoringRecordSolar(username, companyId, request)を検証
        }

        @Test
        @DisplayName("UT-MC-005: Bean Validation違反時は400となり検証Serviceは呼ばれない")
        void validateMonitoringRecordSolar_invalidRequest_returnsValidationError() throws Exception {
            mockMvc().perform(post("/monitoring/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(monitoringService);
            // Input: requestの必須項目欠落または数値制約違反
            // Output: HTTP 400、monitoringService.validateMonitoringRecordSolarは呼ばれない
        }

        @Test
        @DisplayName("UT-MC-006: 検証ServiceのエラーstatusCodeがHTTP statusへ変換される")
        void validateMonitoringRecordSolar_serviceReturnsError_convertsHttpStatus() {
            prepareIdentity();
            RegisterMonitoringRecordSolarRequest request = validMonitoringRequest();
            BaseResponse expected = BaseResponse.error(ResponseConstants.BAD_REQUEST, "Invalid record");
            when(monitoringService.validateMonitoringRecordSolar(USERNAME, COMPANY_ID, request)).thenReturn(expected);

            var response = controller.validateMonitoringRecordSolar(authentication, request);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: ServiceがBAD_REQUEST等を返す
            // Output: ResponseUtilによるHTTP statusとServiceのbody
        }
    }

    @Nested
    @DisplayName("GET /monitoring のテスト")
    class ReadLastMonitoringRecordSolarTest {

        @Test
        @DisplayName("UT-MC-007: projectId/siteIdとJWT情報がServiceへ渡り最新記録が返る")
        void readLastMonitoringRecordSolar_validParams_returnsServiceResponse() {
            prepareIdentity();
            MonitoringRecordSolarResponse expected = new MonitoringRecordSolarResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(monitoringService.readLastMonitoringRecordSolar(USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID))
                    .thenReturn(expected);

            var response = controller.ReadLastMonitoringRecordSolar(authentication, PROJECT_ID, SITE_ID);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(monitoringService).readLastMonitoringRecordSolar(USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID);
            // Input: projectId、siteId、JWT
            // Output: HTTP status/bodyがService responseに一致し、4引数のService呼び出しを検証
        }

        @Test
        @DisplayName("UT-MC-008: 必須query parameter欠落時は400となりServiceは呼ばれない")
        void readLastMonitoringRecordSolar_missingQueryParameter_returnsBadRequest() throws Exception {
            mockMvc().perform(get("/monitoring").queryParam("projectId", PROJECT_ID))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(monitoringService);
            // Input: projectIdまたはsiteIdなし
            // Output: HTTP 400、readLastMonitoringRecordSolarは呼ばれない
        }

        @Test
        @DisplayName("UT-MC-009: ServiceのNOT_FOUNDがHTTP 404に変換される")
        void readLastMonitoringRecordSolar_serviceReturnsNotFound_convertsHttpStatus() {
            prepareIdentity();
            MonitoringRecordSolarResponse expected = new MonitoringRecordSolarResponse();
            expected.setStatusCode(ResponseConstants.NOT_FOUND);
            when(monitoringService.readLastMonitoringRecordSolar(USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID))
                    .thenReturn(expected);

            var response = controller.ReadLastMonitoringRecordSolar(authentication, PROJECT_ID, SITE_ID);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: ServiceがstatusCode=NOT_FOUNDを返す
            // Output: HTTP 404、Serviceのresponse body
        }
    }

    @Nested
    @DisplayName("GET /monitoring/{projectId}/list のテスト")
    class ListMonitoringRecordsTest {

        @Test
        @DisplayName("UT-MC-010: page/limit省略時にdefault値とprojectIdがServiceへ渡る")
        void listMonitoringRecords_defaultPagination_returnsServiceResponse() {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
            MonitoringRecordSolarListResponse expected = new MonitoringRecordSolarListResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(monitoringService.readAllMonitoringRecordsSolar(USERNAME, 1, 50, PROJECT_ID, null))
                    .thenReturn(expected);

            var response = controller.listMonitoringRecords(PROJECT_ID, 1, 50, null, authentication);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(monitoringService).readAllMonitoringRecordsSolar(USERNAME, 1, 50, PROJECT_ID, null);
            // Input: projectId、JWT、page/limit省略
            // Output: HTTP status/bodyがService responseに一致
            //         readAllMonitoringRecordsSolar(username, 1, 50, projectId, null)を検証
        }

        @Test
        @DisplayName("UT-MC-011: paginationとnextPageTokenがServiceへ引き継がれる")
        void listMonitoringRecords_customPagination_returnsServiceResponse() {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
            MonitoringRecordSolarListResponse expected = new MonitoringRecordSolarListResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(monitoringService.readAllMonitoringRecordsSolar(USERNAME, 3, 20, PROJECT_ID, "next-token"))
                    .thenReturn(expected);

            var response = controller.listMonitoringRecords(PROJECT_ID, 3, 20, "next-token", authentication);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(monitoringService).readAllMonitoringRecordsSolar(USERNAME, 3, 20, PROJECT_ID, "next-token");
            // Input: page、limit、nextPageTokenを指定
            // Output: 指定値とJWT usernameがServiceへ渡る
        }

        @Test
        @DisplayName("UT-MC-012: ServiceのstatusCodeがHTTP statusへ変換される")
        void listMonitoringRecords_serviceReturnsError_convertsHttpStatus() {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
            MonitoringRecordSolarListResponse expected = new MonitoringRecordSolarListResponse();
            expected.setStatusCode(ResponseConstants.BAD_REQUEST);
            when(monitoringService.readAllMonitoringRecordsSolar(USERNAME, 1, 50, PROJECT_ID, null))
                    .thenReturn(expected);

            var response = controller.listMonitoringRecords(PROJECT_ID, 1, 50, null, authentication);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: Serviceがエラーレスポンスを返す
            // Output: ResponseUtilに対応するHTTP statusと同じbody
        }
    }

    @Nested
    @DisplayName("GET /monitoring/{siteId}/{projectId}/list のテスト")
    class ListMonitoringRecordsBySiteTest {

        @Test
        @DisplayName("UT-MC-013: site/project、JWT、default paginationがServiceへ渡る")
        void listMonitoringRecordsBySite_defaultPagination_returnsServiceResponse() {
            prepareIdentity();
            MonitoringRecordSolarListResponse expected = new MonitoringRecordSolarListResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(monitoringService.readSiteMonitoringRecordsSolar(
                USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, 1, 50, null)).thenReturn(expected);

            var response = controller.listMonitoringRecordsbySite(SITE_ID, PROJECT_ID, 1, 50, null, authentication);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(monitoringService).readSiteMonitoringRecordsSolar(
                USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, 1, 50, null);
            // Input: siteId、projectId、JWT、page/limit省略
            // Output: readSiteMonitoringRecordsSolar(username, companyId, projectId, siteId, 1, 50, null)
            //         の引数とresponse status/bodyを検証
        }

        @Test
        @DisplayName("UT-MC-014: paginationとnextPageToken指定値がServiceへ渡る")
        void listMonitoringRecordsBySite_customPagination_returnsServiceResponse() {
            prepareIdentity();
            MonitoringRecordSolarListResponse expected = new MonitoringRecordSolarListResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(monitoringService.readSiteMonitoringRecordsSolar(
                USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, 2, 25, "next-token")).thenReturn(expected);

            var response = controller.listMonitoringRecordsbySite(SITE_ID, PROJECT_ID, 2, 25, "next-token", authentication);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(monitoringService).readSiteMonitoringRecordsSolar(
                USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, 2, 25, "next-token");
            // Input: page、limit、nextPageTokenを指定
            // Output: 全てのpath/query値およびJWT由来情報の委譲を検証
        }

        @Test
        @DisplayName("UT-MC-015: ServiceのエラーstatusCodeがHTTP statusへ変換される")
        void listMonitoringRecordsBySite_serviceReturnsError_convertsHttpStatus() {
            prepareIdentity();
            MonitoringRecordSolarListResponse expected = new MonitoringRecordSolarListResponse();
            expected.setStatusCode(ResponseConstants.BAD_REQUEST);
            when(monitoringService.readSiteMonitoringRecordsSolar(
                    USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, 1, 50, null)).thenReturn(expected);

            var response = controller.listMonitoringRecordsbySite(SITE_ID, PROJECT_ID, 1, 50, null, authentication);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: ServiceがBAD_REQUEST等を返す
            // Output: ResponseUtilに対応するHTTP statusとService body
        }
    }

    @Nested
    @DisplayName("POST /monitoring/upload-elpv-evidence のテスト")
    class UploadElpvEvidenceTest {

        @Test
        @DisplayName("UT-MC-016: multipart file、optional description、JWT情報がELPV uploadへ渡る")
        void uploadElpvEvidence_validMultipart_returnsServiceResponse() {
            prepareIdentity();
            MockMultipartFile file = new MockMultipartFile("file", "elpv.pdf", "application/pdf", new byte[] {1, 2});
            FileUploadResponse expected = new FileUploadResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(monitoringService.uploadElpvEvidence(USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, "ELPV", file))
                    .thenReturn(expected);

            var response = controller.uploadElpvEvidence(authentication, PROJECT_ID, SITE_ID, "ELPV", file);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(monitoringService).uploadElpvEvidence(USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, "ELPV", file);
            // Input: projectId/siteId/file、description有無、JWT
            // Output: uploadElpvEvidenceに全引数が渡り、status/bodyが返る
        }

        @Test
        @DisplayName("UT-MC-017: ServiceのファイルエラーstatusCodeがHTTP statusへ変換される")
        void uploadElpvEvidence_serviceReturnsError_convertsHttpStatus() {
            prepareIdentity();
            MockMultipartFile file = new MockMultipartFile("file", "bad.exe", "application/octet-stream", new byte[] {1});
            FileUploadResponse expected = new FileUploadResponse();
            expected.setStatusCode(ResponseConstants.UNSUPPORTED_FILE_TYPE);
            when(monitoringService.uploadElpvEvidence(USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, null, file))
                    .thenReturn(expected);

            var response = controller.uploadElpvEvidence(authentication, PROJECT_ID, SITE_ID, null, file);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: ServiceがUNSUPPORTED_FILE_TYPE等を返す
            // Output: ResponseUtilに対応するHTTP statusとService body
        }
    }

    @Nested
    @DisplayName("POST /monitoring/upload-elpvr-evidence のテスト")
    class UploadElpvrEvidenceTest {

        @Test
        @DisplayName("UT-MC-018: multipart fileとJWT情報がELPVR uploadへ渡る")
        void uploadElpvrEvidence_validMultipart_returnsServiceResponse() {
            prepareIdentity();
            MockMultipartFile file = new MockMultipartFile("file", "elpvr.pdf", "application/pdf", new byte[] {1, 2});
            FileUploadResponse expected = new FileUploadResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(monitoringService.uploadElpvrEvidence(USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, null, file))
                    .thenReturn(expected);

            var response = controller.uploadElpvrEvidence(authentication, PROJECT_ID, SITE_ID, null, file);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(monitoringService).uploadElpvrEvidence(USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, null, file);
            // Input: projectId/siteId/file、optional description、JWT
            // Output: uploadElpvrEvidenceの委譲とstatus/bodyを検証
        }

        @Test
        @DisplayName("UT-MC-019: ServiceのファイルエラーstatusCodeがHTTP statusへ変換される")
        void uploadElpvrEvidence_serviceReturnsError_convertsHttpStatus() {
            prepareIdentity();
            MockMultipartFile file = new MockMultipartFile("file", "bad.exe", "application/octet-stream", new byte[] {1});
            FileUploadResponse expected = new FileUploadResponse();
            expected.setStatusCode(ResponseConstants.BAD_REQUEST);
            when(monitoringService.uploadElpvrEvidence(USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, null, file))
                    .thenReturn(expected);

            var response = controller.uploadElpvrEvidence(authentication, PROJECT_ID, SITE_ID, null, file);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: Serviceがファイルエラーを返す
            // Output: ResponseUtilに対応するHTTP status
        }
    }

    @Nested
    @DisplayName("POST /monitoring/upload-elpj-battery-specification-evidence のテスト")
    class UploadElpjBatterySpecificationEvidenceTest {

        @Test
        @DisplayName("UT-MC-020: multipart fileとJWT情報がbattery specification uploadへ渡る")
        void uploadElpjBatterySpecificationEvidence_validMultipart_returnsServiceResponse() {
            prepareIdentity();
            MockMultipartFile file = new MockMultipartFile("file", "battery.pdf", "application/pdf", new byte[] {1, 2});
            FileUploadResponse expected = new FileUploadResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(monitoringService.uploadElpjBatterySpecificationEvidence(
                USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, "Battery", file)).thenReturn(expected);

            var response = controller.uploadElpjBatterySpecificationEvidence(
                authentication, PROJECT_ID, SITE_ID, "Battery", file);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(monitoringService).uploadElpjBatterySpecificationEvidence(
                USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, "Battery", file);
            // Input: projectId/siteId/file、optional description、JWT
            // Output: uploadElpjBatterySpecificationEvidenceの委譲とstatus/bodyを検証
        }

        @Test
        @DisplayName("UT-MC-021: ServiceのファイルエラーstatusCodeがHTTP statusへ変換される")
        void uploadElpjBatterySpecificationEvidence_serviceReturnsError_convertsHttpStatus() {
            prepareIdentity();
            MockMultipartFile file = new MockMultipartFile("file", "bad.exe", "application/octet-stream", new byte[] {1});
            FileUploadResponse expected = new FileUploadResponse();
            expected.setStatusCode(ResponseConstants.UNSUPPORTED_FILE_TYPE);
            when(monitoringService.uploadElpjBatterySpecificationEvidence(
                USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, null, file)).thenReturn(expected);

            var response = controller.uploadElpjBatterySpecificationEvidence(
                authentication, PROJECT_ID, SITE_ID, null, file);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: Serviceがファイルエラーを返す
            // Output: ResponseUtilに対応するHTTP status
        }
    }

    @Nested
    @DisplayName("POST /monitoring/upload-elpj-control-evidence のテスト")
    class UploadElpjControlEvidenceTest {

        @Test
        @DisplayName("UT-MC-022: multipart fileとJWT情報がcontrol uploadへ渡る")
        void uploadElpjControlEvidence_validMultipart_returnsServiceResponse() {
            prepareIdentity();
            MockMultipartFile file = new MockMultipartFile("file", "control.pdf", "application/pdf", new byte[] {1, 2});
            FileUploadResponse expected = new FileUploadResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(monitoringService.uploadElpjControlEvidence(
                USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, null, file)).thenReturn(expected);

            var response = controller.uploadElpjControlEvidence(authentication, PROJECT_ID, SITE_ID, null, file);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(monitoringService).uploadElpjControlEvidence(
                USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, null, file);
            // Input: projectId/siteId/file、optional description、JWT
            // Output: uploadElpjControlEvidenceの委譲とstatus/bodyを検証
        }

        @Test
        @DisplayName("UT-MC-023: ServiceのファイルエラーstatusCodeがHTTP statusへ変換される")
        void uploadElpjControlEvidence_serviceReturnsError_convertsHttpStatus() {
            prepareIdentity();
            MockMultipartFile file = new MockMultipartFile("file", "bad.exe", "application/octet-stream", new byte[] {1});
            FileUploadResponse expected = new FileUploadResponse();
            expected.setStatusCode(ResponseConstants.UNSUPPORTED_FILE_TYPE);
            when(monitoringService.uploadElpjControlEvidence(
                    USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, null, file)).thenReturn(expected);

            var response = controller.uploadElpjControlEvidence(authentication, PROJECT_ID, SITE_ID, null, file);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: Serviceがファイルエラーを返す
            // Output: ResponseUtilに対応するHTTP status
        }
    }

    @Nested
    @DisplayName("POST /monitoring/cancel-request のテスト")
    class CreateCancelRequestTest {

        @Test
        @DisplayName("UT-MC-024: JWT username/companyIdと有効な取消申請がServiceへ渡る")
        void createCancelRequest_validRequest_returnsServiceResponse() {
            prepareIdentity();
            CreateCancelRequestRequest request = CreateCancelRequestRequest.builder()
                    .siteId(SITE_ID)
                    .recordPK("MONITORING#PROJECT#" + PROJECT_ID)
                    .recordSK("SITE#" + SITE_ID + "#2024-01-15")
                    .reasonText("誤入力のため")
                    .build();
            CancelRequestResponse expected = new CancelRequestResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(cancelRequestService.createCancelRequest(USERNAME, COMPANY_ID, request)).thenReturn(expected);

            var response = controller.createCancelRequest(authentication, request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(cancelRequestService).createCancelRequest(USERNAME, COMPANY_ID, request);
            // Input: JWTとsiteId、recordPK、recordSK、reasonTextを満たすrequest
            // Output: createCancelRequest(username, companyId, request)を検証し、status/bodyが返る
        }

        @Test
        @DisplayName("UT-MC-025: 必須項目欠落時は400となりServiceは呼ばれない")
        void createCancelRequest_invalidRequest_returnsValidationError() throws Exception {
            mockMvc().perform(post("/monitoring/cancel-request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cancelRequestService);
            // Input: siteId、recordPK、recordSK、reasonTextのいずれかがnull/blank
            // Output: HTTP 400、cancelRequestService.createCancelRequestは呼ばれない
        }

        @Test
        @DisplayName("UT-MC-026: ServiceのエラーstatusCodeがHTTP statusへ変換される")
        void createCancelRequest_serviceReturnsError_convertsHttpStatus() {
            prepareIdentity();
            CreateCancelRequestRequest request = CreateCancelRequestRequest.builder()
                    .siteId(SITE_ID)
                    .recordPK("MONITORING#PROJECT#" + PROJECT_ID)
                    .recordSK("SITE#" + SITE_ID + "#2024-01-15")
                    .reasonText("誤入力のため")
                    .build();
            CancelRequestResponse expected = new CancelRequestResponse();
            expected.setStatusCode(ResponseConstants.NOT_FOUND);
            when(cancelRequestService.createCancelRequest(USERNAME, COMPANY_ID, request)).thenReturn(expected);

            var response = controller.createCancelRequest(authentication, request);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: ServiceがNOT_FOUND等を返す
            // Output: ResponseUtilに対応するHTTP statusとService body
        }
    }

    @Nested
    @DisplayName("GET /monitoring/cancel-requests のテスト")
    class GetCancelRequestsTest {

        @Test
        @DisplayName("UT-MC-027: CancelRequestServiceの一覧responseがそのまま返る")
        void getCancelRequests_returnsServiceResponse() {
            CancelRequestListResponse expected = new CancelRequestListResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(cancelRequestService.listCancelRequests()).thenReturn(expected);

            var response = controller.getCancelRequests(authentication);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(cancelRequestService).listCancelRequests();
            // Input: listCancelRequests()が一覧を返す
            // Output: HTTP status/bodyがService responseに一致し、Serviceが呼ばれる
            //         controllerではJWT値を抽出しない
        }

        @Test
        @DisplayName("UT-MC-028: ServiceのエラーstatusCodeがHTTP statusへ変換される")
        void getCancelRequests_serviceReturnsError_convertsHttpStatus() {
            CancelRequestListResponse expected = new CancelRequestListResponse();
            expected.setStatusCode(ResponseConstants.BAD_REQUEST);
            when(cancelRequestService.listCancelRequests()).thenReturn(expected);

            var response = controller.getCancelRequests(authentication);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: listCancelRequests()がエラーresponseを返す
            // Output: ResponseUtilに対応するHTTP statusとbody
        }
    }

    @Nested
    @DisplayName("POST /monitoring/{cancelRequestId}/cancel-decision のテスト")
    class MakeDecisionTest {

        @Test
        @DisplayName("UT-MC-029: APPROVE decisionとJWT usernameがServiceへ渡る")
        void makeDecision_approve_returnsServiceResponse() {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
            CancelRequestDecisionRequest request = CancelRequestDecisionRequest.builder()
                    .decision("APPROVE")
                    .managerRemarks("承認")
                    .build();
            CancelRequestDecisionResponse expected = new CancelRequestDecisionResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(cancelRequestService.processDecision(100, request, USERNAME)).thenReturn(expected);

            var response = controller.makeDecision(authentication, 100, request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(cancelRequestService).processDecision(100, request, USERNAME);
            // Input: cancelRequestId、decision=APPROVE、JWT
            // Output: processDecision(id, request, username)を検証し、status/bodyが返る
        }

        @Test
        @DisplayName("UT-MC-030: REJECT decisionとmanagerRemarksがServiceへ渡る")
        void makeDecision_reject_returnsServiceResponse() {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
            CancelRequestDecisionRequest request = CancelRequestDecisionRequest.builder()
                    .decision("REJECT")
                    .managerRemarks("差戻し")
                    .build();
            CancelRequestDecisionResponse expected = new CancelRequestDecisionResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(cancelRequestService.processDecision(101, request, USERNAME)).thenReturn(expected);

            var response = controller.makeDecision(authentication, 101, request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(cancelRequestService).processDecision(101, request, USERNAME);
            // Input: cancelRequestId、decision=REJECT、managerRemarks、JWT
            // Output: processDecisionへの委譲とstatus/bodyを検証
        }

        @Test
        @DisplayName("UT-MC-031: decisionが欠落または未対応値の場合は400となる")
        void makeDecision_invalidRequest_returnsValidationError() throws Exception {
            mockMvc().perform(post("/monitoring/100/cancel-decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"decision\":\"INVALID\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(cancelRequestService);
            // Input: decision=null/blankまたはAPPROVE/REJECT以外
            // Output: HTTP 400、cancelRequestService.processDecisionは呼ばれない
        }

        @Test
        @DisplayName("UT-MC-032: ServiceのエラーstatusCodeがHTTP statusへ変換される")
        void makeDecision_serviceReturnsError_convertsHttpStatus() {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
            CancelRequestDecisionRequest request = CancelRequestDecisionRequest.builder()
                    .decision("APPROVE")
                    .build();
            CancelRequestDecisionResponse expected = new CancelRequestDecisionResponse();
            expected.setStatusCode(ResponseConstants.NOT_FOUND);
            when(cancelRequestService.processDecision(404, request, USERNAME)).thenReturn(expected);

            var response = controller.makeDecision(authentication, 404, request);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: ServiceがNOT_FOUND等を返す
            // Output: ResponseUtilに対応するHTTP statusとService body
        }
    }

    @Nested
    @DisplayName("GET /monitoring/{siteId}/{projectId}/equipment/{equipmentId}/evidence-item のテスト")
    class ReadMonitoringEvidenceItemsTest {

        @Test
        @DisplayName("UT-MC-033: equipment/path情報とJWTがServiceへ渡り、証跡項目が返る")
        void readMonitoringEvidenceItems_validPath_returnsServiceResponse() {
            prepareIdentity();
            MonitoringEvidenceItemsResponse expected = new MonitoringEvidenceItemsResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(monitoringService.readMonitoringEvidenceItems(
                USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, 1)).thenReturn(expected);

            var response = controller.readMonitoringEvidenceItems(PROJECT_ID, SITE_ID, 1, authentication);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isSameAs(expected);
            verify(monitoringService).readMonitoringEvidenceItems(
                USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, 1);
            // Input: siteId、projectId、equipmentId、JWT
            // Output: readMonitoringEvidenceItems(username, companyId, projectId, siteId, equipmentId)
            //         が呼ばれ、HTTP 200とresponse bodyが返る
        }
    }
}
