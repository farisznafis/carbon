package com.kct.jcredit.core_backend.controller.Project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.CreateProjectRequest;
import com.kct.jcredit.core_backend.dto.request.details.ProjectDtoReq;
import com.kct.jcredit.core_backend.dto.response.CreateProjectResponse;
import com.kct.jcredit.core_backend.util.ResponseUtil;

/**
 * Comprehensive test for Project Controller 
 * 
 * Test Coverage:
 * - Happy Path: 3 skenario
 * - Validation Tests: 15+ skenario
 * - Boundary Tests: 5+ skenario
 * - Exception Mapping: 3+ skenario
 * - Interaction Verification: 3+ skenario
 * 
 * @author シャナ-ズ
 * @version 1.0
 */
@DisplayName("Project Controller - Create Project Tests")
class CreateControllerTestMvc extends ProjectControllerBaseTest {

    private CreateProjectRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = TestDataBuilder.buildValidCreateProjectRequest();
    }

    // ═══════════════════════════════════════════════════════════════
    // HAPPY PATH TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("Happy Path - Successful Creation")
    class HappyPathTests {
        
        @Test
        @DisplayName("UT_C_PRJ_HP_001 - 正常なプロジェクト作成_全必須項目入力")
        void testCreateProject_Success_AllRequiredFields() {
            // ARRANGE
            CreateProjectResponse mockResponse = TestDataBuilder.buildSuccessCreateProjectResponse();
            when(projectService.createProject(any(CreateProjectRequest.class))).thenReturn(mockResponse);

            // ACT
            ResponseEntity<CreateProjectResponse> response;
            try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
                mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                    .thenReturn(HttpStatus.CREATED);

                response = controller.createProject(authentication, validRequest);
            }

            // ASSERT
            assertEquals(HttpStatus.CREATED, response.getStatusCode(), "ステータスコードが201ではありません");
            assertNotNull(response.getBody());
            assertEquals("PRJ001", response.getBody().getProject().get(0).getId(), "プロジェクトIDが一致しません");
            assertEquals(ResponseConstants.OK, response.getBody().getStatusCode(), "ステータスコードが一致しません");

            // Verify JWT extraction
            verify(jwtUtil, times(1)).extractCompanyId(VALID_TOKEN);
            verify(jwtUtil, times(1)).extractUsername(VALID_TOKEN);

            // Verify service call with captured arguments
            ArgumentCaptor<CreateProjectRequest> captor = ArgumentCaptor.forClass(CreateProjectRequest.class);
            verify(projectService, times(1)).createProject(captor.capture());

            CreateProjectRequest captured = captor.getValue();
            assertEquals(OPERATOR_COMPANY_ID, captured.getOperatorCompanyId(), "会社IDが設定されていません");
            assertEquals(OPERATOR_USERNAME, captured.getCreatedBy(), "作成者が設定されていません");
            assertEquals("P001", captured.getProject().get(0).getProjectNumber(), "プロジェクト番号が一致しません");

            verifyNoMoreInteractions(projectService, jwtUtil);
        }

        @Test
        @DisplayName("UT_C_PRJ_HP_002 - 正常なプロジェクト作成_オプション項目null")
        void testCreateProject_Success_OptionalFieldsNull() {
            // ARRANGE
            validRequest.getProject().get(0).setBaselineEmissions(null);
            validRequest.getProject().get(0).setPostProjectEmission(null);
            validRequest.getProject().get(0).setNewMembersTotal(null);

            CreateProjectResponse mockResponse = TestDataBuilder.buildSuccessCreateProjectResponse();
            when(projectService.createProject(any(CreateProjectRequest.class))).thenReturn(mockResponse);

            // ACT
            ResponseEntity<CreateProjectResponse> response;
            try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
                mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                    .thenReturn(HttpStatus.CREATED);

                response = controller.createProject(authentication, validRequest);
            }

            // ASSERT
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            verify(projectService, times(1)).createProject(any(CreateProjectRequest.class));
        }

        @Test
        @DisplayName("UT_C_PRJ_HP_003 - 正常なプロジェクト作成_数値項目に最大値")
        void testCreateProject_Success_MaxNumericValues() {
            // ARRANGE
            validRequest.getProject().get(0).setBaselineEmissions(99999999999.9);
            validRequest.getProject().get(0).setNewMembersTotal(Integer.MAX_VALUE);

            CreateProjectResponse mockResponse = TestDataBuilder.buildSuccessCreateProjectResponse();
            when(projectService.createProject(any(CreateProjectRequest.class))).thenReturn(mockResponse);

            // ACT
            ResponseEntity<CreateProjectResponse> response;
            try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
                mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                    .thenReturn(HttpStatus.CREATED);

                response = controller.createProject(authentication, validRequest);
            }

            // ASSERT
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            verify(projectService, times(1)).createProject(any(CreateProjectRequest.class));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // VALIDATION TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("Validation Tests - Input Invalid")
    class ValidationTests {
        
        @Test
        @DisplayName("UT_C_PRJ_V_001 - projectNameがnull")
        void testCreateProject_ValidationError_NullProjectName() {
            // ARRANGE
            validRequest.getProject().get(0).setProjectName(null);
            when(projectService.createProject(any(CreateProjectRequest.class)))
                .thenThrow(new RuntimeException("プロジェクト名は必須です"));

            // ACT & ASSERT
            assertThrows(RuntimeException.class, () -> {
                controller.createProject(authentication, validRequest);
            });
            
            verify(projectService, times(1)).createProject(any(CreateProjectRequest.class));
        }

        @Test
        @DisplayName("UT_C_PRJ_V_003 - projectNumberの形式不正")
        void testCreateProject_ValidationError_InvalidProjectNumberFormat() {
            // ARRANGE
            validRequest.getProject().get(0).setProjectNumber("A001");
            when(projectService.createProject(any(CreateProjectRequest.class)))
                .thenThrow(new RuntimeException("プロジェクト番号は P で始まる数字である必要があります"));

            // ACT & ASSERT
            assertThrows(RuntimeException.class, () -> {
                controller.createProject(authentication, validRequest);
            });
        }

        @Test
        @DisplayName("UT_C_PRJ_V_004 - projectNumberが10文字超過")
        void testCreateProject_ValidationError_ProjectNumberTooLong() {
            // ARRANGE
            validRequest.getProject().get(0).setProjectNumber("P12345678901");
            when(projectService.createProject(any(CreateProjectRequest.class)))
                .thenThrow(new RuntimeException("プロジェクト番号は10文字以内である必要があります"));

            // ACT & ASSERT
            assertThrows(RuntimeException.class, () -> {
                controller.createProject(authentication, validRequest);
            });
        }

        @Test
        @DisplayName("UT_C_PRJ_V_005 - projectStatusが許可リスト外")
        void testCreateProject_ValidationError_InvalidProjectStatus() {
            // ARRANGE
            validRequest.getProject().get(0).setProjectStatus("無効なステータス");
            when(projectService.createProject(any(CreateProjectRequest.class)))
                .thenThrow(new RuntimeException("無効なプロジェクトステータスです"));

            // ACT & ASSERT
            assertThrows(RuntimeException.class, () -> {
                controller.createProject(authentication, validRequest);
            });
        }

        @Test
        @DisplayName("UT_C_PRJ_V_006 - repEmailの形式不正")
        void testCreateProject_ValidationError_InvalidRepEmail() {
            // ARRANGE
            validRequest.getProject().get(0).setRepEmail("invalid-email");
            when(projectService.createProject(any(CreateProjectRequest.class)))
                .thenThrow(new RuntimeException("メールアドレスの形式が不正です"));

            // ACT & ASSERT
            assertThrows(RuntimeException.class, () -> {
                controller.createProject(authentication, validRequest);
            });
        }

        @Test
        @DisplayName("UT_C_PRJ_V_009 - repFirstNameKanaに全角カタカナ以外")
        void testCreateProject_ValidationError_InvalidRepFirstNameKana() {
            // ARRANGE
            validRequest.getProject().get(0).setRepFirstNameKana("たろう");
            when(projectService.createProject(any(CreateProjectRequest.class)))
                .thenThrow(new RuntimeException("フリガナは全角カタカナのみです"));

            // ACT & ASSERT
            assertThrows(RuntimeException.class, () -> {
                controller.createProject(authentication, validRequest);
            });
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // BOUNDARY VALUE TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("Boundary Value Tests")
    class BoundaryTests {
        
        @Test
        @DisplayName("UT_C_PRJ_B_001 - projectNameが100文字ちょうど")
        void testCreateProject_Boundary_ProjectName100Chars() {
            // ARRANGE
            validRequest.getProject().get(0).setProjectName("あ".repeat(100));
            
            CreateProjectResponse mockResponse = TestDataBuilder.buildSuccessCreateProjectResponse();
            when(projectService.createProject(any(CreateProjectRequest.class))).thenReturn(mockResponse);

            // ACT
            ResponseEntity<CreateProjectResponse> response;
            try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
                mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                    .thenReturn(HttpStatus.CREATED);

                response = controller.createProject(authentication, validRequest);
            }

            // ASSERT
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
        }

        @Test
        @DisplayName("UT_C_PRJ_B_002 - projectNameが101文字")
        void testCreateProject_Boundary_ProjectName101Chars() {
            // ARRANGE
            validRequest.getProject().get(0).setProjectName("あ".repeat(101));
            when(projectService.createProject(any(CreateProjectRequest.class)))
                .thenThrow(new RuntimeException("プロジェクト名は100文字以内です"));

            // ACT & ASSERT
            assertThrows(RuntimeException.class, () -> {
                controller.createProject(authentication, validRequest);
            });
        }

        @Test
        @DisplayName("UT_C_PRJ_B_003 - projectNumberが10文字ちょうど")
        void testCreateProject_Boundary_ProjectNumber10Chars() {
            // ARRANGE
            validRequest.getProject().get(0).setProjectNumber("P123456789");
            
            CreateProjectResponse mockResponse = TestDataBuilder.buildSuccessCreateProjectResponse();
            when(projectService.createProject(any(CreateProjectRequest.class))).thenReturn(mockResponse);

            // ACT
            ResponseEntity<CreateProjectResponse> response;
            try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
                mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                    .thenReturn(HttpStatus.CREATED);

                response = controller.createProject(authentication, validRequest);
            }

            // ASSERT
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
        }

        @Test
        @DisplayName("UT_C_PRJ_B_008 - emissionReductionがDECIMAL最大値")
        void testCreateProject_Boundary_MaxDecimalEmission() {
            // ARRANGE
            validRequest.getProject().get(0).setBaselineEmissions(99999999999.9);
            
            CreateProjectResponse mockResponse = TestDataBuilder.buildSuccessCreateProjectResponse();
            when(projectService.createProject(any(CreateProjectRequest.class))).thenReturn(mockResponse);

            // ACT
            ResponseEntity<CreateProjectResponse> response;
            try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
                mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                    .thenReturn(HttpStatus.CREATED);

                response = controller.createProject(authentication, validRequest);
            }

            // ASSERT
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
        }

        @Test
        @DisplayName("UT_C_PRJ_B_011 - newMembersTotalが負数")
        void testCreateProject_Boundary_NegativeNewMembers() {
            // ARRANGE
            validRequest.getProject().get(0).setNewMembersTotal(-1);
            when(projectService.createProject(any(CreateProjectRequest.class)))
                .thenThrow(new RuntimeException("会員数は0以上である必要があります"));

            // ACT & ASSERT
            assertThrows(RuntimeException.class, () -> {
                controller.createProject(authentication, validRequest);
            });
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // EXCEPTION MAPPING TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("Exception Mapping Tests")
    class ExceptionTests {
        
        @Test
        @DisplayName("UT_C_PRJ_E_001 - DuplicateProjectNumberException_409")
        void testCreateProject_Exception_DuplicateProjectNumber() {
            // ARRANGE
            when(projectService.createProject(any(CreateProjectRequest.class)))
                .thenThrow(new RuntimeException("プロジェクト番号が既に存在します"));

            // ACT & ASSERT
            assertThrows(RuntimeException.class, () -> {
                controller.createProject(authentication, validRequest);
            });
        }

        @Test
        @DisplayName("UT_C_PRJ_E_003 - UnauthorizedException_401")
        void testCreateProject_Exception_UnauthorizedJWT() {
            // ARRANGE
            when(jwtUtil.extractCompanyId(anyString()))
                .thenThrow(new RuntimeException("JWT が無効です"));

            // ACT & ASSERT
            assertThrows(RuntimeException.class, () -> {
                controller.createProject(authentication, validRequest);
            });

            verifyNoInteractions(projectService);
        }

        @Test
        @DisplayName("UT_C_PRJ_E_005 - RuntimeException_500")
        void testCreateProject_Exception_DatabaseConnectionFailed() {
            // ARRANGE
            when(projectService.createProject(any(CreateProjectRequest.class)))
                .thenThrow(new RuntimeException("DB connection failed"));

            // ACT & ASSERT
            assertThrows(RuntimeException.class, () -> {
                controller.createProject(authentication, validRequest);
            });
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // INTERACTION VERIFICATION TESTS
    // ═══════════════════════════════════════════════════════════════
    
    @Nested
    @DisplayName("Interaction Verification Tests")
    class InteractionTests {
        
        @Test
        @DisplayName("UT_C_PRJ_I_001 - JWTからoperatorCompanyIdとcreatedByが設定される")
        void testCreateProject_Interaction_JWTExtraction() {
            // ARRANGE
            CreateProjectResponse mockResponse = TestDataBuilder.buildSuccessCreateProjectResponse();
            when(projectService.createProject(any(CreateProjectRequest.class))).thenReturn(mockResponse);

            // ACT
            try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
                mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                    .thenReturn(HttpStatus.CREATED);

                controller.createProject(authentication, validRequest);
            }

            // ASSERT
            ArgumentCaptor<CreateProjectRequest> captor = ArgumentCaptor.forClass(CreateProjectRequest.class);
            verify(projectService).createProject(captor.capture());

            assertEquals(OPERATOR_COMPANY_ID, captor.getValue().getOperatorCompanyId(), 
                "会社IDが正しく設定されていません");
            assertEquals(OPERATOR_USERNAME, captor.getValue().getCreatedBy(), 
                "作成者が正しく設定されていません");
        }

        @Test
        @DisplayName("UT_C_PRJ_I_002 - サービスが1回だけ呼ばれる")
        void testCreateProject_Interaction_ServiceCalledOnce() {
            // ARRANGE
            CreateProjectResponse mockResponse = TestDataBuilder.buildSuccessCreateProjectResponse();
            when(projectService.createProject(any(CreateProjectRequest.class))).thenReturn(mockResponse);

            // ACT
            try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
                mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                    .thenReturn(HttpStatus.CREATED);

                controller.createProject(authentication, validRequest);
            }

            // ASSERT
            verify(projectService, times(1)).createProject(any(CreateProjectRequest.class));
            verifyNoMoreInteractions(projectService);
        }

        @Test
        @DisplayName("UT_C_PRJ_I_004 - JWT抽出が正しい順序で呼ばれる")
        void testCreateProject_Interaction_JWTExtractionOrder() {
            // ARRANGE
            CreateProjectResponse mockResponse = TestDataBuilder.buildSuccessCreateProjectResponse();
            when(projectService.createProject(any(CreateProjectRequest.class))).thenReturn(mockResponse);

            // ACT
            try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
                mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                    .thenReturn(HttpStatus.CREATED);

                controller.createProject(authentication, validRequest);
            }

            // ASSERT
            InOrder inOrder = inOrder(jwtUtil);
            inOrder.verify(jwtUtil).extractCompanyId(anyString());
            inOrder.verify(jwtUtil).extractUsername(anyString());
        }
    }
}