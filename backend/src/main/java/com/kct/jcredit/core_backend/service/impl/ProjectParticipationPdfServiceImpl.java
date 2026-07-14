package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.dto.response.details.ProjectDetailResponse;
import com.kct.jcredit.core_backend.dto.response.details.SiteResponse;
import com.kct.jcredit.core_backend.dto.response.details.ValidationErrorDetail;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FileItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002JoinProjectRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002SustainabilityCommitmentResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectJoinResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectInfoResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectListInfoResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectPdfDownloadResponse;
import com.kct.jcredit.core_backend.dto.response.SiteListResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.EnR002SustainabilityCommitment;
import com.kct.jcredit.core_backend.entity.ConfirmationItems;
import com.kct.jcredit.core_backend.entity.EnR002ConfirmationItems;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.exception.BulkValidationException;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.service.ParticipationProjectService;
import com.kct.jcredit.core_backend.service.ProjectService;
import com.kct.jcredit.core_backend.service.SiteService;
import com.kct.jcredit.core_backend.service.ParticipationProjectValidationService;
import com.kct.jcredit.core_backend.service.ProjectParticipationPdfService;
import com.kct.jcredit.core_backend.service.EnR002FileUploadService;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002ApplicationConfirmationRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002SustainabilityCommitmentRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingProjectRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;

import java.time.ZoneId;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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