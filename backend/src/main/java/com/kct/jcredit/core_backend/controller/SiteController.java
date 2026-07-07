package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.request.CreateSiteRequest;
import com.kct.jcredit.core_backend.dto.request.UpdateSiteRequest;
import com.kct.jcredit.core_backend.dto.response.CreateSiteResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectListInfoResponse;
import com.kct.jcredit.core_backend.dto.response.EquipmentSolarRegisterResponse;
import com.kct.jcredit.core_backend.dto.response.SiteInfoResponse;
import com.kct.jcredit.core_backend.dto.response.SiteListResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.service.ProjectService;
import com.kct.jcredit.core_backend.service.SiteService;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import com.kct.jcredit.core_backend.util.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/site")
@Tag(name = "拠点", description = "拠点の作成、更新、取得、関連プロジェクトと設備情報を管理するAPIです。")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    private final ProjectService projectService;
    private final JwtUtil jwtUtil;

    @GetMapping
    @PreAuthorize("hasAnyRole('master')")
    @SecurityRequirement(name = "bearerAuth") 
    @Operation(summary = "拠点一覧取得", description = "指定された会社に所属する拠点の一覧を取得します。")
    public ResponseEntity<?> getAllSitesByCompanyId(@RequestParam String companyId) {
        SiteListResponse response = siteService.getSitesByCompanyId(companyId);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @GetMapping("/{siteId}")
    @PreAuthorize("hasAnyRole('master','user')")
    @SecurityRequirement(name = "bearerAuth") 
    @Operation(summary = "拠点情報取得", description = "指定された拠点の情報を取得します。")
    public ResponseEntity<SiteInfoResponse> getSite(@PathVariable String siteId) {

        SiteInfoResponse response = siteService.getSiteById(Integer.parseInt(siteId));

        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @GetMapping("/{siteId}/project")
    @PreAuthorize("hasAnyRole('master','user')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "拠点に紐付くプロジェクト一覧取得", description = "指定された拠点に紐付くプロジェクトの一覧を取得します。")
    public ResponseEntity<ProjectListInfoResponse> getProjectsBySiteId(Authentication authentication, @PathVariable Integer siteId) {
        String token = authentication.getCredentials().toString();
        ProjectListInfoResponse response = projectService.getProjectsBySiteIdAndCompanyId(siteId, jwtUtil.extractCompanyId(token));

        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('master')")
    @SecurityRequirement(name = "bearerAuth") 
    @Operation(summary = "拠点作成", description = "新しい拠点を作成します。")
    public ResponseEntity<CreateSiteResponse> createSite(
            Authentication authentication,
            @RequestBody @Valid CreateSiteRequest request) {
        CreateSiteResponse response;

        response = siteService.createSite(request, jwtUtil.extractCompanyId(authentication.getCredentials().toString()));

        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/validate-create")
    @PreAuthorize("hasAnyRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Validate site creation", description = "Validate site creation request without saving data.")
    public ResponseEntity<BaseResponse> validateCreateSite(
            Authentication authentication,
            @RequestBody @Valid CreateSiteRequest request) {
        BaseResponse response = siteService.validateCreateSite(
                request,
                jwtUtil.extractCompanyId(authentication.getCredentials().toString()));

        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

     @PatchMapping("/{siteId}")
    @PreAuthorize("hasAnyRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "拠点情報更新", description = "指定された拠点の情報を更新します。")
    public ResponseEntity<SiteInfoResponse> updateSite(
            Authentication authentication,
            @PathVariable String siteId,
            @RequestBody @Valid UpdateSiteRequest request) {
        
        // Extract companyId from JWT token
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());
        
        // Update site (service will verify the site belongs to this company)
        SiteInfoResponse response = siteService.updateSite(siteId, companyId, request);
        
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/validate-update/{siteId}")
    @PreAuthorize("hasAnyRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Validate site update", description = "Validate site update request without saving data.")
    public ResponseEntity<BaseResponse> validateUpdateSite(
            Authentication authentication,
            @PathVariable String siteId,
            @RequestBody @Valid UpdateSiteRequest request) {
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());

        BaseResponse response = siteService.validateUpdateSite(siteId, companyId, request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @GetMapping("/{siteId}/{projectId}/equipment")
    @PreAuthorize("hasAnyRole('master','user')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "拠点プロジェクト設備情報取得", description = "指定された拠点とプロジェクトに登録された設備情報を取得します。")
    public ResponseEntity<EquipmentSolarRegisterResponse> getSiteProjectEquipment(
            Authentication authentication,
            @PathVariable Integer siteId,
            @PathVariable Integer projectId) {
        String token = (String) authentication.getCredentials();

        EquipmentSolarRegisterResponse response = siteService.getSiteProjectEquipment(
                siteId,
                projectId,
                jwtUtil.extractCompanyId(token),
                jwtUtil.extractRole(token),
                jwtUtil.extractUsername(token));

        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }
}
