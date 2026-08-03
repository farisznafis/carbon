package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002FileResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectPdfDownloadResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;

import io.jsonwebtoken.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002JoinProjectRequest;

public interface ProjectParticipationPdfService {
    
    void generateAndProcess(Integer participationId);
    String generatePresignedUrl(String s3Key);
    ProjectPdfDownloadResponse getPdfDownloadUrl(Integer participationId, String companyId); // ← ganti tipe return
}