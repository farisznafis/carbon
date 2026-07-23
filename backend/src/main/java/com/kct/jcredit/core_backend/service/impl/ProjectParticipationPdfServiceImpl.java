package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.service.ProjectParticipationPdfService;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;

import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectParticipationPdfServiceImpl
        implements ProjectParticipationPdfService {

    @Value("${app.s3.bucket:}")
    private String bucketName;

    private final S3Client s3Client; // ← hanya S3Client, tidak perlu S3Presigner dulu

    @Override
    public String createAndUpload(
            EnR002EquipmentRequest equipment,
            EnR002ConfirmationItemRequest confirmationItems,
            EnR002SustainabilityCommitmentRequest sustainabilityCommitment,
            ParticipatingProject participation) {

        // TODO: full implementation besok
        log.warn("PDF generation not yet implemented. participationId={}",
                participation.getId());
        return null;
    }

    @Override
    public String generatePresignedUrl(String s3Key) {

        // TODO: ganti dengan S3Presigner setelah bean ditambahkan
        log.warn("generatePresignedUrl not yet implemented. s3Key={}", s3Key);
        return null;
    }
}