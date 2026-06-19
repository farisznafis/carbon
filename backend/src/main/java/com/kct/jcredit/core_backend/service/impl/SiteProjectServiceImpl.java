package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.dto.request.EquipmentRegisterSolarRequest;
import com.kct.jcredit.core_backend.dto.response.EquipmentSolarRegisterResponse;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.dto.request.FileItemSolarRequest;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.EquipmentItemSolarRequest;
import com.kct.jcredit.core_backend.dto.response.details.EquipmentItemSolarResponse;
import com.kct.jcredit.core_backend.dto.response.details.ValidationErrorDetail;
import com.kct.jcredit.core_backend.service.SiteProjectService;
import com.kct.jcredit.core_backend.service.FileUploadSolarService;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.SiteR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.SiteR002MemberData;
import com.kct.jcredit.core_backend.entity.TncAgreementEquip;
import com.kct.jcredit.core_backend.repository.dynamodb.TncAgreementRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteR002MemberDataRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.exception.BulkValidationException;

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
@Transactional
public class SiteProjectServiceImpl implements SiteProjectService {

    private final SiteProjectRelationRepository siteProjectRelationRepository;
    private final SiteR002MemberDataRepository  memberDataRepository;
    private final SiteR002EquipmentFileRepository equipmentFileRepository;
    private final TncAgreementRepository tncAgreementRepository;
    private final SiteRepository siteRepository;
    private final ProjectRepository projectRepository;
    private final FileUploadSolarService fileUploadSolarService;
    // private final S3Client s3Client;

    private static final Set<EquipmentFileType> REQUIRED_FILES = Set.of(
            EquipmentFileType.SOLAR_CONTRACT,
            EquipmentFileType.PANEL_CATALOG,
            EquipmentFileType.PCS_SPEC,
            EquipmentFileType.CONSTRUCTION_INVOICE,
            EquipmentFileType.ELECTRICITY_BILL
    );

    @Override
    @Transactional
    public EquipmentSolarRegisterResponse registerEquipments(
            Integer siteId,
            Integer projectId,
            String companyId,
            EquipmentRegisterSolarRequest request) {

        EquipmentSolarRegisterResponse response = new EquipmentSolarRegisterResponse();

        // 1. Site check
        Site site = siteRepository.findById(siteId).orElse(null);
        if (site == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
            return response;
        }

        // 2. Access auth check
        if (!site.getCompany().getCompanyId().equals(companyId)) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_UNAUTHORIZED);
            return response;
        }

        // 3. Project check
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
            return response;
        }

        // 4. Site–project relation check 
        SiteProjectRelation relation = siteProjectRelationRepository
                .findBySiteIdAndProjectId(siteId, projectId)
                .orElse(null);

        if (relation == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
            return response;
        }

        // 5. Double-submission prevention
        boolean isAlreadySubmitted = memberDataRepository.existsBySite_IdAndProject_Id(siteId, projectId);
        if (isAlreadySubmitted) {
            response.setStatusCode(ResponseConstants.ALREADY_SUBMITTED);
            response.setMessage(ResponseConstants.MSG_ALREADY_SUBMITTED);
            return response;
        }

        // 6. equipment not null
        validateEquipmentsNotEmpty(request.getEquipments());

        // 7. Equipment validation (throw BulkValidationException)
        validateAllEquipments(request.getEquipments());

        // 8. Validasi consent TnC 
        validateTncConsent(request);

        // 9. Tnc validation (throw BulkValidationException)
        validateTncFields(request);

        // 10. Batch insert equipment + files
        final List<SiteR002EquipmentFile> savedFiles = new ArrayList<>();

        List<EquipmentItemSolarResponse> equipmentResponses = request.getEquipments().stream()
                .map(item -> {
                    SiteR002MemberData entity = buildMemberDataEntity(item, relation);
                    SiteR002MemberData saved  = memberDataRepository.save(entity);

                    List<SiteR002EquipmentFile> fileEntities = item.getFiles().stream()
                            .map(f -> buildFileEntity(f, saved))
                            .collect(Collectors.toList());
                    List<SiteR002EquipmentFile> savedFileList = 
                            equipmentFileRepository.saveAll(fileEntities); 
                    savedFiles.addAll(savedFileList);  
                
                    return EquipmentItemSolarResponse.builder()
                            .tempId(item.getTempId())
                            .equipmentId(saved.getId())
                            .methodologyNumber(saved.getMethodologyNumber())
                            .isEligible(saved.getIsEligible())
                            .equipmentOpDate(saved.getEquipmentOpDate())
                            .nominalMaxOutput(saved.getNominalMaxOutput())
                            .pcsManufacturer(saved.getPcsManufacturer())
                            .pcsModel(saved.getPcsModel())
                            .powerCondUnitCount(saved.getPowerCondUnitCount())
                            .deviceNo(saved.getDeviceNo())
                            .powerCondStandby(saved.getPowerCondStandby())
                            .hasBattery(saved.getHasBattery())
                            .batteryMaker(saved.getBatteryMaker())
                            .batteryModel(saved.getBatteryModel())
                            .batteryCapacity(saved.getBatteryCapacity())
                            .ictEquipmentPowerConsumption(saved.getIctEquipmentPowerConsumption())
                            .additionalEquipment(saved.getAdditionalEquipment())
                            .additionalEquipmentDate(saved.getAdditionalEquipmentDate())
                            .additionalEquipmentType(saved.getAdditionalEquipmentType())
                            .additionalUnitCount(saved.getAdditionalUnitCount())
                            .additionalDeviceUniqueNo(saved.getAdditionalDeviceUniqueNo())
                            .totalInvestment(saved.getTotalInvestment())
                            .hasSubsidy(saved.getHasSubsidy())
                            .subsidyName(saved.getSubsidyName())
                            .subsidyAmount(saved.getSubsidyAmount())
                            .preProjectUnitPrice(saved.getPreProjectUnitPrice())
                            .build();
                })
                .collect(Collectors.toList());

        // 11. TnC insert DynamoDB after commit 

        final Integer participationId = relation.getId();

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {

                    savedFiles.forEach(fileEntity -> {
                try {
                    String permanentKey = fileUploadSolarService.moveTempToPermanent(
                            fileEntity.getS3Key(),
                            fileEntity.getMemberData().getId()
                    );
                    equipmentFileRepository.updates3Key(fileEntity.getId(), permanentKey);
                } catch (Exception e) {
                    log.error("S3 move failed. fileId={}, s3Key={}, error={}",
                            fileEntity.getFileId(),
                            fileEntity.getS3Key(),
                            e.getMessage());
                    // TODO: retry table
                }
            });
                    //save TnC ke DynamoDB
                    try {
                        saveTnc(request, siteId, projectId, participationId, companyId);
                    } catch (Exception e) {
                        log.error("TnC DynamoDB save failed. participationId={}, error={}",
                                participationId, e.getMessage());
                        // TODO: retry table
                    }
                }
            }
    );

        // 12. Set response before return
        response.setStatusCode(ResponseConstants.OK);
        response.setEquipments(equipmentResponses);
        return response;
    }

    // VALIDATION — TnC (level request wide)
 
    private void validateTncFields(EquipmentRegisterSolarRequest request) {
        List<ValidationErrorDetail> errors = new ArrayList<>();
 
        // isEmitter = true → emitterCode wajib, harus tepat 9 digit angka
        if (Boolean.TRUE.equals(request.getIsEmitter())) {
            if (request.getEmitterCode() == null
                    || request.getEmitterCode().isBlank()
                    || !request.getEmitterCode().matches("\\d{9}")) {
                errors.add(ValidationErrorDetail.builder()
                        .tempId("request")
                        .field("emitterCode")
                        .message("isEmitterがtrueの場合、emitterCodeは9桁の数字必須です。")
                        .build());
            }
        }
 
        // isSubjectToPeriodicReport = true → energyReport, energyBusinessCode, factoryCode wajib
        if (Boolean.TRUE.equals(request.getIsSubjectToPeriodicReport())) {
            if (request.getEnergyReport() == null || request.getEnergyReport().isBlank()) {
                errors.add(ValidationErrorDetail.builder()
                        .tempId("request")
                        .field("energyReport")
                        .message("isSubjectToPeriodicReportがtrueの場合、energyReportは必須です。")
                        .build());
            }
            if (request.getEnergyBusinessCode() == null
                    || !request.getEnergyBusinessCode().matches("\\d{7}")) {
                errors.add(ValidationErrorDetail.builder()
                        .tempId("request")
                        .field("energyBusinessCode")
                        .message("isSubjectToPeriodicReportがtrueの場合、energyBusinessCodeは7桁の数字必須です。")
                        .build());
            }
            if (request.getFactoryCode() == null
                    || !request.getFactoryCode().matches("\\d{7}")) {
                errors.add(ValidationErrorDetail.builder()
                        .tempId("request")
                        .field("factoryCode")
                        .message("isSubjectToPeriodicReportがtrueの場合、factoryCodeは7桁の数字必須です。")
                        .build());
            }
        }
 
        if (!errors.isEmpty()) {
            throw new BulkValidationException(errors);
        }
    }

    // validation consent
    private void validateTncConsent(EquipmentRegisterSolarRequest request) {
    List<ValidationErrorDetail> errors = new ArrayList<>();

    // Consent 
    if (!Boolean.TRUE.equals(request.getIsAgreeTerms())) {
        errors.add(ValidationErrorDetail.builder()
                .tempId("request")
                .field("isAgreeTerms")
                .message("利用規約への同意が必要です。")
                .build());
    }
    if (!Boolean.TRUE.equals(request.getHasConsent())) {
        errors.add(ValidationErrorDetail.builder()
                .tempId("request")
                .field("hasConsent")
                .message("個人情報の取り扱いへの同意が必要です。")
                .build());
    }
    if (!Boolean.TRUE.equals(request.getIsAgreeDataUse())) {
        errors.add(ValidationErrorDetail.builder()
                .tempId("request")
                .field("isAgreeDataUse")
                .message("データ利用への同意が必要です。")
                .build());
    }
    if (!Boolean.TRUE.equals(request.getIsNotRegistered())) {
        errors.add(ValidationErrorDetail.builder()
                .tempId("request")
                .field("isNotRegistered")
                .message("他のJ-クレジット制度への未登録確認が必要です。")
                .build());
    }

    if (!errors.isEmpty()) {
        throw new BulkValidationException(errors);
    }
}

    // VALIDATION — Equipment

        private void validateAllEquipments(List<EquipmentItemSolarRequest> equipments) {
            List<ValidationErrorDetail> allErrors = new ArrayList<>();

        equipments.forEach(item ->
                collectEquipmentErrors(item, allErrors)
        );

        if (!allErrors.isEmpty()) {
                throw new BulkValidationException(allErrors);
        }
        }
        private void collectEquipmentErrors(
            EquipmentItemSolarRequest item,
            List<ValidationErrorDetail> errors) {
 
        // Non-file required fields
        validateRequiredNonFileFields(item, errors);
 
        // Required files
        validateRequiredFiles(item, errors);
        validateFileContents(item, errors);
 
        // Equipment operation date
        validateEquipmentOpDate(item, errors);
 
        // Conditional: hasBattery
        if (Boolean.TRUE.equals(item.getHasBattery())) {
            collectBatteryErrors(item, errors);
        }
 
        // Conditional: hasSubsidy
        if (Boolean.TRUE.equals(item.getHasSubsidy())) {
            collectSubsidyErrors(item, errors);
        }
 
        // Conditional: additionalEquipment
        if (Boolean.TRUE.equals(item.getAdditionalEquipment())) {
            if (item.getAdditionalEquipmentDate() == null) {
                errors.add(err(item.getTempId(), "additionalEquipmentDate",
                "additionalEquipmentがtrueの場合、additionalEquipmentDateは必須です。"));
            } 
        else if (item.getEquipmentOpDate() != null
            && item.getAdditionalEquipmentDate().isBefore(item.getEquipmentOpDate())) {
            errors.add(err(item.getTempId(), "additionalEquipmentDate",
            "追加設備日は設備稼働日以降である必要があります。"));
        if (item.getAdditionalEquipmentType() == null || item.getAdditionalEquipmentType().isBlank()) {
            errors.add(err(item.getTempId(), "additionalEquipmentType",
                "additionalEquipmentがtrueの場合、additionalEquipmentTypeは必須です。"));
            }
        if (item.getAdditionalUnitCount() == null || item.getAdditionalUnitCount() < 1) {
            errors.add(err(item.getTempId(), "additionalUnitCount",
                "additionalEquipmentがtrueの場合、additionalUnitCountは1以上必須です。"));
            }
        if (item.getAdditionalDeviceUniqueNo() == null || item.getAdditionalDeviceUniqueNo().isBlank()) {
            errors.add(err(item.getTempId(), "additionalDeviceUniqueNo",
                "additionalEquipmentがtrueの場合、additionalDeviceUniqueNoは必須です。"));
            }
        }
    }
}

    private void validateRequiredNonFileFields(
            EquipmentItemSolarRequest item,
            List<ValidationErrorDetail> errors) {
 
        // Required fields
        if (item.getMethodologyNumber() == null || item.getMethodologyNumber().isBlank()) {
            errors.add(err(item.getTempId(), "methodologyNumber", "必須項目です。"));
        }
        if (item.getEquipmentOpDate() == null) {
            errors.add(err(item.getTempId(), "equipmentOpDate", "必須項目です。"));
        }
        if (item.getNominalMaxOutput() != null && item.getNominalMaxOutput().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(err(item.getTempId(), "nominalMaxOutput",
            "定格最大出力は0より大きい値を入力してください。"));
        }
        if (item.getPcsManufacturer() == null || item.getPcsManufacturer().isBlank()) {
            errors.add(err(item.getTempId(), "pcsManufacturer", "必須項目です。"));
        }
        if (item.getPcsModel() == null || item.getPcsModel().isBlank()) {
            errors.add(err(item.getTempId(), "pcsModel", "必須項目です。"));
        }
        if (item.getPowerCondUnitCount() == null || item.getPowerCondUnitCount() < 1) {
            errors.add(err(item.getTempId(), "powerCondUnitCount", "1以上の数値を入力してください。"));
        }
        if (item.getDeviceNo() == null || item.getDeviceNo().isBlank()) {
            errors.add(err(item.getTempId(), "deviceNo", "必須項目です。"));
        }
        if (item.getPowerCondStandby() == null) {
            errors.add(err(item.getTempId(), "powerCondStandBy", "必須項目です。"));
        }
        if (item.getIctEquipmentPowerConsumption() != null
        && item.getIctEquipmentPowerConsumption().compareTo(BigDecimal.ZERO) < 0) {
            errors.add(err(item.getTempId(), "ictEquipmentPowerConsumption",
            "ICT機器消費電力は0以上の値を入力してください。"));
        }
        if (item.getTotalInvestment() != null 
            && new BigDecimal(item.getTotalInvestment()).compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(err(item.getTempId(), "totalInvestment",
            "総投資額は0より大きい値を入力してください。"));
        }
        if (item.getPreProjectUnitPrice() != null && item.getPreProjectUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(err(item.getTempId(), "preProjectUnitPrice",
            "プロジェクト前単価は0より大きい値を入力してください。"));
}
    }
 
    private void validateRequiredFiles(EquipmentItemSolarRequest item,
                                   List<ValidationErrorDetail> errors) {
    List<FileItemSolarRequest> files = item.getFiles() == null
            ? Collections.emptyList()
            : item.getFiles();

    Set<EquipmentFileType> submitted = files.stream()
            .map(FileItemSolarRequest::getFileType)
            .collect(Collectors.toSet());
 
        REQUIRED_FILES.forEach(required -> {
            if (!submitted.contains(required)) {
                errors.add(ValidationErrorDetail.builder()
                        .tempId(item.getTempId())
                        .field(required.getValue())
                        .message(required.getValue() + "ファイルは必須です。")
                        .build());
            }
        });
    }
 
    private void validateEquipmentOpDate(
            EquipmentItemSolarRequest item,
            List<ValidationErrorDetail> errors) {
 
        // equipmentOpDate
        if (item.getEquipmentOpDate() == null) return;
 
        LocalDate today      = LocalDate.now();
        LocalDate twoYearsAgo = today.minusYears(2);
 
        if (item.getEquipmentOpDate().isBefore(twoYearsAgo)) {
            errors.add(err(item.getTempId(), "equipmentOpDate",
                    "設備稼働日は承認日から2年以内である必要があります。"));
        }
        if (item.getEquipmentOpDate().isAfter(today)) {
            errors.add(err(item.getTempId(), "equipmentOpDate",
                    "設備稼働日は未来の日付は設定できません。"));
        }
    }
 
    private void collectBatteryErrors(
            EquipmentItemSolarRequest item,
            List<ValidationErrorDetail> errors) {
 
        if (item.getBatteryMaker() == null || item.getBatteryMaker().isBlank()) {
            errors.add(err(item.getTempId(), "batteryMaker",
                    "hasBatteryがtrueの場合、batteryMakerは必須です。"));
        }
        if (item.getBatteryModel() == null || item.getBatteryModel().isBlank()) {
            errors.add(err(item.getTempId(), "batteryModel",
                    "hasBatteryがtrueの場合、batteryModelは必須です。"));
        }
        if (item.getBatteryCapacity() == null) {
            errors.add(err(item.getTempId(), "batteryCapacity",
            "hasBatteryがtrueの場合、batteryCapacityは必須です。"));
        } 
        else if (item.getBatteryCapacity().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(err(item.getTempId(), "batteryCapacity",
            "バッテリー容量は0より大きい値を入力してください。"));
        }
            boolean hasBatteryCatalog = item.getFiles().stream()
                .anyMatch(f -> EquipmentFileType.BATTERY_CATALOG.equals(f.getFileType()));
        
        if (!hasBatteryCatalog) {
            errors.add(err(item.getTempId(), EquipmentFileType.BATTERY_CATALOG.getValue(),
                    "hasBatteryがtrueの場合、battery_catalogファイルは必須です。"));
        }
    }
 
    private void collectSubsidyErrors(
            EquipmentItemSolarRequest item,
            List<ValidationErrorDetail> errors) {
 
        if (item.getSubsidyName() == null || item.getSubsidyName().isBlank()) {
            errors.add(err(item.getTempId(), "subsidyName",
                    "hasSubsidyがtrueの場合、subsidyNameは必須です。"));
        }
        if (item.getSubsidyAmount() == null) {
            errors.add(err(item.getTempId(), "subsidyAmount",
            "hasSubsidyがtrueの場合、subsidyAmountは必須です。"));
        } 
        else if (item.getSubsidyAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(err(item.getTempId(), "subsidyAmount",
            "補助金額は0より大きい値を入力してください。"));
}
 
        boolean hasSubsidyNotice = item.getFiles().stream()
                .anyMatch(f -> EquipmentFileType.SUBSIDY_NOTICE.equals(f.getFileType()));
        if (!hasSubsidyNotice) {
            errors.add(err(item.getTempId(), EquipmentFileType.SUBSIDY_NOTICE.getValue(),
                    "hasSubsidyがtrueの場合、subsidy_noticeファイルは必須です。"));
        }
    }
 
    // ValidationErrorDetail
    private ValidationErrorDetail err(String tempId, String field, String message) {
        return ValidationErrorDetail.builder()
                .tempId(tempId)
                .field(field)
                .message(message)
                .build();
    }

    // validation equipment not null
    private void validateEquipmentsNotEmpty(List<EquipmentItemSolarRequest> equipments) {
    if (equipments == null || equipments.isEmpty()) {
        throw new BulkValidationException(List.of(
                ValidationErrorDetail.builder()
                        .tempId("request")
                        .field("equipments")
                        .message("設備情報が1件以上必要です。")
                        .build()
        ));
    }
}

    private void validateFileContents(EquipmentItemSolarRequest item, List<ValidationErrorDetail> errors) {
    if (item.getFiles() == null) return;
    item.getFiles().forEach(f -> {
        if (f.getS3Key() == null || f.getS3Key().isBlank()) {
            errors.add(err(item.getTempId(),
                    f.getFileType() != null ? f.getFileType().getValue() : "file",
                    "S3キーが不正です。ファイルを再アップロードしてください。"));
        }
        if (f.getFileName() == null || f.getFileName().isBlank()) {
            errors.add(err(item.getTempId(),
                    f.getFileType() != null ? f.getFileType().getValue() : "file",
                    "ファイル名が不正です。"));
        }
    });
}
 
    // ENTITY BUILDER
    private SiteR002MemberData buildMemberDataEntity(
            EquipmentItemSolarRequest item,
            SiteProjectRelation relation) {
    return SiteR002MemberData.builder()
        .project(relation.getProject())              
        .site(relation.getSite())                    
        .company(relation.getSite().getCompany())    
        .methodologyNumber(item.getMethodologyNumber())
        .isEligible(item.getIsEligible())
        .equipmentOpDate(item.getEquipmentOpDate())
        .pcsManufacturer(item.getPcsManufacturer())
        .pcsModel(item.getPcsModel())
        .powerCondUnitCount(item.getPowerCondUnitCount())
        .deviceNo(item.getDeviceNo())
        .powerCondStandby(item.getPowerCondStandby())
        .additionalEquipment(item.getAdditionalEquipment())
        .additionalEquipmentDate(item.getAdditionalEquipmentDate())
        .additionalEquipmentType(item.getAdditionalEquipmentType())
        .additionalUnitCount(item.getAdditionalUnitCount())
        .additionalDeviceUniqueNo(item.getAdditionalDeviceUniqueNo())
        .totalInvestment(item.getTotalInvestment())
        .hasSubsidy(item.getHasSubsidy())
        .subsidyName(item.getSubsidyName())
        .subsidyAmount(item.getSubsidyAmount())
        .preProjectUnitPrice(item.getPreProjectUnitPrice())
        .nominalMaxOutput(item.getNominalMaxOutput())
        .ictEquipmentPowerConsumption(item.getIctEquipmentPowerConsumption())
        .hasBattery(item.getHasBattery())
        .batteryMaker(item.getBatteryMaker())
        .batteryModel(item.getBatteryModel())
        .batteryCapacity(item.getBatteryCapacity())  
        .build();
    }
 
    // File entity builder
    private SiteR002EquipmentFile buildFileEntity(
        FileItemSolarRequest fileReq,
        SiteR002MemberData savedMemberData) {

    return SiteR002EquipmentFile.builder()
            .memberData(savedMemberData)
            .fileType(fileReq.getFileType())
            .fileId(fileReq.getFileId())
            .s3Key(fileReq.getS3Key())
            .originalName(fileReq.getFileName())
            .build();
}

    // DYNAMO
 
    private void saveTnc(
        EquipmentRegisterSolarRequest request,
        Integer siteId,
        Integer projectId,
        Integer participationId,
        String companyId) {
 
        // pk: projectId#siteId 
        String pk = projectId + "#" + siteId;
        String sk = Instant.now().toString();
 
        TncAgreementEquip tnc = TncAgreementEquip.builder()
                .pk(pk)
                .sk(sk)
                .projectId(projectId)
                .siteId(siteId)
                .companyId(String.valueOf(siteId)) 
                .participationId(participationId)
                .isAgreeDataUse(request.getIsAgreeDataUse())
                .isAgreeExtraData(request.getIsAgreeExtraData())
                .isAgreeTransfer(request.getIsAgreeTransfer())
                .isNotRegistered(request.getIsNotRegistered())
                .isKeidanrenParticipation(request.getIsKeidanrenParticipation())
                .isEmitter(request.getIsEmitter())
                .isAdjustedEmissionsAdded(request.getIsAdjustedEmissionsAdded())
                .emitterCode(request.getEmitterCode())
                .isSubjectToPeriodicReport(request.getIsSubjectToPeriodicReport())
                .energyReport(request.getEnergyReport())
                .energyBusinessCode(request.getEnergyBusinessCode())
                .factoryCode(request.getFactoryCode())
                .repName(request.getRepName())
                .repRole(request.getRepRole())
                .hasConsent(request.getHasConsent())
                .isAgreeTerms(request.getIsAgreeTerms())
                .agreedAt(Instant.now().toString())
                .build();
 
        tncAgreementRepository.save(tnc);
    }
}