package com.kct.jcredit.core_backend.controller.ProjectPlanControllerTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.controller.ProjectPlanController;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanBatchProgressResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFileDownloadResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFileResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanProgressResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanProgressSummary;
import com.kct.jcredit.core_backend.dto.response.CorporationMasterResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectMasterResponse;
import com.kct.jcredit.core_backend.service.projectplan.ProjectPlanAutofillService;
import com.kct.jcredit.core_backend.service.projectplan.ProjectPlanFormService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ProjectPlanController API delegation and access-control test outline.
 *
 * Use MockMvc or a method-security-enabled MVC slice for request binding,
 * multipart behavior, and @PreAuthorize checks.
 */
@ExtendWith(MockitoExtension.class)
class ProjectPlanControllerTest {

    private static final String TOKEN = "project-plan-test-token";

    @Mock private ProjectPlanFormService projectPlanFormService;
    @Mock private ProjectPlanAutofillService projectPlanAutofillService;
    @Mock private JwtUtil jwtUtil;
    @Mock private Authentication authentication;

    @InjectMocks
    private ProjectPlanController controller;

    private void prepareAuthentication() {
        when(authentication.getCredentials()).thenReturn(TOKEN);
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP-001");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("operator");
        when(jwtUtil.extractUsername(TOKEN)).thenReturn("operator01");
    }

    private String preAuthorizeValue(String methodName) {
        return Arrays.stream(ProjectPlanController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .orElseThrow()
                .getAnnotation(PreAuthorize.class)
                .value();
    }

    @Nested
    @DisplayName("GET /projectplan/autofill のテスト")
    class GetCorporationMasterTest {

        @Test
        @DisplayName("UT-PPC-001: JWTのcompanyId/role/usernameが法人マスタServiceへ渡る")
        void getCorporationMaster_validAuthentication_returnsServiceResponse() {
            prepareAuthentication();
            CorporationMasterResponse expected = new CorporationMasterResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(projectPlanAutofillService.getCorporationMaster("COMP-001", "operator", "operator01"))
                    .thenReturn(expected);

            var response = controller.getCorporationMaster(authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(projectPlanAutofillService).getCorporationMaster("COMP-001", "operator", "operator01");
            // Input: Authentication.credentialsのtokenとCorporationMasterResponse
            // Output: getCorporationMaster(companyId, role, username)を呼び、ResponseUtilのHTTP statusとbody
        }

        @Test
        @DisplayName("UT-PPC-002: ServiceのNOT_FOUND/FORBIDDENが対応するHTTP statusへ変換される")
        void getCorporationMaster_serviceReturnsError_convertsHttpStatus() {
            prepareAuthentication();
            CorporationMasterResponse expected = new CorporationMasterResponse();
            expected.setStatusCode(ResponseConstants.NOT_FOUND);
            when(projectPlanAutofillService.getCorporationMaster("COMP-001", "operator", "operator01"))
                    .thenReturn(expected);

            var response = controller.getCorporationMaster(authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: ServiceがNOT_FOUNDまたはFORBIDDENを返す
            // Output: HTTP 404/403、response bodyはServiceの戻り値
        }
    }

    @Nested
    @DisplayName("GET /projectplan/autofill/{projectId} のテスト")
    class GetProjectMasterTest {

        @Test
        @DisplayName("UT-PPC-003: projectIdとJWT情報がProject Master Serviceへ渡る")
        void getProjectMaster_validProject_returnsServiceResponse() {
            prepareAuthentication();
            ProjectMasterResponse expected = new ProjectMasterResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(projectPlanAutofillService.getProjectMaster("PROJECT-001", "COMP-001", "operator", "operator01"))
                    .thenReturn(expected);

            var response = controller.getProjectMaster(authentication, "PROJECT-001");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(projectPlanAutofillService).getProjectMaster("PROJECT-001", "COMP-001", "operator", "operator01");
            // Input: projectId、JWTから抽出するcompanyId/role/username
            // Output: getProjectMaster(projectId, companyId, role, username)の呼び出しとresponse status/body
        }

        @Test
        @DisplayName("UT-PPC-004: 対象projectが見つからない場合HTTP 404となる")
        void getProjectMaster_projectNotFound_convertsHttpStatus() {
            prepareAuthentication();
            ProjectMasterResponse expected = new ProjectMasterResponse();
            expected.setStatusCode(ResponseConstants.NOT_FOUND);
            when(projectPlanAutofillService.getProjectMaster("PROJECT-404", "COMP-001", "operator", "operator01"))
                    .thenReturn(expected);

            var response = controller.getProjectMaster(authentication, "PROJECT-404");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: ServiceがNOT_FOUNDを返す
            // Output: HTTP 404とService response body
        }

        @Test
        @DisplayName("UT-PPC-005: projectへのアクセス権がない場合HTTP 403となる")
        void getProjectMaster_forbidden_convertsHttpStatus() {
            prepareAuthentication();
            ProjectMasterResponse expected = new ProjectMasterResponse();
            expected.setStatusCode(ResponseConstants.FORBIDDEN);
            when(projectPlanAutofillService.getProjectMaster("PROJECT-002", "COMP-001", "operator", "operator01"))
                    .thenReturn(expected);

            var response = controller.getProjectMaster(authentication, "PROJECT-002");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: ServiceがFORBIDDENを返す
            // Output: HTTP 403とService response body
        }
    }

    @Nested
    @DisplayName("GET /projectplan/{projectId}/forms/{formSlug} のテスト")
    class GetFormTest {

        @Test
        @DisplayName("UT-PPC-006: methodology指定値とJWT情報をServiceへ渡しフォームを返す")
        void getForm_withMethodology_returnsServiceResponse() {
            prepareAuthentication();
            ProjectPlanFormResponse expected = new ProjectPlanFormResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(projectPlanFormService.getForm("PROJECT-001", "overview", 2, "COMP-001", "operator", "operator01"))
                    .thenReturn(expected);

            var response = controller.getForm(authentication, "PROJECT-001", "overview", 2);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(projectPlanFormService).getForm("PROJECT-001", "overview", 2, "COMP-001", "operator", "operator01");
            // Input: projectId、formSlug、methodology、JWT
            // Output: getForm(projectId, formSlug, methodology, companyId, role, username)を検証
        }

        @Test
        @DisplayName("UT-PPC-007: methodology省略時はnullでServiceへ渡る")
        void getForm_withoutMethodology_passesNullMethodology() {
            prepareAuthentication();
            ProjectPlanFormResponse expected = new ProjectPlanFormResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(projectPlanFormService.getForm("PROJECT-001", "overview", null, "COMP-001", "operator", "operator01"))
                    .thenReturn(expected);

            var response = controller.getForm(authentication, "PROJECT-001", "overview", null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(projectPlanFormService).getForm("PROJECT-001", "overview", null, "COMP-001", "operator", "operator01");
            // Input: methodology query parameterなし
            // Output: getFormへmethodology=nullが渡りresponseが返る
        }

        @Test
        @DisplayName("UT-PPC-008: readOnlyFields/conditionalFieldsを含むフォームresponseが返る")
        void getForm_returnsFormMetadata() {
            prepareAuthentication();
            ProjectPlanFormResponse expected = new ProjectPlanFormResponse();
            expected.setStatusCode(ResponseConstants.OK);
            expected.setFormSlug("overview");
            expected.setDisplayName("Overview");
            expected.setReadOnlyFields(List.of("projectName"));
            when(projectPlanFormService.getForm("PROJECT-001", "overview", null, "COMP-001", "operator", "operator01"))
                    .thenReturn(expected);

            var response = controller.getForm(authentication, "PROJECT-001", "overview", null);

            assertThat(response.getBody().getFormSlug()).isEqualTo("overview");
            assertThat(response.getBody().getReadOnlyFields()).containsExactly("projectName");
            // Input: Serviceがフォーム定義、readOnlyFields、conditionalFieldsを含むresponseを返す
            // Output: response bodyの各値を確認
        }

        @Test
        @DisplayName("UT-PPC-009: ServiceのNOT_FOUND/FORBIDDENがHTTP statusへ変換される")
        void getForm_serviceReturnsError_convertsHttpStatus() {
            prepareAuthentication();
            ProjectPlanFormResponse expected = new ProjectPlanFormResponse();
            expected.setStatusCode(ResponseConstants.FORBIDDEN);
            when(projectPlanFormService.getForm("PROJECT-404", "overview", null, "COMP-001", "operator", "operator01"))
                    .thenReturn(expected);

            var response = controller.getForm(authentication, "PROJECT-404", "overview", null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: ServiceがNOT_FOUNDまたはFORBIDDENを返す
            // Output: HTTP 404/403、bodyはService response
        }
    }

    @Nested
    @DisplayName("PUT /projectplan/{projectId}/forms/{formSlug} のテスト")
    class SaveFormTest {

        @Test
        @DisplayName("UT-PPC-010: JsonNode body、methodology、JWT情報が保存Serviceへ渡る")
        void saveForm_validBody_returnsServiceResponse() {
            prepareAuthentication();
            JsonNode body = new ObjectMapper().createObjectNode().put("projectName", "Project A");
            ProjectPlanFormResponse expected = new ProjectPlanFormResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(projectPlanFormService.saveForm("PROJECT-001", "overview", 2, body,
                "COMP-001", "operator", "operator01")).thenReturn(expected);

            var response = controller.saveForm(authentication, "PROJECT-001", "overview", 2, body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(projectPlanFormService).saveForm("PROJECT-001", "overview", 2, body,
                "COMP-001", "operator", "operator01");
            // Input: projectId、formSlug、methodology、JSON body、JWT
            // Output: saveForm(projectId, formSlug, methodology, body, companyId, role, username)を検証
        }

        @Test
        @DisplayName("UT-PPC-011: methodology省略時はnullでServiceへ渡る")
        void saveForm_withoutMethodology_passesNullMethodology() {
            prepareAuthentication();
            JsonNode body = new ObjectMapper().createObjectNode().put("projectName", "Project A");
            ProjectPlanFormResponse expected = new ProjectPlanFormResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(projectPlanFormService.saveForm("PROJECT-001", "overview", null, body,
                "COMP-001", "operator", "operator01")).thenReturn(expected);

            var response = controller.saveForm(authentication, "PROJECT-001", "overview", null, body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(projectPlanFormService).saveForm("PROJECT-001", "overview", null, body,
                "COMP-001", "operator", "operator01");
            // Input: methodology query parameterなし
            // Output: saveFormへmethodology=nullと同一bodyが渡る
        }

        @Test
        @DisplayName("UT-PPC-012: ページ検証エラーresponseのHTTP status/bodyがそのまま反映される")
        void saveForm_validationFailure_convertsServiceResponse() {
            prepareAuthentication();
            JsonNode body = new ObjectMapper().createObjectNode().put("incomplete", true);
            ProjectPlanFormResponse expected = new ProjectPlanFormResponse();
            expected.setStatusCode(ResponseConstants.VALIDATION_ERROR);
            when(projectPlanFormService.saveForm("PROJECT-001", "overview", null, body,
                    "COMP-001", "operator", "operator01")).thenReturn(expected);

            var response = controller.saveForm(authentication, "PROJECT-001", "overview", null, body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: Serviceがvalidation error responseを返す
            // Output: ResponseUtilによるHTTP statusとService body
            //         Controllerは保存を行わずServiceへ委譲する
        }
    }

    @Nested
    @DisplayName("POST /projectplan/{projectId}/forms/{formSlug}/files のテスト")
    class UploadFormFileTest {

        @Test
        @DisplayName("UT-PPC-013: multipart fieldName/fileとJWT情報が一時アップロードServiceへ渡る")
        void uploadFormFile_validMultipart_returnsServiceResponse() {
            prepareAuthentication();
            MockMultipartFile file = new MockMultipartFile("file", "plan.pdf", "application/pdf", new byte[] {1, 2});
            ProjectPlanFileResponse expected = new ProjectPlanFileResponse();
            expected.setStatusCode(ResponseConstants.OK);
            expected.setS3Key("tmp/project-plan/plan.pdf");
            when(projectPlanFormService.uploadFormFile("PROJECT-001", "overview", "attachment", file,
                "COMP-001", "operator", "operator01")).thenReturn(expected);

            var response = controller.uploadFormFile(authentication, "PROJECT-001", "overview", "attachment", file);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(projectPlanFormService).uploadFormFile("PROJECT-001", "overview", "attachment", file,
                "COMP-001", "operator", "operator01");
            // Input: projectId、formSlug、fieldName、file、JWT
            // Output: uploadFormFileの全引数とService responseを確認
            //         responseのs3Keyは仮アップロードkey
        }

        @Test
        @DisplayName("UT-PPC-014: fieldNameまたはfileが欠落したmultipart requestは400となる")
        void uploadFormFile_missingRequiredPart_returnsBadRequest() throws Exception {
            MockMvcBuilders.standaloneSetup(controller).build()
                .perform(multipart("/projectplan/PROJECT-001/forms/overview/files")
                    .file(new MockMultipartFile("file", "plan.pdf", "application/pdf", new byte[] {1}))
                    .principal(authentication))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(projectPlanFormService);
            // Input: 必須fieldNameまたはfile partなし
            // Output: HTTP 400、uploadFormFileは呼ばれない
        }

        @Test
        @DisplayName("UT-PPC-015: Serviceのupload errorがHTTP statusへ変換される")
        void uploadFormFile_serviceReturnsError_convertsHttpStatus() {
            prepareAuthentication();
            MockMultipartFile file = new MockMultipartFile("file", "plan.exe", "application/octet-stream", new byte[] {1});
            ProjectPlanFileResponse expected = new ProjectPlanFileResponse();
            expected.setStatusCode(ResponseConstants.BAD_REQUEST);
            when(projectPlanFormService.uploadFormFile("PROJECT-001", "overview", "attachment", file,
                    "COMP-001", "operator", "operator01")).thenReturn(expected);

            var response = controller.uploadFormFile(authentication, "PROJECT-001", "overview", "attachment", file);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: Serviceがエラーresponseを返す
            // Output: ResponseUtilに対応するHTTP statusとService body
        }
    }

    @Nested
    @DisplayName("GET /projectplan/{projectId}/files/download-url のテスト")
    class GetFileDownloadUrlTest {

        @Test
        @DisplayName("UT-PPC-016: projectId/s3KeyとJWT情報が署名URL Serviceへ渡る")
        void getFileDownloadUrl_validKey_returnsServiceResponse() {
            prepareAuthentication();
            ProjectPlanFileDownloadResponse expected = new ProjectPlanFileDownloadResponse();
            expected.setStatusCode(ResponseConstants.OK);
            expected.setUrl("https://example.test/file");
            when(projectPlanFormService.getFileDownloadUrl("PROJECT-001", "plans/form/file.pdf",
                "COMP-001", "operator", "operator01")).thenReturn(expected);

            var response = controller.getFileDownloadUrl(authentication, "PROJECT-001", "plans/form/file.pdf");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(projectPlanFormService).getFileDownloadUrl("PROJECT-001", "plans/form/file.pdf",
                "COMP-001", "operator", "operator01");
            // Input: projectId、s3Key、JWT
            // Output: getFileDownloadUrl(projectId, s3Key, companyId, role, username)の委譲とresponse
        }

        @Test
        @DisplayName("UT-PPC-017: 他projectまたは仮key拒否のService responseがstatusへ変換される")
        void getFileDownloadUrl_rejectedKey_convertsServiceResponse() {
            prepareAuthentication();
            ProjectPlanFileDownloadResponse expected = new ProjectPlanFileDownloadResponse();
            expected.setStatusCode(ResponseConstants.FORBIDDEN);
            when(projectPlanFormService.getFileDownloadUrl("PROJECT-001", "tmp/not-saved.pdf",
                    "COMP-001", "operator", "operator01")).thenReturn(expected);

            var response = controller.getFileDownloadUrl(authentication, "PROJECT-001", "tmp/not-saved.pdf");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: ServiceがFORBIDDEN等を返す
            // Output: ResponseUtilに対応するHTTP statusとService body
        }
    }

    @Nested
    @DisplayName("GET /projectplan/{projectId}/progress のテスト")
    class GetProgressTest {

        @Test
        @DisplayName("UT-PPC-018: project progressとJWT情報がServiceへ渡る")
        void getProgress_validProject_returnsServiceResponse() {
            prepareAuthentication();
            ProjectPlanProgressResponse expected = new ProjectPlanProgressResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(projectPlanFormService.getProgress("PROJECT-001", "COMP-001", "operator", "operator01"))
                    .thenReturn(expected);

            var response = controller.getProgress(authentication, "PROJECT-001");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(projectPlanFormService).getProgress("PROJECT-001", "COMP-001", "operator", "operator01");
            // Input: projectId、JWT
            // Output: getProgress(projectId, companyId, role, username)の委譲とprogress response
        }

        @Test
        @DisplayName("UT-PPC-019: 完了数とnextIncompleteSlugがprogress responseに含まれる")
        void getProgress_returnsResumeMetadata() {
            prepareAuthentication();
            ProjectPlanProgressResponse expected = new ProjectPlanProgressResponse();
            expected.setStatusCode(ResponseConstants.OK);
            expected.setTotalForms(4);
            expected.setCompletedForms(2);
            expected.setNextIncompleteSlug("implementation-plan");
            when(projectPlanFormService.getProgress("PROJECT-001", "COMP-001", "operator", "operator01"))
                    .thenReturn(expected);

            var response = controller.getProgress(authentication, "PROJECT-001");

            assertThat(response.getBody().getTotalForms()).isEqualTo(4);
            assertThat(response.getBody().getCompletedForms()).isEqualTo(2);
            assertThat(response.getBody().getNextIncompleteSlug()).isEqualTo("implementation-plan");
            // Input: Service response with totalForms/completedForms/nextIncompleteSlug/forms
            // Output: resume information and optional page flags are preserved
        }

        @Test
        @DisplayName("UT-PPC-020: ServiceのアクセスエラーがHTTP statusへ変換される")
        void getProgress_serviceReturnsError_convertsHttpStatus() {
            prepareAuthentication();
            ProjectPlanProgressResponse expected = new ProjectPlanProgressResponse();
            expected.setStatusCode(ResponseConstants.NOT_FOUND);
            when(projectPlanFormService.getProgress("PROJECT-404", "COMP-001", "operator", "operator01"))
                    .thenReturn(expected);

            var response = controller.getProgress(authentication, "PROJECT-404");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isSameAs(expected);
            // Input: ServiceがNOT_FOUNDまたはFORBIDDENを返す
            // Output: HTTP 404/403、bodyはService response
        }
    }

    @Nested
    @DisplayName("GET /projectplan/progress のテスト")
    class GetProgressForProjectsTest {

        @Test
        @DisplayName("UT-PPC-021: projectIds一覧とJWT情報が一括progress Serviceへ渡る")
        void getProgressForProjects_validProjectIds_returnsBatchResponse() {
            prepareAuthentication();
            List<String> projectIds = List.of("PROJECT-001", "PROJECT-002");
            ProjectPlanBatchProgressResponse expected = new ProjectPlanBatchProgressResponse();
            expected.setStatusCode(ResponseConstants.OK);
            when(projectPlanFormService.getProgressForProjects(projectIds, "COMP-001", "operator", "operator01"))
                    .thenReturn(expected);

            var response = controller.getProgressForProjects(authentication, projectIds);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(expected);
            verify(projectPlanFormService).getProgressForProjects(projectIds, "COMP-001", "operator", "operator01");
            // Input: repeated projectIds query valuesとJWT
            // Output: getProgressForProjects(projectIds, companyId, role, username)を検証
        }

        @Test
        @DisplayName("UT-PPC-022: projectごとのprogress summaryがbatch responseに含まれる")
        void getProgressForProjects_preservesProjectSummaries() {
            prepareAuthentication();
            List<String> projectIds = List.of("PROJECT-001", "PROJECT-002");
            ProjectPlanBatchProgressResponse expected = new ProjectPlanBatchProgressResponse();
            expected.setStatusCode(ResponseConstants.OK);
            expected.setProjects(List.of(
                new ProjectPlanProgressSummary("PROJECT-001", 4, 4, null, true),
                new ProjectPlanProgressSummary("PROJECT-002", 4, 2, "overview", false)));
            when(projectPlanFormService.getProgressForProjects(projectIds, "COMP-001", "operator", "operator01"))
                .thenReturn(expected);

            var response = controller.getProgressForProjects(authentication, projectIds);

            assertThat(response.getBody().getProjects()).hasSize(2);
            assertThat(response.getBody().getProjects().get(1).getNextIncompleteSlug()).isEqualTo("overview");
            // Input: Serviceが複数ProjectPlanProgressSummaryを返す
            // Output: projects listと各summaryが保持される
        }

        @Test
        @DisplayName("UT-PPC-023: 一覧中に参照不可projectがあってもService responseを返す")
        void getProgressForProjects_serviceOmitsInaccessibleProjects_returnsPartialBatch() {
            prepareAuthentication();
            List<String> projectIds = List.of("PROJECT-001", "PROJECT-404");
            ProjectPlanBatchProgressResponse expected = new ProjectPlanBatchProgressResponse();
            expected.setStatusCode(ResponseConstants.OK);
            expected.setProjects(List.of(new ProjectPlanProgressSummary("PROJECT-001", 4, 1, "overview", false)));
            when(projectPlanFormService.getProgressForProjects(projectIds, "COMP-001", "operator", "operator01"))
                    .thenReturn(expected);

            var response = controller.getProgressForProjects(authentication, projectIds);

            assertThat(response.getBody().getProjects()).hasSize(1);
            assertThat(response.getBody().getProjects().get(0).getProjectId()).isEqualTo("PROJECT-001");
            // Input: request IDsに参照可能/不可projectを含み、Serviceは参照可能分を返す
            // Output: controllerはServiceに一括委譲し、部分batch responseをそのまま返す
        }
    }

    @Nested
    @DisplayName("ProjectPlanController method security のテスト")
    class MethodSecurityTest {

        @Test
        @DisplayName("UT-PPC-024: PROJECT_READなしのoperatorはread APIへアクセスできない")
        void readEndpoint_operatorWithoutProjectRead_isForbidden() {
            for (String methodName : List.of("getCorporationMaster", "getProjectMaster", "getForm",
                    "getFileDownloadUrl", "getProgress", "getProgressForProjects")) {
                assertThat(preAuthorizeValue(methodName)).contains("PROJECT_READ");
            }
            verifyNoInteractions(projectPlanFormService, projectPlanAutofillService);
            // Input: operator principal without PROJECT_READ
            // Output: HTTP 403、該当read endpointのServiceは呼ばれない
        }

        @Test
        @DisplayName("UT-PPC-025: PROJECT_UPDATEなしのoperatorはsave/upload APIへアクセスできない")
        void writeEndpoint_operatorWithoutProjectUpdate_isForbidden() {
            assertThat(preAuthorizeValue("saveForm")).contains("PROJECT_UPDATE");
            assertThat(preAuthorizeValue("uploadFormFile")).contains("PROJECT_UPDATE");
            verifyNoInteractions(projectPlanFormService);
            // Input: operator principal without PROJECT_UPDATE
            // Output: HTTP 403、saveForm/uploadFormFileは呼ばれない
        }

        @Test
        @DisplayName("UT-PPC-026: adminはPROJECT_READ/PROJECT_UPDATE permissionなしで許可される")
        void admin_canReadAndUpdateWithoutOperatorPermissions() {
            for (String methodName : List.of("getCorporationMaster", "getProjectMaster", "getForm",
                    "getFileDownloadUrl", "getProgress", "getProgressForProjects", "saveForm", "uploadFormFile")) {
                assertThat(preAuthorizeValue(methodName)).contains("hasRole('admin')");
            }
            // Input: admin principal
            // Output: read and write routes pass method security and delegate
        }
    }
}
