package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.request.RejectCompanyRequest;
import com.kct.jcredit.core_backend.dto.response.ApproveCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.PendingCompanyListResponse;
import com.kct.jcredit.core_backend.dto.response.RejectCompanyResponse;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.service.CompanyApprovalService;
import com.kct.jcredit.core_backend.util.ResponseUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
 
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminCompanyControllerTest {
    
    @Mock
    private CompanyApprovalService companyApprovalService;
 
    @InjectMocks
    private AdminCompanyController adminCompanyController;
 
    @Test
    @DisplayName("UT-AC-101 有効な companyId で承認が成功した場合、200OK レスポンスを返す")
    void approveCompany_validCompanyId_returnsOkResponse() {
        // Arrange
        String companyId = "COMP001";

        ApproveCompanyResponse mockResponse = new ApproveCompanyResponse();
        mockResponse.setStatusCode("200");
        mockResponse.setMessage("会社を承認しました");

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(companyApprovalService.approveCompany(companyId)).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

            // Act
            ResponseEntity<ApproveCompanyResponse> response =
                    adminCompanyController.approveCompany(companyId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isEqualTo(mockResponse);

            verify(companyApprovalService, times(1)).approveCompany(companyId);
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("200"), times(1));
        }
    }

    @Test
    @DisplayName("UT-AC-102 companyId に対応する会社が見つからない場合、404 NOT_FOUND レスポンスを返す")
    void approveCompany_companyNotFound_returnsNotFoundResponse() {
        // Arrange
        String companyId = "COMP999";

        ApproveCompanyResponse mockResponse = new ApproveCompanyResponse();
        mockResponse.setStatusCode("404");
        mockResponse.setMessage("会社が見つかりません");

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(companyApprovalService.approveCompany(companyId)).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("404")).thenReturn(HttpStatus.NOT_FOUND);

            // Act
            ResponseEntity<ApproveCompanyResponse> response =
                    adminCompanyController.approveCompany(companyId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isEqualTo(mockResponse);

            verify(companyApprovalService, times(1)).approveCompany(companyId);
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("404"), times(1));
        }
    }

    @Test
    @DisplayName("UT-AC-103 サービスが CustomException をスローする場合、例外が伝播される")
    void approveCompany_serviceThrowsCustomException_propagatesException() {
        // Arrange
        String companyId = "COMP001";

        CustomException expectedException = new CustomException(
                "E003",
                "会社の承認処理に失敗しました"
        );

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(companyApprovalService.approveCompany(companyId)).thenThrow(expectedException);

        // Act
        CustomException thrown = assertThrows(
                CustomException.class,
                () -> adminCompanyController.approveCompany(companyId)
        );

        // Assert
        assertThat(thrown.getMessage()).isEqualTo("会社の承認処理に失敗しました");
        assertThat(thrown.getErrorCode()).isEqualTo("E003");

            verify(companyApprovalService, times(1)).approveCompany(companyId);
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus(anyString()), never());
        }
    }

    @Test
    @DisplayName("UT-AC-104 有効なリクエストで却下が成功した場合、200OK レスポンスを返す")
    void rejectCompany_validRequest_returnsOkResponse() {
        // Arrange
        String companyId = "COMP001";
        String rejectionReason = "申請内容に不備があります";

        RejectCompanyRequest request = new RejectCompanyRequest();
        request.setRejectionReason(rejectionReason);

        RejectCompanyResponse mockResponse = new RejectCompanyResponse();
        mockResponse.setStatusCode("200");
        mockResponse.setMessage("会社を却下しました");

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(companyApprovalService.rejectCompany(companyId, rejectionReason))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

            // Act
            ResponseEntity<RejectCompanyResponse> response =
                    adminCompanyController.rejectCompany(companyId, request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(mockResponse);

            verify(companyApprovalService, times(1)).rejectCompany(companyId, rejectionReason);
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("200"), times(1));
        }
    }

    @Test
    @DisplayName("UT-AC-105 companyId に対応する会社が見つからない場合、404 NOT_FOUND レスポンスを返す")
    void rejectCompany_companyNotFound_returnsNotFoundResponse() {
        // Arrange
        String companyId = "COMP999";
        String rejectionReason = "申請内容に不備があります";

        RejectCompanyRequest request = new RejectCompanyRequest();
        request.setRejectionReason(rejectionReason);

        RejectCompanyResponse mockResponse = new RejectCompanyResponse();
        mockResponse.setStatusCode("404");
        mockResponse.setMessage("会社が見つかりません");

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(companyApprovalService.rejectCompany(companyId, rejectionReason))
                    .thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("404")).thenReturn(HttpStatus.NOT_FOUND);

            // Act
            ResponseEntity<RejectCompanyResponse> response =
                    adminCompanyController.rejectCompany(companyId, request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isEqualTo(mockResponse);

            verify(companyApprovalService, times(1)).rejectCompany(companyId, rejectionReason);
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("404"), times(1));
        }
    }

    @Test
    @DisplayName("UT-AC-106 サービスが CustomException をスローする場合、例外が伝播される")
    void rejectCompany_serviceThrowsCustomException_propagatesException() {
    // Arrange
        String companyId = "COMP001";
        String rejectionReason = "申請内容に不備があります";

        RejectCompanyRequest request = new RejectCompanyRequest();
        request.setRejectionReason(rejectionReason);

        CustomException expectedException = new CustomException(
                "E003",
                "会社の却下処理に失敗しました"
        );

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(companyApprovalService.rejectCompany(companyId, rejectionReason))
                    .thenThrow(expectedException);

            // Act
            CustomException thrown = assertThrows(
                    CustomException.class,
                    () -> adminCompanyController.rejectCompany(companyId, request)
            );
            // Assert
            assertThat(thrown.getMessage()).isEqualTo("会社の却下処理に失敗しました");
            assertThat(thrown.getErrorCode()).isEqualTo("E003");

            verify(companyApprovalService, times(1)).rejectCompany(companyId, rejectionReason);
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus(anyString()), never());
        }
    }

    @Test
    @DisplayName("UT-AC-107 承認待ちの会社が存在する場合、200OK レスポンスを返す")
    void getPendingCompanies_withData_returnsOkResponse() {
        // Arrange
        PendingCompanyListResponse mockResponse = new PendingCompanyListResponse();
        mockResponse.setStatusCode("200");

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(companyApprovalService.getPendingCompanies()).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

            // Act
            ResponseEntity<PendingCompanyListResponse> response =
                    adminCompanyController.getPendingCompanies();

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(mockResponse);

            verify(companyApprovalService, times(1)).getPendingCompanies();
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("200"), times(1));
        }
    }

    @Test
    @DisplayName("UT-AC-108 承認待ちの会社が存在しない場合、空リストで200OK レスポンスを返す")
    void getPendingCompanies_emptyList_returnsOkResponse() {
        // Arrange
        PendingCompanyListResponse mockResponse = new PendingCompanyListResponse();
        mockResponse.setStatusCode("200");
        // (default)
        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(companyApprovalService.getPendingCompanies()).thenReturn(mockResponse);
            mockedUtil.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

            // Act
            ResponseEntity<PendingCompanyListResponse> response =
                    adminCompanyController.getPendingCompanies();

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(mockResponse);

            verify(companyApprovalService, times(1)).getPendingCompanies();
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus("200"), times(1));
        }
    }

    @Test
    @DisplayName("UT-AC-109 サービスが CustomException をスローする場合、例外が伝播される")
    void getPendingCompanies_serviceThrowsCustomException_propagatesException() {
        // Arrange
        CustomException expectedException = new CustomException(
                "E003",
                "承認待ち会社一覧の取得に失敗しました"
        );

        try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
            when(companyApprovalService.getPendingCompanies()).thenThrow(expectedException);

            // Act
            CustomException thrown = assertThrows(
                    CustomException.class,
                    adminCompanyController::getPendingCompanies
            );

            // Assert
            assertThat(thrown.getMessage()).isEqualTo("承認待ち会社一覧の取得に失敗しました");
            assertThat(thrown.getErrorCode()).isEqualTo("E003");

            verify(companyApprovalService, times(1)).getPendingCompanies();
            mockedUtil.verify(() -> ResponseUtil.getHttpStatus(anyString()), never());
        }
    }
}
