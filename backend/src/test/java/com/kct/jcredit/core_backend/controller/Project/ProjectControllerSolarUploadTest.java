// package com.kct.jcredit.core_backend.controller.Project;

// import com.kct.jcredit.core_backend.constant.ResponseConstants;
// import com.kct.jcredit.core_backend.dto.response.FileUploadSolarResponse;
// import com.kct.jcredit.core_backend.dto.response.ProjectResponse;
// import com.kct.jcredit.core_backend.exception.CustomException;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.mockito.MockedStatic;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.mock.web.MockMultipartFile;
// import org.springframework.web.multipart.MultipartFile;
// import org.springframework.security.core.Authentication;

// import com.kct.jcredit.core_backend.util.ResponseUtil;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNotNull;
// import static org.junit.jupiter.api.Assertions.assertThrows;
// import static org.mockito.Mockito.*;

// @DisplayName("ProjectController - Solar Register File Uploads")
// class ProjectControllerSolarUploadTest extends ProjectControllerBaseTest {

//     // ==================== Test Data ====================
//     private static final String PROJECT_ID = "PRJ-001";
//     private static final Integer SITE_ID = 100;
//     private final String USERNAME = "test_user";
//     private static final String DESCRIPTION = "テスト用のアップロードです"; 

//     private MockMultipartFile mockFile;
//     private FileUploadSolarResponse mockSuccessResponse;
//     private Authentication mockAuthentication; 

//     @BeforeEach
//     void setUp() {
//         mockFile = new MockMultipartFile(
//                 "file",                  
//                 "test-document.pdf",     
//                 "application/pdf",       
//                 "dummy content".getBytes() 
//         );
//         mockSuccessResponse = new FileUploadSolarResponse();
//         mockSuccessResponse.setStatusCode(ResponseConstants.OK);
//         mockSuccessResponse.setMessage("ファイルが正常にアップロードされました");
//         mockAuthentication = mock(Authentication.class);
//         String dummyToken = "dummy-jwt-token";
        
//         // Simulasikan alur internal di dalam Controller kamu
//         when(mockAuthentication.getCredentials()).thenReturn(dummyToken);
//         when(jwtUtil.extractUsername(dummyToken)).thenReturn(USERNAME);
//     }

//     // ==========================================================
//     // 1. Upload Equipment Op Date
//     // ==========================================================
//     @Test
//     @DisplayName("UT-PC-10: Should upload Equipment Op Date successfully")
//     void uploadEquipmentOpDate_success() {
//         // Given
//         when(fileUploadSolarService.uploadEquipmentOpDate(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenReturn(mockSuccessResponse);

//         // When
//         ResponseEntity<FileUploadSolarResponse> response = controller.uploadEquipmentOpDate(
//                 mockAuthentication, 
//                 PROJECT_ID, 
//                 SITE_ID, 
//                 mockFile, 
//                 DESCRIPTION
//         );

//         // Then
//         assertSuccessResponse(response);
//         verify(fileUploadSolarService, times(1)).uploadEquipmentOpDate(SITE_ID, mockFile, DESCRIPTION, USERNAME);
//     }

//    @Test
//     @DisplayName("UT-PC-11: Should throw CustomException(400) when file format is invalid (e.g., not PDF)")
//     void uploadEquipmentOpDate_invalidFileFormat_throwsException() {
//         // Given
//         when(fileUploadSolarService.uploadEquipmentOpDate(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenThrow(new CustomException("400", "無効なファイル形式です。PDFのみ許可されています。"));

//         // When & Then
//         CustomException exception = assertThrows(CustomException.class, () -> {
//             controller.uploadEquipmentOpDate(mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);
//         });

//         assertEquals("400", exception.getErrorCode(), "エラーコードが一致しません");
//         assertEquals("無効なファイル形式です。PDFのみ許可されています。", exception.getMessage(), "エラーメッセージが一致しません");
        
//         verify(fileUploadSolarService, times(1)).uploadEquipmentOpDate(SITE_ID, mockFile, DESCRIPTION, USERNAME);
//     }

//     @Test
//     @DisplayName("UT-PC-12: Should throw CustomException(404) when siteId is not found")
//     void uploadEquipmentOpDate_siteNotFound_throwsException() {
//         when(fileUploadSolarService.uploadEquipmentOpDate(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenThrow(new CustomException("404", "指定されたサイトIDが見つかりません。"));

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             controller.uploadEquipmentOpDate(mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);
//         });

//         assertEquals("404", exception.getErrorCode(), "エラーコードが一致しません");
//         assertEquals("指定されたサイトIDが見つかりません。", exception.getMessage(), "エラーメッセージが一致しません");
//     }
//     // ==========================================================
//     // 2. Upload Nominal Maximum Output
//     // ==========================================================
//     @Test
//     @DisplayName("UT-PC-13: Should upload Nominal Max Output successfully")
//     void uploadNominalMaxOutput_success() {
//         // Given
//         when(fileUploadSolarService.uploadNominalMaxOutput(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenReturn(mockSuccessResponse);

//         // When
//         ResponseEntity<FileUploadSolarResponse> response = controller.uploadNominalMaxOutput(
//                 mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);

//         // Then
//         assertSuccessResponse(response);
//         verify(fileUploadSolarService, times(1)).uploadNominalMaxOutput(SITE_ID, mockFile, DESCRIPTION, USERNAME);
//     }

//     @Test
//     @DisplayName("UT-PC-14: Should throw CustomException(400) when file format is invalid (e.g., not PDF)")
//     void uploadNominalMaxOutput_invalidFileFormat_throwsException() {
//         // Given
//         when(fileUploadSolarService.uploadNominalMaxOutput(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenThrow(new CustomException("400", "無効なファイル形式です。PDFのみ許可されています。"));

//         // When & Then
//         CustomException exception = assertThrows(CustomException.class, () -> {
//             controller.uploadNominalMaxOutput(mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);
//         });

//         assertEquals("400", exception.getErrorCode(), "エラーコードが一致しません");
//         assertEquals("無効なファイル形式です。PDFのみ許可されています。", exception.getMessage(), "エラーメッセージが一致しません");
        
//         verify(fileUploadSolarService, times(1)).uploadNominalMaxOutput(SITE_ID, mockFile, DESCRIPTION, USERNAME);
//     }

//     @Test
//     @DisplayName("UT-PC-15: Should throw CustomException(404) when siteId is not found")
//     void uploadNominalMaxOutput_siteNotFound_throwsException() {
//         // Given
//         when(fileUploadSolarService.uploadNominalMaxOutput(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenThrow(new CustomException("404", "指定されたサイトIDが見つかりません。"));

//         // When & Then
//         CustomException exception = assertThrows(CustomException.class, () -> {
//             controller.uploadNominalMaxOutput(mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);
//         });

//         assertEquals("404", exception.getErrorCode(), "エラーコードが一致しません");
//         assertEquals("指定されたサイトIDが見つかりません。", exception.getMessage(), "エラーメッセージが一致しません");
//     }

//     // ==========================================================
//     // 3. Upload Power Conditioner Standby
//     // ==========================================================
//     @Test
//     @DisplayName("UT-PC-16: Should upload Power Cond Standby successfully")
//     void uploadPowerCondStandby_success() {
//         // Given
//         when(fileUploadSolarService.uploadPowerCondStandby(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenReturn(mockSuccessResponse);

//         // When
//         ResponseEntity<FileUploadSolarResponse> response = controller.uploadPowerCondStandby(
//                 mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);

//         // Then
//         assertSuccessResponse(response);
//         verify(fileUploadSolarService, times(1)).uploadPowerCondStandby(SITE_ID, mockFile, DESCRIPTION, USERNAME);
//     }

//     @Test
//     @DisplayName("UT-PC-17: Should throw CustomException(400) when file format is invalid (e.g., not PDF)")
//     void uploadPowerCondStandby_invalidFileFormat_throwsException() {
//         when(fileUploadSolarService.uploadPowerCondStandby(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenThrow(new CustomException("400", "無効なファイル形式です。PDFのみ許可されています。"));

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             controller.uploadPowerCondStandby(mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);
//         });

//         assertEquals("400", exception.getErrorCode(), "エラーコードが一致しません");
//         assertEquals("無効なファイル形式です。PDFのみ許可されています。", exception.getMessage(), "エラーメッセージが一致しません");
        
//         verify(fileUploadSolarService, times(1)).uploadPowerCondStandby(SITE_ID, mockFile, DESCRIPTION, USERNAME);
//     }

//     @Test
//     @DisplayName("UT-PC-18: Should throw CustomException(404) when siteId is not found")
//     void uploadPowerCondStandby_siteNotFound_throwsException() {
//         when(fileUploadSolarService.uploadPowerCondStandby(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenThrow(new CustomException("404", "指定されたサイトIDが見つかりません。"));

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             controller.uploadPowerCondStandby(mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);
//         });

//         assertEquals("404", exception.getErrorCode(), "エラーコードが一致しません");
//         assertEquals("指定されたサイトIDが見つかりません。", exception.getMessage(), "エラーメッセージが一致しません");
//     }

//     // ==========================================================
//     // 4. Upload Battery Info
//     // ==========================================================
//     @Test
//     @DisplayName("UT-PC-19: Should upload Battery Info successfully")
//     void uploadBatteryInfo_success() {
//         when(fileUploadSolarService.uploadBatteryInfo(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenReturn(mockSuccessResponse);

//         ResponseEntity<FileUploadSolarResponse> response = controller.uploadBatteryInfo(
//                 mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);

//         assertSuccessResponse(response);
//         verify(fileUploadSolarService, times(1)).uploadBatteryInfo(SITE_ID, mockFile, DESCRIPTION, USERNAME);
//     }

//     @Test
//     @DisplayName("UT-PC-20: Should throw CustomException(400) when file format is invalid (e.g., not PDF)")
//     void uploadBatteryInfo_invalidFileFormat_throwsException() {
//         when(fileUploadSolarService.uploadBatteryInfo(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenThrow(new CustomException("400", "無効なファイル形式です。PDFのみ許可されています。"));

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             controller.uploadBatteryInfo(mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);
//         });

//         assertEquals("400", exception.getErrorCode(), "エラーコードが一致しません");
//         assertEquals("無効なファイル形式です。PDFのみ許可されています。", exception.getMessage(), "エラーメッセージが一致しません");
        
//         verify(fileUploadSolarService, times(1)).uploadBatteryInfo(SITE_ID, mockFile, DESCRIPTION, USERNAME);
//     }

//     @Test
//     @DisplayName("UT-PC-21: Should throw CustomException(404) when siteId is not found")
//     void uploadBatteryInfo_siteNotFound_throwsException() {
//         when(fileUploadSolarService.uploadBatteryInfo(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenThrow(new CustomException("404", "指定されたサイトIDが見つかりません。"));

//         // When & Then
//         CustomException exception = assertThrows(CustomException.class, () -> {
//             controller.uploadBatteryInfo(mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);
//         });

//         assertEquals("404", exception.getErrorCode(), "エラーコードが一致しません");
//         assertEquals("指定されたサイトIDが見つかりません。", exception.getMessage(), "エラーメッセージが一致しません");
//     }

//     // ==========================================================
//     // 5. Upload Total Investment
//     // ==========================================================
//     @Test
//     @DisplayName("UT-PC-22: Should upload Total Investment successfully")
//     void uploadTotalInvestment_success() {
//         when(fileUploadSolarService.uploadTotalInvestment(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenReturn(mockSuccessResponse);

//         ResponseEntity<FileUploadSolarResponse> response = controller.uploadTotalInvestment(
//                 mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);

//         assertSuccessResponse(response);
//         verify(fileUploadSolarService, times(1)).uploadTotalInvestment(SITE_ID, mockFile, DESCRIPTION, USERNAME);
//     }

//     @Test
//     @DisplayName("UT-PC-23: Should throw CustomException(400) when file format is invalid (e.g., not PDF)")
//     void uploadTotalInvestment_invalidFileFormat_throwsException() {
//         when(fileUploadSolarService.uploadTotalInvestment(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenThrow(new CustomException("400", "無効なファイル形式です。PDFのみ許可されています。"));

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             controller.uploadTotalInvestment(mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);
//         });

//         assertEquals("400", exception.getErrorCode(), "エラーコードが一致しません");
//         assertEquals("無効なファイル形式です。PDFのみ許可されています。", exception.getMessage(), "エラーメッセージが一致しません");
        
//         verify(fileUploadSolarService, times(1)).uploadTotalInvestment(SITE_ID, mockFile, DESCRIPTION, USERNAME );
//     }

//     @Test
//     @DisplayName("UT-PC-24: Should throw CustomException(404) when siteId is not found")
//     void uploadTotalInvestment_siteNotFound_throwsException() {
//         when(fileUploadSolarService.uploadTotalInvestment(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenThrow(new CustomException("404", "指定されたサイトIDが見つかりません。"));

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             controller.uploadTotalInvestment(mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);
//         });

//         assertEquals("404", exception.getErrorCode(), "エラーコードが一致しません");
//         assertEquals("指定されたサイトIDが見つかりません。", exception.getMessage(), "エラーメッセージが一致しません");
//     }
    

//     // ==========================================================
//     // 6. Upload Subsidy Amount
//     // ==========================================================
//     @Test
//     @DisplayName("UT-PC-25: Should upload Subsidy Amount successfully")
//     void uploadSubsidyAmount_success() {
//         when(fileUploadSolarService.uploadSubsidyAmount(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenReturn(mockSuccessResponse);

//         ResponseEntity<FileUploadSolarResponse> response = controller.uploadSubsidyAmount(
//                 mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);

//         assertSuccessResponse(response);
//         verify(fileUploadSolarService, times(1)).uploadSubsidyAmount(SITE_ID, mockFile, DESCRIPTION, USERNAME);
//     }

//     @Test
//     @DisplayName("UT-PC-26: Should throw CustomException(400) when file format is invalid (e.g., not PDF)")
//     void uploadSubsidyAmount_invalidFileFormat_throwsException() {
//         when(fileUploadSolarService.uploadSubsidyAmount(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenThrow(new CustomException("400", "無効なファイル形式です。PDFのみ許可されています。"));

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             controller.uploadSubsidyAmount(mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);
//         });

//         assertEquals("400", exception.getErrorCode(), "エラーコードが一致しません");
//         assertEquals("無効なファイル形式です。PDFのみ許可されています。", exception.getMessage(), "エラーメッセージが一致しません");
        
//         verify(fileUploadSolarService, times(1)).uploadSubsidyAmount(SITE_ID, mockFile, DESCRIPTION, USERNAME );
//     }

//     @Test
//     @DisplayName("UT-PC-27: Should throw CustomException(404) when siteId is not found")
//     void uploadSubsidyAmount_siteNotFound_throwsException() {
//         when(fileUploadSolarService.uploadSubsidyAmount(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenThrow(new CustomException("404", "指定されたサイトIDが見つかりません。"));

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             controller.uploadSubsidyAmount(mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);
//         });

//         assertEquals("404", exception.getErrorCode(), "エラーコードが一致しません");
//         assertEquals("指定されたサイトIDが見つかりません。", exception.getMessage(), "エラーメッセージが一致しません");
//     }
    

//     // ==========================================================
//     // 7. Upload Pre Project Unit Price
//     // ==========================================================
//     @Test
//     @DisplayName("UT-PC-28: Should upload Pre Project Unit Price successfully")
//     void uploadPreProjectUnitPrice_success() {
//         when(fileUploadSolarService.uploadPreProjectUnitPrice(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenReturn(mockSuccessResponse);

//         ResponseEntity<FileUploadSolarResponse> response = controller.uploadPreProjectUnitPrice(
//                 mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);

//         assertSuccessResponse(response);
//         verify(fileUploadSolarService, times(1)).uploadPreProjectUnitPrice(SITE_ID, mockFile, DESCRIPTION, USERNAME);
//     }

//     @Test
//     @DisplayName("UT-PC-29: Should throw CustomException(400) when file format is invalid (e.g., not PDF)")
//     void uploadPreProjectUnitPrice_invalidFileFormat_throwsException() {
//         when(fileUploadSolarService.uploadPreProjectUnitPrice(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenThrow(new CustomException("400", "無効なファイル形式です。PDFのみ許可されています。"));

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             controller.uploadPreProjectUnitPrice(mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);
//         });

//         assertEquals("400", exception.getErrorCode(), "エラーコードが一致しません");
//         assertEquals("無効なファイル形式です。PDFのみ許可されています。", exception.getMessage(), "エラーメッセージが一致しません");
        
//         verify(fileUploadSolarService, times(1)).uploadPreProjectUnitPrice(SITE_ID, mockFile, DESCRIPTION, USERNAME);
//     }

//     @Test
//     @DisplayName("UT-PC-30: Should throw CustomException(404) when siteId is not found")
//     void uploadPreProjectUnitPrice_siteNotFound_throwsException() {
//         when(fileUploadSolarService.uploadPreProjectUnitPrice(SITE_ID, mockFile, DESCRIPTION, USERNAME))
//                 .thenThrow(new CustomException("404", "指定されたサイトIDが見つかりません。"));

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             controller.uploadPreProjectUnitPrice(mockAuthentication, PROJECT_ID, SITE_ID, mockFile, DESCRIPTION);
//         });

//         assertEquals("404", exception.getErrorCode(), "エラーコードが一致しません");
//         assertEquals("指定されたサイトIDが見つかりません。", exception.getMessage(), "エラーメッセージが一致しません");
//     }

//     // ==================== Helper Method ====================
//     private void assertSuccessResponse(ResponseEntity<FileUploadSolarResponse> response) {
//         assertNotNull(response, "レスポンスがnullです");
//         assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTPステータスがOKではありません");
//         assertNotNull(response.getBody(), "レスポンスボディがnullです");
//         assertEquals(ResponseConstants.OK, response.getBody().getStatusCode(), "ステータスコードが一致しません");
//         assertEquals("ファイルが正常にアップロードされました", response.getBody().getMessage());
//     }

   
// }