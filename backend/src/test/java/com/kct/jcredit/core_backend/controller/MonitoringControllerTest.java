package com.kct.jcredit.core_backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.CancelRequestDecisionRequest;
import com.kct.jcredit.core_backend.dto.request.CreateCancelRequestRequest;
import com.kct.jcredit.core_backend.dto.request.RegisterMonitoringRecordSolarRequest;
import com.kct.jcredit.core_backend.dto.response.CancelRequestDecisionResponse;
import com.kct.jcredit.core_backend.dto.response.CancelRequestListResponse;
import com.kct.jcredit.core_backend.dto.response.CancelRequestResponse;
import com.kct.jcredit.core_backend.dto.response.FileUploadResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringRecordSolarListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringRecordSolarResponse;
import com.kct.jcredit.core_backend.dto.response.details.FileInfoResponse;
import com.kct.jcredit.core_backend.dto.response.details.MonitoringDataSolarResponse;
import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;
import com.kct.jcredit.core_backend.service.CancelRequestService;
import com.kct.jcredit.core_backend.service.MonitoringService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;


@ExtendWith(MockitoExtension.class)
class MonitoringControllerTest {
    
    @Mock
    private MonitoringService monitoringService;

    @Mock
    private CancelRequestService cancelRequestService;

    @Mock
    private Authentication authentication;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private MonitoringController monitoringController;

    private static final String TEST_TOKEN = "test.jwt.token";
    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_COMPANY_ID = "COMPANY001";
    private static final Integer TEST_PROJECT_ID = 100;
    private static final Integer TEST_SITE_ID = 200;

    private static final String BASE_URL = "/monitoring/list";
    private static final String TOKEN = "Bearer test-jwt-token";
    private static final String USERNAME = "testuser";
    private static final String COMPANY_ID = "company123";
    private static final Integer SITE_ID = 1;
    private static final Integer PROJECT_ID = 100;

    @BeforeEach
    void setUp() {
        // JWT認証のモック設定
        lenient().when(authentication.getCredentials()).thenReturn(TEST_TOKEN);

        lenient().when(jwtUtil.extractUsername(anyString())).thenReturn(USERNAME);
        lenient().when(jwtUtil.extractCompanyId(anyString())).thenReturn(COMPANY_ID);
        lenient().when(authentication.getCredentials()).thenReturn(TOKEN);
    }

    @Test
    @DisplayName("有効なリクエストで登録成功")
    void testRegisterMonitoringRecordSolar_ValidRequest_Success() {
        // Given
        String companyId = "company123";
        
        RegisterMonitoringRecordSolarRequest request = new RegisterMonitoringRecordSolarRequest();
        request.setProjectId(1);
        request.setSiteId(10);
        request.setUserId(100);
        request.setProjectType("SOLAR");
        request.setDateRecorded("2024-01-15T10:30:00");
        request.setElPv(new BigDecimal("150.500"));
        request.setElPvr(new BigDecimal("120.300"));
        request.setChangedBattery(false);
        request.setElpjControl(new BigDecimal("50.000"));
        request.setElPvEvidenceUrl("https://example.com/evidence/elpv_001.pdf");
        request.setElPvrEvidenceUrl("https://example.com/evidence/elpvr_001.pdf");
        request.setElpjBatterySpecificationUrl("https://example.com/evidence/battery_spec_001.pdf");
        request.setElpjControlEvidenceUrl("https://example.com/evidence/control_001.pdf");
        
        MonitoringDataSolarResponse dataResponse = MonitoringDataSolarResponse.builder()
                .projectId(1)
                .siteId(10)
                .userId(100)
                .projectType("SOLAR")
                .dateRecorded("2024-01-15T10:30:00")
                .timeUpdated("2024-01-15T10:35:00")
                .elPv(new BigDecimal("150.500"))
                .elPvr(new BigDecimal("120.300"))
                .changedBattery(false)
                .elpjControl(new BigDecimal("50.000"))
                .build();
        
        MonitoringRecordSolarResponse expectedResponse = MonitoringRecordSolarResponse.builder()
                .data(dataResponse)
                .build();
        expectedResponse.setStatusCode(ResponseConstants.OK);
        expectedResponse.setMessage("監視記録が正常に登録されました");

        // ★★★ モックの設定順序を明確に★★★
        // 1. authentication.getCredentials() が token を返すように設定
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);

        // 2. JwtUtil のメソッドをモック
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(companyId);

        // 3. Service のメソッドをモック
        when(monitoringService.registerMonitoringRecordSolar(TEST_USERNAME, companyId, request))
                .thenReturn(expectedResponse);
        
        // ResponseUtilをモック化
        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                       .thenReturn(HttpStatus.CREATED);

            // When
            ResponseEntity<MonitoringRecordSolarResponse> response = 
                    monitoringController.RegisterMonitoringRecordSolar(authentication, request);
            
            // Then
            assertNotNull(response, "レスポンスがnullです");
            assertEquals(HttpStatus.CREATED, response.getStatusCode(), "ステータスコードがCREATEDではありません");
            
            assertNotNull(response.getBody(), "レスポンスボディがnullです");
            assertEquals(ResponseConstants.OK, response.getBody().getStatusCode(), "レスポンスのstatusCodeが201ではありません");
            assertEquals("監視記録が正常に登録されました", response.getBody().getMessage(), "メッセージが一致しません");
            
            // データの検証
            assertNotNull(response.getBody().getData(), "データがnullです");
            MonitoringDataSolarResponse data = response.getBody().getData();
            
            assertEquals(1, data.getProjectId(), "プロジェクトIDが一致しません");
            assertEquals(10, data.getSiteId(), "サイトIDが一致しません");
            assertEquals(100, data.getUserId(), "ユーザーIDが一致しません");
            assertEquals("SOLAR", data.getProjectType(), "プロジェクトタイプが一致しません");
            assertEquals("2024-01-15T10:30:00", data.getDateRecorded(), "記録日時が一致しません");
            assertEquals("2024-01-15T10:35:00", data.getTimeUpdated(), "更新日時が一致しません");
            assertEquals(new BigDecimal("150.500"), data.getElPv(), "ELpvが一致しません");
            assertEquals(new BigDecimal("120.300"), data.getElPvr(), "ELpvrが一致しません");
            assertEquals(false, data.getChangedBattery(), "バッテリー交換フラグが一致しません");
            assertEquals(new BigDecimal("50.000"), data.getElpjControl(), "ELPJ制御値が一致しません");
            
            // モックメソッドの呼び出し検証
            verify(jwtUtil, times(1)).extractUsername(TEST_TOKEN);
            verify(jwtUtil, times(1)).extractCompanyId(TEST_TOKEN);
            verify(monitoringService, times(1)).registerMonitoringRecordSolar(TEST_USERNAME, companyId, request);
        }
    }
    
    @Test
    @DisplayName("バッテリー交換ありで登録成功")
    void testRegisterMonitoringRecordSolar_WithBatteryReplacement_Success() {
        // Given
        String companyId = "company123";
        
        RegisterMonitoringRecordSolarRequest request = new RegisterMonitoringRecordSolarRequest();
        request.setProjectId(2);
        request.setSiteId(20);
        request.setUserId(200);
        request.setProjectType("SOLAR");
        request.setDateRecorded("2024-02-20T14:00:00");
        request.setElPv(new BigDecimal("180.750"));
        request.setElPvr(new BigDecimal("140.500"));
        request.setChangedBattery(true);
        request.setElpjBatteryValue(new BigDecimal("250.000"));
        request.setElpjControl(new BigDecimal("60.000"));
        request.setElPvEvidenceUrl("https://example.com/evidence/elpv_002.pdf");
        request.setElPvrEvidenceUrl("https://example.com/evidence/elpvr_002.pdf");
        request.setElpjBatterySpecificationUrl("https://example.com/evidence/battery_spec_002.pdf");
        request.setElpjControlEvidenceUrl("https://example.com/evidence/control_002.pdf");
        
        MonitoringDataSolarResponse dataResponse = MonitoringDataSolarResponse.builder()
                .projectId(2)
                .siteId(20)
                .userId(200)
                .projectType("SOLAR")
                .dateRecorded("2024-02-20T14:00:00")
                .timeUpdated("2024-02-20T14:05:00")
                .elPv(new BigDecimal("180.750"))
                .elPvr(new BigDecimal("140.500"))
                .changedBattery(true)
                .elpjBatteryValue(new BigDecimal("250.000"))
                .elpjControl(new BigDecimal("60.000"))
                .build();
        
        MonitoringRecordSolarResponse expectedResponse = MonitoringRecordSolarResponse.builder()
                .data(dataResponse)
                .build();
        expectedResponse.setStatusCode(ResponseConstants.OK);
        expectedResponse.setMessage("監視記録が正常に登録されました");
        
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(companyId);
        when(monitoringService.registerMonitoringRecordSolar(TEST_USERNAME, companyId, request))
                .thenReturn(expectedResponse);
        
        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                       .thenReturn(HttpStatus.CREATED);

            // When
            ResponseEntity<MonitoringRecordSolarResponse> response = 
                    monitoringController.RegisterMonitoringRecordSolar(authentication, request);
            
            // Then
            MonitoringDataSolarResponse data = response.getBody().getData();
            
            assertEquals(true, data.getChangedBattery(), "バッテリー交換フラグが一致しません");
            assertEquals(new BigDecimal("250.000"), data.getElpjBatteryValue(), "バッテリー値が一致しません");
            
        }
    }

    @Test
    @DisplayName("オプション項目ありで登録成功")
    void testRegisterMonitoringRecordSolar_WithOptionalFields_Success() {
        // Given
        String companyId = "company123";
        
        RegisterMonitoringRecordSolarRequest request = new RegisterMonitoringRecordSolarRequest();
        request.setProjectId(3);
        request.setSiteId(30);
        request.setUserId(300);
        request.setProjectType("SOLAR");
        request.setDateRecorded("2024-03-10T09:15:00");
        request.setElPv(new BigDecimal("200.125"));
        request.setElPvr(new BigDecimal("160.875"));
        request.setChangedBattery(false);
        request.setElpjControl(new BigDecimal("70.500"));
        request.setElpjBatteryValue(new BigDecimal("300.000"));
        request.setElpjAccuracy(new BigDecimal("95.50"));
        request.setElPvEvidenceUrl("https://example.com/evidence/elpv_003.pdf");
        request.setElPvrEvidenceUrl("https://example.com/evidence/elpvr_003.pdf");
        request.setElpjBatterySpecificationUrl("https://example.com/evidence/battery_spec_003.pdf");
        request.setElpjControlEvidenceUrl("https://example.com/evidence/control_003.pdf");
        
        MonitoringDataSolarResponse dataResponse = MonitoringDataSolarResponse.builder()
                .projectId(3)
                .siteId(30)
                .userId(300)
                .projectType("SOLAR")
                .dateRecorded("2024-03-10T09:15:00")
                .timeUpdated("2024-03-10T09:20:00")
                .elPv(new BigDecimal("200.125"))
                .elPvr(new BigDecimal("160.875"))
                .changedBattery(false)
                .elpjBatteryValue(new BigDecimal("300.000"))
                .elpjControl(new BigDecimal("70.500"))
                .elpjAccuracy(new BigDecimal("95.50"))
                .build();
        
        MonitoringRecordSolarResponse expectedResponse = MonitoringRecordSolarResponse.builder()
                .data(dataResponse)
                .build();
        expectedResponse.setStatusCode(ResponseConstants.OK);
        expectedResponse.setMessage("監視記録が正常に登録されました");

        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(companyId);
        when(monitoringService.registerMonitoringRecordSolar(TEST_USERNAME, companyId, request))
                .thenReturn(expectedResponse);
        
        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                       .thenReturn(HttpStatus.CREATED);

            // When
            ResponseEntity<MonitoringRecordSolarResponse> response = 
                    monitoringController.RegisterMonitoringRecordSolar(authentication, request);
            
            // Then
            MonitoringDataSolarResponse data = response.getBody().getData();
            
            assertEquals(new BigDecimal("300.000"), data.getElpjBatteryValue(), "バッテリー値が一致しません");
            assertEquals(new BigDecimal("95.50"), data.getElpjAccuracy(), "精度が一致しません");
            
        }
    }

    @Test
    @DisplayName("クエリパラメータで最新レコード取得成功")
    void testReadLastMonitoringRecordSolar_Success() {
        // Given

        String user = "testuser";
        String companyId = "company123";
        String testToken = "Bearer test-jwt-token";


        MonitoringRecordSolarResponse expectedResponse = new MonitoringRecordSolarResponse();
        expectedResponse.setStatusCode("200");
        expectedResponse.setMessage("Success");
        //必要に応じて他のフィールドを設定

        when(monitoringService.readLastMonitoringRecordSolar(
                user,
                companyId,
                TEST_PROJECT_ID,
                TEST_SITE_ID
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
            mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("200"))
                    .thenReturn(HttpStatus.OK);

            // When
            ResponseEntity<MonitoringRecordSolarResponse> result = monitoringController.ReadLastMonitoringRecordSolar(
                    authentication,
                    TEST_PROJECT_ID,
                    TEST_SITE_ID
            );

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getStatusCode()).isEqualTo("200");
            assertThat(result.getBody().getMessage()).isEqualTo("Success");

            //検証
            verify(jwtUtil).extractUsername(testToken);
            verify(jwtUtil).extractCompanyId(testToken);
            verify(monitoringService).readLastMonitoringRecordSolar(
                    user,
                    companyId,
                    TEST_PROJECT_ID,
                    TEST_SITE_ID
            );
        }
    }

    @Test
    @DisplayName("JWTからusernameとcompanyId抽出成功")
    void testJwtExtraction_Success() {

        String user = "testuser";
        String companyId = "company123";
        String testToken = "Bearer test-jwt-token";

        // Given
        when(jwtUtil.extractUsername(testToken)).thenReturn(user);
        when(jwtUtil.extractCompanyId(testToken)).thenReturn(companyId);

        MonitoringRecordSolarResponse mockResponse = new MonitoringRecordSolarResponse();
        mockResponse.setStatusCode("200");

        when(monitoringService.readLastMonitoringRecordSolar(
                anyString(),
                anyString(),
                anyInt(),
                anyInt()
        )).thenReturn(mockResponse);

        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
            mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("200"))
                    .thenReturn(HttpStatus.OK);

            // When
            monitoringController.ReadLastMonitoringRecordSolar(
                    authentication,
                    TEST_PROJECT_ID,
                    TEST_SITE_ID
            );

            // Then
            verify(jwtUtil).extractUsername(testToken);
            verify(jwtUtil).extractCompanyId(testToken);

            // サービス呼び出し時に正しいusernameとcompanyIdが渡されていることを確認
            verify(monitoringService).readLastMonitoringRecordSolar(
                    eq(user),
                    eq(companyId),
                    eq(TEST_PROJECT_ID),
                    eq(TEST_SITE_ID)
            );
        }
    }
/* 
    @Test
    @DisplayName("レコードが見つからない場合")
    void testReadLastMonitoringRecordSolar_NotFound() {
        // Given
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(TEST_COMPANY_ID);

        MonitoringRecordSolarResponse expectedResponse = new MonitoringRecordSolarResponse();
        expectedResponse.setStatusCode("404");
        expectedResponse.setMessage("Record not found");

        when(monitoringService.readLastMonitoringRecordSolar(
                TEST_USERNAME,
                TEST_COMPANY_ID,
                TEST_PROJECT_ID,
                TEST_SITE_ID
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
            mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("404"))
                    .thenReturn(HttpStatus.NOT_FOUND);

            // When
            ResponseEntity<MonitoringRecordSolarResponse> result = monitoringController.ReadLastMonitoringRecordSolar(
                    authentication,
                    TEST_PROJECT_ID,
                    TEST_SITE_ID
            );

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getStatusCode()).isEqualTo("404");
            assertThat(result.getBody().getMessage()).isEqualTo("Record not found");
        }
    }

    @Test
    @DisplayName("認証エラーの場合")
    void testReadLastMonitoringRecordSolar_AuthenticationError() {
        // Given
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(TEST_COMPANY_ID);

        MonitoringRecordSolarResponse expectedResponse = new MonitoringRecordSolarResponse();
        expectedResponse.setStatusCode("401");
        expectedResponse.setMessage("Unauthorized");

        when(monitoringService.readLastMonitoringRecordSolar(
                TEST_USERNAME,
                TEST_COMPANY_ID,
                TEST_PROJECT_ID,
                TEST_SITE_ID
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
            mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("401"))
                    .thenReturn(HttpStatus.UNAUTHORIZED);

            // When
            ResponseEntity<MonitoringRecordSolarResponse> result = monitoringController.ReadLastMonitoringRecordSolar(
                    authentication,
                    TEST_PROJECT_ID,
                    TEST_SITE_ID
            );

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getStatusCode()).isEqualTo("401");
        }
    }

    @Test
    @DisplayName("内部サーバーエラーの場合")
    void testReadLastMonitoringRecordSolar_InternalServerError() {
        // Given
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(TEST_COMPANY_ID);

        MonitoringRecordSolarResponse expectedResponse = new MonitoringRecordSolarResponse();
        expectedResponse.setStatusCode("500");
        expectedResponse.setMessage("Internal Server Error");

        when(monitoringService.readLastMonitoringRecordSolar(
                TEST_USERNAME,
                TEST_COMPANY_ID,
                TEST_PROJECT_ID,
                TEST_SITE_ID
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
            mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("500"))
                    .thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

            // When
            ResponseEntity<MonitoringRecordSolarResponse> result = monitoringController.ReadLastMonitoringRecordSolar(
                    authentication,
                    TEST_PROJECT_ID,
                    TEST_SITE_ID
            );

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getStatusCode()).isEqualTo("500");
        }
    }

    @Test
    @DisplayName("異なるクエリパラメータでの呼び出し")
    void testReadLastMonitoringRecordSolar_DifferentParameters() {
        // Given
        Integer differentProjectId = 999;
        Integer differentSiteId = 888;

        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(TEST_COMPANY_ID);

        MonitoringRecordSolarResponse expectedResponse = new MonitoringRecordSolarResponse();
        expectedResponse.setStatusCode("200");

        when(monitoringService.readLastMonitoringRecordSolar(
                TEST_USERNAME,
                TEST_COMPANY_ID,
                differentProjectId,
                differentSiteId
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
            mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("200"))
                    .thenReturn(HttpStatus.OK);

            // When
            monitoringController.ReadLastMonitoringRecordSolar(
                    authentication,
                    differentProjectId,
                    differentSiteId
            );

            // Then
            verify(monitoringService).readLastMonitoringRecordSolar(
                    TEST_USERNAME,
                    TEST_COMPANY_ID,
                    differentProjectId,
                    differentSiteId
            );
        }
    }
        */
    @Test
    @DisplayName("デフォルトページネーションで一覧取得成功")
    void testListMonitoringRecords_DefaultPagination_Success() {
        // Given
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(TEST_COMPANY_ID);

        MonitoringRecordSolarListResponse expectedResponse = new MonitoringRecordSolarListResponse();
        expectedResponse.setStatusCode(ResponseConstants.OK);
        expectedResponse.setMessage("Success");
        
        // PaginationDetailResponseを使用
        PaginationDetailResponse pagination = new PaginationDetailResponse();
        pagination.setPage(1);
        pagination.setLimit(50);
        pagination.setTotalItems(100);  // ✅ totalItems
        pagination.setTotalPages(2);
        pagination.setNextPageToken("eyJwYWdlIjoyLCJsaW1pdCI6NTB9");
        pagination.setHasMore(true);  // ✅ hasMore
        expectedResponse.setPagination(pagination);
        
        // MonitoringTableRecordSolarのリストを作成
        List<EnR002MonitoringRecord> items = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
                EnR002MonitoringRecord record = EnR002MonitoringRecord.builder()
                        .id(TEST_SITE_ID + "#" + TEST_PROJECT_ID)
                        .projectID(TEST_PROJECT_ID)
                        .siteID(TEST_SITE_ID)
                        .participatingUserID(100 + i)
                        .dateRecorded("2024-01-15T10:30:00")
                        .elPv(new BigDecimal("150.500"))
                        .timeUpdated("2024-01-15T10:30:00")
                        .isActive(true)
                        .build();
                items.add(record);
        }
        expectedResponse.setItems(items);  // ✅ setItems

        when(monitoringService.readAllMonitoringRecordsSolar(
                eq(TEST_USERNAME),
                eq(TEST_COMPANY_ID),
                eq(TEST_PROJECT_ID),
                eq(TEST_SITE_ID),
                eq(1), // デフォルトpage
                eq(50), // デフォルトlimit
                eq(null) // nextPageToken
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
                mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                        .thenReturn(HttpStatus.OK);

                // When
                ResponseEntity<MonitoringRecordSolarListResponse> result = 
                        monitoringController.listMonitoringRecords(
                                TEST_SITE_ID,
                                TEST_PROJECT_ID,
                                1, // デフォルトpage
                                50, // デフォルトlimit
                                null, // nextPageToken
                                authentication
                        );

                // Then
                assertNotNull(result, "レスポンスがnullです");
                assertEquals(HttpStatus.OK, result.getStatusCode(), "ステータスコードがOKではありません");
                assertNotNull(result.getBody(), "レスポンスボディがnullです");
                assertEquals(ResponseConstants.OK, result.getBody().getStatusCode(), 
                        "statusCodeが一致しません");
                assertEquals("Success", result.getBody().getMessage(), "メッセージが一致しません");
                
                // ページネーション検証
                assertNotNull(result.getBody().getPagination(), "Paginationがnullです");
                assertEquals(1, result.getBody().getPagination().getPage(), 
                        "ページ番号が一致しません");
                assertEquals(50, result.getBody().getPagination().getLimit(), 
                        "リミットが一致しません");
                assertEquals(100, result.getBody().getPagination().getTotalItems(), 
                        "総レコード数が一致しません");
                assertEquals(2, result.getBody().getPagination().getTotalPages(), 
                        "総ページ数が一致しません");
                assertNotNull(result.getBody().getPagination().getNextPageToken(), 
                        "次ページトークンがnullです");
                assertEquals("eyJwYWdlIjoyLCJsaW1pdCI6NTB9", 
                        result.getBody().getPagination().getNextPageToken(), 
                        "次ページトークンが一致しません");
                assertTrue(result.getBody().getPagination().isHasMore(),
                        "hasMoreがtrueではありません");
                
                // データ検証
                assertNotNull(result.getBody().getItems(), "Itemsがnullです");
                assertEquals(50, result.getBody().getItems().size(), "データ件数が一致しません");
                
                // モック呼び出し検証
                verify(jwtUtil, times(1)).extractUsername(TEST_TOKEN);
                verify(jwtUtil, times(1)).extractCompanyId(TEST_TOKEN);
                verify(monitoringService, times(1)).readAllMonitoringRecordsSolar(
                        TEST_USERNAME,
                        TEST_COMPANY_ID,
                        TEST_PROJECT_ID,
                        TEST_SITE_ID,
                        1,
                        50,
                        null
                );
        }
        }
    @Test
    @DisplayName("カスタムページサイズで一覧取得成功")
    void testListMonitoringRecords_CustomPageSize_Success() {
        // Given
        Integer customLimit = 20;
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(TEST_COMPANY_ID);

        MonitoringRecordSolarListResponse expectedResponse = new MonitoringRecordSolarListResponse();
        expectedResponse.setStatusCode(ResponseConstants.OK);
        expectedResponse.setMessage("Success");
        
        // PaginationDetailResponseを使用
        PaginationDetailResponse pagination = new PaginationDetailResponse();
        pagination.setPage(1);
        pagination.setLimit(customLimit);
        pagination.setTotalItems(100);  // ✅ totalItems
        pagination.setTotalPages(5);
        pagination.setNextPageToken("eyJwYWdlIjoyLCJsaW1pdCI6MjB9");
        pagination.setHasMore(true);  // ✅ hasMore
        expectedResponse.setPagination(pagination);
        
        // MonitoringTableRecordSolarのリストを作成
        List<EnR002MonitoringRecord> items = new ArrayList<>();
        for (int i = 0; i < customLimit; i++) {
                EnR002MonitoringRecord record = EnR002MonitoringRecord.builder()
                        .id(TEST_SITE_ID + "#" + TEST_PROJECT_ID)
                        .projectID(TEST_PROJECT_ID)
                        .siteID(TEST_SITE_ID)
                        .participatingUserID(100 + i)
                        .dateRecorded("2024-01-15T10:30:00")
                        .elPv(new BigDecimal("150.500"))
                        .timeUpdated("2024-01-15T10:30:00")
                        .isActive(true)
                        .build();
                items.add(record);
        }
        expectedResponse.setItems(items);  // ✅ setItems

        when(monitoringService.readAllMonitoringRecordsSolar(
                eq(TEST_USERNAME),
                eq(TEST_COMPANY_ID),
                eq(TEST_PROJECT_ID),
                eq(TEST_SITE_ID),
                eq(1),
                eq(customLimit),
                eq(null)
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
                mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                        .thenReturn(HttpStatus.OK);

                // When
                ResponseEntity<MonitoringRecordSolarListResponse> result = 
                        monitoringController.listMonitoringRecords(
                                TEST_SITE_ID,
                                TEST_PROJECT_ID,
                                1,
                                customLimit,
                                null,
                                authentication
                        );

                // Then
                assertNotNull(result, "レスポンスがnullです");
                assertEquals(HttpStatus.OK, result.getStatusCode(), "ステータスコードがOKではありません");
                assertNotNull(result.getBody(), "レスポンスボディがnullです");
                assertEquals(ResponseConstants.OK, result.getBody().getStatusCode(), 
                        "statusCodeが一致しません");
                
                // ページネーション検証
                assertNotNull(result.getBody().getPagination(), "Paginationがnullです");
                assertEquals(1, result.getBody().getPagination().getPage(), 
                        "ページ番号が一致しません");
                assertEquals(customLimit, result.getBody().getPagination().getLimit(), 
                        "カスタムリミットが一致しません");
                assertEquals(100, result.getBody().getPagination().getTotalItems(), 
                        "総レコード数が一致しません");
                assertEquals(5, result.getBody().getPagination().getTotalPages(), 
                        "総ページ数が一致しません");
                assertNotNull(result.getBody().getPagination().getNextPageToken(), 
                        "次ページトークンがnullです");
                assertEquals("eyJwYWdlIjoyLCJsaW1pdCI6MjB9", 
                        result.getBody().getPagination().getNextPageToken(), 
                        "次ページトークンが一致しません");
                assertTrue(result.getBody().getPagination().isHasMore(),
                        "hasMoreがtrueではありません");
                
                // データ件数検証
                assertNotNull(result.getBody().getItems(), "Itemsがnullです");
                assertEquals(customLimit, result.getBody().getItems().size(), 
                        "データ件数がカスタムリミットと一致しません");

                verify(jwtUtil, times(1)).extractUsername(TEST_TOKEN);
                verify(jwtUtil, times(1)).extractCompanyId(TEST_TOKEN);
                verify(monitoringService, times(1)).readAllMonitoringRecordsSolar(
                        TEST_USERNAME,
                        TEST_COMPANY_ID,
                        TEST_PROJECT_ID,
                        TEST_SITE_ID,
                        1,
                        customLimit,
                        null
                );
        }
    }

    @Test
    @DisplayName("nextPageTokenで次ページ取得成功")
    void testListMonitoringRecords_WithNextPageToken_Success() {
        // Given
        String nextPageToken = "eyJwYWdlIjoyLCJsaW1pdCI6NTB9";
        Integer page = 2;
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(TEST_COMPANY_ID);

        MonitoringRecordSolarListResponse expectedResponse = new MonitoringRecordSolarListResponse();
        expectedResponse.setStatusCode(ResponseConstants.OK);
        expectedResponse.setMessage("Success");
        
        // PaginationDetailResponseを使用
        PaginationDetailResponse pagination = new PaginationDetailResponse();
        pagination.setPage(page);
        pagination.setLimit(50);
        pagination.setTotalItems(150);  // ✅ totalItems
        pagination.setTotalPages(3);
        pagination.setNextPageToken("eyJwYWdlIjozLCJsaW1pdCI6NTB9");
        pagination.setHasMore(true);// ✅ hasMore
        expectedResponse.setPagination(pagination);
        
        // MonitoringTableRecordSolarのリストを作成
        List<EnR002MonitoringRecord> items = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
                EnR002MonitoringRecord record = EnR002MonitoringRecord.builder()
                        .id(TEST_SITE_ID + "#" + TEST_PROJECT_ID)
                        .projectID(TEST_PROJECT_ID)
                        .siteID(TEST_SITE_ID)
                        .participatingUserID(200 + i)
                        .dateRecorded("2024-01-16T10:30:00")
                        .elPv(new BigDecimal("160.500"))
                        .timeUpdated("2024-01-16T10:30:00")
                        .isActive(true)
                        .build();
                items.add(record);
        }
        expectedResponse.setItems(items);  // ✅ setItems

        when(monitoringService.readAllMonitoringRecordsSolar(
                eq(TEST_USERNAME),
                eq(TEST_COMPANY_ID),
                eq(TEST_PROJECT_ID),
                eq(TEST_SITE_ID),
                eq(page),
                eq(50),
                eq(nextPageToken)
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
                mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                        .thenReturn(HttpStatus.OK);

                // When
                ResponseEntity<MonitoringRecordSolarListResponse> result = 
                        monitoringController.listMonitoringRecords(
                                TEST_SITE_ID,
                                TEST_PROJECT_ID,
                                page,
                                50,
                                nextPageToken,
                                authentication
                        );

                // Then
                assertNotNull(result, "レスポンスがnullです");
                assertEquals(HttpStatus.OK, result.getStatusCode(), "ステータスコードがOKではありません");
                assertNotNull(result.getBody(), "レスポンスボディがnullです");
                assertEquals(ResponseConstants.OK, result.getBody().getStatusCode(), 
                        "statusCodeが一致しません");
                
                //ページネーション検証
                assertNotNull(result.getBody().getPagination(), "Paginationがnullです");
                assertEquals(page, result.getBody().getPagination().getPage(), 
                        "ページ番号が一致しません");
                assertEquals(150, result.getBody().getPagination().getTotalItems(),
                        "総レコード数が一致しません");
                assertEquals(3, result.getBody().getPagination().getTotalPages(),
                        "総ページ数が一致しません");
                assertNotNull(result.getBody().getPagination().getNextPageToken(), 
                        "次ページトークンがnullです");
                assertEquals("eyJwYWdlIjozLCJsaW1pdCI6NTB9", 
                        result.getBody().getPagination().getNextPageToken(), 
                        "次ページトークンが一致しません");assertTrue(result.getBody().getPagination().isHasMore(),
                        "hasMoreがtrueではありません");
                
                // データ検証
                assertNotNull(result.getBody().getItems(), "Itemsがnullです");
                assertEquals(50, result.getBody().getItems().size(), "データ件数が一致しません");

                verify(jwtUtil, times(1)).extractUsername(TEST_TOKEN);
                verify(jwtUtil, times(1)).extractCompanyId(TEST_TOKEN);
                verify(monitoringService, times(1)).readAllMonitoringRecordsSolar(
                        TEST_USERNAME,
                        TEST_COMPANY_ID,
                        TEST_PROJECT_ID,
                        TEST_SITE_ID,
                        page,
                        50,
                        nextPageToken
                );
        }
    }

    @Test
    @DisplayName("ページ3で一覧取得成功")
        void testListMonitoringRecords_Page3_Success() {
        // Given
        Integer page = 3;
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(TEST_COMPANY_ID);

        MonitoringRecordSolarListResponse expectedResponse = new MonitoringRecordSolarListResponse();
        expectedResponse.setStatusCode(ResponseConstants.OK);
        expectedResponse.setMessage("Success");
        
        // PaginationDetailResponseを使用
        PaginationDetailResponse pagination = new PaginationDetailResponse();
        pagination.setPage(page);
        pagination.setLimit(50);
        pagination.setTotalItems(150);  // ✅ totalItems
        pagination.setTotalPages(3);
        pagination.setNextPageToken(null); // 最終ページなのでnull
        pagination.setHasMore(false);  // ✅ hasMore -最終ページなのでfalse
        expectedResponse.setPagination(pagination);
        
        // ページ3なので残り50件
        List<EnR002MonitoringRecord> items = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
                EnR002MonitoringRecord record = EnR002MonitoringRecord.builder()
                        .id(TEST_SITE_ID + "#" + TEST_PROJECT_ID)
                        .projectID(TEST_PROJECT_ID)
                        .siteID(TEST_SITE_ID)
                        .participatingUserID(300 + i) // ページ3なので異なるユーザーID範囲
                        .dateRecorded("2024-01-17T10:30:00")
                        .elPv(new BigDecimal("170.500"))
                        .timeUpdated("2024-01-17T10:30:00")
                        .isActive(true)
                        .build();
                items.add(record);
        }
        expectedResponse.setItems(items);  // ✅ setItems

        when(monitoringService.readAllMonitoringRecordsSolar(
                eq(TEST_USERNAME),
                eq(TEST_COMPANY_ID),
                eq(TEST_PROJECT_ID),
                eq(TEST_SITE_ID),
                eq(page),
                eq(50),
                eq(null)
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
                mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                        .thenReturn(HttpStatus.OK);

                // When
                ResponseEntity<MonitoringRecordSolarListResponse> result = 
                        monitoringController.listMonitoringRecords(
                                TEST_SITE_ID,
                                TEST_PROJECT_ID,
                                page,
                                50,
                                null,
                                authentication
                        );

                // Then
                assertNotNull(result, "レスポンスがnullです");
                assertEquals(HttpStatus.OK, result.getStatusCode(), "HTTPステータスがOKではありません");
                assertNotNull(result.getBody(), "レスポンスボディがnullです");
                assertEquals(ResponseConstants.OK, result.getBody().getStatusCode(), 
                        "statusCodeが一致しません");
                
                // ページネーション検証
                assertNotNull(result.getBody().getPagination(), "Paginationがnullです");
                assertEquals(page, result.getBody().getPagination().getPage(), 
                        "ページ番号が3ではありません");
                assertEquals(50, result.getBody().getPagination().getLimit(), 
                        "リミットが一致しません");
                assertEquals(150, result.getBody().getPagination().getTotalItems(), 
                        "総レコード数が一致しません");
                assertEquals(3, result.getBody().getPagination().getTotalPages(), 
                        "総ページ数が一致しません");
                // 最終ページなので次ページトークンはnull
                assertNull(result.getBody().getPagination().getNextPageToken(), 
                        "最終ページなので次ページトークンはnullであるべき");
                
                //最終ページなのでhasMoreはfalse
                assertFalse(result.getBody().getPagination().isHasMore(),
                        "最終ページなのでhasMoreがfalseであるべき");
                
                // データ検証
                assertNotNull(result.getBody().getItems(), "Itemsがnullです");
                assertEquals(50, result.getBody().getItems().size(), 
                        "ページ3のデータ件数が一致しません");
                
                // データの内容検証（最初のレコード）
                EnR002MonitoringRecord firstRecord = result.getBody().getItems().get(0);
                assertEquals(TEST_PROJECT_ID, firstRecord.getProjectID(), 
                        "プロジェクトIDが一致しません");
                assertEquals(TEST_SITE_ID, firstRecord.getSiteID(), 
                        "サイトIDが一致しません");
                assertEquals(300, firstRecord.getParticipatingUserID(), 
                        "ユーザーIDが一致しません");

                // モック呼び出し検証
                verify(jwtUtil, times(1)).extractUsername(TEST_TOKEN);
                verify(jwtUtil, times(1)).extractCompanyId(TEST_TOKEN);
                verify(monitoringService, times(1)).readAllMonitoringRecordsSolar(
                        TEST_USERNAME,
                        TEST_COMPANY_ID,
                        TEST_PROJECT_ID,
                        TEST_SITE_ID,
                        page,
                        50,
                        null
                );
        }
    }
    @Test
    @DisplayName("ELpv証憑PDFアップロード成功(PDF形式のファイルアップロード)")
    void testUploadElpvEvidence_Success_PDF() {
        // Given
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(TEST_COMPANY_ID);

        MockMultipartFile pdfFile = new MockMultipartFile("file",
                "evidence.pdf",
                "application/pdf",
                "PDF file content".getBytes()
        );

        String description = "ELpv証憑PDFファイル";

        // FileInfoResponse を作成
        FileInfoResponse fileInfo = FileInfoResponse.builder()
                .id("file-123")
                .fileType("PDF")
                .fileName("evidence.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .url("https://storage.example.com/evidence.pdf")
                .description(description)
                .uploadedAt("2024-01-15T10:30:00")
                .build();

        // FileUploadResponse を作成
        FileUploadResponse expectedResponse = new FileUploadResponse();
        expectedResponse.setStatusCode("200");
        expectedResponse.setMessage("PDF upload successful");
        expectedResponse.setFile(fileInfo);

        when(monitoringService.uploadElpvEvidence(
                TEST_USERNAME,
                TEST_COMPANY_ID,
                TEST_PROJECT_ID,
                TEST_SITE_ID,
                description,
                pdfFile
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
                mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("200"))
                        .thenReturn(HttpStatus.OK);

                // When
                ResponseEntity<FileUploadResponse> result = 
                monitoringController.uploadElpvEvidence(
                        authentication,
                        TEST_PROJECT_ID,
                        TEST_SITE_ID,
                        description,
                        pdfFile
                );

                // Then
                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(result.getBody()).isNotNull();
                assertThat(result.getBody().getStatusCode()).isEqualTo("200");
                assertThat(result.getBody().getMessage()).isEqualTo("PDF upload successful");
                // FileInfoResponse の検証
                assertThat(result.getBody().getFile()).isNotNull();
                assertThat(result.getBody().getFile().getId()).isEqualTo("file-123");
                assertThat(result.getBody().getFile().getFileType()).isEqualTo("PDF");
                assertThat(result.getBody().getFile().getFileName()).isEqualTo("evidence.pdf");
                assertThat(result.getBody().getFile().getContentType()).isEqualTo("application/pdf");
                assertThat(result.getBody().getFile().getFileSize()).isEqualTo(1024L);
                assertThat(result.getBody().getFile().getUrl()).isEqualTo("https://storage.example.com/evidence.pdf");
                assertThat(result.getBody().getFile().getDescription()).isEqualTo(description);
                assertThat(result.getBody().getFile().getUploadedAt()).isEqualTo("2024-01-15T10:30:00");

                // 検証
                verify(authentication, times(2)).getCredentials();
                verify(jwtUtil).extractUsername(TEST_TOKEN);
                verify(jwtUtil).extractCompanyId(TEST_TOKEN);
                verify(monitoringService).uploadElpvEvidence(
                        TEST_USERNAME,
                        TEST_COMPANY_ID,
                        TEST_PROJECT_ID,
                        TEST_SITE_ID,
                        description,
                        pdfFile
                );
        }
    }

    @Test
    @DisplayName("ELpv証憑CSVアップロード成功(CSV形式のファイルアップロード)")
        void testUploadElpvEvidence_Success_CSV() {
        // Given
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(TEST_COMPANY_ID);

        String csvContent = "timestamp,power,voltage\n2024-01-01,100,220\n2024-01-02,105,221";
        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "evidence.csv",
                "text/csv",
                csvContent.getBytes()
        );

        String description = "ELpv証憑CSVファイル";

        // FileInfoResponse を作成
        FileInfoResponse fileInfo = FileInfoResponse.builder()
                .id("file-456")
                .fileType("CSV")
                .fileName("evidence.csv")
                .contentType("text/csv")
                .fileSize(2048L)
                .url("https://storage.example.com/evidence.csv")
                .description(description)
                .uploadedAt("2024-01-15T10:35:00")
                .build();

        // FileUploadResponse を作成
        FileUploadResponse expectedResponse = new FileUploadResponse();
        expectedResponse.setStatusCode("200");
        expectedResponse.setMessage("CSV upload successful");
        expectedResponse.setFile(fileInfo);

        when(monitoringService.uploadElpvEvidence(
                TEST_USERNAME,
                TEST_COMPANY_ID,
                TEST_PROJECT_ID,
                TEST_SITE_ID,
                description,
                csvFile
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
                mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("200"))
                        .thenReturn(HttpStatus.OK);

                // When
                ResponseEntity<FileUploadResponse> result = 
                monitoringController.uploadElpvEvidence(
                        authentication,
                        TEST_PROJECT_ID,
                        TEST_SITE_ID,
                        description,
                        csvFile
                );

                // Then
                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(result.getBody()).isNotNull();
                assertThat(result.getBody().getStatusCode()).isEqualTo("200");
                assertThat(result.getBody().getMessage()).isEqualTo("CSV upload successful");
                
                // FileInfoResponse の検証
                assertThat(result.getBody().getFile()).isNotNull();
                assertThat(result.getBody().getFile().getId()).isEqualTo("file-456");
                assertThat(result.getBody().getFile().getFileType()).isEqualTo("CSV");
                assertThat(result.getBody().getFile().getFileName()).isEqualTo("evidence.csv");
                assertThat(result.getBody().getFile().getContentType()).isEqualTo("text/csv");
                assertThat(result.getBody().getFile().getFileSize()).isEqualTo(2048L);
                assertThat(result.getBody().getFile().getUrl()).isEqualTo("https://storage.example.com/evidence.csv");
                assertThat(result.getBody().getFile().getDescription()).isEqualTo(description);

                // 検証
                verify(authentication, times(2)).getCredentials();
                verify(jwtUtil).extractUsername(TEST_TOKEN);
                verify(jwtUtil).extractCompanyId(TEST_TOKEN);
                verify(monitoringService).uploadElpvEvidence(
                        TEST_USERNAME,
                        TEST_COMPANY_ID,
                        TEST_PROJECT_ID,
                        TEST_SITE_ID,
                        description,
                        csvFile
                );
        }
    }

    @Test
    @DisplayName("ELpv証憑画像アップロード成功（description付き）")
    void testUploadElpvEvidence_Success_ImageWithDescription() {
        // Given
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(TEST_COMPANY_ID);

        byte[] imageContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "evidence.png",
                "image/png",
                imageContent
        );

        String description = "太陽光パネルの現場写真 - 2024年1月撮影";

        // FileInfoResponse を作成
        FileInfoResponse fileInfo = FileInfoResponse.builder()
                .id("file-789")
                .fileType("IMAGE")
                .fileName("evidence.png")
                .contentType("image/png")
                .fileSize(4096L)
                .url("https://storage.example.com/evidence.png")
                .description(description)
                .uploadedAt("2024-01-15T10:40:00")
                .build();

        // FileUploadResponse を作成
        FileUploadResponse expectedResponse = new FileUploadResponse();
        expectedResponse.setStatusCode("200");
        expectedResponse.setMessage("Image upload successful");
        expectedResponse.setFile(fileInfo);

        when(monitoringService.uploadElpvEvidence(
                TEST_USERNAME,
                TEST_COMPANY_ID,
                TEST_PROJECT_ID,
                TEST_SITE_ID,
                description,
                imageFile
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
                mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("200"))
                        .thenReturn(HttpStatus.OK);

                // When
                ResponseEntity<FileUploadResponse> result = 
                monitoringController.uploadElpvEvidence(
                        authentication,
                        TEST_PROJECT_ID,
                        TEST_SITE_ID,
                        description,
                        imageFile
                );

                // Then
                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(result.getBody()).isNotNull();
                assertThat(result.getBody().getStatusCode()).isEqualTo("200");
                assertThat(result.getBody().getMessage()).isEqualTo("Image upload successful");
                
                // FileInfoResponse の検証
                assertThat(result.getBody().getFile()).isNotNull();
                assertThat(result.getBody().getFile().getId()).isEqualTo("file-789");
                assertThat(result.getBody().getFile().getFileType()).isEqualTo("IMAGE");
                assertThat(result.getBody().getFile().getFileName()).isEqualTo("evidence.png");
                assertThat(result.getBody().getFile().getContentType()).isEqualTo("image/png");
                assertThat(result.getBody().getFile().getFileSize()).isEqualTo(4096L);
                assertThat(result.getBody().getFile().getUrl()).isEqualTo("https://storage.example.com/evidence.png");
                assertThat(result.getBody().getFile().getDescription()).isEqualTo(description);
                assertThat(result.getBody().getFile().getUploadedAt()).isEqualTo("2024-01-15T10:40:00");

                // 検証
                verify(authentication, times(2)).getCredentials();
                verify(jwtUtil).extractUsername(TEST_TOKEN);
                verify(jwtUtil).extractCompanyId(TEST_TOKEN);
                verify(monitoringService).uploadElpvEvidence(
                        TEST_USERNAME,
                        TEST_COMPANY_ID,
                        TEST_PROJECT_ID,
                        TEST_SITE_ID,
                        description,
                        imageFile
                );
        }
    }

    @Test
    @DisplayName("ELpv証憑画像アップロード成功（description無し）")
    void testUploadElpvEvidence_Success_ImageWithoutDescription() {
        // Given
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(TEST_COMPANY_ID);

        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "evidence.jpg",
                "image/jpeg",
                "JPEG image content".getBytes()
        );

        // FileInfoResponse を作成（description無し）
        FileInfoResponse fileInfo = FileInfoResponse.builder()
                .id("file-101")
                .fileType("IMAGE")
                .fileName("evidence.jpg")
                .contentType("image/jpeg")
                .fileSize(3072L)
                .url("https://storage.example.com/evidence.jpg")
                .description(null)  // description無し
                .uploadedAt("2024-01-15T10:45:00")
                .build();

        FileUploadResponse expectedResponse = new FileUploadResponse();
        expectedResponse.setStatusCode("200");
        expectedResponse.setMessage("Image upload successful");
        expectedResponse.setFile(fileInfo);

        when(monitoringService.uploadElpvEvidence(
                TEST_USERNAME,
                TEST_COMPANY_ID,
                TEST_PROJECT_ID,
                TEST_SITE_ID,
                null,  // description無し
                imageFile
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
                mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("200"))
                        .thenReturn(HttpStatus.OK);

                // When
                ResponseEntity<FileUploadResponse> result = 
                monitoringController.uploadElpvEvidence(
                        authentication,
                        TEST_PROJECT_ID,
                        TEST_SITE_ID,
                        null,  // description無し
                        imageFile
                );

                // Then
                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(result.getBody()).isNotNull();
                assertThat(result.getBody().getStatusCode()).isEqualTo("200");
                assertThat(result.getBody().getMessage()).isEqualTo("Image upload successful");
                
                assertThat(result.getBody().getFile()).isNotNull();
                assertThat(result.getBody().getFile().getFileName()).isEqualTo("evidence.jpg");
                assertThat(result.getBody().getFile().getDescription()).isNull();

                verify(monitoringService).uploadElpvEvidence(
                        TEST_USERNAME,
                        TEST_COMPANY_ID,
                        TEST_PROJECT_ID,
                        TEST_SITE_ID,
                        null,
                        imageFile
                );
        }
    }

  @Test
  @DisplayName("バッテリー仕様書アップロード成功")
    void testUploadElpjBatterySpecificationEvidence_Success() {
    // Given
    when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
    when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
    when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(TEST_COMPANY_ID);

    // バッテリー仕様書PDFファイルのモック作成
    MockMultipartFile specificationFile = new MockMultipartFile(
            "file",
            "battery_specification.pdf",
            "application/pdf",
            "Battery specification PDF content".getBytes()
    );

    String description = "蓄電池仕様書 - メーカー型番XYZ-1000";

    // FileInfoResponse を作成
    FileInfoResponse fileInfo = FileInfoResponse.builder()
            .id("file-spec-001")
            .fileType("PDF")
            .fileName("battery_specification.pdf")
            .contentType("application/pdf")
            .fileSize(5120L)
            .url("https://storage.example.com/battery_specification.pdf")
            .description(description)
            .uploadedAt("2024-01-15T11:00:00")
            .build();

    // FileUploadResponse を作成
    FileUploadResponse expectedResponse = new FileUploadResponse();
    expectedResponse.setStatusCode("200");
    expectedResponse.setMessage("Battery specification upload successful");
    expectedResponse.setFile(fileInfo);

    when(monitoringService.uploadElpjBatterySpecificationEvidence(
            TEST_USERNAME,
            TEST_COMPANY_ID,
            TEST_PROJECT_ID,
            TEST_SITE_ID,
            description,
            specificationFile
    )).thenReturn(expectedResponse);

    try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
        mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("200"))
                .thenReturn(HttpStatus.OK);

        // When
        ResponseEntity<FileUploadResponse> result = 
            monitoringController.uploadElpjBatterySpecificationEvidence(
                authentication,
                TEST_PROJECT_ID,
                TEST_SITE_ID,
                description,
                specificationFile
        );

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getStatusCode()).isEqualTo("200");
        assertThat(result.getBody().getMessage()).isEqualTo("Battery specification upload successful");
        
        // FileInfoResponse の検証
        assertThat(result.getBody().getFile()).isNotNull();
        assertThat(result.getBody().getFile().getFileName()).isEqualTo("battery_specification.pdf");
        assertThat(result.getBody().getFile().getContentType()).isEqualTo("application/pdf");
        assertThat(result.getBody().getFile().getUrl()).isEqualTo("https://storage.example.com/battery_specification.pdf");
        assertThat(result.getBody().getFile().getDescription()).isEqualTo(description);

        // 検証
        verify(authentication, times(2)).getCredentials();
        verify(jwtUtil).extractUsername(TEST_TOKEN);
        verify(jwtUtil).extractCompanyId(TEST_TOKEN);
        verify(monitoringService).uploadElpjBatterySpecificationEvidence(
                TEST_USERNAME,
                TEST_COMPANY_ID,
                TEST_PROJECT_ID,
                TEST_SITE_ID,
                description,
                specificationFile
            );
        }
    }

    @Test
    @DisplayName("制御装置証憑アップロード成功")
    void testUploadElpjControlEvidence_Success_PDF() {  // ← 名前を変更
        // Given
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(TEST_COMPANY_ID);

        // 制御装置証憑ファイルのモック作成
        MockMultipartFile controlFile = new MockMultipartFile(
                "file",
                "control_device.pdf",
                "application/pdf",
                "Control device evidence PDF content".getBytes()
        );

        String description = "制御装置証憑 - 型番ABC-2000";

        // FileInfoResponse を作成
        FileInfoResponse fileInfo = FileInfoResponse.builder()
                .id("file-control-001")
                .fileType("PDF")
                .fileName("control_device.pdf")
                .contentType("application/pdf")
                .fileSize(6144L)
                .url("https://storage.example.com/control_device.pdf")
                .description(description)
                .uploadedAt("2024-01-15T11:15:00")
                .build();

        // FileUploadResponse を作成
        FileUploadResponse expectedResponse = new FileUploadResponse();
        expectedResponse.setStatusCode("200");
        expectedResponse.setMessage("Control device evidence upload successful");
        expectedResponse.setFile(fileInfo);

        when(monitoringService.uploadElpjControlEvidence(
                TEST_USERNAME,
                TEST_COMPANY_ID,
                TEST_PROJECT_ID,
                TEST_SITE_ID,
                description,
                controlFile
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
                mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("200"))
                        .thenReturn(HttpStatus.OK);

                // When
                ResponseEntity<FileUploadResponse> result = 
                monitoringController.uploadElpjControlEvidence(
                        authentication,
                        TEST_PROJECT_ID,
                        TEST_SITE_ID,
                        description,
                        controlFile
                );

                // Then
                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(result.getBody()).isNotNull();
                assertThat(result.getBody().getStatusCode()).isEqualTo("200");
                assertThat(result.getBody().getMessage()).isEqualTo("Control device evidence upload successful");
                
                // FileInfoResponse の検証
                assertThat(result.getBody().getFile()).isNotNull();
                assertThat(result.getBody().getFile().getFileName()).isEqualTo("control_device.pdf");
                assertThat(result.getBody().getFile().getContentType()).isEqualTo("application/pdf");
                assertThat(result.getBody().getFile().getUrl()).isEqualTo("https://storage.example.com/control_device.pdf");
                assertThat(result.getBody().getFile().getDescription()).isEqualTo(description);

                // 検証
                verify(authentication, times(2)).getCredentials();
                verify(jwtUtil).extractUsername(TEST_TOKEN);
                verify(jwtUtil).extractCompanyId(TEST_TOKEN);
                verify(monitoringService).uploadElpjControlEvidence(
                        TEST_USERNAME,
                        TEST_COMPANY_ID,
                        TEST_PROJECT_ID,
                        TEST_SITE_ID,
                        description,
                        controlFile
                );
        }
    }
    @Test
    @DisplayName("取消申請作成成功（masterユーザー）")
    void testCreateCancelRequest_Success_MasterUser() {
        // Given
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(TEST_COMPANY_ID);

        CreateCancelRequestRequest request = CreateCancelRequestRequest.builder()
                .siteId(TEST_SITE_ID)
                .recordPK("MONITORING#PROJECT#" + TEST_PROJECT_ID)
                .recordSK("SITE#" + TEST_SITE_ID + "#2024-01-15")
                .reasonText("設備の不具合により取消を申請します")
                .build();

        CancelRequestResponse expectedResponse = CancelRequestResponse.builder()
                .cancelRequestId(100)
                .recordPk("MONITORING#PROJECT#" + TEST_PROJECT_ID)
                .recordSk("SITE#" + TEST_SITE_ID + "#2024-01-15")
                .status("PENDING")
                .requestedBy(TEST_USERNAME)
                .requestedAt("2024-01-15T12:00:00")
                .siteId(TEST_SITE_ID)
                .reason("設備の不具合により取消を申請します")
                .build();
        expectedResponse.setStatusCode("201");
        expectedResponse.setMessage("Cancel request created successfully");

        when(cancelRequestService.createCancelRequest(
                TEST_USERNAME,
                TEST_COMPANY_ID,
                request
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
                mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("201"))
                        .thenReturn(HttpStatus.CREATED);

                // When
                ResponseEntity<CancelRequestResponse> result = 
                monitoringController.createCancelRequest(
                        authentication,
                        request
                );

                // Then
                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(result.getBody()).isNotNull();
                assertThat(result.getBody().getStatusCode()).isEqualTo("201");
                assertThat(result.getBody().getMessage()).isEqualTo("Cancel request created successfully");
                assertThat(result.getBody().getCancelRequestId()).isEqualTo(100);
                assertThat(result.getBody().getRecordPk()).isEqualTo("MONITORING#PROJECT#" + TEST_PROJECT_ID);
                assertThat(result.getBody().getRecordSk()).isEqualTo("SITE#" + TEST_SITE_ID + "#2024-01-15");
                assertThat(result.getBody().getStatus()).isEqualTo("PENDING");
                assertThat(result.getBody().getRequestedBy()).isEqualTo(TEST_USERNAME);
                assertThat(result.getBody().getRequestedAt()).isEqualTo("2024-01-15T12:00:00");
                assertThat(result.getBody().getSiteId()).isEqualTo(TEST_SITE_ID);
                assertThat(result.getBody().getReason()).isEqualTo("設備の不具合により取消を申請します");

                // 検証
                verify(authentication, times(2)).getCredentials();
                verify(jwtUtil).extractUsername(TEST_TOKEN);
                verify(jwtUtil).extractCompanyId(TEST_TOKEN);
                verify(cancelRequestService).createCancelRequest(
                        TEST_USERNAME,
                        TEST_COMPANY_ID,
                        request
                );
        }
    }

    @Test
    @DisplayName("取消申請作成成功（userユーザー）")
    void testCreateCancelRequest_Success_UserUser() {
        // Given
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn("user001");
        when(jwtUtil.extractCompanyId(TEST_TOKEN)).thenReturn(TEST_COMPANY_ID);

        CreateCancelRequestRequest request = CreateCancelRequestRequest.builder()
                .siteId(TEST_SITE_ID)
                .recordPK("MONITORING#PROJECT#" + TEST_PROJECT_ID)
                .recordSK("SITE#" + TEST_SITE_ID + "#2024-01-20")
                .reasonText("データ入力誤りのため取消を申請します")
                .build();

        CancelRequestResponse expectedResponse = CancelRequestResponse.builder()
                .cancelRequestId(101)
                .recordPk("MONITORING#PROJECT#" + TEST_PROJECT_ID)
                .recordSk("SITE#" + TEST_SITE_ID + "#2024-01-20")
                .status("PENDING")
                .requestedBy("user001")
                .requestedAt("2024-01-20T12:05:00")
                .siteId(TEST_SITE_ID)
                .reason("データ入力誤りのため取消を申請します")
                .build();
        expectedResponse.setStatusCode("201");
        expectedResponse.setMessage("Cancel request created successfully");

        when(cancelRequestService.createCancelRequest(
                "user001",
                TEST_COMPANY_ID,
                request
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
                mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("201"))
                        .thenReturn(HttpStatus.CREATED);

                // When
                ResponseEntity<CancelRequestResponse> result = 
                monitoringController.createCancelRequest(
                        authentication,
                        request
                );

                // Then
                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(result.getBody()).isNotNull();
                assertThat(result.getBody().getStatusCode()).isEqualTo("201");
                assertThat(result.getBody().getMessage()).isEqualTo("Cancel request created successfully");
                assertThat(result.getBody().getCancelRequestId()).isEqualTo(101);
                assertThat(result.getBody().getRecordPk()).isEqualTo("MONITORING#PROJECT#" + TEST_PROJECT_ID);
                assertThat(result.getBody().getRecordSk()).isEqualTo("SITE#" + TEST_SITE_ID + "#2024-01-20");
                assertThat(result.getBody().getStatus()).isEqualTo("PENDING");
                assertThat(result.getBody().getRequestedBy()).isEqualTo("user001");
                assertThat(result.getBody().getRequestedAt()).isEqualTo("2024-01-20T12:05:00");
                assertThat(result.getBody().getSiteId()).isEqualTo(TEST_SITE_ID);
                assertThat(result.getBody().getReason()).isEqualTo("データ入力誤りのため取消を申請します");

                // 検証
                verify(authentication, times(2)).getCredentials();
                verify(jwtUtil).extractUsername(TEST_TOKEN);
                verify(jwtUtil).extractCompanyId(TEST_TOKEN);
                verify(cancelRequestService).createCancelRequest(
                        "user001",
                        TEST_COMPANY_ID,
                        request
                );
        }
    }

    @Test
    @DisplayName("取消申請一覧取得成功")
    void testGetCancelRequests_Success() {
        // Given

        CancelRequestResponse request1 = CancelRequestResponse.builder()
                .cancelRequestId(100)
                .recordPk("MONITORING#PROJECT#" + TEST_PROJECT_ID)
                .recordSk("SITE#" + TEST_SITE_ID + "#2024-01-15")
                .status("PENDING")
                .requestedBy(TEST_USERNAME)
                .requestedAt("2024-01-15T12:00:00")
                .siteId(TEST_SITE_ID)
                .reason("設備の不具合により取消を申請します")
                .build();

        CancelRequestResponse request2 = CancelRequestResponse.builder()
                .cancelRequestId(101)
                .recordPk("MONITORING#PROJECT#" + TEST_PROJECT_ID)
                .recordSk("SITE#" + TEST_SITE_ID + "#2024-01-20")
                .status("PENDING")
                .requestedBy("user001")
                .requestedAt("2024-01-20T14:30:00")
                .siteId(TEST_SITE_ID)
                .reason("データ入力誤りのため取消を申請します")
                .build();

        CancelRequestResponse request3 = CancelRequestResponse.builder()
                .cancelRequestId(102)
                .recordPk("MONITORING#PROJECT#2")
                .recordSk("SITE#200#2024-01-22")
                .status("APPROVED")
                .requestedBy("user002")
                .requestedAt("2024-01-22T09:15:00")
                .siteId(200)
                .reason("計測機器の故障のため取消を申請します")
                .build();

        CancelRequestListResponse expectedResponse = new CancelRequestListResponse();
        expectedResponse.setStatusCode("200");
        expectedResponse.setMessage("Cancel requests retrieved successfully");
        //実際のフィールド名に応じて以下のいずれかを使用
        // expectedResponse.setData(Arrays.asList(request1, request2, request3));
        // expectedResponse.setItems(Arrays.asList(request1, request2, request3));
        // expectedResponse.setRequests(Arrays.asList(request1, request2, request3));
        // expectedResponse.setCount(3);
        // expectedResponse.setSize(3);

        when(cancelRequestService.listCancelRequests()).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
                mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("200"))
                        .thenReturn(HttpStatus.OK);

                // When
                ResponseEntity<CancelRequestListResponse> result = 
                monitoringController.getCancelRequests(authentication);

                // Then
                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(result.getBody()).isNotNull();
                assertThat(result.getBody().getStatusCode()).isEqualTo("200");
                assertThat(result.getBody().getMessage()).isEqualTo("Cancel requests retrieved successfully");
                // 実際のメソッド名に応じて修正してください
                // assertThat(result.getBody().getData()).hasSize(3);
                // assertThat(result.getBody().getItems()).hasSize(3);
                // assertThat(result.getBody().getRequests()).hasSize(3);

                // 検証
                verify(cancelRequestService).listCancelRequests();
        }
    }
    @Test
    @DisplayName("空の取り消し申請一覧取得成功")
    void testGetCancelRequests_Success_EmptyList() {
        // Given
        CancelRequestListResponse expectedResponse = CancelRequestListResponse.builder()
                .items(Collections.emptyList())
                .build();
        expectedResponse.setStatusCode("200");
        expectedResponse.setMessage("No cancel requests found");

        when(cancelRequestService.listCancelRequests()).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
                mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("200"))
                        .thenReturn(HttpStatus.OK);

                // When
                ResponseEntity<CancelRequestListResponse> result = 
                monitoringController.getCancelRequests(authentication);

                // Then
                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(result.getBody()).isNotNull();
                assertThat(result.getBody().getStatusCode()).isEqualTo("200");
                assertThat(result.getBody().getMessage()).isEqualTo("No cancel requests found");
                assertThat(result.getBody().getItems()).isEmpty();

                // 検証
                verify(cancelRequestService).listCancelRequests();
        }
    }

    @Test
    @DisplayName("取消申請承認成功")
    void testMakeDecision_Approve_Success() {
        // Given
        Integer cancelRequestId = 100;
        CancelRequestDecisionRequest request = CancelRequestDecisionRequest.builder()
                .decision("APPROVE")
                .managerRemarks("承認します")
                .build();
        
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        
        CancelRequestDecisionResponse expectedResponse = CancelRequestDecisionResponse.builder()
                .cancelRequestId(cancelRequestId)
                .status("APPROVED")
                .decidedBy(TEST_USERNAME)
                .decidedAt("2024-01-23 10:30:00")
                .originalRecordPk("MONITORING#PROJECT#" + TEST_PROJECT_ID)
                .originalRecordSk("SITE#1#2024-01-15T09:00:00")
                .offsetRecordPk("MONITORING#PROJECT#" + TEST_PROJECT_ID)
                .offsetRecordSk("SITE#1#2024-01-23T10:30:00#OFFSET")
                .managerRemarks("承認します")
                .build();
        expectedResponse.setStatusCode("200");
        expectedResponse.setMessage("Cancel request approved successfully");
        
        when(cancelRequestService.processDecision(cancelRequestId, request, TEST_USERNAME))
                .thenReturn(expectedResponse);
        
        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
                mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("200"))
                        .thenReturn(HttpStatus.OK);
                
                // When
                ResponseEntity<CancelRequestDecisionResponse> result = 
                monitoringController.makeDecision(authentication, cancelRequestId, request);
                
                // Then
                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(result.getBody()).isNotNull();
                assertThat(result.getBody().getStatusCode()).isEqualTo("200");
                assertThat(result.getBody().getMessage()).isEqualTo("Cancel request approved successfully");
                assertThat(result.getBody().getCancelRequestId()).isEqualTo(cancelRequestId);
                assertThat(result.getBody().getStatus()).isEqualTo("APPROVED");
                assertThat(result.getBody().getDecidedBy()).isEqualTo(TEST_USERNAME);
                assertThat(result.getBody().getDecidedAt()).isEqualTo("2024-01-23 10:30:00");
                assertThat(result.getBody().getOriginalRecordPk()).isEqualTo("MONITORING#PROJECT#" + TEST_PROJECT_ID);
                assertThat(result.getBody().getOriginalRecordSk()).isEqualTo("SITE#1#2024-01-15T09:00:00");
                assertThat(result.getBody().getOffsetRecordPk()).isEqualTo("MONITORING#PROJECT#" + TEST_PROJECT_ID);
                assertThat(result.getBody().getOffsetRecordSk()).isEqualTo("SITE#1#2024-01-23T10:30:00#OFFSET");
                assertThat(result.getBody().getManagerRemarks()).isEqualTo("承認します");
                
                // 検証
                verify(jwtUtil).extractUsername(TEST_TOKEN);
                verify(cancelRequestService).processDecision(cancelRequestId, request, TEST_USERNAME);
        }
    }

    @Test
    @DisplayName("取消申請却下成功")
    void testMakeDecision_Reject_Success() {
        // Given
        Integer cancelRequestId = 101;
        
        CancelRequestDecisionRequest request = CancelRequestDecisionRequest.builder()
                .decision("REJECT")
                .managerRemarks("理由が不十分なため却下します")
                .build();
        
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        
        CancelRequestDecisionResponse expectedResponse = CancelRequestDecisionResponse.builder()
                .cancelRequestId(cancelRequestId)
                .status("REJECTED")
                .decidedBy(TEST_USERNAME)
                .decidedAt("2024-01-23 11:00:00")
                .originalRecordPk("MONITORING#PROJECT#" + TEST_PROJECT_ID)
                .originalRecordSk("SITE#2#2024-01-20T14:30:00")
                .offsetRecordPk(null)  // 却下時は相殺レコード無し
                .offsetRecordSk(null)  // 却下時は相殺レコード無し
                .managerRemarks("理由が不十分なため却下します")
                .build();
        expectedResponse.setStatusCode("200");
        expectedResponse.setMessage("Cancel request rejected successfully");
        
        when(cancelRequestService.processDecision(cancelRequestId, request, TEST_USERNAME))
                .thenReturn(expectedResponse);
        
        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
                mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("200"))
                        .thenReturn(HttpStatus.OK);
                
                // When
                ResponseEntity<CancelRequestDecisionResponse> result = 
                monitoringController.makeDecision(authentication, cancelRequestId, request);
                
                // Then
                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(result.getBody()).isNotNull();
                assertThat(result.getBody().getStatusCode()).isEqualTo("200");
                assertThat(result.getBody().getMessage()).isEqualTo("Cancel request rejected successfully");
                assertThat(result.getBody().getCancelRequestId()).isEqualTo(cancelRequestId);
                assertThat(result.getBody().getStatus()).isEqualTo("REJECTED");
                assertThat(result.getBody().getDecidedBy()).isEqualTo(TEST_USERNAME);
                assertThat(result.getBody().getDecidedAt()).isEqualTo("2024-01-23 11:00:00");
                assertThat(result.getBody().getOriginalRecordPk()).isEqualTo("MONITORING#PROJECT#" + TEST_PROJECT_ID);
                assertThat(result.getBody().getOriginalRecordSk()).isEqualTo("SITE#2#2024-01-20T14:30:00");
                assertThat(result.getBody().getOffsetRecordPk()).isNull();
                assertThat(result.getBody().getOffsetRecordSk()).isNull();
                assertThat(result.getBody().getManagerRemarks()).isEqualTo("理由が不十分なため却下します");
                
                // 検証
                verify(jwtUtil).extractUsername(TEST_TOKEN);
                verify(cancelRequestService).processDecision(cancelRequestId, request, TEST_USERNAME);
        }
    }

    @Test
    @DisplayName("パスパラメータとリクエストボディの処理")
    void testMakeDecision_PathParamAndRequestBody() {
        // Given - 複数のパターンをテスト
        Integer cancelRequestId = 999;
        CancelRequestDecisionRequest request = CancelRequestDecisionRequest.builder()
                .decision("APPROVE")
                .managerRemarks("特別な承認理由")
                .build();
        
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn("operator123");
        
        CancelRequestDecisionResponse expectedResponse = CancelRequestDecisionResponse.builder()
                .cancelRequestId(cancelRequestId)
                .status("APPROVED")
                .decidedBy("operator123")
                .decidedAt("2024-01-23 12:00:00")
                .originalRecordPk("MONITORING#PROJECT#3")
                .originalRecordSk("SITE#5#2024-01-22T08:00:00")
                .offsetRecordPk("MONITORING#PROJECT#3")
                .offsetRecordSk("SITE#5#2024-01-23T12:00:00#OFFSET")
                .managerRemarks("特別な承認理由")
                .build();
        expectedResponse.setStatusCode("200");
        expectedResponse.setMessage("Decision processed successfully");
        
        when(cancelRequestService.processDecision(cancelRequestId, request, "operator123"))
                .thenReturn(expectedResponse);
        
        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
                mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("200"))
                        .thenReturn(HttpStatus.OK);
                
                // When
                ResponseEntity<CancelRequestDecisionResponse> result = 
                monitoringController.makeDecision(authentication, cancelRequestId, request);
                
                // Then
                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(result.getBody()).isNotNull();
                
                // パスパラメータが正しく処理されているか
                assertThat(result.getBody().getCancelRequestId()).isEqualTo(cancelRequestId);
                
                // リクエストボディの内容が正しく処理されているか
                assertThat(result.getBody().getManagerRemarks()).isEqualTo(request.getManagerRemarks());
                
                // JWTから抽出したユーザー名が使用されているか
                assertThat(result.getBody().getDecidedBy()).isEqualTo("operator123");
                
                // APPROVEの場合は相殺レコードが作成されているか
                assertThat(result.getBody().getOffsetRecordPk()).isNotNull();
                assertThat(result.getBody().getOffsetRecordSk()).isNotNull();
                assertThat(result.getBody().getOffsetRecordSk()).contains("#OFFSET");
                
                // 検証: サービスが正しいパラメータで呼ばれているか
                ArgumentCaptor<Integer> idCaptor = ArgumentCaptor.forClass(Integer.class);
                ArgumentCaptor<CancelRequestDecisionRequest> requestCaptor = 
                ArgumentCaptor.forClass(CancelRequestDecisionRequest.class);
                ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
                
                verify(cancelRequestService).processDecision(
                idCaptor.capture(), 
                requestCaptor.capture(), 
                usernameCaptor.capture()
                );
                
                assertThat(idCaptor.getValue()).isEqualTo(cancelRequestId);
                assertThat(requestCaptor.getValue().getDecision()).isEqualTo("APPROVE");
                assertThat(requestCaptor.getValue().getManagerRemarks()).isEqualTo("特別な承認理由");
                assertThat(usernameCaptor.getValue()).isEqualTo("operator123");
        }
    }
/* 
    @Test
    @DisplayName("取消申請承認失敗- 存在しないID")
    void testMakeDecision_Approve_NotFound() {
        // Given
        Integer nonExistentId = 999;
        
        CancelRequestDecisionRequest request = CancelRequestDecisionRequest.builder()
                .decision("APPROVE")
                .managerRemarks("承認します")
                .build();
        
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        
        CancelRequestDecisionResponse expectedResponse = new CancelRequestDecisionResponse();
        expectedResponse.setStatusCode("404");
        expectedResponse.setMessage("Cancel request not found");
        
        when(cancelRequestService.processDecision(nonExistentId, request, TEST_USERNAME))
                .thenReturn(expectedResponse);
        
        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
                mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("404"))
                        .thenReturn(HttpStatus.NOT_FOUND);
                
                // When
                ResponseEntity<CancelRequestDecisionResponse> result = 
                monitoringController.makeDecision(authentication, nonExistentId, request);
                
                // Then
                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(result.getBody()).isNotNull();
                assertThat(result.getBody().getStatusCode()).isEqualTo("404");
                assertThat(result.getBody().getMessage()).isEqualTo("Cancel request not found");
                
                // 検証
                verify(cancelRequestService).processDecision(nonExistentId, request, TEST_USERNAME);
        }
    }

    @Test
    @DisplayName("取消申請承認失敗 - 既に処理済み")
    void testMakeDecision_Approve_AlreadyProcessed() {
        // Given
        Integer cancelRequestId = 100;
        
        CancelRequestDecisionRequest request = CancelRequestDecisionRequest.builder()
                .decision("APPROVE")
                .managerRemarks("承認します")
                .build();
        
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        
        CancelRequestDecisionResponse expectedResponse = new CancelRequestDecisionResponse();
        expectedResponse.setStatusCode("400");
        expectedResponse.setMessage("Cancel request has already been processed");
        
        when(cancelRequestService.processDecision(cancelRequestId, request, TEST_USERNAME))
                .thenReturn(expectedResponse);
        
        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
                mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("400"))
                        .thenReturn(HttpStatus.BAD_REQUEST);
                
                // When
                ResponseEntity<CancelRequestDecisionResponse> result = 
                monitoringController.makeDecision(authentication, cancelRequestId, request);
                
                // Then
                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(result.getBody()).isNotNull();
                assertThat(result.getBody().getStatusCode()).isEqualTo("400");
                assertThat(result.getBody().getMessage()).isEqualTo("Cancel request has already been processed");
                
                // 検証
                verify(cancelRequestService).processDecision(cancelRequestId, request, TEST_USERNAME);
        }
    }

    @Test
    @DisplayName("取消申請却下 - コメント無し")
    void testMakeDecision_Reject_WithoutComment() {
        // Given
        Integer cancelRequestId = 102;
        
        CancelRequestDecisionRequest request = CancelRequestDecisionRequest.builder()
                .decision("REJECT")
                .managerRemarks(null)  // コメントなし
                .build();
        
        when(authentication.getCredentials()).thenReturn(TEST_TOKEN);
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        
        CancelRequestDecisionResponse expectedResponse = CancelRequestDecisionResponse.builder()
                .cancelRequestId(cancelRequestId)
                .status("REJECTED")
                .decidedBy(TEST_USERNAME)
                .decidedAt("2024-01-23 13:00:00")
                .originalRecordPk("MONITORING#PROJECT#" + TEST_PROJECT_ID)
                .originalRecordSk("SITE#3#2024-01-21T10:00:00")
                .managerRemarks(null)
                .build();
        expectedResponse.setStatusCode("200");
        expectedResponse.setMessage("Cancel request rejected successfully");
        
        when(cancelRequestService.processDecision(cancelRequestId, request, TEST_USERNAME))
                .thenReturn(expectedResponse);
        
        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
                mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("200"))
                        .thenReturn(HttpStatus.OK);
                
                // When
                ResponseEntity<CancelRequestDecisionResponse> result = 
                monitoringController.makeDecision(authentication, cancelRequestId, request);
                
                // Then
                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(result.getBody()).isNotNull();
                assertThat(result.getBody().getStatusCode()).isEqualTo("200");
                assertThat(result.getBody().getStatus()).isEqualTo("REJECTED");
                assertThat(result.getBody().getManagerRemarks()).isNull();
                assertThat(result.getBody().getOffsetRecordPk()).isNull();
                assertThat(result.getBody().getOffsetRecordSk()).isNull();
                // 検証
                verify(cancelRequestService).processDecision(cancelRequestId, request, TEST_USERNAME);
        }
    }
        */
}
