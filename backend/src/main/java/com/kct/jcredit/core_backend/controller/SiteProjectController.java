package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentRegisterResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyMethodologyValidateRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentFileResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002FileResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002SustainabilityCommitmentResponse;
import com.kct.jcredit.core_backend.dto.request.EnR002JoinProjectRequest;
import com.kct.jcredit.core_backend.dto.response.ProjectJoinResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectPdfDownloadResponse;
import com.kct.jcredit.core_backend.dto.response.SiteListResponse;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.service.ParticipationProjectService;
import com.kct.jcredit.core_backend.service.EnR002FileUploadService;
import com.kct.jcredit.core_backend.service.ParticipationProjectService;
import com.kct.jcredit.core_backend.service.ParticipationProjectValidationService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.*;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/site/project")
@Tag(name = "拠点プロジェクト設備", description = "拠点プロジェクトの設備登録、ファイルアップロード、入力検証を管理するAPIです。")
public class SiteProjectController {

    private final JwtUtil jwtUtil;
    private final ParticipationProjectService participationService;
    private final ParticipationProjectValidationService validationService;
    private final EnR002FileUploadService  fileUploadSolarService;

    public SiteProjectController(JwtUtil jwtUtil,
                                 ParticipationProjectService participationService,
                                 ParticipationProjectValidationService validationService,
                                 EnR002FileUploadService fileUploadSolarService) {
        this.jwtUtil = jwtUtil;
        this.participationService = participationService;
        this.validationService = validationService;
        this.fileUploadSolarService = fileUploadSolarService;
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
            @PathVariable Integer siteId,
            @RequestParam("fileType") String fileType,
            @RequestParam("file") MultipartFile file) {

        String uploadedBy = jwtUtil.extractUsername(authentication.getCredentials().toString());
        EquipmentFileType fileTypeEnum = EquipmentFileType.fromValue(fileType);

        EnR002FileResponse response = fileUploadSolarService
                .uploadFile(siteId, fileTypeEnum, uploadedBy, file);
        return new ResponseEntity<EnR002FileResponse>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    // ─────────────────────────────────────────────────────────────
    // VALIDATE — per halaman (stateless, tidak simpan apapun)
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/equipment/validate")
    @PreAuthorize("hasRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BaseResponse> validateEquipment(
            @RequestBody @Valid EnR002EquipmentRequest request) {
        validationService.validateEquipment(request);
        return ResponseEntity.ok(BaseResponse.success("入力内容に問題ありません。"));
    }

    @PostMapping("/confirmation-items/validate")
    @PreAuthorize("hasRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BaseResponse> validateConfirmationItems(
            @RequestBody @Valid EnR002ConfirmationItemRequest request) {
        validationService.validateConfirmationItems(request);
        return ResponseEntity.ok(BaseResponse.success("入力内容に問題ありません。"));
    }

    @GetMapping("/sites/{siteId}/sustainability-commitment/default")
    @PreAuthorize("hasRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<EnR002SustainabilityCommitmentResponse> getSustainabilityCommitmentDefault(
            @PathVariable Integer siteId) {
        return ResponseEntity.ok(
                participationService.getSustainabilityCommitmentDefault(siteId));
    }

    @PostMapping("/sustainability-commitment/validate")
    @PreAuthorize("hasRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BaseResponse> validateSustainabilityCommitment(
            @RequestBody @Valid EnR002SustainabilityCommitmentRequest request) {
        validationService.validateSustainabilityCommitment(request);
        return ResponseEntity.ok(BaseResponse.success("入力内容に問題ありません。"));
    }

    @PostMapping("/final/validate")
    @PreAuthorize("hasRole('master')")
    @SecurityRequirement(name = "bearerAuth")
   public ResponseEntity<BaseResponse> validateFinalAgreement(
            @RequestBody @Valid EnR002FinalAgreementRequest request) {
        validationService.validateFinalAgreement(request);
        return ResponseEntity.ok(BaseResponse.success("入力内容に問題ありません。"));
    }
    // ─────────────────────────────────────────────────────────────
    // SUBMIT — join project (endpoint utama)
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/projects/{projectId}/join")
    @PreAuthorize("hasRole('master')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProjectJoinResponse> joinProject(
            Authentication authentication,
            @PathVariable Integer projectId,
            @RequestBody @Valid EnR002JoinProjectRequest request) {

        String  companyId = extractCompanyId(authentication);
        Integer userId    = extractUserId(authentication);

        ProjectJoinResponse response = participationService
                .submitParticipation(projectId, companyId, userId, request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    // ─────────────────────────────────────────────────────────────
    // PDF DOWNLOAD
    // ─────────────────────────────────────────────────────────────

    // @GetMapping("/{participationId}/pdf")
    // @PreAuthorize("hasRole('master')")
    // @SecurityRequirement(name = "bearerAuth")
    // public ResponseEntity<ProjectPdfDownloadResponse> downloadPdf(
    //         Authentication authentication,
    //         @PathVariable Integer participationId) {

    //     String companyId = extractCompanyId(authentication);
    //     ProjectPdfDownloadResponse response = participationService
    //             .getPdfDownloadUrl(participationId, companyId);

    //     return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(200));
    // }

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
}
