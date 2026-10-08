package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.response.MonitoringDataDeletionHistoryListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringDeletionScheduleListResponse;
import com.kct.jcredit.core_backend.service.EnR002IndividualActivityService;
import com.kct.jcredit.core_backend.service.MonitoringDataDeletionService;
import com.kct.jcredit.core_backend.dto.response.CompanyActivityGroupResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002IndividualActivityDetailResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002IndividualActivityRowResponse;
import com.kct.jcredit.core_backend.service.EnR002IndividualActivityService;
import com.kct.jcredit.core_backend.util.ResponseUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes operator-only views of the automatic monitoring data deletion.
 */
@RestController
@RequestMapping("/admin/monitoring-data")
@Tag(name = "モニタリングデータ管理", description = "モニタリングデータの自動削除について、削除予定と削除履歴を取得するAPIです。")
@RequiredArgsConstructor
@Validated
public class AdminMonitoringController {

    private final MonitoringDataDeletionService monitoringDataDeletionService;
    private final EnR002IndividualActivityService individualActivityService;

    @GetMapping("/deletion-schedule")
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).MONITORING_READ.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "削除予定取得", description = "プロジェクトの終了から保存期間が経過すると自動で削除されるモニタリングデータの削除予定日を、プロジェクト単位で取得します。")
    public ResponseEntity<MonitoringDeletionScheduleListResponse> getDeletionSchedule() {
        MonitoringDeletionScheduleListResponse response = monitoringDataDeletionService.findDeletionSchedule();
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @GetMapping("/deletion-history")
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).MONITORING_READ.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "削除履歴取得", description = "モニタリングデータの削除履歴を取得します。")
    public ResponseEntity<MonitoringDataDeletionHistoryListResponse> getDeletionHistory(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "ページ番号は1以上で指定してください。") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "表示件数は1以上で指定してください。")
            @Max(value = 100, message = "表示件数は100以下で指定してください。") int limit,
            Authentication authentication) {
        MonitoringDataDeletionHistoryListResponse response =
                monitoringDataDeletionService.findDeletionHistory(projectId, page, limit, authentication.getName());
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    //Individual Activity

    /** Lists every reportable equipment of one project. */
    @GetMapping("/projects/{projectId}/individual-activity/rows")
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).MONITORING_READ.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "個別活動データ一覧取得（プロジェクト単位）",
           description = "指定プロジェクトに紐づく報告対象設備の一覧を取得します。companyIdを指定すると、特定の参加企業の設備のみに絞り込めます。")
    public List<EnR002IndividualActivityRowResponse> getRowsByProject(
            @PathVariable String projectId,
            @RequestParam(required = false) String companyId,
            Authentication authentication) {
        return individualActivityService.getRowsByProject(projectId, companyId, authentication.getName());
    }
 
    /** Lists one member company's equipment, grouped by project. */
    @GetMapping("/companies/{companyId}/individual-activity")
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).MONITORING_READ.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "個別活動データ一覧取得（企業単位）",
           description = "指定企業が保有する報告対象設備を、参加プロジェクトごとにまとめて取得します。")
    public List<CompanyActivityGroupResponse> getRowsByCompany(
            @PathVariable String companyId,
            @RequestParam(required = false) String projectId,
            Authentication authentication) {
        return individualActivityService.getRowsByCompany(companyId, projectId, authentication.getName());
    }
 
    /** Full detail of one row, reached from either of the two listings above. */
    @GetMapping("/individual-activity/rows/{equipmentId}")
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).MONITORING_READ.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "個別活動データ詳細取得",
           description = "設備1件分の個別活動データの詳細を取得します。")
    public EnR002IndividualActivityDetailResponse getRowDetail(
            @PathVariable Integer equipmentId,
            @RequestParam String projectPublicId,
            Authentication authentication) {
        return individualActivityService.getRowDetail(equipmentId, projectPublicId, authentication.getName());
    }
}
