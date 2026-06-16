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

import com.kct.jcredit.core_backend.dto.response.CorporationMasterResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectMasterResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectApplicationResponse;
import com.kct.jcredit.core_backend.dto.request.ProjectApplicationRequest;
import com.kct.jcredit.core_backend.service.ProjectApplicationService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;


@RestController
@RequestMapping("/projectplan")
@RequiredArgsConstructor
public class ProjectPlanController {
    private final ProjectApplicationService projectApplicationService;
    private final JwtUtil jwtUtil;

        @GetMapping("/autofill")
        @PreAuthorize("hasRole('operator')")
        @SecurityRequirement(name = "bearerAuth")
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
        public ResponseEntity<ProjectMasterResponse> getProjectMaster(Authentication authentication,
                @PathVariable Integer projectId) {
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
        public ResponseEntity<ProjectApplicationResponse> submitProjectApplication(Authentication authentication,
            @PathVariable Integer projectId,
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
        public ResponseEntity<ProjectApplicationResponse> updateProjectApplication(Authentication authentication,
            @PathVariable Integer projectId,
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
        public ResponseEntity<ProjectApplicationResponse> getProjectApplication(Authentication authentication,
            @PathVariable Integer projectId) {
        String token = authentication.getCredentials().toString();
        ProjectApplicationResponse response = projectApplicationService.getProjectApplication(
            projectId,
            jwtUtil.extractCompanyId(token),
            jwtUtil.extractRole(token),
            jwtUtil.extractUsername(token));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
        }
    }
