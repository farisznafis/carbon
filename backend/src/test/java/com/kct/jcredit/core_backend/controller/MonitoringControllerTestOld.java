//USE -SHANAZ
package com.kct.jcredit.core_backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.CancelRequestDecisionRequest;
import com.kct.jcredit.core_backend.dto.request.CreateCancelRequestRequest;
import com.kct.jcredit.core_backend.dto.request.RegisterMonitoringRecordSolarRequest;
import com.kct.jcredit.core_backend.dto.response.CancelRequestDecisionResponse;
import com.kct.jcredit.core_backend.dto.response.CancelRequestItemDto;
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
@MockitoSettings(strictness = Strictness.LENIENT)
class MonitoringControllerTestOld {
    
    @Mock
    private MonitoringService monitoringService;

    @Mock
    private CancelRequestService cancelRequestService;

    @Mock
    private Authentication authentication;

    @Mock
    private RegisterMonitoringRecordSolarRequest request;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private MonitoringController monitoringController;

    private static final String TOKEN = "Bearer test-jwt-token";
    private static final String USERNAME = "testuser";
    private static final String COMPANY_ID = "company123";
    private static final String SITE_ID = "1";
    private static final String PROJECT_ID = "PROJ0100";

    @BeforeEach
    void setUp() {
        // JWT認証のモック設定
        lenient().when(jwtUtil.extractUsername(anyString())).thenReturn(USERNAME);
        lenient().when(jwtUtil.extractCompanyId(anyString())).thenReturn(COMPANY_ID);
        lenient().when(authentication.getCredentials()).thenReturn(TOKEN);

        request = new RegisterMonitoringRecordSolarRequest();
        // リクエストの必須フィールドを設定
        request.setProjectId("PROJ0001");
        request.setSiteId("100");
        request.setProjectType("SOLAR");
        request.setDateRecorded("2024-01-15"); // ← これが正しい
        request.setElPv(new BigDecimal("123.456"));
        request.setElPvr(new BigDecimal("789.012"));
        request.setElpjControl(new BigDecimal("50.000"));
    }

    @Test
    @DisplayName("有効なリクエストで登録成功〇")
    void testRegisterMonitoringRecordSolar_ValidRequest_Success() {

        // Given
        String companyId = "company123";
        
        RegisterMonitoringRecordSolarRequest request = new RegisterMonitoringRecordSolarRequest();
        request.setProjectId("PROJ0001");
        request.setSiteId("10");
        request.setUserId(100);
        request.setProjectType("SOLAR");
        request.setDateRecorded("2024-01-15T10:30:00");
        request.setElPv(new BigDecimal("150.500"));
        request.setElPvr(new BigDecimal("120.300"));
        request.setElpjControl(new BigDecimal("50.000"));
        request.setElPvEvidenceUrl("https://example.com/evidence/elpv_001.pdf");
        request.setElPvrEvidenceUrl("https://example.com/evidence/elpvr_001.pdf");
        request.setElpjBatterySpecificationUrl("https://example.com/evidence/battery_spec_001.pdf");
        request.setElpjControlEvidenceUrl("https://example.com/evidence/control_001.pdf");
        
        MonitoringDataSolarResponse dataResponse = MonitoringDataSolarResponse.builder()
                .projectId("1")
                .siteId("10")
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

        // 3. Service のメソッドをモック
        when(monitoringService.registerMonitoringRecordSolar(USERNAME, companyId, request))
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
            
            assertEquals("1", data.getProjectId(), "プロジェクトIDが一致しません");
            assertEquals("10", data.getSiteId(), "サイトIDが一致しません");
            assertEquals(100, data.getUserId(), "ユーザーIDが一致しません");
            assertEquals("SOLAR", data.getProjectType(), "プロジェクトタイプが一致しません");
            assertEquals("2024-01-15T10:30:00", data.getDateRecorded(), "記録日時が一致しません");
            assertEquals("2024-01-15T10:35:00", data.getTimeUpdated(), "更新日時が一致しません");
            assertEquals(new BigDecimal("150.500"), data.getElPv(), "ELpvが一致しません");
            assertEquals(new BigDecimal("120.300"), data.getElPvr(), "ELpvrが一致しません");
            assertEquals(false, data.getChangedBattery(), "バッテリー交換フラグが一致しません");
            assertEquals(new BigDecimal("50.000"), data.getElpjControl(), "ELPJ制御値が一致しません");
            
            // モックメソッドの呼び出し検証
            verify(jwtUtil, times(1)).extractUsername(TOKEN);
            verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
            verify(monitoringService, times(1)).registerMonitoringRecordSolar(USERNAME, companyId, request);

        }
    }
    
    @Test
    @DisplayName("バッテリー交換ありで登録成功〇")
    void testRegisterMonitoringRecordSolar_WithBatteryReplacement_Success() {
        // Given
        String companyId = "company123";
        
        RegisterMonitoringRecordSolarRequest request = new RegisterMonitoringRecordSolarRequest();
        request.setProjectId("PROJ0002");
        request.setSiteId("20");
        request.setUserId(200);
        request.setProjectType("SOLAR");
        request.setDateRecorded("2024-02-20T14:00:00");
        request.setElPv(new BigDecimal("180.750"));
        request.setElPvr(new BigDecimal("140.500"));
        request.setElpjBatteryValue(new BigDecimal("250.000"));
        request.setElpjControl(new BigDecimal("60.000"));
        request.setElPvEvidenceUrl("https://example.com/evidence/elpv_002.pdf");
        request.setElPvrEvidenceUrl("https://example.com/evidence/elpvr_002.pdf");
        request.setElpjBatterySpecificationUrl("https://example.com/evidence/battery_spec_002.pdf");
        request.setElpjControlEvidenceUrl("https://example.com/evidence/control_002.pdf");
        
        MonitoringDataSolarResponse dataResponse = MonitoringDataSolarResponse.builder()
                .projectId("2")
                .siteId("20")
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
        
        when(authentication.getCredentials()).thenReturn(TOKEN);
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(companyId);
        when(monitoringService.registerMonitoringRecordSolar(USERNAME, companyId, request))
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
        request.setProjectId("PROJ0003");
        request.setSiteId("30");
        request.setUserId(300);
        request.setProjectType("SOLAR");
        request.setDateRecorded("2024-03-10T09:15:00");
        request.setElPv(new BigDecimal("200.125"));
        request.setElPvr(new BigDecimal("160.875"));
        request.setElpjControl(new BigDecimal("70.500"));
        request.setElpjBatteryValue(new BigDecimal("300.000"));
        request.setElpjAccuracy(new BigDecimal("95.50"));
        request.setElPvEvidenceUrl("https://example.com/evidence/elpv_003.pdf");
        request.setElPvrEvidenceUrl("https://example.com/evidence/elpvr_003.pdf");
        request.setElpjBatterySpecificationUrl("https://example.com/evidence/battery_spec_003.pdf");
        request.setElpjControlEvidenceUrl("https://example.com/evidence/control_003.pdf");
        
        MonitoringDataSolarResponse dataResponse = MonitoringDataSolarResponse.builder()
                .projectId("3")
                .siteId("30")
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

        when(authentication.getCredentials()).thenReturn(TOKEN);
        
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(companyId);
        when(monitoringService.registerMonitoringRecordSolar(USERNAME, companyId, request))
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
    @DisplayName("JWTトークンなし → 500 Internal Server Error〇")
    void testRegisterMonitoringRecordSolar_401_Unauthorized() {

        when(authentication.getCredentials()).thenReturn(null);

        // Given
        RegisterMonitoringRecordSolarRequest request = new RegisterMonitoringRecordSolarRequest();
        request.setProjectId("PROJ0001");
        request.setSiteId("10");
        request.setUserId(100);
        request.setProjectType("SOLAR");
        request.setDateRecorded("2024-01-15T10:30:00");
        request.setElPv(new BigDecimal("150.500"));
        request.setElPvr(new BigDecimal("120.300"));
        request.setElpjControl(new BigDecimal("50.000"));
        request.setElPvEvidenceUrl("https://example.com/evidence/elpv_001.pdf");
        request.setElPvrEvidenceUrl("https://example.com/evidence/elpvr_001.pdf");
        request.setElpjBatterySpecificationUrl("https://example.com/evidence/battery_spec_001.pdf");
        request.setElpjControlEvidenceUrl("https://example.com/evidence/control_001.pdf");
        // When & Then
        // ✅ NullPointerExceptionまたは適切な例外が発生することを確認
        assertThrows(NullPointerException.class, () -> {
                monitoringController.RegisterMonitoringRecordSolar(authentication, request);
        });
}
    
 @Test
@DisplayName("@Valid により必須フィールド欠落 → 400 Bad Request〇")
void testMissingRequiredField_ShouldReturn400() {
    // Given
    
    // 必須フィールドが欠落したリクエスト
    RegisterMonitoringRecordSolarRequest invalidRequest = 
        new RegisterMonitoringRecordSolarRequest();
    
    // バリデーション違反をシミュレート
    when(monitoringService.registerMonitoringRecordSolar(USERNAME, COMPANY_ID, invalidRequest))
        .thenThrow(new IllegalArgumentException("Required field missing"));
    
    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () -> monitoringController.RegisterMonitoringRecordSolar(authentication, invalidRequest)
    );
    verify(jwtUtil, times(1)).extractUsername(TOKEN);
    verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
}
    
@Test
@DisplayName("サービス層から404レスポンス → 404 Not Found（ResponseUtilで適切に変換）〇")
void testServiceReturns404Response_ShouldReturn404() {
    // Given
    
    // 404レスポンスを返すようにモック
    MonitoringRecordSolarResponse errorResponse = new MonitoringRecordSolarResponse();
    errorResponse.setStatusCode(ResponseConstants.NOT_FOUND);
    errorResponse.setMessage("Site not found");
    //他の必要なフィールドもセット
    
    when(monitoringService.registerMonitoringRecordSolar(USERNAME, COMPANY_ID, request))
        .thenReturn(errorResponse);

    try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
        mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.NOT_FOUND))
                .thenReturn(HttpStatus.NOT_FOUND);
    
        // When
        ResponseEntity<MonitoringRecordSolarResponse> response = 
                monitoringController.RegisterMonitoringRecordSolar(authentication, request);
    
        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NOT_FOUND", response.getBody().getStatusCode());
        assertEquals("Site not found", response.getBody().getMessage());
    
        // ✅ モック呼び出し検証
        verify(authentication, times(2)).getCredentials();
        verify(jwtUtil, times(1)).extractUsername(TOKEN);
        verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
        verify(monitoringService, times(1)).registerMonitoringRecordSolar(USERNAME, COMPANY_ID, request);
    }
}

 @Test
@DisplayName("JWT から username/companyId 正常抽出 → サービスに正しく渡される〇")
void testExtractUsernameAndCompanyIdFromJwt_ShouldPassToService() {
    // Given

    MonitoringDataSolarResponse dataResponse = MonitoringDataSolarResponse.builder()
        .userId(10)
        .projectId("100")
        .siteId("200")
        .projectType("SOLAR")
        .build();

    MonitoringRecordSolarResponse expectedResponse = MonitoringRecordSolarResponse.builder()
        .data(dataResponse)
        .build();
    expectedResponse.setStatusCode(ResponseConstants.OK);
    expectedResponse.setMessage("Success");
    
    when(monitoringService.registerMonitoringRecordSolar(
        USERNAME, COMPANY_ID, request))
        .thenReturn(expectedResponse);
    
    // When
    ResponseEntity<MonitoringRecordSolarResponse> response = 
        monitoringController.RegisterMonitoringRecordSolar(authentication, request);
    
    // Then
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(ResponseConstants.OK, response.getBody().getStatusCode());
    assertEquals("Success", response.getBody().getMessage());
    // dataオブジェクトの検証
    assertNotNull(response.getBody().getData());
    assertEquals(10, response.getBody().getData().getUserId());
    assertEquals("100", response.getBody().getData().getProjectId());
    assertEquals("200", response.getBody().getData().getSiteId());
    assertEquals("SOLAR", response.getBody().getData().getProjectType());
    
    // サービスに正しいusername/companyId/requestが渡されたことを検証
    ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> companyIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<RegisterMonitoringRecordSolarRequest> requestCaptor = 
        ArgumentCaptor.forClass(RegisterMonitoringRecordSolarRequest.class);
    
    verify(monitoringService, times(1)).registerMonitoringRecordSolar(
        usernameCaptor.capture(),
        companyIdCaptor.capture(),
        requestCaptor.capture()
    );
    
    assertEquals(USERNAME, usernameCaptor.getValue());
    assertEquals(COMPANY_ID, companyIdCaptor.getValue());
    assertEquals(request, requestCaptor.getValue());
    
    // JwtUtilの呼び出しも検証
    verify(jwtUtil, times(1)).extractUsername(TOKEN);
    verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
}


  // ==================== ReadLastMonitoringRecordSolar ====================
    @Test
    @DisplayName("クエリパラメータで最新レコード取得成功〇")
    void testReadLastMonitoringRecordSolar_Success() {
        // Given

        MonitoringRecordSolarResponse expectedResponse = new MonitoringRecordSolarResponse();
        expectedResponse.setStatusCode("200");
        expectedResponse.setMessage("Success");
        //必要に応じて他のフィールドを設定

        when(monitoringService.readLastMonitoringRecordSolar(
                USERNAME,
                COMPANY_ID,
                PROJECT_ID,
                SITE_ID
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
            mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus("200"))
                    .thenReturn(HttpStatus.OK);

            // When
            ResponseEntity<MonitoringRecordSolarResponse> result = monitoringController.ReadLastMonitoringRecordSolar(
                    authentication,
                    PROJECT_ID,
                    SITE_ID
            );

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getStatusCode()).isEqualTo("200");
            assertThat(result.getBody().getMessage()).isEqualTo("Success");

            //検証
            verify(jwtUtil).extractUsername(TOKEN);
            verify(jwtUtil).extractCompanyId(TOKEN);
            verify(monitoringService).readLastMonitoringRecordSolar(
                    USERNAME,
                    COMPANY_ID,
                    PROJECT_ID,
                    SITE_ID
            );

        verify(authentication, times(2)).getCredentials();
        verify(jwtUtil, times(1)).extractUsername(TOKEN);
        verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
        }
    }

    @Test
    @DisplayName("JWT から username/companyId 正常抽出 → サービスに正しく渡される〇")
    void testJwtExtraction_Success() {

        // Given

        MonitoringDataSolarResponse dataResponse = MonitoringDataSolarResponse.builder()
                .userId(10)
                .projectId("100")
                .siteId("200")
                .projectType("SOLAR")
                .build();

        MonitoringRecordSolarResponse expectedResponse = MonitoringRecordSolarResponse.builder()
                .data(dataResponse)
                .build();
        expectedResponse.setStatusCode(ResponseConstants.OK);
        expectedResponse.setMessage("Success");
        
        // ✅ クラス定数を使用
        when(monitoringService.registerMonitoringRecordSolar(USERNAME, COMPANY_ID, request))
                .thenReturn(expectedResponse);
        
        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
                mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                        .thenReturn(HttpStatus.OK);
                
                // When
                ResponseEntity<MonitoringRecordSolarResponse> response = 
                monitoringController.RegisterMonitoringRecordSolar(authentication, request);
                
                // Then
                assertThat(response).isNotNull();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.OK);
                assertThat(response.getBody().getMessage()).isEqualTo("Success");
                
                // dataオブジェクトの検証
                assertThat(response.getBody().getData()).isNotNull();
                assertThat(response.getBody().getData().getUserId()).isEqualTo(10);
                assertThat(response.getBody().getData().getProjectId()).isEqualTo("100");
                assertThat(response.getBody().getData().getSiteId()).isEqualTo("200");
                assertThat(response.getBody().getData().getProjectType()).isEqualTo("SOLAR");
                
                // ✅ サービスに正しいusername/companyId/requestが渡されたことを検証
                ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<String> companyIdCaptor = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<RegisterMonitoringRecordSolarRequest> requestCaptor = 
                ArgumentCaptor.forClass(RegisterMonitoringRecordSolarRequest.class);
                
                verify(monitoringService, times(1)).registerMonitoringRecordSolar(
                usernameCaptor.capture(),
                companyIdCaptor.capture(),
                requestCaptor.capture()
                );
                
                assertThat(usernameCaptor.getValue()).isEqualTo(USERNAME);
                assertThat(companyIdCaptor.getValue()).isEqualTo(COMPANY_ID);
                assertThat(requestCaptor.getValue()).isEqualTo(request);
                
                // ✅ JwtUtilの呼び出しも検証
                verify(authentication, times(2)).getCredentials();
                verify(jwtUtil, times(1)).extractUsername(TOKEN);
                verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
        }
}
    @Test
    @DisplayName("サービス層から404例外 → 404 Not Found")
    void testReadLastMonitoringRecordSolar_ServiceReturns404() {
        // Arrange
        MonitoringRecordSolarResponse mockResponse = new MonitoringRecordSolarResponse();
        mockResponse.setStatusCode(ResponseConstants.NOT_FOUND);
        mockResponse.setMessage(ResponseConstants.MSG_NOT_FOUND);
        mockResponse.setTimestamp(LocalDateTime.now());

        when(monitoringService.readLastMonitoringRecordSolar(
            anyString(), anyString(), anyString(), anyString()
        )).thenReturn(mockResponse);

        // Act
        ResponseEntity<MonitoringRecordSolarResponse> response = 
            monitoringController.ReadLastMonitoringRecordSolar(
                authentication, PROJECT_ID, SITE_ID
            );

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ResponseConstants.NOT_FOUND, response.getBody().getStatusCode());

        verify(authentication, times(2)).getCredentials();
        verify(jwtUtil, times(1)).extractUsername(TOKEN);
        verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
    }

     // ==================== listMonitoringRecordsbySite ====================

    @Test
    @DisplayName("デフォルトページネーションで一覧取得成功")
    void testlistMonitoringRecordsbySite_DefaultPagination_Success() {
        // Given
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
        List<MonitoringDataSolarResponse> items = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
                MonitoringDataSolarResponse record = MonitoringDataSolarResponse.builder()
                        
                        .projectId("PROJ0100")
                        .siteId(SITE_ID)
                        .userId(100 + i)
                        .dateRecorded("2024-01-15T10:30:00")
                        .elPv(new BigDecimal("150.500"))
                        .timeUpdated("2024-01-15T10:30:00")
                        
                        .build();
                items.add(record);
        }
        expectedResponse.setItems(items);  // ✅ setItems

        when(monitoringService.readSiteMonitoringRecordsSolar(
                eq(USERNAME),
                eq(COMPANY_ID),
                eq(PROJECT_ID),
                eq(SITE_ID),
                eq(1), // デフォルトpage
                eq(50), // デフォルトlimit
                eq(null) // nextPageToken
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
                mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                        .thenReturn(HttpStatus.OK);

                // When
                ResponseEntity<MonitoringRecordSolarListResponse> result = 
                        monitoringController.listMonitoringRecordsbySite(
                                SITE_ID,
                                PROJECT_ID,
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
                verify(jwtUtil, times(1)).extractUsername(TOKEN);
                verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
                verify(monitoringService, times(1)).readSiteMonitoringRecordsSolar(
                        USERNAME,
                        COMPANY_ID,
                        PROJECT_ID,
                        SITE_ID,
                        1,
                        50,
                        null
                );
        }
        }
    @Test
    @DisplayName("カスタムページサイズで一覧取得成功〇")
    void testlistMonitoringRecordsbySite_CustomPageSize_Success() {
        // Given
        Integer customLimit = 20;

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
        List<MonitoringDataSolarResponse> items = new ArrayList<>();
        for (int i = 0; i < customLimit; i++) {
                MonitoringDataSolarResponse record = MonitoringDataSolarResponse.builder()
                        
                        .projectId("PROJ0100")
                        .siteId(SITE_ID)
                        .userId(100 + i)
                        .dateRecorded("2024-01-15T10:30:00")
                        .elPv(new BigDecimal("150.500"))
                        .timeUpdated("2024-01-15T10:30:00")
                        
                        .build();
                items.add(record);
        }
        expectedResponse.setItems(items);  // ✅ setItems

        when(monitoringService.readSiteMonitoringRecordsSolar(
                eq(USERNAME),
                eq(COMPANY_ID),
                eq(PROJECT_ID),
                eq(SITE_ID),
                eq(1),
                eq(customLimit),
                eq(null)
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
                mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                        .thenReturn(HttpStatus.OK);

                // When
                ResponseEntity<MonitoringRecordSolarListResponse> result = 
                        monitoringController.listMonitoringRecordsbySite(
                                SITE_ID,
                                PROJECT_ID,
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

                verify(jwtUtil, times(1)).extractUsername(TOKEN);
                verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
                verify(monitoringService, times(1)).readSiteMonitoringRecordsSolar(
                        USERNAME,
                        COMPANY_ID,
                        PROJECT_ID,
                        SITE_ID,
                        1,
                        customLimit,
                        null
                );
        }
    }

    @Test
    @DisplayName("nextPageTokenで次ページ取得成功〇")
    void testlistMonitoringRecordsbySite_WithNextPageToken_Success() {
        // Given
        String nextPageToken = "eyJwYWdlIjoyLCJsaW1pdCI6NTB9";
        Integer page = 2;

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
        List<MonitoringDataSolarResponse> items = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
                MonitoringDataSolarResponse record = MonitoringDataSolarResponse.builder()
                        
                        .projectId("PROJ0100")
                        .siteId(SITE_ID)
                        .userId(200 + i)
                        .dateRecorded("2024-01-16T10:30:00")
                        .elPv(new BigDecimal("160.500"))
                        .timeUpdated("2024-01-16T10:30:00")
                        
                        .build();
                items.add(record);
        }
        expectedResponse.setItems(items);  // ✅ setItems

        when(monitoringService.readSiteMonitoringRecordsSolar(
                eq(USERNAME),
                eq(COMPANY_ID),
                eq(PROJECT_ID),
                eq(SITE_ID),
                eq(page),
                eq(50),
                eq(nextPageToken)
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
                mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                        .thenReturn(HttpStatus.OK);

                // When
                ResponseEntity<MonitoringRecordSolarListResponse> result = 
                        monitoringController.listMonitoringRecordsbySite(SITE_ID,PROJECT_ID,
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

                verify(authentication, times(2)).getCredentials();
                verify(jwtUtil, times(1)).extractUsername(TOKEN);
                verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
                verify(monitoringService, times(1)).readSiteMonitoringRecordsSolar(
                        USERNAME,
                        COMPANY_ID,
                        PROJECT_ID,
                        SITE_ID,
                        page,
                        50,
                        nextPageToken
                );
        }
    }

    @Test
    @DisplayName("ページ3で一覧取得成功〇")
        void testlistMonitoringRecordsbySite_Page3_Success() {
        // Given
        Integer page = 3;

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
        List<MonitoringDataSolarResponse> items = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
                MonitoringDataSolarResponse record = MonitoringDataSolarResponse.builder()
                        
                        .projectId("PROJ0100")
                        .siteId(SITE_ID)
                        .userId(300 + i) // ページ3なので異なるユーザーID範囲
                        .dateRecorded("2024-01-17T10:30:00")
                        .elPv(new BigDecimal("170.500"))
                        .timeUpdated("2024-01-17T10:30:00")
                        
                        .build();
                items.add(record);
        }
        expectedResponse.setItems(items);  // ✅ setItems

        when(monitoringService.readSiteMonitoringRecordsSolar(
                eq(USERNAME),
                eq(COMPANY_ID),
                eq(PROJECT_ID),
                eq(SITE_ID),
                eq(page),
                eq(50),
                eq(null)
        )).thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
                mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                        .thenReturn(HttpStatus.OK);

                // When
                ResponseEntity<MonitoringRecordSolarListResponse> result = 
                        monitoringController.listMonitoringRecordsbySite(
                                SITE_ID,
                                PROJECT_ID,
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
                MonitoringDataSolarResponse firstRecord = result.getBody().getItems().get(0);
                assertEquals(String.valueOf(PROJECT_ID), firstRecord.getProjectId(), 
                        "プロジェクトIDが一致しません");
                assertEquals(SITE_ID, firstRecord.getSiteId(), 
                        "サイトIDが一致しません");
                assertEquals(300, firstRecord.getUserId(), 
                        "ユーザーIDが一致しません");

                // モック呼び出し検証
                verify(jwtUtil, times(1)).extractUsername(TOKEN);
                verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
                verify(monitoringService, times(1)).readSiteMonitoringRecordsSolar(
                        USERNAME,
                        COMPANY_ID,
                        PROJECT_ID,
                        SITE_ID,
                        page,
                        50,
                        null
                );
        }
    }

    @Test
    @DisplayName("デフォルト値（page=1, limit=50）が適用される → サービスに正しく渡される〇")
    void testlistMonitoringRecordsbySite_DefaultValues() {
        // Arrange
        MonitoringRecordSolarListResponse mockResponse = new MonitoringRecordSolarListResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);
        mockResponse.setMessage("取得成功");
        mockResponse.setTimestamp(LocalDateTime.now());

        when(monitoringService.readSiteMonitoringRecordsSolar(
            USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, 1, 50, null
        )).thenReturn(mockResponse);

        // Act
        ResponseEntity<MonitoringRecordSolarListResponse> response = 
            monitoringController.listMonitoringRecordsbySite(
                SITE_ID, PROJECT_ID, 1, 50, null, authentication
            );

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        verify(monitoringService, times(1)).readSiteMonitoringRecordsSolar(
            USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, 1, 50, null
        );
    }


    @Test
    @DisplayName("pageが負の数 → 400 Bad Request（バリデーション確認）〇")
    void testlistMonitoringRecordsbySite_NegativePage() {
        // Arrange
        MonitoringRecordSolarListResponse mockResponse = new MonitoringRecordSolarListResponse();
        mockResponse.setStatusCode(ResponseConstants.BAD_REQUEST);
        mockResponse.setMessage("ページ番号は正の数である必要があります");
        mockResponse.setTimestamp(LocalDateTime.now());

        when(monitoringService.readSiteMonitoringRecordsSolar(
            anyString(), anyString(), anyString(), anyString(), eq(-1), anyInt(), any()
        )).thenReturn(mockResponse);

        // Act
        ResponseEntity<MonitoringRecordSolarListResponse> response = 
            monitoringController.listMonitoringRecordsbySite(
                SITE_ID, PROJECT_ID, -1, 50, null, authentication
            );

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        verify(monitoringService, times(1)).readSiteMonitoringRecordsSolar(
            USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, -1, 50, null
        );
    }

    @Test
    @DisplayName("limitが0 → 400 Bad Request〇")
    void testlistMonitoringRecordsbySite_ZeroLimit() {

        // Arrange
        MonitoringRecordSolarListResponse mockResponse = new MonitoringRecordSolarListResponse();
        mockResponse.setStatusCode(ResponseConstants.BAD_REQUEST);
        mockResponse.setMessage("リミットは1以上である必要があります");
        mockResponse.setTimestamp(LocalDateTime.now());

        when(monitoringService.readSiteMonitoringRecordsSolar(
            anyString(), anyString(), anyString(), anyString(), anyInt(), eq(0), any()
        )).thenReturn(mockResponse);

        // Act
        ResponseEntity<MonitoringRecordSolarListResponse> response = 
            monitoringController.listMonitoringRecordsbySite(
                SITE_ID, PROJECT_ID, 1, 0, null, authentication
            );

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        verify(monitoringService, times(1)).readSiteMonitoringRecordsSolar(
            USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, 1, 0, null
        );

        verify(authentication, times(2)).getCredentials();
        verify(jwtUtil, times(1)).extractUsername(TOKEN);
        verify(jwtUtil, times(1)).extractCompanyId(TOKEN);

    }

    @Test
    @DisplayName("nextPageToken が正しく渡される → サービスに伝達")
    void testlistMonitoringRecordsbySite_WithNextPageToken() {

        // Arrange
        String nextPageToken = "eyJwYWdlIjoyfQ==";
        MonitoringRecordSolarListResponse mockResponse = new MonitoringRecordSolarListResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);
        mockResponse.setTimestamp(LocalDateTime.now());

        when(monitoringService.readSiteMonitoringRecordsSolar(
            USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, 2, 50, nextPageToken
        )).thenReturn(mockResponse);

        // Act
        ResponseEntity<MonitoringRecordSolarListResponse> response = 
            monitoringController.listMonitoringRecordsbySite(
                SITE_ID, PROJECT_ID, 2, 50, nextPageToken, authentication
            );

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        verify(monitoringService, times(1)).readSiteMonitoringRecordsSolar(
            USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, 2, 50, nextPageToken
        );

        verify(authentication, times(2)).getCredentials();
        verify(jwtUtil, times(1)).extractUsername(TOKEN);
        verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
    }

    // ==================== uploadElpvEvidence ====================

   @Test
    @DisplayName("ELpv証憑PDFアップロード成功(PDF形式のファイルアップロード)〇")
    void testUploadElpvEvidence_PDF_Success() {
        // Given
        MockMultipartFile pdfFile = new MockMultipartFile(
            "file", "evidence.pdf", "application/pdf", "PDF content".getBytes()
        );
        String description = "ELpv証憑PDF";

        FileInfoResponse fileInfo = FileInfoResponse.builder()
                .url("https://s3.amazonaws.com/bucket/elpv_evidence.pdf")
                .fileName("evidence.pdf")
                .fileSize(pdfFile.getSize())
                .build();
        
        FileUploadResponse expectedResponse = new FileUploadResponse();
        expectedResponse.setFile(fileInfo);
        expectedResponse.setStatusCode(ResponseConstants.OK);

        when(monitoringService.uploadElpvEvidence(
            USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, description, pdfFile))
                .thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                       .thenReturn(HttpStatus.OK);

            // When
            ResponseEntity<FileUploadResponse> response = 
                    monitoringController.uploadElpvEvidence(
                        authentication, PROJECT_ID, SITE_ID, description, pdfFile
                    );

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("evidence.pdf", response.getBody().getFile().getFileName());
            verify(monitoringService).uploadElpvEvidence(
                USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, description, pdfFile
            );
        }
    }

    @Test
    @DisplayName("ELpv証憑CSVアップロード成功(CSV形式のファイルアップロード)")
    void testUploadElpvEvidence_CSV_Success() {
        // Given
        MockMultipartFile csvFile = new MockMultipartFile(
            "file", "evidence.csv", "text/csv", "column1,column2\nvalue1,value2".getBytes()
        );
        String description = "ELpv証憑CSV";

        FileInfoResponse fileInfo = FileInfoResponse.builder()
                .url("https://s3.amazonaws.com/bucket/elpv_evidence.csv")
                .fileName("evidence.csv")
                .build();
        
        FileUploadResponse expectedResponse = new FileUploadResponse();
        expectedResponse.setFile(fileInfo);
        expectedResponse.setStatusCode(ResponseConstants.OK);

        when(monitoringService.uploadElpvEvidence(
            USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, description, csvFile))
                .thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                       .thenReturn(HttpStatus.OK);

            // When
            ResponseEntity<FileUploadResponse> response = 
                    monitoringController.uploadElpvEvidence(
                        authentication, PROJECT_ID, SITE_ID, description, csvFile
                    );

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("evidence.csv", response.getBody().getFile().getFileName());
        }
    }

    @Test
    @DisplayName("MultipartFile が正しくサービスに渡される → モックで確認〇")
    void testUploadElpvEvidence_FilePassedToService() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "image content".getBytes()
        );

        FileUploadResponse expectedResponse = new FileUploadResponse();
        expectedResponse.setStatusCode(ResponseConstants.OK);

        when(monitoringService.uploadElpvEvidence(
            USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, null, file))
                .thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                       .thenReturn(HttpStatus.OK);

            // When
            monitoringController.uploadElpvEvidence(
                authentication, PROJECT_ID, SITE_ID, null, file
            );

            // Then
            verify(monitoringService).uploadElpvEvidence(
                USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, null, file
            );
        }
    }

    @Test
    @DisplayName("descriptionがrequired=falseで省略可能 → サービス呼び出し成功〇")
    void testUploadElpvEvidence_DescriptionOptional() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", "PDF content".getBytes()
        );

        FileUploadResponse expectedResponse = new FileUploadResponse();
        expectedResponse.setStatusCode(ResponseConstants.OK);

        when(monitoringService.uploadElpvEvidence(
            USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, null, file))
                .thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                       .thenReturn(HttpStatus.OK);

            // When
            ResponseEntity<FileUploadResponse> response = 
                    monitoringController.uploadElpvEvidence(
                        authentication, PROJECT_ID, SITE_ID, null, file
                    );

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(monitoringService).uploadElpvEvidence(
                USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, null, file
            );
        }
    }

    @Test
    @DisplayName("サービス層からファイル形式エラー → 400 Bad Request〇")
    void testUploadElpvEvidence_InvalidFileFormat() {
        // Given
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file", "test.exe", "application/x-msdownload", "executable content".getBytes()
        );

        FileUploadResponse errorResponse = new FileUploadResponse();
        errorResponse.setStatusCode(ResponseConstants.BAD_REQUEST);
        errorResponse.setMessage("サポートされていないファイル形式です");

        when(monitoringService.uploadElpvEvidence(
            USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, null, invalidFile))
                .thenReturn(errorResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.BAD_REQUEST))
                       .thenReturn(HttpStatus.BAD_REQUEST);

            // When
            ResponseEntity<FileUploadResponse> response = 
                    monitoringController.uploadElpvEvidence(
                        authentication, PROJECT_ID, SITE_ID, null, invalidFile
                    );

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("サポートされていないファイル形式です", response.getBody().getMessage());
        }
    }


    // ==================== uploadElpvrEvidence ====================

    
    @Test
    @DisplayName("ELpv証憑PDFアップロード成功(PDF形式のファイルアップロード)〇")
    void testUploadElpvEvidence_Success_PDF() {
        // Given

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
                USERNAME,
                COMPANY_ID,
                PROJECT_ID,
                SITE_ID,
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
                        PROJECT_ID,
                        SITE_ID,
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
                verify(jwtUtil).extractUsername(TOKEN);
                verify(jwtUtil).extractCompanyId(TOKEN);
                verify(monitoringService).uploadElpvEvidence(
                        USERNAME,
                        COMPANY_ID,
                        PROJECT_ID,
                        SITE_ID,
                        description,
                        pdfFile
                );

                verify(authentication, times(2)).getCredentials();
                verify(jwtUtil, times(1)).extractUsername(TOKEN);
        }
    }

    @Test
    @DisplayName("ELpv証憑CSVアップロード成功(CSV形式のファイルアップロード)〇")
        void testUploadElpvEvidence_Success_CSV() {
        // Given

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
                USERNAME,
                COMPANY_ID,
                PROJECT_ID,
                SITE_ID,
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
                        PROJECT_ID,
                        SITE_ID,
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
                verify(jwtUtil, times(1)).extractUsername(TOKEN);
                verify(jwtUtil).extractUsername(TOKEN);
                verify(jwtUtil).extractCompanyId(TOKEN);
                verify(monitoringService).uploadElpvEvidence(
                        USERNAME,
                        COMPANY_ID,
                        PROJECT_ID,
                        SITE_ID,
                        description,
                        csvFile
                );
        }
    }

    @Test
    @DisplayName("ELpv証憑画像アップロード成功（description付き）〇")
    void testUploadElpvEvidence_Success_ImageWithDescription() {
        // Given

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
                USERNAME,
                COMPANY_ID,
                PROJECT_ID,
                SITE_ID,
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
                        PROJECT_ID,
                        SITE_ID,
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
                verify(jwtUtil).extractUsername(TOKEN);
                verify(jwtUtil).extractCompanyId(TOKEN);
                verify(monitoringService).uploadElpvEvidence(
                        USERNAME,
                        COMPANY_ID,
                        PROJECT_ID,
                        SITE_ID,
                        description,
                        imageFile
                );
        }
    }

    @Test
    @DisplayName("ELpv証憑画像アップロード成功（description無し）〇")
    void testUploadElpvEvidence_Success_ImageWithoutDescription() {
        // Given

        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "evidence.jpg",
                "image/jpeg",
                "JPEG image content".getBytes()
        );

        // FileInfoResponse を作成（description無し）
        FileInfoResponse fileInfo = FileInfoResponse.builder()
                .id("file-789")
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
                USERNAME,
                COMPANY_ID,
                PROJECT_ID,
                SITE_ID,
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
                        PROJECT_ID,
                        SITE_ID,
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
                        USERNAME,
                        COMPANY_ID,
                        PROJECT_ID,
                        SITE_ID,
                        null,
                        imageFile
                );
        }
    }

    @Test
    @DisplayName("画像ファイルが正しくサービスに渡される → モックで確認〇")
    void testUploadElpvrEvidence_ImageWithDescription_Success() {
        // Given
        MockMultipartFile imageFile = new MockMultipartFile(
            "file", "report.jpg", "image/jpeg", "image content".getBytes()
        );
        String description = "ELpvr証憑画像";

        FileInfoResponse fileInfo = FileInfoResponse.builder()
                .url("https://s3.amazonaws.com/bucket/elpvr_report.jpg")
                .fileName("report.jpg")
                .build();

        FileUploadResponse expectedResponse = new FileUploadResponse();
        expectedResponse.setFile(fileInfo);
        expectedResponse.setStatusCode(ResponseConstants.OK);

        when(monitoringService.uploadElpvrEvidence(
            USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, description, imageFile))
                .thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                       .thenReturn(HttpStatus.OK);

            // When
            ResponseEntity<FileUploadResponse> response = 
                    monitoringController.uploadElpvrEvidence(
                        authentication, PROJECT_ID, SITE_ID, description, imageFile
                    );

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("report.jpg", response.getBody().getFile().getFileName());
            verify(monitoringService).uploadElpvrEvidence(
                USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, description, imageFile
            );
        }
    }
// ==================== uploadElpjBatterySpecificationEvidence ====================

  @Test
  @DisplayName("バッテリー仕様書アップロード成功〇")
    void testUploadElpjBatterySpecificationEvidence_Success() {
    // Given

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
            USERNAME,
            COMPANY_ID,
            PROJECT_ID,
            SITE_ID,
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
                PROJECT_ID,
                SITE_ID,
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
        verify(jwtUtil).extractUsername(TOKEN);
        verify(jwtUtil).extractCompanyId(TOKEN);
        verify(monitoringService).uploadElpjBatterySpecificationEvidence(
                USERNAME,
                COMPANY_ID,
                PROJECT_ID,
                SITE_ID,
                description,
                specificationFile
            );
        }
    }

     @Test
    @DisplayName("PDFファイルが正しくサービスに渡される → モックで確認〇")
    void testUploadElpjControlEvidence_PDF_Success() {
        // Given
        MockMultipartFile pdfFile = new MockMultipartFile(
            "file", "control.pdf", "application/pdf", "Control data".getBytes()
        );

        FileUploadResponse expectedResponse = new FileUploadResponse();
        expectedResponse.setStatusCode(ResponseConstants.OK);

        when(monitoringService.uploadElpjControlEvidence(
            USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, null, pdfFile))
                .thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                       .thenReturn(HttpStatus.OK);

            // When
            ResponseEntity<FileUploadResponse> response = 
                    monitoringController.uploadElpjControlEvidence(
                        authentication, PROJECT_ID, SITE_ID, null, pdfFile
                    );

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(monitoringService).uploadElpjControlEvidence(
                USERNAME, COMPANY_ID, PROJECT_ID, SITE_ID, null, pdfFile
            );
        }
    }

// ==================== uploadElpjControlEvidence ====================

    @Test
    @DisplayName("制御装置証憑アップロード成功〇")
    void testUploadElpjControlEvidence_Success_PDF() {  // ← 名前を変更
        // Given

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
                .id("file-123")
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
                USERNAME,
                COMPANY_ID,
                PROJECT_ID,
                SITE_ID,
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
                        PROJECT_ID,
                        SITE_ID,
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
                verify(jwtUtil).extractUsername(TOKEN);
                verify(jwtUtil).extractCompanyId(TOKEN);
                verify(monitoringService).uploadElpjControlEvidence(
                        USERNAME,
                        COMPANY_ID,
                        PROJECT_ID,
                        SITE_ID,
                        description,
                        controlFile
                );

                verify(authentication, times(2)).getCredentials();
                verify(jwtUtil, times(1)).extractUsername(TOKEN);
                verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
        }
    }

        // ==================== createCancelRequest ====================


    @Test
    @DisplayName("取消申請作成成功（masterユーザー）〇")
    void testCreateCancelRequest_Success_MasterUser() {
        // Given

        CreateCancelRequestRequest request = CreateCancelRequestRequest.builder()
                .siteId(SITE_ID)
                .recordPK("MONITORING#PROJECT#" + PROJECT_ID)
                .recordSK("SITE#" + SITE_ID + "#2024-01-15")
                .reasonText("設備の不具合により取消を申請します")
                .build();

        CancelRequestResponse expectedResponse = CancelRequestResponse.builder()
                .cancelRequestId(100)
                .recordPk("MONITORING#PROJECT#" + PROJECT_ID)
                .recordSk("SITE#" + SITE_ID + "#2024-01-15")
                .status("PENDING")
                .requestedBy(USERNAME)
                .requestedAt("2024-01-15T12:00:00")
                .siteId(SITE_ID)
                .reason("設備の不具合により取消を申請します")
                .build();
        expectedResponse.setStatusCode("201");
        expectedResponse.setMessage("Cancel request created successfully");

        when(cancelRequestService.createCancelRequest(
                USERNAME,
                COMPANY_ID,
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
                assertThat(result.getBody().getRecordPk()).isEqualTo("MONITORING#PROJECT#" + PROJECT_ID);
                assertThat(result.getBody().getRecordSk()).isEqualTo("SITE#" + SITE_ID + "#2024-01-15");
                assertThat(result.getBody().getStatus()).isEqualTo("PENDING");
                assertThat(result.getBody().getRequestedBy()).isEqualTo(USERNAME);
                assertThat(result.getBody().getRequestedAt()).isEqualTo("2024-01-15T12:00:00");
                assertThat(result.getBody().getSiteId()).isEqualTo(SITE_ID);
                assertThat(result.getBody().getReason()).isEqualTo("設備の不具合により取消を申請します");

                // 検証
                verify(authentication, times(2)).getCredentials();
                verify(jwtUtil).extractUsername(TOKEN);
                verify(jwtUtil).extractCompanyId(TOKEN);
                verify(cancelRequestService).createCancelRequest(
                        USERNAME,
                        COMPANY_ID,
                        request
                );
        }
    }

    @Test
    @DisplayName("取消申請作成成功（userユーザー）〇")
    void testCreateCancelRequest_Success_UserUser() {
        // Given
        CreateCancelRequestRequest request = CreateCancelRequestRequest.builder()
                .siteId(SITE_ID)
                .recordPK("MONITORING#PROJECT#" + PROJECT_ID)
                .recordSK("SITE#" + SITE_ID + "#2024-01-20")
                .reasonText("データ入力誤りのため取消を申請します")
                .build();

        CancelRequestResponse expectedResponse = CancelRequestResponse.builder()
                .cancelRequestId(101)
                .recordPk("MONITORING#PROJECT#" + PROJECT_ID)
                .recordSk("SITE#" + SITE_ID + "#2024-01-20")
                .status("PENDING")
                .requestedBy("user001")
                .requestedAt("2024-01-20T12:05:00")
                .siteId(SITE_ID)
                .reason("データ入力誤りのため取消を申請します")
                .build();
        expectedResponse.setStatusCode("201");
        expectedResponse.setMessage("Cancel request created successfully");

        when(cancelRequestService.createCancelRequest(
                USERNAME,
                COMPANY_ID,
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
                assertThat(result.getBody().getRecordPk()).isEqualTo("MONITORING#PROJECT#" + PROJECT_ID);
                assertThat(result.getBody().getRecordSk()).isEqualTo("SITE#" + SITE_ID + "#2024-01-20");
                assertThat(result.getBody().getStatus()).isEqualTo("PENDING");
                assertThat(result.getBody().getRequestedBy()).isEqualTo("user001");
                assertThat(result.getBody().getRequestedAt()).isEqualTo("2024-01-20T12:05:00");
                assertThat(result.getBody().getSiteId()).isEqualTo(SITE_ID);
                assertThat(result.getBody().getReason()).isEqualTo("データ入力誤りのため取消を申請します");

                // 検証
                verify(authentication, times(2)).getCredentials();
                verify(jwtUtil).extractUsername(TOKEN);
                verify(jwtUtil).extractCompanyId(TOKEN);
                verify(cancelRequestService).createCancelRequest(
                        USERNAME,
                        COMPANY_ID,
                        request
                );
        }
    }

    @Test
    @DisplayName("JWT から username/companyId 正常抽出 → サービスに正しく渡される〇")
    void testCreateCancelRequest_Success() {

        // Given
        CreateCancelRequestRequest request = CreateCancelRequestRequest.builder()
            .siteId(SITE_ID)
            .recordPK("COMP00000001#SITE#21")
            .recordSK("RECORD#2024-01-01T10:00:00")
            .reasonText("誤入力のため取消")
            .build();

        CancelRequestResponse expectedResponse = CancelRequestResponse.builder()
            .cancelRequestId(1)
            .status("PENDING")
            .requestedBy(USERNAME)
            .build();
        expectedResponse.setStatusCode(ResponseConstants.OK);

        when(cancelRequestService.createCancelRequest(USERNAME, COMPANY_ID, request))
                .thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                       .thenReturn(HttpStatus.OK);

            // When
            ResponseEntity<CancelRequestResponse> response = 
                    monitoringController.createCancelRequest(authentication, request);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("PENDING", response.getBody().getStatus());
            
            verify(jwtUtil).extractUsername(TOKEN);
            verify(jwtUtil).extractCompanyId(TOKEN);
            verify(cancelRequestService).createCancelRequest(USERNAME, COMPANY_ID, request);

            verify(authentication, times(2)).getCredentials();
            verify(jwtUtil, times(1)).extractUsername(TOKEN);
            verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
        }
    }

    @Test
    @DisplayName("@Valid により recordId が null → 400 Bad Request〇")
    void testCreateCancelRequest_RecordIdNull() {

        // Given
        CreateCancelRequestRequest request = CreateCancelRequestRequest.builder()
            .siteId(SITE_ID)
            .recordPK(null)
            .recordSK("RECORD#2024-01-01T10:00:00")
            .reasonText("理由")
            .build();

        CancelRequestResponse errorResponse = new CancelRequestResponse();
        errorResponse.setStatusCode(ResponseConstants.BAD_REQUEST);
        errorResponse.setMessage("recordPK is required");

        when(cancelRequestService.createCancelRequest(USERNAME, COMPANY_ID, request))
                .thenReturn(errorResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.BAD_REQUEST))
                       .thenReturn(HttpStatus.BAD_REQUEST);

            // When
            ResponseEntity<CancelRequestResponse> response = 
                    monitoringController.createCancelRequest(authentication, request);

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

            verify(authentication, times(2)).getCredentials();
            verify(jwtUtil, times(1)).extractUsername(TOKEN);
            verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
        }
    }

    @Test
    @DisplayName("@Valid により reason が空文字 → 400 Bad Request〇")
    void testCreateCancelRequest_ReasonEmpty() {

        // Given
        CreateCancelRequestRequest request = CreateCancelRequestRequest.builder()
            .siteId(SITE_ID)
            .recordPK("COMP00000001#SITE#21")
            .recordSK("RECORD#2024-01-01T10:00:00")
            .reasonText("")
            .build();

        CancelRequestResponse errorResponse = new CancelRequestResponse();
        errorResponse.setStatusCode(ResponseConstants.BAD_REQUEST);
        errorResponse.setMessage("reasonText is required");

        when(cancelRequestService.createCancelRequest(USERNAME, COMPANY_ID, request))
                .thenReturn(errorResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.BAD_REQUEST))
                       .thenReturn(HttpStatus.BAD_REQUEST);

            // When
            ResponseEntity<CancelRequestResponse> response = 
                    monitoringController.createCancelRequest(authentication, request);

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

            verify(authentication, times(2)).getCredentials();
            verify(jwtUtil, times(1)).extractUsername(TOKEN);
            verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
        }
    }

    // ==================== getCancelRequests ====================

    @Test
    @DisplayName("取消申請一覧取得成功")
    void testGetCancelRequests_Success() {
        // Given

        CancelRequestItemDto request1 = CancelRequestItemDto.builder()
                .id(100)
                .recordPk("MONITORING#PROJECT#" + PROJECT_ID)
                .status("PENDING")
                .requestedBy(USERNAME)
                .requestedAt("2024-01-15T12:00:00")
                .siteName("テスト拠点1")
                .reason("設備の不具合により取消を申請します")
                .build();

        CancelRequestItemDto request2 = CancelRequestItemDto.builder()
                .id(100)
                .recordPk("MONITORING#PROJECT#" + PROJECT_ID)
                .status("PENDING")
                .requestedBy("user001")
                .requestedAt("2024-01-20T14:30:00")
                .siteName("テスト拠点2")
                .reason("データ入力誤りのため取消を申請します")
                .build();

        CancelRequestItemDto request3 = CancelRequestItemDto.builder()
                .id(102)
                .recordPk("MONITORING#PROJECT#2")
                .status("APPROVED")
                .requestedBy("user002")
                .requestedAt("2024-01-22T09:15:00")
                .siteName("テスト拠点3")
                .reason("計測機器の故障のため取消を申請します")
                .build();


        CancelRequestListResponse expectedResponse = new CancelRequestListResponse();
        expectedResponse.setStatusCode("200");
        expectedResponse.setMessage("Cancel requests retrieved successfully");
        //実際のフィールド名に応じて以下のいずれかを使用
        expectedResponse.setItems(Arrays.asList(request1, request2, request3));

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
                assertThat(result.getBody().getItems()).hasSize(3);
                assertThat(result.getBody().getItems().get(0).getId()).isEqualTo(100);

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

    // ==================== makeDecision ====================

    @Test
    @DisplayName("取消申請承認成功〇")
    void testMakeDecision_Approve_Success() {
        // Given
        Integer cancelRequestId = 100;
        CancelRequestDecisionRequest request = CancelRequestDecisionRequest.builder()
                .decision("APPROVE")
                .managerRemarks("承認します")
                .build();
        
        when(authentication.getCredentials()).thenReturn(TOKEN);
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
        
        CancelRequestDecisionResponse expectedResponse = CancelRequestDecisionResponse.builder()
                .cancelRequestId(cancelRequestId)
                .status("APPROVED")
                .decidedBy(USERNAME)
                .decidedAt("2024-01-23 10:30:00")
                .originalRecordPk("MONITORING#PROJECT#" + PROJECT_ID)
                .originalRecordSk("SITE#1#2024-01-15T09:00:00")
                .offsetRecordPk("MONITORING#PROJECT#" + PROJECT_ID)
                .offsetRecordSk("SITE#1#2024-01-23T10:30:00#OFFSET")
                .managerRemarks("承認します")
                .build();
        expectedResponse.setStatusCode("200");
        expectedResponse.setMessage("Cancel request approved successfully");
        
        when(cancelRequestService.processDecision(cancelRequestId, request, USERNAME))
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
                assertThat(result.getBody().getDecidedBy()).isEqualTo(USERNAME);
                assertThat(result.getBody().getDecidedAt()).isEqualTo("2024-01-23 10:30:00");
                assertThat(result.getBody().getOriginalRecordPk()).isEqualTo("MONITORING#PROJECT#" + PROJECT_ID);
                assertThat(result.getBody().getOriginalRecordSk()).isEqualTo("SITE#1#2024-01-15T09:00:00");
                assertThat(result.getBody().getOffsetRecordPk()).isEqualTo("MONITORING#PROJECT#" + PROJECT_ID);
                assertThat(result.getBody().getOffsetRecordSk()).isEqualTo("SITE#1#2024-01-23T10:30:00#OFFSET");
                assertThat(result.getBody().getManagerRemarks()).isEqualTo("承認します");
                
                // 検証
                verify(jwtUtil).extractUsername(TOKEN);
                verify(cancelRequestService).processDecision(cancelRequestId, request, USERNAME);
        }
    }

    @Test
    @DisplayName("取消申請却下成功〇")
    void testMakeDecision_Reject_Success() {
        // Given
        Integer cancelRequestId = 101;
        
        CancelRequestDecisionRequest request = CancelRequestDecisionRequest.builder()
                .decision("REJECT")
                .managerRemarks("理由が不十分なため却下します")
                .build();
        
        when(authentication.getCredentials()).thenReturn(TOKEN);
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
        
        CancelRequestDecisionResponse expectedResponse = CancelRequestDecisionResponse.builder()
                .cancelRequestId(cancelRequestId)
                .status("REJECTED")
                .decidedBy(USERNAME)
                .decidedAt("2024-01-23 11:00:00")
                .originalRecordPk("MONITORING#PROJECT#" + PROJECT_ID)
                .originalRecordSk("SITE#2#2024-01-20T14:30:00")
                .offsetRecordPk(null)  // 却下時は相殺レコード無し
                .offsetRecordSk(null)  // 却下時は相殺レコード無し
                .managerRemarks("理由が不十分なため却下します")
                .build();
        expectedResponse.setStatusCode("200");
        expectedResponse.setMessage("Cancel request rejected successfully");
        
        when(cancelRequestService.processDecision(cancelRequestId, request, USERNAME))
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
                assertThat(result.getBody().getDecidedBy()).isEqualTo(USERNAME);
                assertThat(result.getBody().getDecidedAt()).isEqualTo("2024-01-23 11:00:00");
                assertThat(result.getBody().getOriginalRecordPk()).isEqualTo("MONITORING#PROJECT#" + PROJECT_ID);
                assertThat(result.getBody().getOriginalRecordSk()).isEqualTo("SITE#2#2024-01-20T14:30:00");
                assertThat(result.getBody().getOffsetRecordPk()).isNull();
                assertThat(result.getBody().getOffsetRecordSk()).isNull();
                assertThat(result.getBody().getManagerRemarks()).isEqualTo("理由が不十分なため却下します");
                
                // 検証
                verify(jwtUtil).extractUsername(TOKEN);
                verify(cancelRequestService).processDecision(cancelRequestId, request, USERNAME);
        }
    }
}