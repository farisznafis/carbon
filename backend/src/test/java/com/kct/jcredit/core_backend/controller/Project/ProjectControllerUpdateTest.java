package com.kct.jcredit.core_backend.controller.Project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.controller.ProjectController;
import com.kct.jcredit.core_backend.dto.response.ProjectResponse;
import com.kct.jcredit.core_backend.dto.response.details.ProjectDetailResponse;
import com.kct.jcredit.core_backend.dto.request.UpdateProjectRequest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import org.junit.jupiter.api.DisplayName;

@DisplayName("ProjectController - PATCH /api/projects/{projectId}")
class ProjectControllerUpdateTest extends ProjectControllerBaseTest {
 
    // ==================== Test Data ====================
    
    private static final Integer PROJECT_ID = 1;
    private static final String UPDATED_PROJECT_NAME = "Updated Solar Project";
    private static final String REGIONAL_CATEGORY = "関東";
    private static final String METHODOLOGY_VERSION = "Ver.2.0";
    private static final String OPERATOR_COMPANY_ID = "123";
    private static final String OPERATOR_USERNAME = "operator_user";
    
    // ==================== 200 OK (Success) ====================
    
    @Test
    @DisplayName("UT-PC-31: Should return OK and project when patch request is valid")
    void should_return_ok_and_project_when_patch_request_is_valid() {
        // Given
        UpdateProjectRequest request = buildValidUpdateRequest();
        ProjectResponse mockResponse = buildSuccessUpdateResponse();
        
        when(projectService.updateProject(any(UpdateProjectRequest.class)))
            .thenReturn(mockResponse);
        
        ResponseEntity<ProjectResponse> response = controller.updateProject(
            authentication,
            PROJECT_ID,
            request
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getBody().getProject()).isNotNull();
        
        ProjectDetailResponse projectDetail = response.getBody().getProject();
        assertThat(projectDetail.getProjectName()).isEqualTo(UPDATED_PROJECT_NAME);
        assertThat(projectDetail.getRegionalCategory()).isEqualTo(REGIONAL_CATEGORY);
        assertThat(projectDetail.getMethodologyVersion()).isEqualTo(METHODOLOGY_VERSION);
        assertThat(projectDetail.getUpdatedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("UT-PC-32: Should call service once with project ID and request when request is valid")
void should_call_service_once_with_project_id_and_request_when_request_is_valid() {
    UpdateProjectRequest request = buildValidUpdateRequest();
    ProjectResponse mockResponse = buildSuccessUpdateResponse();

    setupJwtCompanyId(OPERATOR_COMPANY_ID); 
    setupJwtUsername(OPERATOR_USERNAME);   
    
    when(projectService.updateProject(any(UpdateProjectRequest.class)))
        .thenReturn(mockResponse);

    controller.updateProject(authentication, PROJECT_ID, request);

    verify(projectService, times(1)).updateProject(any(UpdateProjectRequest.class));

    verify(projectService).updateProject(argThat(req -> 
        req.getId() == PROJECT_ID && 
        OPERATOR_COMPANY_ID.equals(req.getCompanyId()) && 
        OPERATOR_USERNAME.equals(req.getProcessedBy())    
    ));
}
    
    @Test
    @DisplayName("UT-PC-33: Should set correct companyId and processedBy from JWT token")
    void should_set_correct_company_id_and_processed_by_from_jwt_token() {
        // Given
        String customCompanyId = "999";
        String customUsername = "custom_operator";
        
        setupJwtCompanyId(customCompanyId);
        setupJwtUsername(customUsername);
        
        UpdateProjectRequest request = buildValidUpdateRequest();
        ProjectResponse mockResponse = buildSuccessUpdateResponse();
        
        when(projectService.updateProject(any(UpdateProjectRequest.class)))
            .thenReturn(mockResponse);

        controller.updateProject(authentication, PROJECT_ID, request);
        
        verify(projectService).updateProject(argThat(req -> 
            req.getCompanyId() == customCompanyId && 
            req.getProcessedBy().equals(customUsername)
        ));
    }
    
    // ==================== 404 NOT_FOUND ====================
    
    @Test
    @DisplayName("UT-PC-34: Should return not found when project does not exist")
    void should_return_not_found_when_project_does_not_exist() {
        UpdateProjectRequest request = buildValidUpdateRequest();
        ProjectResponse errorResponse = buildNotFoundResponse();
        
        when(projectService.updateProject(any(UpdateProjectRequest.class)))
            .thenReturn(errorResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
        mockedStatic.when(() -> ResponseUtil.getHttpStatus("404"))
                .thenReturn(HttpStatus.NOT_FOUND);

        ResponseEntity<ProjectResponse> response = controller.updateProject(
            authentication,
            PROJECT_ID,
            request
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatusCode()).isEqualTo("404");
        assertThat(response.getBody().getMessage())
            .isEqualTo("該当するプロジェクトが見つかりませんでした。");
        }
    }
// ==================== 404 NOT_FOUND ====================

    @Test
    @DisplayName("UT-PC-35: Should throw exception when service throws ProjectNotFoundException (404)")
    void should_throw_exception_when_service_throws_project_not_found_exception() {
        UpdateProjectRequest request = buildValidUpdateRequest();

        setupJwtCompanyId(OPERATOR_COMPANY_ID);
        setupJwtUsername(OPERATOR_USERNAME);
        
        when(projectService.updateProject(any(UpdateProjectRequest.class)))
        .thenThrow(new CustomException("403", "このリソースにアクセスする権限がありません。"));

        CustomException exception = assertThrows(CustomException.class, () -> {
            controller.updateProject(authentication, PROJECT_ID, request);
        });

        assertEquals("403", exception.getErrorCode());
        assertEquals("このリソースにアクセスする権限がありません。", exception.getMessage());
    }
    
    // ==================== 403 FORBIDDEN ====================
    
    @Test
    @DisplayName("UT-PC-36: Should throw exception when service throws AccessDeniedException (403)")
    void should_throw_exception_when_service_throws_access_denied_exception() {
        UpdateProjectRequest request = buildValidUpdateRequest();

        setupJwtCompanyId(OPERATOR_COMPANY_ID);
        setupJwtUsername(OPERATOR_USERNAME);
        
        when(projectService.updateProject(any(UpdateProjectRequest.class)))
        .thenThrow(new CustomException("404", "該当するプロジェクトが見つかりませんでした。"));

        CustomException exception = assertThrows(CustomException.class, () -> {
            controller.updateProject(authentication, PROJECT_ID, request);
        });

        assertEquals("404", exception.getErrorCode());
        assertEquals("該当するプロジェクトが見つかりませんでした。", exception.getMessage());
    }
    
    // ==================== 500 SERVER_ERROR ====================
    
    @Test
    @DisplayName("UT-PC-37: Should return server error when service throws unexpected exception")
    void should_return_server_error_when_service_throws_unexpected_exception() {
        UpdateProjectRequest request = buildValidUpdateRequest();
        ProjectResponse errorResponse = buildServerErrorResponse();
        
        when(projectService.updateProject(any(UpdateProjectRequest.class)))
            .thenReturn(errorResponse);

        ResponseEntity<ProjectResponse> response = controller.updateProject(
            authentication,
            PROJECT_ID,
            request
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatusCode()).isEqualTo("500");
        assertThat(response.getBody().getMessage())
            .isEqualTo("サーバー内部でエラーが発生しました。");
    }
    
    @Test
    @DisplayName("UT-PC-38: Should throw exception when service throws RuntimeException")
    void should_throw_exception_when_service_throws_runtime_exception() {
        // Given
        UpdateProjectRequest request = buildValidUpdateRequest();
        
        when(projectService.updateProject(any(UpdateProjectRequest.class)))
            .thenThrow(new RuntimeException("Unexpected database error"));
        
        // When & Then
        
        assertThatThrownBy(() -> controller.updateProject(authentication, PROJECT_ID, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Unexpected database error");
    }
    
    // ==================== Request Population ====================
    
    @Test
    @DisplayName("UT-PC-39: Should populate request ID from path variable")
    void should_populate_request_id_from_path_variable() {
        // Given
        Integer customProjectId = 999;
        UpdateProjectRequest request = buildValidUpdateRequest();
        ProjectResponse mockResponse = buildSuccessUpdateResponse();
        
        when(projectService.updateProject(any(UpdateProjectRequest.class)))
            .thenReturn(mockResponse);

        controller.updateProject(authentication, customProjectId, request);

        verify(projectService).updateProject(argThat(req -> 
            req.getId() == customProjectId
        ));
    }
    
    // ==================== Test Data Builders ====================
    
    private UpdateProjectRequest buildValidUpdateRequest() {
        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setProjectName(UPDATED_PROJECT_NAME);
        request.setRegionalCategory(REGIONAL_CATEGORY);
        request.setMethodologyVersion(METHODOLOGY_VERSION);
        // request.setProjectStatus("計画変更");
        request.setRepEmail("updated.email@example.co.jp");
        request.setRepPhoneNumber("+819012345678");
        request.setPicEmail("updated.pic@example.co.jp");
        request.setPicPhoneNumber("+819087654321");
        // Note: id, companyId, processedBy will be set by controller
        return request;
    }
    
    private ProjectResponse buildSuccessUpdateResponse() {
        ProjectResponse response = new ProjectResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("プロジェクトが正常に更新されました");
        
        ProjectDetailResponse projectDetail = new ProjectDetailResponse();
        projectDetail.setId(PROJECT_ID.toString());
        projectDetail.setProjectName(UPDATED_PROJECT_NAME);
        projectDetail.setRegionalCategory(REGIONAL_CATEGORY);
        projectDetail.setMethodologyVersion(METHODOLOGY_VERSION);
        projectDetail.setProjectStatus("計画変更");
        projectDetail.setUpdatedAt(LocalDateTime.now());
        // projectDetail.setUpdatedBy(OPERATOR_USERNAME);
        
        response.setProject(projectDetail);
        
        return response;
    }
    
    private ProjectResponse buildNotFoundResponse() {
        ProjectResponse response = new ProjectResponse();
        response.setStatusCode("404");
        response.setMessage("該当するプロジェクトが見つかりませんでした。");
        return response;
    }
    
    private ProjectResponse buildForbiddenResponse() {
        ProjectResponse response = new ProjectResponse();
        response.setStatusCode("403");
        response.setMessage("このリソースにアクセスする権限がありません。");
        return response;
    }
    
    private ProjectResponse buildServerErrorResponse() {
        ProjectResponse response = new ProjectResponse();
        response.setStatusCode("500");
        response.setMessage("サーバー内部でエラーが発生しました。");
        return response;
    }

     // ==========================================================
    // GET Project Details (/api/projects/{projectId})
    // ==========================================================

    @Test
    @DisplayName("UT-PC-40: Should return project details successfully")
    void getProject_success() {
        // Given
        Integer projectId = 1;
        
        setupJwtCompanyId(OPERATOR_COMPANY_ID); 
        
        ProjectResponse mockResponse = new ProjectResponse();
        mockResponse.setStatusCode(ResponseConstants.OK);
        mockResponse.setMessage("プロジェクト詳細を取得しました");
        
        when(projectService.getProject(projectId, OPERATOR_COMPANY_ID))
                .thenReturn(mockResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                    .thenReturn(HttpStatus.OK);

            ResponseEntity<ProjectResponse> response = controller.getProject(authentication, projectId);

            assertNotNull(response, "レスポンスがnullです");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTPステータスがOKではありません");
            assertNotNull(response.getBody(), "レスポンスボディがnullです");
            assertEquals(ResponseConstants.OK, response.getBody().getStatusCode(), "ステータスコードが一致しません");

            verify(projectService, times(1)).getProject(projectId, OPERATOR_COMPANY_ID);
        }
    }

    // ==========================================================
    // Unhappy Path - Project Not Found (404)
    // ==========================================================
    @Test
    @DisplayName("UT-PC-41: Should throw CustomException(404) when project details not found")
    void getProject_notFound_throwsException() {
        Integer projectId = 999; 
        setupJwtCompanyId(OPERATOR_COMPANY_ID); 
        
        when(projectService.getProject(projectId, OPERATOR_COMPANY_ID))
                .thenThrow(new CustomException("404", "該当するプロジェクトが見つかりませんでした。"));

        CustomException exception = assertThrows(CustomException.class, () -> {
            controller.getProject(authentication, projectId);
        });

        assertEquals("404", exception.getErrorCode(), "エラーコードが一致しません");
        assertEquals("該当するプロジェクトが見つかりませんでした。", exception.getMessage(), "エラーメッセージが一致しません");
        
        verify(projectService, times(1)).getProject(projectId, OPERATOR_COMPANY_ID);
    }

    // ==========================================================
    // Unhappy Path - Server Error (500)
    // ==========================================================
    @Test
    @DisplayName("UT-PC-42: Should throw CustomException(500) when server error occurs")
    void getProject_serverError_throwsException() {
        // Given
        Integer projectId = 1;
        setupJwtCompanyId(OPERATOR_COMPANY_ID); 
        
        when(projectService.getProject(projectId, OPERATOR_COMPANY_ID))
                .thenThrow(new CustomException("500", "サーバー内部でエラーが発生しました。"));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            controller.getProject(authentication, projectId);
        });

        assertEquals("500", exception.getErrorCode(), "エラーコードが一致しません");
        assertEquals("サーバー内部でエラーが発生しました。", exception.getMessage(), "エラーメッセージが一致しません");
        
        verify(projectService, times(1)).getProject(projectId, OPERATOR_COMPANY_ID);
    }
}