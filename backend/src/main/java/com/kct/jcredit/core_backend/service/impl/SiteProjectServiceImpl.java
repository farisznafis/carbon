package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.EquipmentItemSolarRequest;
import com.kct.jcredit.core_backend.dto.request.TncValidateRequest;
import com.kct.jcredit.core_backend.dto.request.FileItemSolarRequest;
import com.kct.jcredit.core_backend.dto.request.JigyoushaValidateRequest;
import com.kct.jcredit.core_backend.dto.request.FinalAgreementValidateRequest;
import com.kct.jcredit.core_backend.dto.request.EquipmentRegisterSolarRequest;
import com.kct.jcredit.core_backend.dto.response.EquipmentSolarRegisterResponse;
import com.kct.jcredit.core_backend.dto.response.JigyoushaDefaultResponse;
import com.kct.jcredit.core_backend.dto.response.details.EquipmentItemSolarResponse;
import com.kct.jcredit.core_backend.dto.response.details.ValidationErrorDetail;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.SiteR002MemberData;
import com.kct.jcredit.core_backend.entity.TncAgreementEquip;
import com.kct.jcredit.core_backend.exception.BulkValidationException;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.service.SiteProjectService;
import com.kct.jcredit.core_backend.service.FileUploadSolarService;
import com.kct.jcredit.core_backend.repository.dynamodb.ApplicationConfirmationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteR002MemberDataRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;

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
    private final SiteR002MemberDataRepository memberDataRepository;
    private final EnR002EquipmentFileRepository equipmentFileRepository;
    private final ApplicationConfirmationRepository tncAgreementRepository;
    private final SiteRepository siteRepository;
    private final ProjectRepository projectRepository;
    private final FileUploadSolarService fileUploadSolarService;

    private static final Set<EquipmentFileType> REQUIRED_FILES = Set.of(
            EquipmentFileType.SOLAR_CONTRACT,
            EquipmentFileType.PANEL_CATALOG,
            EquipmentFileType.PCS_SPEC,
            EquipmentFileType.CONSTRUCTION_INVOICE,
            EquipmentFileType.ELECTRICITY_BILL
    );

    // ─────────────────────────────────────────────────────────────
    // MAIN USE CASE
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public EquipmentSolarRegisterResponse registerEquipments(
            Integer siteId,
            Integer projectId,
            String companyId,
            EquipmentRegisterSolarRequest request) {

        EquipmentSolarRegisterResponse response = new EquipmentSolarRegisterResponse();

        // Site check
        Site site = siteRepository.findById(siteId).orElse(null);
        if (site == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
            return response;
        }

        // Access auth check
        if (!site.getCompany().getCompanyId().equals(companyId)) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_UNAUTHORIZED);
            return response;
        }

        // Project check
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
            return response;
        }

        // Site–project relation check
        SiteProjectRelation relation = siteProjectRelationRepository
                .findBySiteIdAndProjectId(siteId, projectId)
                .orElse(null);
        if (relation == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
            return response;
        }

        // Double-submission prevention
        boolean isAlreadySubmitted = memberDataRepository.existsBySite_IdAndProject_Id(siteId, projectId);
        if (isAlreadySubmitted) {
            response.setStatusCode(ResponseConstants.ALREADY_SUBMITTED);
            response.setMessage(ResponseConstants.MSG_ALREADY_SUBMITTED);
            return response;
        }

        // 1. validate equipment not null
        validateEquipmentsNotEmpty(request.getEquipments());

        // 2. validate Equipment validation (throw BulkValidationException)
        validateAllEquipments(request.getEquipments());

        // 3. validate consent TnC
        validateTncConsent(request);

        // 4. validate Tnc (throw BulkValidationException)
        validateTncFields(request);

        // 5. validate Company consent
        validateJigyousha(request);

        // 6. validate final agreement
        validateFinalAgreement(request);

        // Batch insert equipment + files
        final List<EnR002EquipmentFile> savedFiles = new ArrayList<>();

        List<EquipmentItemSolarResponse> equipmentResponses = request.getEquipments().stream()
                .map(item -> {
                    SiteR002MemberData entity = buildMemberDataEntity(item, relation);
                    SiteR002MemberData saved = memberDataRepository.save(entity);

                    List<EnR002EquipmentFile> fileEntities = item.getFiles().stream()
                            .map(f -> buildFileEntity(f, saved))
                            .collect(Collectors.toList());
                    List<EnR002EquipmentFile> savedFileList =
                            equipmentFileRepository.saveAll(fileEntities);
                    savedFiles.addAll(savedFileList);

                    return EquipmentItemSolarResponse.builder()
                            .tempId(item.getTempId())
                            .equipmentId(saved.getId())
                            .methodologyNumber(saved.getMethodologyNumber())
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

        // TnC and files insert DynamoDB and s3 after commit
        final Integer participationId = relation.getId();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        savedFiles.forEach(fileEntity -> {
                            try {
                                String permanentKey = fileUploadSolarService.moveTempToPermanent(
                                        fileEntity.getS3Key(),
                                        fileEntity.getId()
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

                        // save TnC to DynamoDB
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

        // Set response before return
        response.setStatusCode(ResponseConstants.OK);
        response.setEquipments(equipmentResponses);

        return response;
    }

    // ─────────────────────────────────────────────────────────────
    // VALIDATION — Equipment
    // ─────────────────────────────────────────────────────────────

    @Override
    public void validateEquipmentsNotEmpty(List<EquipmentItemSolarRequest> equipments) {
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

    @Override
    public void validateAllEquipments(List<EquipmentItemSolarRequest> equipments) {
        List<ValidationErrorDetail> allErrors = new ArrayList<>();
        equipments.forEach(item -> collectEquipmentErrors(item, allErrors));

        if (!allErrors.isEmpty()) {
            throw new BulkValidationException(allErrors);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // VALIDATION — TnC
    // ─────────────────────────────────────────────────────────────

    @Override
    public void validateTncConsent(EquipmentRegisterSolarRequest request) {
        List<ValidationErrorDetail> errors = new ArrayList<>();
        validateTncConsentCore(request.getIsAgreeDataUse(), request.getIsAgreeExtraData(),
                request.getIsAgreeTransfer(), request.getIsNotRegistered(),
                request.getIsKeidanrenParticipation(), errors);
        if (!errors.isEmpty()) throw new BulkValidationException(errors);
    }

    @Override
    public void validateTncFields(EquipmentRegisterSolarRequest request) {
        List<ValidationErrorDetail> errors = new ArrayList<>();
        validateTncFieldsCore(request.getIsEmitter(), request.getEmitterCode(),
                request.getIsAdjustedEmissionsAdded(),
                request.getIsSubjectToPeriodicReport(), request.getIsPeriodicReportTypeOne(),
                request.getEnergyBusinessCode(), request.getFactoryCode(), errors);
        if (!errors.isEmpty()) throw new BulkValidationException(errors);
    }

    // validate each page
    @Override
    public void validateTnc(TncValidateRequest request) {
        List<ValidationErrorDetail> errors = new ArrayList<>();
        validateTncConsentCore(request.getIsAgreeDataUse(), request.getIsAgreeExtraData(),
                request.getIsAgreeTransfer(), request.getIsNotRegistered(),
                request.getIsKeidanrenParticipation(), errors);
        validateTncFieldsCore(request.getIsEmitter(), request.getEmitterCode(),
                request.getIsAdjustedEmissionsAdded(),
                request.getIsSubjectToPeriodicReport(), request.getIsPeriodicReportTypeOne(),
                request.getEnergyBusinessCode(), request.getFactoryCode(), errors);
        if (!errors.isEmpty()) throw new BulkValidationException(errors);
    }

    // ─────────────────────────────────────────────────────────────
    // VALIDATION — Jigyousha (company consent)
    // ─────────────────────────────────────────────────────────────

    @Override
    public void validateJigyousha(JigyoushaValidateRequest request) {
        List<ValidationErrorDetail> errors = new ArrayList<>();
        validateJigyoushaCore(request.getCompanyName(), request.getRepName(),
                request.getRepRole(), request.getHasConsent(), errors);
        if (!errors.isEmpty()) throw new BulkValidationException(errors);
    }

    @Override
    public void validateJigyousha(EquipmentRegisterSolarRequest request) {
        List<ValidationErrorDetail> errors = new ArrayList<>();
        validateJigyoushaCore(request.getCompanyName(), request.getRepName(),
                request.getRepRole(), request.getHasConsent(), errors);
        if (!errors.isEmpty()) throw new BulkValidationException(errors);
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY — Jigyousha default auto
    // ─────────────────────────────────────────────────────────────

    @Override
    public JigyoushaDefaultResponse getJigyoushaDefault(Integer siteId, Integer projectId) {

        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        ResponseConstants.MSG_SITE_NOT_FOUND));

        projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        ResponseConstants.MSG_PROJECT_NOT_FOUND));

        siteProjectRelationRepository
                .findBySiteIdAndProjectId(siteId, projectId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        ResponseConstants.MSG_SITE_PROJECT_RELATION_NOT_FOUND));

        ParticipatingCompany company = site.getCompany();
        String repName = company.getRepLastName() + company.getRepFirstName();

        return JigyoushaDefaultResponse.builder()
                .companyName(company.getNameJa())
                .repName(repName)
                .repRole(company.getRepPosition())
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // VALIDATION — Final agreement
    // ─────────────────────────────────────────────────────────────

    @Override
    public void validateFinalAgreement(FinalAgreementValidateRequest request) {
        validateFinalAgreementCore(request.getIsAgreeTerms());
    }

    @Override
    public void validateFinalAgreement(EquipmentRegisterSolarRequest request) {
        validateFinalAgreementCore(request.getIsAgreeTerms());
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — common helper
    // ─────────────────────────────────────────────────────────────

    private ValidationErrorDetail err(String tempId, String field, String message) {
        return ValidationErrorDetail.builder()
                .tempId(tempId)
                .field(field)
                .message(message)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — Equipment collectors
    // ─────────────────────────────────────────────────────────────

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
            } else if (item.getEquipmentOpDate() != null
                    && item.getAdditionalEquipmentDate().isBefore(item.getEquipmentOpDate())) {
                errors.add(err(item.getTempId(), "additionalEquipmentDate",
                        "追加設備日は設備稼働日以降である必要があります。"));
            }

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

    private void validateRequiredNonFileFields(
            EquipmentItemSolarRequest item,
            List<ValidationErrorDetail> errors) {

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

        if (item.getEquipmentOpDate() == null) return;

        LocalDate today = LocalDate.now();
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
        } else if (item.getBatteryCapacity().compareTo(BigDecimal.ZERO) <= 0) {
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
        } else if (item.getSubsidyAmount().compareTo(BigDecimal.ZERO) <= 0) {
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

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — TNC shared core logic
    // ─────────────────────────────────────────────────────────────

    private void validateTncConsentCore(Boolean isAgreeDataUse, Boolean isAgreeExtraData,
                                         Boolean isAgreeTransfer, Boolean isNotRegistered,
                                         Boolean isKeidanrenParticipation,
                                         List<ValidationErrorDetail> errors) {
        if (!Boolean.TRUE.equals(isAgreeDataUse)) {
            errors.add(err("request", "isAgreeDataUse", "データ利用への同意が必要です。"));
        }
        if (!Boolean.TRUE.equals(isAgreeExtraData)) {
            errors.add(err("request", "isAgreeExtraData",
                    "本入会届に記載されていない情報の提供への同意が必要です。"));
        }
        if (!Boolean.TRUE.equals(isAgreeTransfer)) {
            errors.add(err("request", "isAgreeTransfer",
                    "環境価値(Ｊ－クレジット)の譲渡への同意が必要です。"));
        }
        if (!Boolean.TRUE.equals(isNotRegistered)) {
            errors.add(err("request", "isNotRegistered", "他のJ-クレジット制度への未登録確認が必要です。"));
        }
        if (isKeidanrenParticipation == null) {
            errors.add(err("request", "isKeidanrenParticipation",
                    "経団連カーボンニュートラル行動計画への参加状況の選択が必要です。"));
        }
    }

    private void validateTncFieldsCore(Boolean isEmitter, String emitterCode,
                                        Boolean isAdjustedEmissionsAdded,
                                        Boolean isSubjectToPeriodicReport, Boolean isPeriodicReportTypeOne,
                                        String energyBusinessCode, String factoryCode,
                                        List<ValidationErrorDetail> errors) {
        if (Boolean.TRUE.equals(isEmitter)) {
            if (emitterCode == null || emitterCode.isBlank() || !emitterCode.matches("\\d{9}")) {
                errors.add(err("request", "emitterCode",
                        "isEmitterがtrueの場合、emitterCodeは9桁の数字必須です。"));
            }
            if (!Boolean.TRUE.equals(isAdjustedEmissionsAdded)) {
                errors.add(err("request", "isAdjustedEmissionsAdded",
                        "isEmitterがtrueの場合、調整後排出量への加算への同意が必要です。"));
            }
        }
        if (Boolean.TRUE.equals(isSubjectToPeriodicReport)) {
            if (isPeriodicReportTypeOne == null) {
                errors.add(err("request", "isPeriodicReportTypeOne",
                        "isSubjectToPeriodicReportがtrueの場合、一種・二種の選択が必須です。"));
            }
            if (energyBusinessCode == null || !energyBusinessCode.matches("\\d{7}")) {
                errors.add(err("request", "energyBusinessCode",
                        "isSubjectToPeriodicReportがtrueの場合、energyBusinessCodeは7桁の数字必須です。"));
            }
            if (factoryCode == null || !factoryCode.matches("\\d{7}")) {
                errors.add(err("request", "factoryCode",
                        "isSubjectToPeriodicReportがtrueの場合、factoryCodeは7桁の数字必須です。"));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — JIGYOUSHA shared core logic
    // ─────────────────────────────────────────────────────────────

    private void validateJigyoushaCore(String companyName, String repName, String repRole,
                                        Boolean hasConsent, List<ValidationErrorDetail> errors) {
        if (companyName == null || companyName.isBlank()) {
            errors.add(err("request", "companyName", "事業者名は必須です。"));
        }
        if (repName == null || repName.isBlank()) {
            errors.add(err("request", "repName", "代表者名は必須です。"));
        }
        if (repRole == null || repRole.isBlank()) {
            errors.add(err("request", "repRole", "代表者役職は必須です。"));
        }
        if (!Boolean.TRUE.equals(hasConsent)) {
            errors.add(err("request", "hasConsent", "署名への同意が必要です。"));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — FINAL AGREEMENT shared core logic
    // ─────────────────────────────────────────────────────────────

    private void validateFinalAgreementCore(Boolean isAgreeTerms) {
        if (!Boolean.TRUE.equals(isAgreeTerms)) {
            throw new BulkValidationException(List.of(
                    err("request", "isAgreeTerms", "利用規約への同意が必要です。")
            ));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — Entity builders
    // ─────────────────────────────────────────────────────────────

    private SiteR002MemberData buildMemberDataEntity(
            EquipmentItemSolarRequest item,
            SiteProjectRelation relation) {
        return SiteR002MemberData.builder()
                .project(relation.getProject())
                .site(relation.getSite())
                .company(relation.getSite().getCompany())
                .methodologyNumber(item.getMethodologyNumber())
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

    private EnR002EquipmentFile buildFileEntity(
            FileItemSolarRequest fileReq,
            SiteR002MemberData savedMemberData) {
        return EnR002EquipmentFile.builder()
                // .memberData(savedMemberData)
                .fileType(fileReq.getFileType())
                .fileId(fileReq.getFileId())
                .s3Key(fileReq.getS3Key())
                // .originalName(fileReq.getFileName())
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — DynamoDB
    // ─────────────────────────────────────────────────────────────

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
                .companyId(String.valueOf(companyId))
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
                .isPeriodicReportTypeOne(request.getIsPeriodicReportTypeOne())
                .energyBusinessCode(request.getEnergyBusinessCode())
                .factoryCode(request.getFactoryCode())
                .companyName(request.getCompanyName())
                .repName(request.getRepName())
                .repRole(request.getRepRole())
                .hasConsent(request.getHasConsent())
                .isAgreeTerms(request.getIsAgreeTerms())
                .agreedAt(Instant.now().toString())
                .build();

        tncAgreementRepository.save(tnc);
    }
}