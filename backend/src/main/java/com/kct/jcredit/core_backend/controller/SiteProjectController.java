package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentRegisterResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyMethodologyValidateRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002FileResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002SustainabilityCommitmentResponse;
import com.kct.jcredit.core_backend.dto.response.MemberTermsInfoResponse;
import com.kct.jcredit.core_backend.dto.response.MyParticipationResponse;
import com.kct.jcredit.core_backend.dto.response.ParticipationReviewHistoryResponse;
import com.kct.jcredit.core_backend.dto.response.PdfDownloadResponse;
import com.kct.jcredit.core_backend.dto.request.EnR002JoinProjectRequest;
import com.kct.jcredit.core_backend.dto.response.ProjectJoinResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectPdfDownloadResponse;
import com.kct.jcredit.core_backend.dto.response.SiteListResponse;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.service.ParticipationProjectService;
import com.kct.jcredit.core_backend.service.EnR002FileUploadService;
import com.kct.jcredit.core_backend.service.ParticipationProjectService;
import com.kct.jcredit.core_backend.service.ParticipationProjectValidationService;
import com.kct.jcredit.core_backend.service.ProjectParticipationPdfService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.*;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/site/project")
@Tag(name = "拠点プロジェクト設備", description = "拠点プロジェクトの設備登録、ファイルアップロード、入力検証を管理するAPIです。")
public class SiteProjectController {

    private final JwtUtil jwtUtil;
    private final ParticipationProjectService participationService;
    private final ParticipationProjectValidationService validationService;
    private final EnR002FileUploadService  fileUploadSolarService;
    private final ProjectParticipationPdfService pdfService;

    public SiteProjectController(JwtUtil jwtUtil,
                                 ParticipationProjectService participationService,
                                 ParticipationProjectValidationService validationService,
                                 EnR002FileUploadService fileUploadSolarService,
                                 ProjectParticipationPdfService pdfService) {
        this.jwtUtil = jwtUtil;
        this.participationService = participationService;
        this.validationService = validationService;
        this.fileUploadSolarService = fileUploadSolarService;
        this.pdfService = pdfService;
    }
    // ─────────────────────────────────────────────────────────────
    // FILE UPLOAD (reuse FileUploadSolarService)
    // ─────────────────────────────────────────────────────────────

    @PostMapping(value = "/sites/{siteId}/files/upload",
             consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('master','user')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<EnR002FileResponse> uploadFile(
            Authentication authentication,
            @PathVariable String siteId,
            @RequestParam("fileType") String fileType,
            @RequestParam("file") MultipartFile file) {

        String uploadedBy = jwtUtil.extractUsername(authentication.getCredentials().toString());
        EquipmentFileType fileTypeEnum = EquipmentFileType.fromValue(fileType);

        EnR002FileResponse response = fileUploadSolarService
                .uploadFile(siteId, fileTypeEnum, uploadedBy, file);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    // ─────────────────────────────────────────────────────────────
    // VALIDATE
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/equipment/validate")
    @PreAuthorize("hasRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BaseResponse> validateEquipment(
            @RequestBody EnR002EquipmentRequest request) {
        validationService.validateEquipment(request);
        return ResponseEntity.ok(BaseResponse.success("入力内容に問題ありません。"));
    }

    @PostMapping("/confirmation-items/validate")
    @PreAuthorize("hasRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BaseResponse> validateConfirmationItems(
            @RequestBody EnR002ConfirmationItemRequest request) {
        validationService.validateConfirmationItems(request);
        return ResponseEntity.ok(BaseResponse.success("入力内容に問題ありません。"));
    }

    @GetMapping("/sites/{siteId}/sustainability-commitment/default")
    @PreAuthorize("hasRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<EnR002SustainabilityCommitmentResponse> getSustainabilityCommitmentDefault(
            @PathVariable String siteId) {
        return ResponseEntity.ok(
                participationService.getSustainabilityCommitmentDefault(siteId));
    }

    @PostMapping("/sustainability-commitment/validate")
    @PreAuthorize("hasRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BaseResponse> validateSustainabilityCommitment(
            @RequestBody EnR002SustainabilityCommitmentRequest request) {
        validationService.validateSustainabilityCommitment(request);
        return ResponseEntity.ok(BaseResponse.success("入力内容に問題ありません。"));
    }

    @PostMapping("/final/validate")
    @PreAuthorize("hasRole('master')")
    @SecurityRequirement(name = "bearerAuth")
   public ResponseEntity<BaseResponse> validateFinalAgreement(
            @RequestBody EnR002FinalAgreementRequest request) {
        validationService.validateFinalAgreement(request);
        return ResponseEntity.ok(BaseResponse.success("入力内容に問題ありません。"));
    }
    // ─────────────────────────────────────────────────────────────
    // SUBMIT — join project
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/projects/{projectId}/join-company") 
    @PreAuthorize("hasRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProjectJoinResponse> joinProject(
            Authentication authentication,
            @PathVariable String projectId,
            @RequestBody EnR002JoinProjectRequest request) {

        String  companyId = extractCompanyId(authentication);
        Integer userId    = extractUserId(authentication);

        ProjectJoinResponse response = participationService
                .submitParticipation(projectId, companyId, userId, request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    // ─────────────────────────────────────────────────────────────
    // JOIN SITE- connect site to already joined project
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/projects/{projectId}/{siteId}/join-site") 
    @PreAuthorize("hasRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProjectJoinResponse> joinProjectSite(
            Authentication authentication,
            @PathVariable String projectId,
            @PathVariable String siteId) {

        String  companyId = extractCompanyId(authentication);
        Integer userId    = extractUserId(authentication);

        ProjectJoinResponse response = participationService
                .joinSite(projectId, companyId, userId, siteId);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — JWT helpers
    // ─────────────────────────────────────────────────────────────

    private String extractCompanyId(Authentication auth) {
        return jwtUtil.extractCompanyId(auth.getCredentials().toString());
    }

    private String extractUsername(Authentication auth) {
        return jwtUtil.extractUsername(auth.getCredentials().toString());
    }

    private Integer extractUserId(Authentication auth) {
        return jwtUtil.extractUserId(auth.getCredentials().toString());
    }

    // ─────────────────────────────────────────────────────────────
    // PDF SERVICES
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/{projectId}/membership-pdf")
    @PreAuthorize("hasAnyRole('admin','master','user') or( hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PROJECT_VIEW.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "入会届PDFダウンロード（自社用）",
            description = "プロジェクトIDのみで、自社の入会届PDFダウンロードURLを取得します。")
    public ResponseEntity<ProjectPdfDownloadResponse> getMembershipPdfByCompany(
            Authentication authentication,
            @PathVariable String projectId) {

        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());
        return ResponseEntity.ok(participationService.getMembershipPdfByCompany(projectId, companyId));
    }

    @PostMapping("/generate/{participationId}")
    @PreAuthorize("hasAnyRole('admin','operator')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<String> generate(@PathVariable Integer participationId) {
        log.warn("generateAndProcess を手動実行します。participationId={}", participationId);

        try {
            pdfService.generateAndProcess(participationId);
            return ResponseEntity.ok("PDF生成処理が完了しました。participationId=" + participationId);
        } catch (Exception e) {
            log.error("PDF生成失敗", e);
            return ResponseEntity.internalServerError()
                    .body("エラー: " + e.getMessage());
        }
    }

    @GetMapping("/presigned-url")
    @PreAuthorize("hasAnyRole('admin','operator')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<String> presignedUrl(@RequestParam String s3Key) {
        log.warn("[DEBUG] Presigned URL を手動生成します。s3Key={}", s3Key);

        try {
            String url = pdfService.generatePresignedUrl(s3Key);
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            log.error("[DEBUG] Presigned URL生成失敗", e);
            return ResponseEntity.internalServerError()
                    .body("エラー: " + e.getMessage());
        }
    }

    @PostMapping("/generate-and-get-url/{participationId}")
    @PreAuthorize("hasAnyRole('admin','operator')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<String> generateAndGetUrl(
            @PathVariable Integer participationId,
            @RequestParam String s3Key) {

        try {
            pdfService.generateAndProcess(participationId);
            String url = pdfService.generatePresignedUrl(s3Key);
            log.warn("生成完了。ブラウザで以下URLを開いてPDFを確認してください: {}", url);
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            log.error("[DEBUG] end-to-endテスト失敗", e);
            return ResponseEntity.internalServerError()
                    .body("エラー: " + e.getMessage());
        }
    }

}
