package com.kct.jcredit.core_backend.controller;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;

import com.kct.jcredit.core_backend.dto.request.UpdateProjectStatusRequest;
import com.kct.jcredit.core_backend.dto.request.CreateProjectRequest;
import com.kct.jcredit.core_backend.dto.request.UpdateProjectRequest;
import com.kct.jcredit.core_backend.dto.request.ProjectUploadDocumentRequest;
import com.kct.jcredit.core_backend.dto.response.CreateProjectResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectListInfoResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectUploadDocumentResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectMonitoringResponse;
import com.kct.jcredit.core_backend.dto.response.UpdateProjectStatusResponse;


import com.kct.jcredit.core_backend.service.ProjectService;
import com.kct.jcredit.core_backend.service.FileUploadSolarService;
import com.kct.jcredit.core_backend.service.SiteProjectService;
import com.kct.jcredit.core_backend.service.ProjectMonitoringService;
import com.kct.jcredit.core_backend.service.ProjectUploadDocumentService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;


@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
@Validated
public class ProjectController {

    private final ProjectService projectService;
    private final JwtUtil jwtUtil;
    private final ProjectUploadDocumentService projectUploadDocumentService;
    private final ProjectMonitoringService projectMonitoringService;

    @PostMapping
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CreateProjectResponse> createProject(Authentication authentication,
            @Valid @RequestBody CreateProjectRequest request) {
            
        request.setOperatorCompanyId(jwtUtil.extractCompanyId(authentication.getCredentials().toString()));
        request.setCreatedBy(jwtUtil.extractUsername(authentication.getCredentials().toString()));
        CreateProjectResponse response = projectService.createProject(request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    //update project details
    @PatchMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('operator','master','user')") // allow both operator and master to update project details
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProjectResponse> updateProject(Authentication authentication,
            @PathVariable Integer projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
            
        request.setId(projectId);
        request.setCompanyId(jwtUtil.extractCompanyId(authentication.getCredentials().toString()));
        request.setProcessedBy(jwtUtil.extractUsername(authentication.getCredentials().toString()));
        ProjectResponse response = projectService.updateProject(request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

     //update project details
    @PatchMapping("/{projectId}/status")
    @PreAuthorize("hasAnyRole('operator','master')") // allow both operator and master to update project details
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UpdateProjectStatusResponse> updateProjectStatus(Authentication authentication,
            @PathVariable Integer projectId,
            @Valid @RequestBody UpdateProjectStatusRequest request) {
                String newStatus = request.getProjectStatus();
        UpdateProjectStatusResponse response = projectService.updateProjectStatus(newStatus, projectId);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    //get project details
    @GetMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('operator','master','user')") 
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProjectResponse> getProject(Authentication authentication,
            @PathVariable Integer projectId) {
        
        ProjectResponse response = projectService.getProject(projectId, jwtUtil.extractCompanyId(authentication.getCredentials().toString()));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }  

    //5.4 upload document
    @PostMapping(value="/{projectId}/document/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    public ProjectUploadDocumentResponse uploadApprovalDocuments(
            Authentication authentication, 
            @PathVariable String projectId,
            @RequestPart(value = "プロジェクト登録証", required = true) MultipartFile fileRegistrationCert,
            @RequestPart(value = "妥当性報告書", required = true) MultipartFile fileValidationReport,
            @RequestPart(value = "検証報告書", required = true) MultipartFile fileVerificationReport
    ) {
        String token = (String) authentication.getCredentials();
        String username = jwtUtil.extractUsername(token);
        return projectUploadDocumentService.processUpload(
                projectId, 
                fileRegistrationCert, 
                fileValidationReport, 
                fileVerificationReport, 
                username
        );
    }

    @GetMapping("/{projectId}/document/list")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    public ProjectUploadDocumentResponse getDocumentList(@PathVariable String projectId) {
        return projectUploadDocumentService.getDocumentsByProjectId(projectId);
    }

    @PatchMapping("/{projectId}/document/status")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    public ProjectUploadDocumentResponse updateDocumentStatus(
            @PathVariable String projectId,
            @Valid @RequestBody ProjectUploadDocumentRequest request
    ) {
        return projectUploadDocumentService.updateStatus(projectId, request);
    }

    //list projects with filters
    @GetMapping
    @PreAuthorize("hasRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProjectListInfoResponse> listProjects(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "ページは1以上で入力してください。") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "表示件数は1以上で入力してください。") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String methodology,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate updatedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate updatedTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "desc") @Pattern(regexp = "^(asc|desc)$", message = "ソート順はascまたはdescで入力してください。") String sortOrder
    ) {
        ProjectListInfoResponse response = projectService.findByFilter(
                page,
                limit,
                status,
                methodology,
                createdFrom,
                createdTo,
                updatedFrom,
                updatedTo,
                search,
                sort,
                sortOrder
        );
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    //5.5 project monitoring details
    @GetMapping("/{projectId}/monitoring")
    @PreAuthorize("hasRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProjectMonitoringResponse> getProjectMonitoringDetails(
            @PathVariable String projectId) {
        ProjectMonitoringResponse response = projectMonitoringService.getProjectDetail(projectId);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }


}