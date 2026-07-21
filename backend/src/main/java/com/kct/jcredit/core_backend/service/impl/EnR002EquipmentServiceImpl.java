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
import com.kct.jcredit.core_backend.entity.EnR002ApplicationConfirmation;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.exception.BulkValidationException;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.service.EnR002EquipmentService;
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
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;


import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnR002EquipmentServiceImpl implements EnR002EquipmentService {

    private final SiteRepository                        siteRepository;
    private final SiteProjectRelationRepository          siteProjectRelationRepository;
    private final EnR002EquipmentRepository              equipmentRepository;
    private final EnR002EquipmentFileRepository          equipmentFileRepository;
    private final EnR002FileUploadService                fileUploadService;
    private final ParticipationProjectValidationService  validationService; 

    @Override
    @Transactional
    public EnR002EquipmentResponse addEquipment(Integer siteId,
                                                 String companyId,
                                                 EnR002EquipmentRequest request) {

        // 1. Guard check — site harus milik company yang login
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        ResponseConstants.MSG_SITE_NOT_FOUND));

        if (!site.getCompany().getCompanyId().equals(companyId))
            throw new CustomException(ResponseConstants.FORBIDDEN,
                    ResponseConstants.MSG_UNAUTHORIZED);

        // 2. Guard check — site harus sudah pernah join MINIMAL satu project
        //    (equipment tambahan hanya valid untuk site yang statusnya aktif berpartisipasi)
        boolean hasJoinedAnyProject = siteProjectRelationRepository
                .existsBySite_Id(siteId);
        if (!hasJoinedAnyProject)
            throw new CustomException(
                    "SITE_NOT_JOINED",
                    "このサイトはまだプロジェクトに参加していません。先に参加登録を行ってください。");

        // 3. Validasi field equipment — reuse logic yang sudah ada
        validationService.validateEquipment(request);

        // 4. Save Equipment
        EnR002Equipment saved = equipmentRepository.save(EnR002Equipment.builder()
                .site(site)
                .equipmentOpDate(request.getEquipmentOpDate())
                .nominalMaxOutput(request.getNominalMaxOutput())
                .pcsManufacturer(request.getPcsManufacturer())
                .pcsModel(request.getPcsModel())
                .pcsUnitCount(request.getPcsUnitCount())
                .deviceNo(request.getDeviceNo())
                .pcsStandby(request.getPcsStandby())
                .hasBattery(request.getHasBattery())
                .batteryMaker(request.getBatteryMaker())
                .batteryModel(request.getBatteryModel())
                .batteryCapacity(request.getBatteryCapacity())
                .totalInvestment(request.getTotalInvestment())
                .hasSubsidy(request.getHasSubsidy())
                .subsidyName(request.getSubsidyName())
                .subsidyAmount(request.getSubsidyAmount())
                .preProjectUnitPrice(request.getPreProjectUnitPrice())
                .build());

        // 5. Save EquipmentFile
        List<EnR002EquipmentFile> savedFiles = equipmentFileRepository.saveAll(
                request.getFiles().stream()
                        .map(f -> EnR002EquipmentFile.builder()
                                .equipment(saved)
                                .fileType(f.getFileType())
                                .fileId(f.getFileId())
                                .s3Key(f.getS3Key())
                                .build())
                        .collect(Collectors.toList()));

        // 6. Move file temp → permanent setelah commit
        //    Cari projectId untuk path S3 — ambil dari relation pertama site ini
        Integer projectId = siteProjectRelationRepository
                .findBySite_Id(siteId).stream()
                .findFirst()
                .map(r -> r.getProject().getId())
                .orElseThrow(() -> new CustomException(
                        "SITE_NOT_JOINED", "プロジェクト情報が見つかりません。"));

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        savedFiles.forEach(fileEntity -> {
                            try {
                                String permanentKey = fileUploadService.moveTempToPermanent(
                                        fileEntity.getS3Key(), siteId, saved.getId());
                                equipmentFileRepository.updateS3Key(
                                        fileEntity.getId(), permanentKey);
                            } catch (Exception e) {
                                log.error("S3 move failed. fileId={}, error={}",
                                        fileEntity.getFileId(), e.getMessage());
                                // TODO: retry table
                            }
                        });
                    }
                }
        );

        return toResponse(saved);
    }

    @Override
    public List<EnR002EquipmentResponse> getEquipmentsBySite(Integer siteId, String companyId) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        ResponseConstants.MSG_SITE_NOT_FOUND));

        if (!site.getCompany().getCompanyId().equals(companyId))
            throw new CustomException(ResponseConstants.FORBIDDEN,
                    ResponseConstants.MSG_UNAUTHORIZED);

        return equipmentRepository.findBySite_Id(siteId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private EnR002EquipmentResponse toResponse(EnR002Equipment e) {
        return EnR002EquipmentResponse.builder()
                .id(e.getId())
                .siteId(e.getSite().getId())
                .equipmentOpDate(e.getEquipmentOpDate())
                .nominalMaxOutput(e.getNominalMaxOutput())
                .pcsManufacturer(e.getPcsManufacturer())
                .pcsModel(e.getPcsModel())
                .pcsUnitCount(e.getPcsUnitCount())
                .deviceNo(e.getDeviceNo())
                .pcsStandby(e.getPcsStandby())
                .hasBattery(e.getHasBattery())
                .batteryMaker(e.getBatteryMaker())
                .batteryModel(e.getBatteryModel())
                .batteryCapacity(e.getBatteryCapacity())
                .totalInvestment(e.getTotalInvestment())
                .hasSubsidy(e.getHasSubsidy())
                .subsidyName(e.getSubsidyName())
                .subsidyAmount(e.getSubsidyAmount())
                .preProjectUnitPrice(e.getPreProjectUnitPrice())
                .build();
    }
    @Override
    public EnR002EquipmentResponse getEquipmentById(Integer siteId,
                                                    Integer equipmentId,
                                                    String companyId) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        ResponseConstants.MSG_SITE_NOT_FOUND));

        if (!site.getCompany().getCompanyId().equals(companyId))
            throw new CustomException(ResponseConstants.FORBIDDEN,
                    ResponseConstants.MSG_UNAUTHORIZED);

        EnR002Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "設備情報が見つかりません。"));

        if (!equipment.getSite().getId().equals(siteId))
            throw new CustomException(
                    ResponseConstants.NOT_FOUND, "設備情報が見つかりません。");

        return toResponse(equipment);
    }
}