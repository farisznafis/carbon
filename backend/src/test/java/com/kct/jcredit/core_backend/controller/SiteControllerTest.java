package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.request.CreateSiteRequest;
import com.kct.jcredit.core_backend.dto.request.UpdateSiteRequest;
import com.kct.jcredit.core_backend.dto.response.CreateSiteResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectListInfoResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentRegisterResponse;
import com.kct.jcredit.core_backend.dto.response.SiteInfoResponse;
import com.kct.jcredit.core_backend.dto.response.SiteListResponse;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.service.ProjectService;
import com.kct.jcredit.core_backend.service.SiteService;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import com.kct.jcredit.core_backend.util.JwtUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SiteControllerTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private SiteService siteService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SiteController siteController;

    // 1. 
        //  Unit tests for SiteController.getAllSitesByCompanyId()
        //  Test scope: Business logic only (security annotations are not tested)
        //  Dependencies: SiteService is mocked
        //  Static utility: ResponseUtil.getHttpStatus is mocked using MockedStatic

        /**
     * 正常系テスト1: companyId="COMP001"で正常にサイト一覧を取得
     * 
     * 期待結果:
     * - ステータスコード: 200 OK
     * - レスポンスボディ: mockResponseと一致
     * - siteService.getSitesByCompanyId()が1回呼ばれること
     * - ResponseUtil.getHttpStatus()が1回呼ばれること
     */
 
    @Test
    @DisplayName("UT-ST-101")
    void testGetAllSitesByCompanyId_Success() {
        // Arrange
        String companyId = "COMP001";
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup mock response
            SiteListResponse mockResponse = mock(SiteListResponse.class);
            when(mockResponse.getStatusCode()).thenReturn("200");
            
            // Setup service mock
            when(siteService.getSitesByCompanyId("COMP001")).thenReturn(mockResponse);
            
            // Setup static utility mock
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);
            
            // Act
            ResponseEntity<?> result = siteController.getAllSitesByCompanyId(companyId);
            
            // Assert
            assertNotNull(result);
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals(mockResponse, result.getBody());
            
            // Verify interactions
            verify(siteService, times(1)).getSitesByCompanyId("COMP001");
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("200"), times(1));
        }
    }

    /**
     * 異常系テスト2: companyId="COMP001"でサイトが見つからない (404)
     * 
     * 期待結果:
     * - ステータスコード: 404 NOT_FOUND
     * - レスポンスボディ: mockResponseと一致
     * - siteService.getSitesByCompanyId()が1回呼ばれること
     * - ResponseUtil.getHttpStatus()が1回呼ばれること
     */
    @Test
    @DisplayName("UT-ST-102")
    void testGetAllSitesByCompanyId_NotFound() {
        // Arrange
        String companyId = "COMP001";
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup mock response
            SiteListResponse mockResponse = mock(SiteListResponse.class);
            when(mockResponse.getStatusCode()).thenReturn("404");
            
            // Setup service mock
            when(siteService.getSitesByCompanyId("COMP001")).thenReturn(mockResponse);
            
            // Setup static utility mock
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("404")).thenReturn(HttpStatus.NOT_FOUND);
            
            // Act
            ResponseEntity<?> result = siteController.getAllSitesByCompanyId(companyId);
            
            // Assert
            assertNotNull(result);
            assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals(mockResponse, result.getBody());
            
            // Verify interactions
            verify(siteService, times(1)).getSitesByCompanyId("COMP001");
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("404"), times(1));
        }
    }

    /**
     * 異常系テスト3: CustomExceptionがスローされる
     * 
     * 期待結果:
     * - CustomExceptionがスローされること
     * - エラーコードとメッセージが正しいこと
     * - siteService.getSitesByCompanyId()が1回呼ばれること
     */
    @Test
    @DisplayName("UT-ST-103")
    void testGetAllSitesByCompanyId_CustomException() {
        // Arrange
        String companyId = "COMP001";
        CustomException expectedException = new CustomException("E001", "拠点一覧の取得に失敗しました");
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup service mock to throw exception
            when(siteService.getSitesByCompanyId("COMP001")).thenThrow(expectedException);
            
            // Act & Assert
            CustomException exception = assertThrows(CustomException.class, () -> {
                siteController.getAllSitesByCompanyId(companyId);
            });
            
            assertEquals("E001", exception.getErrorCode());
            assertEquals("拠点一覧の取得に失敗しました", exception.getMessage());
            
            // Verify interactions
            verify(siteService, times(1)).getSitesByCompanyId("COMP001");}
    }

    /**
     * 異常系テスト4: companyIdがnullの場合にIllegalArgumentExceptionがスローされる
     * 
     * 期待結果:
     * - IllegalArgumentExceptionがスローされること
     * - siteService.getSitesByCompanyId()が1回呼ばれること
     */
    @Test
    @DisplayName("UT-ST-104")
    void testGetAllSitesByCompanyId_NullCompanyId() {
        // Arrange
        String companyId = null;
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup service mock to throw exception
            when(siteService.getSitesByCompanyId(null)).thenThrow(new IllegalArgumentException("companyId must not be null"));
            
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                siteController.getAllSitesByCompanyId(companyId);
            });
            
            // Verify interactions
            verify(siteService, times(1)).getSitesByCompanyId(null);
        }
    }
        /**
     * 2. Unit tests for SiteController.getSite()
     *
     * Test scope: Business logic only (security annotations are not tested)
     * Dependencies: SiteService is mocked
     * Static utility: ResponseUtil.getHttpStatus is mocked using MockedStatic
     */

     /**
     * 正常系テスト1: siteId="SITE001"で正常に拠点情報を取得
     * 
     * 期待結果:
     * - ステータスコード: 200 OK
     * - レスポンスボディ: mockResponseと一致
     * - siteService.getSite()が1回呼ばれること
     * - ResponseUtil.getHttpStatus()が1回呼ばれること
     */
    @Test
    @DisplayName("UT-ST-201")
    void testGetSite_Success() {
        // Arrange
        String siteId = "SITEaB3kM9pQ7xL2vN8s";
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup mock response
            SiteInfoResponse mockResponse = mock(SiteInfoResponse.class);
            when(mockResponse.getStatusCode()).thenReturn("200");
            
            // Setup service mock
            when(siteService.getSiteByPublicId(siteId)).thenReturn(mockResponse);
            
            // Setup static utility mock
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);
            
            // Act
            ResponseEntity<SiteInfoResponse> result = siteController.getSite(siteId);
            
            // Assert
            assertNotNull(result);
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals(mockResponse, result.getBody());
            
            // Verify interactions
            verify(siteService, times(1)).getSiteByPublicId(siteId);
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("200"), times(1));
        }
    }

    /**
     * 異常系テスト2: siteId="SITE999"で拠点が見つからない (404)
     * 
     * 期待結果:
     * - ステータスコード: 404 NOT_FOUND
     * - レスポンスボディ: mockResponseと一致
     * - siteService.getSite()が1回呼ばれること
     * - ResponseUtil.getHttpStatus()が1回呼ばれること
     */
    @Test
    @DisplayName("UT-ST-202")
    void testGetSite_NotFound() {
        // Arrange
        String siteId = "SITEnonExistent99999";

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            SiteInfoResponse mockResponse = mock(SiteInfoResponse.class);
            when(mockResponse.getStatusCode()).thenReturn("404");

            when(siteService.getSiteByPublicId(siteId)).thenReturn(mockResponse);

            mockedUtil.when(() -> ResponseUtil.getHttpStatus("404"))
                    .thenReturn(HttpStatus.NOT_FOUND);

            // Act
            ResponseEntity<SiteInfoResponse> result = siteController.getSite(siteId);

            // Assert
            assertNotNull(result);
            assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals(mockResponse, result.getBody());

            // Verify
            verify(siteService, times(1)).getSiteByPublicId(siteId); 
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("404"), times(1));
        }
    }

    /**
     * 異常系テスト3: CustomExceptionがスローされる
     * 
     * 期待結果:
     * - CustomExceptionがスローされること
     * - エラーコードとメッセージが正しいこと
     * - siteService.getSite()が1回呼ばれること
     */
    @Test
    @DisplayName("UT-ST-203")
    void testGetSite_CustomException() {
        // Arrange
        String siteId = "SITEaB3kM9pQ7xL2vN8s";
        CustomException expectedException = new CustomException("E001", "拠点データの取得に失敗しました");
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup service mock to throw exception
            when(siteService.getSiteByPublicId(siteId)).thenThrow(expectedException);
            
            // Act & Assert
            CustomException exception = assertThrows(CustomException.class, () -> {
                siteController.getSite(siteId);
            });
            
            assertEquals("E001", exception.getErrorCode());
            assertEquals("拠点データの取得に失敗しました", exception.getMessage());
            
            // Verify interactions
            verify(siteService, times(1)).getSiteByPublicId(siteId);
        }
    }

    /**
     * 異常系テスト4: siteIdがnullの場合にIllegalArgumentExceptionがスローされる
     * 
     * 期待結果:
     * - IllegalArgumentExceptionがスローされること
     * - siteService.getSite()が1回呼ばれること
     */
    @Test
    @DisplayName("UT-ST-204")
    void testGetSite_NullSiteId() {
        // Arrange
        String siteId = null;

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup mock response — service handles null gracefully
            SiteInfoResponse mockResponse = mock(SiteInfoResponse.class);
            when(mockResponse.getStatusCode()).thenReturn("404");

            when(siteService.getSiteByPublicId(null)).thenReturn(mockResponse);

            mockedUtil.when(() -> ResponseUtil.getHttpStatus("404"))
                    .thenReturn(HttpStatus.NOT_FOUND);

            // Act
            ResponseEntity<SiteInfoResponse> result = siteController.getSite(siteId);

            // Assert
            assertNotNull(result);
            assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());

            // Verify
            verify(siteService, times(1)).getSiteByPublicId(null);
        }
    }

    /**
     * 3. Unit tests for SiteController.getProjectsBySiteId()
     *
     * Test scope: Business logic only (security annotations are not tested)
     * Dependencies: ProjectService and JwtUtil are mocked
     * Static utility: ResponseUtil.getHttpStatus is mocked using MockedStatic
     */

     /**
     * 正常系テスト1: siteId=1で正常にプロジェクト一覧を取得（プロジェクトあり）
     * 
     * 期待結果:
     * - ステータスコード: 200 OK
     * - レスポンスボディ: mockResponseと一致（空でないリスト）
     * - projectService.getProjectsBySiteIdAndCompanyId()が1回呼ばれること
     * - jwtUtil.extractCompanyId()が1回呼ばれること
     * - siteServiceは使用されないこと
     * - ResponseUtil.getHttpStatus()が1回呼ばれること
     */
    @Test
    @DisplayName("UT-ST-301")
    void testGetProjectsBySiteId_SuccessWithProjects() {
        // Arrange
        String siteId = "SITEaB3kM9pQ7xL2vN8s";
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock
            when(authentication.getCredentials()).thenReturn("dummy-token");
            
            // Setup JWT util mock
            when(jwtUtil.extractCompanyId("dummy-token")).thenReturn("COMP001");
            
            // Setup mock response with non-empty list
            ProjectListInfoResponse mockResponse = mock(ProjectListInfoResponse.class);
            when(mockResponse.getStatusCode()).thenReturn("200");
            
            // Setup service mock
            when(projectService.getProjectsBySiteIdAndCompanyId(siteId, "COMP001")).thenReturn(mockResponse);
            
            // Setup static utility mock
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);
            
            // Act
            ResponseEntity<ProjectListInfoResponse> result = siteController.getProjectsBySiteId(authentication, siteId);
            
            // Assert
            assertNotNull(result);
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals(mockResponse, result.getBody());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("dummy-token");
            verify(projectService, times(1)).getProjectsBySiteIdAndCompanyId(siteId, "COMP001");
            verifyNoInteractions(siteService);
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("200"), times(1));
        }
    }

    /**
     * 正常系テスト2: siteId=1でプロジェクト一覧が空（拠点は存在するがプロジェクト未登録）
     * 
     * 期待結果:
     * - ステータスコード: 200 OK
     * - レスポンスボディ: mockResponseと一致（空のリスト）
     * - projectService.getProjectsBySiteIdAndCompanyId()が1回呼ばれること
     * - jwtUtil.extractCompanyId()が1回呼ばれること
     * - siteServiceは使用されないこと
     * - ResponseUtil.getHttpStatus()が1回呼ばれること
     */
    @Test
    @DisplayName("UT-ST-302")
    void testGetProjectsBySiteId_SuccessWithEmptyList() {
        // Arrange
        String siteId = "SITEaB3kM9pQ7xL2vN8s";
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock
            when(authentication.getCredentials()).thenReturn("dummy-token");
            
            // Setup JWT util mock
            when(jwtUtil.extractCompanyId("dummy-token")).thenReturn("COMP001");
            
            // Setup mock response with empty list
            ProjectListInfoResponse mockResponse = mock(ProjectListInfoResponse.class);
            when(mockResponse.getStatusCode()).thenReturn("200");
            
            // Setup service mock
            when(projectService.getProjectsBySiteIdAndCompanyId(siteId, "COMP001")).thenReturn(mockResponse);
            
            // Setup static utility mock
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);
            
            // Act
            ResponseEntity<ProjectListInfoResponse> result = siteController.getProjectsBySiteId(authentication, siteId);
            
            // Assert
            assertNotNull(result);
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals(mockResponse, result.getBody());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("dummy-token");
            verify(projectService, times(1)).getProjectsBySiteIdAndCompanyId(siteId, "COMP001");
            verifyNoInteractions(siteService);
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("200"), times(1));
        }
    }

    /**
     * 異常系テスト3: siteId=999で拠点が見つからない (404)
     * 
     * 期待結果:
     * - ステータスコード: 404 NOT_FOUND
     * - レスポンスボディ: mockResponseと一致
     * - projectService.getProjectsBySiteIdAndCompanyId()が1回呼ばれること
     * - jwtUtil.extractCompanyId()が1回呼ばれること
     * - siteServiceは使用されないこと
     * - ResponseUtil.getHttpStatus()が1回呼ばれること
     */
    @Test
    @DisplayName("UT-ST-303")
    void testGetProjectsBySiteId_NotFound() {
        // Arrange
        String siteId = "SITEnonExistent99999";
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock
            when(authentication.getCredentials()).thenReturn("dummy-token");
            
            // Setup JWT util mock
            when(jwtUtil.extractCompanyId("dummy-token")).thenReturn("COMP001");
            
            // Setup mock response
            ProjectListInfoResponse mockResponse = mock(ProjectListInfoResponse.class);
            when(mockResponse.getStatusCode()).thenReturn("404");
            
            // Setup service mock
            when(projectService.getProjectsBySiteIdAndCompanyId(siteId, "COMP001")).thenReturn(mockResponse);
            
            // Setup static utility mock
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("404")).thenReturn(HttpStatus.NOT_FOUND);
            
            // Act
            ResponseEntity<ProjectListInfoResponse> result = siteController.getProjectsBySiteId(authentication, siteId);
            
            // Assert
            assertNotNull(result);
            assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals(mockResponse, result.getBody());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("dummy-token");
            verify(projectService, times(1)).getProjectsBySiteIdAndCompanyId(siteId, "COMP001");
            verifyNoInteractions(siteService);
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("404"), times(1));
        }
    }

    /**
     * 異常系テスト4: CustomExceptionがスローされる（プロジェクト一覧取得失敗）
     * 
     * 期待結果:
     * - CustomExceptionがスローされること
     * - エラーコードとメッセージが正しいこと
     * - projectService.getProjectsBySiteIdAndCompanyId()が1回呼ばれること
     * - jwtUtil.extractCompanyId()が1回呼ばれること
     * - siteServiceは使用されないこと
     */
    @Test
    @DisplayName("UT-ST-304")
    void testGetProjectsBySiteId_CustomException() {
        // Arrange
        String siteId = "SITEaB3kM9pQ7xL2vN8s";
        CustomException expectedException = new CustomException("E002", "プロジェクト一覧の取得に失敗しました");
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock
            when(authentication.getCredentials()).thenReturn("dummy-token");
            
            // Setup JWT util mock
            when(jwtUtil.extractCompanyId("dummy-token")).thenReturn("COMP001");
            
            // Setup service mock to throw exception
            when(projectService.getProjectsBySiteIdAndCompanyId(siteId, "COMP001")).thenThrow(expectedException);
            
            // Act & Assert
            CustomException exception = assertThrows(CustomException.class, () -> {
                siteController.getProjectsBySiteId(authentication, siteId);
            });
            
            assertEquals("E002", exception.getErrorCode());
            assertEquals("プロジェクト一覧の取得に失敗しました", exception.getMessage());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("dummy-token");
            verify(projectService, times(1)).getProjectsBySiteIdAndCompanyId(siteId, "COMP001");
            verifyNoInteractions(siteService);
        }
    }

    /**
     * 異常系テスト5: トークンが無効（空文字列）の場合にCustomExceptionがスローされる
     * 
     * 期待結果:
     * - CustomExceptionがスローされること
     * - エラーコードとメッセージが正しいこと
     * - jwtUtil.extractCompanyId()が1回呼ばれること
     * - projectServiceは呼ばれないこと
     * - siteServiceは使用されないこと
     */
    @Test
    @DisplayName("UT-ST-305")
    void testGetProjectsBySiteId_InvalidToken() {
        // Arrange
        String siteId = "SITEaB3kM9pQ7xL2vN8s";
        CustomException expectedException = new CustomException("E004", "トークンが無効です");
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock with empty token
            when(authentication.getCredentials()).thenReturn("");
            
            // Setup JWT util mock to throw exception
            when(jwtUtil.extractCompanyId("")).thenThrow(expectedException);
            
            // Act & Assert
            CustomException exception = assertThrows(CustomException.class, () -> {
                siteController.getProjectsBySiteId(authentication, siteId);
            });
            
            assertEquals("E004", exception.getErrorCode());
            assertEquals("トークンが無効です", exception.getMessage());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("");
            verifyNoInteractions(projectService);
            verifyNoInteractions(siteService);
        }
    }

    /**
     * 4. Unit tests for SiteController.createSite()
     *
     * Test scope: Business logic only (security annotations are not tested)
     * Dependencies: SiteService and JwtUtil are mocked
     * Static utility: ResponseUtil.getHttpStatus is mocked using MockedStatic
     * Service method: createSite(CreateSiteRequest request, String companyId)
     */
    /**
     * 正常系テスト1: 有効なリクエストで正常に拠点を作成
     * 
     * 期待結果:
     * - ステータスコード: 200 OK
     * - レスポンスボディ: mockResponseと一致
     * - siteService.createSite()が1回呼ばれること
     * - jwtUtil.extractCompanyId()が1回呼ばれること
     * - projectServiceは使用されないこと
     * - ResponseUtil.getHttpStatus()が1回呼ばれること
     */
    @Test
    @DisplayName("UT-ST-401")
    void testCreateSite_Success() {
        // Arrange
        CreateSiteRequest request = new CreateSiteRequest();
        request.setSiteName("New Site");
        // request.setAddress("Tokyo");
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock
            when(authentication.getCredentials()).thenReturn("dummy-token");
            
            // Setup JWT util mock
            when(jwtUtil.extractCompanyId("dummy-token")).thenReturn("COMP001");
            
            // Setup mock response
            CreateSiteResponse mockResponse = mock(CreateSiteResponse.class);
            when(mockResponse.getStatusCode()).thenReturn("200");
            
            // Setup service mock
            when(siteService.createSite(request, "COMP001")).thenReturn(mockResponse);
            
            // Setup static utility mock
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);
            
            // Act
            ResponseEntity<CreateSiteResponse> result = siteController.createSite(authentication, request);
            
            // Assert
            assertNotNull(result);
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals(mockResponse, result.getBody());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("dummy-token");
            verify(siteService, times(1)).createSite(request, "COMP001");
            verifyNoInteractions(projectService);
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("200"), times(1));
        }
    }

    /**
     * 異常系テスト2: 拠点名が重複している場合 (409 Conflict)
     * 
     * 期待結果:
     * - ステータスコード: 409 CONFLICT
     * - レスポンスボディ: mockResponseと一致
     * - siteService.createSite()が1回呼ばれること
     * - jwtUtil.extractCompanyId()が1回呼ばれること
     * - projectServiceは使用されないこと
     * - ResponseUtil.getHttpStatus()が1回呼ばれること
     */
    @Test
    @DisplayName("UT-ST-402")
    void testCreateSite_Conflict() {
        // Arrange
        CreateSiteRequest request = new CreateSiteRequest();
        request.setSiteName("Duplicate Site");
        // request.setAddress("Osaka");
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock
            when(authentication.getCredentials()).thenReturn("dummy-token");
            
            // Setup JWT util mock
            when(jwtUtil.extractCompanyId("dummy-token")).thenReturn("COMP001");
            
            // Setup mock response
            CreateSiteResponse mockResponse = mock(CreateSiteResponse.class);
            when(mockResponse.getStatusCode()).thenReturn("409");
            
            // Setup service mock
            when(siteService.createSite(request, "COMP001")).thenReturn(mockResponse);
            
            // Setup static utility mock
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("409")).thenReturn(HttpStatus.CONFLICT);
            
            // Act
            ResponseEntity<CreateSiteResponse> result = siteController.createSite(authentication, request);
            
            // Assert
            assertNotNull(result);
            assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals(mockResponse, result.getBody());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("dummy-token");
            verify(siteService, times(1)).createSite(request, "COMP001");
            verifyNoInteractions(projectService);
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("409"), times(1));
        }
    }

    /**
     * 異常系テスト3: CustomExceptionがスローされる（拠点作成失敗）
     * 
     * 期待結果:
     * - CustomExceptionがスローされること
     * - エラーコードとメッセージが正しいこと
     * - siteService.createSite()が1回呼ばれること
     * - jwtUtil.extractCompanyId()が1回呼ばれること
     * - projectServiceは使用されないこと
     */
    @Test
    @DisplayName("UT-ST-403")
    void testCreateSite_CustomException() {
        // Arrange
        CreateSiteRequest request = new CreateSiteRequest();
        request.setSiteName("Error Site");
        // request.setAddress("Nagoya");
        
        CustomException expectedException = new CustomException("E003", "拠点の作成に失敗しました");
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock
            when(authentication.getCredentials()).thenReturn("dummy-token");
            
            // Setup JWT util mock
            when(jwtUtil.extractCompanyId("dummy-token")).thenReturn("COMP001");
            
            // Setup service mock to throw exception
            when(siteService.createSite(request, "COMP001")).thenThrow(expectedException);
            
            // Act & Assert
            CustomException exception = assertThrows(CustomException.class, () -> {
                siteController.createSite(authentication, request);
            });
            
            assertEquals("E003", exception.getErrorCode());
            assertEquals("拠点の作成に失敗しました", exception.getMessage());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("dummy-token");
            verify(siteService, times(1)).createSite(request, "COMP001");
            verifyNoInteractions(projectService);
        }
    }

    /**
     * 異常系テスト4: トークンが無効（空文字列）の場合にCustomExceptionがスローされる
     * 
     * 期待結果:
     * - CustomExceptionがスローされること
     * - エラーコードとメッセージが正しいこと
     * - jwtUtil.extractCompanyId()が1回呼ばれること
     * - siteServiceは呼ばれないこと（トークン検証で失敗）
     * - projectServiceは使用されないこと
     */
    @Test
    @DisplayName("UT-ST-404")
    void testCreateSite_InvalidToken() {
        // Arrange
        CreateSiteRequest request = new CreateSiteRequest();
        request.setSiteName("Test Site");
        // request.setAddress("Fukuoka");
        
        CustomException expectedException = new CustomException("E004", "トークンが無効です");
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock with empty token
            when(authentication.getCredentials()).thenReturn("");
            
            // Setup JWT util mock to throw exception
            when(jwtUtil.extractCompanyId("")).thenThrow(expectedException);
            
            // Act & Assert
            CustomException exception = assertThrows(CustomException.class, () -> {
                siteController.createSite(authentication, request);
            });
            
            assertEquals("E004", exception.getErrorCode());
            assertEquals("トークンが無効です", exception.getMessage());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("");
            verifyNoInteractions(siteService);
            verifyNoInteractions(projectService);
        }
    }

    /**
     * 5. Unit tests for SiteController.updateSite()
     *
     * Test scope: Business logic only (security annotations are not tested)
     * Dependencies: SiteService and JwtUtil are mocked
     * Static utility: ResponseUtil.getHttpStatus is mocked using MockedStatic
     * Service method: updateSite(String siteId, String companyId, UpdateSiteRequest request)
     */
    /**
     * 正常系テスト1: 有効なリクエストで正常に拠点を更新
     * 
     * 期待結果:
     * - ステータスコード: 200 OK
     * - レスポンスボディ: mockResponseと一致
     * - siteService.updateSite()が1回呼ばれること
     * - jwtUtil.extractCompanyId()が1回呼ばれること
     * - projectServiceは使用されないこと
     * - ResponseUtil.getHttpStatus()が1回呼ばれること
     */
    @Test
    @DisplayName("UT-ST-501")
    void testUpdateSite_Success() {
        // Arrange
        String siteId = "SITE001";
        UpdateSiteRequest request = new UpdateSiteRequest();
        // request.setSiteName("Updated Site");
        // request.setAddress("Tokyo Tower");
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock
            when(authentication.getCredentials()).thenReturn("dummy-token");
            
            // Setup JWT util mock
            when(jwtUtil.extractCompanyId("dummy-token")).thenReturn("COMP001");
            
            // Setup mock response
            SiteInfoResponse mockResponse = mock(SiteInfoResponse.class);
            when(mockResponse.getStatusCode()).thenReturn("200");
            
            // Setup service mock
            when(siteService.updateSite("SITE001", "COMP001", request)).thenReturn(mockResponse);
            
            // Setup static utility mock
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);
            
            // Act
            ResponseEntity<SiteInfoResponse> result = siteController.updateSite(authentication, siteId, request);
            
            // Assert
            assertNotNull(result);
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals(mockResponse, result.getBody());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("dummy-token");
            verify(siteService, times(1)).updateSite("SITE001", "COMP001", request);
            verifyNoInteractions(projectService);
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("200"), times(1));
        }
    }

    /**
     * 異常系テスト2: 更新対象の拠点が存在しない (404)
     * 
     * 期待結果:
     * - ステータスコード: 404 NOT_FOUND
     * - レスポンスボディ: mockResponseと一致
     * - siteService.updateSite()が1回呼ばれること
     * - jwtUtil.extractCompanyId()が1回呼ばれること
     * - projectServiceは使用されないこと
     * - ResponseUtil.getHttpStatus()が1回呼ばれること
     */
    @Test
    @DisplayName("UT-ST-502")
    void testUpdateSite_NotFound() {
        // Arrange
        String siteId = "SITE999";
        UpdateSiteRequest request = new UpdateSiteRequest();
        // request.setSiteName("Non-existent Site");
        // request.setAddress("Unknown Location");
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock
            when(authentication.getCredentials()).thenReturn("dummy-token");
            
            // Setup JWT util mock
            when(jwtUtil.extractCompanyId("dummy-token")).thenReturn("COMP001");
            
            // Setup mock response
            SiteInfoResponse mockResponse = mock(SiteInfoResponse.class);
            when(mockResponse.getStatusCode()).thenReturn("404");
            
            // Setup service mock
            when(siteService.updateSite("SITE999", "COMP001", request)).thenReturn(mockResponse);
            
            // Setup static utility mock
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("404")).thenReturn(HttpStatus.NOT_FOUND);
            
            // Act
            ResponseEntity<SiteInfoResponse> result = siteController.updateSite(authentication, siteId, request);
            
            // Assert
            assertNotNull(result);
            assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals(mockResponse, result.getBody());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("dummy-token");
            verify(siteService, times(1)).updateSite("SITE999", "COMP001", request);
            verifyNoInteractions(projectService);
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("404"), times(1));
        }
    }

    /**
     * 異常系テスト3: CustomExceptionがスローされる（拠点情報の更新失敗）
     * 
     * 期待結果:
     * - CustomExceptionがスローされること
     * - エラーコードとメッセージが正しいこと
     * - siteService.updateSite()が1回呼ばれること
     * - jwtUtil.extractCompanyId()が1回呼ばれること
     * - projectServiceは使用されないこと
     */
    @Test
    @DisplayName("UT-ST-503")
    void testUpdateSite_CustomException() {
        // Arrange
        String siteId = "SITE001";
        UpdateSiteRequest request = new UpdateSiteRequest();
        // request.setSiteName("Error Site");
        // request.setAddress("Error Location");
        
        CustomException expectedException = new CustomException("E005", "拠点情報の更新に失敗しました");
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock
            when(authentication.getCredentials()).thenReturn("dummy-token");
            
            // Setup JWT util mock
            when(jwtUtil.extractCompanyId("dummy-token")).thenReturn("COMP001");
            
            // Setup service mock to throw exception
            when(siteService.updateSite("SITE001", "COMP001", request)).thenThrow(expectedException);
            
            // Act & Assert
            CustomException exception = assertThrows(CustomException.class, () -> {
                siteController.updateSite(authentication, siteId, request);
            });
            
            assertEquals("E005", exception.getErrorCode());
            assertEquals("拠点情報の更新に失敗しました", exception.getMessage());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("dummy-token");
            verify(siteService, times(1)).updateSite("SITE001", "COMP001", request);
            verifyNoInteractions(projectService);
        }
    }

    /**
     * 異常系テスト4: トークンが無効（空文字列）の場合にCustomExceptionがスローされる
     * 
     * 期待結果:
     * - CustomExceptionがスローされること
     * - エラーコードとメッセージが正しいこと
     * - jwtUtil.extractCompanyId()が1回呼ばれること
     * - siteServiceは呼ばれないこと（トークン検証で失敗）
     * - projectServiceは使用されないこと
     */
    @Test
    @DisplayName("UT-ST-504")
    void testUpdateSite_InvalidToken() {
        // Arrange
        String siteId = "SITE001";
        UpdateSiteRequest request = new UpdateSiteRequest();
        // request.setSiteName("Test Site");
        // request.setAddress("Test Location");
        
        CustomException expectedException = new CustomException("E004", "トークンが無効です");
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock with empty token
            when(authentication.getCredentials()).thenReturn("");
            
            // Setup JWT util mock to throw exception
            when(jwtUtil.extractCompanyId("")).thenThrow(expectedException);
            
            // Act & Assert
            CustomException exception = assertThrows(CustomException.class, () -> {
                siteController.updateSite(authentication, siteId, request);
            });
            
            assertEquals("E004", exception.getErrorCode());
            assertEquals("トークンが無効です", exception.getMessage());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("");
            verifyNoInteractions(siteService);
            verifyNoInteractions(projectService);
        }
    }

    /**
     * 異常系テスト5: siteIdがnullの場合にNullPointerExceptionがスローされる
     * 
     * 期待結果:
     * - NullPointerExceptionがスローされること
     * - siteService.updateSite()が1回呼ばれること（null値が渡される）
     * - jwtUtil.extractCompanyId()が1回呼ばれること
     * - projectServiceは使用されないこと
     *
     * 注意: siteIdはString型のため、parseIntは行わず、nullがそのままサービスに渡される
     */
    @Test
    @DisplayName("UT-ST-505")
    void testUpdateSite_NullSiteId() {
        // Arrange
        String siteId = null;
        UpdateSiteRequest request = new UpdateSiteRequest();
        // request.setSiteName("Null Site");
        // request.setAddress("Null Location");
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock
            when(authentication.getCredentials()).thenReturn("dummy-token");
            
            // Setup JWT util mock
            when(jwtUtil.extractCompanyId("dummy-token")).thenReturn("COMP001");
            
            // Setup service mock to throw NullPointerException
            when(siteService.updateSite(null, "COMP001", request))
                    .thenThrow(new NullPointerException("siteId must not be null"));
            
            // Act & Assert
            assertThrows(NullPointerException.class, () -> {
                siteController.updateSite(authentication, siteId, request);
            });
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("dummy-token");
            verify(siteService, times(1)).updateSite(null, "COMP001", request);
            verifyNoInteractions(projectService);
        }
    }
    /**
     * 6. Unit tests for SiteController.getSiteProjectEquipment()
     *
     * Test scope: Business logic only (security annotations are not tested)
     * Dependencies: SiteService and JwtUtil are mocked
     * Static utility: ResponseUtil.getHttpStatus is mocked using MockedStatic
     * Service method: getSiteProjectEquipment(String siteId, Integer projectId, String companyId, String role, String username)
     * Token extraction: Direct cast (String) authentication.getCredentials() - NOT .toString()
     */
    /**
     * 正常系テスト1: 有効なリクエストで正常に機器情報を取得（機器あり）
     * 
     * 期待結果:
     * - ステータスコード: 200 OK
     * - レスポンスボディ: mockResponseと一致（機器リスト含む）
     * - siteService.getSiteProjectEquipment()が1回呼ばれること
     * - jwtUtil.extractCompanyId()が1回呼ばれること
     * - jwtUtil.extractRole()が1回呼ばれること
     * - jwtUtil.extractUsername()が1回呼ばれること
     * - projectServiceは使用されないこと
     * - ResponseUtil.getHttpStatus()が1回呼ばれること
     */
    @Test
    @DisplayName("UT-ST-601")
    void testGetSiteProjectEquipment_SuccessWithEquipment() {
        // Arrange
        String siteId = "1";
        String projectId = "PROJ0001";
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock - direct cast, NOT .toString()
            when(authentication.getCredentials()).thenReturn("dummy-token");
            
            // Setup JWT util mocks - ALL three extractions needed
            when(jwtUtil.extractCompanyId("dummy-token")).thenReturn("COMP001");
            when(jwtUtil.extractRole("dummy-token")).thenReturn("user");
            when(jwtUtil.extractUsername("dummy-token")).thenReturn("testuser");
            
            EnR002EquipmentRegisterResponse mockResponse = mock(EnR002EquipmentRegisterResponse.class);
            // Setup mock responseEquipmentSolarRegisterResponse mockResponse = mock(EquipmentSolarRegisterResponse.class);
            when(mockResponse.getStatusCode()).thenReturn("200");
            
            // Setup service mock
            when(siteService.getSiteProjectEquipment("1", "PROJ0001", "COMP001", "user", "testuser"))
                    .thenReturn(mockResponse);
            
            // Setup static utility mock
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);
            
            // Act
            ResponseEntity<EnR002EquipmentRegisterResponse> result = 
                    siteController.getSiteProjectEquipment(authentication, siteId, projectId);
            
            // Assert
            assertNotNull(result);
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals(mockResponse, result.getBody());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("dummy-token");
            verify(jwtUtil, times(1)).extractRole("dummy-token");
            verify(jwtUtil, times(1)).extractUsername("dummy-token");
            verify(siteService, times(1)).getSiteProjectEquipment("1", "PROJ0001", "COMP001", "user", "testuser");
            verifyNoInteractions(projectService);
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("200"), times(1));
        }
    }

    /**
     * 正常系テスト2: 有効なリクエストで機器情報が空（機器未登録）
     * 
     * 期待結果:
     * - ステータスコード: 200 OK
     * - レスポンスボディ: mockResponseと一致（空の機器リスト）
     * - siteService.getSiteProjectEquipment()が1回呼ばれること
     * - jwtUtil.extractCompanyId()が1回呼ばれること
     * - jwtUtil.extractRole()が1回呼ばれること
     * - jwtUtil.extractUsername()が1回呼ばれること
     * - projectServiceは使用されないこと
     * - ResponseUtil.getHttpStatus()が1回呼ばれること
     */
    @Test
    @DisplayName("UT-ST-602")
    void testGetSiteProjectEquipment_SuccessWithEmptyEquipment() {
        // Arrange
        String siteId = "1";
        String projectId = "PROJ0001";
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock
            when(authentication.getCredentials()).thenReturn("dummy-token");
            
            // Setup JWT util mocks
            when(jwtUtil.extractCompanyId("dummy-token")).thenReturn("COMP001");
            when(jwtUtil.extractRole("dummy-token")).thenReturn("user");
            when(jwtUtil.extractUsername("dummy-token")).thenReturn("testuser");
            
            // Setup mock response with empty equipment
            EnR002EquipmentRegisterResponse mockResponse = mock(EnR002EquipmentRegisterResponse.class);
            when(mockResponse.getStatusCode()).thenReturn("200");
            
            // Setup service mock
            when(siteService.getSiteProjectEquipment("1", "PROJ0001", "COMP001", "user", "testuser"))
                    .thenReturn(mockResponse);
            
            // Setup static utility mock
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);
            
            // Act
            ResponseEntity<EnR002EquipmentRegisterResponse> result = 
                    siteController.getSiteProjectEquipment(authentication, siteId, projectId);
            
            // Assert
            assertNotNull(result);
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals(mockResponse, result.getBody());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("dummy-token");
            verify(jwtUtil, times(1)).extractRole("dummy-token");
            verify(jwtUtil, times(1)).extractUsername("dummy-token");
            verify(siteService, times(1)).getSiteProjectEquipment("1", "PROJ0001", "COMP001", "user", "testuser");
            verifyNoInteractions(projectService);
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("200"), times(1));
        }
    }

    /**
     * 異常系テスト3: 拠点が見つからない (404 - siteId not found)
     * 
     * 期待結果:
     * - ステータスコード: 404 NOT_FOUND
     * - レスポンスボディ: mockResponseと一致
     * - siteService.getSiteProjectEquipment()が1回呼ばれること
     * - jwtUtil抽出メソッドがそれぞれ1回呼ばれること
     * - projectServiceは使用されないこと
     * - ResponseUtil.getHttpStatus()が1回呼ばれること
     */
    @Test
    @DisplayName("UT-ST-603")
    void testGetSiteProjectEquipment_SiteNotFound() {
        // Arrange
        String siteId = "999";
        String projectId = "PROJ0001";
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock
            when(authentication.getCredentials()).thenReturn("dummy-token");
            
            // Setup JWT util mocks
            when(jwtUtil.extractCompanyId("dummy-token")).thenReturn("COMP001");
            when(jwtUtil.extractRole("dummy-token")).thenReturn("user");
            when(jwtUtil.extractUsername("dummy-token")).thenReturn("testuser");
            
            // Setup mock response
            EnR002EquipmentRegisterResponse mockResponse = mock(EnR002EquipmentRegisterResponse.class);
            when(mockResponse.getStatusCode()).thenReturn("404");
            
            // Setup service mock
            when(siteService.getSiteProjectEquipment("999", "PROJ0001", "COMP001", "user", "testuser"))
                    .thenReturn(mockResponse);
            
            // Setup static utility mock
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("404")).thenReturn(HttpStatus.NOT_FOUND);
            
            // Act
            ResponseEntity<EnR002EquipmentRegisterResponse> result = 
                    siteController.getSiteProjectEquipment(authentication, siteId, projectId);
            
            // Assert
            assertNotNull(result);
            assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals(mockResponse, result.getBody());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("dummy-token");
            verify(jwtUtil, times(1)).extractRole("dummy-token");
            verify(jwtUtil, times(1)).extractUsername("dummy-token");
            verify(siteService, times(1)).getSiteProjectEquipment("999", "PROJ0001", "COMP001", "user", "testuser");
            verifyNoInteractions(projectService);
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("404"), times(1));
        }
    }

    /**
     * 異常系テスト4: プロジェクトが見つからない (404 - projectId not found)
     * 
     * 期待結果:
     * - ステータスコード: 404 NOT_FOUND
     * - レスポンスボディ: mockResponseと一致
     * - siteService.getSiteProjectEquipment()が1回呼ばれること
     * - jwtUtil抽出メソッドがそれぞれ1回呼ばれること
     * - projectServiceは使用されないこと
     * - ResponseUtil.getHttpStatus()が1回呼ばれること
     */
    @Test
    @DisplayName("UT-ST-604")
    void testGetSiteProjectEquipment_ProjectNotFound() {
        // Arrange
        String siteId = "1";
        String projectId = "PROJ0999";
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock
            when(authentication.getCredentials()).thenReturn("dummy-token");
            
            // Setup JWT util mocks
            when(jwtUtil.extractCompanyId("dummy-token")).thenReturn("COMP001");
            when(jwtUtil.extractRole("dummy-token")).thenReturn("user");
            when(jwtUtil.extractUsername("dummy-token")).thenReturn("testuser");
            
            // Setup mock response
            EnR002EquipmentRegisterResponse mockResponse = mock(EnR002EquipmentRegisterResponse.class);
            when(mockResponse.getStatusCode()).thenReturn("404");
            
            // Setup service mock
            when(siteService.getSiteProjectEquipment("1", "PROJ0999", "COMP001", "user", "testuser"))
                    .thenReturn(mockResponse);
            
            // Setup static utility mock
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("404")).thenReturn(HttpStatus.NOT_FOUND);
            
            // Act
            ResponseEntity<EnR002EquipmentRegisterResponse> result = 
                    siteController.getSiteProjectEquipment(authentication, siteId, projectId);
            
            // Assert
            assertNotNull(result);
            assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals(mockResponse, result.getBody());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("dummy-token");
            verify(jwtUtil, times(1)).extractRole("dummy-token");
            verify(jwtUtil, times(1)).extractUsername("dummy-token");
            verify(siteService, times(1)).getSiteProjectEquipment("1", "PROJ0999", "COMP001", "user", "testuser");
            verifyNoInteractions(projectService);
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("404"), times(1));
        }
    }

    /**
     * 異常系テスト5: CustomExceptionがスローされる（機器情報の取得失敗）
     * 
     * 期待結果:
     * - CustomExceptionがスローされること
     * - エラーコードとメッセージが正しいこと
     * - siteService.getSiteProjectEquipment()が1回呼ばれること
     * - jwtUtil抽出メソッドがそれぞれ1回呼ばれること
     * - projectServiceは使用されないこと
     */
    @Test
    @DisplayName("UT-ST-605")
    void testGetSiteProjectEquipment_CustomException() {
        // Arrange
        String siteId = "1";
        String projectId = "PROJ0001";
        CustomException expectedException = new CustomException("E006", "機器情報の取得に失敗しました");
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock
            when(authentication.getCredentials()).thenReturn("dummy-token");
            
            // Setup JWT util mocks
            when(jwtUtil.extractCompanyId("dummy-token")).thenReturn("COMP001");
            when(jwtUtil.extractRole("dummy-token")).thenReturn("user");
            when(jwtUtil.extractUsername("dummy-token")).thenReturn("testuser");
            
            // Setup service mock to throw exception
            when(siteService.getSiteProjectEquipment("1", "PROJ0001", "COMP001", "user", "testuser"))
                    .thenThrow(expectedException);
            
            // Act & Assert
            CustomException exception = assertThrows(CustomException.class, () -> {
                siteController.getSiteProjectEquipment(authentication, siteId, projectId);
            });
            
            assertEquals("E006", exception.getErrorCode());
            assertEquals("機器情報の取得に失敗しました", exception.getMessage());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("dummy-token");
            verify(jwtUtil, times(1)).extractRole("dummy-token");
            verify(jwtUtil, times(1)).extractUsername("dummy-token");
            verify(siteService, times(1)).getSiteProjectEquipment("1", "PROJ0001", "COMP001", "user", "testuser");
            verifyNoInteractions(projectService);}
    }

    /**
     * 異常系テスト6: トークンが無効（空文字列）の場合にCustomExceptionがスローされる
     * 
     * 期待結果:
     * - CustomExceptionがスローされること
     * - エラーコードとメッセージが正しいこと
     * - jwtUtil.extractCompanyId()が1回呼ばれること
     * - siteServiceは呼ばれないこと（トークン検証で失敗）
     * - projectServiceは使用されないこと
     */
    @Test
    @DisplayName("UT-ST-606")
    void testGetSiteProjectEquipment_InvalidToken() {
        // Arrange
        String siteId = "1";
        String projectId = "PROJ0001";
        CustomException expectedException = new CustomException("E004", "トークンが無効です");
        
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            // Setup authentication mock with empty token
            when(authentication.getCredentials()).thenReturn("");
            
            // Setup JWT util mock to throw exception on first extraction
            when(jwtUtil.extractCompanyId("")).thenThrow(expectedException);
            
            // Act & Assert
            CustomException exception = assertThrows(CustomException.class, () -> {
                siteController.getSiteProjectEquipment(authentication, siteId, projectId);
            });
            
            assertEquals("E004", exception.getErrorCode());
            assertEquals("トークンが無効です", exception.getMessage());
            
            // Verify interactions
            verify(authentication, times(1)).getCredentials();
            verify(jwtUtil, times(1)).extractCompanyId("");
            // Role and Username extraction should NOT be called (early failure)
            verify(jwtUtil, never()).extractRole(anyString());
            verify(jwtUtil, never()).extractUsername(anyString());
            verifyNoInteractions(siteService);
            verifyNoInteractions(projectService);
        }
    }
}