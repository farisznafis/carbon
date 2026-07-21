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

public interface EnR002FileUploadService {

    // Upload file evidence equipment ke S3 temp
    EnR002FileUploadResponse uploadFile(String siteId,
                                        EquipmentFileType fileType,
                                        String uploadedBy,
                                        MultipartFile file);

    // projectId dihapus — equipment cuma butuh siteId + equipmentId
    String moveTempToPermanent(String tempS3Key,
                               String siteId,
                               Integer equipmentId);
}