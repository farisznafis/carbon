package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.request.MonitoringDeletionExecutionRequest;
import com.kct.jcredit.core_backend.dto.response.MonitoringDataDeletionHistoryListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringDeletionCandidateListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringDeletionCandidateRecordListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringDeletionExecutionResponse;
import com.kct.jcredit.core_backend.service.MonitoringDataDeletionService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes operator-only monitoring data deletion operations.
 */
@RestController
@RequestMapping("/admin/monitoring-data")
@Tag(name = "モニタリングデータ管理", description = "モニタリングデータの削除候補抽出、削除実行、削除履歴取得を管理するAPIです。")
@RequiredArgsConstructor
@Validated
public class AdminMonitoringController {

    private final MonitoringDataDeletionService monitoringDataDeletionService;
    private final JwtUtil jwtUtil;

    @GetMapping("/deletion-candidates")
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).MONITORING_READ.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "削除対象データ抽出", description = "認証対象期間終了から2年が経過したモニタリングデータを削除候補として抽出します。")
    public ResponseEntity<MonitoringDeletionCandidateListResponse> getDeletionCandidates() {
        MonitoringDeletionCandidateListResponse response = monitoringDataDeletionService.previewDeletionCandidates();
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @GetMapping("/deletion-candidates/{projectPublicId}")
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).MONITORING_READ.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "削除対象データ明細取得", description = "指定したプロジェクト・拠点の削除候補モニタリングデータを、レコード単位の明細で取得します。")
    public ResponseEntity<MonitoringDeletionCandidateRecordListResponse> getDeletionCandidateRecords(
            @PathVariable String projectPublicId,
            @RequestParam String sitePublicId) {
        MonitoringDeletionCandidateRecordListResponse response =
                monitoringDataDeletionService.previewDeletionCandidateRecords(projectPublicId, sitePublicId);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/deletion")
    @PreAuthorize("hasRole('admin') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).MONITORING_DELETE.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "モニタリングデータ削除実行", description = "指定されたプロジェクトの削除対象モニタリングデータを削除し、削除履歴を記録します。")
    public ResponseEntity<MonitoringDeletionExecutionResponse> executeDeletion(
            Authentication authentication,
            @RequestBody @Valid MonitoringDeletionExecutionRequest request) {
        String actor = jwtUtil.extractUsername(authentication.getCredentials().toString());
        MonitoringDeletionExecutionResponse response = monitoringDataDeletionService.executeDeletion(
                actor,
                request.getProjectPublicIds()
        );
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
            @Max(value = 100, message = "表示件数は100以下で指定してください。") int limit) {
        MonitoringDataDeletionHistoryListResponse response =
                monitoringDataDeletionService.findDeletionHistory(projectId, page, limit);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }
}
