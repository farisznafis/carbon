package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.response.EquipmentSolarRegisterResponse;
import com.kct.jcredit.core_backend.dto.request.EquipmentRegisterSolarRequest;
import com.kct.jcredit.core_backend.dto.request.EquipmentItemSolarRequest;
import com.kct.jcredit.core_backend.dto.response.FileSolarUploadResponse;
import com.kct.jcredit.core_backend.dto.response.details.EquipmentItemSolarResponse;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.service.SiteProjectService;
import com.kct.jcredit.core_backend.service.FileUploadSolarService;
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
import static org.mockito.Mockito.*;


import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import static org.mockito.ArgumentMatchers.*;


/**
 * SiteProjectController の統合ユニットテスト
 *
 * テスト対象メソッド:
 * 1. registerEquipments() - POST /sites/{siteId}/projects/{projectId}/equipments
 * 2. uploadFile() - POST /sites/{siteId}/projects/{projectId}/files
 *
 * テストフレームワーク: JUnit 5 + Mockito + AssertJ
 * テスト範囲: Controller レイヤーのみ（Entity/Service の実装は対象外）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SiteProjectController の統合テスト")
class SiteProjectControllerTest {

    @Mock
    private SiteProjectService siteProjectService;

    @Mock
    private FileUploadSolarService fileUploadSolarService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SiteProjectController siteProjectController;

    //========== グローバルテストデータ ==========
    private Integer testSiteId;
    private Integer testProjectId;
    private String testToken;
    private String testUsername;
    private String testCompanyId;
    private EquipmentRegisterSolarRequest testEquipmentRequest;
    private EquipmentItemSolarRequest testEquipmentItemSolarRequest;
    private EquipmentSolarRegisterResponse testEquipmentSuccessResponse;
    private EquipmentSolarRegisterResponse testEquipmentConflictResponse;
    private FileSolarUploadResponse testFileSuccessResponse;
    private MultipartFile testMultipartFile;

    /**
     * 各テストメソッド実行前に共通のモックデータをセットアップ
     */
    @BeforeEach
    void setUp() {
        // 共通テストパラメータ
        testSiteId = 1;
        testProjectId = 10;
        testToken = "dummy-token";
        testUsername = "user001";
        testCompanyId = "COMP001";

        // EquipmentRegisterSolarRequest のモックデータ
        testEquipmentItemSolarRequest = new EquipmentItemSolarRequest();
        testEquipmentItemSolarRequest.setPcsManufacturer("パナソニック");
        testEquipmentItemSolarRequest.setPcsModel("VBPC255A");
        testEquipmentItemSolarRequest.setPowerCondUnitCount(5);
        testEquipmentItemSolarRequest.setDeviceNo("DEV-001");

        // EquipmentSolarRegisterResponse (成功) のモックデータ
        testEquipmentSuccessResponse = new EquipmentSolarRegisterResponse();
        testEquipmentSuccessResponse.setStatusCode("200");
        testEquipmentSuccessResponse.setMessage("設備を正常に登録しました");
        
        EquipmentItemSolarResponse equipmentItem = new EquipmentItemSolarResponse();
        equipmentItem.setEquipmentId(100);
        equipmentItem.setPcsManufacturer("パナソニック");
        equipmentItem.setPcsModel("VBPC255A");
        equipmentItem.setPowerCondUnitCount(5);
        equipmentItem.setDeviceNo("DEV-001");
        
        List<EquipmentItemSolarResponse> equipmentList = new ArrayList<>();
        equipmentList.add(equipmentItem);
        testEquipmentSuccessResponse.setEquipments(equipmentList);

        // EquipmentSolarRegisterResponse (競合) のモックデータ
        testEquipmentConflictResponse = new EquipmentSolarRegisterResponse();
        testEquipmentConflictResponse.setStatusCode("409");
        testEquipmentConflictResponse.setMessage("同一の設備が既に登録されています");

        // FileSolarUploadResponse のモックデータ
        testFileSuccessResponse = new FileSolarUploadResponse();
        testFileSuccessResponse.setFileId("200");
        testFileSuccessResponse.setOriginalName("panel-spec.pdf");
        testFileSuccessResponse.setS3Key("equipments/panel-spec.pdf");
        testFileSuccessResponse.setStatusCode("ファイルを正常にアップロードしました");

        // MockMultipartFile のモックデータ
        testMultipartFile = new MockMultipartFile(
                "file",
                "panel-spec.pdf",
                "application/pdf",
                "dummy file content for testing".getBytes()
        );
    }

    // ==================== registerEquipments() のテスト ====================

    /**
     * registerEquipments() - 正常系テストグループ
     */
        /**
         * UT-SP-REG-001: 有効なリクエストで設備登録が成功
         */
        @Test
        @DisplayName("有効なリクエストで設備登録が成功した場合、200OK レスポンスを返す")
        void registerEquipments_validRequestAndAuthentication_returnsOkResponse() {
            // Arrange
            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                when(authentication.getCredentials()).thenReturn(testToken);
                when(jwtUtil.extractUsername(testToken)).thenReturn(testUsername);
                when(jwtUtil.extractCompanyId(testToken)).thenReturn(testCompanyId);
                when(siteProjectService.registerEquipments(
                        testSiteId, testProjectId, testCompanyId, testEquipmentRequest))
                        .thenReturn(testEquipmentSuccessResponse);
                mockedUtil.when(() -> ResponseUtil.getHttpStatus("200"))
                        .thenReturn(HttpStatus.OK);

                // Act
                ResponseEntity<EquipmentSolarRegisterResponse> response =
                        siteProjectController.registerEquipments(
                                authentication, testSiteId, testProjectId, testEquipmentRequest);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody()).isEqualTo(testEquipmentSuccessResponse);
                assertThat(response.getBody().getStatusCode()).isEqualTo("200");
                assertThat(response.getBody().getMessage()).isEqualTo("設備を正常に登録しました");
                assertThat(testEquipmentRequest.getProcessedBy()).isEqualTo(testUsername);

                // Verify
                verify(authentication, times(1)).getCredentials();
                verify(jwtUtil, times(1)).extractUsername(testToken);
                verify(jwtUtil, times(1)).extractCompanyId(testToken);
                verify(siteProjectService, times(1))
                        .registerEquipments(testSiteId, testProjectId, testCompanyId, testEquipmentRequest);
                mockedUtil.verify(() -> ResponseUtil.getHttpStatus("200"), times(1));
            }
        }

        /**
         * UT-SP-REG-002: サービスが 409Conflict を返す
         */
        @Test
        @DisplayName("サービスが 409 Conflict を返す場合、CONFLICT レスポンスを返す")
        void registerEquipments_serviceReturnsConflict_returnsConflictResponse() {
            // Arrange
            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                when(authentication.getCredentials()).thenReturn(testToken);
                when(jwtUtil.extractUsername(testToken)).thenReturn(testUsername);
                when(jwtUtil.extractCompanyId(testToken)).thenReturn(testCompanyId);
                when(siteProjectService.registerEquipments(
                        testSiteId, testProjectId, testCompanyId, testEquipmentRequest))
                        .thenReturn(testEquipmentConflictResponse);
                mockedUtil.when(() -> ResponseUtil.getHttpStatus("409"))
                        .thenReturn(HttpStatus.CONFLICT);

                // Act
                ResponseEntity<EquipmentSolarRegisterResponse> response =
                        siteProjectController.registerEquipments(
                                authentication, testSiteId, testProjectId, testEquipmentRequest);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody()).isEqualTo(testEquipmentConflictResponse);
                assertThat(response.getBody().getStatusCode()).isEqualTo("409");
                assertThat(testEquipmentRequest.getProcessedBy()).isEqualTo(testUsername);

                // Verify
                verify(authentication, times(1)).getCredentials();
                verify(jwtUtil, times(1)).extractUsername(testToken);
                verify(jwtUtil, times(1)).extractCompanyId(testToken);
                verify(siteProjectService, times(1))
                        .registerEquipments(testSiteId, testProjectId, testCompanyId, testEquipmentRequest);
                mockedUtil.verify(() -> ResponseUtil.getHttpStatus("409"), times(1));
            }
        }

    /**
     * registerEquipments() - 異常系テストグループ
     */

        /**
         * UT-SP-REG-003: サービスが CustomException をスロー
         */
        @Test
        @DisplayName("サービスが CustomException をスローする場合、例外が伝播される")
        void registerEquipments_serviceThrowsCustomException_propagatesException() {
            // Arrange
            CustomException expectedException = new CustomException(
                    "E003",
                    "設備の登録に失敗しました"
            );

            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                when(authentication.getCredentials()).thenReturn(testToken);
                when(jwtUtil.extractUsername(testToken)).thenReturn(testUsername);
                when(jwtUtil.extractCompanyId(testToken)).thenReturn(testCompanyId);
                when(siteProjectService.registerEquipments(
                        testSiteId, testProjectId, testCompanyId, testEquipmentRequest))
                        .thenThrow(expectedException);

                // Act & Assert
                assertThatThrownBy(() ->
                        siteProjectController.registerEquipments(
                                authentication, testSiteId, testProjectId, testEquipmentRequest)
                )
                        .isInstanceOf(CustomException.class)
                        .hasMessage("設備の登録に失敗しました")
                        .satisfies(ex -> {
                            CustomException customEx = (CustomException) ex;
                            assertThat(customEx.getErrorCode()).isEqualTo("E003");
                        });

                assertThat(testEquipmentRequest.getProcessedBy()).isEqualTo(testUsername);

                // Verify
                verify(authentication, times(1)).getCredentials();
                verify(jwtUtil, times(1)).extractUsername(testToken);
                verify(jwtUtil, times(1)).extractCompanyId(testToken);
                verify(siteProjectService, times(1))
                        .registerEquipments(testSiteId, testProjectId, testCompanyId, testEquipmentRequest);
                mockedUtil.verifyNoInteractions();
            }
        }

        /**
         * UT-SP-REG-004: 空のトークンで extractUsername が例外をスロー
         */
        @Test
        @DisplayName("空のトークンで extractUsername が例外をスローする場合、例外が伝播される")
        void registerEquipments_emptyTokenExtractUsernameThrowsException_propagatesException() {
            // Arrange
            String emptyToken = "";
            CustomException expectedException = new CustomException(
                    "E004",
                    "トークンが無効です"
            );

            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                when(authentication.getCredentials()).thenReturn(emptyToken);
                when(jwtUtil.extractUsername(emptyToken)).thenThrow(expectedException);

                // Act & Assert
                assertThatThrownBy(() ->
                        siteProjectController.registerEquipments(
                                authentication, testSiteId, testProjectId, testEquipmentRequest)
                )
                        .isInstanceOf(CustomException.class)
                        .hasMessage("トークンが無効です")
                        .satisfies(ex -> {
                            CustomException customEx = (CustomException) ex;
                            assertThat(customEx.getErrorCode()).isEqualTo("E004");
                        });

                assertThat(testEquipmentRequest.getProcessedBy()).isNull();

                // Verify
                verify(authentication, times(1)).getCredentials();
                verify(jwtUtil, times(1)).extractUsername(emptyToken);
                verify(jwtUtil, never()).extractCompanyId(anyString());
                verifyNoInteractions(siteProjectService);
                mockedUtil.verifyNoInteractions();
            }
        }

        /**
         * UT-SP-REG-005: extractCompanyId が例外をスロー
         */
        @Test
        @DisplayName("extractCompanyId が例外をスローする場合、例外が伝播される")
        void registerEquipments_extractCompanyIdThrowsException_propagatesException() {
            // Arrange
            CustomException expectedException = new CustomException(
                    "E005",
                    "会社情報を取得できません"
            );

            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                when(authentication.getCredentials()).thenReturn(testToken);
                when(jwtUtil.extractUsername(testToken)).thenReturn(testUsername);
                when(jwtUtil.extractCompanyId(testToken)).thenThrow(expectedException);

                // Act & Assert
                assertThatThrownBy(() ->
                        siteProjectController.registerEquipments(
                                authentication, testSiteId, testProjectId, testEquipmentRequest)
                )
                        .isInstanceOf(CustomException.class)
                        .hasMessage("会社情報を取得できません")
                        .satisfies(ex -> {
                            CustomException customEx = (CustomException) ex;
                            assertThat(customEx.getErrorCode()).isEqualTo("E005");
                        });

                assertThat(testEquipmentRequest.getProcessedBy()).isNull();

                // Verify
                verify(authentication, times(1)).getCredentials();
                verify(jwtUtil, times(1)).extractUsername(testToken);
                verify(jwtUtil, times(1)).extractCompanyId(testToken);
                verifyNoInteractions(siteProjectService);
                mockedUtil.verifyNoInteractions();
            }
        }

    // ==================== uploadFile() のテスト ====================

    /**
     * uploadFile() - 正常系テストグループ
     */

        /**
         * UT-SP-UPL-001: 有効なファイルタイプでアップロード成功
         */
        @Test
        @DisplayName("有効なファイルタイプでファイルアップロードが成功した場合、200OK レスポンスを返す")
        void uploadFile_validFileTypeAndAuthentication_returnsOkResponse() throws Exception {
            // Arrange
            String fileType = "solar_contract";

            when(authentication.getCredentials()).thenReturn(testToken);
            when(jwtUtil.extractUsername(testToken)).thenReturn(testUsername);
            when(fileUploadSolarService.uploadFile(
                    eq(testSiteId),
                    eq(testProjectId),
                    eq(EquipmentFileType.SOLAR_CONTRACT),
                    eq(testUsername),
                    eq(testMultipartFile)
            )).thenReturn(testFileSuccessResponse);

            // Act
            ResponseEntity<FileSolarUploadResponse> response =
                    siteProjectController.uploadFile(
                            authentication, testSiteId, testProjectId, fileType, testMultipartFile);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isEqualTo(testFileSuccessResponse);
            assertThat(response.getBody().getFileId()).isEqualTo("200");
            assertThat(response.getBody().getOriginalName()).isEqualTo("panel-spec.pdf");

            // Verify
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractUsername(testToken);
            verify(fileUploadSolarService, times(1)).uploadFile(
                    testSiteId,
                    testProjectId,
                    EquipmentFileType.SOLAR_CONTRACT,
                    testUsername,
                    testMultipartFile
            );
        }

    /**
     * uploadFile() - 異常系テストグループ
     */

        /**
         * UT-SP-UPL-002:無効なファイルタイプ
         */
        @Test
        @DisplayName("無効なファイルタイプが指定された場合、IllegalArgumentException がスローされる")
        void uploadFile_invalidFileType_throwsIllegalArgumentException() {
            // Arrange
            String invalidFileType = "INVALID_TYPE";

            // Act & Assert
            assertThatThrownBy(() ->
                    siteProjectController.uploadFile(
                            authentication, testSiteId, testProjectId, invalidFileType, testMultipartFile)
            )
                    .isInstanceOf(IllegalArgumentException.class);

            // Verify
            verifyNoInteractions(jwtUtil);
            verifyNoInteractions(fileUploadSolarService);
            verifyNoInteractions(authentication);
        }

        /**
         * UT-SP-UPL-003: 空のトークンで例外
         */
        @Test
        @DisplayName("空のトークンで extractUsername が例外をスローする場合、例外が伝播される")
        void uploadFile_emptyTokenExtractUsernameThrowsException_propagatesException() {
            // Arrange
            String fileType = "PANEL";
            String emptyToken = "";
            CustomException expectedException = new CustomException(
                    "E004",
                    "トークンが無効です"
            );

            when(authentication.getCredentials()).thenReturn(emptyToken);
            when(jwtUtil.extractUsername(emptyToken)).thenThrow(expectedException);

            // Act & Assert
            assertThatThrownBy(() ->
                    siteProjectController.uploadFile(
                            authentication, testSiteId, testProjectId, fileType, testMultipartFile)
            )
                    .isInstanceOf(CustomException.class)
                    .hasMessage("トークンが無効です")
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo("E004");
                    });

            // Verify
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractUsername(emptyToken);
            verifyNoInteractions(fileUploadSolarService);
        }

        /**
         * UT-SP-UPL-004: サービスが IOException をスロー
         */
        @Test
        @DisplayName("サービスが IOException をスローする場合、例外が伝播される")
        void uploadFile_serviceThrowsIOException_propagatesException() throws Exception {
            // Arrange
            String fileType = "PANEL";
            IOException expectedException = new IOException("ファイルのアップロードに失敗しました");

            when(authentication.getCredentials()).thenReturn(testToken);
            when(jwtUtil.extractUsername(testToken)).thenReturn(testUsername);
            when(fileUploadSolarService.uploadFile(
                    eq(testSiteId),
                    eq(testProjectId),
                    eq(EquipmentFileType.SOLAR_CONTRACT),
                    eq(testUsername),
                    eq(testMultipartFile)
            )).thenThrow(expectedException);

            // Act & Assert
            assertThatThrownBy(() ->
                    siteProjectController.uploadFile(
                            authentication, testSiteId, testProjectId, fileType, testMultipartFile)
            )
                    .isInstanceOf(IOException.class)
                    .hasMessage("ファイルのアップロードに失敗しました");

            // Verify
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractUsername(testToken);
            verify(fileUploadSolarService, times(1)).uploadFile(
                    testSiteId,
                    testProjectId,
                    EquipmentFileType.SOLAR_CONTRACT,
                    testUsername,
                    testMultipartFile
            );
        }

        /**
         * UT-SP-UPL-005: サービスが CustomException をスロー
         */
        @Test
        @DisplayName("サービスが CustomException をスローする場合、例外が伝播される")
        void uploadFile_serviceThrowsCustomException_propagatesException() throws Exception {
            // Arrange
            String fileType = "PANEL";
            CustomException expectedException = new CustomException(
                    "E003",
                    "ファイルの処理に失敗しました"
            );

            when(authentication.getCredentials()).thenReturn(testToken);
            when(jwtUtil.extractUsername(testToken)).thenReturn(testUsername);
            when(fileUploadSolarService.uploadFile(
                    eq(testSiteId),
                    eq(testProjectId),
                    eq(EquipmentFileType.SOLAR_CONTRACT),
                    eq(testUsername),
                    eq(testMultipartFile)
            )).thenThrow(expectedException);

            // Act & Assert
            assertThatThrownBy(() ->
                    siteProjectController.uploadFile(
                            authentication, testSiteId, testProjectId, fileType, testMultipartFile)
            )
                    .isInstanceOf(CustomException.class)
                    .hasMessage("ファイルの処理に失敗しました")
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getErrorCode()).isEqualTo("E003");
                    });

            // Verify
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractUsername(testToken);
            verify(fileUploadSolarService, times(1)).uploadFile(
                    testSiteId,
                    testProjectId,
                    EquipmentFileType.SOLAR_CONTRACT,
                    testUsername,
                    testMultipartFile
            );
        }
    }