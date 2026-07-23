package com.kct.jcredit.core_backend.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import com.kct.jcredit.core_backend.dto.response.ApplicableConditionsResponse;
import com.kct.jcredit.core_backend.dto.response.CorporationMasterResponse;
import com.kct.jcredit.core_backend.dto.response.OtherOperatorProjectsResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectMasterResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectApplicationResponse;
import com.kct.jcredit.core_backend.dto.request.ApplicableConditionsRequest;
import com.kct.jcredit.core_backend.dto.request.OtherOperatorProjectsRequest;
import com.kct.jcredit.core_backend.dto.request.ProjectApplicationRequest;
import com.kct.jcredit.core_backend.service.projectplan.ApplicableConditionsService;
import com.kct.jcredit.core_backend.service.projectplan.OtherOperatorProjectsService;
import com.kct.jcredit.core_backend.service.projectplan.ProjectApplicationService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/projectplan")
@Tag(name = "プロジェクト計画書", description = "プロジェクト計画書の自動入力情報、申請情報、適用条件を管理するAPIです。")
@RequiredArgsConstructor
public class ProjectPlanController {
    private final ProjectApplicationService projectApplicationService;
    private final OtherOperatorProjectsService otherOperatorProjectsService;
    private final ApplicableConditionsService applicableConditionsService;
    private final JwtUtil jwtUtil;

        @GetMapping("/autofill")
        @PreAuthorize("hasRole('operator')")
        @SecurityRequirement(name = "bearerAuth")
        @Operation(summary = "法人マスタ自動入力情報取得", description = "プロジェクト計画書入力用の法人マスタ情報を取得します。")
        public ResponseEntity<CorporationMasterResponse> getCorporationMaster(Authentication authentication) {
        String token = authentication.getCredentials().toString();
        CorporationMasterResponse response = projectApplicationService.getCorporationMaster(
            jwtUtil.extractCompanyId(token),
            jwtUtil.extractRole(token),
            jwtUtil.extractUsername(token));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
        }

        @GetMapping("/autofill/{projectId}")
        @PreAuthorize("hasRole('operator')")
        @SecurityRequirement(name = "bearerAuth")
        @Operation(summary = "プロジェクトマスタ自動入力情報取得", description = "指定されたプロジェクトの計画書入力用マスタ情報を取得します。")
        public ResponseEntity<ProjectMasterResponse> getProjectMaster(Authentication authentication,
                @PathVariable String projectId) {
        String token = authentication.getCredentials().toString();
        ProjectMasterResponse response = projectApplicationService.getProjectMaster(
                    projectId,
            jwtUtil.extractCompanyId(token),
            jwtUtil.extractRole(token),
            jwtUtil.extractUsername(token));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
        }

        @PostMapping("/{projectId}/application")
        @PreAuthorize("hasRole('operator')")
        @SecurityRequirement(name = "bearerAuth")
        @Operation(summary = "プロジェクト申請情報登録", description = "指定されたプロジェクトの申請情報を登録します。")
        public ResponseEntity<ProjectApplicationResponse> submitProjectApplication(Authentication authentication,
            @PathVariable String projectId,
            @Valid @RequestBody ProjectApplicationRequest request) {
        String token = authentication.getCredentials().toString();
        ProjectApplicationResponse response = projectApplicationService.submitProjectApplication(
            projectId,
            request,
            jwtUtil.extractCompanyId(token),
            jwtUtil.extractRole(token),
            jwtUtil.extractUsername(token));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
        }

        @PutMapping("/{projectId}/application")
        @PreAuthorize("hasRole('operator')")
        @SecurityRequirement(name = "bearerAuth")
        @Operation(summary = "プロジェクト申請情報更新", description = "指定されたプロジェクトの申請情報を更新します。")
        public ResponseEntity<ProjectApplicationResponse> updateProjectApplication(Authentication authentication,
            @PathVariable String projectId,
            @Valid @RequestBody ProjectApplicationRequest request) {
        String token = authentication.getCredentials().toString();
        ProjectApplicationResponse response = projectApplicationService.updateProjectApplication(
            projectId,
            request,
            jwtUtil.extractCompanyId(token),
            jwtUtil.extractRole(token),
            jwtUtil.extractUsername(token));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
        }

        @GetMapping("/{projectId}/application")
        @PreAuthorize("hasRole('operator')")
        @SecurityRequirement(name = "bearerAuth")
        @Operation(summary = "プロジェクト申請情報取得", description = "指定されたプロジェクトの申請情報を取得します。")
        public ResponseEntity<ProjectApplicationResponse> getProjectApplication(Authentication authentication,
            @PathVariable String projectId) {
        String token = authentication.getCredentials().toString();
        ProjectApplicationResponse response = projectApplicationService.getProjectApplication(
            projectId,
            jwtUtil.extractCompanyId(token),
            jwtUtil.extractRole(token),
            jwtUtil.extractUsername(token));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
        }

        @PostMapping("/{projectId}/other-operator-projects")
        @PreAuthorize("hasRole('operator')")
        @SecurityRequirement(name = "bearerAuth")
        @Operation(summary = "他運営事業者プロジェクト情報登録", description = "指定されたプロジェクトの他運営事業者プロジェクト情報を登録します。")
        public ResponseEntity<OtherOperatorProjectsResponse> submitOtherOperatorProjects(Authentication authentication,
            @PathVariable String projectId,
            @Valid @RequestBody OtherOperatorProjectsRequest request) {
        String token = authentication.getCredentials().toString();
        OtherOperatorProjectsResponse response = otherOperatorProjectsService.submitOtherOperatorProjects(
            projectId,
            request,
            jwtUtil.extractCompanyId(token),
            jwtUtil.extractRole(token),
            jwtUtil.extractUsername(token));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
        }

        @PutMapping("/{projectId}/other-operator-projects")
        @PreAuthorize("hasRole('operator')")
        @SecurityRequirement(name = "bearerAuth")
        @Operation(summary = "他運営事業者プロジェクト情報更新", description = "指定されたプロジェクトの他運営事業者プロジェクト情報を更新します。")
        public ResponseEntity<OtherOperatorProjectsResponse> updateOtherOperatorProjects(Authentication authentication,
            @PathVariable String projectId,
            @Valid @RequestBody OtherOperatorProjectsRequest request) {
        String token = authentication.getCredentials().toString();
        OtherOperatorProjectsResponse response = otherOperatorProjectsService.updateOtherOperatorProjects(
            projectId,
            request,
            jwtUtil.extractCompanyId(token),
            jwtUtil.extractRole(token),
            jwtUtil.extractUsername(token));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
        }

        @GetMapping("/{projectId}/other-operator-projects")
        @PreAuthorize("hasRole('operator')")
        @SecurityRequirement(name = "bearerAuth")
        @Operation(summary = "他運営事業者プロジェクト情報取得", description = "指定されたプロジェクトの他運営事業者プロジェクト情報を取得します。")
        public ResponseEntity<OtherOperatorProjectsResponse> getOtherOperatorProjects(Authentication authentication,
            @PathVariable String projectId) {
        String token = authentication.getCredentials().toString();
        OtherOperatorProjectsResponse response = otherOperatorProjectsService.getOtherOperatorProjects(
            projectId,
            jwtUtil.extractCompanyId(token),
            jwtUtil.extractRole(token),
            jwtUtil.extractUsername(token));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
        }

        @PostMapping("/{projectId}/applicable-conditions")
        @PreAuthorize("hasRole('operator')")
        @SecurityRequirement(name = "bearerAuth")
        @Operation(summary = "適用条件情報登録", description = "指定されたプロジェクトの適用条件情報を登録します。")
        public ResponseEntity<ApplicableConditionsResponse> submitApplicableConditions(Authentication authentication,
            @PathVariable String projectId,
            @Valid @RequestBody ApplicableConditionsRequest request) {
        String token = authentication.getCredentials().toString();
        ApplicableConditionsResponse response = applicableConditionsService.submitApplicableConditions(
            projectId,
            request,
            jwtUtil.extractCompanyId(token),
            jwtUtil.extractRole(token),
            jwtUtil.extractUsername(token));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
        }

        @PutMapping("/{projectId}/applicable-conditions")
        @PreAuthorize("hasRole('operator')")
        @SecurityRequirement(name = "bearerAuth")
        @Operation(summary = "適用条件情報更新", description = "指定されたプロジェクトの適用条件情報を更新します。")
        public ResponseEntity<ApplicableConditionsResponse> updateApplicableConditions(Authentication authentication,
            @PathVariable String projectId,
            @Valid @RequestBody ApplicableConditionsRequest request) {
        String token = authentication.getCredentials().toString();
        ApplicableConditionsResponse response = applicableConditionsService.updateApplicableConditions(
            projectId,
            request,
            jwtUtil.extractCompanyId(token),
            jwtUtil.extractRole(token),
            jwtUtil.extractUsername(token));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
        }

        @GetMapping("/{projectId}/applicable-conditions")
        @PreAuthorize("hasRole('operator')")
        @SecurityRequirement(name = "bearerAuth")
        @Operation(summary = "適用条件情報取得", description = "指定されたプロジェクトの適用条件情報を取得します。")
        public ResponseEntity<ApplicableConditionsResponse> getApplicableConditions(Authentication authentication,
            @PathVariable String projectId) {
        String token = authentication.getCredentials().toString();
        ApplicableConditionsResponse response = applicableConditionsService.getApplicableConditions(
            projectId,
            jwtUtil.extractCompanyId(token),
            jwtUtil.extractRole(token),
            jwtUtil.extractUsername(token));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
        }
    }
