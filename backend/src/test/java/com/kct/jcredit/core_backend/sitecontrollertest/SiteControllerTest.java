package com.kct.jcredit.core_backend.sitecontrollertest;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.controller.SiteController;
import com.kct.jcredit.core_backend.dto.request.CreateSiteRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.UpdateSiteRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentChangeRequest;
import com.kct.jcredit.core_backend.dto.response.CreateSiteResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectListInfoResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentRegisterResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentListResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentHistoryListResponse;
import com.kct.jcredit.core_backend.dto.response.SiteInfoResponse;
import com.kct.jcredit.core_backend.dto.response.SiteListResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentHistoryResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.SiteResponse;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.service.EnR002EquipmentService;
import com.kct.jcredit.core_backend.service.ProjectService;
import com.kct.jcredit.core_backend.service.SiteService;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import com.kct.jcredit.core_backend.util.JwtUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
@DisplayName("SiteController のテスト")
class SiteControllerTest {

    @Mock
    private SiteService siteService;
    @Mock
    private ProjectService projectService;
    @Mock
    private EnR002EquipmentService equipmentService;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private SiteController siteController;

    private static final String TOKEN = "dummy-token";
    private static final String BAD_TOKEN = "bad-token";
    private static final String COMPANY_ID = "COMP001";
    private static final String SITE_ID = "PUB-SITE-001";
    private static final String PROJECT_ID = "PUB-PROJ-001";

    // ==== 01. getAllSitesByCompanyId ====

    @Test
    @DisplayName("UT-STC-01-01: 正常 - 有効なcompanyIdで拠点一覧を取得し200OKを返す")
    void getAllSitesByCompanyId_validCompanyId_returnsOk() {
        SiteListResponse mockResponse = new SiteListResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(siteService.getSitesByCompanyId(COMPANY_ID)).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            ResponseEntity<?> response = siteController.getAllSitesByCompanyId(COMPANY_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(mockResponse);
            verify(siteService, times(1)).getSitesByCompanyId(COMPANY_ID);
        }
    }

    @Test
    @DisplayName("UT-STC-01-02: 異常 - 拠点が見つからない場合404NOT_FOUNDを返す")
    void getAllSitesByCompanyId_companyNotFound_returnsNotFound() {
        SiteListResponse mockResponse = new SiteListResponse();
        mockResponse.setStatusCode(ResponseConstants.NOT_FOUND);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(siteService.getSitesByCompanyId("COMP999")).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.NOT_FOUND))
                    .thenReturn(HttpStatus.NOT_FOUND);

            ResponseEntity<?> response = siteController.getAllSitesByCompanyId("COMP999");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    @DisplayName("UT-STC-01-03: 準正常 - companyIdが呼び出し元の身元確認なしに素通しされる（アーキテクチャ上の注記）")
    void getAllSitesByCompanyId_noOwnershipCheckAtControllerLevel() {
        // NOTE: This endpoint has no Authentication parameter at all, so there is
        // structurally no code verifying that companyId matches the caller.
        // This test records the current design; it is not a defect being fixed here.
        SiteListResponse mockResponse = new SiteListResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(siteService.getSitesByCompanyId("COMP-OTHER")).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            siteController.getAllSitesByCompanyId("COMP-OTHER");

            verify(siteService, times(1)).getSitesByCompanyId("COMP-OTHER");
            verifyNoInteractions(authentication, jwtUtil);
        }
    }

    // ==== 02. getSite ====

    @Test
    @DisplayName("UT-STC-02-01: 正常 - 有効なsiteIdで拠点情報を取得し200OKを返す")
    void getSite_validSiteId_returnsOk() {
        SiteInfoResponse mockResponse = new SiteInfoResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(siteService.getSiteByPublicId(SITE_ID)).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            ResponseEntity<SiteInfoResponse> response = siteController.getSite(SITE_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(mockResponse);
        }
    }

    @Test
    @DisplayName("UT-STC-02-02: 異常 - 拠点が見つからない場合404NOT_FOUNDを返す")
    void getSite_siteNotFound_returnsNotFound() {
        SiteInfoResponse mockResponse = new SiteInfoResponse();
        mockResponse.setStatusCode(ResponseConstants.NOT_FOUND);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(siteService.getSiteByPublicId("UNKNOWN")).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.NOT_FOUND))
                    .thenReturn(HttpStatus.NOT_FOUND);

            ResponseEntity<SiteInfoResponse> response = siteController.getSite("UNKNOWN");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    @DisplayName("UT-STC-02-03: 準正常 - siteIdに対する会社スコープ制限が存在しない（アーキテクチャ上の注記）")
    void getSite_noCompanyScopingAtControllerLevel() {
        SiteInfoResponse mockResponse = new SiteInfoResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(siteService.getSiteByPublicId("PUB-SITE-OTHER-COMPANY")).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            siteController.getSite("PUB-SITE-OTHER-COMPANY");

            verifyNoInteractions(authentication, jwtUtil);
        }
    }

    // ==== 03. getProjectsBySiteId ====

    @Test
    @DisplayName("UT-STC-03-01: 正常 - 有効なsiteIdでプロジェクト一覧を取得し200OKを返す")
    void getProjectsBySiteId_validSiteId_returnsOk() {
        ProjectListInfoResponse mockResponse = new ProjectListInfoResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(projectService.getProjectsBySiteIdAndCompanyId(SITE_ID, COMPANY_ID))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            ResponseEntity<ProjectListInfoResponse> response =
                    siteController.getProjectsBySiteId(authentication, SITE_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(projectService, times(1)).getProjectsBySiteIdAndCompanyId(SITE_ID, COMPANY_ID);
            verifyNoInteractions(siteService);
        }
    }

    @Test
    @DisplayName("UT-STC-03-02: 異常 - サービスがFORBIDDENを返す場合403に変換される")
    void getProjectsBySiteId_forbidden_returnsForbidden() {
        ProjectListInfoResponse mockResponse = new ProjectListInfoResponse();
        mockResponse.setStatusCode(ResponseConstants.FORBIDDEN);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(projectService.getProjectsBySiteIdAndCompanyId(SITE_ID, COMPANY_ID))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.FORBIDDEN))
                    .thenReturn(HttpStatus.FORBIDDEN);

            ResponseEntity<ProjectListInfoResponse> response =
                    siteController.getProjectsBySiteId(authentication, SITE_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    @DisplayName("UT-STC-03-03: 異常 - トークンが無効な場合projectServiceは呼ばれない")
    void getProjectsBySiteId_invalidToken_skipsProjectService() {
        when(authentication.getCredentials()).thenReturn(BAD_TOKEN);
        when(jwtUtil.extractCompanyId(BAD_TOKEN))
                .thenThrow(new CustomException("E004", "トークンが無効です"));

        assertThrows(CustomException.class,
                () -> siteController.getProjectsBySiteId(authentication, SITE_ID));

        verifyNoInteractions(projectService);
    }

    // ==== 04. createSite ====

    @Test
    @DisplayName("UT-STC-04-01: 正常 - 有効なリクエストで拠点作成が成功し200OKを返す")
    void createSite_validRequest_returnsOk() {
        CreateSiteRequest request = new CreateSiteRequest();
        CreateSiteResponse mockResponse = new CreateSiteResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(siteService.createSite(request, COMPANY_ID)).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            ResponseEntity<CreateSiteResponse> response =
                    siteController.createSite(authentication, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(siteService, times(1)).createSite(request, COMPANY_ID);
        }
    }

    @Test
    @DisplayName("UT-STC-04-02: 異常 - 拠点数上限到達の場合LIMIT_EXCEEDEDが対応するHTTPステータスに変換される")
    void createSite_siteLimitExceeded_returnsLimitExceeded() {
        CreateSiteRequest request = new CreateSiteRequest();
        CreateSiteResponse mockResponse = new CreateSiteResponse();
        mockResponse.setStatusCode(ResponseConstants.LIMIT_EXCEEDED);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(siteService.createSite(request, COMPANY_ID)).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.LIMIT_EXCEEDED))
                    .thenReturn(HttpStatus.FORBIDDEN);

            ResponseEntity<CreateSiteResponse> response =
                    siteController.createSite(authentication, request);

            assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.LIMIT_EXCEEDED);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    @DisplayName("UT-STC-04-03: 異常 - トークンが無効な場合サービスは呼ばれない")
    void createSite_invalidToken_skipsService() {
        CreateSiteRequest request = new CreateSiteRequest();
        when(authentication.getCredentials()).thenReturn(BAD_TOKEN);
        when(jwtUtil.extractCompanyId(BAD_TOKEN))
                .thenThrow(new CustomException("E004", "トークンが無効です"));

        assertThrows(CustomException.class,
                () -> siteController.createSite(authentication, request));

        verifyNoInteractions(siteService);
    }

    // ==== 05. validateCreateSite ====

    @Test
    @DisplayName("UT-STC-05-01: 正常 - 検証エラーがない場合200OKを返す")
    void validateCreateSite_noValidationErrors_returnsOk() {
        CreateSiteRequest request = new CreateSiteRequest();
        BaseResponse mockResponse = BaseResponse.success(ResponseConstants.MSG_VALIDATION_SUCCESS);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(siteService.validateCreateSite(request, COMPANY_ID)).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            ResponseEntity<BaseResponse> response =
                    siteController.validateCreateSite(authentication, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(siteService, times(1)).validateCreateSite(request, COMPANY_ID);
        }
    }

    @Test
    @DisplayName("UT-STC-05-02: 異常 - 検証エラーがある場合VALIDATION_ERRORが対応するHTTPステータスに変換される")
    void validateCreateSite_validationErrors_returnsValidationError() {
        CreateSiteRequest request = new CreateSiteRequest();
        BaseResponse mockResponse = BaseResponse.validationError(Map.of("userId", "USER_NOT_FOUND"));

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(siteService.validateCreateSite(request, COMPANY_ID)).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.VALIDATION_ERROR))
                    .thenReturn(HttpStatus.BAD_REQUEST);

            ResponseEntity<BaseResponse> response =
                    siteController.validateCreateSite(authentication, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Test
    @DisplayName("UT-STC-05-03: 異常 - トークンが無効な場合サービスは呼ばれない")
    void validateCreateSite_invalidToken_skipsService() {
        CreateSiteRequest request = new CreateSiteRequest();
        when(authentication.getCredentials()).thenReturn(BAD_TOKEN);
        when(jwtUtil.extractCompanyId(BAD_TOKEN))
                .thenThrow(new CustomException("E004", "トークンが無効です"));

        assertThrows(CustomException.class,
                () -> siteController.validateCreateSite(authentication, request));

        verifyNoInteractions(siteService);
    }

    // ==== 06. updateSite ====

    @Test
    @DisplayName("UT-STC-06-01: 正常 - 有効なリクエストで拠点更新が成功し200OKを返す（引数順序を検証）")
    void updateSite_validRequest_returnsOk() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        SiteInfoResponse mockResponse = new SiteInfoResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(siteService.updateSite(SITE_ID, COMPANY_ID, request)).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            ResponseEntity<SiteInfoResponse> response =
                    siteController.updateSite(authentication, SITE_ID, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            // Verify the argument order is (siteId, companyId, request): both are String,
            // so a swap in the implementation would not be caught by the compiler.
            verify(siteService, times(1)).updateSite(SITE_ID, COMPANY_ID, request);
        }
    }

    @Test
    @DisplayName("UT-STC-06-02: 異常 - 拠点が見つからない場合404NOT_FOUNDを返す")
    void updateSite_siteNotFound_returnsNotFound() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        SiteInfoResponse mockResponse = new SiteInfoResponse();
        mockResponse.setStatusCode(ResponseConstants.NOT_FOUND);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(siteService.updateSite("UNKNOWN", COMPANY_ID, request)).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.NOT_FOUND))
                    .thenReturn(HttpStatus.NOT_FOUND);

            ResponseEntity<SiteInfoResponse> response =
                    siteController.updateSite(authentication, "UNKNOWN", request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    @DisplayName("UT-STC-06-03: 異常 - トークンが無効な場合サービスは呼ばれない")
    void updateSite_invalidToken_skipsService() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        when(authentication.getCredentials()).thenReturn(BAD_TOKEN);
        when(jwtUtil.extractCompanyId(BAD_TOKEN))
                .thenThrow(new CustomException("E004", "トークンが無効です"));

        assertThrows(CustomException.class,
                () -> siteController.updateSite(authentication, SITE_ID, request));

        verifyNoInteractions(siteService);
    }

    // ==== 07. validateUpdateSite ====

    @Test
    @DisplayName("UT-STC-07-01: 正常 - 検証エラーがない場合200OKを返す（引数順序を検証）")
    void validateUpdateSite_noValidationErrors_returnsOk() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        BaseResponse mockResponse = BaseResponse.success(ResponseConstants.MSG_VALIDATION_SUCCESS);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(siteService.validateUpdateSite(SITE_ID, COMPANY_ID, request)).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            ResponseEntity<BaseResponse> response =
                    siteController.validateUpdateSite(authentication, SITE_ID, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(siteService, times(1)).validateUpdateSite(SITE_ID, COMPANY_ID, request);
        }
    }

    @Test
    @DisplayName("UT-STC-07-02: 異常 - トークンが無効な場合サービスは呼ばれない")
    void validateUpdateSite_invalidToken_skipsService() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        when(authentication.getCredentials()).thenReturn(BAD_TOKEN);
        when(jwtUtil.extractCompanyId(BAD_TOKEN))
                .thenThrow(new CustomException("E004", "トークンが無効です"));

        assertThrows(CustomException.class,
                () -> siteController.validateUpdateSite(authentication, SITE_ID, request));

        verifyNoInteractions(siteService);
    }

    // ==== 08. getSiteProjectEquipment ====

    @Test
    @DisplayName("UT-STC-08-01: 正常 - 有効なリクエストで設備情報を取得し200OKを返す（トークン抽出1回・引数順序を検証）")
    void getSiteProjectEquipment_validRequest_returnsOk() {
        EnR002EquipmentRegisterResponse mockResponse = new EnR002EquipmentRegisterResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(jwtUtil.extractRole(TOKEN)).thenReturn(RoleConstants.MASTER);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn("master001");
            when(siteService.getSiteProjectEquipment(
                    SITE_ID, PROJECT_ID, COMPANY_ID, RoleConstants.MASTER, "master001"))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            ResponseEntity<EnR002EquipmentRegisterResponse> response =
                    siteController.getSiteProjectEquipment(authentication, SITE_ID, PROJECT_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            // The token is extracted once and reused for all three JWT extractions.
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
            verify(jwtUtil, times(1)).extractRole(TOKEN);
            verify(jwtUtil, times(1)).extractUsername(TOKEN);
            // Verify the 5-argument order (siteId, projectId, companyId, role, username).
            verify(siteService, times(1)).getSiteProjectEquipment(
                    SITE_ID, PROJECT_ID, COMPANY_ID, RoleConstants.MASTER, "master001");
        }
    }

    @Test
    @DisplayName("UT-STC-08-02: 異常 - サービスがFORBIDDENを返す場合403に変換される")
    void getSiteProjectEquipment_forbidden_returnsForbidden() {
        EnR002EquipmentRegisterResponse mockResponse = new EnR002EquipmentRegisterResponse();
        mockResponse.setStatusCode(ResponseConstants.FORBIDDEN);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(jwtUtil.extractRole(TOKEN)).thenReturn(RoleConstants.USER);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn("user001");
            when(siteService.getSiteProjectEquipment(any(), any(), any(), any(), any()))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.FORBIDDEN))
                    .thenReturn(HttpStatus.FORBIDDEN);

            ResponseEntity<EnR002EquipmentRegisterResponse> response =
                    siteController.getSiteProjectEquipment(authentication, SITE_ID, PROJECT_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    @DisplayName("UT-STC-08-03: 異常 - companyId抽出が失敗した場合role・username抽出とサービス呼び出しは行われない")
    void getSiteProjectEquipment_extractCompanyIdThrows_skipsRestAndService() {
        when(authentication.getCredentials()).thenReturn(TOKEN);
        when(jwtUtil.extractCompanyId(TOKEN))
                .thenThrow(new CustomException("E005", "会社情報を取得できません"));

        assertThrows(CustomException.class,
                () -> siteController.getSiteProjectEquipment(authentication, SITE_ID, PROJECT_ID));

        // Java evaluates arguments left to right, so if companyId extraction throws,
        // role/username extraction never runs.
        verify(jwtUtil, never()).extractRole(any());
        verify(jwtUtil, never()).extractUsername(any());
        verifyNoInteractions(siteService);
    }

    // ==== 09. addEquipment ====
    // NOTE: After the error-handling refactor the equipment service returns a
    // response object carrying statusCode instead of throwing CustomException,
    // so these endpoints now go through ResponseUtil like endpoints 01-08.

    @Test
    @DisplayName("UT-STC-09-01: 正常 - 有効なリクエストで設備追加が成功し200OKを返す")
    void addEquipment_validRequest_returnsOk() {
        EnR002EquipmentRequest request = new EnR002EquipmentRequest();
        EnR002EquipmentResponse mockResponse = new EnR002EquipmentResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(equipmentService.addEquipment(SITE_ID, COMPANY_ID, "PRJ001", request))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            ResponseEntity<EnR002EquipmentResponse> response =
                    siteController.addEquipment(authentication, SITE_ID, "PRJ001", request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(mockResponse);
            // Verify the 4-argument order (siteId, companyId, projectId, request).
            verify(equipmentService, times(1)).addEquipment(SITE_ID, COMPANY_ID, "PRJ001", request);
        }
    }

    @Test
    @DisplayName("UT-STC-09-02: 異常 - サービスがSITE_NOT_JOINEDを返す場合対応するHTTPステータスに変換される")
    void addEquipment_siteNotJoined_returnsMappedStatus() {
        EnR002EquipmentRequest request = new EnR002EquipmentRequest();
        EnR002EquipmentResponse mockResponse = new EnR002EquipmentResponse();
        mockResponse.setStatusCode(ResponseConstants.SITE_NOT_JOINED);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(equipmentService.addEquipment(SITE_ID, COMPANY_ID, "PRJ001", request))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.SITE_NOT_JOINED))
                    .thenReturn(HttpStatus.BAD_REQUEST);

            ResponseEntity<EnR002EquipmentResponse> response =
                    siteController.addEquipment(authentication, SITE_ID, "PRJ001", request);

            assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.SITE_NOT_JOINED);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Test
    @DisplayName("UT-STC-09-03: 異常 - トークンが無効な場合サービスは呼ばれない")
    void addEquipment_invalidToken_skipsService() {
        EnR002EquipmentRequest request = new EnR002EquipmentRequest();
        when(authentication.getCredentials()).thenReturn(BAD_TOKEN);
        when(jwtUtil.extractCompanyId(BAD_TOKEN))
                .thenThrow(new CustomException("E004", "トークンが無効です"));

        assertThrows(CustomException.class,
                () -> siteController.addEquipment(authentication, SITE_ID, "PRJ001", request));

        verifyNoInteractions(equipmentService);
    }

    // ==== 10. getEquipments ====

    @Test
    @DisplayName("UT-STC-10-01: 正常 - 設備が存在する場合リストを返し200OKを返す")
    void getEquipments_hasEquipments_returnsOk() {
        EnR002EquipmentListResponse mockResponse = new EnR002EquipmentListResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);
        mockResponse.setEquipments(List.of(
                new EnR002EquipmentResponse(), new EnR002EquipmentResponse()));

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(equipmentService.getEquipmentsBySite(SITE_ID, COMPANY_ID)).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            ResponseEntity<EnR002EquipmentListResponse> response =
                    siteController.getEquipments(authentication, SITE_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getEquipments()).hasSize(2);
            verify(equipmentService, times(1)).getEquipmentsBySite(SITE_ID, COMPANY_ID);
        }
    }

    @Test
    @DisplayName("UT-STC-10-02: 準正常 - 設備が1件もない場合空リストで200OKを返す")
    void getEquipments_noEquipments_returnsOkWithEmptyList() {
        EnR002EquipmentListResponse mockResponse = new EnR002EquipmentListResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);
        mockResponse.setEquipments(Collections.emptyList());

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(equipmentService.getEquipmentsBySite(SITE_ID, COMPANY_ID)).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            ResponseEntity<EnR002EquipmentListResponse> response =
                    siteController.getEquipments(authentication, SITE_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getEquipments()).isEmpty();
        }
    }

    @Test
    @DisplayName("UT-STC-10-03: 異常 - サービスがFORBIDDENを返す場合403に変換される")
    void getEquipments_forbidden_returnsForbidden() {
        EnR002EquipmentListResponse mockResponse = new EnR002EquipmentListResponse();
        mockResponse.setStatusCode(ResponseConstants.FORBIDDEN);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(equipmentService.getEquipmentsBySite(SITE_ID, COMPANY_ID)).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.FORBIDDEN))
                    .thenReturn(HttpStatus.FORBIDDEN);

            ResponseEntity<EnR002EquipmentListResponse> response =
                    siteController.getEquipments(authentication, SITE_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    @DisplayName("UT-STC-10-04: 異常 - トークンが無効な場合サービスは呼ばれない")
    void getEquipments_invalidToken_skipsService() {
        when(authentication.getCredentials()).thenReturn(BAD_TOKEN);
        when(jwtUtil.extractCompanyId(BAD_TOKEN))
                .thenThrow(new CustomException("E004", "トークンが無効です"));

        assertThrows(CustomException.class,
                () -> siteController.getEquipments(authentication, SITE_ID));

        verifyNoInteractions(equipmentService);
    }

    // ==== 11. getEquipmentById ====

    @Test
    @DisplayName("UT-STC-11-01: 正常 - 有効なequipmentIdで設備情報を取得し200OKを返す")
    void getEquipmentById_validEquipmentId_returnsOk() {
        EnR002EquipmentResponse mockResponse = new EnR002EquipmentResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);
        mockResponse.setId(100);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(equipmentService.getEquipmentById(SITE_ID, 100, COMPANY_ID)).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            ResponseEntity<EnR002EquipmentResponse> response =
                    siteController.getEquipmentById(authentication, SITE_ID, 100);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getId()).isEqualTo(100);
            // Verify the 3-argument order (siteId, equipmentId, companyId).
            verify(equipmentService, times(1)).getEquipmentById(SITE_ID, 100, COMPANY_ID);
        }
    }

    @Test
    @DisplayName("UT-STC-11-02: 異常 - 設備が見つからない場合404NOT_FOUNDを返す")
    void getEquipmentById_equipmentNotFound_returnsNotFound() {
        EnR002EquipmentResponse mockResponse = new EnR002EquipmentResponse();
        mockResponse.setStatusCode(ResponseConstants.NOT_FOUND);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(equipmentService.getEquipmentById(SITE_ID, 999, COMPANY_ID)).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.NOT_FOUND))
                    .thenReturn(HttpStatus.NOT_FOUND);

            ResponseEntity<EnR002EquipmentResponse> response =
                    siteController.getEquipmentById(authentication, SITE_ID, 999);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    @DisplayName("UT-STC-11-03: 異常 - トークンが無効な場合サービスは呼ばれない")
    void getEquipmentById_invalidToken_skipsService() {
        when(authentication.getCredentials()).thenReturn(BAD_TOKEN);
        when(jwtUtil.extractCompanyId(BAD_TOKEN))
                .thenThrow(new CustomException("E004", "トークンが無効です"));

        assertThrows(CustomException.class,
                () -> siteController.getEquipmentById(authentication, SITE_ID, 100));

        verifyNoInteractions(equipmentService);
    }

    // ==== 12. changeEquipment ====

    @Test
    @DisplayName("UT-STC-12-01: 正常 - 有効なリクエストで設備更新が成功し200OKを返す")
    void changeEquipment_validRequest_returnsOk() {
        EnR002EquipmentChangeRequest request = new EnR002EquipmentChangeRequest();
        EnR002EquipmentResponse mockResponse = new EnR002EquipmentResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);
        mockResponse.setId(100);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(equipmentService.changeEquipment(SITE_ID, 100, COMPANY_ID, request))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            ResponseEntity<EnR002EquipmentResponse> response =
                    siteController.changeEquipment(authentication, SITE_ID, 100, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(equipmentService, times(1)).changeEquipment(SITE_ID, 100, COMPANY_ID, request);
        }
    }

    @Test
    @DisplayName("UT-STC-12-02: 異常 - サービスがEQUIPMENT_ALREADY_CHANGEDを返す場合対応するHTTPステータスに変換される")
    void changeEquipment_alreadyChanged_returnsMappedStatus() {
        EnR002EquipmentChangeRequest request = new EnR002EquipmentChangeRequest();
        EnR002EquipmentResponse mockResponse = new EnR002EquipmentResponse();
        mockResponse.setStatusCode(ResponseConstants.EQUIPMENT_ALREADY_CHANGED);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(equipmentService.changeEquipment(SITE_ID, 100, COMPANY_ID, request))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.EQUIPMENT_ALREADY_CHANGED))
                    .thenReturn(HttpStatus.CONFLICT);

            ResponseEntity<EnR002EquipmentResponse> response =
                    siteController.changeEquipment(authentication, SITE_ID, 100, request);

            assertThat(response.getBody().getStatusCode())
                    .isEqualTo(ResponseConstants.EQUIPMENT_ALREADY_CHANGED);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Test
    @DisplayName("UT-STC-12-03: 異常 - トークンが無効な場合サービスは呼ばれない")
    void changeEquipment_invalidToken_skipsService() {
        EnR002EquipmentChangeRequest request = new EnR002EquipmentChangeRequest();
        when(authentication.getCredentials()).thenReturn(BAD_TOKEN);
        when(jwtUtil.extractCompanyId(BAD_TOKEN))
                .thenThrow(new CustomException("E004", "トークンが無効です"));

        assertThrows(CustomException.class,
                () -> siteController.changeEquipment(authentication, SITE_ID, 100, request));

        verifyNoInteractions(equipmentService);
    }

    // ==== 13. getEquipmentHistory ====

    @Test
    @DisplayName("UT-STC-13-01: 正常 - 履歴が存在する場合リストを返し200OKを返す")
    void getEquipmentHistory_hasHistory_returnsOk() {
        EnR002EquipmentHistoryListResponse mockResponse = new EnR002EquipmentHistoryListResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);
        mockResponse.setHistory(List.of(
        EnR002EquipmentHistoryResponse.builder().id(100).build(),
        EnR002EquipmentHistoryResponse.builder().id(200).build()));

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(equipmentService.getEquipmentHistory(SITE_ID, 100, COMPANY_ID)).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            ResponseEntity<EnR002EquipmentHistoryListResponse> response =
                    siteController.getEquipmentHistory(authentication, SITE_ID, 100);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getHistory()).hasSize(2);
            verify(equipmentService, times(1)).getEquipmentHistory(SITE_ID, 100, COMPANY_ID);
        }
    }

    @Test
    @DisplayName("UT-STC-13-02: 異常 - 設備が見つからない場合404NOT_FOUNDを返す")
    void getEquipmentHistory_equipmentNotFound_returnsNotFound() {
        EnR002EquipmentHistoryListResponse mockResponse = new EnR002EquipmentHistoryListResponse();
        mockResponse.setStatusCode(ResponseConstants.NOT_FOUND);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(equipmentService.getEquipmentHistory(SITE_ID, 999, COMPANY_ID)).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.NOT_FOUND))
                    .thenReturn(HttpStatus.NOT_FOUND);

            ResponseEntity<EnR002EquipmentHistoryListResponse> response =
                    siteController.getEquipmentHistory(authentication, SITE_ID, 999);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    @DisplayName("UT-STC-13-03: 異常 - トークンが無効な場合サービスは呼ばれない")
    void getEquipmentHistory_invalidToken_skipsService() {
        when(authentication.getCredentials()).thenReturn(BAD_TOKEN);
        when(jwtUtil.extractCompanyId(BAD_TOKEN))
                .thenThrow(new CustomException("E004", "トークンが無効です"));

        assertThrows(CustomException.class,
                () -> siteController.getEquipmentHistory(authentication, SITE_ID, 100));

        verifyNoInteractions(equipmentService);
    }
}