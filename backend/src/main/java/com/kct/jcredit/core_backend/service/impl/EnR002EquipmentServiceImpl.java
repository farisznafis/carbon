package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentUpdateRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FileItemRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentFileResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.service.EnR002EquipmentService;
import com.kct.jcredit.core_backend.service.ParticipationProjectValidationService;
import com.kct.jcredit.core_backend.service.EnR002FileUploadService;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.stream.Collectors;

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
    public EnR002EquipmentResponse addEquipment(String siteId,
                                                 String companyId,
                                                 EnR002EquipmentRequest request) {

        // 1. Guard check — site harus milik company yang login
        Site site = siteRepository.findByPublicIdWithFallback(siteId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        ResponseConstants.MSG_SITE_NOT_FOUND));
        
        Integer siteDbId = site.getId();

        if (!site.getCompany().getCompanyId().equals(companyId))
            throw new CustomException(ResponseConstants.FORBIDDEN,
                    ResponseConstants.MSG_UNAUTHORIZED);

        // 2. Guard check 
        boolean hasJoinedAnyProject = siteProjectRelationRepository
                .existsBySite_Id(siteDbId);
        if (!hasJoinedAnyProject)
            throw new CustomException(
                    "SITE_NOT_JOINED",
                    "このサイトはまだプロジェクトに参加していません。先に参加登録を行ってください。");

        // 3. field equipment
        validationService.validateEquipment(request);

        // 4. Save Equipment
        EnR002Equipment equipment = new EnR002Equipment();
        equipment.setSite(site);
        equipment.setEquipmentOpDate(request.getEquipmentOpDate());
        equipment.setNominalMaxOutput(request.getNominalMaxOutput());
        equipment.setPcsManufacturer(request.getPcsManufacturer());
        equipment.setPcsModel(request.getPcsModel());
        equipment.setPcsUnitCount(request.getPcsUnitCount());
        equipment.setDeviceNo(request.getDeviceNo());
        equipment.setPcsStandby(request.getPcsStandby());
        equipment.setHasBattery(request.getHasBattery());
        equipment.setBatteryMaker(request.getBatteryMaker());
        equipment.setBatteryModel(request.getBatteryModel());
        equipment.setBatteryCapacity(request.getBatteryCapacity());
        equipment.setTotalInvestment(request.getTotalInvestment());
        equipment.setHasSubsidy(request.getHasSubsidy());
        equipment.setSubsidyName(request.getSubsidyName());
        equipment.setSubsidyAmount(request.getSubsidyAmount());
        equipment.setPreProjectUnitPrice(request.getPreProjectUnitPrice());
        
        EnR002Equipment saved = equipmentRepository.save(equipment);

        // 5. Save EquipmentFile
        List<EnR002EquipmentFile> fileEntities = new java.util.ArrayList<>();
        if (request.getFiles() != null) {
            for (com.kct.jcredit.core_backend.dto.request.EnR002FileItemRequest f : request.getFiles()) {
                EnR002EquipmentFile ef = new EnR002EquipmentFile();
                ef.setEquipment(saved);
                ef.setFileType(f.getFileType());
                ef.setFileId(f.getFileId());
                ef.setS3Key(f.getS3Key());
                fileEntities.add(ef);
            }
        }
        List<EnR002EquipmentFile> savedFiles = equipmentFileRepository.saveAll(fileEntities);

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
    public List<EnR002EquipmentResponse> getEquipmentsBySite(String siteId, String companyId) {
        Site site = siteRepository.findByPublicIdWithFallback(siteId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        ResponseConstants.MSG_SITE_NOT_FOUND));

        if (!site.getCompany().getCompanyId().equals(companyId))
            throw new CustomException(ResponseConstants.FORBIDDEN,
                    ResponseConstants.MSG_UNAUTHORIZED);

        Integer siteDbId = site.getId();

        return equipmentRepository.findBySite_Id(siteDbId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private EnR002EquipmentResponse toResponse(EnR002Equipment e) {
        List<EnR002EquipmentFileResponse> fileResponses = equipmentFileRepository
                .findByEquipment_Id(e.getId()).stream()
                .map(f -> EnR002EquipmentFileResponse.builder()
                        .id(f.getId())
                        .fileType(f.getFileType() != null ? f.getFileType().getValue() : null)
                        .fileId(f.getFileId())
                        .s3Key(f.getS3Key())
                        .build())
                .collect(Collectors.toList());

        return EnR002EquipmentResponse.builder()
                .id(e.getId())
                .siteId(e.getSite().getPublicId())
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
                .files(fileResponses)
                .build();
        }
    @Override
    public EnR002EquipmentResponse getEquipmentById(String siteId,
                                                    Integer equipmentId,
                                                    String companyId) {
        Site site = siteRepository.findByPublicIdWithFallback(siteId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        ResponseConstants.MSG_SITE_NOT_FOUND));

        if (!site.getCompany().getCompanyId().equals(companyId))
            throw new CustomException(ResponseConstants.FORBIDDEN,
                    ResponseConstants.MSG_UNAUTHORIZED);
        
        Integer siteDbId = site.getId();

        EnR002Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "設備情報が見つかりません。"));

        if (!equipment.getSite().getId().equals(siteDbId))
            throw new CustomException(
                    ResponseConstants.NOT_FOUND, "設備情報が見つかりません。");

        return toResponse(equipment);
    }

        @Override
        @Transactional
        public EnR002EquipmentResponse updateEquipment(String siteId, Integer equipmentId,
                                                        String companyId, EnR002EquipmentUpdateRequest request) {

        Site site = siteRepository.findByPublicIdWithFallback(siteId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, ResponseConstants.MSG_SITE_NOT_FOUND));

        if (!site.getCompany().getCompanyId().equals(companyId))
                throw new CustomException(ResponseConstants.FORBIDDEN, ResponseConstants.MSG_UNAUTHORIZED);

        Integer siteDbId = site.getId();

        EnR002Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "設備情報が見つかりません。"));

        if (!equipment.getSite().getId().equals(siteDbId))
                throw new CustomException(ResponseConstants.NOT_FOUND, "設備情報が見つかりません。");

        // ... field update non-file, not change

        EnR002Equipment saved = equipmentRepository.save(equipment);

        // ── Update file new vs existing ──
        List<EnR002EquipmentFile> filesToMoveAfterCommit = new ArrayList<>();

        if (request.getFiles() != null) {

                List<EnR002EquipmentFile> existingFiles = equipmentFileRepository.findByEquipment_Id(equipmentId);

                // gather s3key
                Set<String> requestedS3Keys = request.getFiles().stream()
                        .map(EnR002FileItemRequest::getS3Key)
                        .collect(Collectors.toSet());

                // 1. delete file not in reqest
                List<EnR002EquipmentFile> filesToDelete = existingFiles.stream()
                        .filter(f -> !requestedS3Keys.contains(f.getS3Key()))
                        .collect(Collectors.toList());

                filesToDelete.forEach(f -> {
                try {
                        fileUploadService.deletePermanentFile(f.getS3Key()); // ⚠️ lihat catatan di bawah
                } catch (Exception e) {
                        log.error("Failed to delete old S3 file. s3Key={}, error={}", f.getS3Key(), e.getMessage());
                }
                });
                equipmentFileRepository.deleteAll(filesToDelete);

                // 2. 
                for (EnR002FileItemRequest f : request.getFiles()) {

                boolean isNewFile = f.getS3Key().startsWith("tmp/equipments/");
                boolean alreadyExists = existingFiles.stream()
                        .anyMatch(existing -> existing.getS3Key().equals(f.getS3Key()));

                if (isNewFile) {
                        EnR002EquipmentFile ef = new EnR002EquipmentFile();
                        ef.setEquipment(saved);
                        ef.setFileType(f.getFileType());
                        ef.setFileId(f.getFileId());
                        ef.setS3Key(f.getS3Key());
                        EnR002EquipmentFile savedFile = equipmentFileRepository.save(ef);
                        filesToMoveAfterCommit.add(savedFile);

                } else if (alreadyExists) {

                }

                }
        }

        // ── afterCommit — just for new equipment ──
        if (!filesToMoveAfterCommit.isEmpty()) {
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                                filesToMoveAfterCommit.forEach(fileEntity -> {
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
        }

        return toResponse(saved);
        }
}