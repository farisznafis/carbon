package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.response.EquipmentSolarRegisterResponse;
import com.kct.jcredit.core_backend.dto.request.EquipmentItemSolarRequest;
import com.kct.jcredit.core_backend.dto.request.EquipmentRegisterSolarRequest;
import com.kct.jcredit.core_backend.dto.request.FinalAgreementValidateRequest;
import com.kct.jcredit.core_backend.dto.request.JigyoushaValidateRequest;
import com.kct.jcredit.core_backend.dto.request.TncValidateRequest;
import com.kct.jcredit.core_backend.dto.response.FileSolarUploadResponse;
import com.kct.jcredit.core_backend.dto.response.JigyoushaDefaultResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.service.SiteProjectService;
import com.kct.jcredit.core_backend.service.FileUploadSolarService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
public class SiteProjectController {

    private final JwtUtil jwtUtil;
    private final FileUploadSolarService fileUploadSolarService;
    private final SiteProjectService siteProjectService;

    public SiteProjectController(
            JwtUtil jwtUtil,
            FileUploadSolarService fileUploadSolarService,
            SiteProjectService equipmentSolarService) {
        this.jwtUtil = jwtUtil;
        this.fileUploadSolarService = fileUploadSolarService;
        this.siteProjectService = equipmentSolarService;
    }
    // upload file temp
        @PostMapping(value = "/{siteId}/{projectId}/files/upload",
             consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("hasAnyRole('master','user')")
        @SecurityRequirement(name = "bearerAuth")
        public ResponseEntity<FileSolarUploadResponse> uploadFile(
                Authentication authentication,
                @PathVariable Integer siteId,
                @PathVariable Integer projectId,
                @RequestParam("fileType") String fileType,
                @RequestParam("file") MultipartFile file) throws IOException {

            EquipmentFileType fileTypeEnum = EquipmentFileType.fromValue(fileType);
            String uploadedBy = jwtUtil.extractUsername(
                    authentication.getCredentials().toString());

            FileSolarUploadResponse response = fileUploadSolarService
                    .uploadFile(siteId, projectId, fileTypeEnum, uploadedBy, file);
            return ResponseEntity.ok(response);
        }

        // 5.2 register solar equipment
        @PostMapping("/{siteId}/{projectId}/solar/register")
        @PreAuthorize("hasAnyRole('master','user')")
        @SecurityRequirement(name = "bearerAuth")
        public ResponseEntity<EquipmentSolarRegisterResponse> registerEquipments(
                Authentication authentication,
                @PathVariable Integer siteId,
                @PathVariable Integer projectId,
                @RequestBody @Valid EquipmentRegisterSolarRequest request) {

            String token = authentication.getCredentials().toString();
            request.setProcessedBy(jwtUtil.extractUsername(token));

            EquipmentSolarRegisterResponse response = siteProjectService
                    .registerEquipments(siteId, projectId, jwtUtil.extractCompanyId(token), request);

            return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode())
        );
    }
    
        // Halaman 1a
        @PostMapping("/equipments/validate")
        @PreAuthorize("hasAnyRole('master','user')")
        @SecurityRequirement(name = "bearerAuth")
        public ResponseEntity<BaseResponse> validateEquipments(
                @RequestBody @Valid List<EquipmentItemSolarRequest> equipments) {
        siteProjectService.validateEquipmentsNotEmpty(equipments);
        siteProjectService.validateAllEquipments(equipments);
        return ResponseEntity.ok(BaseResponse.success("入力内容に問題ありません。"));
        }

        // Halaman 2
        @PostMapping("/tnc/validate")
        @PreAuthorize("hasAnyRole('master','user')")
        @SecurityRequirement(name = "bearerAuth")
        public ResponseEntity<BaseResponse> validateTnc(
                @RequestBody @Valid TncValidateRequest request) {
        siteProjectService.validateTnc(request);
        return ResponseEntity.ok(BaseResponse.success("入力内容に問題ありません。"));
        }

        // Halaman 3 — auto-fill
        @GetMapping("/{siteId}/{projectId}/jigyousha/default")
        @PreAuthorize("hasAnyRole('master','user')")
        @SecurityRequirement(name = "bearerAuth")
        public ResponseEntity<JigyoushaDefaultResponse> getJigyoushaDefault(
                @PathVariable Integer siteId,
                @PathVariable Integer projectId) {
        return ResponseEntity.ok(siteProjectService.getJigyoushaDefault(siteId, projectId));
        }

        // Halaman 3 — validate
        @PostMapping("/jigyousha/validate")
        @PreAuthorize("hasAnyRole('master','user')")
        @SecurityRequirement(name = "bearerAuth")
        public ResponseEntity<BaseResponse> validateJigyousha(
                @RequestBody @Valid JigyoushaValidateRequest request) {
        siteProjectService.validateJigyousha(request);
        return ResponseEntity.ok(BaseResponse.success("入力内容に問題ありません。"));
        }

        // Halaman 4
        @PostMapping("/final/validate")
        @PreAuthorize("hasAnyRole('master','user')")
        @SecurityRequirement(name = "bearerAuth")
        public ResponseEntity<BaseResponse> validateFinalAgreement(
                @RequestBody @Valid FinalAgreementValidateRequest request) {
        siteProjectService.validateFinalAgreement(request);
        return ResponseEntity.ok(BaseResponse.success("入力内容に問題ありません。"));
        }
}