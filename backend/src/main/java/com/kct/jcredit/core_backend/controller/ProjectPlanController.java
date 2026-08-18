package com.kct.jcredit.core_backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.kct.jcredit.core_backend.dto.response.CorporationMasterResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectMasterResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ApplicableConditionsRequest;
import com.kct.jcredit.core_backend.dto.projectplan.OtherOperatorProjectsRequest;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectApplicationRequest;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFileResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanBatchProgressResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanProgressResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.service.projectplan.ProjectPlanAutofillService;
import com.kct.jcredit.core_backend.service.projectplan.ProjectPlanFormService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Project plan (プロジェクト計画書).
 *
 * <p>Every page is served by one registry-driven set of endpoints, so adding a
 * page needs a registry entry and its DTO rather than a new controller. Pages
 * are atomic — stored only once they validate in full — which is what lets
 * {@code /progress} say where an interrupted operator should resume.
 */
@RestController
@RequestMapping("/projectplan")
@Tag(name = "プロジェクト計画書",
        description = "プロジェクト計画書の各ページの取得・保存、入力状況、および自動入力情報を管理するAPIです。")
@RequiredArgsConstructor
public class ProjectPlanController {

    /** The page slug backing the deprecated /application endpoints. */
    private static final String APPLICATION_SLUG = "application";

    private final ProjectPlanFormService projectPlanFormService;
    private final ProjectPlanAutofillService projectPlanAutofillService;
    private final JwtUtil jwtUtil;

    // --- Autofill (master data from PostgreSQL) ---

    @GetMapping("/autofill")
    @PreAuthorize("hasRole('admin') or (hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PROJECT_READ.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "法人マスタ自動入力情報取得",
            description = "プロジェクト計画書入力用の法人マスタ情報を取得します。")
    public ResponseEntity<CorporationMasterResponse> getCorporationMaster(
            Authentication authentication) {
        String token = token(authentication);
        return respond(projectPlanAutofillService.getCorporationMaster(
                jwtUtil.extractCompanyId(token),
                jwtUtil.extractRole(token),
                jwtUtil.extractUsername(token)));
    }

    @GetMapping("/autofill/{projectId}")
    @PreAuthorize("hasRole('admin') or (hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PROJECT_READ.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "プロジェクトマスタ自動入力情報取得",
            description = "指定されたプロジェクトの計画書入力用マスタ情報を取得します。")
    public ResponseEntity<ProjectMasterResponse> getProjectMaster(
            Authentication authentication, @PathVariable String projectId) {
        String token = token(authentication);
        return respond(projectPlanAutofillService.getProjectMaster(
                projectId,
                jwtUtil.extractCompanyId(token),
                jwtUtil.extractRole(token),
                jwtUtil.extractUsername(token)));
    }

    // --- Registry-driven pages ---

    @GetMapping("/{projectId}/forms/{formSlug}")
    @PreAuthorize("hasRole('admin') or (hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PROJECT_READ.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "計画書フォーム取得",
            description = "指定ページを取得します。SQL由来の項目は readOnlyFields、"
                    + "有/無で必須が変わる項目は conditionalFields で返却します。")
    public ResponseEntity<ProjectPlanFormResponse> getForm(Authentication authentication,
            @PathVariable String projectId,
            @PathVariable String formSlug,
            @RequestParam(name = "methodology", required = false) Integer methodology) {
        return respond(loadForm(authentication, projectId, formSlug, methodology));
    }

    @PutMapping("/{projectId}/forms/{formSlug}")
    @PreAuthorize("hasRole('admin') or (hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PROJECT_UPDATE.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "計画書フォーム保存",
            description = "ページ全体を検証し、すべて有効な場合のみ保存します。"
                    + "検証エラー時は何も保存されません。")
    public ResponseEntity<ProjectPlanFormResponse> saveForm(Authentication authentication,
            @PathVariable String projectId,
            @PathVariable String formSlug,
            @RequestParam(name = "methodology", required = false) Integer methodology,
            @RequestBody JsonNode body) {
        return respond(storeForm(authentication, projectId, formSlug, methodology, body));
    }

    @PostMapping(value = "/{projectId}/forms/{formSlug}/files",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('admin') or (hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PROJECT_UPDATE.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "計画書添付ファイル仮アップロード",
            description = "ファイルを一時領域に保存し、s3Key を返します。返却された s3Key を"
                    + "該当項目に設定してページを保存すると、正式な保存先へ移動します。"
                    + "ページが検証エラーになった場合、ファイルは移動されず一時領域で失効します。")
    public ResponseEntity<ProjectPlanFileResponse> uploadFormFile(
            Authentication authentication,
            @PathVariable String projectId,
            @PathVariable String formSlug,
            @RequestParam(name = "fieldName") String fieldName,
            @RequestPart("file") MultipartFile file) {
        String token = token(authentication);
        return respond(projectPlanFormService.uploadFormFile(
                projectId,
                formSlug,
                fieldName,
                file,
                jwtUtil.extractCompanyId(token),
                jwtUtil.extractRole(token),
                jwtUtil.extractUsername(token)));
    }

    @GetMapping("/{projectId}/progress")
    @PreAuthorize("hasRole('admin') or (hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PROJECT_READ.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "計画書入力状況取得",
            description = "全ページの入力状況と、次に入力すべきページを返します。")
    public ResponseEntity<ProjectPlanProgressResponse> getProgress(
            Authentication authentication, @PathVariable String projectId) {
        String token = token(authentication);
        return respond(projectPlanFormService.getProgress(
                projectId,
                jwtUtil.extractCompanyId(token),
                jwtUtil.extractRole(token),
                jwtUtil.extractUsername(token)));
    }

    @GetMapping("/progress")
    @PreAuthorize("hasRole('admin') or (hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PROJECT_READ.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "計画書入力状況一括取得",
            description = "複数プロジェクトの入力状況をまとめて取得します。一覧画面用。"
                    + "参照権限のないプロジェクトは結果から除外されます。")
    public ResponseEntity<ProjectPlanBatchProgressResponse> getProgressForProjects(
            Authentication authentication,
            @RequestParam(name = "projectIds") List<String> projectIds) {
        String token = token(authentication);
        return respond(projectPlanFormService.getProgressForProjects(
                projectIds,
                jwtUtil.extractCompanyId(token),
                jwtUtil.extractRole(token),
                jwtUtil.extractUsername(token)));
    }

    // --- Deprecated: the frontend still calls these ---
    //
    // Thin aliases for the application page. POST and PUT were already
    // behaviourally identical — both fully validated and overwrote the item —
    // so both map onto the single save path. Remove once the frontend moves to
    // /forms/application.

    @Deprecated(forRemoval = true)
    @GetMapping("/{projectId}/application")
    @PreAuthorize("hasRole('admin') or (hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PROJECT_READ.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "プロジェクト申請情報取得（非推奨）",
            description = "GET /{projectId}/forms/application を使用してください。",
            deprecated = true)
    public ResponseEntity<ProjectPlanFormResponse> getProjectApplication(
            Authentication authentication, @PathVariable String projectId) {
        return respond(loadForm(authentication, projectId, APPLICATION_SLUG, null));
    }

    @Deprecated(forRemoval = true)
    @PostMapping("/{projectId}/application")
    @PreAuthorize("hasRole('admin') or (hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PROJECT_UPDATE.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "プロジェクト申請情報登録（非推奨）",
            description = "PUT /{projectId}/forms/application を使用してください。",
            deprecated = true)
    public ResponseEntity<ProjectPlanFormResponse> submitProjectApplication(
            Authentication authentication,
            @PathVariable String projectId,
            @RequestBody JsonNode body) {
        return respond(storeForm(authentication, projectId, APPLICATION_SLUG, null, body));
    }

    @Deprecated(forRemoval = true)
    @PutMapping("/{projectId}/application")
    @PreAuthorize("hasRole('admin') or (hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PROJECT_UPDATE.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "プロジェクト申請情報更新（非推奨）",
            description = "PUT /{projectId}/forms/application を使用してください。",
            deprecated = true)
    public ResponseEntity<ProjectPlanFormResponse> updateProjectApplication(
            Authentication authentication,
            @PathVariable String projectId,
            @RequestBody JsonNode body) {
        return respond(storeForm(authentication, projectId, APPLICATION_SLUG, null, body));
    }

    // --- helpers ---

    private ProjectPlanFormResponse loadForm(
            Authentication authentication, String projectId, String formSlug, Integer methodology) {
        String token = token(authentication);
        return projectPlanFormService.getForm(
                projectId,
                formSlug,
                methodology,
                jwtUtil.extractCompanyId(token),
                jwtUtil.extractRole(token),
                jwtUtil.extractUsername(token));
    }

    private ProjectPlanFormResponse storeForm(
            Authentication authentication,
            String projectId,
            String formSlug,
            Integer methodology,
            JsonNode body) {
        String token = token(authentication);
        return projectPlanFormService.saveForm(
                projectId,
                formSlug,
                methodology,
                body,
                jwtUtil.extractCompanyId(token),
                jwtUtil.extractRole(token),
                jwtUtil.extractUsername(token));
    }

    private static String token(Authentication authentication) {
        return authentication.getCredentials().toString();
    }

    private static <T extends BaseResponse> ResponseEntity<T> respond(T response) {
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }
}
