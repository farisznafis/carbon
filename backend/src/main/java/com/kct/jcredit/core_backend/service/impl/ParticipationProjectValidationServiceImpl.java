package com.kct.jcredit.core_backend.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

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
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.EnR002ConfirmationItems;
import com.kct.jcredit.core_backend.exception.BulkValidationException;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.service.ParticipationProjectService;
import com.kct.jcredit.core_backend.service.ParticipationProjectValidationService;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002ApplicationConfirmationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteR002MemberDataRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationProjectValidationServiceImpl implements ParticipationProjectValidationService {

    private static final Set<EquipmentFileType> REQUIRED_FILES = Set.of(
            EquipmentFileType.SOLAR_CONTRACT,
            EquipmentFileType.PANEL_CATALOG,
            EquipmentFileType.PCS_SPEC,
            EquipmentFileType.CONSTRUCTION_INVOICE,
            EquipmentFileType.ELECTRICITY_BILL
    );

    // ─────────────────────────────────────────────────────────────
    // PUBLIC — dipanggil dari controller (per halaman)
    //          dan dari ParticipationService (safety net)
    // ─────────────────────────────────────────────────────────────

    @Override
    public void validateEquipment(EnR002EquipmentRequest equipment) {
        List<ValidationErrorDetail> errors = new ArrayList<>();

        validateRequiredFields(equipment, errors);
        validateOperationDate(equipment, errors);
        validateRequiredFiles(equipment, errors);
        validateFileContents(equipment, errors);

        if (Boolean.TRUE.equals(equipment.getHasBattery()))
            validateBattery(equipment, errors);

        if (Boolean.TRUE.equals(equipment.getHasSubsidy()))
            validateSubsidy(equipment, errors);

        if (!errors.isEmpty()) throw new BulkValidationException(errors);
    }

    @Override
    public void validateConfirmationItems(EnR002ConfirmationItemRequest request) {
        List<ValidationErrorDetail> errors = new ArrayList<>();

        // Consent dasar — wajib semua true
        if (!Boolean.TRUE.equals(request.getIsAgreeDataUse()))
            errors.add(err("request", "isAgreeDataUse",
                    "データ利用への同意が必要です。"));
        if (!Boolean.TRUE.equals(request.getIsAgreeExtraData()))
            errors.add(err("request", "isAgreeExtraData",
                    "本入会届に記載されていない情報の提供への同意が必要です。"));
        if (!Boolean.TRUE.equals(request.getIsAgreeTransfer()))
            errors.add(err("request", "isAgreeTransfer",
                    "環境価値(Ｊ－クレジット)の譲渡への同意が必要です。"));
        if (!Boolean.TRUE.equals(request.getIsNotRegistered()))
            errors.add(err("request", "isNotRegistered",
                    "他のJ-クレジット制度への未登録確認が必要です。"));
        if (request.getIsKeidanrenParticipation() == null)
            errors.add(err("request", "isKeidanrenParticipation",
                    "経団連カーボンニュートラル行動計画への参加状況の選択が必要です。"));

        // Conditional: isEmitter
        if (Boolean.TRUE.equals(request.getIsEmitter())) {
            if (request.getEmitterCode() == null
                    || request.getEmitterCode().isBlank()
                    || !request.getEmitterCode().matches("\\d{9}"))
                errors.add(err("request", "emitterCode",
                        "isEmitterがtrueの場合、emitterCodeは9桁の数字必須です。"));
            if (!Boolean.TRUE.equals(request.getIsAdjustedEmissionsAdded()))
                errors.add(err("request", "isAdjustedEmissionsAdded",
                        "isEmitterがtrueの場合、調整後排出量への加算への同意が必要です。"));
        }

        // Conditional: isSubjectToPeriodicReport
        if (Boolean.TRUE.equals(request.getIsSubjectToPeriodicReport())) {
            if (request.getIsPeriodicReportTypeOne() == null)
                errors.add(err("request", "isPeriodicReportTypeOne",
                        "isSubjectToPeriodicReportがtrueの場合、一種・二種の選択が必須です。"));
            if (request.getEnergyBusinessCode() == null
                    || !request.getEnergyBusinessCode().matches("\\d{7}"))
                errors.add(err("request", "energyBusinessCode",
                        "isSubjectToPeriodicReportがtrueの場合、energyBusinessCodeは7桁の数字必須です。"));
            if (request.getFactoryCode() == null
                    || !request.getFactoryCode().matches("\\d{7}"))
                errors.add(err("request", "factoryCode",
                        "isSubjectToPeriodicReportがtrueの場合、factoryCodeは7桁の数字必須です。"));
        }

        if (!errors.isEmpty()) throw new BulkValidationException(errors);
    }

    @Override
    public void validateSustainabilityCommitment(EnR002SustainabilityCommitmentRequest request) {
        List<ValidationErrorDetail> errors = new ArrayList<>();

        if (request.getCompanyName() == null || request.getCompanyName().isBlank())
            errors.add(err("request", "companyName", "事業者名は必須です。"));
        if (request.getRepName() == null || request.getRepName().isBlank())
            errors.add(err("request", "repName", "代表者名は必須です。"));
        if (request.getRepRole() == null || request.getRepRole().isBlank())
            errors.add(err("request", "repRole", "代表者役職は必須です。"));
        if (!Boolean.TRUE.equals(request.getHasConsent()))
            errors.add(err("request", "hasConsent", "署名への同意が必要です。"));

        if (!errors.isEmpty()) throw new BulkValidationException(errors);
    }

    @Override
    public void validateFinalAgreement(EnR002FinalAgreementRequest request) {
        List<ValidationErrorDetail> errors = new ArrayList<>();

        if (!Boolean.TRUE.equals(request.getIsAgreeTerms()))
            errors.add(err("request", "isAgreeTerms", "利用規約への同意が必要です。"));

        if (!errors.isEmpty()) throw new BulkValidationException(errors);
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — Equipment field validators
    // ─────────────────────────────────────────────────────────────

    private void validateRequiredFields(EnR002EquipmentRequest item,
                                         List<ValidationErrorDetail> errors) {
        if (item.getEquipmentOpDate() == null)
            errors.add(err(item.getTempId(), "equipmentOpDate", "必須項目です。"));
        if (item.getNominalMaxOutput() != null
                && item.getNominalMaxOutput().compareTo(BigDecimal.ZERO) <= 0)
            errors.add(err(item.getTempId(), "nominalMaxOutput",
                    "定格最大出力は0より大きい値を入力してください。"));
        if (item.getPcsManufacturer() == null || item.getPcsManufacturer().isBlank())
            errors.add(err(item.getTempId(), "pcsManufacturer", "必須項目です。"));
        if (item.getPcsModel() == null || item.getPcsModel().isBlank())
            errors.add(err(item.getTempId(), "pcsModel", "必須項目です。"));
        if (item.getPcsUnitCount() == null || item.getPcsUnitCount() < 1)
            errors.add(err(item.getTempId(), "PcsUnitCount",
                    "1以上の数値を入力してください。"));
        if (item.getDeviceNo() == null || item.getDeviceNo().isBlank())
            errors.add(err(item.getTempId(), "deviceNo", "必須項目です。"));
        if (item.getPcsStandby() == null)
            errors.add(err(item.getTempId(), "PcsStandby", "必須項目です。"));
        if (item.getTotalInvestment() == null || item.getTotalInvestment() <= 0)
            errors.add(err(item.getTempId(), "totalInvestment",
                    "総投資額は0より大きい値を入力してください。"));
        if (item.getPreProjectUnitPrice() != null
                && item.getPreProjectUnitPrice().compareTo(BigDecimal.ZERO) <= 0)
            errors.add(err(item.getTempId(), "preProjectUnitPrice",
                    "プロジェクト前単価は0より大きい値を入力してください。"));
    }

    private void validateOperationDate(EnR002EquipmentRequest item,
                                        List<ValidationErrorDetail> errors) {
        if (item.getEquipmentOpDate() == null) return;
        LocalDate today      = LocalDate.now();
        LocalDate twoYearsAgo = today.minusYears(2);

        if (item.getEquipmentOpDate().isBefore(twoYearsAgo))
            errors.add(err(item.getTempId(), "equipmentOpDate",
                    "設備稼働日は申込日から2年以内である必要があります。"));
        if (item.getEquipmentOpDate().isAfter(today))
            errors.add(err(item.getTempId(), "equipmentOpDate",
                    "設備稼働日は未来の日付は設定できません。"));
    }

    private void validateRequiredFiles(EnR002EquipmentRequest item,
                                        List<ValidationErrorDetail> errors) {
        List<EnR002FileItemRequest> files = item.getFiles() == null
                ? Collections.emptyList() : item.getFiles();

        Set<EquipmentFileType> submitted = files.stream()
                .map(EnR002FileItemRequest::getFileType)
                .collect(Collectors.toSet());

        REQUIRED_FILES.forEach(required -> {
            if (!submitted.contains(required))
                errors.add(ValidationErrorDetail.builder()
                        .tempId(item.getTempId())
                        .field(required.getValue())
                        .message(required.getValue() + "ファイルは必須です。")
                        .build());
        });
    }

    private void validateFileContents(EnR002EquipmentRequest item,
                                       List<ValidationErrorDetail> errors) {
        if (item.getFiles() == null) return;
        item.getFiles().forEach(f -> {
            if (f.getS3Key() == null || f.getS3Key().isBlank())
                errors.add(err(item.getTempId(),
                        f.getFileType() != null ? f.getFileType().getValue() : "file",
                        "S3キーが不正です。ファイルを再アップロードしてください。"));
            if (f.getFileName() == null || f.getFileName().isBlank())
                errors.add(err(item.getTempId(),
                        f.getFileType() != null ? f.getFileType().getValue() : "file",
                        "ファイル名が不正です。"));
        });
    }

    private void validateBattery(EnR002EquipmentRequest item,
                                  List<ValidationErrorDetail> errors) {
        if (item.getBatteryMaker() == null || item.getBatteryMaker().isBlank())
            errors.add(err(item.getTempId(), "batteryMaker",
                    "hasBatteryがtrueの場合、batteryMakerは必須です。"));
        if (item.getBatteryModel() == null || item.getBatteryModel().isBlank())
            errors.add(err(item.getTempId(), "batteryModel",
                    "hasBatteryがtrueの場合、batteryModelは必須です。"));
        if (item.getBatteryCapacity() == null)
            errors.add(err(item.getTempId(), "batteryCapacity",
                    "hasBatteryがtrueの場合、batteryCapacity(kWh)は必須です。"));
        else if (item.getBatteryCapacity().compareTo(BigDecimal.ZERO) <= 0)
            errors.add(err(item.getTempId(), "batteryCapacity",
                    "バッテリー容量は0より大きい値を入力してください。"));

        boolean hasBatteryCatalog = item.getFiles().stream()
                .anyMatch(f -> EquipmentFileType.BATTERY_CATALOG.equals(f.getFileType()));
        if (!hasBatteryCatalog)
            errors.add(err(item.getTempId(),
                    EquipmentFileType.BATTERY_CATALOG.getValue(),
                    "hasBatteryがtrueの場合、battery_catalogファイルは必須です。"));
    }

    private void validateSubsidy(EnR002EquipmentRequest item,
                                  List<ValidationErrorDetail> errors) {
        if (item.getSubsidyName() == null || item.getSubsidyName().isBlank())
            errors.add(err(item.getTempId(), "subsidyName",
                    "hasSubsidyがtrueの場合、subsidyNameは必須です。"));
        if (item.getSubsidyAmount() == null)
            errors.add(err(item.getTempId(), "subsidyAmount",
                    "hasSubsidyがtrueの場合、subsidyAmountは必須です。"));
        else if (item.getSubsidyAmount().compareTo(BigDecimal.ZERO) <= 0)
            errors.add(err(item.getTempId(), "subsidyAmount",
                    "補助金額は0より大きい値を入力してください。"));

        boolean hasSubsidyNotice = item.getFiles().stream()
                .anyMatch(f -> EquipmentFileType.SUBSIDY_NOTICE.equals(f.getFileType()));
        if (!hasSubsidyNotice)
            errors.add(err(item.getTempId(),
                    EquipmentFileType.SUBSIDY_NOTICE.getValue(),
                    "hasSubsidyがtrueの場合、subsidy_noticeファイルは必須です。"));
    }

    private ValidationErrorDetail err(String tempId, String field, String message) {
        return ValidationErrorDetail.builder()
                .tempId(tempId).field(field).message(message).build();
    }

    @Override
    public void validateSustainabilityCommitment(EnR002SustainabilityCommitmentResponse request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'validateSustainabilityCommitment'");
    }
}

