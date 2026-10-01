package com.kct.jcredit.core_backend.siteprojectcontrollertest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FileItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.dto.response.details.ValidationErrorDetail;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.exception.BulkValidationException;
import com.kct.jcredit.core_backend.service.impl.ParticipationProjectValidationServiceImpl;
// NOTE: this service has no dependencies, so it is instantiated directly
// instead of going through Mockito.
// NOTE: BulkValidationException does not keep the List<ValidationErrorDetail> it is
// constructed with. Its constructor flattens the list into CustomException's error map,
// keyed "<tempId>::<field>" with the message as the value, so the tests below read that
// map instead of the detail objects. errorMap() locates it reflectively so the tests do
// not depend on the accessor name CustomException happens to expose.
@DisplayName("ParticipationProjectValidationServiceImpl のテスト")
class ParticipationProjectValidationServiceImplTest {

    private ParticipationProjectValidationServiceImpl validationService;

    @BeforeEach
    void setUp() {
        validationService = new ParticipationProjectValidationServiceImpl();
    }

    private static final String TEMP_ID = "TMP-001";

    // ─────────────────────────────────────────────────────────────
    // Fixtures
    // ─────────────────────────────────────────────────────────────

    /** A file item that passes validateFileContents. */
    private EnR002FileItemRequest file(EquipmentFileType type) {
        EnR002FileItemRequest f = new EnR002FileItemRequest();
        f.setFileType(type);
        f.setFileId(UUID.randomUUID().toString());
        f.setS3Key("tmp/equipments/" + UUID.randomUUID() + "/" + type.getValue() + "_doc.pdf");
        f.setFileName(type.getValue() + "_doc.pdf");
        return f;
    }

    /** The five files REQUIRED_FILES demands. */
    private List<EnR002FileItemRequest> requiredFiles() {
        return new ArrayList<>(List.of(
                file(EquipmentFileType.SOLAR_CONTRACT),
                file(EquipmentFileType.PANEL_CATALOG),
                file(EquipmentFileType.PCS_SPEC),
                file(EquipmentFileType.CONSTRUCTION_INVOICE),
                file(EquipmentFileType.ELECTRICITY_BILL)));
    }

    /** A request that passes validateEquipment with no battery and no subsidy. */
    private EnR002EquipmentRequest validEquipment() {
        EnR002EquipmentRequest r = new EnR002EquipmentRequest();
        r.setTempId(TEMP_ID);
        r.setSiteId("PUB-SITE-001");
        r.setEquipmentOpDate(LocalDate.now().minusMonths(6));
        r.setNominalMaxOutput(new BigDecimal("49.5"));
        r.setPcsManufacturer("MakerA");
        r.setPcsModel("Model-X");
        r.setDeviceNo("SN-0001");
        r.setPcsStandby(new BigDecimal("3.5"));
        r.setTotalInvestment(5_000_000);
        r.setPreProjectUnitPrice(new BigDecimal("18.5"));
        r.setHasBattery(false);
        r.setHasSubsidy(false);
        r.setFiles(requiredFiles());
        return r;
    }

    private EnR002ConfirmationItemRequest validConfirmationItems() {
        EnR002ConfirmationItemRequest r = new EnR002ConfirmationItemRequest();
        r.setIsAgreeDataUse(true);
        r.setIsAgreeExtraData(true);
        r.setIsAgreeTransfer(true);
        r.setIsNotRegistered(true);
        r.setIsKeidanrenParticipation(false);
        r.setIsEmitter(false);
        r.setIsSubjectToPeriodicReport(false);
        return r;
    }

    private EnR002SustainabilityCommitmentRequest validCommitment() {
        EnR002SustainabilityCommitmentRequest r = new EnR002SustainabilityCommitmentRequest();
        r.setCompanyName("株式会社テスト");
        r.setRepName("熊本太郎");
        r.setRepRole("代表取締役");
        r.setHasConsent(true);
        return r;
    }

    /** Reads the flattened "<tempId>::<field>" -> message map out of the exception. */
    @SuppressWarnings("unchecked")
    private Map<String, String> errorMap(BulkValidationException ex) {
        for (Class<?> c = ex.getClass(); c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    try {
                        Map<String, String> m = (Map<String, String>) f.get(ex);
                        return m == null ? Map.of() : m;
                    } catch (IllegalAccessException e) {
                        throw new AssertionError("error map not readable", e);
                    }
                }
            }
        }
        throw new AssertionError("no error map found on " + ex.getClass().getName());
    }

    /** Field names, with the "<tempId>::" key prefix stripped. */
    private List<String> fieldsOf(BulkValidationException ex) {
        return errorMap(ex).keySet().stream()
                .map(k -> k.contains("::") ? k.substring(k.indexOf("::") + 2) : k)
                .collect(Collectors.toList());
    }

    /** All error messages, regardless of which field they belong to. */
    private List<String> messagesOf(BulkValidationException ex) {
        return new ArrayList<>(errorMap(ex).values());
    }

    // ==================================================================
    // 02. validateEquipment
    // ==================================================================

    @Test
    @DisplayName("UT-SPS-02-01: 正常 - 全項目が有効で蓄電池・補助金なしの場合、例外はスローされない")
    void validateEquipment_allValidNoBatteryNoSubsidy_doesNotThrow() {
        assertDoesNotThrow(() -> validationService.validateEquipment(validEquipment()));
    }

    @Test
    @DisplayName("UT-SPS-02-02: 正常 - 蓄電池・補助金ありで条件付き項目と証憑が揃っている場合、例外はスローされない")
    void validateEquipment_withBatteryAndSubsidy_doesNotThrow() {
        EnR002EquipmentRequest r = validEquipment();
        r.setHasBattery(true);
        r.setBatteryMaker("BattMaker");
        r.setBatteryModel("Batt-100");
        r.setBatteryCapacity(new BigDecimal("13.5"));
        r.setHasSubsidy(true);
        r.setSubsidyName("再エネ補助金");
        r.setSubsidyAmount(new BigDecimal("1000000"));
        r.getFiles().add(file(EquipmentFileType.BATTERY_CATALOG));
        r.getFiles().add(file(EquipmentFileType.SUBSIDY_NOTICE));

        assertDoesNotThrow(() -> validationService.validateEquipment(r));
    }

    @Test
    @DisplayName("UT-SPS-02-03: 異常 - 必須項目が全て未入力の場合、項目ごとにエラーが収集される")
    void validateEquipment_allRequiredFieldsMissing_collectsAllErrors() {
        EnR002EquipmentRequest r = validEquipment();
        r.setSiteId(null);
        r.setEquipmentOpDate(null);
        r.setPcsManufacturer(null);
        r.setPcsModel("   ");
        r.setDeviceNo(null);
        r.setPcsStandby(null);
        r.setTotalInvestment(null);

        BulkValidationException ex = assertThrows(BulkValidationException.class,
                () -> validationService.validateEquipment(r));

        assertThat(fieldsOf(ex)).contains(
                "拠点",
                "稼働開始日（電力受給開始日）",
                "メーカー",
                "型式",
                "機器固有番号（製造番号）",
                "夜間待機電力（1台あたり） (W)",
                "設備の総投資額 (円)");
    }

    @Test
    @DisplayName("UT-SPS-02-04: 異常 - 稼働開始日が2年より前の場合エラーになる")
    void validateEquipment_operationDateOlderThanTwoYears_throws() {
        EnR002EquipmentRequest r = validEquipment();
        r.setEquipmentOpDate(LocalDate.now().minusYears(2).minusDays(1));

        BulkValidationException ex = assertThrows(BulkValidationException.class,
                () -> validationService.validateEquipment(r));

        assertThat(messagesOf(ex)).anyMatch(m -> m.contains("2年以内"));
    }

    @Test
    @DisplayName("UT-SPS-02-05: 異常 - 稼働開始日が未来日付の場合エラーになる")
    void validateEquipment_operationDateInFuture_throws() {
        EnR002EquipmentRequest r = validEquipment();
        r.setEquipmentOpDate(LocalDate.now().plusDays(1));

        BulkValidationException ex = assertThrows(BulkValidationException.class,
                () -> validationService.validateEquipment(r));

        assertThat(messagesOf(ex)).anyMatch(m -> m.contains("未来の日付"));
    }

    @Test
    @DisplayName("UT-SPS-02-06: 準正常 - 稼働開始日がちょうど2年前・本日の場合は許容される（境界値）")
    void validateEquipment_operationDateBoundaries_areAccepted() {
        EnR002EquipmentRequest exactlyTwoYearsAgo = validEquipment();
        exactlyTwoYearsAgo.setEquipmentOpDate(LocalDate.now().minusYears(2));
        assertDoesNotThrow(() -> validationService.validateEquipment(exactlyTwoYearsAgo));

        EnR002EquipmentRequest today = validEquipment();
        today.setEquipmentOpDate(LocalDate.now());
        assertDoesNotThrow(() -> validationService.validateEquipment(today));
    }

    @Test
    @DisplayName("UT-SPS-02-07: 異常 - 必須証憑が1件不足している場合、その証憑のエラーが返る")
    void validateEquipment_missingRequiredFile_throws() {
        EnR002EquipmentRequest r = validEquipment();
        r.getFiles().removeIf(f -> f.getFileType() == EquipmentFileType.ELECTRICITY_BILL);

        BulkValidationException ex = assertThrows(BulkValidationException.class,
                () -> validationService.validateEquipment(r));

        assertThat(fieldsOf(ex)).contains("確認資料：電力会社からの請求明細書等");
    }

    @Test
    @DisplayName("UT-SPS-02-08: 異常 - 証憑のs3Key・ファイル名が空の場合それぞれエラーになる【要修正】")
    void validateEquipment_fileWithBlankKeyAndName_throws() {
        EnR002EquipmentRequest r = validEquipment();
        EnR002FileItemRequest broken = r.getFiles().get(0);
        broken.setS3Key("  ");
        broken.setFileName(null);

        BulkValidationException ex = assertThrows(BulkValidationException.class,
                () -> validationService.validateEquipment(r));

        assertThat(messagesOf(ex))
                .as("同一ファイルの2件のエラーは両方返るべき。実際の内容: %s", errorMap(ex))
                .anyMatch(m -> m.contains("アップロードに失敗"))
                .anyMatch(m -> m.contains("ファイル名を取得できませんでした"));
    }

    @Test
    @DisplayName("UT-SPS-02-09: 異常 - 蓄電池ありで蓄電池項目とカタログが未入力の場合、全てエラーになる")
    void validateEquipment_batteryWithoutFieldsAndCatalog_throws() {
        EnR002EquipmentRequest r = validEquipment();
        r.setHasBattery(true);

        BulkValidationException ex = assertThrows(BulkValidationException.class,
                () -> validationService.validateEquipment(r));

        assertThat(fieldsOf(ex)).contains(
                "蓄電池メーカー",
                "蓄電池型式",
                "蓄電池容量 (kWh)",
                "確認資料：蓄電池仕様書・カタログ");
    }

    @Test
    @DisplayName("UT-SPS-02-10: 異常 - 補助金ありで補助金項目と通知書が未入力の場合、全てエラーになる")
    void validateEquipment_subsidyWithoutFieldsAndNotice_throws() {
        EnR002EquipmentRequest r = validEquipment();
        r.setHasSubsidy(true);

        BulkValidationException ex = assertThrows(BulkValidationException.class,
                () -> validationService.validateEquipment(r));

        assertThat(fieldsOf(ex)).contains(
                "補助金名称",
                "補助金額 (円)",
                "確認資料：補助金交付決定通知書");
    }

    @Test
    @DisplayName("UT-SPS-02-11: 準正常 - 数値項目はnullなら検査されず0以下ならエラーになる（境界値）")
    void validateEquipment_numericFields_nullSkippedZeroRejected() {
        // nominalMaxOutput / preProjectUnitPrice are only checked when non-null.
        EnR002EquipmentRequest nulls = validEquipment();
        nulls.setNominalMaxOutput(null);
        nulls.setPreProjectUnitPrice(null);
        assertDoesNotThrow(() -> validationService.validateEquipment(nulls));

        EnR002EquipmentRequest zeros = validEquipment();
        zeros.setNominalMaxOutput(BigDecimal.ZERO);
        zeros.setPreProjectUnitPrice(BigDecimal.ZERO);
        zeros.setTotalInvestment(0);

        BulkValidationException ex = assertThrows(BulkValidationException.class,
                () -> validationService.validateEquipment(zeros));

        assertThat(fieldsOf(ex)).contains(
                "公称最大出力 (kW)",
                "プロジェクト実施前の電力購入単価 (円/kWh)",
                "設備の総投資額 (円)");
    }

    @Test
    @DisplayName("UT-SPS-02-12: 異常 - 蓄電池ありでfilesがnullの場合、NPEではなくバリデーションエラーを返すべき【要修正】")
    void validateEquipment_batteryWithNullFiles_shouldReturnValidationErrors() {
        // Pattern B: validateRequiredFiles/validateFileContents handle a null file list,
        // but validateBattery calls item.getFiles().stream() directly and throws NPE.
        EnR002EquipmentRequest r = validEquipment();
        r.setHasBattery(true);
        r.setBatteryMaker("BattMaker");
        r.setBatteryModel("Batt-100");
        r.setBatteryCapacity(new BigDecimal("13.5"));
        r.setFiles(null);

        assertThrows(BulkValidationException.class,
                () -> validationService.validateEquipment(r),
                "filesがnullでもNPEではなくバリデーションエラーとして返すべき");
    }

    // ==================================================================
    // 03. validateConfirmationItems
    // ==================================================================

    @Test
    @DisplayName("UT-SPS-03-01: 正常 - 全ての同意がありで特定排出者・定期報告対象外の場合、例外はスローされない")
    void validateConfirmationItems_allAgreedNoConditionals_doesNotThrow() {
        assertDoesNotThrow(() -> validationService.validateConfirmationItems(validConfirmationItems()));
    }

    @Test
    @DisplayName("UT-SPS-03-02: 正常 - 特定排出者ありで9桁コードと加算同意がある場合、例外はスローされない")
    void validateConfirmationItems_emitterWithValidCode_doesNotThrow() {
        EnR002ConfirmationItemRequest r = validConfirmationItems();
        r.setIsEmitter(true);
        r.setEmitterCode("123456789");
        r.setIsAdjustedEmissionsAdded(true);

        assertDoesNotThrow(() -> validationService.validateConfirmationItems(r));
    }

    @Test
    @DisplayName("UT-SPS-03-03: 正常 - 定期報告対象で区分と7桁コードが揃っている場合、例外はスローされない")
    void validateConfirmationItems_periodicReportWithValidCodes_doesNotThrow() {
        EnR002ConfirmationItemRequest r = validConfirmationItems();
        r.setIsSubjectToPeriodicReport(true);
        r.setIsPeriodicReportTypeOne(true);
        r.setEnergyBusinessCode("1234567");
        r.setFactoryCode("7654321");

        assertDoesNotThrow(() -> validationService.validateConfirmationItems(r));
    }

    @Test
    @DisplayName("UT-SPS-03-04: 異常 - 4つの同意項目が全て未同意の場合、それぞれエラーになる")
    void validateConfirmationItems_allAgreementsFalse_collectsFourErrors() {
        EnR002ConfirmationItemRequest r = validConfirmationItems();
        r.setIsAgreeDataUse(false);
        r.setIsAgreeExtraData(false);
        r.setIsAgreeTransfer(false);
        r.setIsNotRegistered(false);

        BulkValidationException ex = assertThrows(BulkValidationException.class,
                () -> validationService.validateConfirmationItems(r));

        assertThat(fieldsOf(ex)).contains(
                "データ利用への同意",
                "本入会届に記載された以外の情報の提供への同意",
                "環境価値（J-クレジット）の譲渡への同意",
                "他のJ-クレジット制度への未登録確認");
    }

    @Test
    @DisplayName("UT-SPS-03-05: 異常 - 経団連参加状況が未選択の場合エラーになる")
    void validateConfirmationItems_keidanrenNull_throws() {
        EnR002ConfirmationItemRequest r = validConfirmationItems();
        r.setIsKeidanrenParticipation(null);

        BulkValidationException ex = assertThrows(BulkValidationException.class,
                () -> validationService.validateConfirmationItems(r));

        assertThat(fieldsOf(ex)).contains("経団連カーボンニュートラル行動計画への参加状況");
    }

    @Test
    @DisplayName("UT-SPS-03-06: 異常 - 特定排出者ありで事業者コードが9桁でない場合エラーになる")
    void validateConfirmationItems_emitterCodeWrongFormat_throws() {
        EnR002ConfirmationItemRequest r = validConfirmationItems();
        r.setIsEmitter(true);
        r.setEmitterCode("12345");          // too short
        r.setIsAdjustedEmissionsAdded(true);

        BulkValidationException ex = assertThrows(BulkValidationException.class,
                () -> validationService.validateConfirmationItems(r));

        assertThat(fieldsOf(ex)).contains("事業者コード（9桁）");
    }

    @Test
    @DisplayName("UT-SPS-03-07: 異常 - 特定排出者ありで調整後排出量への加算が未同意の場合エラーになる")
    void validateConfirmationItems_emitterWithoutAdjustedConsent_throws() {
        EnR002ConfirmationItemRequest r = validConfirmationItems();
        r.setIsEmitter(true);
        r.setEmitterCode("123456789");
        r.setIsAdjustedEmissionsAdded(false);

        BulkValidationException ex = assertThrows(BulkValidationException.class,
                () -> validationService.validateConfirmationItems(r));

        assertThat(fieldsOf(ex)).contains("調整後排出量への加算");
    }

    @Test
    @DisplayName("UT-SPS-03-08: 異常 - 定期報告対象で区分未選択・コード形式不正の場合それぞれエラーになる")
    void validateConfirmationItems_periodicReportInvalid_throws() {
        EnR002ConfirmationItemRequest r = validConfirmationItems();
        r.setIsSubjectToPeriodicReport(true);
        r.setIsPeriodicReportTypeOne(null);
        r.setEnergyBusinessCode("12");       // not 7 digits
        r.setFactoryCode("ABCDEFG");         // not numeric

        BulkValidationException ex = assertThrows(BulkValidationException.class,
                () -> validationService.validateConfirmationItems(r));

        assertThat(fieldsOf(ex)).contains(
                "第一種・第二種区分",
                "事業者番号",
                "工場等番号");
    }

    @Test
    @DisplayName("UT-SPS-03-09: 異常 - 特定排出者該当が未選択(null)の場合エラーになるべき【要修正】")
    void validateConfirmationItems_emitterNull_shouldRequireSelection() {
        // Pattern B: the "選択してください。" check for a null isEmitter is nested inside
        // if (Boolean.TRUE.equals(isEmitter)), so it can never run. A null value
        // currently passes validation silently.
        EnR002ConfirmationItemRequest r = validConfirmationItems();
        r.setIsEmitter(null);

        assertThrows(BulkValidationException.class,
                () -> validationService.validateConfirmationItems(r),
                "特定排出者該当が未選択の場合は「選択してください。」を返すべき");
    }

    @Test
    @DisplayName("UT-SPS-03-10: 異常 - 定期報告対象者該当が未選択(null)の場合エラーになるべき【要修正】")
    void validateConfirmationItems_periodicReportNull_shouldRequireSelection() {
        // Pattern B: same unreachable null check as UT-SPS-03-09.
        EnR002ConfirmationItemRequest r = validConfirmationItems();
        r.setIsSubjectToPeriodicReport(null);

        assertThrows(BulkValidationException.class,
                () -> validationService.validateConfirmationItems(r),
                "定期報告対象者該当が未選択の場合は「選択してください。」を返すべき");
    }

    // ==================================================================
    // 04. validateSustainabilityCommitment
    // ==================================================================

    @Test
    @DisplayName("UT-SPS-04-01: 正常 - 全項目が入力され同意ありの場合、例外はスローされない")
    void validateSustainabilityCommitment_allValid_doesNotThrow() {
        assertDoesNotThrow(() -> validationService.validateSustainabilityCommitment(validCommitment()));
    }

    @Test
    @DisplayName("UT-SPS-04-02: 異常 - 全項目が未入力かつ未同意の場合、4件のエラーが収集される")
    void validateSustainabilityCommitment_allMissing_collectsFourErrors() {
        EnR002SustainabilityCommitmentRequest r = new EnR002SustainabilityCommitmentRequest();

        BulkValidationException ex = assertThrows(BulkValidationException.class,
                () -> validationService.validateSustainabilityCommitment(r));

        assertThat(fieldsOf(ex)).contains("事業者名", "代表者名", "代表者役職", "同意する");
    }

    @Test
    @DisplayName("UT-SPS-04-03: 異常 - 空白のみの文字列は未入力として扱われる")
    void validateSustainabilityCommitment_blankStrings_treatedAsMissing() {
        EnR002SustainabilityCommitmentRequest r = validCommitment();
        r.setCompanyName("   ");
        r.setRepName("");
        r.setRepRole("  ");

        BulkValidationException ex = assertThrows(BulkValidationException.class,
                () -> validationService.validateSustainabilityCommitment(r));

        assertThat(fieldsOf(ex)).contains("事業者名", "代表者名", "代表者役職");
    }

    // ==================================================================
    // 05. validateFinalAgreement
    // ==================================================================

    @Test
    @DisplayName("UT-SPS-05-01: 正常 - 規約に同意している場合、例外はスローされない")
    void validateFinalAgreement_agreed_doesNotThrow() {
        EnR002FinalAgreementRequest r = new EnR002FinalAgreementRequest();
        r.setIsAgreeTerms(true);

        assertDoesNotThrow(() -> validationService.validateFinalAgreement(r));
    }

    @Test
    @DisplayName("UT-SPS-05-02: 異常 - 規約に同意していない場合エラーになる")
    void validateFinalAgreement_notAgreed_throws() {
        EnR002FinalAgreementRequest r = new EnR002FinalAgreementRequest();
        r.setIsAgreeTerms(false);

        BulkValidationException ex = assertThrows(BulkValidationException.class,
                () -> validationService.validateFinalAgreement(r));

        assertThat(fieldsOf(ex)).contains("同意します");
    }

    @Test
    @DisplayName("UT-SPS-05-03: 異常 - 規約同意が未選択(null)の場合もエラーになる")
    void validateFinalAgreement_nullAgreement_throws() {
        EnR002FinalAgreementRequest r = new EnR002FinalAgreementRequest();
        r.setIsAgreeTerms(null);

        assertThrows(BulkValidationException.class,
                () -> validationService.validateFinalAgreement(r));
    }
}