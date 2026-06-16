package com.kct.jcredit.core_backend.controller.Project;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.ProjectMonitoringResponse;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("ProjectController - Get Monitoring Details")
class ProjectControllerMonitoringTest extends ProjectControllerBaseTest {

    private static final String PROJECT_ID = "PRJ-001";
    private ProjectMonitoringResponse mockSuccessResponse;

    @BeforeEach
    void setUp() {
        mockSuccessResponse = new ProjectMonitoringResponse();
        mockSuccessResponse.setStatusCode(ResponseConstants.OK);
        mockSuccessResponse.setMessage("モニタリングデータを取得しました"); 
    }

    // ==========================================================
    // 1. Happy Path (200 OK)
    // ==========================================================
    @Test
    @DisplayName("UT-PC-04: Should return monitoring details successfully")
    void getProjectMonitoringDetails_success() {
        when(projectMonitoringService.getProjectDetail(PROJECT_ID))
                .thenReturn(mockSuccessResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                    .thenReturn(HttpStatus.OK);
            ResponseEntity<ProjectMonitoringResponse> response = controller.getProjectMonitoringDetails(PROJECT_ID);

            // Then
            assertNotNull(response, "レスポンスがnullです");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTPステータスがOKではありません");
            assertNotNull(response.getBody(), "レスポンスボディがnullです");
            assertEquals(ResponseConstants.OK, response.getBody().getStatusCode(), "ステータスコードが一致しません");
            assertEquals("モニタリングデータを取得しました", response.getBody().getMessage());

            verify(projectMonitoringService, times(1)).getProjectDetail(PROJECT_ID);
        }
    }

    // ==========================================================
    // 2. Unhappy Path - Data Not Found (404)
    // ==========================================================
    @Test
    @DisplayName("UT-PC-05: Should throw CustomException(404) when project is not found")
    void getProjectMonitoringDetails_notFound_throwsException() {
        // Given
        when(projectMonitoringService.getProjectDetail(PROJECT_ID))
                .thenThrow(new CustomException("404", "該当するプロジェクトが見つかりませんでした。"));
        CustomException exception = assertThrows(CustomException.class, () -> {
            controller.getProjectMonitoringDetails(PROJECT_ID);
        });

        assertEquals("404", exception.getErrorCode(), "エラーコードが一致しません");
        assertEquals("該当するプロジェクトが見つかりませんでした。", exception.getMessage(), "エラーメッセージが一致しません");
        
        verify(projectMonitoringService, times(1)).getProjectDetail(PROJECT_ID);
    }

    // ==========================================================
    // 3. Unhappy Path - Server Error (500)
    // ==========================================================
    @Test
    @DisplayName("UT-PC-06: Should throw CustomException(500) when server error occurs")
    void getProjectMonitoringDetails_serverError_throwsException() {
        // Given
        when(projectMonitoringService.getProjectDetail(PROJECT_ID))
                .thenThrow(new CustomException("500", "サーバー内部でエラーが発生しました。"));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            controller.getProjectMonitoringDetails(PROJECT_ID);
        });

        assertEquals("500", exception.getErrorCode(), "エラーコードが一致しません");
        assertEquals("サーバー内部でエラーが発生しました。", exception.getMessage(), "エラーメッセージが一致しません");
        
        verify(projectMonitoringService, times(1)).getProjectDetail(PROJECT_ID);
    }
}