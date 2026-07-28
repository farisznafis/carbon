package com.kct.jcredit.core_backend.controller.Project;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.ProjectUploadDocumentRequest;
import com.kct.jcredit.core_backend.dto.response.ProjectUploadDocumentResponse;
import com.kct.jcredit.core_backend.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("ProjectController - Upload Approval Documents")
class ProjectControllerUploadDocumentTest extends ProjectControllerBaseTest {

    private static final String PROJECT_ID = "PRJ-001";

    private MockMultipartFile mockRegistrationCert;
    private MockMultipartFile mockValidationReport;
    private MockMultipartFile mockVerificationReport;
    private ProjectUploadDocumentResponse mockSuccessResponse;

    @BeforeEach
    void setUp() {
        mockRegistrationCert = new MockMultipartFile(
                "プロジェクト登録証", "registration.pdf", "application/pdf", "dummy content 1".getBytes());
        mockValidationReport = new MockMultipartFile(
                "妥当性報告書", "validation.pdf", "application/pdf", "dummy content 2".getBytes());
        mockVerificationReport = new MockMultipartFile(
                "検証報告書", "verification.pdf", "application/pdf", "dummy content 3".getBytes());

        mockSuccessResponse = new ProjectUploadDocumentResponse();
        mockSuccessResponse.setStatusCode(ResponseConstants.OK);

        mockSuccessResponse.setMessage("資料が正常にアップロードされました"); 
    }

    // ==========================================================
    // 1. Happy Path (Success)
    // ==========================================================
    @Test
    @DisplayName("UT-PC-43: Should upload all 3 documents successfully")
    void uploadApprovalDocuments_success() {

        when(projectUploadDocumentService.processUpload(
                eq(PROJECT_ID), eq(mockRegistrationCert), eq(mockValidationReport), eq(mockVerificationReport), anyString()))
                .thenReturn(mockSuccessResponse);

        ProjectUploadDocumentResponse response = controller.uploadApprovalDocuments(
                authentication, PROJECT_ID, mockRegistrationCert, mockValidationReport, mockVerificationReport);

        assertNotNull(response, "レスポンスがnullです");
        assertEquals(ResponseConstants.OK, response.getStatusCode(), "ステータスコードが一致しません");
        assertEquals("資料が正常にアップロードされました", response.getMessage(), "メッセージが一致しません");

        verify(projectUploadDocumentService, times(1)).processUpload(
                eq(PROJECT_ID), eq(mockRegistrationCert), eq(mockValidationReport), eq(mockVerificationReport), anyString());
    }

    // ==========================================================
    // 2. Unhappy Path - Validation Error (400)
    // ==========================================================
    @Test
    @DisplayName("UT-PC-44: Should throw CustomException(400) when a file is invalid")
    void uploadApprovalDocuments_invalidFile_throwsException() {
        when(projectUploadDocumentService.processUpload(
                eq(PROJECT_ID), eq(mockRegistrationCert), eq(mockValidationReport), eq(mockVerificationReport), anyString()))
                .thenThrow(new CustomException("400", "無効なファイル形式です。"));

        CustomException exception = assertThrows(CustomException.class, () -> {
            controller.uploadApprovalDocuments(
                    authentication, PROJECT_ID, mockRegistrationCert, mockValidationReport, mockVerificationReport);
        });

        assertEquals("400", exception.getErrorCode(), "エラーコードが一致しません");
        assertEquals("無効なファイル形式です。", exception.getMessage(), "エラーメッセージが一致しません");
        
        verify(projectUploadDocumentService, times(1)).processUpload(
                eq(PROJECT_ID), eq(mockRegistrationCert), eq(mockValidationReport), eq(mockVerificationReport), anyString());
    }

    // ==========================================================
    // 3. Unhappy Path - Server Error (500)
    // ==========================================================
    @Test
    @DisplayName("UT-PC-45: Should throw CustomException(500) when server error occurs")
    void uploadApprovalDocuments_serverError_throwsException() {
        when(projectUploadDocumentService.processUpload(
                eq(PROJECT_ID), eq(mockRegistrationCert), eq(mockValidationReport), eq(mockVerificationReport), anyString()))
                .thenThrow(new CustomException("500", "サーバー内部でエラーが発生しました。"));

        CustomException exception = assertThrows(CustomException.class, () -> {
            controller.uploadApprovalDocuments(
                    authentication, PROJECT_ID, mockRegistrationCert, mockValidationReport, mockVerificationReport);
        });

        assertEquals("500", exception.getErrorCode(), "エラーコードが一致しません");
        assertEquals("サーバー内部でエラーが発生しました。", exception.getMessage(), "エラーメッセージが一致しません");
        
        verify(projectUploadDocumentService, times(1)).processUpload(
                eq(PROJECT_ID), eq(mockRegistrationCert), eq(mockValidationReport), eq(mockVerificationReport), anyString());
    }

    // ==========================================================
    // 4. GET Document List
    // ==========================================================
    
    @Test
    @DisplayName("UT-PC-46: Should return document list successfully")
    void getDocumentList_success() {
        when(projectUploadDocumentService.getDocumentsByProjectId(PROJECT_ID))
                .thenReturn(mockSuccessResponse);

        ProjectUploadDocumentResponse response = controller.getDocumentList(PROJECT_ID);

        assertNotNull(response, "レスポンスがnullです");
        assertEquals(ResponseConstants.OK, response.getStatusCode(), "ステータスコードが一致しません");

        verify(projectUploadDocumentService, times(1)).getDocumentsByProjectId(PROJECT_ID);
    }

    @Test
    @DisplayName("UT-PC-47: Should throw CustomException(404) when project not found for document list")
    void getDocumentList_notFound_throwsException() {
        when(projectUploadDocumentService.getDocumentsByProjectId(PROJECT_ID))
                .thenThrow(new CustomException("404", "該当するプロジェクトが見つかりませんでした。")); // Project tidak ditemukan

        CustomException exception = assertThrows(CustomException.class, () -> {
            controller.getDocumentList(PROJECT_ID);
        });

        assertEquals("404", exception.getErrorCode(), "エラーコードが一致しません");
        assertEquals("該当するプロジェクトが見つかりませんでした。", exception.getMessage(), "エラーメッセージが一致しません");
        
        verify(projectUploadDocumentService, times(1)).getDocumentsByProjectId(PROJECT_ID);
    }

    // ==========================================================
    // 5. PATCH Document Status
    // ==========================================================
    
    @Test
    @DisplayName("UT-PC-48: Should update document status successfully")
    void updateDocumentStatus_success() {

        ProjectUploadDocumentRequest mockRequest = new ProjectUploadDocumentRequest();
        // mockRequest.setStatus("承認済み");
        
        when(projectUploadDocumentService.updateStatus(eq(PROJECT_ID), any(ProjectUploadDocumentRequest.class)))
                .thenReturn(mockSuccessResponse);

        ProjectUploadDocumentResponse response = controller.updateDocumentStatus(PROJECT_ID, mockRequest);

        assertNotNull(response, "レスポンスがnullです");
        assertEquals(ResponseConstants.OK, response.getStatusCode(), "ステータスコードが一致しません");

        verify(projectUploadDocumentService, times(1)).updateStatus(PROJECT_ID, mockRequest);
    }

    @Test
    @DisplayName("UT-PC-49: Should throw CustomException(400) when status update data is invalid")
    void updateDocumentStatus_invalidRequest_throwsException() {

        ProjectUploadDocumentRequest mockRequest = new ProjectUploadDocumentRequest();
        
        when(projectUploadDocumentService.updateStatus(eq(PROJECT_ID), any(ProjectUploadDocumentRequest.class)))
                .thenThrow(new CustomException("400", "無効なステータスです。")); // Status tidak valid

        CustomException exception = assertThrows(CustomException.class, () -> {
            controller.updateDocumentStatus(PROJECT_ID, mockRequest);
        });

        assertEquals("400", exception.getErrorCode(), "エラーコードが一致しません");
        assertEquals("無効なステータスです。", exception.getMessage(), "エラーメッセージが一致しません");
        
        verify(projectUploadDocumentService, times(1)).updateStatus(PROJECT_ID, mockRequest);
    }
}