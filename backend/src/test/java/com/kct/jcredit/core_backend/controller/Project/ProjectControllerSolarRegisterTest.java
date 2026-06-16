// package com.kct.jcredit.core_backend.controller.Project;

// import com.kct.jcredit.core_backend.constant.ResponseConstants;
// import com.kct.jcredit.core_backend.dto.request.EquipmentItemSolarRequest;
// import com.kct.jcredit.core_backend.dto.response.EquipmentSolarRegisterResponse;
// import com.kct.jcredit.core_backend.exception.CustomException;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNotNull;
// import static org.junit.jupiter.api.Assertions.assertThrows;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.*;

// @DisplayName("ProjectController - Solar Register")
// class ProjectControllerSolarRegisterTest extends ProjectControllerBaseTest {

//     private EquipmentItemSolarRequest mockRequest;
//     private EquipmentSolarRegisterResponse mockSuccessResponse;

//     @BeforeEach
//     void setUp() {
//         // 1. Buat dummy request (cukup instansiasi saja, isinya tidak penting untuk tes controller ini)
//         mockRequest = new EquipmentItemSolarRequest();

//         // 2. Setup response sukses dari service
//         mockSuccessResponse = new EquipmentSolarRegisterResponse();
//         mockSuccessResponse.setStatusCode(ResponseConstants.OK);
//     }

//     // ==========================================================
//     // 1. Happy Path (200 OK)
//     // ==========================================================
//     @Test
//     @DisplayName("UT-PC-07: Should return OK when registration is successful")
//     void registerSolar_success() {
//         // Given
//         when(equipmentSolarService.processRegistration(any(EquipmentItemSolarRequest.class)))
//                 .thenReturn(mockSuccessResponse);

//         // When
//         ResponseEntity<EquipmentSolarRegisterResponse> response = controller.registerSolar(mockRequest);

//         // Then
//         assertNotNull(response, "レスポンスがnullです");
//         assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTPステータスがOKではありません");
//         assertNotNull(response.getBody(), "レスポンスボディがnullです");
//         assertEquals(ResponseConstants.OK, response.getBody().getStatusCode(), "ステータスコードが一致しません");
//         // assertEquals("登録が完了しました", response.getBody().getMessage());

//         verify(equipmentSolarService, times(1)).processRegistration(mockRequest);
//     }

//     // ==========================================================
//     // 2. Unhappy Path - Error Validasi (400)
//     // ==========================================================
//     @Test
//     @DisplayName("UT-PC-08: Should throw CustomException(400) when request data is invalid")
//     void registerSolar_invalidRequest_throwsException() {
//         // Given
//         when(equipmentSolarService.processRegistration(any(EquipmentItemSolarRequest.class)))
//                 .thenThrow(new CustomException("400", "入力データに誤りがあります。")); // "Ada kesalahan pada data input"

//         // When & Then
//         CustomException exception = assertThrows(CustomException.class, () -> {
//             controller.registerSolar(mockRequest);
//         });

//         assertEquals("400", exception.getErrorCode(), "エラーコードが一致しません");
//         assertEquals("入力データに誤りがあります。", exception.getMessage(), "エラーメッセージが一致しません");
        
//         verify(equipmentSolarService, times(1)).processRegistration(mockRequest);
//     }

//     // ==========================================================
//     // 3. Unhappy Path - Error Server/Lainnya (500)
//     // ==========================================================
//     @Test
//     @DisplayName("UT-PC-09: Should throw CustomException(500) when internal server error occurs")
//     void registerSolar_serverError_throwsException() {
//         when(equipmentSolarService.processRegistration(any(EquipmentItemSolarRequest.class)))
//                 .thenThrow(new CustomException("500", "サーバー内部でエラーが発生しました。")); // "Terjadi error di internal server"

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             controller.registerSolar(mockRequest);
//         });

//         assertEquals("500", exception.getErrorCode(), "エラーコードが一致しません");
//         assertEquals("サーバー内部でエラーが発生しました。", exception.getMessage(), "エラーメッセージが一致しません");
        
//         verify(equipmentSolarService, times(1)).processRegistration(mockRequest);
//     }
// }
