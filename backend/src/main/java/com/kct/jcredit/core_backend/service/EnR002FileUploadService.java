package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002FileResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;

import io.jsonwebtoken.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002JoinProjectRequest;

public interface EnR002FileUploadService {

    // Upload file evidence equipment S3 temp
    EnR002FileResponse uploadFile(String siteId,
                                        EquipmentFileType fileType,
                                        String uploadedBy,
                                        MultipartFile file);

    String buildPermanentKey(String tempS3Key,
                             String siteId,
                             Integer equipmentId);

    // Copy tmp → permanent 
    void moveToPermanent(String tempS3Key, String permanentKey);

    // Hapus object permanent.
    void deletePermanentFile(String s3Key, Integer equipmentId);

    // buildPermanentKey + moveToPermanent dalam satu panggilan, untuk pemakaian
    // di luar transaksi yang tidak perlu mencatat key ke DB lebih dulu.
    String moveTempToPermanent(String tempS3Key, String siteId, Integer equipmentId);
}
