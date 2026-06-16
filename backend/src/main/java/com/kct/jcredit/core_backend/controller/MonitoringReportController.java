package com.kct.jcredit.core_backend.controller;
import com.kct.jcredit.core_backend.dto.request.MonitoringReportRequest;
import com.kct.jcredit.core_backend.dto.response.CorporationMasterResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringReportResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectMasterResponse;
import com.kct.jcredit.core_backend.service.MonitoringApplicationService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/monitoringreport")
@RequiredArgsConstructor
public class MonitoringReportController {
    private final MonitoringApplicationService monitoringApplicationService;
    private final JwtUtil jwtUtil;
  
  
  // ==================== モニタリング報告書 APIs ====================

    /**
     * GET /monitoring/autofill/company/{companyId}
     * Lookup company master data from PostgreSQL for auto-fill
     */
    @GetMapping("/autofill/company/{companyId}")
    @PreAuthorize("hasAnyRole('master','user')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CorporationMasterResponse> getCompanyMaster(
            Authentication authentication,
            @PathVariable String companyId) {
        String token = authentication.getCredentials().toString();
        CorporationMasterResponse response = monitoringApplicationService.getCompanyMaster(
                companyId,
                jwtUtil.extractCompanyId(token),
                jwtUtil.extractRole(token),
                jwtUtil.extractUsername(token));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    /**
     * GET /monitoring/autofill/projects/{projectId}
     * Lookup project master data from PostgreSQL for auto-fill
     */
    @GetMapping("/autofill/projects/{projectId}")
    @PreAuthorize("hasAnyRole('master','user')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProjectMasterResponse> getProjectMaster(
            Authentication authentication,
            @PathVariable Integer projectId) {
        String token = authentication.getCredentials().toString();
        ProjectMasterResponse response = monitoringApplicationService.getProjectMaster(
                projectId,
                jwtUtil.extractCompanyId(token),
                jwtUtil.extractRole(token),
                jwtUtil.extractUsername(token));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    /**
     * POST /monitoring/project/{projectId}/monitoring-report
     * Submit a fully validated monitoring report to DynamoDB
     */
    @PostMapping("/project/{projectId}/monitoring-report")
    @PreAuthorize("hasAnyRole('master','user')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MonitoringReportResponse> submitMonitoringReport(
            Authentication authentication,
            @PathVariable Integer projectId,
            @RequestParam Integer siteId,
            @Valid @RequestBody MonitoringReportRequest request) {
        String token = authentication.getCredentials().toString();
        MonitoringReportResponse response = monitoringApplicationService.submitMonitoringReport(
                projectId,
                siteId,
                request,
                jwtUtil.extractCompanyId(token),
                jwtUtil.extractRole(token),
                jwtUtil.extractUsername(token));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    /**
     * GET /monitoring/project/{projectId}/monitoring-report
     * Fetch the latest monitoring report from DynamoDB
     */
    @GetMapping("/project/{projectId}/monitoring-report")
    @PreAuthorize("hasAnyRole('master','user')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MonitoringReportResponse> getMonitoringReport(
            Authentication authentication,
            @PathVariable Integer projectId,
            @RequestParam Integer siteId) {
        String token = authentication.getCredentials().toString();
        MonitoringReportResponse response = monitoringApplicationService.getMonitoringReport(
                projectId,
                siteId,
                jwtUtil.extractCompanyId(token),
                jwtUtil.extractRole(token),
                jwtUtil.extractUsername(token));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

}