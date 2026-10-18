package com.kct.jcredit.core_backend.controller.Project;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.CreateProjectRequest;
import com.kct.jcredit.core_backend.dto.response.CreateProjectResponse;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@DisplayName("ProjectController - Create Project")
class ProjectControllerCreateTest extends ProjectControllerBaseTest {

    // ==========================================================
    // 1. Happy Path (200 OK)
    // ==========================================================
    @Test
    @DisplayName("UT-PC-01: Should create project successfully and return OK")
    void createProject_success() {
        CreateProjectRequest request = TestDataBuilder.buildValidCreateProjectRequest();
        CreateProjectResponse mockResponse = TestDataBuilder.buildSuccessCreateProjectResponse();

        setupJwtCompanyId(OPERATOR_COMPANY_ID);
        setupJwtUsername(OPERATOR_USERNAME);

        when(projectService.createProject(any(CreateProjectRequest.class)))
                .thenReturn(mockResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                    .thenReturn(HttpStatus.OK);

            ResponseEntity<CreateProjectResponse> response = controller.createProject(authentication, request);

            assertNotNull(response, "レスポンスがnullです");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTPステータスがOKではありません");
            assertNotNull(response.getBody(), "レスポンスボディがnullです");
            assertEquals(ResponseConstants.OK, response.getBody().getStatusCode(), "ステータスコードが一致しません");
            assertEquals("プロジェクトが正常に作成されました", response.getBody().getMessage());

    
            verify(projectService, times(1)).createProject(argThat(req ->
                    OPERATOR_COMPANY_ID.equals(req.getOperatorCompanyId()) &&
                    OPERATOR_USERNAME.equals(req.getCreatedBy())
            ));
        }
    }

    // ==========================================================
    // 2. Unhappy Path - Validation Error (400)
    // ==========================================================
    @Test
    @DisplayName("UT-PC-02: Should throw CustomException(400) when request data is invalid")
    void createProject_invalidRequest_throwsException() {
        // Given
        CreateProjectRequest request = TestDataBuilder.buildValidCreateProjectRequest();
        
        setupJwtCompanyId(OPERATOR_COMPANY_ID);
        setupJwtUsername(OPERATOR_USERNAME);

        when(projectService.createProject(any(CreateProjectRequest.class)))
                .thenThrow(new CustomException("400", "入力データに誤りがあります。")); 

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            controller.createProject(authentication, request);
        });

        assertEquals("400", exception.getErrorCode(), "エラーコードが一致しません");
        assertEquals("入力データに誤りがあります。", exception.getMessage(), "エラーメッセージが一致しません");
        
        // Verifikasi bahwa service tetap sempat dipanggil sebelum melempar exception
        verify(projectService, times(1)).createProject(any(CreateProjectRequest.class));
    }

    // ==========================================================
    // 3. Unhappy Path - Server Error (500)
    // ==========================================================
    @Test
    @DisplayName("UT-PC-03: Should throw CustomException(500) when internal server error occurs")
    void createProject_serverError_throwsException() {
        // Given
        CreateProjectRequest request = TestDataBuilder.buildValidCreateProjectRequest();
        
        setupJwtCompanyId(OPERATOR_COMPANY_ID);
        setupJwtUsername(OPERATOR_USERNAME);

        when(projectService.createProject(any(CreateProjectRequest.class)))
                .thenThrow(new CustomException("500", "サーバー内部でエラーが発生しました。"));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            controller.createProject(authentication, request);
        });

        assertEquals("500", exception.getErrorCode(), "エラーコードが一致しません");
        assertEquals("サーバー内部でエラーが発生しました。", exception.getMessage(), "エラーメッセージが一致しません");
        
        verify(projectService, times(1)).createProject(any(CreateProjectRequest.class));
    }
}