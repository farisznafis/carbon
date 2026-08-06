package com.kct.jcredit.core_backend.service.batch.calculator;

// TODO: sesuaikan package & import dengan struktur project asli
// TODO: `SiteProjectRelation` di bawah ini nama tebakan untuk entity yang dikembalikan
//       siteProjectRelationRepo.findByProjectIdAndSiteId(...) — sesuaikan dengan nama aslinya.
//
// CATATAN NILAI EKSPEKTASI
// -----------------------------------------------------------------------------
// Semua angka ekspektasi di file ini DIHITUNG ULANG dari rumus di dalam kode,
// BUKAN disalin dari dummy SQL (en_r002_reduction_result). Alasannya:
//   - dummy SQL memakai faktor emisi flat ~0.000453, sementara kode menghitung
//     CEF dari CMO/CA_MAP + fungsi transisi f(t) → 0.000512 / 0.000477 / 0.000441
//   - baris dummy SQL adalah nilai PER HARI, sedangkan kode menyimpan nilai
//     KUMULATIF (batch saat ini + histori)
// Nilai MENTAH (ELpv, ELpvr, control, battery) tetap diambil dari dummy DynamoDB.
//
// KONSTANTA CEF (dari kode, tidak bisa diakses karena private):
//   CMO         = 0.000512
//   CA_MAP      = {2020:0.000460, 2021:0.000444, 2022:0.000436, 2023:0.000441, 2024:0.000438}
//   DEFAULT_CA  = 0.000441   ← dipakai untuk tahun di luar map (mis. 2026)
//   CEF = (CMO*(1-f) + Ca*f) setScale(6, HALF_UP)
//     f=0   → 0.000512
//     f=0.5 → 0.000512*0.5 + 0.000441*0.5 = 0.0004765 → 0.000477
//     f=1   → 0.000441
// -----------------------------------------------------------------------------

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResult;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultRepository;


@ExtendWith(MockitoExtension.class)
@DisplayName("SolarEmissionsCalculator のテスト")
class SolarEmissionCalculatorTest {

    @Mock
    private SiteProjectRelationRepository siteProjectRelationRepo;

    @Mock
    private EnR002ReductionResultRepository reductionResultRepo;

    @InjectMocks
    private SolarEmissionsCalculator calculator;

    private static final Integer PROJECT_ID = 1;
    private static final Integer SITE_ID = 1;
    private static final Integer BATCH_ID = 1;
    private static final Integer EQUIPMENT_ID = 1;

    private static final LocalDate PROCESSING_DATE = LocalDate.of(2026, 7, 5);

    /** f=0 (t < 1年) → CEF = 0.000512 */
    private static final LocalDate START_F0 = LocalDate.of(2026, 1, 1);
    /** 366日 → t=1.0021 → f=0.5 → CEF = 0.000477 */
    private static final LocalDate START_F05 = LocalDate.of(2025, 7, 4);
    /** 914日 → t=2.5024 → f=1 → CEF = 0.000441 */
    private static final LocalDate START_F1 = LocalDate.of(2024, 1, 3);

    // =========================================================================
    // ヘルパー
    // =========================================================================

    /**
     * Membuat mock monitoring record.
     *
     * Memakai lenient() karena tidak semua getter dipanggil di setiap skenario —
     * contohnya record dengan equipmentID null akan langsung tersaring keluar
     * sebelum getter lainnya sempat dibaca. Tanpa lenient(), Mockito strict-stubs
     * akan menggagalkan test dengan UnnecessaryStubbingException.
     */
    private EnR002MonitoringRecord record(Integer equipmentId, String elpv, String elpvr,
                                          String battery, String control) {
        EnR002MonitoringRecord r = mock(EnR002MonitoringRecord.class);
        lenient().when(r.getEquipmentID()).thenReturn(equipmentId);
        lenient().when(r.getElPv()).thenReturn(bd(elpv));
        lenient().when(r.getElPvr()).thenReturn(bd(elpvr));
        lenient().when(r.getElpjBatteryValue()).thenReturn(bd(battery));
        lenient().when(r.getElpjControlValue()).thenReturn(bd(control));
        return r;
    }

    private BigDecimal bd(String v) {
        return v == null ? null : new BigDecimal(v);
    }

    /** Mengatur activityStartDate yang dikembalikan siteProjectRelationRepo. */
    private void givenActivityStart(LocalDate startDate) {
        SiteProjectRelation relation = mock(SiteProjectRelation.class);
        when(relation.getActivityStartDate()).thenReturn(startDate);
        when(siteProjectRelationRepo.findByProjectIdAndSiteId(PROJECT_ID, SITE_ID))
                .thenReturn(relation);
    }

    /** Mengatur agar tidak ada histori untuk equipment tertentu. */
    private void givenNoHistory(Integer equipmentId) {
        when(reductionResultRepo
                .findTopByProjectIdAndSiteIdAndEquipmentIdOrderByBatchIdDesc(
                        PROJECT_ID, SITE_ID, equipmentId))
                .thenReturn(Optional.empty());
    }

    /** Membuat mock hasil historis dengan seluruh field terisi. */
    private EnR002ReductionResult history(String elpj, String elbl, String embl, String empj,
                                          String empjM, String empjS, String empjSBattery,
                                          String empjSControl, String er) {
        EnR002ReductionResult h = mock(EnR002ReductionResult.class);
        lenient().when(h.getElpj()).thenReturn(bd(elpj));
        lenient().when(h.getElbl()).thenReturn(bd(elbl));
        lenient().when(h.getEmbl()).thenReturn(bd(embl));
        lenient().when(h.getEmpj()).thenReturn(bd(empj));
        lenient().when(h.getEmpjM()).thenReturn(bd(empjM));
        lenient().when(h.getEmpjS()).thenReturn(bd(empjS));
        lenient().when(h.getEmpjSBattery()).thenReturn(bd(empjSBattery));
        lenient().when(h.getEmpjSControl()).thenReturn(bd(empjSControl));
        lenient().when(h.getEr()).thenReturn(bd(er));
        return h;
    }

    private List<EnR002ReductionResult> calculate(List<EnR002MonitoringRecord> records) {
        return calculator.calculateEmissions(PROJECT_ID, SITE_ID, BATCH_ID, records, PROCESSING_DATE);
    }

    /** Mencari hasil berdasarkan equipmentId — urutan hasil TIDAK dijamin (HashMap). */
    private EnR002ReductionResult findByEquipment(List<EnR002ReductionResult> results, Integer equipmentId) {
        return results.stream()
                .filter(r -> equipmentId.equals(r.getEquipmentId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Hasil untuk equipmentId " + equipmentId + " tidak ditemukan"));
    }

    // =========================================================================
    // A. 算定式の正確性（EN-R-002 方法論)
    // =========================================================================
    @Nested
    @DisplayName("【A】算定式の正確性")
    class FormulaCorrectness {

        @Test
        @DisplayName("A-1: 単一設備・単一レコード・control明示値の場合、全フィールドが方法論どおり算定される")
        void singleRecordWithExplicitControl_calculatesAllFieldsCorrectly() {
            // Arrange
            // ELpv=1000, ELpvr=200, battery=20, control=50, CEF=0.000512 (f=0)
            givenActivityStart(START_F0);
            givenNoHistory(EQUIPMENT_ID);

            // Act
            List<EnR002ReductionResult> results =
                    calculate(List.of(record(EQUIPMENT_ID, "1000", "200", "20", "50")));

            // Assert
            assertThat(results).hasSize(1);
            EnR002ReductionResult r = results.get(0);

            // 式7: ELPJ = ELpv - ELpvr = 1000 - 200 = 800
            assertThat(r.getElpj()).isEqualByComparingTo("800");
            // ELBL = ELPJ
            assertThat(r.getElbl()).isEqualByComparingTo("800");
            // 式8: EMBL = 800 × 0.000512 = 0.4096 → 0.410
            assertThat(r.getEmbl()).isEqualByComparingTo("0.410");
            // 式3: EMPJ,M = 0
            assertThat(r.getEmpjM()).isEqualByComparingTo("0");
            // 式5: EMPJ,S,control = 50 × 0.000512 = 0.0256 → 0.026
            assertThat(r.getEmpjSControl()).isEqualByComparingTo("0.026");
            // 式6: EMPJ,S,battery = 20 × 0.000512 = 0.01024 → 0.010
            assertThat(r.getEmpjSBattery()).isEqualByComparingTo("0.010");
            // 式4: EMPJ,S = 0.026 + 0.010 = 0.036
            assertThat(r.getEmpjS()).isEqualByComparingTo("0.036");
            // 式2: EMPJ = 0 + 0.036 = 0.036
            assertThat(r.getEmpj()).isEqualByComparingTo("0.036");
            // 式1: ER = EMBL - EMPJ = 0.410 - 0.036 = 0.374
            assertThat(r.getEr()).isEqualByComparingTo("0.374");

            // メタ情報
            assertThat(r.getBatchId()).isEqualTo(BATCH_ID);
            assertThat(r.getProjectId()).isEqualTo(PROJECT_ID);
            assertThat(r.getSiteId()).isEqualTo(SITE_ID);
            assertThat(r.getEquipmentId()).isEqualTo(EQUIPMENT_ID);
            assertThat(r.getProcessingDate()).isEqualTo(PROCESSING_DATE);
            assertThat(r.getStartTime()).isNotNull();
            assertThat(r.getEndTime()).isNotNull();
            assertThat(r.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("A-2: control が null の場合、デフォルト 0.02 kWh/kWh × ELpv が使われる")
        void controlValueNull_usesDefaultProxyRatio() {
            // Arrange
            givenActivityStart(START_F0);
            givenNoHistory(EQUIPMENT_ID);

            // Act — control = null
            List<EnR002ReductionResult> results =
                    calculate(List.of(record(EQUIPMENT_ID, "1000", "200", null, null)));

            // Assert
            EnR002ReductionResult r = results.get(0);
            // default: ELpv × 0.02 = 1000 × 0.02 = 20 kWh
            // EMPJ,S,control = 20 × 0.000512 = 0.01024 → 0.010
            assertThat(r.getEmpjSControl()).isEqualByComparingTo("0.010");
            // battery null → nullToZero → 0 → calculateBatteryEmissions返り値 0
            assertThat(r.getEmpjSBattery()).isEqualByComparingTo("0");
            assertThat(r.getEmpjS()).isEqualByComparingTo("0.010");
            assertThat(r.getEmbl()).isEqualByComparingTo("0.410");
            assertThat(r.getEr()).isEqualByComparingTo("0.400");
        }

        @Test
        @DisplayName("A-3: control が明示的に 0 の場合、デフォルト値ではなく 0 が尊重される")
        void controlValueExplicitZero_respectsZeroInsteadOfDefault() {
            // Skenario ini membedakan "user mengisi 0" dari "user tidak mengisi (null)".
            // Komentar di kode menegaskan perbedaan ini, jadi wajib dikunci oleh test.
            // Arrange
            givenActivityStart(START_F0);
            givenNoHistory(EQUIPMENT_ID);

            // Act — control = "0" (bukan null)
            List<EnR002ReductionResult> results =
                    calculate(List.of(record(EQUIPMENT_ID, "1000", "200", "0", "0")));

            // Assert — 0 × CEF = 0, BUKAN 0.010 seperti pada A-2
            EnR002ReductionResult r = results.get(0);
            assertThat(r.getEmpjSControl()).isEqualByComparingTo("0");
            assertThat(r.getEmpjS()).isEqualByComparingTo("0");
            assertThat(r.getEmpj()).isEqualByComparingTo("0");
            assertThat(r.getEr()).isEqualByComparingTo("0.410");
        }

        @Test
        @DisplayName("A-4: battery が負の値の場合、EMPJ,S,battery は 0 として扱われる")
        void batteryValueNegative_treatedAsZero() {
            // Arrange
            givenActivityStart(START_F0);
            givenNoHistory(EQUIPMENT_ID);

            // Act
            List<EnR002ReductionResult> results =
                    calculate(List.of(record(EQUIPMENT_ID, "1000", "200", "-50", "0")));

            // Assert
            assertThat(results.get(0).getEmpjSBattery()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("A-5: 蓄電池なし設備（battery 未設定）でも control のみで正しく算定される")
        void equipmentWithoutBattery_calculatesControlOnly() {
            // Data mentah dari dummy site 3 (changedBattery=false, tanpa ELPJ_battery_value),
            // 5 record dijumlahkan: ELpv=4829.2, ELpvr=1192.6, control=96.6
            givenActivityStart(START_F0);
            givenNoHistory(EQUIPMENT_ID);

            List<EnR002MonitoringRecord> records = List.of(
                    record(EQUIPMENT_ID, "960.4", "240.0", null, "19.2"),
                    record(EQUIPMENT_ID, "944.0", "224.0", null, "18.9"),
                    record(EQUIPMENT_ID, "1000.2", "248.4", null, "20.0"),
                    record(EQUIPMENT_ID, "956.6", "236.2", null, "19.1"),
                    record(EQUIPMENT_ID, "968.0", "244.0", null, "19.4"));

            // Act
            List<EnR002ReductionResult> results = calculate(records);

            // Assert
            EnR002ReductionResult r = results.get(0);
            // ELPJ = 4829.2 - 1192.6 = 3636.6
            assertThat(r.getElpj()).isEqualByComparingTo("3636.6");
            // control = 96.6 × 0.000512 = 0.0494592 → 0.049
            assertThat(r.getEmpjSControl()).isEqualByComparingTo("0.049");
            assertThat(r.getEmpjSBattery()).isEqualByComparingTo("0");
            // EMBL = 3636.6 × 0.000512 = 1.8619392 → 1.862
            assertThat(r.getEmbl()).isEqualByComparingTo("1.862");
            // ER = 1.862 - 0.049 = 1.813
            assertThat(r.getEr()).isEqualByComparingTo("1.813");
        }
    }

    // =========================================================================
    // B. 集約（グルーピング・合算)
    // =========================================================================
    @Nested
    @DisplayName("【B】集約")
    class Aggregation {

        @Test
        @DisplayName("B-1: 同一設備の複数レコードは生値が合算されてから算定される")
        void multipleRecordsSameEquipment_sumsRawValuesBeforeCalculating() {
            // Data mentah dari dummy site 1 (5 hari):
            // ΣELpv=6036.5, ΣELpvr=1490.75, Σcontrol=120.7, Σbattery=77.2
            givenActivityStart(START_F0);
            givenNoHistory(EQUIPMENT_ID);

            List<EnR002MonitoringRecord> records = List.of(
                    record(EQUIPMENT_ID, "1200.5", "300.0", "15.5", "24.0"),
                    record(EQUIPMENT_ID, "1180.0", "280.0", "14.8", "23.6"),
                    record(EQUIPMENT_ID, "1250.25", "310.5", "16.2", "25.0"),
                    record(EQUIPMENT_ID, "1195.75", "295.25", "15.0", "23.9"),
                    record(EQUIPMENT_ID, "1210.0", "305.0", "15.7", "24.2"));

            // Act
            List<EnR002ReductionResult> results = calculate(records);

            // Assert — satu baris hasil untuk satu equipment, bukan 5 baris
            assertThat(results).hasSize(1);
            EnR002ReductionResult r = results.get(0);
            // ELPJ = 6036.5 - 1490.75 = 4545.75
            assertThat(r.getElpj()).isEqualByComparingTo("4545.75");
            // control = 120.7 × 0.000512 = 0.0617984 → 0.062
            assertThat(r.getEmpjSControl()).isEqualByComparingTo("0.062");
            // battery = 77.2 × 0.000512 = 0.0395264 → 0.040
            assertThat(r.getEmpjSBattery()).isEqualByComparingTo("0.040");
            assertThat(r.getEmpjS()).isEqualByComparingTo("0.102");
            // EMBL = 4545.75 × 0.000512 = 2.327424 → 2.327
            assertThat(r.getEmbl()).isEqualByComparingTo("2.327");
            // ER = 2.327 - 0.102 = 2.225
            assertThat(r.getEr()).isEqualByComparingTo("2.225");
        }

        @Test
        @DisplayName("B-2: 複数設備がある場合、設備ごとに独立した結果行が返される")
        void multipleEquipments_returnsOneResultPerEquipment() {
            // Arrange
            givenActivityStart(START_F0);
            givenNoHistory(1);
            givenNoHistory(2);

            List<EnR002MonitoringRecord> records = List.of(
                    record(1, "1000", "200", "20", "50"),
                    record(2, "2000", "400", "40", "100"));

            // Act
            List<EnR002ReductionResult> results = calculate(records);

            // Assert — cari berdasarkan equipmentId, JANGAN pakai index:
            // urutan hasil mengikuti HashMap sehingga tidak dijamin.
            assertThat(results).hasSize(2);

            EnR002ReductionResult eq1 = findByEquipment(results, 1);
            assertThat(eq1.getElpj()).isEqualByComparingTo("800");
            assertThat(eq1.getEr()).isEqualByComparingTo("0.374");

            EnR002ReductionResult eq2 = findByEquipment(results, 2);
            // ELPJ = 1600; EMBL = 1600 × 0.000512 = 0.8192 → 0.819
            // control = 100 × 0.000512 = 0.0512 → 0.051
            // battery = 40 × 0.000512 = 0.02048 → 0.020
            // EMPJ = 0.071 → ER = 0.819 - 0.071 = 0.748
            assertThat(eq2.getElpj()).isEqualByComparingTo("1600");
            assertThat(eq2.getEmpjSControl()).isEqualByComparingTo("0.051");
            assertThat(eq2.getEmpjSBattery()).isEqualByComparingTo("0.020");
            assertThat(eq2.getEr()).isEqualByComparingTo("0.748");
        }

        @Test
        @DisplayName("B-3: equipmentID が null のレコードは黙って除外される")
        void recordWithNullEquipmentId_isSilentlyDropped() {
            // Perilaku saat ini: data hilang TANPA log/peringatan.
            // Test ini mengunci perilaku tersebut agar perubahan di masa depan terdeteksi.
            givenActivityStart(START_F0);
            givenNoHistory(EQUIPMENT_ID);

            List<EnR002MonitoringRecord> records = List.of(
                    record(EQUIPMENT_ID, "1000", "200", "20", "50"),
                    record(null, "9999", "0", "9999", "9999")); // ← dibuang

            // Act
            List<EnR002ReductionResult> results = calculate(records);

            // Assert — record kedua tidak mempengaruhi hasil sama sekali
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getElpj()).isEqualByComparingTo("800");
            assertThat(results.get(0).getEr()).isEqualByComparingTo("0.374");
        }

        @Test
        @DisplayName("B-4: 全レコードの equipmentID が null の場合、空リストが返される")
        void allRecordsWithNullEquipmentId_returnsEmptyList() {
            // Skenario ini persis menggambarkan dummy DynamoDB saat ini
            // (monitoring record tidak memiliki atribut equipmentID sama sekali).
            List<EnR002ReductionResult> results = calculate(List.of(
                    record(null, "1000", "200", "20", "50"),
                    record(null, "2000", "400", "40", "100")));

            assertThat(results).isEmpty();
            verifyNoInteractions(siteProjectRelationRepo, reductionResultRepo);
        }

        @Test
        @DisplayName("B-5: monitoringData が空の場合、空リストが返される（例外なし）")
        void emptyMonitoringData_returnsEmptyList() {
            List<EnR002ReductionResult> results = calculate(List.of());

            assertThat(results).isEmpty();
            verifyNoInteractions(siteProjectRelationRepo, reductionResultRepo);
        }
    }

    // =========================================================================
    // C. 履歴累積
    // =========================================================================
    @Nested
    @DisplayName("【C】履歴累積")
    class HistoryAccumulation {

        @Test
        @DisplayName("C-1: 履歴がない場合、今回バッチの値のみが結果になる")
        void noHistory_resultEqualsCurrentBatchOnly() {
            givenActivityStart(START_F0);
            givenNoHistory(EQUIPMENT_ID);

            List<EnR002ReductionResult> results =
                    calculate(List.of(record(EQUIPMENT_ID, "1000", "200", "20", "50")));

            assertThat(results.get(0).getEr()).isEqualByComparingTo("0.374");
            assertThat(results.get(0).getElpj()).isEqualByComparingTo("800");
        }

        @Test
        @DisplayName("C-2: 履歴がある場合、今回バッチの値と履歴が全フィールドで加算される")
        void withHistory_accumulatesAllFields() {
            // Arrange
            givenActivityStart(START_F0);
            when(reductionResultRepo.findTopByProjectIdAndSiteIdAndEquipmentIdOrderByBatchIdDesc(
                    PROJECT_ID, SITE_ID, EQUIPMENT_ID))
                    .thenReturn(Optional.of(history(
                            "1000",   // elpj
                            "1000",   // elbl
                            "0.500",  // embl
                            "0.050",  // empj
                            "0",      // empjM
                            "0.050",  // empjS
                            "0.020",  // empjSBattery
                            "0.030",  // empjSControl
                            "0.450"   // er
                    )));

            // Act — batch saat ini sama dengan A-1
            List<EnR002ReductionResult> results =
                    calculate(List.of(record(EQUIPMENT_ID, "1000", "200", "20", "50")));

            // Assert — setiap field = batch + histori
            EnR002ReductionResult r = results.get(0);
            assertThat(r.getElpj()).isEqualByComparingTo("1800");          // 800 + 1000
            assertThat(r.getElbl()).isEqualByComparingTo("1800");          // 800 + 1000
            assertThat(r.getEmbl()).isEqualByComparingTo("0.910");         // 0.410 + 0.500
            assertThat(r.getEmpj()).isEqualByComparingTo("0.086");         // 0.036 + 0.050
            assertThat(r.getEmpjM()).isEqualByComparingTo("0");            // 0 + 0
            assertThat(r.getEmpjS()).isEqualByComparingTo("0.086");        // 0.036 + 0.050
            assertThat(r.getEmpjSBattery()).isEqualByComparingTo("0.030"); // 0.010 + 0.020
            assertThat(r.getEmpjSControl()).isEqualByComparingTo("0.056"); // 0.026 + 0.030
            assertThat(r.getEr()).isEqualByComparingTo("0.824");           // 0.374 + 0.450
        }

        @Test
        @DisplayName("C-3: 履歴フィールドが null の場合、nullToZero により 0 として扱われる")
        void historyWithNullFields_treatedAsZero() {
            givenActivityStart(START_F0);
            when(reductionResultRepo.findTopByProjectIdAndSiteIdAndEquipmentIdOrderByBatchIdDesc(
                    PROJECT_ID, SITE_ID, EQUIPMENT_ID))
                    .thenReturn(Optional.of(history(
                            "1000", null, null, null, null, null, null, null, "0.450")));

            List<EnR002ReductionResult> results =
                    calculate(List.of(record(EQUIPMENT_ID, "1000", "200", "20", "50")));

            EnR002ReductionResult r = results.get(0);
            assertThat(r.getElpj()).isEqualByComparingTo("1800");   // 800 + 1000
            assertThat(r.getElbl()).isEqualByComparingTo("800");    // 800 + 0 (null)
            assertThat(r.getEmbl()).isEqualByComparingTo("0.410");  // 0.410 + 0 (null)
            assertThat(r.getEr()).isEqualByComparingTo("0.824");    // 0.374 + 0.450
        }

        @Test
        @DisplayName("C-4: 今回バッチが空で履歴がある場合、履歴の値がそのまま繰り越される")
        void emptyBatchWithHistory_carriesForwardHistoryValues() {
            // Arrange — batch semua nol; histori ada
            when(reductionResultRepo.findTopByProjectIdAndSiteIdAndEquipmentIdOrderByBatchIdDesc(
                    PROJECT_ID, SITE_ID, EQUIPMENT_ID))
                    .thenReturn(Optional.of(history(
                            "1000", "1000", "0.500", "0.050", "0",
                            "0.050", "0.020", "0.030", "0.450")));

            // Act
            List<EnR002ReductionResult> results =
                    calculate(List.of(record(EQUIPMENT_ID, "0", "0", "0", "0")));

            // Assert — nilai identik dengan histori
            EnR002ReductionResult r = results.get(0);
            assertThat(r.getElpj()).isEqualByComparingTo("1000");
            assertThat(r.getElbl()).isEqualByComparingTo("1000");
            assertThat(r.getEmbl()).isEqualByComparingTo("0.500");
            assertThat(r.getEmpj()).isEqualByComparingTo("0.050");
            assertThat(r.getEmpjS()).isEqualByComparingTo("0.050");
            assertThat(r.getEmpjSBattery()).isEqualByComparingTo("0.020");
            assertThat(r.getEmpjSControl()).isEqualByComparingTo("0.030");
            assertThat(r.getEr()).isEqualByComparingTo("0.450");
            assertThat(r.getIsActive()).isTrue();

            // CEF tidak perlu dihitung pada jalur ini → repo relasi tidak disentuh
            verifyNoInteractions(siteProjectRelationRepo);
        }

        /*
         * =====================================================================
         * C-5 — TEST PENGEKSPOS DEFECT (pola sama seperti UT-AS-010 sebelumnya)
         * =====================================================================
         * Method zeroResult() di dalam kode TIDAK mengisi dua hal yang diisi oleh
         * dua jalur return lainnya:
         *   1. .empjSControl(...)     → field ini akan tersimpan sebagai NULL
         *   2. result.setIsActive(true) → flag aktif tidak diset
         *
         * Test ini menuliskan ekspektasi yang SEHARUSNYA (konsisten dengan dua
         * jalur lain), bukan perilaku saat ini. Jadi test ini KEMUNGKINAN BESAR
         * GAGAL — dan kegagalannya adalah BUKTI inkonsistensi, bukan kesalahan
         * penulisan test.
         */
        @Test
        @DisplayName("C-5: バッチも履歴も空の場合、全フィールドが 0 で isActive=true であるべき（欠陥検出）")
        void emptyBatchAndEmptyHistory_shouldReturnFullyZeroedActiveResult() {
            givenNoHistory(EQUIPMENT_ID);

            List<EnR002ReductionResult> results =
                    calculate(List.of(record(EQUIPMENT_ID, "0", "0", "0", "0")));

            assertThat(results).hasSize(1);
            EnR002ReductionResult r = results.get(0);

            assertThat(r.getElpj()).isEqualByComparingTo("0");
            assertThat(r.getElbl()).isEqualByComparingTo("0");
            assertThat(r.getEmbl()).isEqualByComparingTo("0");
            assertThat(r.getEmpj()).isEqualByComparingTo("0");
            assertThat(r.getEmpjM()).isEqualByComparingTo("0");
            assertThat(r.getEmpjS()).isEqualByComparingTo("0");
            assertThat(r.getEmpjSBattery()).isEqualByComparingTo("0");
            // ↓ dua assertion inilah yang akan gagal pada implementasi saat ini
            assertThat(r.getEmpjSControl()).isEqualByComparingTo("0");
            assertThat(r.getIsActive()).isTrue();
        }

        /*
         * =====================================================================
         * C-6 — TEST PENGEKSPOS DEFECT
         * =====================================================================
         * currentBatchIsEmpty hanya memeriksa elpv, elpvr, dan elpjBattery —
         * TIDAK memeriksa elpjControl. Akibatnya, batch yang hanya memiliki
         * konsumsi listrik perangkat kontrol (PCS menyala tanpa pembangkitan
         * tercatat) dianggap "kosong", sehingga emisi kontrolnya HILANG.
         *
         * Ekspektasi yang benar: 50 kWh konsumsi kontrol harus tetap
         * menghasilkan EMPJ,S,control = 50 × 0.000512 = 0.026, dan karena
         * EMBL = 0, hasil ER menjadi -0.026.
         */
        @Test
        @DisplayName("C-6: control のみ値がある場合でも制御機器の排出量が算定されるべき（欠陥検出）")
        void onlyControlValuePresent_shouldStillCalculateControlEmissions() {
            givenActivityStart(START_F0);
            givenNoHistory(EQUIPMENT_ID);

            // ELpv=0, ELpvr=0, battery=0, control=50
            List<EnR002ReductionResult> results =
                    calculate(List.of(record(EQUIPMENT_ID, "0", "0", "0", "50")));

            EnR002ReductionResult r = results.get(0);
            // ↓ implementasi saat ini mengembalikan 0 karena dianggap batch kosong
            assertThat(r.getEmpjSControl()).isEqualByComparingTo("0.026");
            assertThat(r.getEmpj()).isEqualByComparingTo("0.026");
            assertThat(r.getEr()).isEqualByComparingTo("-0.026");
        }
    }

    // =========================================================================
    // D. CEF 境界値（移行関数 f(t))
    // =========================================================================
    @Nested
    @DisplayName("【D】CEF 境界値")
    class CefBoundary {

        /** Input tetap: ELpv=1000, ELpvr=200, battery=20, control=50 → ELPJ=800 */
        private List<EnR002ReductionResult> calcWithStart(LocalDate startDate, LocalDate processingDate) {
            SiteProjectRelation relation = mock(SiteProjectRelation.class);
            when(relation.getActivityStartDate()).thenReturn(startDate);
            when(siteProjectRelationRepo.findByProjectIdAndSiteId(PROJECT_ID, SITE_ID))
                    .thenReturn(relation);
            givenNoHistory(EQUIPMENT_ID);

            return calculator.calculateEmissions(PROJECT_ID, SITE_ID, BATCH_ID,
                    List.of(record(EQUIPMENT_ID, "1000", "200", "20", "50")), processingDate);
        }

        @Test
        @DisplayName("D-1: t < 1年 の場合、f=0 → CEF = CMO = 0.000512")
        void lessThanOneYear_transitionFactorZero() {
            // 2026-01-01 → 2026-07-05 = 185日 → t = 0.5065
            List<EnR002ReductionResult> results = calcWithStart(START_F0, PROCESSING_DATE);

            EnR002ReductionResult r = results.get(0);
            assertThat(r.getEmbl()).isEqualByComparingTo("0.410");         // 800 × 0.000512
            assertThat(r.getEmpjSControl()).isEqualByComparingTo("0.026"); // 50 × 0.000512
            assertThat(r.getEr()).isEqualByComparingTo("0.374");
        }

        /*
         * D-2: PERHATIAN — temuan boundary.
         * Metodologi menyatakan f(t)=0.5 untuk 1年≦t. Tapi kode menghitung
         * t = daysBetween / 365.25. Tepat 1 tahun kalender (365 hari) memberi
         * t = 365/365.25 = 0.99932 < 1.0 → f masih 0, BUKAN 0.5.
         * Jadi ulang tahun pertama proyek belum memicu transisi.
         */
        @Test
        @DisplayName("D-2: ちょうど1暦年（365日）の場合、365/365.25 < 1 のため f=0 のまま")
        void exactlyOneCalendarYear_stillTransitionFactorZero() {
            // 2025-07-05 → 2026-07-05 = 365日
            List<EnR002ReductionResult> results =
                    calcWithStart(LocalDate.of(2025, 7, 5), PROCESSING_DATE);

            // CEF masih 0.000512 (f=0), bukan 0.000477 (f=0.5)
            assertThat(results.get(0).getEmbl()).isEqualByComparingTo("0.410");
        }

        @Test
        @DisplayName("D-3: 366日経過の場合、f=0.5 → CEF = 0.000477")
        void threeHundredSixtySixDays_transitionFactorHalf() {
            // 2025-07-04 → 2026-07-05 = 366日 → t = 1.00205
            List<EnR002ReductionResult> results = calcWithStart(START_F05, PROCESSING_DATE);

            EnR002ReductionResult r = results.get(0);
            // EMBL = 800 × 0.000477 = 0.3816 → 0.382
            assertThat(r.getEmbl()).isEqualByComparingTo("0.382");
            // control = 50 × 0.000477 = 0.02385 → 0.024
            assertThat(r.getEmpjSControl()).isEqualByComparingTo("0.024");
            // battery = 20 × 0.000477 = 0.00954 → 0.010
            assertThat(r.getEmpjSBattery()).isEqualByComparingTo("0.010");
            assertThat(r.getEr()).isEqualByComparingTo("0.348");
        }

        @Test
        @DisplayName("D-4: 913日経過（t=2.4997）の場合、まだ f=0.5")
        void justUnderTwoPointFiveYears_stillTransitionFactorHalf() {
            // 2024-01-04 → 2026-07-05 = 913日 → t = 2.49966 < 2.5
            List<EnR002ReductionResult> results =
                    calcWithStart(LocalDate.of(2024, 1, 4), PROCESSING_DATE);

            assertThat(results.get(0).getEmbl()).isEqualByComparingTo("0.382");
        }

        @Test
        @DisplayName("D-5: 914日経過（t=2.5024）の場合、f=1 → CEF = Ca = 0.000441")
        void justOverTwoPointFiveYears_transitionFactorOne() {
            // 2024-01-03 → 2026-07-05 = 914日 → t = 2.50239 ≧ 2.5
            List<EnR002ReductionResult> results = calcWithStart(START_F1, PROCESSING_DATE);

            EnR002ReductionResult r = results.get(0);
            // EMBL = 800 × 0.000441 = 0.3528 → 0.353
            assertThat(r.getEmbl()).isEqualByComparingTo("0.353");
            // control = 50 × 0.000441 = 0.02205 → 0.022
            assertThat(r.getEmpjSControl()).isEqualByComparingTo("0.022");
            // battery = 20 × 0.000441 = 0.00882 → 0.009
            assertThat(r.getEmpjSBattery()).isEqualByComparingTo("0.009");
            assertThat(r.getEr()).isEqualByComparingTo("0.322");
        }

        @Test
        @DisplayName("D-6: CA_MAP に存在する年（2024）の場合、その年の Ca が使われる")
        void yearPresentInCaMap_usesMappedValue() {
            // processingDate 2024-07-05, activityStart 2020-01-01 → f=1 → CEF = Ca(2024) = 0.000438
            List<EnR002ReductionResult> results =
                    calcWithStart(LocalDate.of(2020, 1, 1), LocalDate.of(2024, 7, 5));

            EnR002ReductionResult r = results.get(0);
            // EMBL = 800 × 0.000438 = 0.3504 → 0.350（f=1 の 2026年 0.353 とは異なる）
            assertThat(r.getEmbl()).isEqualByComparingTo("0.350");
            assertThat(r.getEr()).isEqualByComparingTo("0.319");
        }

        @Test
        @DisplayName("D-7: CA_MAP にない年（2026）の場合、DEFAULT_CA = 0.000441 が使われる")
        void yearMissingFromCaMap_usesDefaultCa() {
            // f=1 かつ 2026年 → DEFAULT_CA 0.000441
            List<EnR002ReductionResult> results = calcWithStart(START_F1, PROCESSING_DATE);

            assertThat(results.get(0).getEmbl()).isEqualByComparingTo("0.353");
        }

        @Test
        @DisplayName("D-8: activityStartDate が processingDate より後の場合、IllegalStateException にラップされる")
        void activityStartAfterProcessingDate_throwsWrappedException() {
            SiteProjectRelation relation = mock(SiteProjectRelation.class);
            when(relation.getActivityStartDate()).thenReturn(LocalDate.of(2027, 1, 1));
            when(siteProjectRelationRepo.findByProjectIdAndSiteId(PROJECT_ID, SITE_ID))
                    .thenReturn(relation);
            givenNoHistory(EQUIPMENT_ID);

            // IllegalArgumentException dilempar di dalam, lalu ditangkap catch(Exception)
            // dan dibungkus ulang menjadi IllegalStateException.
            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> calculate(List.of(record(EQUIPMENT_ID, "1000", "200", "20", "50"))));

            assertThat(thrown.getCause()).isInstanceOf(IllegalArgumentException.class);
            assertThat(thrown.getMessage()).contains("Activity start date cannot be after processing date");
        }
    }

    // =========================================================================
    // E. 異常系・境界値
    // =========================================================================
    @Nested
    @DisplayName("【E】異常系・境界値")
    class EdgeAndErrorCases {

        @Test
        @DisplayName("E-1: ELpvr > ELpv の場合、ELPJ と ER が負の値になる（現行仕様の記録）")
        void elpvrGreaterThanElpv_producesNegativeValues() {
            // Secara fisika tidak masuk akal (listrik yang diberikan ke pihak lain
            // melebihi total pembangkitan), tetapi kode tidak memiliki penjagaan.
            // Test ini MENGUNCI perilaku saat ini agar perubahan terdeteksi.
            givenActivityStart(START_F0);
            givenNoHistory(EQUIPMENT_ID);

            List<EnR002ReductionResult> results =
                    calculate(List.of(record(EQUIPMENT_ID, "100", "300", "0", "0")));

            EnR002ReductionResult r = results.get(0);
            // ELPJ = 100 - 300 = -200
            assertThat(r.getElpj()).isEqualByComparingTo("-200");
            // EMBL = -200 × 0.000512 = -0.1024 → -0.102
            assertThat(r.getEmbl()).isEqualByComparingTo("-0.102");
            assertThat(r.getEr()).isEqualByComparingTo("-0.102");
            assertThat(r.getEr()).isNegative();
        }

        @Test
        @DisplayName("E-2: 極小値の場合、setScale(3) により 0.000 に丸められて消失する")
        void verySmallValues_roundedAwayToZero() {
            // CEF ≈ 0.0005 のため、setScale(3, HALF_UP) では約 1 kWh 未満の
            // 発電量が排出量 0.000 tCO2 に丸められる。
            // 3桁丸めが業務要件として妥当かはチームで確認が必要。
            givenActivityStart(START_F0);
            givenNoHistory(EQUIPMENT_ID);

            List<EnR002ReductionResult> results =
                    calculate(List.of(record(EQUIPMENT_ID, "0.5", "0", "0", "0")));

            EnR002ReductionResult r = results.get(0);
            assertThat(r.getElpj()).isEqualByComparingTo("0.5"); // energi tetap tercatat
            // 0.5 × 0.000512 = 0.000256 → 0.000 （排出量が完全に消える）
            assertThat(r.getEmbl()).isEqualByComparingTo("0");
            assertThat(r.getEr()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("E-3: 履歴取得リポジトリが例外をスローする場合、IllegalStateException にラップされる")
        void historyRepositoryThrows_wrappedInIllegalStateException() {
            when(reductionResultRepo.findTopByProjectIdAndSiteIdAndEquipmentIdOrderByBatchIdDesc(
                    PROJECT_ID, SITE_ID, EQUIPMENT_ID))
                    .thenThrow(new RuntimeException("DB connection lost"));

            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> calculate(List.of(record(EQUIPMENT_ID, "1000", "200", "20", "50"))));

            assertThat(thrown.getMessage()).isEqualTo("DB connection lost");
            assertThat(thrown.getCause()).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("E-4: SiteProjectRelation が null の場合、NPE が IllegalStateException にラップされる")
        void siteProjectRelationNull_wrappedInIllegalStateException() {
            // findByProjectIdAndSiteId は Optional ではなく entity を直接返すため、
            // 関連が存在しない場合 null → getActivityStartDate() で NPE。
            when(siteProjectRelationRepo.findByProjectIdAndSiteId(PROJECT_ID, SITE_ID))
                    .thenReturn(null);
            givenNoHistory(EQUIPMENT_ID);

            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> calculate(List.of(record(EQUIPMENT_ID, "1000", "200", "20", "50"))));

            assertThat(thrown.getCause()).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("E-5: 1台の設備で失敗した場合、サイト全体の算定が中断され部分結果は返らない")
        void oneEquipmentFails_entireSiteCalculationAborts() {
            // Loop di calculateEmissions tidak melindungi kegagalan per-equipment,
            // sehingga satu equipment bermasalah menggagalkan seluruh site.
            // Perlu dikonfirmasi ke tim: apakah ini memang perilaku yang diinginkan,
            // atau equipment bermasalah seharusnya di-skip saja.
            lenient().when(siteProjectRelationRepo.findByProjectIdAndSiteId(PROJECT_ID, SITE_ID))
                    .thenReturn(null); // memicu NPE untuk equipment manapun

            List<EnR002MonitoringRecord> records = List.of(
                    record(1, "1000", "200", "20", "50"),
                    record(2, "2000", "400", "40", "100"));

            lenient().when(reductionResultRepo
                            .findTopByProjectIdAndSiteIdAndEquipmentIdOrderByBatchIdDesc(any(), any(), any()))
                    .thenReturn(Optional.empty());

            // Act & Assert — tidak ada hasil parsial yang dikembalikan
            assertThrows(IllegalStateException.class, () -> calculate(records));
        }
    }
}