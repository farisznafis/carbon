package com.kct.jcredit.core_backend.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;


import com.kct.jcredit.core_backend.dto.response.NotificationListResponse;
import com.kct.jcredit.core_backend.dto.request.InquiryRequest;
import com.kct.jcredit.core_backend.dto.response.NotificationCountResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

/*
 * TODO: verify actual constructor/builder for the following DTOs.
 * This test assumes a no-args constructor + setStatusCode(String) setter exists
 * on NotificationListResponse / NotificationCountResponse / BaseResponse.
 * If the real DTOs use @Builder or a different field type for statusCode,
 * these instantiations will fail to compile and must be adjusted.
 */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private NotificationService notificationService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private NotificationController notificationController;

    private static final String DUMMY_TOKEN = "dummy-jwt-token";

    @BeforeEach
    void setUp() {
        // Common stubbing for authentication.getCredentials() is intentionally
        // NOT placed here because some scenarios need authentication == null
        // or getCredentials() == null. Each test stubs what it needs.
    }

    // ==== getNotifications ====

    @Test
    @DisplayName("UT-NC-001: getNotifications_validUserUnreadOnlyFalse_returnsOkResponse")
    void getNotifications_validUserUnreadOnlyFalse_returnsOkResponse() {
        when(authentication.getCredentials()).thenReturn(DUMMY_TOKEN);
        when(jwtUtil.extractUserId(DUMMY_TOKEN)).thenReturn(1);
        when(jwtUtil.extractRole(DUMMY_TOKEN)).thenReturn("participant");

        NotificationListResponse response = new NotificationListResponse();
        response.setStatusCode("200");

        when(notificationService.getNotifications(eq(1), eq(false), eq(0), eq(10), eq(false)))
                .thenReturn(response);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

            ResponseEntity<NotificationListResponse> result =
                    notificationController.getNotifications(authentication, false, 0, 10);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(response);
        }

        ArgumentCaptor<Boolean> isOperatingUserCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(notificationService).getNotifications(eq(1), eq(false), eq(0), eq(10), isOperatingUserCaptor.capture());
        assertThat(isOperatingUserCaptor.getValue()).isFalse();
    }

    @Test
    @DisplayName("UT-NC-002: getNotifications_unreadOnlyTrueAndOperatorRole_passesFlagsThrough")
    void getNotifications_unreadOnlyTrueAndOperatorRole_passesFlagsThrough() {
        when(authentication.getCredentials()).thenReturn(DUMMY_TOKEN);
        when(jwtUtil.extractUserId(DUMMY_TOKEN)).thenReturn(5);
        when(jwtUtil.extractRole(DUMMY_TOKEN)).thenReturn("operator");

        NotificationListResponse response = new NotificationListResponse();
        response.setStatusCode("200");

        when(notificationService.getNotifications(eq(5), eq(true), eq(2), eq(20), eq(true)))
                .thenReturn(response);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

            ResponseEntity<NotificationListResponse> result =
                    notificationController.getNotifications(authentication, true, 2, 20);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        ArgumentCaptor<Boolean> unreadOnlyCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Boolean> isOperatingUserCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(notificationService).getNotifications(
                eq(5), unreadOnlyCaptor.capture(), eq(2), eq(20), isOperatingUserCaptor.capture());
        assertThat(unreadOnlyCaptor.getValue()).isTrue();
        assertThat(isOperatingUserCaptor.getValue()).isTrue();
    }

    // Current behavior: authentication == null causes NPE on authentication.getCredentials().
    // This is not handled by the controller; test documents actual (unhandled) behavior,
    // not asserting it's correct — no explicit confirmation this is intentional.
    @Test
    @DisplayName("UT-NC-003: getNotifications_authenticationNull_throwsNpeAndNeverCallsService")
    void getNotifications_authenticationNull_throwsNpeAndNeverCallsService() {
        assertThrows(NullPointerException.class,
                () -> notificationController.getNotifications(null, false, 0, 10));

        verifyNoInteractions(notificationService);
    }

    // Current behavior: getCredentials() returning null causes NPE on .toString().
    @Test
    @DisplayName("UT-NC-004: getNotifications_credentialsNull_throwsNpeAndNeverCallsService")
    void getNotifications_credentialsNull_throwsNpeAndNeverCallsService() {
        when(authentication.getCredentials()).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> notificationController.getNotifications(authentication, false, 0, 10));

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("UT-NC-005: getNotifications_serviceThrowsRuntimeException_propagatesException")
    void getNotifications_serviceThrowsRuntimeException_propagatesException() {
        when(authentication.getCredentials()).thenReturn(DUMMY_TOKEN);
        when(jwtUtil.extractUserId(DUMMY_TOKEN)).thenReturn(1);
        when(jwtUtil.extractRole(DUMMY_TOKEN)).thenReturn("participant");
        when(notificationService.getNotifications(anyInt(), anyBoolean(), anyInt(), anyInt(), anyBoolean()))
                .thenThrow(new RuntimeException("service failure"));

        assertThatThrownBy(() -> notificationController.getNotifications(authentication, false, 0, 10))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("service failure");
    }

    // ==== getNotificationCount ====

    @Test
    @DisplayName("UT-NC-006: getNotificationCount_validParticipantUser_returnsOkResponse")
    void getNotificationCount_validParticipantUser_returnsOkResponse() {
        when(authentication.getCredentials()).thenReturn(DUMMY_TOKEN);
        when(jwtUtil.extractUserId(DUMMY_TOKEN)).thenReturn(1);
        when(jwtUtil.extractRole(DUMMY_TOKEN)).thenReturn("participant");

        NotificationCountResponse response = new NotificationCountResponse();
        response.setStatusCode("200");

        when(notificationService.getNotificationCount(eq(1), eq(false))).thenReturn(response);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

            ResponseEntity<NotificationCountResponse> result =
                    notificationController.getNotificationCount(authentication);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(response);
        }

        verify(notificationService).getNotificationCount(eq(1), eq(false));
    }

    @Test
    @DisplayName("UT-NC-007: getNotificationCount_operatorRole_passesIsOperatingUserTrue")
    void getNotificationCount_operatorRole_passesIsOperatingUserTrue() {
        when(authentication.getCredentials()).thenReturn(DUMMY_TOKEN);
        when(jwtUtil.extractUserId(DUMMY_TOKEN)).thenReturn(9);
        when(jwtUtil.extractRole(DUMMY_TOKEN)).thenReturn("operator");

        NotificationCountResponse response = new NotificationCountResponse();
        response.setStatusCode("200");

        when(notificationService.getNotificationCount(eq(9), eq(true))).thenReturn(response);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

            notificationController.getNotificationCount(authentication);
        }

        ArgumentCaptor<Boolean> isOperatingUserCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(notificationService).getNotificationCount(eq(9), isOperatingUserCaptor.capture());
        assertThat(isOperatingUserCaptor.getValue()).isTrue();
    }

    @Test
    @DisplayName("UT-NC-008: getNotificationCount_authenticationNull_throwsNpeAndNeverCallsService")
    void getNotificationCount_authenticationNull_throwsNpeAndNeverCallsService() {
        assertThrows(NullPointerException.class,
                () -> notificationController.getNotificationCount(null));

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("UT-NC-009: getNotificationCount_serviceThrowsRuntimeException_propagatesException")
    void getNotificationCount_serviceThrowsRuntimeException_propagatesException() {
        when(authentication.getCredentials()).thenReturn(DUMMY_TOKEN);
        when(jwtUtil.extractUserId(DUMMY_TOKEN)).thenReturn(1);
        when(jwtUtil.extractRole(DUMMY_TOKEN)).thenReturn("participant");
        when(notificationService.getNotificationCount(anyInt(), anyBoolean()))
                .thenThrow(new RuntimeException("count failure"));

        assertThatThrownBy(() -> notificationController.getNotificationCount(authentication))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("count failure");
    }

    // ==== readNotification ====

    @Test
    @DisplayName("UT-NC-010: readNotification_validParticipantUser_returnsOkResponse")
    void readNotification_validParticipantUser_returnsOkResponse() {
        when(authentication.getCredentials()).thenReturn(DUMMY_TOKEN);
        when(jwtUtil.extractUserId(DUMMY_TOKEN)).thenReturn(1);
        when(jwtUtil.extractRole(DUMMY_TOKEN)).thenReturn("participant");

        NotificationListResponse response = new NotificationListResponse();
        response.setStatusCode("200");

        when(notificationService.readNotification(eq(1), eq(100), eq(false))).thenReturn(response);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

            ResponseEntity<NotificationListResponse> result =
                    notificationController.readNotification(authentication, 100);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(response);
        }

        ArgumentCaptor<Integer> notificationIdCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(notificationService).readNotification(eq(1), notificationIdCaptor.capture(), eq(false));
        assertThat(notificationIdCaptor.getValue()).isEqualTo(100);
    }

    @Test
    @DisplayName("UT-NC-011: readNotification_operatorRole_passesIsOperatingUserTrue")
    void readNotification_operatorRole_passesIsOperatingUserTrue() {
        when(authentication.getCredentials()).thenReturn(DUMMY_TOKEN);
        when(jwtUtil.extractUserId(DUMMY_TOKEN)).thenReturn(2);
        when(jwtUtil.extractRole(DUMMY_TOKEN)).thenReturn("operator");

        NotificationListResponse response = new NotificationListResponse();
        response.setStatusCode("200");

        when(notificationService.readNotification(eq(2), eq(200), eq(true))).thenReturn(response);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

            notificationController.readNotification(authentication, 200);
        }

        ArgumentCaptor<Boolean> isOperatingUserCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(notificationService).readNotification(eq(2), eq(200), isOperatingUserCaptor.capture());
        assertThat(isOperatingUserCaptor.getValue()).isTrue();
    }

    @Test
    @DisplayName("UT-NC-012: readNotification_authenticationNull_throwsNpeAndNeverCallsService")
    void readNotification_authenticationNull_throwsNpeAndNeverCallsService() {
        assertThrows(NullPointerException.class,
                () -> notificationController.readNotification(null, 100));

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("UT-NC-013: readNotification_serviceThrowsRuntimeException_propagatesException")
    void readNotification_serviceThrowsRuntimeException_propagatesException() {
        when(authentication.getCredentials()).thenReturn(DUMMY_TOKEN);
        when(jwtUtil.extractUserId(DUMMY_TOKEN)).thenReturn(1);
        when(jwtUtil.extractRole(DUMMY_TOKEN)).thenReturn("participant");
        when(notificationService.readNotification(anyInt(), anyInt(), anyBoolean()))
                .thenThrow(new RuntimeException("read failure"));

        assertThatThrownBy(() -> notificationController.readNotification(authentication, 100))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("read failure");
    }

    // ==== deleteNotification ====

    @Test
    @DisplayName("UT-NC-014: deleteNotification_validParticipantUser_returnsOkResponse")
    void deleteNotification_validParticipantUser_returnsOkResponse() {
        when(authentication.getCredentials()).thenReturn(DUMMY_TOKEN);
        when(jwtUtil.extractUserId(DUMMY_TOKEN)).thenReturn(1);
        when(jwtUtil.extractRole(DUMMY_TOKEN)).thenReturn("participant");

        NotificationListResponse response = new NotificationListResponse();
        response.setStatusCode("200");

        when(notificationService.deleteNotification(eq(1), eq(300), eq(false))).thenReturn(response);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

            ResponseEntity<NotificationListResponse> result =
                    notificationController.deleteNotification(authentication, 300);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(response);
        }

        ArgumentCaptor<Integer> notificationIdCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(notificationService).deleteNotification(eq(1), notificationIdCaptor.capture(), eq(false));
        assertThat(notificationIdCaptor.getValue()).isEqualTo(300);
    }

    @Test
    @DisplayName("UT-NC-015: deleteNotification_operatorRole_passesIsOperatingUserTrue")
    void deleteNotification_operatorRole_passesIsOperatingUserTrue() {
        when(authentication.getCredentials()).thenReturn(DUMMY_TOKEN);
        when(jwtUtil.extractUserId(DUMMY_TOKEN)).thenReturn(3);
        when(jwtUtil.extractRole(DUMMY_TOKEN)).thenReturn("operator");

        NotificationListResponse response = new NotificationListResponse();
        response.setStatusCode("200");

        when(notificationService.deleteNotification(eq(3), eq(400), eq(true))).thenReturn(response);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

            notificationController.deleteNotification(authentication, 400);
        }

        ArgumentCaptor<Boolean> isOperatingUserCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(notificationService).deleteNotification(eq(3), eq(400), isOperatingUserCaptor.capture());
        assertThat(isOperatingUserCaptor.getValue()).isTrue();
    }

    @Test
    @DisplayName("UT-NC-016: deleteNotification_authenticationNull_throwsNpeAndNeverCallsService")
    void deleteNotification_authenticationNull_throwsNpeAndNeverCallsService() {
        assertThrows(NullPointerException.class,
                () -> notificationController.deleteNotification(null, 300));

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("UT-NC-017: deleteNotification_serviceThrowsRuntimeException_propagatesException")
    void deleteNotification_serviceThrowsRuntimeException_propagatesException() {
        when(authentication.getCredentials()).thenReturn(DUMMY_TOKEN);
        when(jwtUtil.extractUserId(DUMMY_TOKEN)).thenReturn(1);
        when(jwtUtil.extractRole(DUMMY_TOKEN)).thenReturn("participant");
        when(notificationService.deleteNotification(anyInt(), anyInt(), anyBoolean()))
                .thenThrow(new RuntimeException("delete failure"));

        assertThatThrownBy(() -> notificationController.deleteNotification(authentication, 300))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("delete failure");
    }

    // ==== createInquiry ====

    @Test
    @DisplayName("UT-NC-018: createInquiry_validRequest_returnsOkResponse")
    void createInquiry_validRequest_returnsOkResponse() {
        InquiryRequest request = new InquiryRequest();

        BaseResponse response = new BaseResponse();
        response.setStatusCode("200");

        when(notificationService.createInquiry(eq(request))).thenReturn(response);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

            ResponseEntity<BaseResponse> result = notificationController.createInquiry(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(response);
        }

        ArgumentCaptor<InquiryRequest> requestCaptor = ArgumentCaptor.forClass(InquiryRequest.class);
        verify(notificationService).createInquiry(requestCaptor.capture());
        assertThat(requestCaptor.getValue()).isSameAs(request);
    }

    // SEC-01: createInquiry performs no authentication/authorization check at all —
    // unlike the other 4 endpoints, it never touches Authentication, JwtUtil, or any
    // role/ownership check. This test documents that jwtUtil is never consulted for
    // this endpoint. Flagged as a potential security gap (unauthenticated write,
    // no identity binding on inquiry submissions) — not asserted as a "bug" since
    // no confirmation was given that authentication is required here; only verifying
    // actual current behavior (no interaction with JwtUtil).
    @Test
    @DisplayName("SEC-01: createInquiry_noAuthenticationRequired_neverInteractsWithJwtUtil")
    void createInquiry_noAuthenticationRequired_neverInteractsWithJwtUtil() {
        InquiryRequest request = new InquiryRequest();

        BaseResponse response = new BaseResponse();
        response.setStatusCode("200");

        when(notificationService.createInquiry(eq(request))).thenReturn(response);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus("200")).thenReturn(HttpStatus.OK);

            notificationController.createInquiry(request);
        }

        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("UT-NC-019: createInquiry_serviceThrowsRuntimeException_propagatesException")
    void createInquiry_serviceThrowsRuntimeException_propagatesException() {
        InquiryRequest request = new InquiryRequest();

        when(notificationService.createInquiry(any(InquiryRequest.class)))
                .thenThrow(new RuntimeException("inquiry failure"));

        assertThatThrownBy(() -> notificationController.createInquiry(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("inquiry failure");
    }
}