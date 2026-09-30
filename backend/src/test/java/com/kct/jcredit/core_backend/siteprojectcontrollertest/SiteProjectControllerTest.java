package com.kct.jcredit.core_backend.siteprojectcontrollertest;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.controller.SiteProjectController;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002JoinProjectRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002FileResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectJoinResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectPdfDownloadResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.service.EnR002FileUploadService;
import com.kct.jcredit.core_backend.service.ParticipationProjectService;
import com.kct.jcredit.core_backend.service.ParticipationProjectValidationService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;

import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import static org.mockito.ArgumentMatchers.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("SiteProjectController のテスト")
class SiteProjectControllerTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private ParticipationProjectService participationService;
    @Mock
    private ParticipationProjectValidationService validationService;
    @Mock
    private EnR002FileUploadService fileUploadSolarService;
    @Mock
    private Authentication authentication;
    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private SiteProjectController siteProjectController;

    private static final String TOKEN = "dummy-token";
    private static final String BAD_TOKEN = "bad-token";
    private static final String COMPANY_ID = "COMP001";
    private static final Integer USER_ID = 5;
    private static final String USERNAME = "master001";
    private static final String SITE_ID = "PUB-SITE-001";
    private static final String PROJECT_ID = "PUB-PROJ-001";

    private static final EquipmentFileType FILE_TYPE = EquipmentFileType.SOLAR_CONTRACT;

    // ==================================================================
    // 01. uploadFile - POST /sites/{siteId}/files/upload
    // ==================================================================

    @Test
    @DisplayName("UT-SPC-01-01: 正常 - 有効なファイルでアップロードが成功し200OKを返す（引数順序を検証）")
    void uploadFile_validRequest_returnsOk() {
        EnR002FileResponse mockResponse = new EnR002FileResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
            when(fileUploadSolarService.uploadFile(SITE_ID, FILE_TYPE, USERNAME, multipartFile))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            ResponseEntity<EnR002FileResponse> response = siteProjectController.uploadFile(
                    authentication, SITE_ID, FILE_TYPE.getValue(), multipartFile);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(mockResponse);
            // Verify the 4-argument order (siteId, fileType, uploadedBy, file).
            verify(fileUploadSolarService, times(1))
                    .uploadFile(SITE_ID, FILE_TYPE, USERNAME, multipartFile);
            // The uploader recorded must come from the token, never from the request body.
            verify(jwtUtil, times(1)).extractUsername(TOKEN);
        }
    }

    @Test
    @DisplayName("UT-SPC-01-02: 異常 - サービスがFILE_TOO_LARGEを返す場合対応するHTTPステータスに変換される")
    void uploadFile_fileTooLarge_returnsMappedStatus() {
        EnR002FileResponse mockResponse = new EnR002FileResponse();
        mockResponse.setStatusCode(ResponseConstants.FILE_TOO_LARGE);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
            when(fileUploadSolarService.uploadFile(SITE_ID, FILE_TYPE, USERNAME, multipartFile))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.FILE_TOO_LARGE))
                    .thenReturn(HttpStatus.PAYLOAD_TOO_LARGE);

            ResponseEntity<EnR002FileResponse> response = siteProjectController.uploadFile(
                    authentication, SITE_ID, FILE_TYPE.getValue(), multipartFile);

            assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.FILE_TOO_LARGE);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        }
    }

    @Test
    @DisplayName("UT-SPC-01-03: 異常 - サービスがUNSUPPORTED_FILE_TYPEを返す場合対応するHTTPステータスに変換される")
    void uploadFile_unsupportedFileType_returnsMappedStatus() {
        EnR002FileResponse mockResponse = new EnR002FileResponse();
        mockResponse.setStatusCode(ResponseConstants.UNSUPPORTED_FILE_TYPE);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
            when(fileUploadSolarService.uploadFile(SITE_ID, FILE_TYPE, USERNAME, multipartFile))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.UNSUPPORTED_FILE_TYPE))
                    .thenReturn(HttpStatus.BAD_REQUEST);

            ResponseEntity<EnR002FileResponse> response = siteProjectController.uploadFile(
                    authentication, SITE_ID, FILE_TYPE.getValue(), multipartFile);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Test
    @DisplayName("UT-SPC-01-04: 異常 - サービスがFORBIDDENを返す場合403に変換される（拠点が他社のもの）")
    void uploadFile_forbidden_returnsForbidden() {
        EnR002FileResponse mockResponse = new EnR002FileResponse();
        mockResponse.setStatusCode(ResponseConstants.FORBIDDEN);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
            when(fileUploadSolarService.uploadFile(SITE_ID, FILE_TYPE, USERNAME, multipartFile))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.FORBIDDEN))
                    .thenReturn(HttpStatus.FORBIDDEN);

            ResponseEntity<EnR002FileResponse> response = siteProjectController.uploadFile(
                    authentication, SITE_ID, FILE_TYPE.getValue(), multipartFile);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    @DisplayName("UT-SPC-01-05: 異常 - 未知のfileType文字列の場合、列挙型変換で失敗しサービスは呼ばれない")
    void uploadFile_unknownFileType_skipsService() {
        // EquipmentFileType.fromValue() throws IllegalArgumentException listing the
        // accepted values, so the request never reaches the upload service.
        when(authentication.getCredentials()).thenReturn(TOKEN);
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);

        assertThrows(IllegalArgumentException.class,
                () -> siteProjectController.uploadFile(
                        authentication, SITE_ID, "UNKNOWN_FILE_TYPE", multipartFile));

        verifyNoInteractions(fileUploadSolarService);
    }

    @Test
    @DisplayName("UT-SPC-01-06: 異常 - トークンが無効な場合サービスは呼ばれない")
    void uploadFile_invalidToken_skipsService() {
        when(authentication.getCredentials()).thenReturn(BAD_TOKEN);
        when(jwtUtil.extractUsername(BAD_TOKEN))
                .thenThrow(new CustomException("E004", "トークンが無効です"));

        assertThrows(CustomException.class,
                () -> siteProjectController.uploadFile(
                        authentication, SITE_ID, FILE_TYPE.getValue(), multipartFile));

        verifyNoInteractions(fileUploadSolarService);
    }

    // ==================================================================
    // 02. validateEquipment - POST /equipment/validate
    // ==================================================================

    @Test
    @DisplayName("UT-SPC-02-01: 正常 - 検証が通る場合200OKと成功メッセージを返す")
    void validateEquipment_valid_returnsOk() {
        EnR002EquipmentRequest request = new EnR002EquipmentRequest();

        ResponseEntity<BaseResponse> response = siteProjectController.validateEquipment(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getBody().getMessage()).isEqualTo(ResponseConstants.MSG_VALIDATION_SUCCESS);
        // The request object must be passed through untouched.
        verify(validationService, times(1)).validateEquipment(request);
    }

    @Test
    @DisplayName("UT-SPC-02-02: 異常 - 検証サービスが例外をスローする場合そのまま伝播される")
    void validateEquipment_validationFails_propagatesException() {
        EnR002EquipmentRequest request = new EnR002EquipmentRequest();
        doThrow(new CustomException(ResponseConstants.VALIDATION_ERROR,
                ResponseConstants.MSG_VALIDATION_ERROR))
                .when(validationService).validateEquipment(request);

        assertThrows(CustomException.class,
                () -> siteProjectController.validateEquipment(request));
    }

    @Test
    @DisplayName("UT-SPC-02-03: 準正常 - 検証系エンドポイントにAuthenticationパラメータが存在しない（アーキテクチャ上の注記）")
    void validateEquipment_noAuthenticationAtControllerLevel() {
        // NOTE: This endpoint (and the other three validate endpoints) takes no
        // Authentication parameter, so there is structurally no company scoping and
        // no record of who validated what. Access is limited only by @PreAuthorize.
        // This test records the current design; it is not a defect being fixed here.
        EnR002EquipmentRequest request = new EnR002EquipmentRequest();

        siteProjectController.validateEquipment(request);

        verifyNoInteractions(jwtUtil, authentication);
    }

    // ==================================================================
    // 03. validateConfirmationItems - POST /confirmation-items/validate
    // ==================================================================

    @Test
    @DisplayName("UT-SPC-03-01: 正常 - 検証が通る場合200OKと成功メッセージを返す")
    void validateConfirmationItems_valid_returnsOk() {
        EnR002ConfirmationItemRequest request = new EnR002ConfirmationItemRequest();

        ResponseEntity<BaseResponse> response =
                siteProjectController.validateConfirmationItems(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).isEqualTo(ResponseConstants.MSG_VALIDATION_SUCCESS);
        verify(validationService, times(1)).validateConfirmationItems(request);
    }

    @Test
    @DisplayName("UT-SPC-03-02: 異常 - 検証サービスが例外をスローする場合そのまま伝播される")
    void validateConfirmationItems_validationFails_propagatesException() {
        EnR002ConfirmationItemRequest request = new EnR002ConfirmationItemRequest();
        doThrow(new CustomException(ResponseConstants.VALIDATION_ERROR,
                ResponseConstants.MSG_VALIDATION_ERROR))
                .when(validationService).validateConfirmationItems(request);

        assertThrows(CustomException.class,
                () -> siteProjectController.validateConfirmationItems(request));
    }

    // ==================================================================
    // 04. validateSustainabilityCommitment - POST /sustainability-commitment/validate
    // ==================================================================

    @Test
    @DisplayName("UT-SPC-04-01: 正常 - 検証が通る場合200OKと成功メッセージを返す")
    void validateSustainabilityCommitment_valid_returnsOk() {
        EnR002SustainabilityCommitmentRequest request = new EnR002SustainabilityCommitmentRequest();

        ResponseEntity<BaseResponse> response =
                siteProjectController.validateSustainabilityCommitment(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).isEqualTo(ResponseConstants.MSG_VALIDATION_SUCCESS);
        verify(validationService, times(1)).validateSustainabilityCommitment(request);
    }

    @Test
    @DisplayName("UT-SPC-04-02: 異常 - 検証サービスが例外をスローする場合そのまま伝播される")
    void validateSustainabilityCommitment_validationFails_propagatesException() {
        EnR002SustainabilityCommitmentRequest request = new EnR002SustainabilityCommitmentRequest();
        doThrow(new CustomException(ResponseConstants.VALIDATION_ERROR,
                ResponseConstants.MSG_VALIDATION_ERROR))
                .when(validationService).validateSustainabilityCommitment(request);

        assertThrows(CustomException.class,
                () -> siteProjectController.validateSustainabilityCommitment(request));
    }

    // ==================================================================
    // 05. validateFinalAgreement - POST /final/validate
    // ==================================================================

    @Test
    @DisplayName("UT-SPC-05-01: 正常 - 検証が通る場合200OKと成功メッセージを返す")
    void validateFinalAgreement_valid_returnsOk() {
        EnR002FinalAgreementRequest request = new EnR002FinalAgreementRequest();

        ResponseEntity<BaseResponse> response =
                siteProjectController.validateFinalAgreement(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).isEqualTo(ResponseConstants.MSG_VALIDATION_SUCCESS);
        verify(validationService, times(1)).validateFinalAgreement(request);
    }

    @Test
    @DisplayName("UT-SPC-05-02: 異常 - 検証サービスが例外をスローする場合そのまま伝播される")
    void validateFinalAgreement_validationFails_propagatesException() {
        EnR002FinalAgreementRequest request = new EnR002FinalAgreementRequest();
        doThrow(new CustomException(ResponseConstants.VALIDATION_ERROR,
                ResponseConstants.MSG_VALIDATION_ERROR))
                .when(validationService).validateFinalAgreement(request);

        assertThrows(CustomException.class,
                () -> siteProjectController.validateFinalAgreement(request));
    }

    // ==================================================================
    // 06. joinProject (入会届) - POST /projects/{projectId}/join-company
    // ==================================================================

    @Test
    @DisplayName("UT-SPC-06-01: 正常 - 入会届の登録が成功し200OKを返す（4引数の順序を検証）")
    void joinProject_validRequest_returnsOk() {
        EnR002JoinProjectRequest request = new EnR002JoinProjectRequest();
        ProjectJoinResponse mockResponse = new ProjectJoinResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(USER_ID);
            when(participationService.submitParticipation(PROJECT_ID, COMPANY_ID, USER_ID, request))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            ResponseEntity<ProjectJoinResponse> response =
                    siteProjectController.joinProject(authentication, PROJECT_ID, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            // companyId and userId must come from the token, never from the request body.
            verify(participationService, times(1))
                    .submitParticipation(PROJECT_ID, COMPANY_ID, USER_ID, request);
        }
    }

    @Test
    @DisplayName("UT-SPC-06-02: 異常 - 既に提出済みの場合ALREADY_SUBMITTEDが対応するHTTPステータスに変換される")
    void joinProject_alreadySubmitted_returnsMappedStatus() {
        EnR002JoinProjectRequest request = new EnR002JoinProjectRequest();
        ProjectJoinResponse mockResponse = new ProjectJoinResponse();
        mockResponse.setStatusCode(ResponseConstants.ALREADY_SUBMITTED);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(USER_ID);
            when(participationService.submitParticipation(PROJECT_ID, COMPANY_ID, USER_ID, request))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.ALREADY_SUBMITTED))
                    .thenReturn(HttpStatus.CONFLICT);

            ResponseEntity<ProjectJoinResponse> response =
                    siteProjectController.joinProject(authentication, PROJECT_ID, request);

            assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.ALREADY_SUBMITTED);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Test
    @DisplayName("UT-SPC-06-03: 異常 - プロジェクトが存在しない場合404NOT_FOUNDを返す")
    void joinProject_projectNotFound_returnsNotFound() {
        EnR002JoinProjectRequest request = new EnR002JoinProjectRequest();
        ProjectJoinResponse mockResponse = new ProjectJoinResponse();
        mockResponse.setStatusCode(ResponseConstants.NOT_FOUND);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(USER_ID);
            when(participationService.submitParticipation("UNKNOWN-PJ", COMPANY_ID, USER_ID, request))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.NOT_FOUND))
                    .thenReturn(HttpStatus.NOT_FOUND);

            ResponseEntity<ProjectJoinResponse> response =
                    siteProjectController.joinProject(authentication, "UNKNOWN-PJ", request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    @DisplayName("UT-SPC-06-04: 異常 - サービスがFORBIDDENを返す場合403に変換される")
    void joinProject_forbidden_returnsForbidden() {
        EnR002JoinProjectRequest request = new EnR002JoinProjectRequest();
        ProjectJoinResponse mockResponse = new ProjectJoinResponse();
        mockResponse.setStatusCode(ResponseConstants.FORBIDDEN);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(USER_ID);
            when(participationService.submitParticipation(PROJECT_ID, COMPANY_ID, USER_ID, request))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.FORBIDDEN))
                    .thenReturn(HttpStatus.FORBIDDEN);

            ResponseEntity<ProjectJoinResponse> response =
                    siteProjectController.joinProject(authentication, PROJECT_ID, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    @DisplayName("UT-SPC-06-05: 異常 - companyId抽出が失敗した場合userId抽出とサービス呼び出しは行われない")
    void joinProject_extractCompanyIdThrows_skipsRestAndService() {
        EnR002JoinProjectRequest request = new EnR002JoinProjectRequest();
        when(authentication.getCredentials()).thenReturn(BAD_TOKEN);
        when(jwtUtil.extractCompanyId(BAD_TOKEN))
                .thenThrow(new CustomException("E004", "トークンが無効です"));

        assertThrows(CustomException.class,
                () -> siteProjectController.joinProject(authentication, PROJECT_ID, request));

        // companyId is resolved on the line before userId, so userId extraction never runs.
        verify(jwtUtil, never()).extractUserId(any());
        verifyNoInteractions(participationService);
    }

    @Test
    @DisplayName("UT-SPC-06-06: 準正常 - トークンが2回読み出される（SiteControllerの1回読み出しパターンとの差異を記録）")
    void joinProject_tokenIsReadTwice() {
        // NOTE: extractCompanyId() and extractUserId() are separate private helpers,
        // each calling auth.getCredentials().toString() again. SiteController reads the
        // token once and reuses it. Recorded here so the difference is visible; not a defect.
        EnR002JoinProjectRequest request = new EnR002JoinProjectRequest();
        ProjectJoinResponse mockResponse = new ProjectJoinResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(USER_ID);
            when(participationService.submitParticipation(PROJECT_ID, COMPANY_ID, USER_ID, request))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            siteProjectController.joinProject(authentication, PROJECT_ID, request);

            verify(authentication, times(2)).getCredentials();
        }
    }

    // ==================================================================
    // 07. joinProjectSite - POST /projects/{projectId}/{siteId}/join-site
    // ==================================================================

    @Test
    @DisplayName("UT-SPC-07-01: 正常 - 拠点のプロジェクト参加が成功し200OKを返す（String4引数の順序を厳密に検証）")
    void joinProjectSite_validRequest_returnsOk() {
        ProjectJoinResponse mockResponse = new ProjectJoinResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(USER_ID);
            when(participationService.joinSite(PROJECT_ID, COMPANY_ID, USER_ID, SITE_ID))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            ResponseEntity<ProjectJoinResponse> response =
                    siteProjectController.joinProjectSite(authentication, PROJECT_ID, SITE_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            // projectId, companyId and siteId are all String: a swap in the implementation
            // would not be caught by the compiler, so the order is asserted explicitly.
            verify(participationService, times(1))
                    .joinSite(PROJECT_ID, COMPANY_ID, USER_ID, SITE_ID);
        }
    }

    @Test
    @DisplayName("UT-SPC-07-02: 異常 - 拠点またはプロジェクトが存在しない場合404NOT_FOUNDを返す")
    void joinProjectSite_notFound_returnsNotFound() {
        ProjectJoinResponse mockResponse = new ProjectJoinResponse();
        mockResponse.setStatusCode(ResponseConstants.NOT_FOUND);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(USER_ID);
            when(participationService.joinSite(PROJECT_ID, COMPANY_ID, USER_ID, "UNKNOWN-SITE"))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.NOT_FOUND))
                    .thenReturn(HttpStatus.NOT_FOUND);

            ResponseEntity<ProjectJoinResponse> response =
                    siteProjectController.joinProjectSite(authentication, PROJECT_ID, "UNKNOWN-SITE");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    @DisplayName("UT-SPC-07-03: 異常 - 既に参加済みの場合CONFLICTが対応するHTTPステータスに変換される")
    void joinProjectSite_alreadyJoined_returnsConflict() {
        ProjectJoinResponse mockResponse = new ProjectJoinResponse();
        mockResponse.setStatusCode(ResponseConstants.CONFLICT);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(USER_ID);
            when(participationService.joinSite(PROJECT_ID, COMPANY_ID, USER_ID, SITE_ID))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.CONFLICT))
                    .thenReturn(HttpStatus.CONFLICT);

            ResponseEntity<ProjectJoinResponse> response =
                    siteProjectController.joinProjectSite(authentication, PROJECT_ID, SITE_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Test
    @DisplayName("UT-SPC-07-04: 異常 - トークンが無効な場合サービスは呼ばれない")
    void joinProjectSite_invalidToken_skipsService() {
        when(authentication.getCredentials()).thenReturn(BAD_TOKEN);
        when(jwtUtil.extractCompanyId(BAD_TOKEN))
                .thenThrow(new CustomException("E004", "トークンが無効です"));

        assertThrows(CustomException.class,
                () -> siteProjectController.joinProjectSite(authentication, PROJECT_ID, SITE_ID));

        verify(jwtUtil, never()).extractUserId(any());
        verifyNoInteractions(participationService);
    }

    // ==================================================================
    // 08. getMembershipPdfByCompany (入会届PDF) - GET /{projectId}/membership-pdf
    // ==================================================================

    @Test
    @DisplayName("UT-SPC-08-01: 正常 - 自社の入会届PDF情報を取得し200OKを返す")
    void getMembershipPdfByCompany_validRequest_returnsOk() {
        ProjectPdfDownloadResponse mockResponse = new ProjectPdfDownloadResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(participationService.getMembershipPdfByCompany(PROJECT_ID, COMPANY_ID))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

            ResponseEntity<ProjectPdfDownloadResponse> response =
                    siteProjectController.getMembershipPdfByCompany(authentication, PROJECT_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(mockResponse);
            // companyId comes from the token, so another company's PDF cannot be
            // requested by manipulating the path.
            verify(participationService, times(1)).getMembershipPdfByCompany(PROJECT_ID, COMPANY_ID);
        }
    }

    @Test
    @DisplayName("UT-SPC-08-02: 異常 - PDFが存在しない場合404NOT_FOUNDに変換される")
    void getMembershipPdfByCompany_pdfNotFound_returnsNotFound() {
        ProjectPdfDownloadResponse mockResponse = new ProjectPdfDownloadResponse();
        mockResponse.setStatusCode(ResponseConstants.NOT_FOUND);

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
            when(participationService.getMembershipPdfByCompany("UNKNOWN-PJ", COMPANY_ID))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.NOT_FOUND))
                    .thenReturn(HttpStatus.NOT_FOUND);

            ResponseEntity<ProjectPdfDownloadResponse> response =
                    siteProjectController.getMembershipPdfByCompany(authentication, "UNKNOWN-PJ");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    @DisplayName("UT-SPC-08-03: 異常 - トークンが無効な場合サービスは呼ばれない")
    void getMembershipPdfByCompany_invalidToken_skipsService() {
        when(authentication.getCredentials()).thenReturn(BAD_TOKEN);
        when(jwtUtil.extractCompanyId(BAD_TOKEN))
                .thenThrow(new CustomException("E004", "トークンが無効です"));

        assertThrows(CustomException.class,
                () -> siteProjectController.getMembershipPdfByCompany(authentication, PROJECT_ID));

        verifyNoInteractions(participationService);
    }
}