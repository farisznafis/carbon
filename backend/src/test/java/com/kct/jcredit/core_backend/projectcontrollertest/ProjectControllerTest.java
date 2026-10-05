package com.kct.jcredit.core_backend.projectcontrollertest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.controller.ProjectController;
import com.kct.jcredit.core_backend.dto.request.CreateProjectRequest;
import com.kct.jcredit.core_backend.dto.request.UpdateProjectRequest;
import com.kct.jcredit.core_backend.dto.request.UpdateProjectStatusRequest;
import com.kct.jcredit.core_backend.dto.response.CreateProjectResponse;
import com.kct.jcredit.core_backend.dto.response.MemberTermsInfoResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectCompanyListResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectFileDownloadResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectFileListResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectFileResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectListInfoResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectMonitoringResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectPdfDownloadResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectResponse;
import com.kct.jcredit.core_backend.dto.response.UpdateProjectStatusResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.enums.ProjectFileType;
import com.kct.jcredit.core_backend.service.ParticipationProjectService;
import com.kct.jcredit.core_backend.service.ProjectFileService;
import com.kct.jcredit.core_backend.service.ProjectMonitoringService;
import com.kct.jcredit.core_backend.service.ProjectService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;

// NOTE: response DTOs and request DTOs are mocked rather than built, so the tests do not
// depend on constructor or builder shapes that have not been verified here. For request
// DTOs that is deliberate: the controller fills them from the token, and a mock lets those
// setter calls be verified directly.
// NOTE: ResponseUtil.getHttpStatus is stubbed per test. The mapping itself belongs to
// ResponseUtil's own tests; what is verified here is that the controller uses whatever
// that mapping returns.
// ASSUMPTION: FILE_TYPE uses values()[0] because the ProjectFileType constants have not
// been verified here. Replace with the real constant once confirmed.
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectController のテスト")
class ProjectControllerTest {

    @Mock private ProjectService projectService;
    @Mock private JwtUtil jwtUtil;
    @Mock private ProjectFileService projectFileService;
    @Mock private ProjectMonitoringService projectMonitoringService;
    @Mock private ParticipationProjectService participationProjectService;
    @Mock private Authentication authentication;

    @InjectMocks
    private ProjectController controller;

    private MockedStatic<ResponseUtil> responseUtil;

    private static final String TOKEN = "dummy-token";
    private static final String COMPANY_ID = "OP-0001";
    private static final String USERNAME = "operator001";
    private static final String ROLE_MASTER = "master";
    private static final String PROJECT_ID = "PUB-PJ-001";
    private static final Integer FILE_ID = 77;
    private static final Integer PARTICIPATION_ID = 55;
    private static final ProjectFileType FILE_TYPE = ProjectFileType.values()[0];

    @BeforeEach
    void setUp() {
        responseUtil = mockStatic(ResponseUtil.class);
    }

    @AfterEach
    void tearDown() {
        responseUtil.close();
    }

    // ─────────────────────────────────────────────────────────────
    // Fixtures
    // ─────────────────────────────────────────────────────────────

    private void stubToken() {
        when(authentication.getCredentials()).thenReturn(TOKEN);
    }

    private void stubCompanyAndUsername() {
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
    }

    private void stubStatus(String statusCode, HttpStatus httpStatus) {
        responseUtil.when(() -> ResponseUtil.getHttpStatus(statusCode)).thenReturn(httpStatus);
    }

    // ==================================================================
    // 01. createProject
    // ==================================================================

    @Test
    @DisplayName("UT-PC-01-01: 正常 - 作成が成功し、トークン由来の会社IDと作成者がリクエストに設定される")
    void createProject_validRequest_fillsOperatorCompanyAndCreatedByFromToken() {
        stubToken();
        stubCompanyAndUsername();
        CreateProjectRequest request = mock(CreateProjectRequest.class);

        CreateProjectResponse serviceResponse = mock(CreateProjectResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectService.createProject(request)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<CreateProjectResponse> result =
                controller.createProject(authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);

        // Both values come from the token, not from the request body, so a caller cannot
        // create a project under another operator company by crafting the payload.
        verify(request).setOperatorCompanyId(COMPANY_ID);
        verify(request).setCreatedBy(USERNAME);
    }

    @Test
    @DisplayName("UT-PC-01-02: 異常 - 重複データでCONFLICTが対応するHTTPステータスに変換される")
    void createProject_duplicateData_mapsToConflict() {
        stubToken();
        stubCompanyAndUsername();
        CreateProjectRequest request = mock(CreateProjectRequest.class);

        CreateProjectResponse serviceResponse = mock(CreateProjectResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.CONFLICT);
        when(projectService.createProject(request)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.CONFLICT, HttpStatus.CONFLICT);

        ResponseEntity<CreateProjectResponse> result =
                controller.createProject(authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody().getStatusCode()).isEqualTo(ResponseConstants.CONFLICT);
    }

    @Test
    @DisplayName("UT-PC-01-03: 異常 - トークンが無効な場合、作成者の設定もサービス呼び出しも行われない")
    void createProject_invalidToken_serviceNotCalled() {
        stubToken();
        when(jwtUtil.extractCompanyId(TOKEN))
                .thenThrow(new RuntimeException("invalid token"));
        CreateProjectRequest request = mock(CreateProjectRequest.class);

        assertThrows(RuntimeException.class,
                () -> controller.createProject(authentication, request));

        verify(request, never()).setCreatedBy(anyString());
        verifyNoInteractions(projectService);
    }

    @Test
    @DisplayName("UT-PC-01-04: 正常 - 会社ID設定→作成者設定→サービス呼び出しの順序で実行される")
    void createProject_fillsRequestBeforeCallingService() {
        stubToken();
        stubCompanyAndUsername();
        CreateProjectRequest request = mock(CreateProjectRequest.class);

        CreateProjectResponse serviceResponse = mock(CreateProjectResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectService.createProject(request)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        controller.createProject(authentication, request);

        // The service must receive a request that is already filled in; a later call order
        // would hand it a payload still carrying whatever the client sent.
        InOrder inOrder = inOrder(request, projectService);
        inOrder.verify(request).setOperatorCompanyId(COMPANY_ID);
        inOrder.verify(request).setCreatedBy(USERNAME);
        inOrder.verify(projectService).createProject(request);
    }

    // ==================================================================
    // 02. updateProject
    // ==================================================================

    @Test
    @DisplayName("UT-PC-02-01: 正常 - 更新が成功し、パスのprojectIdとトークン由来の値がリクエストに設定される")
    void updateProject_validRequest_fillsIdCompanyAndProcessedBy() {
        stubToken();
        stubCompanyAndUsername();
        UpdateProjectRequest request = mock(UpdateProjectRequest.class);

        ProjectResponse serviceResponse = mock(ProjectResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectService.updateProject(request)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<ProjectResponse> result =
                controller.updateProject(authentication, PROJECT_ID, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);
        verify(request).setId(PROJECT_ID);
        verify(request).setCompanyId(COMPANY_ID);
        verify(request).setProcessedBy(USERNAME);
    }

    @Test
    @DisplayName("UT-PC-02-02: 異常 - プロジェクトが存在しない場合NOT_FOUNDが変換される")
    void updateProject_projectNotFound_mapsToNotFound() {
        stubToken();
        stubCompanyAndUsername();
        UpdateProjectRequest request = mock(UpdateProjectRequest.class);

        ProjectResponse serviceResponse = mock(ProjectResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.NOT_FOUND);
        when(projectService.updateProject(request)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.NOT_FOUND, HttpStatus.NOT_FOUND);

        ResponseEntity<ProjectResponse> result =
                controller.updateProject(authentication, PROJECT_ID, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("UT-PC-02-03: 異常 - トークンが無効な場合、処理者の設定もサービス呼び出しも行われない")
    void updateProject_invalidToken_serviceNotCalled() {
        stubToken();
        when(jwtUtil.extractCompanyId(TOKEN))
                .thenThrow(new RuntimeException("invalid token"));
        UpdateProjectRequest request = mock(UpdateProjectRequest.class);

        assertThrows(RuntimeException.class,
                () -> controller.updateProject(authentication, PROJECT_ID, request));

        verify(request, never()).setProcessedBy(anyString());
        verifyNoInteractions(projectService);
    }

    @Test
    @DisplayName("UT-PC-02-04: 正常 - ボディ側のidではなくパスのprojectIdが使われる")
    void updateProject_pathIdOverridesBody() {
        // The body may carry any id; the controller overwrites it from the path so a caller
        // cannot aim the update at a different project than the URL says.
        stubToken();
        stubCompanyAndUsername();
        UpdateProjectRequest request = mock(UpdateProjectRequest.class);
        lenient().when(request.getId()).thenReturn("PUB-PJ-OTHER");

        ProjectResponse serviceResponse = mock(ProjectResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectService.updateProject(request)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        controller.updateProject(authentication, PROJECT_ID, request);

        verify(request).setId(PROJECT_ID);
        verify(request, never()).setId("PUB-PJ-OTHER");
    }

    // ==================================================================
    // 03. updateProjectStatus
    // ==================================================================

    @Test
    @DisplayName("UT-PC-03-01: 正常 - ステータス更新が成功し、3引数(status, projectId, processedBy)の順序で呼ばれる")
    void updateProjectStatus_valid_passesArgumentsInOrder() {
        stubToken();
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
        UpdateProjectStatusRequest request = mock(UpdateProjectStatusRequest.class);
        when(request.getProjectStatus()).thenReturn("公開");

        UpdateProjectStatusResponse serviceResponse = mock(UpdateProjectStatusResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectService.updateProjectStatus("公開", PROJECT_ID, USERNAME))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<UpdateProjectStatusResponse> result =
                controller.updateProjectStatus(authentication, PROJECT_ID, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);

        // status and processedBy are both Strings, so a swap would compile silently.
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(projectService).updateProjectStatus(
                captor.capture(), captor.capture(), captor.capture());
        assertThat(captor.getAllValues()).containsExactly("公開", PROJECT_ID, USERNAME);
    }

    @Test
    @DisplayName("UT-PC-03-02: 異常 - プロジェクトが存在しない場合NOT_FOUNDが変換される")
    void updateProjectStatus_projectNotFound_mapsToNotFound() {
        stubToken();
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
        UpdateProjectStatusRequest request = mock(UpdateProjectStatusRequest.class);
        when(request.getProjectStatus()).thenReturn("公開");

        UpdateProjectStatusResponse serviceResponse = mock(UpdateProjectStatusResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.NOT_FOUND);
        when(projectService.updateProjectStatus("公開", PROJECT_ID, USERNAME))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.NOT_FOUND, HttpStatus.NOT_FOUND);

        ResponseEntity<UpdateProjectStatusResponse> result =
                controller.updateProjectStatus(authentication, PROJECT_ID, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("UT-PC-03-03: 異常 - 同一ステータスへの変更でBAD_REQUESTが変換される")
    void updateProjectStatus_sameStatus_mapsToBadRequest() {
        stubToken();
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
        UpdateProjectStatusRequest request = mock(UpdateProjectStatusRequest.class);
        when(request.getProjectStatus()).thenReturn("公開");

        UpdateProjectStatusResponse serviceResponse = mock(UpdateProjectStatusResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.BAD_REQUEST);
        when(projectService.updateProjectStatus("公開", PROJECT_ID, USERNAME))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.BAD_REQUEST, HttpStatus.BAD_REQUEST);

        ResponseEntity<UpdateProjectStatusResponse> result =
                controller.updateProjectStatus(authentication, PROJECT_ID, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("UT-PC-03-04: 異常 - 無効化されたプロジェクトでFORBIDDENが変換される")
    void updateProjectStatus_inactiveProject_mapsToForbidden() {
        stubToken();
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
        UpdateProjectStatusRequest request = mock(UpdateProjectStatusRequest.class);
        when(request.getProjectStatus()).thenReturn("公開");

        UpdateProjectStatusResponse serviceResponse = mock(UpdateProjectStatusResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.FORBIDDEN);
        when(projectService.updateProjectStatus("公開", PROJECT_ID, USERNAME))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.FORBIDDEN, HttpStatus.FORBIDDEN);

        ResponseEntity<UpdateProjectStatusResponse> result =
                controller.updateProjectStatus(authentication, PROJECT_ID, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("UT-PC-03-05: 異常 - トークンが無効な場合サービスは呼ばれない")
    void updateProjectStatus_invalidToken_serviceNotCalled() {
        stubToken();
        when(jwtUtil.extractUsername(TOKEN))
                .thenThrow(new RuntimeException("invalid token"));
        UpdateProjectStatusRequest request = mock(UpdateProjectStatusRequest.class);

        assertThrows(RuntimeException.class,
                () -> controller.updateProjectStatus(authentication, PROJECT_ID, request));

        verifyNoInteractions(projectService);
    }

    // ==================================================================
    // 04. getProject
    // ==================================================================

    @Test
    @DisplayName("UT-PC-04-01: 正常 - プロジェクト取得が成功し、トークン由来のcompanyIdが渡される")
    void getProject_validToken_passesCompanyIdFromToken() {
        stubToken();
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);

        ProjectResponse serviceResponse = mock(ProjectResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectService.getProject(PROJECT_ID, COMPANY_ID)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<ProjectResponse> result =
                controller.getProject(authentication, PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);
        verify(projectService, times(1)).getProject(PROJECT_ID, COMPANY_ID);
    }

    @Test
    @DisplayName("UT-PC-04-02: 異常 - プロジェクトが存在しない場合NOT_FOUNDが変換される")
    void getProject_notFound_mapsToNotFound() {
        stubToken();
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);

        ProjectResponse serviceResponse = mock(ProjectResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.NOT_FOUND);
        when(projectService.getProject(PROJECT_ID, COMPANY_ID)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.NOT_FOUND, HttpStatus.NOT_FOUND);

        ResponseEntity<ProjectResponse> result =
                controller.getProject(authentication, PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("UT-PC-04-03: 異常 - トークンが無効な場合サービスは呼ばれない")
    void getProject_invalidToken_serviceNotCalled() {
        stubToken();
        when(jwtUtil.extractCompanyId(TOKEN))
                .thenThrow(new RuntimeException("invalid token"));

        assertThrows(RuntimeException.class,
                () -> controller.getProject(authentication, PROJECT_ID));

        verifyNoInteractions(projectService);
    }

    // ==================================================================
    // 05. upload
    // ==================================================================

    @Test
    @DisplayName("UT-PC-05-01: 正常 - アップロードが成功し、4引数(projectId, fileType, file, uploadedBy)の順序で呼ばれる")
    void upload_validFile_passesArgumentsInOrder() {
        stubToken();
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
        MultipartFile file = mock(MultipartFile.class);

        ProjectFileResponse serviceResponse = mock(ProjectFileResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectFileService.uploadFile(PROJECT_ID, FILE_TYPE, file, USERNAME))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<ProjectFileResponse> result =
                controller.upload(PROJECT_ID, FILE_TYPE, file, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);
        // uploadedBy comes from the token, not from a request field.
        verify(projectFileService, times(1))
                .uploadFile(PROJECT_ID, FILE_TYPE, file, USERNAME);
    }

    @Test
    @DisplayName("UT-PC-05-02: 異常 - ファイルサイズ超過でFILE_TOO_LARGEが変換される")
    void upload_fileTooLarge_mapsToHttpStatus() {
        stubToken();
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
        MultipartFile file = mock(MultipartFile.class);

        ProjectFileResponse serviceResponse = mock(ProjectFileResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.FILE_TOO_LARGE);
        when(projectFileService.uploadFile(PROJECT_ID, FILE_TYPE, file, USERNAME))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.FILE_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE);

        ResponseEntity<ProjectFileResponse> result =
                controller.upload(PROJECT_ID, FILE_TYPE, file, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(result.getBody().getStatusCode())
                .isEqualTo(ResponseConstants.FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("UT-PC-05-03: 異常 - 非対応形式でUNSUPPORTED_FILE_TYPEが変換される")
    void upload_unsupportedType_mapsToHttpStatus() {
        stubToken();
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
        MultipartFile file = mock(MultipartFile.class);

        ProjectFileResponse serviceResponse = mock(ProjectFileResponse.class);
        when(serviceResponse.getStatusCode())
                .thenReturn(ResponseConstants.UNSUPPORTED_FILE_TYPE);
        when(projectFileService.uploadFile(PROJECT_ID, FILE_TYPE, file, USERNAME))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.UNSUPPORTED_FILE_TYPE,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);

        ResponseEntity<ProjectFileResponse> result =
                controller.upload(PROJECT_ID, FILE_TYPE, file, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    @DisplayName("UT-PC-05-04: 異常 - トークンが無効な場合、ファイルは保存されない")
    void upload_invalidToken_fileServiceNotCalled() {
        stubToken();
        when(jwtUtil.extractUsername(TOKEN))
                .thenThrow(new RuntimeException("invalid token"));
        MultipartFile file = mock(MultipartFile.class);

        assertThrows(RuntimeException.class,
                () -> controller.upload(PROJECT_ID, FILE_TYPE, file, authentication));

        verifyNoInteractions(projectFileService);
    }

    // ==================================================================
    // 06. list (files)
    // ==================================================================

    @Test
    @DisplayName("UT-PC-06-01: 正常 - fileType指定ありで一覧が取得できる")
    void listFiles_withFileType_passesFilter() {
        ProjectFileListResponse serviceResponse = mock(ProjectFileListResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectFileService.listFiles(PROJECT_ID, FILE_TYPE)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<ProjectFileListResponse> result =
                controller.list(PROJECT_ID, FILE_TYPE);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);
    }

    @Test
    @DisplayName("UT-PC-06-02: 正常 - fileType未指定の場合nullのままサービスに渡される")
    void listFiles_withoutFileType_passesNull() {
        // The parameter is optional; the controller must not substitute a default, because
        // "no filter" and "filter by the first type" are different requests.
        ProjectFileListResponse serviceResponse = mock(ProjectFileListResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectFileService.listFiles(PROJECT_ID, null)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<ProjectFileListResponse> result = controller.list(PROJECT_ID, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(projectFileService).listFiles(PROJECT_ID, null);
    }

    @Test
    @DisplayName("UT-PC-06-03: 異常 - プロジェクトが存在しない場合NOT_FOUNDが変換される")
    void listFiles_projectNotFound_mapsToNotFound() {
        ProjectFileListResponse serviceResponse = mock(ProjectFileListResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.NOT_FOUND);
        when(projectFileService.listFiles(PROJECT_ID, null)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.NOT_FOUND, HttpStatus.NOT_FOUND);

        ResponseEntity<ProjectFileListResponse> result = controller.list(PROJECT_ID, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ==================================================================
    // 07. download
    // ==================================================================

    @Test
    @DisplayName("UT-PC-07-01: 正常 - ダウンロードURLが取得できる")
    void download_validFile_returnsUrl() {
        ProjectFileDownloadResponse serviceResponse = mock(ProjectFileDownloadResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectFileService.getDownloadUrl(PROJECT_ID, FILE_ID)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<ProjectFileDownloadResponse> result =
                controller.download(PROJECT_ID, FILE_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);
        verify(projectFileService, times(1)).getDownloadUrl(PROJECT_ID, FILE_ID);
    }

    @Test
    @DisplayName("UT-PC-07-02: 異常 - ファイルが存在しない場合NOT_FOUNDが変換される")
    void download_fileNotFound_mapsToNotFound() {
        ProjectFileDownloadResponse serviceResponse = mock(ProjectFileDownloadResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.NOT_FOUND);
        when(projectFileService.getDownloadUrl(PROJECT_ID, FILE_ID)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.NOT_FOUND, HttpStatus.NOT_FOUND);

        ResponseEntity<ProjectFileDownloadResponse> result =
                controller.download(PROJECT_ID, FILE_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("UT-PC-07-03: 準正常 - 呼び出し元を示す情報がサービスに渡らない（アーキテクチャ上の注記）")
    void download_passesNoCallerIdentity() {
        // NOTE: this endpoint takes no Authentication, so the service cannot record who
        // downloaded a review document, nor scope the file to a caller. Acceptable while
        // the endpoint is admin/operator only; it would need revisiting if participating
        // company roles are ever added, as happened with the membership PDF.
        ProjectFileDownloadResponse serviceResponse = mock(ProjectFileDownloadResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectFileService.getDownloadUrl(PROJECT_ID, FILE_ID)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        controller.download(PROJECT_ID, FILE_ID);

        verifyNoInteractions(jwtUtil);
    }

    // ==================================================================
    // 08. delete
    // ==================================================================

    @Test
    @DisplayName("UT-PC-08-01: 正常 - 削除が成功し、トークン由来の削除者が渡される")
    void deleteFile_valid_passesDeletedByFromToken() {
        stubToken();
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);

        BaseResponse serviceResponse = mock(BaseResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectFileService.deleteFile(PROJECT_ID, FILE_ID, USERNAME))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<BaseResponse> result =
                controller.delete(PROJECT_ID, FILE_ID, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);
        verify(projectFileService, times(1)).deleteFile(PROJECT_ID, FILE_ID, USERNAME);
    }

    @Test
    @DisplayName("UT-PC-08-02: 異常 - ファイルが存在しない場合NOT_FOUNDが変換される")
    void deleteFile_notFound_mapsToNotFound() {
        stubToken();
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);

        BaseResponse serviceResponse = mock(BaseResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.NOT_FOUND);
        when(projectFileService.deleteFile(PROJECT_ID, FILE_ID, USERNAME))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.NOT_FOUND, HttpStatus.NOT_FOUND);

        ResponseEntity<BaseResponse> result =
                controller.delete(PROJECT_ID, FILE_ID, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("UT-PC-08-03: 異常 - トークンが無効な場合、ファイルは削除されない")
    void deleteFile_invalidToken_fileServiceNotCalled() {
        stubToken();
        when(jwtUtil.extractUsername(TOKEN))
                .thenThrow(new RuntimeException("invalid token"));

        assertThrows(RuntimeException.class,
                () -> controller.delete(PROJECT_ID, FILE_ID, authentication));

        verifyNoInteractions(projectFileService);
    }

    // ==================================================================
    // 09. listProjects
    // ==================================================================

    @Test
    @DisplayName("UT-PC-09-01: 正常 - 11個の検索条件がすべて同じ順序でサービスに渡される")
    void listProjects_allFilters_passedInOrder() {
        LocalDate createdFrom = LocalDate.of(2026, 1, 1);
        LocalDate createdTo = LocalDate.of(2026, 3, 31);
        LocalDate updatedFrom = LocalDate.of(2026, 4, 1);
        LocalDate updatedTo = LocalDate.of(2026, 6, 30);

        ProjectListInfoResponse serviceResponse = mock(ProjectListInfoResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectService.findByFilter(2, 50, "公開", "EN-R-002",
                createdFrom, createdTo, updatedFrom, updatedTo,
                "太陽光", "createdAt", "asc")).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<ProjectListInfoResponse> result = controller.listProjects(
                2, 50, "公開", "EN-R-002",
                createdFrom, createdTo, updatedFrom, updatedTo,
                "太陽光", "createdAt", "asc");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);
        // Four adjacent LocalDate parameters and three adjacent Strings: any swap among
        // them would compile, so the exact argument list is asserted.
        verify(projectService, times(1)).findByFilter(2, 50, "公開", "EN-R-002",
                createdFrom, createdTo, updatedFrom, updatedTo,
                "太陽光", "createdAt", "asc");
    }

    @Test
    @DisplayName("UT-PC-09-02: 正常 - 既定値(page=1, limit=20, sort=updatedAt, sortOrder=desc)がそのまま渡される")
    void listProjects_defaults_passedThrough() {
        ProjectListInfoResponse serviceResponse = mock(ProjectListInfoResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectService.findByFilter(1, 20, null, null, null, null, null, null,
                null, "updatedAt", "desc")).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        // Spring applies @RequestParam defaults before the method runs, so the values the
        // framework would supply are passed in explicitly here.
        ResponseEntity<ProjectListInfoResponse> result = controller.listProjects(
                1, 20, null, null, null, null, null, null, null, "updatedAt", "desc");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(projectService).findByFilter(1, 20, null, null, null, null, null, null,
                null, "updatedAt", "desc");
    }

    @Test
    @DisplayName("UT-PC-09-03: 正常 - 未指定の絞り込み条件はnullのまま渡され、既定値に置き換えられない")
    void listProjects_absentFilters_stayNull() {
        ProjectListInfoResponse serviceResponse = mock(ProjectListInfoResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectService.findByFilter(1, 20, "公開", null, null, null, null, null,
                null, "updatedAt", "desc")).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        controller.listProjects(1, 20, "公開", null, null, null, null, null,
                null, "updatedAt", "desc");

        // "not filtered" must reach the service as null; substituting a blank string would
        // change the query the specification builds.
        verify(projectService).findByFilter(1, 20, "公開", null, null, null, null, null,
                null, "updatedAt", "desc");
    }

    @Test
    @DisplayName("UT-PC-09-04: 異常 - サービスがエラーを返す場合、対応するHTTPステータスに変換される")
    void listProjects_serviceError_mapsToHttpStatus() {
        ProjectListInfoResponse serviceResponse = mock(ProjectListInfoResponse.class);
        when(serviceResponse.getStatusCode())
                .thenReturn(ResponseConstants.INTERNAL_SERVER_ERROR);
        when(projectService.findByFilter(1, 20, null, null, null, null, null, null,
                null, "updatedAt", "desc")).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR);

        ResponseEntity<ProjectListInfoResponse> result = controller.listProjects(
                1, 20, null, null, null, null, null, null, null, "updatedAt", "desc");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("UT-PC-09-05: 準正常 - @Min・@Patternは単体テストでは評価されない（アーキテクチャ上の注記）")
    void listProjects_beanValidationIsNotEvaluatedHere() {
        // NOTE: page >= 1, limit >= 1 and sortOrder matching ^(asc|desc)$ are enforced by
        // the @Validated proxy, which a direct method call bypasses. page = 0 therefore
        // reaches the service untouched here, and would reach PageRequest.of(-1, ...) in
        // production only if the proxy were absent. Covering these needs @WebMvcTest.
        ProjectListInfoResponse serviceResponse = mock(ProjectListInfoResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectService.findByFilter(0, 20, null, null, null, null, null, null,
                null, "updatedAt", "invalid")).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        controller.listProjects(0, 20, null, null, null, null, null, null,
                null, "updatedAt", "invalid");

        verify(projectService).findByFilter(0, 20, null, null, null, null, null, null,
                null, "updatedAt", "invalid");
    }

    // ==================================================================
    // 10. getProjectMonitoringDetails
    // ==================================================================

    @Test
    @DisplayName("UT-PC-10-01: 正常 - モニタリング情報が取得できる")
    void getMonitoring_valid_returnsDetails() {
        ProjectMonitoringResponse serviceResponse = mock(ProjectMonitoringResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectMonitoringService.getProjectDetail(PROJECT_ID)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<ProjectMonitoringResponse> result =
                controller.getProjectMonitoringDetails(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);
        verify(projectMonitoringService, times(1)).getProjectDetail(PROJECT_ID);
    }

    @Test
    @DisplayName("UT-PC-10-02: 異常 - プロジェクトが存在しない場合NOT_FOUNDが変換される")
    void getMonitoring_notFound_mapsToNotFound() {
        ProjectMonitoringResponse serviceResponse = mock(ProjectMonitoringResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.NOT_FOUND);
        when(projectMonitoringService.getProjectDetail(PROJECT_ID)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.NOT_FOUND, HttpStatus.NOT_FOUND);

        ResponseEntity<ProjectMonitoringResponse> result =
                controller.getProjectMonitoringDetails(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("UT-PC-10-03: 準正常 - 呼び出し元の会社情報が渡らない（アーキテクチャ上の注記）")
    void getMonitoring_passesNoCompanyScope() {
        // NOTE: no Authentication parameter, so the service receives no caller identity.
        // Safe while the endpoint is admin/operator only. Recorded because monitoring data
        // is per participating company, and widening the roles without adding a scope
        // parameter would expose other companies' generation data.
        ProjectMonitoringResponse serviceResponse = mock(ProjectMonitoringResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectMonitoringService.getProjectDetail(PROJECT_ID)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        controller.getProjectMonitoringDetails(PROJECT_ID);

        verifyNoInteractions(jwtUtil);
    }

    // ==================================================================
    // 11. getProjectCompanies
    // ==================================================================

    @Test
    @DisplayName("UT-PC-11-01: 正常 - 参加企業一覧が取得できる")
    void getProjectCompanies_valid_returnsList() {
        ProjectCompanyListResponse serviceResponse = mock(ProjectCompanyListResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectService.getProjectCompanies(PROJECT_ID)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<ProjectCompanyListResponse> result =
                controller.getProjectCompanies(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);
    }

    @Test
    @DisplayName("UT-PC-11-02: 異常 - プロジェクトが存在しない場合NOT_FOUNDが変換される")
    void getProjectCompanies_notFound_mapsToNotFound() {
        ProjectCompanyListResponse serviceResponse = mock(ProjectCompanyListResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.NOT_FOUND);
        when(projectService.getProjectCompanies(PROJECT_ID)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.NOT_FOUND, HttpStatus.NOT_FOUND);

        ResponseEntity<ProjectCompanyListResponse> result =
                controller.getProjectCompanies(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ==================================================================
    // 12. getMembershipPdf
    // ==================================================================

    @Test
    @DisplayName("UT-PC-12-01: 正常 - 4引数(projectId, participationId, role, companyId)の順序で呼ばれる")
    void getMembershipPdf_valid_passesRoleAndCompanyFromToken() {
        stubToken();
        when(jwtUtil.extractRole(TOKEN)).thenReturn(ROLE_MASTER);
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);

        ProjectPdfDownloadResponse serviceResponse = mock(ProjectPdfDownloadResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectService.getMembershipPdf(PROJECT_ID, PARTICIPATION_ID,
                ROLE_MASTER, COMPANY_ID)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<ProjectPdfDownloadResponse> result =
                controller.getMembershipPdf(authentication, PROJECT_ID, PARTICIPATION_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);

        // role and companyId are adjacent Strings taken from the same token; the service
        // decides access from them, so a swap must not go unnoticed.
        InOrder inOrder = inOrder(jwtUtil, projectService);
        inOrder.verify(jwtUtil).extractRole(TOKEN);
        inOrder.verify(jwtUtil).extractCompanyId(TOKEN);
        inOrder.verify(projectService).getMembershipPdf(
                PROJECT_ID, PARTICIPATION_ID, ROLE_MASTER, COMPANY_ID);
    }

    @Test
    @DisplayName("UT-PC-12-02: 異常 - 参加情報が存在しない場合NOT_FOUNDが変換される")
    void getMembershipPdf_notFound_mapsToNotFound() {
        stubToken();
        when(jwtUtil.extractRole(TOKEN)).thenReturn(ROLE_MASTER);
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);

        ProjectPdfDownloadResponse serviceResponse = mock(ProjectPdfDownloadResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.NOT_FOUND);
        when(projectService.getMembershipPdf(PROJECT_ID, PARTICIPATION_ID,
                ROLE_MASTER, COMPANY_ID)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.NOT_FOUND, HttpStatus.NOT_FOUND);

        ResponseEntity<ProjectPdfDownloadResponse> result =
                controller.getMembershipPdf(authentication, PROJECT_ID, PARTICIPATION_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("UT-PC-12-03: 異常 - 他社の参加情報を指定した場合FORBIDDENが変換される")
    void getMembershipPdf_otherCompany_mapsToForbidden() {
        // The company-scope decision belongs to the service, which is why role and
        // companyId are passed. What is verified here is that its refusal becomes a 403.
        stubToken();
        when(jwtUtil.extractRole(TOKEN)).thenReturn(ROLE_MASTER);
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);

        ProjectPdfDownloadResponse serviceResponse = mock(ProjectPdfDownloadResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.FORBIDDEN);
        when(projectService.getMembershipPdf(PROJECT_ID, PARTICIPATION_ID,
                ROLE_MASTER, COMPANY_ID)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.FORBIDDEN, HttpStatus.FORBIDDEN);

        ResponseEntity<ProjectPdfDownloadResponse> result =
                controller.getMembershipPdf(authentication, PROJECT_ID, PARTICIPATION_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("UT-PC-12-04: 異常 - PDF未準備の場合PDF_NOT_READYが変換される")
    void getMembershipPdf_notReady_mapsToHttpStatus() {
        stubToken();
        when(jwtUtil.extractRole(TOKEN)).thenReturn(ROLE_MASTER);
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);

        ProjectPdfDownloadResponse serviceResponse = mock(ProjectPdfDownloadResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.PDF_NOT_READY);
        when(projectService.getMembershipPdf(PROJECT_ID, PARTICIPATION_ID,
                ROLE_MASTER, COMPANY_ID)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.PDF_NOT_READY, HttpStatus.CONFLICT);

        ResponseEntity<ProjectPdfDownloadResponse> result =
                controller.getMembershipPdf(authentication, PROJECT_ID, PARTICIPATION_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody().getStatusCode())
                .isEqualTo(ResponseConstants.PDF_NOT_READY);
    }

    @Test
    @DisplayName("UT-PC-12-05: 異常 - role抽出が失敗した場合、companyId抽出もサービス呼び出しも行われない")
    void getMembershipPdf_invalidToken_serviceNotCalled() {
        stubToken();
        when(jwtUtil.extractRole(TOKEN))
                .thenThrow(new RuntimeException("invalid token"));

        assertThrows(RuntimeException.class,
                () -> controller.getMembershipPdf(
                        authentication, PROJECT_ID, PARTICIPATION_ID));

        // Arguments are evaluated left to right, so extractRole failing stops the call
        // before extractCompanyId is reached.
        verify(jwtUtil, never()).extractCompanyId(anyString());
        verifyNoInteractions(projectService);
    }

    @Test
    @DisplayName("UT-PC-12-06: 正常 - トークンの読み出しは1回のみ")
    void getMembershipPdf_readsCredentialsOnce() {
        // This endpoint reads getCredentials() once into a local variable even though it
        // needs two claims. The other endpoints in this controller read it per claim.
        stubToken();
        when(jwtUtil.extractRole(TOKEN)).thenReturn(ROLE_MASTER);
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);

        ProjectPdfDownloadResponse serviceResponse = mock(ProjectPdfDownloadResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(projectService.getMembershipPdf(PROJECT_ID, PARTICIPATION_ID,
                ROLE_MASTER, COMPANY_ID)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        controller.getMembershipPdf(authentication, PROJECT_ID, PARTICIPATION_ID);

        verify(authentication, times(1)).getCredentials();
    }

    // ==================================================================
    // 13. getMemberTermsInfo
    // ==================================================================

    @Test
    @DisplayName("UT-PC-13-01: 正常 - 会員規約表示用情報が取得できる")
    void getMemberTermsInfo_valid_returnsInfo() {
        MemberTermsInfoResponse serviceResponse = mock(MemberTermsInfoResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(participationProjectService.getMemberTermsInfo(PROJECT_ID))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<MemberTermsInfoResponse> result =
                controller.getMemberTermsInfo(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);
        verify(participationProjectService, times(1)).getMemberTermsInfo(PROJECT_ID);
    }

    @Test
    @DisplayName("UT-PC-13-02: 異常 - プロジェクトが存在しない場合NOT_FOUNDが変換される")
    void getMemberTermsInfo_notFound_mapsToNotFound() {
        MemberTermsInfoResponse serviceResponse = mock(MemberTermsInfoResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.NOT_FOUND);
        when(participationProjectService.getMemberTermsInfo(PROJECT_ID))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.NOT_FOUND, HttpStatus.NOT_FOUND);

        ResponseEntity<MemberTermsInfoResponse> result =
                controller.getMemberTermsInfo(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
