package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.impl.NotificationServiceImpl;
import com.kct.jcredit.core_backend.repository.postgres.NotificationRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.dto.request.InquiryRequest;
import com.kct.jcredit.core_backend.dto.response.NotificationListResponse;
import com.kct.jcredit.core_backend.dto.response.NotificationCountResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.util.JwtUtil;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.kct.jcredit.core_backend.entity.Notification;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.constant.ResponseConstants;

import com.kct.jcredit.core_backend.util.ResponseUtil;

//import com.kct.jcredit.core_backend.dto.response.BaseResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * NotificationController / NotificationServiceImpl 単体試験 アウトライン
 *
 * 試験項目書対応表:
 *   UT-NC-001 ~ UT-NC-010 : NotificationController
 *   UT-NS-001 ~ UT-NS-018 : NotificationServiceImpl
 *
 * 各テストメソッドは未実装（TODO）。1つずつ実装していくこと。
 */
class NotificationTest {

    // ============================================================================
    // NotificationController のテスト
    // ============================================================================
    @ExtendWith(MockitoExtension.class)
    @Nested
    @DisplayName("NotificationController のテスト")
    class NotificationControllerTest {

        @Mock
        private NotificationServiceImpl notificationService;

        @Mock
        private JwtUtil jwtUtil;

        @Mock
        private Authentication authentication;

        @InjectMocks
        private NotificationController notificationController;

        @Autowired
        private MockMvc mockMvc;


        private static final String TOKEN = "dummy-jwt-token";

        @BeforeEach
        void setUp() {
            lenient().when(authentication.getCredentials()).thenReturn(TOKEN);
        }

        // ------------------------------------------------------------------------
        // GET /notification/list
        // ------------------------------------------------------------------------
        @Nested
        @DisplayName("GET /notification/list のテスト")
        class GetNotificationsTest {

        @Test
            @DisplayName("UT-NC-001: 参加企業ユーザーが通知一覧を取得し、Serviceの結果がそのまま返る")
            void getNotifications_participantUser_returnsServiceResponseAsIs() {
            // Input: userId=1, unreadOnly=false, page=0, size=10, isOperatingUser=false
                Integer userId = 1;
                Boolean unreadOnly = false;
                Integer page = 0;
                Integer size = 10;
                boolean isOperatingUser = false;

            
                when(jwtUtil.extractUserId(TOKEN)).thenReturn(userId);
                when(jwtUtil.extractRole(TOKEN)).thenReturn("participant");

                NotificationListResponse mockResponse = NotificationListResponse.builder()
                        .notifications(List.of())
                        .pagination(null)
                        .build();
                mockResponse.setStatusCode(ResponseConstants.OK);
                mockResponse.setMessage("Success");

                when(notificationService.getNotifications(
                        eq(userId), eq(unreadOnly), eq(page), eq(size), eq(isOperatingUser)))
                        .thenReturn(mockResponse);

                // Execute
                ResponseEntity<NotificationListResponse> response =
                        notificationController.getNotifications(authentication, unreadOnly, page, size);

                // Output: HTTPステータス200、レスポンスボディがServiceの戻り値と一致すること
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isEqualTo(mockResponse);

                // isOperatingUser=falseがgetNotificationsに渡っていること
                verify(notificationService, times(1))
                        .getNotifications(eq(userId), eq(unreadOnly), eq(page), eq(size), eq(isOperatingUser));
                verify(jwtUtil, times(1)).extractUserId(TOKEN);
                verify(jwtUtil, times(1)).extractRole(TOKEN);
            }

        @Test
        @DisplayName("UT-NC-002: オペレーターユーザーでServiceがエラー系statusCode（NOT_FOUND等）を返した場合のステータス変換確認")
        void getNotifications_operatorUserServiceError_convertsHttpStatus() {
            // Input: userId=1, unreadOnly=true, page=0, size=10, isOperatingUser=true
            Integer userId = 1;
            Boolean unreadOnly = true;
            Integer page = 0;
            Integer size = 10;
            boolean isOperatingUser = true;

            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(userId);
            // ※ isOperatingUser=true になるroleの値に合わせて修正してください（例: "operator", "OPERATING_USER" など）
            when(jwtUtil.extractRole(TOKEN)).thenReturn("operator");

            // Serviceがエラーレスポンス(NOT_FOUND)を返すようモック
            NotificationListResponse mockErrorResponse = NotificationListResponse.builder()
                    .notifications(null)
                    .pagination(null)
                    .build();
            mockErrorResponse.setStatusCode(ResponseConstants.NOT_FOUND);
            mockErrorResponse.setMessage(ResponseConstants.MSG_NOT_FOUND);

            when(notificationService.getNotifications(
                    eq(userId), eq(unreadOnly), eq(page), eq(size), eq(isOperatingUser)))
                    .thenReturn(mockErrorResponse);

            // Execute
            ResponseEntity<NotificationListResponse> response =
                    notificationController.getNotifications(authentication, unreadOnly, page, size);

            // Output: ResponseUtil.getHttpStatusで変換されたエラーステータスとエラーボディが返却されること
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isEqualTo(mockErrorResponse);
            assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getBody().getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);

            // isOperatingUser=trueがServiceに渡っていること
            verify(notificationService, times(1))
                    .getNotifications(eq(userId), eq(unreadOnly), eq(page), eq(size), eq(isOperatingUser));
            verify(jwtUtil, times(1)).extractUserId(TOKEN);
            verify(jwtUtil, times(1)).extractRole(TOKEN);
         }
        }

        // ------------------------------------------------------------------------
        // GET /notification/count
        // ------------------------------------------------------------------------
        @Nested
        @DisplayName("GET /notification/count のテスト")
        class GetNotificationCountTest {

            @Test
            @DisplayName("UT-NC-003: 参加企業ユーザーの未読数取得結果がそのまま返る")
            void getNotificationCount_participantUser_returnsServiceResponseAsIs() {
                // Input: userId=1, isOperatingUser=false
                Integer userId = 1;
                boolean isOperatingUser = false;

                when(authentication.getCredentials()).thenReturn(TOKEN);
                when(jwtUtil.extractUserId(TOKEN)).thenReturn(userId);
                when(jwtUtil.extractRole(TOKEN)).thenReturn("participant");

                NotificationCountResponse mockResponse = NotificationCountResponse.builder()
                        .unreadCount(5L)
                        .build();
                mockResponse.setStatusCode(ResponseConstants.OK);
                mockResponse.setMessage("Success");

                when(notificationService.getNotificationCount(eq(userId), eq(isOperatingUser)))
                        .thenReturn(mockResponse);

                // Execute
                ResponseEntity<NotificationCountResponse> response =
                        notificationController.getNotificationCount(authentication);

                // Output: HTTPステータス200、レスポンスボディがServiceの戻り値と一致すること
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isEqualTo(mockResponse);

                // isOperatingUser=falseがgetNotificationCountに渡っていること
                verify(notificationService, times(1))
                        .getNotificationCount(eq(userId), eq(isOperatingUser));
                verify(jwtUtil, times(1)).extractUserId(TOKEN);
                verify(jwtUtil, times(1)).extractRole(TOKEN);
            }

            @Test
            @DisplayName("UT-NC-004: オペレーターユーザーでServiceがエラーレスポンスを返した場合のステータス変換確認")
            void getNotificationCount_operatorUserServiceError_convertsHttpStatus() {
                // Input: userId=1, isOperatingUser=true
                Integer userId = 1;
                boolean isOperatingUser = true;

                when(authentication.getCredentials()).thenReturn(TOKEN);
                when(jwtUtil.extractUserId(TOKEN)).thenReturn(userId);
                // ※ isOperatingUser=true になるroleの値に合わせて修正してください
                when(jwtUtil.extractRole(TOKEN)).thenReturn("operator");

                // Serviceがエラーレスポンス(NOT_FOUND)を返すようモック
                NotificationCountResponse mockErrorResponse = NotificationCountResponse.builder()
                        .unreadCount(null)
                        .build();
                mockErrorResponse.setStatusCode(ResponseConstants.NOT_FOUND);
                mockErrorResponse.setMessage(ResponseConstants.MSG_NOT_FOUND);

                when(notificationService.getNotificationCount(eq(userId), eq(isOperatingUser)))
                        .thenReturn(mockErrorResponse);

                // Execute
                ResponseEntity<NotificationCountResponse> response =
                        notificationController.getNotificationCount(authentication);

                // Output: ResponseUtil.getHttpStatusで変換されたエラーステータスとエラーボディが返却されること
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(response.getBody()).isEqualTo(mockErrorResponse);
                assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
                assertThat(response.getBody().getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);

                // isOperatingUser=trueがServiceに渡っていること
                verify(notificationService, times(1))
                        .getNotificationCount(eq(userId), eq(isOperatingUser));
                verify(jwtUtil, times(1)).extractUserId(TOKEN);
                verify(jwtUtil, times(1)).extractRole(TOKEN);
            }
        }

        // ------------------------------------------------------------------------
        // PATCH /notification/{notificationId}/read
        // ------------------------------------------------------------------------
        @Nested
        @DisplayName("PATCH /notification/{notificationId}/read のテスト")
        class ReadNotificationTest {

            @Test
            @DisplayName("UT-NC-005: 参加企業ユーザーが自身の通知を既読化しServiceの成功結果が返る")
            void readNotification_participantUserOwnNotification_returnsServiceResponseAsIs() {
                // Input: userId=1, notificationId=1, isOperatingUser=false
                Integer userId = 1;
                Integer notificationId = 1;
                boolean isOperatingUser = false;

                when(authentication.getCredentials()).thenReturn(TOKEN);
                when(jwtUtil.extractUserId(TOKEN)).thenReturn(userId);
                when(jwtUtil.extractRole(TOKEN)).thenReturn("participant");

                NotificationListResponse mockResponse = NotificationListResponse.builder()
                        .pagination(null)
                        .build();
                mockResponse.setStatusCode(ResponseConstants.OK);
                mockResponse.setMessage("Success");

                when(notificationService.readNotification(eq(userId), eq(notificationId), eq(isOperatingUser)))
                        .thenReturn(mockResponse);

                // Execute
                ResponseEntity<NotificationListResponse> response =
                        notificationController.readNotification(authentication, notificationId);

                // Output: HTTPステータス200、レスポンスボディがServiceの戻り値と一致すること
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isEqualTo(mockResponse);

                // userId・notificationId・isOperatingUser=falseがreadNotificationに渡っていること
                verify(notificationService, times(1))
                        .readNotification(eq(userId), eq(notificationId), eq(isOperatingUser));
                verify(jwtUtil, times(1)).extractUserId(TOKEN);
                verify(jwtUtil, times(1)).extractRole(TOKEN);
            }

            @Test
            @DisplayName("UT-NC-006: ServiceがNOT_FOUNDレスポンスを返した場合のステータス変換確認")
            void readNotification_serviceReturnsNotFound_convertsHttpStatus404() {
                // Input: userId=1, notificationId=99999
                Integer userId = 1;
                Integer notificationId = 99999;
                boolean isOperatingUser = false;

                when(authentication.getCredentials()).thenReturn(TOKEN);
                when(jwtUtil.extractUserId(TOKEN)).thenReturn(userId);
                when(jwtUtil.extractRole(TOKEN)).thenReturn("participant");

                // Serviceがstatus=NOT_FOUNDを返すようモック
                NotificationListResponse mockErrorResponse = NotificationListResponse.builder()
                        .notifications(null)
                        .pagination(null)
                        .build();
                mockErrorResponse.setStatusCode(ResponseConstants.NOT_FOUND);
                mockErrorResponse.setMessage(ResponseConstants.MSG_NOT_FOUND);

                when(notificationService.readNotification(eq(userId), eq(notificationId), eq(isOperatingUser)))
                        .thenReturn(mockErrorResponse);

                // Execute
                ResponseEntity<NotificationListResponse> response =
                        notificationController.readNotification(authentication, notificationId);

                // Output: HTTPステータス404、statusCode=NOT_FOUNDのレスポンスが返却されること
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(response.getBody()).isEqualTo(mockErrorResponse);
                assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
                assertThat(response.getBody().getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);

                verify(notificationService, times(1))
                        .readNotification(eq(userId), eq(notificationId), eq(isOperatingUser));
                verify(jwtUtil, times(1)).extractUserId(TOKEN);
                verify(jwtUtil, times(1)).extractRole(TOKEN);
            }
        }

        // ------------------------------------------------------------------------
        // PATCH /notification/{notificationId}/delete
        // ------------------------------------------------------------------------
        @Nested
        @DisplayName("PATCH /notification/{notificationId}/delete のテスト")
        class DeleteNotificationTest {

            @Test
            @DisplayName("UT-NC-007: 参加企業ユーザーが自身の通知を削除しServiceの成功結果が返る")
            void deleteNotification_participantUserOwnNotification_returnsServiceResponseAsIs() {
                // Input: userId=1, notificationId=1, isOperatingUser=false
                Integer userId = 1;
                Integer notificationId = 1;
                boolean isOperatingUser = false;

                when(authentication.getCredentials()).thenReturn(TOKEN);
                when(jwtUtil.extractUserId(TOKEN)).thenReturn(userId);
                when(jwtUtil.extractRole(TOKEN)).thenReturn("participant");

                NotificationListResponse mockResponse = NotificationListResponse.builder()
                        .notifications(List.of())
                        .pagination(null)
                        .build();
                mockResponse.setStatusCode(ResponseConstants.OK);
                mockResponse.setMessage("Success");

                when(notificationService.deleteNotification(eq(userId), eq(notificationId), eq(isOperatingUser)))
                        .thenReturn(mockResponse);

                // Execute
                ResponseEntity<NotificationListResponse> response =
                        notificationController.deleteNotification(authentication, notificationId);

                // Output: HTTPステータス200、レスポンスボディがServiceの戻り値と一致すること
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isEqualTo(mockResponse);

                // userId・notificationId・isOperatingUser=falseがdeleteNotificationに渡っていること
                verify(notificationService, times(1))
                        .deleteNotification(eq(userId), eq(notificationId), eq(isOperatingUser));
                verify(jwtUtil, times(1)).extractUserId(TOKEN);
                verify(jwtUtil, times(1)).extractRole(TOKEN);
            }

            @Test
            @DisplayName("UT-NC-008: ServiceがNOT_FOUNDレスポンスを返した場合のステータス変換確認")
            void deleteNotification_serviceReturnsNotFound_convertsHttpStatus404() {
                // Input: userId=1, notificationId=99999, isOperatingUser=true
                Integer userId = 1;
                Integer notificationId = 99999;
                boolean isOperatingUser = true;

                when(authentication.getCredentials()).thenReturn(TOKEN);
                when(jwtUtil.extractUserId(TOKEN)).thenReturn(userId);
                when(jwtUtil.extractRole(TOKEN)).thenReturn("operator");

                // Serviceがstatus=NOT_FOUNDを返すようモック
                NotificationListResponse mockErrorResponse = NotificationListResponse.builder()
                        .notifications(null)
                        .pagination(null)
                        .build();
                mockErrorResponse.setStatusCode(ResponseConstants.NOT_FOUND);
                mockErrorResponse.setMessage(ResponseConstants.MSG_NOT_FOUND);

                when(notificationService.deleteNotification(eq(userId), eq(notificationId), eq(isOperatingUser)))
                        .thenReturn(mockErrorResponse);

                // Execute
                ResponseEntity<NotificationListResponse> response =
                        notificationController.deleteNotification(authentication, notificationId);

                // Output: HTTPステータス404、statusCode=NOT_FOUNDのレスポンスが返却されること
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(response.getBody()).isEqualTo(mockErrorResponse);
                assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
                assertThat(response.getBody().getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);

                // isOperatingUser=trueがdeleteNotificationに渡っていること
                verify(notificationService, times(1))
                        .deleteNotification(eq(userId), eq(notificationId), eq(isOperatingUser));
                verify(jwtUtil, times(1)).extractUserId(TOKEN);
                verify(jwtUtil, times(1)).extractRole(TOKEN);
            }
        }

        // ------------------------------------------------------------------------
        // POST /notification/inquiry
        // ------------------------------------------------------------------------
        @Nested
        @DisplayName("POST /notification/inquiry のテスト")
        class CreateInquiryTest {
                @BeforeEach
                void setUpMockMvc() {
                    mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();
                }

            @Test
            @DisplayName("UT-NC-009: 有効なリクエストで問い合わせ受付しServiceの成功結果が返る")
            void createInquiry_validRequest_returnsServiceSuccessResponse() {
                // Input: request: email="test@example.com", companyName="テスト株式会社",
                //        name="山田太郎", content="問い合わせ内容"
                InquiryRequest request = new InquiryRequest();
                    request.setEmail("test@example.com");
                    request.setCompanyName("テスト株式会社");
                    request.setName("山田太郎");
                    request.setContent("問い合わせ内容");

                BaseResponse mockResponse = new BaseResponse();
                mockResponse.setStatusCode(ResponseConstants.OK);
                mockResponse.setMessage("Success");

                when(notificationService.createInquiry(any(InquiryRequest.class)))
                        .thenReturn(mockResponse);

                // Execute
                ResponseEntity<BaseResponse> response =
                        notificationController.createInquiry(request);

                // Output: HTTPステータス200、statusCode=OKのBaseResponseが返却されること
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isEqualTo(mockResponse);
                assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.OK);

                // createInquiryにリクエスト内容が正しく渡っていること
                ArgumentCaptor<InquiryRequest> captor = ArgumentCaptor.forClass(InquiryRequest.class);
                verify(notificationService, times(1)).createInquiry(captor.capture());
                InquiryRequest capturedRequest = captor.getValue();
                assertThat(capturedRequest.getEmail()).isEqualTo("test@example.com");
                assertThat(capturedRequest.getCompanyName()).isEqualTo("テスト株式会社");
                assertThat(capturedRequest.getName()).isEqualTo("山田太郎");
                assertThat(capturedRequest.getContent()).isEqualTo("問い合わせ内容");
            }

            @Test
            @DisplayName("UT-NC-010: 必須項目（email）が不正/欠落し@Validでバリデーションエラーとなる")
            void createInquiry_invalidEmail_returnsValidationError() throws Exception {
                // Input: request: email="invalid-email", companyName="テスト株式会社",
                //        name="山田太郎", content="問い合わせ内容"
                String requestBody = """
                        {
                            "email": "invalid-email",
                            "companyName": "テスト株式会社",
                            "name": "山田太郎",
                            "content": "問い合わせ内容"
                        }
                        """;

                // Execute & Output: HTTPステータス400、バリデーションエラーレスポンスが返却されること
                mockMvc.perform(post("/api/notifications/inquiry")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andExpect(status().isBadRequest());

                // Serviceのcreate Inquiryが呼ばれないこと
                verify(notificationService, never()).createInquiry(any(InquiryRequest.class));
            }
        }
    }


    // ============================================================================
    // NotificationServiceImpl のテスト
    // ============================================================================
    @ExtendWith(MockitoExtension.class)
    @Nested
    @DisplayName("NotificationServiceImpl のテスト")
    class NotificationServiceImplTest {

        @Mock
        private NotificationRepository notificationRepository;

        @Mock
        private OperatingUserRepository operatingUserRepository;

        @Mock
        private JavaMailSender mailSender;

        @InjectMocks
        private NotificationServiceImpl notificationService;

        @BeforeEach
        void setUp() {
            // TODO: 共通フィクスチャ（Notification、ParticipatingUser、OperatingUser等）をここに設定
        }

        // ------------------------------------------------------------------------
        // getNotifications
        // ------------------------------------------------------------------------
        @Nested
        @DisplayName("getNotifications のテスト")
        class GetNotificationsServiceTest {

            @Test
            @DisplayName("UT-NS-001: 参加企業ユーザー・unreadOnly=falseで全件取得しDTO変換・ページネーションが正しく組み立てられる")
            void getNotifications_participantAllNotifications_buildsCorrectPagination() {
                // Input: userId=1, unreadOnly=false, page=0, size=10, isOperatingUser=false
                int userId = 1;
                boolean unreadOnly = false;
                int page = 0;
                int size = 10;
                boolean isOperatingUser = false;

                Notification notification1 = Notification.builder()
                        
                        .title("通知1")
                        .message("内容1")
                        .read(false)
                        .build();
                Notification notification2 = Notification.builder() 
                        .title("通知2")
                        .message("内容2")
                        .read(true)
                        .build();

                notification1.setId(1);
                notification2.setId(2);
                List<Notification> notificationList = List.of(notification1, notification2);
                Pageable pageable = PageRequest.of(page, size);
                Page<Notification> notificationPage = new PageImpl<>(notificationList, pageable, 2);

                // findByParticipatingUserIdAndIsActiveが2件を含むPageを返す
                when(notificationRepository.findByParticipatingUserIdAndIsActive(eq(userId), any(Pageable.class)))
                        .thenReturn(notificationPage);

                // Execute
                NotificationListResponse response = notificationService.getNotifications(
                        userId, unreadOnly, page, size, isOperatingUser);

                // Output: statusCode=OK
                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

                // notifications件数2
                assertThat(response.getNotifications()).hasSize(2);

                // pagination（page=0,size=10,total=2,totalPages=1）が設定される
                assertThat(response.getPagination().getPage()).isEqualTo(0);
                assertThat(response.getPagination().getLimit()).isEqualTo(10);
                assertThat(response.getPagination().getTotalItems()).isEqualTo(2);
                assertThat(response.getPagination().getTotalPages()).isEqualTo(1);

                // findByParticipatingUserIdAndIsActiveが呼ばれること
                verify(notificationRepository).findByParticipatingUserIdAndIsActive(eq(userId), any(Pageable.class));
            }

            @Test
            @DisplayName("UT-NS-002: 参加企業ユーザー・unreadOnly=trueで未読専用メソッドが呼ばれる")
            void getNotifications_participantUnreadOnly_callsUnreadMethod() {
                // Input: userId=1, unreadOnly=true, page=0, size=10, isOperatingUser=false
                int userId = 1;
                boolean unreadOnly = true;
                int page = 0;
                int size = 10;
                boolean isOperatingUser = false;

                Notification notification1 = Notification.builder()
                        .title("通知1")
                        .message("内容1")
                        .read(false)
                        .build();
                notification1.setId(1);

                List<Notification> notificationList = List.of(notification1);
                Pageable pageable = PageRequest.of(page, size);
                Page<Notification> notificationPage = new PageImpl<>(notificationList, pageable, 1);

                when(notificationRepository.findByParticipatingUserIdAndReadAndIsActive(eq(userId), eq(false), any(Pageable.class)))
                        .thenReturn(notificationPage);

                // Execute
                NotificationListResponse response = notificationService.getNotifications(
                        userId, unreadOnly, page, size, isOperatingUser);

                // Output: 未読通知のみのレスポンスが返る
                assertThat(response.getNotifications()).hasSize(1);
                assertThat(response.getNotifications().get(0).getRead()).isFalse();

                // findByParticipatingUserIdAndReadAndIsActive(userId, false, pageable)が呼ばれていること
                verify(notificationRepository).findByParticipatingUserIdAndReadAndIsActive(eq(userId), eq(false), any(Pageable.class));
            }

            @Test
            @DisplayName("UT-NS-003: 運営ユーザー・unreadOnly=false/trueで運営用メソッドに呼び分けられる")
            void getNotifications_operatingUser_callsCorrectMethodForBothUnreadOnlyValues() {
                // Input: userId=1, unreadOnly=false→true, page=0, size=10, isOperatingUser=true（2パターン）
                int userId = 1;
                int page = 0;
                int size = 10;
                boolean isOperatingUser = true;

                Notification notification1 = Notification.builder()
                        .title("運営通知1")
                        .message("内容1")
                        .read(false)
                        .build();
                notification1.setId(1);

                List<Notification> notificationList = List.of(notification1);
                Pageable pageable = PageRequest.of(page, size);
                Page<Notification> notificationPage = new PageImpl<>(notificationList, pageable, 1);

                // --- パターン1: unreadOnly=false ---
                when(notificationRepository.findByOperatingUserIdAndIsActive(eq(userId), any(Pageable.class)))
                        .thenReturn(notificationPage);

                NotificationListResponse responseAll = notificationService.getNotifications(
                        userId, false, page, size, isOperatingUser);

                // Output: 運営ユーザー向けの通知が返る
                assertThat(responseAll.getNotifications()).hasSize(1);

                // findByOperatingUserIdAndIsActiveが呼ばれていること
                verify(notificationRepository).findByOperatingUserIdAndIsActive(eq(userId), any(Pageable.class));

                // --- パターン2: unreadOnly=true ---
                when(notificationRepository.findByOperatingUserIdAndReadAndIsActive(eq(userId), eq(false), any(Pageable.class)))
                        .thenReturn(notificationPage);

                NotificationListResponse responseUnread = notificationService.getNotifications(
                        userId, true, page, size, isOperatingUser);

                // Output: 運営ユーザー向けの未読通知が返る
                assertThat(responseUnread.getNotifications()).hasSize(1);

                // findByOperatingUserIdAndReadAndIsActiveが呼ばれていること
                verify(notificationRepository).findByOperatingUserIdAndReadAndIsActive(eq(userId), eq(false), any(Pageable.class));
            }

            @Test
            @DisplayName("UT-NS-004: 該当データが0件でも例外なく正常レスポンスが返る")
            void getNotifications_emptyResult_returnsSuccessWithEmptyList() {
                // Input: userId=1, unreadOnly=false, page=0, size=10（Repositoryが空のPageを返す）
                int userId = 1;
                boolean unreadOnly = false;
                int page = 0;
                int size = 10;
                boolean isOperatingUser = false;

                Pageable pageable = PageRequest.of(page, size);
                Page<Notification> emptyPage = new PageImpl<>(List.of(), pageable, 0);

                when(notificationRepository.findByParticipatingUserIdAndIsActive(eq(userId), any(Pageable.class)))
                        .thenReturn(emptyPage);

                // Execute
                NotificationListResponse response = notificationService.getNotifications(
                        userId, unreadOnly, page, size, isOperatingUser);

                // Output: statusCode=OK
                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

                // notifications=空リスト
                assertThat(response.getNotifications()).isEmpty();

                // pagination.total=0
                assertThat(response.getPagination().getTotalItems()).isEqualTo(0);
                assertThat(response.getPagination().getTotalPages()).isEqualTo(0);
                assertThat(response.getPagination().getPage()).isEqualTo(0);
                assertThat(response.getPagination().getLimit()).isEqualTo(10);

                // 空リスト時に例外がスローされないこと（このアサーション自体が例外なく到達したことの証明）
                assertThatCode(() -> notificationService.getNotifications(userId, unreadOnly, page, size, isOperatingUser))
                        .doesNotThrowAnyException();

                verify(notificationRepository, atLeastOnce()).findByParticipatingUserIdAndIsActive(eq(userId), any(Pageable.class));
            }
        }

        // ------------------------------------------------------------------------
        // getNotificationCount
        // ------------------------------------------------------------------------
        @Nested
        @DisplayName("getNotificationCount のテスト")
        class GetNotificationCountServiceTest {

            @Test
            @DisplayName("UT-NS-005: 参加企業ユーザーの未読数取得（countUnreadByParticipatingUserIdが呼ばれる）")
            void getNotificationCount_participantUser_callsParticipatingCountMethod() {
                // Input: userId=1, isOperatingUser=false（countUnreadByParticipatingUserIdが3を返す）
                int userId = 1;
                boolean isOperatingUser = false;

                when(notificationRepository.countUnreadByParticipatingUserId(eq(userId)))
                        .thenReturn(3L);

                // Execute
                NotificationCountResponse response = notificationService.getNotificationCount(userId, isOperatingUser);

                // Output: statusCode=OK
                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

                // unreadCount=3
                assertThat(response.getUnreadCount()).isEqualTo(3);

                // countUnreadByParticipatingUserIdが呼ばれること
                verify(notificationRepository).countUnreadByParticipatingUserId(eq(userId));

                // countUnreadByOperatingUserIdが呼ばれないこと
                verify(notificationRepository, never()).countUnreadByOperatingUserId(anyInt());
            }

            @Test
            @DisplayName("UT-NS-006: 運営ユーザーの未読数取得（countUnreadByOperatingUserIdが呼ばれる：分岐カバレッジ）")
            void getNotificationCount_operatingUser_callsOperatingCountMethod() {
                // Input: userId=1, isOperatingUser=true（countUnreadByOperatingUserIdが0を返す）
                int userId = 1;
                boolean isOperatingUser = true;

                when(notificationRepository.countUnreadByOperatingUserId(eq(userId)))
                        .thenReturn(0L);

                // Execute
                NotificationCountResponse response = notificationService.getNotificationCount(userId, isOperatingUser);

                // Output: statusCode=OK
                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

                // unreadCount=0
                assertThat(response.getUnreadCount()).isEqualTo(0);

                // countUnreadByOperatingUserIdが呼ばれること
                verify(notificationRepository).countUnreadByOperatingUserId(eq(userId));

                // countUnreadByParticipatingUserIdが呼ばれないこと
                verify(notificationRepository, never()).countUnreadByParticipatingUserId(anyInt());
            }
        }

        // ------------------------------------------------------------------------
        // readNotification
        // ------------------------------------------------------------------------
        @Nested
        @DisplayName("readNotification のテスト")
        class ReadNotificationServiceTest {

            @Test
            @DisplayName("UT-NS-007: 未読の自身の通知を既読化（markAsRead＋saveが呼ばれる）")
            void readNotification_ownUnreadNotification_marksAsReadAndSaves() {
                // Input: userId=1, notificationId=1, isOperatingUser=false
                //        （該当NotificationはisActive=true, read=false, participatingUser.id=1）
                int userId = 1;
                int notificationId = 1;
                boolean isOperatingUser = false;

                ParticipatingUser participatingUser = new ParticipatingUser();
                participatingUser.setId(userId);

                Notification notification = Notification.builder()
                        .title("通知1")
                        .message("内容1")
                        .read(false)
                        .participatingUser(participatingUser)
                        .build();
                notification.setId(notificationId);
                notification.setIsActive(true);

                when(notificationRepository.findById(eq(notificationId)))
                        .thenReturn(Optional.of(notification));

                when(notificationRepository.save(any(Notification.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));

                // Execute
                NotificationListResponse response = notificationService.readNotification(
                        userId, notificationId, isOperatingUser);

                // Output: statusCode=OK
                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

                // 返却DTOのreadがtrueに対応する
                assertThat(response.getNotifications().get(0).getRead()).isTrue();

                // markAsReadが呼ばれること（Notificationエンティティ自身のメソッドとして呼ばれる想定）
                assertThat(notification.getRead()).isTrue();

                // notificationRepository.saveが呼ばれること
                verify(notificationRepository).save(eq(notification));
            }

            @Test
            @DisplayName("UT-NS-008: 既読済みの通知を指定した場合、save処理を行わず冪等に成功レスポンスが返る")
            void readNotification_alreadyReadNotification_idempotentNoSave() {
                // Input: userId=1, notificationId=1, isOperatingUser=false（該当Notificationはread=true）
                int userId = 1;
                int notificationId = 1;
                boolean isOperatingUser = false;

                ParticipatingUser participatingUser = new ParticipatingUser();
                participatingUser.setId(userId);

                Notification notification = Notification.builder()
                        .title("通知1")
                        .message("内容1")
                        .read(true)
                        .participatingUser(participatingUser)
                        .build();
                notification.setId(notificationId);
                notification.setIsActive(true);

                when(notificationRepository.findById(eq(notificationId)))
                        .thenReturn(Optional.of(notification));

                // Execute
                NotificationListResponse response = notificationService.readNotification(
                        userId, notificationId, isOperatingUser);

                // Output: statusCode=OKが返る
                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

                // notificationRepository.saveが呼ばれないこと（既に既読のため冪等に処理される）
                verify(notificationRepository, never()).save(any(Notification.class));
            }

            @ParameterizedTest(name = "notificationId={0}のケース: {1}")
            @DisplayName("UT-NS-009: notificationIdが存在しない、または論理削除済み（isActive=false）の場合404相当を返す")
            @MethodSource("notFoundOrInactiveCases")
            void readNotification_notFoundOrInactive_returnsNotFound(
                    int notificationId, String caseName, Notification mockedNotification) {
                // Input: userId=1, notificationId=999 または 1, isOperatingUser=false
                int userId = 1;
                boolean isOperatingUser = false;

                when(notificationRepository.findById(eq(notificationId)))
                        .thenReturn(Optional.ofNullable(mockedNotification));

                // Execute
                NotificationListResponse response = notificationService.readNotification(
                        userId, notificationId, isOperatingUser);

                // Output: statusCode=NOT_FOUND
                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);

                // message="Notification not found"
                assertThat(response.getMessage()).isEqualTo("Notification not found");

                // notificationRepository.saveが呼ばれないこと
                verify(notificationRepository, never()).save(any(Notification.class));
            }

            private static Stream<Arguments> notFoundOrInactiveCases() {
                ParticipatingUser participatingUser = new ParticipatingUser();
                participatingUser.setId(1);

                Notification inactiveNotification = Notification.builder()
                        .title("削除済み通知")
                        .message("内容")
                        .read(false)
                        .participatingUser(participatingUser)
                        .build();
                inactiveNotification.setId(1);
                inactiveNotification.setIsActive(false);

                return Stream.of(
                        Arguments.of(999, "存在しないID", null),
                        Arguments.of(1, "論理削除済み(isActive=false)", inactiveNotification)
                );
            }

            @Test
            @DisplayName("UT-NS-010: 他ユーザーが所有する通知を指定した場合、アクセス不可として404相当を返す")
            void readNotification_notOwnedByUser_returnsNotFound() {
                // Input: userId=1, notificationId=1, isOperatingUser=false
                //        （該当NotificationのparticipatingUser.id=2）
                int userId = 1;
                int notificationId = 1;
                boolean isOperatingUser = false;

                ParticipatingUser otherParticipatingUser = new ParticipatingUser();
                otherParticipatingUser.setId(2);

                Notification notification = Notification.builder()
                        .title("他ユーザーの通知")
                        .message("内容")
                        .read(false)
                        .participatingUser(otherParticipatingUser)
                        .build();
                notification.setId(notificationId);
                notification.setIsActive(true);

                when(notificationRepository.findById(eq(notificationId)))
                        .thenReturn(Optional.of(notification));

                // Execute
                NotificationListResponse response = notificationService.readNotification(
                        userId, notificationId, isOperatingUser);

                // Output: statusCode=NOT_FOUND
                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);

                // message="Notification not found"
                assertThat(response.getMessage()).isEqualTo("Notification not found");

                // isNotificationAccessibleがfalseとなり、notificationRepository.saveが呼ばれないこと
                verify(notificationRepository, never()).save(any(Notification.class));
            }
        }

        // ------------------------------------------------------------------------
        // deleteNotification
        // ------------------------------------------------------------------------
        @Nested
        @DisplayName("deleteNotification のテスト")
        class DeleteNotificationServiceTest {

            @Test
            @DisplayName("UT-NS-011: 自身が所有する通知を論理削除（isActive=falseに更新してsave）")
            void deleteNotification_ownNotification_setsInactiveAndSaves() {
                // Input: userId=1, notificationId=1, isOperatingUser=false
                //        （該当Notificationはparticipatingユーザーid=1、isActive=true）
                int userId = 1;
                int notificationId = 1;
                boolean isOperatingUser = false;

                ParticipatingUser participatingUser = new ParticipatingUser();
                participatingUser.setId(userId);

                Notification notification = Notification.builder()
                        .title("通知1")
                        .message("内容1")
                        .read(false)
                        .participatingUser(participatingUser)
                        .build();
                notification.setId(notificationId);
                notification.setIsActive(true);

                when(notificationRepository.findById(eq(notificationId)))
                        .thenReturn(Optional.of(notification));

                when(notificationRepository.save(any(Notification.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));

                // Execute
                NotificationListResponse response = notificationService.deleteNotification(
                        userId, notificationId, isOperatingUser);

                // Output: statusCode=OK
                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

                // notifications=空リスト
                assertThat(response.getNotifications()).isEmpty();

                // notification.setIsActive(false)後にsaveが呼ばれること
                assertThat(notification.getIsActive()).isFalse();
                verify(notificationRepository).save(eq(notification));
            }

            @Test
            @DisplayName("UT-NS-012: 運営ユーザーが自身の通知を削除（isOperatingUser=true分岐カバレッジ）")
            void deleteNotification_operatingUserOwnNotification_setsInactiveAndSaves() {
                // Input: userId=1, notificationId=1, isOperatingUser=true
                //        （該当NotificationのoperatingUser.id=1）
                int userId = 1;
                int notificationId = 1;
                boolean isOperatingUser = true;

                OperatingUser operatingUser = new OperatingUser();
                operatingUser.setId(userId);

                Notification notification = Notification.builder()
                        .title("運営通知1")
                        .message("内容1")
                        .read(false)
                        .operatingUser(operatingUser)
                        .build();
                notification.setId(notificationId);
                notification.setIsActive(true);

                when(notificationRepository.findById(eq(notificationId)))
                        .thenReturn(Optional.of(notification));

                when(notificationRepository.save(any(Notification.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));

                // Execute
                NotificationListResponse response = notificationService.deleteNotification(
                        userId, notificationId, isOperatingUser);

                // Output: statusCode=OK
                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

                // isNotificationAccessibleで運営ユーザー分岐が通ること（結果的にisActive=falseに更新される）
                assertThat(notification.getIsActive()).isFalse();

                // saveが呼ばれること
                verify(notificationRepository).save(eq(notification));
            }

            @ParameterizedTest(name = "notificationId={0}のケース: {1}")
            @DisplayName("UT-NS-013: notificationIdが存在しない、または既にisActive=falseの場合404相当を返す")
            @MethodSource("notFoundOrInactiveCasesForDelete")
            void deleteNotification_notFoundOrInactive_returnsNotFound(
                    int notificationId, String caseName, Notification mockedNotification) {
                // Input: userId=1, notificationId=999 または 1, isOperatingUser=false
                int userId = 1;
                boolean isOperatingUser = false;

                when(notificationRepository.findById(eq(notificationId)))
                        .thenReturn(Optional.ofNullable(mockedNotification));

                // Execute
                NotificationListResponse response = notificationService.deleteNotification(
                        userId, notificationId, isOperatingUser);

                // Output: statusCode=NOT_FOUND
                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);

                // message="Notification not found"
                assertThat(response.getMessage()).isEqualTo("Notification not found");

                // notificationRepository.saveが呼ばれないこと
                verify(notificationRepository, never()).save(any(Notification.class));
            }

            private static Stream<Arguments> notFoundOrInactiveCasesForDelete() {
                ParticipatingUser participatingUser = new ParticipatingUser();
                participatingUser.setId(1);

                Notification inactiveNotification = Notification.builder()
                        .title("削除済み通知")
                        .message("内容")
                        .read(false)
                        .participatingUser(participatingUser)
                        .build();
                inactiveNotification.setId(1);
                inactiveNotification.setIsActive(false);

                return Stream.of(
                        Arguments.of(999, "存在しないID", null),
                        Arguments.of(1, "既にisActive=false", inactiveNotification)
                );
            }

            @Test
            @DisplayName("UT-NS-014: 他ユーザーが所有する通知を指定した場合、アクセス不可として404相当を返す")
            void deleteNotification_notOwnedByUser_returnsNotFound() {
                // Input: userId=1, notificationId=1, isOperatingUser=false
                //        （該当NotificationのparticipatingUser.id=2）
                int userId = 1;
                int notificationId = 1;
                boolean isOperatingUser = false;

                ParticipatingUser otherParticipatingUser = new ParticipatingUser();
                otherParticipatingUser.setId(2);

                Notification notification = Notification.builder()
                        .title("他ユーザーの通知")
                        .message("内容")
                        .read(false)
                        .participatingUser(otherParticipatingUser)
                        .build();
                notification.setId(notificationId);
                notification.setIsActive(true);

                when(notificationRepository.findById(eq(notificationId)))
                        .thenReturn(Optional.of(notification));

                // Execute
                NotificationListResponse response = notificationService.deleteNotification(
                        userId, notificationId, isOperatingUser);

                // Output: statusCode=NOT_FOUND
                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);

                // message="Notification not found"
                assertThat(response.getMessage()).isEqualTo("Notification not found");

                // isNotificationAccessibleがfalseとなり、saveが呼ばれないこと
                verify(notificationRepository, never()).save(any(Notification.class));
            }
        }

        // ------------------------------------------------------------------------
        // createInquiry
        // ------------------------------------------------------------------------
        @Nested
        @DisplayName("createInquiry のテスト")
        class CreateInquiryServiceTest {

            @Test
            @DisplayName("UT-NS-015: 問い合わせ内容から運営ユーザー全員へメール送信＋通知作成が行われ成功レスポンスが返る")
            void createInquiry_multipleOperatingUsers_sendsEmailAndCreatesNotificationForAll() {
                // Input: request: email="test@example.com", companyName="テスト株式会社",
                //        name="山田太郎", content="問い合わせ内容"
                InquiryRequest request = new InquiryRequest();
                request.setEmail("test@example.com");
                request.setCompanyName("テスト株式会社");
                request.setName("山田太郎");
                request.setContent("問い合わせ内容");

                // （operatingUserRepository.findAllが2件のユーザーを返す）
                OperatingUser operatingUser1 = new OperatingUser();
                operatingUser1.setId(1);
                operatingUser1.setEmail("admin1@example.com");

                OperatingUser operatingUser2 = new OperatingUser();
                operatingUser2.setId(2);
                operatingUser2.setEmail("admin2@example.com");

                List<OperatingUser> operatingUsers = List.of(operatingUser1, operatingUser2);

                when(operatingUserRepository.findAll()).thenReturn(operatingUsers);

                // createNotification内部でIDから再取得されるためfindByIdも個別にスタブする
                when(operatingUserRepository.findById(1)).thenReturn(Optional.of(operatingUser1));
                when(operatingUserRepository.findById(2)).thenReturn(Optional.of(operatingUser2));

                when(notificationRepository.save(any(Notification.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));

                // Execute
                BaseResponse response = notificationService.createInquiry(request);

                // Output: statusCode=OK
                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

                // message="お問い合わせ内容を受け付けました。"
                assertThat(response.getMessage()).isEqualTo("お問い合わせ内容を受け付けました。");

                // mailSender.sendが2件分実行されること
                verify(mailSender, times(2)).send(any(SimpleMailMessage.class));

                // notificationRepository.saveが2件分実行されること
                verify(notificationRepository, times(2)).save(any(Notification.class));
            }

            @Test
            @DisplayName("UT-NS-016: 運営ユーザーが1人も存在しない場合でも例外なく成功レスポンスが返る")
            void createInquiry_noOperatingUsers_returnsSuccessWithoutException() {
                // Input: request: 有効な内容（operatingUserRepository.findAllが空リストを返す）
                InquiryRequest request = new InquiryRequest();
                request.setEmail("test@example.com");
                request.setCompanyName("テスト株式会社");
                request.setName("山田太郎");
                request.setContent("問い合わせ内容");

                when(operatingUserRepository.findAll()).thenReturn(List.of());

                // Execute
                BaseResponse response = notificationService.createInquiry(request);

                // Output: statusCode=OKのレスポンスが返る（例外なし）
                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
                assertThat(response.getMessage()).isEqualTo("お問い合わせ内容を受け付けました。");

                // mailSender.send・notificationRepository.saveが呼ばれないこと
                verify(mailSender, never()).send(any(SimpleMailMessage.class));
                verify(notificationRepository, never()).save(any(Notification.class));
            }
        }

        // ------------------------------------------------------------------------
        // sendMail共通処理
        // ------------------------------------------------------------------------
        @Nested
        @DisplayName("sendMail共通処理のテスト")
        class SendMailCommonTest {

            @Test
            @DisplayName("UT-NS-017: sendSuspensionNotification実行時、mailSender.sendに正しいfrom/to/subject/bodyが設定される（共通ロジックの代表確認）")
            void sendSuspensionNotification_validEmail_sendsMailWithCorrectFields() {
                // Arrange
                 String fromAddress = "noreply@example.com";
                ReflectionTestUtils.setField(notificationService, "fromAddress", fromAddress);
                ParticipatingCompany company = new ParticipatingCompany();
                company.setId(1);
                company.setEmail("company@example.com");
                // overrideToAddress未設定（nullのまま）

                // Act
                notificationService.sendSuspensionNotification(company);

                // Assert
                ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
                verify(mailSender, times(1)).send(messageCaptor.capture());

                SimpleMailMessage sentMessage = messageCaptor.getValue();
                assertNotNull(sentMessage.getTo());
                assertEquals(1, sentMessage.getTo().length);
                assertEquals("company@example.com", sentMessage.getTo()[0]);
                assertEquals(fromAddress, sentMessage.getFrom());
                assertEquals("アカウント停止のお知らせ", sentMessage.getSubject());
                assertNotNull(sentMessage.getText());
            }

            @Test
            @DisplayName("UT-NS-018: 宛先メールアドレスが空/nullの場合、mailSender.sendが呼ばれずwarnログのみで正常終了する")
            void sendSuspensionNotification_nullEmail_doesNotSendMail() {
                // Arrange
                String fromAddress = "noreply@example.com";
                ReflectionTestUtils.setField(notificationService, "fromAddress", fromAddress);
                ParticipatingCompany company = new ParticipatingCompany();
                company.setId(1);
                company.setEmail(null);
                // overrideToAddress未設定（nullのまま）

                // Act & Assert（例外がスローされないこと）
                assertDoesNotThrow(() -> notificationService.sendSuspensionNotification(company));

                // mailSender.sendが呼ばれないこと
                verify(mailSender, never()).send(any(SimpleMailMessage.class));
            }
        }
    }
}
