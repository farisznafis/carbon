package com.kct.jcredit.core_backend.controller;

// TODO: sesuaikan package & import dengan struktur project asli
// TODO: nama class controller ini placeholder ("CancelRequestController") — sesuaikan
// TODO: PENTING — kode makeDecision() yang diberikan TERPOTONG (tidak ada return/closing brace).
//       Test di bawah MENGASUMSIKAN pola yang sama seperti createCancelRequest:
//         return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
//       Cek kode asli sebelum menjalankan — kalau polanya beda, sesuaikan bagian
//       MockedStatic<ResponseUtil> di kelas MakeDecisionTest.
// TODO: struktur CreateCancelRequestRequest / CancelRequestDecisionRequest ditebak minimal —
//       sesuaikan field-nya (mis. monitoringRecordId, reason, decision) dengan DTO asli.

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.kct.jcredit.core_backend.dto.request.CancelRequestDecisionRequest;
import com.kct.jcredit.core_backend.dto.request.CreateCancelRequestRequest;
import com.kct.jcredit.core_backend.dto.response.CancelRequestResponse;
import com.kct.jcredit.core_backend.dto.response.CancelRequestDecisionResponse;
import com.kct.jcredit.core_backend.dto.response.CancelRequestListResponse;
import com.kct.jcredit.core_backend.service.CancelRequestService;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import com.kct.jcredit.core_backend.util.JwtUtil;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelRequestController のテスト")
class CancelRequestControllerTest {

    @Mock
    private CancelRequestService cancelRequestService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private MonitoringController cancelRequestController;

    private static final String TOKEN = "dummy-token";

    // ============================================================================
    // createCancelRequest
    // ============================================================================
    @Nested
    @DisplayName("createCancelRequest のテスト")
    class CreateCancelRequestTest {

        @Nested
        @DisplayName("【正常系】")
        class SuccessCase {

            @Test
            @DisplayName("SC-1: 有効なリクエストで取消申請が作成された場合、200OK レスポンスを返す")
            void createCancelRequest_validRequest_returnsOkResponse() {
                // Arrange
                CreateCancelRequestRequest request = new CreateCancelRequestRequest();
                // TODO: set field request sesuai struktur asli (mis. monitoringRecordId, reason)

                CancelRequestResponse mockResponse = new CancelRequestResponse();
                mockResponse.setStatusCode("200");
                mockResponse.setMessage("取消申請を受け付けました");

                try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                    when(authentication.getCredentials()).thenReturn(TOKEN);
                    when(jwtUtil.extractUsername(TOKEN)).thenReturn("user001");
                    when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
                    when(cancelRequestService.createCancelRequest("user001", "COMP001", request))
                            .thenReturn(mockResponse);
                    mockedUtil.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

                    // Act
                    ResponseEntity<CancelRequestResponse> response =
                            cancelRequestController.createCancelRequest(authentication, request);

                    // Assert
                    assertThat(response).isNotNull();
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isEqualTo(mockResponse);

                    // getCredentials() dipanggil 2x karena diekstrak ulang untuk username & companyId
                    // (bukan disimpan ke variabel seperti pola di controller lain — lihat catatan review)
                    verify(authentication, times(2)).getCredentials();
                    verify(jwtUtil, times(1)).extractUsername(TOKEN);
                    verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
                    verify(cancelRequestService, times(1))
                            .createCancelRequest("user001", "COMP001", request);
                    mockedUtil.verify(() -> ResponseUtil.getHttpStatus("200"), times(1));
                }
            }
        }

        @Nested
        @DisplayName("【異常系】")
        class ErrorCase {

            @Test
            @DisplayName("EC-1: 既に取消申請が存在する場合、409 CONFLICT レスポンスを返す")
            void createCancelRequest_alreadyExists_returnsConflictResponse() {
                // Arrange
                CreateCancelRequestRequest request = new CreateCancelRequestRequest();

                CancelRequestResponse mockResponse = new CancelRequestResponse();
                mockResponse.setStatusCode("409");
                mockResponse.setMessage("既に取消申請が存在します");

                try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                    when(authentication.getCredentials()).thenReturn(TOKEN);
                    when(jwtUtil.extractUsername(TOKEN)).thenReturn("user001");
                    when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
                    when(cancelRequestService.createCancelRequest("user001", "COMP001", request))
                            .thenReturn(mockResponse);
                    mockedUtil.when(() -> ResponseUtil.getHttpStatus("409")).thenReturn(HttpStatus.CONFLICT);

                    // Act
                    ResponseEntity<CancelRequestResponse> response =
                            cancelRequestController.createCancelRequest(authentication, request);

                    // Assert
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(response.getBody()).isEqualTo(mockResponse);
                    mockedUtil.verify(() -> ResponseUtil.getHttpStatus("409"), times(1));
                }
            }

            @Test
            @DisplayName("EC-2: トークンが無効で extractUsername が例外をスローする場合、companyId 抽出・サービス呼び出しは行われない")
            void createCancelRequest_extractUsernameThrows_skipsCompanyIdAndService() {
                // Arrange
                CreateCancelRequestRequest request = new CreateCancelRequestRequest();
                CustomException expectedException =
                        new CustomException("E004", "トークンが無効です");

                try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                    when(authentication.getCredentials()).thenReturn("");
                    when(jwtUtil.extractUsername("")).thenThrow(expectedException);

                    // Act & Assert
                    CustomException thrown = assertThrows(CustomException.class,
                            () -> cancelRequestController.createCancelRequest(authentication, request));

                    assertThat(thrown.getMessage()).isEqualTo("トークンが無効です");

                    verify(jwtUtil, times(1)).extractUsername("");
                    // Kode memanggil extractUsername lebih dulu — kalau itu gagal,
                    // extractCompanyId dan service TIDAK BOLEH pernah dipanggil.
                    verify(jwtUtil, never()).extractCompanyId(any());
                    verifyNoInteractions(cancelRequestService);
                    mockedUtil.verify(() -> ResponseUtil.getHttpStatus(any()), never());
                }
            }

            @Test
            @DisplayName("EC-3: username 抽出は成功するが companyId 抽出が例外をスローする場合、サービスは呼ばれない")
            void createCancelRequest_extractCompanyIdThrows_skipsService() {
                // Arrange
                CreateCancelRequestRequest request = new CreateCancelRequestRequest();
                CustomException expectedException =
                        new CustomException("E005", "会社情報を取得できません");

                try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                    when(authentication.getCredentials()).thenReturn(TOKEN);
                    when(jwtUtil.extractUsername(TOKEN)).thenReturn("user001");
                    when(jwtUtil.extractCompanyId(TOKEN)).thenThrow(expectedException);

                    // Act & Assert
                    CustomException thrown = assertThrows(CustomException.class,
                            () -> cancelRequestController.createCancelRequest(authentication, request));

                    assertThat(thrown.getMessage()).isEqualTo("会社情報を取得できません");

                    verify(jwtUtil, times(1)).extractUsername(TOKEN);
                    verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
                    verifyNoInteractions(cancelRequestService);
                }
            }

            @Test
            @DisplayName("EC-4: サービスが CustomException をスローする場合、例外が伝播される")
            void createCancelRequest_serviceThrowsCustomException_propagatesException() {
                // Arrange
                CreateCancelRequestRequest request = new CreateCancelRequestRequest();
                CustomException expectedException =
                        new CustomException("E003", "取消申請の作成に失敗しました");

                try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                    when(authentication.getCredentials()).thenReturn(TOKEN);
                    when(jwtUtil.extractUsername(TOKEN)).thenReturn("user001");
                    when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
                    when(cancelRequestService.createCancelRequest("user001", "COMP001", request))
                            .thenThrow(expectedException);

                    // Act & Assert
                    CustomException thrown = assertThrows(CustomException.class,
                            () -> cancelRequestController.createCancelRequest(authentication, request));

                    assertThat(thrown.getMessage()).isEqualTo("取消申請の作成に失敗しました");

                    verify(cancelRequestService, times(1))
                            .createCancelRequest("user001", "COMP001", request);
                    mockedUtil.verify(() -> ResponseUtil.getHttpStatus(any()), never());
                }
            }
        }
    }

    // ============================================================================
    // getCancelRequests
    // ============================================================================
    @Nested
    @DisplayName("getCancelRequests のテスト")
    class GetCancelRequestsTest {

        @Nested
        @DisplayName("【正常系】")
        class SuccessCase {

            @Test
            @DisplayName("SC-1: 取消申請が存在する場合、200OK レスポンスを返す")
            void getCancelRequests_withData_returnsOkResponse() {
                // Arrange
                CancelRequestListResponse mockResponse = new CancelRequestListResponse();
                mockResponse.setStatusCode("200");
                // TODO: set list data sesuai struktur asli CancelRequestListResponse

                try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                    when(cancelRequestService.listCancelRequests()).thenReturn(mockResponse);
                    mockedUtil.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

                    // Act
                    ResponseEntity<CancelRequestListResponse> response =
                            cancelRequestController.getCancelRequests(authentication);

                    // Assert
                    assertThat(response).isNotNull();
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isEqualTo(mockResponse);

                    verify(cancelRequestService, times(1)).listCancelRequests();
                    mockedUtil.verify(() -> ResponseUtil.getHttpStatus("200"), times(1));

                    // Parameter Authentication TIDAK PERNAH dipakai di dalam method
                    // (lihat catatan review) — dibuktikan di sini secara eksplisit.
                    verifyNoInteractions(authentication);
                    verifyNoInteractions(jwtUtil);
                }
            }

            @Test
            @DisplayName("SC-2: 取消申請が1件もない場合、空リストで200OK レスポンスを返す")
            void getCancelRequests_emptyList_returnsOkResponse() {
                // Arrange
                CancelRequestListResponse mockResponse = new CancelRequestListResponse();
                mockResponse.setStatusCode("200");
                // list kosong (default)

                try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                    when(cancelRequestService.listCancelRequests()).thenReturn(mockResponse);
                    mockedUtil.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

                    // Act
                    ResponseEntity<CancelRequestListResponse> response =
                            cancelRequestController.getCancelRequests(authentication);

                    // Assert
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isEqualTo(mockResponse);

                    verify(cancelRequestService, times(1)).listCancelRequests();
                }
            }
        }

        @Nested
        @DisplayName("【異常系】")
        class ErrorCase {

            @Test
            @DisplayName("EC-1: サービスが CustomException をスローする場合、例外が伝播される")
            void getCancelRequests_serviceThrowsCustomException_propagatesException() {
                // Arrange
                CustomException expectedException =
                        new CustomException("E003", "取消申請一覧の取得に失敗しました");

                try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                    when(cancelRequestService.listCancelRequests()).thenThrow(expectedException);

                    // Act & Assert
                    CustomException thrown = assertThrows(CustomException.class,
                            () -> cancelRequestController.getCancelRequests(authentication));

                    assertThat(thrown.getMessage()).isEqualTo("取消申請一覧の取得に失敗しました");

                    verify(cancelRequestService, times(1)).listCancelRequests();
                    mockedUtil.verify(() -> ResponseUtil.getHttpStatus(any()), never());
                }
            }
        }
    }

    // ============================================================================
    // makeDecision
    // ============================================================================
    @Nested
    @DisplayName("makeDecision のテスト")
    class MakeDecisionTest {

        private static final Integer CANCEL_REQUEST_ID = 10;

        @Nested
        @DisplayName("【正常系】")
        class SuccessCase {

            @Test
            @DisplayName("SC-1: 承認決定が成功した場合、200OK レスポンスを返す（相殺レコード作成はサービス層の責務）")
            void makeDecision_approve_returnsOkResponse() {
                // Arrange
                CancelRequestDecisionRequest request = new CancelRequestDecisionRequest();
                // TODO: set field decision sesuai struktur asli (mis. request.setDecision("APPROVE"))

                CancelRequestDecisionResponse mockResponse = new CancelRequestDecisionResponse();
                mockResponse.setStatusCode("200");
                mockResponse.setMessage("取消申請を承認しました");

                try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                    when(authentication.getCredentials()).thenReturn(TOKEN);
                    when(jwtUtil.extractUsername(TOKEN)).thenReturn("operator001");
                    when(cancelRequestService.processDecision(CANCEL_REQUEST_ID, request, "operator001"))
                            .thenReturn(mockResponse);
                    mockedUtil.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

                    // Act
                    ResponseEntity<CancelRequestDecisionResponse> response =
                            cancelRequestController.makeDecision(authentication, CANCEL_REQUEST_ID, request);

                    // Assert
                    assertThat(response).isNotNull();
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isEqualTo(mockResponse);

                    verify(jwtUtil, times(1)).extractUsername(TOKEN);
                    verify(cancelRequestService, times(1))
                            .processDecision(CANCEL_REQUEST_ID, request, "operator001");
                    mockedUtil.verify(() -> ResponseUtil.getHttpStatus("200"), times(1));

                    // CATATAN: pembuatan相殺レコード（offset record）dan registrasi ke NoSQL
                    // adalah tanggung jawab cancelRequestService — TIDAK bisa dan TIDAK perlu
                    // dibuktikan di level controller test ini. Itu masuk ke cakupan unit test
                    // CancelRequestServiceImpl (mock repository NoSQL-nya di sana).
                }
            }

            @Test
            @DisplayName("SC-2: 却下決定が成功した場合、200OK レスポンスを返す")
            void makeDecision_reject_returnsOkResponse() {
                // Arrange
                CancelRequestDecisionRequest request = new CancelRequestDecisionRequest();
                // TODO: request.setDecision("REJECT")

                CancelRequestDecisionResponse mockResponse = new CancelRequestDecisionResponse();
                mockResponse.setStatusCode("200");
                mockResponse.setMessage("取消申請を却下しました");

                try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                    when(authentication.getCredentials()).thenReturn(TOKEN);
                    when(jwtUtil.extractUsername(TOKEN)).thenReturn("operator001");
                    when(cancelRequestService.processDecision(CANCEL_REQUEST_ID, request, "operator001"))
                            .thenReturn(mockResponse);
                    mockedUtil.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

                    // Act
                    ResponseEntity<CancelRequestDecisionResponse> response =
                            cancelRequestController.makeDecision(authentication, CANCEL_REQUEST_ID, request);

                    // Assert
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isEqualTo(mockResponse);
                    verify(cancelRequestService, times(1))
                            .processDecision(CANCEL_REQUEST_ID, request, "operator001");
                }
            }
        }

        @Nested
        @DisplayName("【異常系】")
        class ErrorCase {

            @Test
            @DisplayName("EC-1: cancelRequestId が存在しない場合、404 NOT_FOUND レスポンスを返す")
            void makeDecision_cancelRequestNotFound_returnsNotFoundResponse() {
                // Arrange
                Integer unknownId = 999;
                CancelRequestDecisionRequest request = new CancelRequestDecisionRequest();

                CancelRequestDecisionResponse mockResponse = new CancelRequestDecisionResponse();
                mockResponse.setStatusCode("404");
                mockResponse.setMessage("取消申請が見つかりません");

                try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                    when(authentication.getCredentials()).thenReturn(TOKEN);
                    when(jwtUtil.extractUsername(TOKEN)).thenReturn("operator001");
                    when(cancelRequestService.processDecision(unknownId, request, "operator001"))
                            .thenReturn(mockResponse);
                    mockedUtil.when(() -> ResponseUtil.getHttpStatus("404")).thenReturn(HttpStatus.NOT_FOUND);

                    // Act
                    ResponseEntity<CancelRequestDecisionResponse> response =
                            cancelRequestController.makeDecision(authentication, unknownId, request);

                    // Assert
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(response.getBody()).isEqualTo(mockResponse);
                }
            }

            @Test
            @DisplayName("EC-2: トークンが無効で extractUsername が例外をスローする場合、サービスは呼ばれない")
            void makeDecision_extractUsernameThrows_skipsService() {
                // Arrange
                CancelRequestDecisionRequest request = new CancelRequestDecisionRequest();
                CustomException expectedException =
                        new CustomException("E004", "トークンが無効です");

                try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                    when(authentication.getCredentials()).thenReturn("");
                    when(jwtUtil.extractUsername("")).thenThrow(expectedException);

                    // Act & Assert
                    CustomException thrown = assertThrows(CustomException.class,
                            () -> cancelRequestController.makeDecision(authentication, CANCEL_REQUEST_ID, request));

                    assertThat(thrown.getMessage()).isEqualTo("トークンが無効です");

                    verify(jwtUtil, times(1)).extractUsername("");
                    verifyNoInteractions(cancelRequestService);
                    mockedUtil.verify(() -> ResponseUtil.getHttpStatus(any()), never());
                }
            }

            @Test
            @DisplayName("EC-3: サービスが CustomException をスローする場合（例: 既に決定済み）、例外が伝播される")
            void makeDecision_serviceThrowsCustomException_propagatesException() {
                // Arrange
                CancelRequestDecisionRequest request = new CancelRequestDecisionRequest();
                CustomException expectedException =
                        new CustomException("E006", "この取消申請は既に処理済みです");

                try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                    when(authentication.getCredentials()).thenReturn(TOKEN);
                    when(jwtUtil.extractUsername(TOKEN)).thenReturn("operator001");
                    when(cancelRequestService.processDecision(CANCEL_REQUEST_ID, request, "operator001"))
                            .thenThrow(expectedException);

                    // Act & Assert
                    CustomException thrown = assertThrows(CustomException.class,
                            () -> cancelRequestController.makeDecision(authentication, CANCEL_REQUEST_ID, request));

                    assertThat(thrown.getMessage()).isEqualTo("この取消申請は既に処理済みです");

                    verify(cancelRequestService, times(1))
                            .processDecision(CANCEL_REQUEST_ID, request, "operator001");
                    mockedUtil.verify(() -> ResponseUtil.getHttpStatus(any()), never());
                }
            }

            @Test
            @DisplayName("EC-4: NoSQL への相殺レコード登録が失敗した場合、サービス例外がそのまま伝播される")
            void makeDecision_offsetRecordRegistrationFails_propagatesException() {
                // Arrange
                // Skenario ini mensimulasikan kegagalan di lapisan service (DynamoDB down, dst)
                // saat approval — dari sudut pandang controller, perilakunya identik dengan EC-3:
                // exception apapun dari service harus lolos apa adanya.
                CancelRequestDecisionRequest request = new CancelRequestDecisionRequest();
                CustomException expectedException =
                        new CustomException("E007", "相殺レコードの登録に失敗しました");

                try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                    when(authentication.getCredentials()).thenReturn(TOKEN);
                    when(jwtUtil.extractUsername(TOKEN)).thenReturn("operator001");
                    when(cancelRequestService.processDecision(CANCEL_REQUEST_ID, request, "operator001"))
                            .thenThrow(expectedException);

                    // Act & Assert
                    CustomException thrown = assertThrows(CustomException.class,
                            () -> cancelRequestController.makeDecision(authentication, CANCEL_REQUEST_ID, request));

                    assertThat(thrown.getMessage()).isEqualTo("相殺レコードの登録に失敗しました");
                    mockedUtil.verify(() -> ResponseUtil.getHttpStatus(any()), never());
                }
            }
        }
    }
}
