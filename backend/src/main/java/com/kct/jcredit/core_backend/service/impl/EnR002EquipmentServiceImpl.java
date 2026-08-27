package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentChangeRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FileItemRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentFileResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentHistoryResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.service.EnR002EquipmentService;
import com.kct.jcredit.core_backend.service.ParticipationProjectValidationService;
import com.kct.jcredit.core_backend.service.EnR002FileUploadService;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final ParticipatingProjectRepository participatingProjectRepository;
    private final EnR002EquipmentRepository              equipmentRepository;
    private final EnR002EquipmentFileRepository          equipmentFileRepository;
    private final EnR002FileUploadService                fileUploadService;
    private final ParticipationProjectValidationService  validationService; 
    private final ProjectRepository                     projectRepository;

    @Override
    @Transactional
    public EnR002EquipmentResponse addEquipment(String siteId,
                                                 String companyId,
                                                 String projectId,
                                                 EnR002EquipmentRequest request) {

        // 1. Guard check site
        Site site = siteRepository.findByPublicIdWithFallback(siteId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        ResponseConstants.MSG_SITE_NOT_FOUND));
        
        Project project = projectRepository.findByPublicId(projectId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        "プロジェクトが見つかりません。"));
        
        Integer siteDbId = site.getId();

        if (!site.getCompany().getCompanyId().equals(companyId))
            throw new CustomException(ResponseConstants.FORBIDDEN,
                    ResponseConstants.MSG_UNAUTHORIZED);

        // 2. Guard check site-project relation
        Optional<SiteProjectRelation> relation = siteProjectRelationRepository.findBySite_IdAndProject_Id(siteDbId, project.getId());
        if (relation.isEmpty()) {
            throw new CustomException(
                    "SITE_NOT_JOINED",
                    "このサイトはまだプロジェクトに参加していません。先に参加登録を行ってください。");
        }

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

        // 5. Save EquipmentFile — s3Key yang disimpan sudah key final (permanent),
        //    bukan key tmp. Object S3-nya baru dipindah setelah commit.
        List<EnR002EquipmentFile> fileEntities = new java.util.ArrayList<>();
        Map<String, String> tempKeyByFileId = new java.util.HashMap<>();
        if (request.getFiles() != null) {
            for (com.kct.jcredit.core_backend.dto.request.EnR002FileItemRequest f : request.getFiles()) {
                String permanentKey = fileUploadService.buildPermanentKey(
                        f.getS3Key(), siteId, saved.getId());

                EnR002EquipmentFile ef = new EnR002EquipmentFile();
                ef.setEquipment(saved);
                ef.setFileType(f.getFileType());
                ef.setFileId(f.getFileId());
                ef.setS3Key(permanentKey);
                fileEntities.add(ef);

                tempKeyByFileId.put(f.getFileId(), f.getS3Key());
            }
        }
        List<EnR002EquipmentFile> savedFiles = equipmentFileRepository.saveAll(fileEntities);

        if (!savedFiles.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            savedFiles.forEach(fileEntity -> {
                                try {
                                    fileUploadService.moveToPermanent(
                                            tempKeyByFileId.get(fileEntity.getFileId()),
                                            fileEntity.getS3Key());
                                } catch (Exception e) {
                                    log.error("S3 move failed. fileId={}, s3Key={}, error={}",
                                            fileEntity.getFileId(), fileEntity.getS3Key(),
                                            e.getMessage());
                                    // TODO: retry table
                                }
                            });
                        }
                    }
            );
        }

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

    private Integer resolveParticipationId(EnR002Equipment equipment) {
    EnR002Equipment root = equipment;
    while (root.getPreviousEquipment() != null) {
        root = root.getPreviousEquipment();
    }

    return participatingProjectRepository
            .findByEquipment_Id(root.getId())
            .stream()
            .findFirst()
            .map(ParticipatingProject::getId)
            .orElse(null);
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

        // Integer participationId = participatingProjectRepository
        //         .findByEquipment_Id(e.getId())
        //         .stream()
        //         .findFirst()
        //         .map(ParticipatingProject::getId)
        //         .orElse(null);

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
                .participationId(resolveParticipationId(e))
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
        public EnR002EquipmentResponse changeEquipment(String siteId, Integer equipmentId,
                                                        String companyId,
                                                        EnR002EquipmentChangeRequest request) {

        // ── 1. Guard checks ──────────────────────────────────────────
        Site site = siteRepository.findByPublicIdWithFallback(siteId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, ResponseConstants.MSG_SITE_NOT_FOUND));

        if (!site.getCompany().getCompanyId().equals(companyId))
                throw new CustomException(ResponseConstants.FORBIDDEN, ResponseConstants.MSG_UNAUTHORIZED);

        EnR002Equipment oldEquipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "設備情報が見つかりません。"));

        if (!oldEquipment.getSite().getId().equals(site.getId()))
                throw new CustomException(ResponseConstants.NOT_FOUND, "設備情報が見つかりません。");

        // ── 2. Old Equipment lama should the newest version
        Optional<EnR002Equipment> existingSuccessor =
                equipmentRepository.findByPreviousEquipment_Id(equipmentId);
        if (existingSuccessor.isPresent()) {
                throw new CustomException("EQUIPMENT_ALREADY_CHANGED",
                        "この設備は既に変更済みです。最新の設備に対して変更を行ってください。");
        }

        // ── 3. changeDate should be after the old equipment's monitoring start date ──
        LocalDate changeDate = request.getChangeDate();
        LocalDate oldStartDate = resolveMonitoringStartDate(oldEquipment);

        if (!changeDate.isAfter(oldStartDate)) {
                throw new CustomException("INVALID_CHANGE_DATE",
                        "変更日は変更前設備のモニタリング開始日より後である必要があります。");
        }

        // ── 4. Make new equipment ──────
        EnR002Equipment newEquipment = EnR002Equipment.builder()
                .site(site)
                // equipmentOpDate DI-INHERIT — tanggal operasi asli tidak berubah
                .equipmentOpDate(oldEquipment.getEquipmentOpDate())
                .nominalMaxOutput(inherit(request.getNominalMaxOutput(), oldEquipment.getNominalMaxOutput()))
                .pcsManufacturer(inherit(request.getPcsManufacturer(), oldEquipment.getPcsManufacturer()))
                .pcsModel(inherit(request.getPcsModel(), oldEquipment.getPcsModel()))
                .pcsUnitCount(inherit(request.getPcsUnitCount(), oldEquipment.getPcsUnitCount()))
                .deviceNo(inherit(request.getDeviceNo(), oldEquipment.getDeviceNo()))
                .pcsStandby(inherit(request.getPcsStandby(), oldEquipment.getPcsStandby()))
                .hasBattery(inherit(request.getHasBattery(), oldEquipment.getHasBattery()))
                .batteryMaker(inherit(request.getBatteryMaker(), oldEquipment.getBatteryMaker()))
                .batteryModel(inherit(request.getBatteryModel(), oldEquipment.getBatteryModel()))
                .batteryCapacity(inherit(request.getBatteryCapacity(), oldEquipment.getBatteryCapacity()))
                .totalInvestment(inherit(request.getTotalInvestment(), oldEquipment.getTotalInvestment()))
                .hasSubsidy(inherit(request.getHasSubsidy(), oldEquipment.getHasSubsidy()))
                .subsidyName(inherit(request.getSubsidyName(), oldEquipment.getSubsidyName()))
                .subsidyAmount(inherit(request.getSubsidyAmount(), oldEquipment.getSubsidyAmount()))
                .preProjectUnitPrice(inherit(request.getPreProjectUnitPrice(), oldEquipment.getPreProjectUnitPrice()))
                // Field history
                .changeDate(changeDate)
                .previousEquipment(oldEquipment)
                .build();

        EnR002Equipment savedNew = equipmentRepository.save(newEquipment);

        // ── 5. Inherit old files ──
        List<EnR002EquipmentFile> oldFiles = equipmentFileRepository.findByEquipment_Id(equipmentId);

        Set<EquipmentFileType> replacedTypes = request.getFiles() == null
                ? Collections.emptySet()
                : request.getFiles().stream()
                        .map(EnR002FileItemRequest::getFileType)
                        .collect(Collectors.toSet());

        List<EnR002EquipmentFile> inheritedFiles = oldFiles.stream()
                .filter(f -> !replacedTypes.contains(f.getFileType()))
                .map(f -> {
                        EnR002EquipmentFile inherited = new EnR002EquipmentFile();
                        inherited.setEquipment(savedNew);
                        inherited.setFileType(f.getFileType());
                        inherited.setFileId(f.getFileId());
                        inherited.setS3Key(f.getS3Key()); 
                        return inherited;
                })
                .collect(Collectors.toList());

        equipmentFileRepository.saveAll(inheritedFiles);

        // ── 6. save new files ───────────────────────────────────────
        //     s3Key yang disimpan sudah key final; object S3 dipindah setelah commit.
        List<EnR002EquipmentFile> newFilesToMove = new ArrayList<>();
        Map<String, String> tempKeyByFileId = new java.util.HashMap<>();
        if (request.getFiles() != null) {
                for (EnR002FileItemRequest f : request.getFiles()) {
                if (!f.getS3Key().startsWith("tmp/equipments/")) {
                        throw new CustomException("INVALID_FILE_REFERENCE",
                                "設備変更では新規アップロードファイルのみ指定可能です。s3Key=" + f.getS3Key());
                }
                EnR002EquipmentFile ef = new EnR002EquipmentFile();
                ef.setEquipment(savedNew);
                ef.setFileType(f.getFileType());
                ef.setFileId(f.getFileId());
                ef.setS3Key(fileUploadService.buildPermanentKey(
                        f.getS3Key(), siteId, savedNew.getId()));
                newFilesToMove.add(equipmentFileRepository.save(ef));

                tempKeyByFileId.put(f.getFileId(), f.getS3Key());
                }
        }

        // ── 7. Validation  ───────────────────
        List<EnR002EquipmentFile> finalFiles = equipmentFileRepository.findByEquipment_Id(savedNew.getId());
        ParticipationProjectValidationServiceImpl.EquipmentUpdateValidator
                .validateEquipmentEntityState(String.valueOf(savedNew.getId()), savedNew, finalFiles);

        // ── 8. afterCommit ─────────────────────
        if (!newFilesToMove.isEmpty()) {
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                                newFilesToMove.forEach(fileEntity -> {
                                try {
                                        fileUploadService.moveToPermanent(
                                                tempKeyByFileId.get(fileEntity.getFileId()),
                                                fileEntity.getS3Key());
                                } catch (Exception e) {
                                        log.error("S3 move failed. fileId={}, s3Key={}, error={}",
                                                fileEntity.getFileId(), fileEntity.getS3Key(),
                                                e.getMessage());
                                        // TODO: retry table
                                }
                                });
                        }
                        }
                );
        }

        return toResponse(savedNew);
        }

        private <T> T inherit(T newValue, T oldValue) {
        return newValue != null ? newValue : oldValue;
}
        

        @Override
        public List<EnR002EquipmentHistoryResponse> getEquipmentHistory(String siteId,
                                                                        Integer equipmentId,
                                                                        String companyId) {
        Site site = siteRepository.findByPublicIdWithFallback(siteId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, ResponseConstants.MSG_SITE_NOT_FOUND));

        if (!site.getCompany().getCompanyId().equals(companyId))
                throw new CustomException(ResponseConstants.FORBIDDEN, ResponseConstants.MSG_UNAUTHORIZED);

        EnR002Equipment target = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "設備情報が見つかりません。"));

        if (!target.getSite().getId().equals(site.getId()))
                throw new CustomException(ResponseConstants.NOT_FOUND, "設備情報が見つかりません。");

        List<EnR002Equipment> chain = new ArrayList<>();
        EnR002Equipment current = target;
        while (current != null) {
                chain.add(current);
                current = current.getPreviousEquipment();
        }
        Collections.reverse(chain); 

        // newest version
        EnR002Equipment last = chain.get(chain.size() - 1);
        Optional<EnR002Equipment> successor = equipmentRepository.findByPreviousEquipment_Id(last.getId());
        while (successor.isPresent()) {
                chain.add(successor.get());
                successor = equipmentRepository.findByPreviousEquipment_Id(successor.get().getId());
        }

        return chain.stream()
                .map(e -> EnR002EquipmentHistoryResponse.builder()
                        .id(e.getId())
                        .changeDate(e.getChangeDate())
                        .previousEquipmentId(e.getPreviousEquipment() != null
                                ? e.getPreviousEquipment().getId() : null)
                        .monitoringStartDate(resolveMonitoringStartDate(e))
                        .monitoringEndDate(resolveMonitoringEndDate(e))
                        .isActive(resolveMonitoringEndDate(e) == null)
                        .registeredBy(e.getCreatedBy())
                        .registeredAt(e.getCreatedAt())
                        .equipment(toResponse(e))
                        .build())
                .collect(Collectors.toList());
        }

        // ─────────────────────────────────────────────────────────────
        // PRIVATE 
        // ─────────────────────────────────────────────────────────────

        private LocalDate resolveMonitoringStartDate(EnR002Equipment equipment) {
        return equipment.getChangeDate() != null
                ? equipment.getChangeDate()
                : equipment.getEquipmentOpDate();
        }

        private LocalDate resolveMonitoringEndDate(EnR002Equipment equipment) {
        return equipmentRepository.findByPreviousEquipment_Id(equipment.getId())
                .map(successor -> successor.getChangeDate().minusDays(1))
                .orElse(null);
        }

}
