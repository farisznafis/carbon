package com.kct.jcredit.core_backend.adminmonitoringcontrollertest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.controller.AdminMonitoringController;
import com.kct.jcredit.core_backend.dto.response.CompanyActivityGroupResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002IndividualActivityDetailResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002IndividualActivityRowResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringDataDeletionHistoryListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringDeletionScheduleListResponse;
import com.kct.jcredit.core_backend.service.EnR002IndividualActivityService;
import com.kct.jcredit.core_backend.service.MonitoringDataDeletionService;
import com.kct.jcredit.core_backend.service.impl.EnR002IndividualActivityServiceImpl;
import com.kct.jcredit.core_backend.util.ResponseUtil;

// Scope note: plain Mockito test of AdminMonitoringController. It proves delegation,
// argument order, status mapping and exception propagation.
//
// NOT covered here, because plain Mockito never runs the Spring proxies:
//  - @PreAuthorize on all five endpoints (admin, or operator + MONITORING_READ)
//  - @Min / @Max on page and limit of /deletion-history
//  - the defaultValue of page (1) and limit (20)
// These need a @WebMvcTest round (MockMvc + @WithMockUser).
//
// IMPORTS: copy the project-specific imports (ResponseConstants, ResponseUtil and the
// response DTOs) from AdminMonitoringController itself rather than guessing packages.
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminMonitoringController のテスト")
class AdminMonitoringControllerTest {

    @Mock private MonitoringDataDeletionService deletionService;
    @Mock private EnR002IndividualActivityService individualActivityService;
    @Mock private Authentication authentication;

    @InjectMocks
    private AdminMonitoringController controller;

    private MockedStatic<ResponseUtil> responseUtil;

    private static final String USERNAME = "operator01";
    private static final String PROJECT_PUBLIC_ID = "PUB-PJ-001";
    private static final String COMPANY_ID = "COMP-001";

    @BeforeEach
    void setUp() {
        // Lenient: the three individual-activity endpoints never call ResponseUtil.
        responseUtil = mockStatic(ResponseUtil.class, withSettings().lenient());
    }

    @AfterEach
    void tearDown() {
        responseUtil.close();
    }

    // ─────────────────────────────────────────────────────────────
    // Fixtures
    // ─────────────────────────────────────────────────────────────

    private void stubOkMapsToHttp200() {
        responseUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                .thenReturn(HttpStatus.OK);
    }

    private void stubNotFoundMapsToHttp404() {
        responseUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.NOT_FOUND))
                .thenReturn(HttpStatus.NOT_FOUND);
    }

    private MonitoringDeletionScheduleListResponse scheduleResponse(boolean ok) {
        MonitoringDeletionScheduleListResponse response = new MonitoringDeletionScheduleListResponse();
        response.setStatusCode(ok ? ResponseConstants.OK : ResponseConstants.NOT_FOUND);
        return response;
    }

    private MonitoringDataDeletionHistoryListResponse historyResponse(boolean ok) {
        MonitoringDataDeletionHistoryListResponse response = new MonitoringDataDeletionHistoryListResponse();
        response.setStatusCode(ok ? ResponseConstants.OK : ResponseConstants.NOT_FOUND);
        return response;
    }

    // ==================================================================
    // 01. GET /admin/monitoring-data/deletion-schedule
    // ==================================================================

    @Test
    @DisplayName("UT-AMC-01-01: 正常 - サービスの結果がHTTP 200でそのまま返る")
    void getDeletionSchedule_valid_returnsServiceResponseWith200() {
        MonitoringDeletionScheduleListResponse body = scheduleResponse(true);
        when(deletionService.findDeletionSchedule()).thenReturn(body);
        stubOkMapsToHttp200();

        ResponseEntity<MonitoringDeletionScheduleListResponse> result = controller.getDeletionSchedule();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(body);
    }

    @Test
    @DisplayName("UT-AMC-01-02: 準正常 - サービスがNOT_FOUNDを返した場合、HTTPステータスに変換される")
    void getDeletionSchedule_notFoundStatus_mapsToHttp404() {
        MonitoringDeletionScheduleListResponse body = scheduleResponse(false);
        when(deletionService.findDeletionSchedule()).thenReturn(body);
        stubNotFoundMapsToHttp404();

        ResponseEntity<MonitoringDeletionScheduleListResponse> result = controller.getDeletionSchedule();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).isSameAs(body);
    }

    @Test
    @DisplayName("UT-AMC-01-03: 要確認 - 呼び出し元ユーザー情報をサービスへ渡さない")
    void getDeletionSchedule_doesNotPassCallerToService() {
        // NOTE: this endpoint takes no Authentication, so the service cannot scope the
        // schedule to the caller (compare the three individual-activity endpoints, which
        // do pass authentication.getName()). Recorded as current behaviour until the
        // scope question for operators of different companies is settled.
        when(deletionService.findDeletionSchedule()).thenReturn(scheduleResponse(true));
        stubOkMapsToHttp200();

        controller.getDeletionSchedule();

        verify(deletionService).findDeletionSchedule();
        verifyNoMoreInteractions(deletionService);
        verifyNoInteractions(individualActivityService);
    }

    // ==================================================================
    // 02. GET /admin/monitoring-data/deletion-history
    // ==================================================================

    @Test
    @DisplayName("UT-AMC-02-01: 正常 - projectId・page・limit・ログインユーザー名がこの順でサービスへ渡る")
    void getDeletionHistory_valid_passesArgumentsInOrder() {
        // Distinct values for page and limit so a swap of the two int arguments would fail.
        MonitoringDataDeletionHistoryListResponse body = historyResponse(true);
        when(authentication.getName()).thenReturn(USERNAME);
        when(deletionService.findDeletionHistory(7, 3, 50, USERNAME)).thenReturn(body);
        stubOkMapsToHttp200();

        ResponseEntity<MonitoringDataDeletionHistoryListResponse> result =
                controller.getDeletionHistory(7, 3, 50, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(body);
        verify(deletionService).findDeletionHistory(7, 3, 50, USERNAME);
    }

    @Test
    @DisplayName("UT-AMC-02-02: 正常 - projectId未指定（null）のままサービスへ渡る")
    void getDeletionHistory_projectIdNull_passedThroughAsNull() {
        when(authentication.getName()).thenReturn(USERNAME);
        when(deletionService.findDeletionHistory(null, 1, 20, USERNAME)).thenReturn(historyResponse(true));
        stubOkMapsToHttp200();

        ResponseEntity<MonitoringDataDeletionHistoryListResponse> result =
                controller.getDeletionHistory(null, 1, 20, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(deletionService).findDeletionHistory(null, 1, 20, USERNAME);
    }

    @Test
    @DisplayName("UT-AMC-02-03: 境界値 - page=1・limit=100はコントローラーで加工されずに渡る")
    void getDeletionHistory_boundaryValues_notModifiedByController() {
        // The @Min/@Max limits are enforced by Spring before the method runs; the method
        // itself neither clamps nor converts page to 0-based.
        when(authentication.getName()).thenReturn(USERNAME);
        when(deletionService.findDeletionHistory(1, 1, 100, USERNAME)).thenReturn(historyResponse(true));
        stubOkMapsToHttp200();

        controller.getDeletionHistory(1, 1, 100, authentication);

        verify(deletionService).findDeletionHistory(1, 1, 100, USERNAME);
    }

    @Test
    @DisplayName("UT-AMC-02-04: 準正常 - サービスがNOT_FOUNDを返した場合（存在しない／閲覧権限なしのプロジェクト）、HTTPステータスに変換される")
    void getDeletionHistory_notFoundStatus_mapsToHttp404() {
        MonitoringDataDeletionHistoryListResponse body = historyResponse(false);
        when(authentication.getName()).thenReturn(USERNAME);
        when(deletionService.findDeletionHistory(7, 1, 20, USERNAME)).thenReturn(body);
        stubNotFoundMapsToHttp404();

        ResponseEntity<MonitoringDataDeletionHistoryListResponse> result =
                controller.getDeletionHistory(7, 1, 20, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).isSameAs(body);
    }

    @Test
    @DisplayName("UT-AMC-02-05: 正常 - ログインユーザー名はAuthenticationから取得してサービスへ渡る")
    void getDeletionHistory_usesNameFromAuthentication() {
        when(authentication.getName()).thenReturn("another.user");
        when(deletionService.findDeletionHistory(7, 1, 20, "another.user"))
                .thenReturn(historyResponse(true));
        stubOkMapsToHttp200();

        controller.getDeletionHistory(7, 1, 20, authentication);

        verify(authentication).getName();
        verify(deletionService).findDeletionHistory(7, 1, 20, "another.user");
        verifyNoMoreInteractions(deletionService);
        verifyNoInteractions(individualActivityService);
    }

    // ==================================================================
    // 03. GET /admin/monitoring-data/projects/{projectId}/individual-activity/rows
    // ==================================================================

    @Test
    @DisplayName("UT-AMC-03-01: 正常 - projectId・companyId・ログインユーザー名がこの順でサービスへ渡り、結果がそのまま返る")
    void getRowsByProject_valid_delegatesWithCallerName() {
        // Three String arguments side by side: distinct values catch any swap.
        List<EnR002IndividualActivityRowResponse> rows = List.of(mock(EnR002IndividualActivityRowResponse.class));
        when(authentication.getName()).thenReturn(USERNAME);
        when(individualActivityService.getRowsByProject(PROJECT_PUBLIC_ID, COMPANY_ID, USERNAME))
                .thenReturn(rows);

        List<EnR002IndividualActivityRowResponse> result =
                controller.getRowsByProject(PROJECT_PUBLIC_ID, COMPANY_ID, authentication);

        assertThat(result).isSameAs(rows);
        verify(individualActivityService).getRowsByProject(PROJECT_PUBLIC_ID, COMPANY_ID, USERNAME);
        verifyNoInteractions(deletionService);
    }

    @Test
    @DisplayName("UT-AMC-03-02: 正常 - companyId未指定（null）のままサービスへ渡る")
    void getRowsByProject_companyIdNull_passedThroughAsNull() {
        when(authentication.getName()).thenReturn(USERNAME);
        when(individualActivityService.getRowsByProject(PROJECT_PUBLIC_ID, null, USERNAME))
                .thenReturn(List.of());

        controller.getRowsByProject(PROJECT_PUBLIC_ID, null, authentication);

        verify(individualActivityService).getRowsByProject(PROJECT_PUBLIC_ID, null, USERNAME);
    }

    @Test
    @DisplayName("UT-AMC-03-03: 正常 - 該当設備が0件の場合、空リストがそのまま返る")
    void getRowsByProject_noRows_returnsEmptyList() {
        when(authentication.getName()).thenReturn(USERNAME);
        when(individualActivityService.getRowsByProject(PROJECT_PUBLIC_ID, null, USERNAME))
                .thenReturn(List.of());

        List<EnR002IndividualActivityRowResponse> result =
                controller.getRowsByProject(PROJECT_PUBLIC_ID, null, authentication);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("UT-AMC-03-04: 要確認 - サービスの例外はコントローラーで変換されずそのまま伝播する")
    void getRowsByProject_serviceThrows_propagatesUnchanged() {
        // NOTE: the controller has no try/catch and this endpoint returns a bare list, so
        // "project not found" leaves as an exception. Whether it becomes 404 or 500 depends
        // on a @ControllerAdvice outside this class; confirm that mapping exists.
        when(authentication.getName()).thenReturn(USERNAME);
        when(individualActivityService.getRowsByProject(PROJECT_PUBLIC_ID, null, USERNAME))
                .thenThrow(new IllegalArgumentException("Project not found: " + PROJECT_PUBLIC_ID));

        assertThatThrownBy(() -> controller.getRowsByProject(PROJECT_PUBLIC_ID, null, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(PROJECT_PUBLIC_ID);
    }

    // ==================================================================
    // 04. GET /admin/monitoring-data/companies/{companyId}/individual-activity
    // ==================================================================

    @Test
    @DisplayName("UT-AMC-04-01: 正常 - companyId・projectId・ログインユーザー名がこの順でサービスへ渡り、結果がそのまま返る")
    void getRowsByCompany_valid_delegatesWithCallerName() {
        // The path variable and the request parameter are both Strings, and their order
        // is the reverse of getRowsByProject; distinct values catch a swap.
        List<CompanyActivityGroupResponse> groups = List.of(mock(CompanyActivityGroupResponse.class));
        when(authentication.getName()).thenReturn(USERNAME);
        when(individualActivityService.getRowsByCompany(COMPANY_ID, PROJECT_PUBLIC_ID, USERNAME))
                .thenReturn(groups);

        List<CompanyActivityGroupResponse> result =
                controller.getRowsByCompany(COMPANY_ID, PROJECT_PUBLIC_ID, authentication);

        assertThat(result).isSameAs(groups);
        verify(individualActivityService).getRowsByCompany(COMPANY_ID, PROJECT_PUBLIC_ID, USERNAME);
        verifyNoInteractions(deletionService);
    }

    @Test
    @DisplayName("UT-AMC-04-02: 正常 - projectId未指定（null）のままサービスへ渡る")
    void getRowsByCompany_projectIdNull_passedThroughAsNull() {
        when(authentication.getName()).thenReturn(USERNAME);
        when(individualActivityService.getRowsByCompany(COMPANY_ID, null, USERNAME))
                .thenReturn(List.of());

        controller.getRowsByCompany(COMPANY_ID, null, authentication);

        verify(individualActivityService).getRowsByCompany(COMPANY_ID, null, USERNAME);
    }

    @Test
    @DisplayName("UT-AMC-04-03: 正常 - 該当プロジェクトが0件の場合、空リストがそのまま返る")
    void getRowsByCompany_noGroups_returnsEmptyList() {
        when(authentication.getName()).thenReturn(USERNAME);
        when(individualActivityService.getRowsByCompany(COMPANY_ID, null, USERNAME))
                .thenReturn(List.of());

        List<CompanyActivityGroupResponse> result =
                controller.getRowsByCompany(COMPANY_ID, null, authentication);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("UT-AMC-04-04: 要確認 - サービスの例外はコントローラーで変換されずそのまま伝播する")
    void getRowsByCompany_serviceThrows_propagatesUnchanged() {
        // NOTE: same as UT-AMC-03-04, for "Company not found".
        when(authentication.getName()).thenReturn(USERNAME);
        when(individualActivityService.getRowsByCompany(COMPANY_ID, null, USERNAME))
                .thenThrow(new IllegalArgumentException("Company not found: " + COMPANY_ID));

        assertThatThrownBy(() -> controller.getRowsByCompany(COMPANY_ID, null, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(COMPANY_ID);
    }

    // ==================================================================
    // 05. GET /admin/monitoring-data/individual-activity/rows/{equipmentId}
    // ==================================================================

    @Test
    @DisplayName("UT-AMC-05-01: 正常 - equipmentIdとログインユーザー名がサービスへ渡り、詳細がそのまま返る")
    void getRowDetail_valid_delegatesWithCallerName() {
        EnR002IndividualActivityDetailResponse detail = mock(EnR002IndividualActivityDetailResponse.class);
        when(authentication.getName()).thenReturn(USERNAME);
        when(individualActivityService.getRowDetail(42, USERNAME)).thenReturn(detail);

        EnR002IndividualActivityDetailResponse result = controller.getRowDetail(42, authentication);

        assertThat(result).isSameAs(detail);
        verify(individualActivityService).getRowDetail(42, USERNAME);
        verifyNoInteractions(deletionService);
    }

    @Test
    @DisplayName("UT-AMC-05-02: 要確認 - 該当設備なしの例外はコントローラーで変換されずそのまま伝播する")
    void getRowDetail_notFound_propagatesUnchanged() {
        // NOTE: IndividualActivityNotFoundException carries no @ResponseStatus, so unless a
        // @ControllerAdvice maps it, the client sees HTTP 500 for an unknown equipmentId.
        when(authentication.getName()).thenReturn(USERNAME);
        when(individualActivityService.getRowDetail(999, USERNAME))
                .thenThrow(new EnR002IndividualActivityServiceImpl.IndividualActivityNotFoundException(999));

        assertThatThrownBy(() -> controller.getRowDetail(999, authentication))
                .isInstanceOf(EnR002IndividualActivityServiceImpl.IndividualActivityNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("UT-AMC-05-03: 正常 - ログインユーザー名はAuthenticationから取得される")
    void getRowDetail_usesNameFromAuthentication() {
        when(authentication.getName()).thenReturn("another.user");
        when(individualActivityService.getRowDetail(42, "another.user"))
                .thenReturn(mock(EnR002IndividualActivityDetailResponse.class));

        controller.getRowDetail(42, authentication);

        verify(authentication).getName();
        verify(individualActivityService).getRowDetail(42, "another.user");
    }
}
