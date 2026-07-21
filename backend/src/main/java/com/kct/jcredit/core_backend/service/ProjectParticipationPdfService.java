package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002FileUploadResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;

import io.jsonwebtoken.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002JoinProjectRequest;

public interface ProjectParticipationPdfService {

    // Orchestrator PDF pipeline — dipanggil dari afterCommit
    // return s3Key → disimpan ke participating_project.s3_key_pdf_membership
    String createAndUpload(EnR002EquipmentRequest equipment,
                           EnR002ConfirmationItemRequest confirmationItems,
                           EnR002SustainabilityCommitmentRequest sustainabilityCommitment,
                           ParticipatingProject participation);

    // Generate presigned URL untuk download — dipanggil dari endpoint GET /pdf
    String generatePresignedUrl(String s3Key);
}
