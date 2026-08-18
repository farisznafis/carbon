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
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FileItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002SustainabilityCommitmentResponse;
import com.kct.jcredit.core_backend.exception.BulkValidationException;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.service.ParticipationProjectValidationService;

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
    // PUBLIC
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

        if (!Boolean.TRUE.equals(request.getIsAgreeDataUse()))
            errors.add(err("request",
                    "データ利用への同意",
                    "データ利用への同意が必要です。"));

        if (!Boolean.TRUE.equals(request.getIsAgreeExtraData()))
            errors.add(err("request",
                    "本入会届に記載された以外の情報の提供への同意",
                    "本入会届に記載された以外の情報の提供への同意が必要です。"));

        if (!Boolean.TRUE.equals(request.getIsAgreeTransfer()))
            errors.add(err("request",
                    "環境価値（J-クレジット）の譲渡への同意",
                    "環境価値（J-クレジット）の譲渡への同意が必要です。"));

        if (!Boolean.TRUE.equals(request.getIsNotRegistered()))
            errors.add(err("request",
                    "他のJ-クレジット制度への未登録確認",
                    "他のJ-クレジット制度への未登録確認が必要です。"));

        if (request.getIsKeidanrenParticipation() == null)
            errors.add(err("request",
                    "経団連カーボンニュートラル行動計画への参加状況",
                    "選択してください。"));

        // Conditional: isEmitter
        if (Boolean.TRUE.equals(request.getIsEmitter())) {

            if (request.getEmitterCode() == null
                    || request.getEmitterCode().isBlank()
                    || !request.getEmitterCode().matches("\\d{9}"))
                errors.add(err("request",
                        "事業者コード（9桁）",
                        "9桁の数字を入力してください。"));

            if (!Boolean.TRUE.equals(request.getIsAdjustedEmissionsAdded()))
                errors.add(err("request",
                        "調整後排出量への加算",
                        "同意が必要です。"));
        }

        // Conditional: isSubjectToPeriodicReport
        if (Boolean.TRUE.equals(request.getIsSubjectToPeriodicReport())) {

            if (request.getIsPeriodicReportTypeOne() == null)
                errors.add(err("request",
                        "第一種・第二種区分",
                        "選択してください。"));

            if (request.getEnergyBusinessCode() == null
                    || !request.getEnergyBusinessCode().matches("\\d{7}"))
                errors.add(err("request",
                        "事業者番号",
                        "7桁の数字を入力してください。"));

            if (request.getFactoryCode() == null
                    || !request.getFactoryCode().matches("\\d{7}"))
                errors.add(err("request",
                        "工場等番号",
                        "7桁の数字を入力してください。"));
        }

        if (!errors.isEmpty()) throw new BulkValidationException(errors);
    }

    @Override
    public void validateSustainabilityCommitment(EnR002SustainabilityCommitmentRequest request) {
        List<ValidationErrorDetail> errors = new ArrayList<>();

        if (request.getCompanyName() == null || request.getCompanyName().isBlank())
            errors.add(err("request",
                    "事業者名",
                    "必須項目です。"));

        if (request.getRepName() == null || request.getRepName().isBlank())
            errors.add(err("request",
                    "代表者名",
                    "必須項目です。"));

        if (request.getRepRole() == null || request.getRepRole().isBlank())
            errors.add(err("request",
                    "代表者役職",
                    "必須項目です。"));

        if (!Boolean.TRUE.equals(request.getHasConsent()))
            errors.add(err("request",
                    "同意する",
                    "同意してください。"));

        if (!errors.isEmpty()) throw new BulkValidationException(errors);
    }

    @Override
    public void validateFinalAgreement(EnR002FinalAgreementRequest request) {
        List<ValidationErrorDetail> errors = new ArrayList<>();

        if (!Boolean.TRUE.equals(request.getIsAgreeTerms()))
            errors.add(err("request",
                    "同意します",
                    "同意してください。"));

        if (!errors.isEmpty()) throw new BulkValidationException(errors);
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — Equipment field validators
    // ─────────────────────────────────────────────────────────────

    private void validateRequiredFields(EnR002EquipmentRequest item,
                                    List<ValidationErrorDetail> errors) {
                                        
        if (item.getSiteId() == null)        errors.add(err(item.getTempId(),"拠点","必須項目です。"));

        if (item.getEquipmentOpDate() == null)
            errors.add(err(item.getTempId(),
                    "稼働開始日（電力受給開始日）",
                    "必須項目です。"));

        if (item.getNominalMaxOutput() != null
                && item.getNominalMaxOutput().compareTo(BigDecimal.ZERO) <= 0)
            errors.add(err(item.getTempId(),
                    "公称最大出力 (kW)",
                    "0より大きい値を入力してください。"));

        if (item.getPcsManufacturer() == null || item.getPcsManufacturer().isBlank())
            errors.add(err(item.getTempId(),
                    "メーカー",
                    "必須項目です。"));

        if (item.getPcsModel() == null || item.getPcsModel().isBlank())
            errors.add(err(item.getTempId(),
                    "型式",
                    "必須項目です。"));

        if (item.getPcsUnitCount() == null || item.getPcsUnitCount() < 1)
            errors.add(err(item.getTempId(),
                    "導入台数 (台)",
                    "1以上の数値を入力してください。"));

        if (item.getDeviceNo() == null || item.getDeviceNo().isBlank())
            errors.add(err(item.getTempId(),
                    "機器固有番号（製造番号）",
                    "必須項目です。"));

        if (item.getPcsStandby() == null)
            errors.add(err(item.getTempId(),
                    "夜間待機電力（1台あたり） (W)",
                    "必須項目です。"));

        if (item.getTotalInvestment() == null || item.getTotalInvestment() <= 0)
            errors.add(err(item.getTempId(),
                    "設備の総投資額 (円)",
                    "0より大きい値を入力してください。"));

        if (item.getPreProjectUnitPrice() != null
                && item.getPreProjectUnitPrice().compareTo(BigDecimal.ZERO) <= 0)
            errors.add(err(item.getTempId(),
                    "プロジェクト実施前の電力購入単価 (円/kWh)",
                    "0より大きい値を入力してください。"));
    }

        private void validateOperationDate(EnR002EquipmentRequest item,
                                    List<ValidationErrorDetail> errors) {
        if (item.getEquipmentOpDate() == null) return;

        LocalDate today = LocalDate.now();
        LocalDate twoYearsAgo = today.minusYears(2);

        if (item.getEquipmentOpDate().isBefore(twoYearsAgo))
            errors.add(err(item.getTempId(),
                    "稼働開始日（電力受給開始日）",
                    "申込日から2年以内の日付を入力してください。"));

        if (item.getEquipmentOpDate().isAfter(today))
            errors.add(err(item.getTempId(),
                    "稼働開始日（電力受給開始日）",
                    "未来の日付は入力できません。"));
    }

        private void validateRequiredFiles(EnR002EquipmentRequest item,
                                    List<ValidationErrorDetail> errors) {

        List<EnR002FileItemRequest> files = item.getFiles() == null
                ? Collections.emptyList()
                : item.getFiles();

        Set<EquipmentFileType> submitted = files.stream()
                .map(EnR002FileItemRequest::getFileType)
                .collect(Collectors.toSet());

        REQUIRED_FILES.forEach(required -> {
            if (!submitted.contains(required)) {

                String field;
                String message;

                switch (required) {
                    case SOLAR_CONTRACT:
                        field = "確認資料：太陽光発電電力受給契約確認書";
                        message = "太陽光発電電力受給契約確認書を添付してください。";
                        break;

                    case PANEL_CATALOG:
                        field = "確認資料：仕様書・カタログ";
                        message = "仕様書・カタログを添付してください。";
                        break;

                    case PCS_SPEC:
                        field = "確認資料：仕様書・カタログ";
                        message = "仕様書・カタログを添付してください。";
                        break;

                    case CONSTRUCTION_INVOICE:
                        field = "確認資料：工事請負契約書、請求書、領収書等";
                        message = "工事請負契約書、請求書、領収書等を添付してください。";
                        break;

                    case ELECTRICITY_BILL:
                        field = "確認資料：電力会社からの請求明細書等";
                        message = "電力会社からの請求明細書等を添付してください。";
                        break;

                    default:
                        field = "確認資料";
                        message = "確認資料を添付してください。";
                        break;
                }

                errors.add(ValidationErrorDetail.builder()
                        .tempId(item.getTempId())
                        .field(field)
                        .message(message)
                        .build());
                    }
                }
            );
    }

    private void validateFileContents(EnR002EquipmentRequest item,
                                  List<ValidationErrorDetail> errors) {

    if (item.getFiles() == null) return;

    item.getFiles().forEach(f -> {

        String field;

        if (f.getFileType() == null) {
            field = "確認資料";
        } else {
            switch (f.getFileType()) {
                case SOLAR_CONTRACT:
                    field = "確認資料：太陽光発電電力受給契約確認書";
                    break;

                case PANEL_CATALOG:
                    field = "確認資料：太陽光パネル仕様書・カタログ";
                    break;

                case PCS_SPEC:
                    field = "確認資料：PCS仕様書・カタログ";
                    break;

                case CONSTRUCTION_INVOICE:
                    field = "確認資料：工事請負契約書、請求書、領収書等";
                    break;

                case ELECTRICITY_BILL:
                    field = "確認資料：電力会社からの請求明細書等";
                    break;

                case BATTERY_CATALOG:
                    field = "確認資料：蓄電池仕様書・カタログ";
                    break;

                case SUBSIDY_NOTICE:
                    field = "確認資料：補助金交付決定通知書";
                    break;

                default:
                    field = "確認資料";
            }
        }

        if (f.getS3Key() == null || f.getS3Key().isBlank())
            errors.add(err(item.getTempId(),
                    field,
                    "ファイルのアップロードに失敗しました。もう一度アップロードしてください。"));

        if (f.getFileName() == null || f.getFileName().isBlank())
            errors.add(err(item.getTempId(),
                    field,
                    "ファイル名を取得できませんでした。もう一度アップロードしてください。"));
    });
}

    private void validateBattery(EnR002EquipmentRequest item,
                             List<ValidationErrorDetail> errors) {

    if (item.getBatteryMaker() == null || item.getBatteryMaker().isBlank())
        errors.add(err(item.getTempId(),
                "蓄電池メーカー",
                "蓄電池を設置する場合は必須項目です。"));

    if (item.getBatteryModel() == null || item.getBatteryModel().isBlank())
        errors.add(err(item.getTempId(),
                "蓄電池型式",
                "蓄電池を設置する場合は必須項目です。"));

    if (item.getBatteryCapacity() == null)
        errors.add(err(item.getTempId(),
                "蓄電池容量 (kWh)",
                "蓄電池を設置する場合は必須項目です。"));

    else if (item.getBatteryCapacity().compareTo(BigDecimal.ZERO) <= 0)
        errors.add(err(item.getTempId(),
                "蓄電池容量 (kWh)",
                "0より大きい値を入力してください。"));

    boolean hasBatteryCatalog = item.getFiles().stream()
            .anyMatch(f -> EquipmentFileType.BATTERY_CATALOG.equals(f.getFileType()));

    if (!hasBatteryCatalog)
        errors.add(err(item.getTempId(),
                "確認資料：蓄電池仕様書・カタログ",
                "蓄電池を設置する場合は、蓄電池仕様書・カタログを添付してください。"));
}

    private void validateSubsidy(EnR002EquipmentRequest item,
                             List<ValidationErrorDetail> errors) {

    if (item.getSubsidyName() == null || item.getSubsidyName().isBlank())
        errors.add(err(item.getTempId(),
                "補助金名称",
                "補助金を受給する場合は必須項目です。"));

    if (item.getSubsidyAmount() == null)
        errors.add(err(item.getTempId(),
                "補助金額 (円)",
                "補助金を受給する場合は必須項目です。"));

    else if (item.getSubsidyAmount().compareTo(BigDecimal.ZERO) <= 0)
        errors.add(err(item.getTempId(),
                "補助金額 (円)",
                "0より大きい値を入力してください。"));

    boolean hasSubsidyNotice = item.getFiles().stream()
            .anyMatch(f -> EquipmentFileType.SUBSIDY_NOTICE.equals(f.getFileType()));

    if (!hasSubsidyNotice)
        errors.add(err(item.getTempId(),
                "確認資料：補助金交付決定通知書",
                "補助金を受給する場合は、補助金交付決定通知書を添付してください。"));
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
    // PATCH専用バリデーター
    public static class EquipmentUpdateValidator {

        private EquipmentUpdateValidator() {
            // static utility — インスタンス化不要
        }

        public static void validateEquipmentEntityState(String tempId, EnR002Equipment equipment,
                                                          List<EnR002EquipmentFile> currentFiles) {
            List<ValidationErrorDetail> errors = new ArrayList<>();

            validateRequiredFields(tempId, equipment, errors);
            validateRequiredFiles(tempId, currentFiles, errors);

            if (Boolean.TRUE.equals(equipment.getHasBattery())) {
                boolean hasBatteryCatalog = currentFiles.stream()
                        .anyMatch(f -> EquipmentFileType.BATTERY_CATALOG.equals(f.getFileType()));
                validateBattery(tempId, equipment, hasBatteryCatalog, errors);
            }

            if (Boolean.TRUE.equals(equipment.getHasSubsidy())) {
                boolean hasSubsidyNotice = currentFiles.stream()
                        .anyMatch(f -> EquipmentFileType.SUBSIDY_NOTICE.equals(f.getFileType()));
                validateSubsidy(tempId, equipment, hasSubsidyNotice, errors);
            }

            if (!errors.isEmpty()) throw new BulkValidationException(errors);
        }
            public static void validateNewFileContents(String tempId, List<EnR002FileItemRequest> files) {
                List<ValidationErrorDetail> errors = new ArrayList<>();
                files.forEach(f -> {
                    String field = fieldNameFor(f.getFileType());
                    if (f.getS3Key() == null || f.getS3Key().isBlank())
                        errors.add(err(tempId, field,
                                "ファイルのアップロードに失敗しました。もう一度アップロードしてください。"));
                    if (f.getFileName() == null || f.getFileName().isBlank())
                        errors.add(err(tempId, field,
                                "ファイル名を取得できませんでした。もう一度アップロードしてください。"));
                });
                if (!errors.isEmpty()) throw new BulkValidationException(errors);
            }

        private static void validateRequiredFiles(String tempId, List<EnR002EquipmentFile> currentFiles,
                                               List<ValidationErrorDetail> errors) {

        Set<EquipmentFileType> submitted = currentFiles.stream()
                .map(EnR002EquipmentFile::getFileType)
                .collect(Collectors.toSet());

        REQUIRED_FILES.forEach(required -> {
            if (!submitted.contains(required)) {

                String field;
                switch (required) {
                    case SOLAR_CONTRACT:
                        field = "確認資料：太陽光発電電力受給契約確認書"; break;
                    case PANEL_CATALOG:
                        field = "確認資料：仕様書・カタログ"; break;
                    case PCS_SPEC:
                        field = "確認資料：仕様書・カタログ"; break;
                    case CONSTRUCTION_INVOICE:
                        field = "確認資料：工事請負契約書、請求書、領収書等"; break;
                    case ELECTRICITY_BILL:
                        field = "確認資料：電力会社からの請求明細書等"; break;
                    default:
                        field = "確認資料";
                }

                errors.add(err(tempId, field, required.getValue() + "の添付が必要です。"));
            }
        });
    }

        private static void validateRequiredFields(String tempId, EnR002Equipment e,
                                                     List<ValidationErrorDetail> errors) {

            if (e.getEquipmentOpDate() == null)
                errors.add(err(tempId, "稼働開始日（電力受給開始日）", "必須項目です。"));

            if (e.getNominalMaxOutput() != null
                    && e.getNominalMaxOutput().compareTo(BigDecimal.ZERO) <= 0)
                errors.add(err(tempId, "公称最大出力 (kW)", "0より大きい値を入力してください。"));

            if (e.getPcsManufacturer() == null || e.getPcsManufacturer().isBlank())
                errors.add(err(tempId, "メーカー", "必須項目です。"));

            if (e.getPcsModel() == null || e.getPcsModel().isBlank())
                errors.add(err(tempId, "型式", "必須項目です。"));

            if (e.getPcsUnitCount() == null || e.getPcsUnitCount() < 1)
                errors.add(err(tempId, "導入台数 (台)", "1以上の数値を入力してください。"));

            if (e.getDeviceNo() == null || e.getDeviceNo().isBlank())
                errors.add(err(tempId, "機器固有番号（製造番号）", "必須項目です。"));

            if (e.getPcsStandby() == null)
                errors.add(err(tempId, "夜間待機電力（1台あたり） (W)", "必須項目です。"));

            if (e.getTotalInvestment() == null || e.getTotalInvestment() <= 0)
                errors.add(err(tempId, "設備の総投資額 (円)", "0より大きい値を入力してください。"));

            if (e.getPreProjectUnitPrice() != null
                    && e.getPreProjectUnitPrice().compareTo(BigDecimal.ZERO) <= 0)
                errors.add(err(tempId, "プロジェクト実施前の電力購入単価 (円/kWh)",
                        "0より大きい値を入力してください。"));
        }

        private static void validateBattery(String tempId, EnR002Equipment e, boolean hasBatteryCatalog,
                                             List<ValidationErrorDetail> errors) {

            if (e.getBatteryMaker() == null || e.getBatteryMaker().isBlank())
                errors.add(err(tempId, "蓄電池メーカー", "蓄電池を設置する場合は必須項目です。"));

            if (e.getBatteryModel() == null || e.getBatteryModel().isBlank())
                errors.add(err(tempId, "蓄電池型式", "蓄電池を設置する場合は必須項目です。"));

            if (e.getBatteryCapacity() == null)
                errors.add(err(tempId, "蓄電池容量 (kWh)", "蓄電池を設置する場合は必須項目です。"));
            else if (e.getBatteryCapacity().compareTo(BigDecimal.ZERO) <= 0)
                errors.add(err(tempId, "蓄電池容量 (kWh)", "0より大きい値を入力してください。"));

            if (!hasBatteryCatalog)
                errors.add(err(tempId, "確認資料：蓄電池仕様書・カタログ",
                        "蓄電池を設置する場合は、蓄電池仕様書・カタログを添付してください。"));
        }

        private static void validateSubsidy(String tempId, EnR002Equipment e, boolean hasSubsidyNotice,
                                             List<ValidationErrorDetail> errors) {

            if (e.getSubsidyName() == null || e.getSubsidyName().isBlank())
                errors.add(err(tempId, "補助金名称", "補助金を受給する場合は必須項目です。"));

            if (e.getSubsidyAmount() == null)
                errors.add(err(tempId, "補助金額 (円)", "補助金を受給する場合は必須項目です。"));
            else if (e.getSubsidyAmount().compareTo(BigDecimal.ZERO) <= 0)
                errors.add(err(tempId, "補助金額 (円)", "0より大きい値を入力してください。"));

            if (!hasSubsidyNotice)
                errors.add(err(tempId, "確認資料：補助金交付決定通知書",
                        "補助金を受給する場合は、補助金交付決定通知書を添付してください。"));
        }

        private static String fieldNameFor(EquipmentFileType type) {
            if (type == null) return "確認資料";
            switch (type) {
                case SOLAR_CONTRACT: return "確認資料：太陽光発電電力受給契約確認書";
                case PANEL_CATALOG: return "確認資料：太陽光パネル仕様書・カタログ";
                case PCS_SPEC: return "確認資料：PCS仕様書・カタログ";
                case CONSTRUCTION_INVOICE: return "確認資料：工事請負契約書、請求書、領収書等";
                case ELECTRICITY_BILL: return "確認資料：電力会社からの請求明細書等";
                case BATTERY_CATALOG: return "確認資料：蓄電池仕様書・カタログ";
                case SUBSIDY_NOTICE: return "確認資料：補助金交付決定通知書";
                default: return "確認資料";
            }
        }

        private static ValidationErrorDetail err(String tempId, String field, String message) {
            return ValidationErrorDetail.builder()
                    .tempId(tempId).field(field).message(message).build();
        }
    }
}

