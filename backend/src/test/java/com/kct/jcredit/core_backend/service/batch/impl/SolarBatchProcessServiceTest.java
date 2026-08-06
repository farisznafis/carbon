package com.kct.jcredit.core_backend.service.batch.impl;

import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.service.batch.BaseBatchProcessService;
import com.kct.jcredit.core_backend.service.batch.BatchHistoryService;
import com.kct.jcredit.core_backend.service.batch.CsvLogService;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.batch.calculator.EmissionsCalculator;
import com.kct.jcredit.core_backend.service.batch.calculator.SolarEmissionsCalculator;
import com.kct.jcredit.core_backend.service.batch.repository.GenericMonitoringRepository;
import com.kct.jcredit.core_backend.service.batch.repository.SolarMonitoringRepositoryAdapter;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultTotalRepository;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResult;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResultTotal;

// TODO: sesuaikan package & import dengan struktur project asli

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // ← WAJIB. Tanpa ini semua @Mock bernilai null → NPE.
@DisplayName("SolarBatchProcessService のテスト")
class SolarBatchProcessServiceTest {

    @Mock
    private SolarMonitoringRepositoryAdapter solarRepository;
    @Mock
    private SolarEmissionsCalculator solarCalculator;
    @Mock
    private EnR002ReductionResultRepository resultRepository;
    @Mock
    private EnR002ReductionResultTotalRepository resultTotalRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private SiteProjectRelationRepository siteProjectRelationRepo;
    @Mock
    private BatchHistoryServiceImpl batchHistoryService;
    @Mock
    private CsvLogServiceImpl csvLogService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SolarBatchProcessService service;

    private static final LocalDate PROCESSING_DATE = LocalDate.of(2026, 7, 10);

    // ---------- helper ----------

    private static EnR002ReductionResult resultOf(int siteId, int equipmentId,
                                                  String empj, String control, String battery) {
        return EnR002ReductionResult.builder()
                .projectId(1)
                .siteId(siteId)
                .equipmentId(equipmentId)
                .batchId(1)
                .processingDate(PROCESSING_DATE)
                .empj(new BigDecimal(empj))
                .empjM(BigDecimal.ZERO)
                .empjS(new BigDecimal(empj))
                .empjSControl(new BigDecimal(control))
                .empjSBattery(new BigDecimal(battery))
                .build();
    }

    /** isActive ada di BaseEntity → harus di-set eksplisit, jangan dibiarkan null. */
    private static Project projectWith(int id, Boolean isActive) {
        Project project = Project.builder().build();
        project.setId(id);
        project.setIsActive(isActive);
        return project;
    }

    private static SiteProjectRelation relationFor(int siteId) {
        Site site = Site.builder().build();
        site.setId(siteId);
        return SiteProjectRelation.builder().site(site).build();
    }

    // =========================================================================
    @Nested
    @DisplayName("saveSiteResults のテスト")
    class SaveSiteResultsTest {

        @Test
        @DisplayName("UT-SB-401: 設備ごとの結果を1件ずつ保存し、全件に isActive=true を設定する")
        void persistsEachEquipmentResultAndSetsActive() {
            EnR002ReductionResult first = resultOf(2, 1, "120.50", "80.25", "40.25");
            EnR002ReductionResult second = resultOf(2, 2, "79.50", "19.75", "9.75");

            when(resultRepository.save(any(EnR002ReductionResult.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Object saved = service.saveSiteResults(1, 2, 3, PROCESSING_DATE, List.of(first, second));

            ArgumentCaptor<EnR002ReductionResult> captor =
                    ArgumentCaptor.forClass(EnR002ReductionResult.class);
            verify(resultRepository, times(2)).save(captor.capture());

            List<EnR002ReductionResult> persisted = captor.getAllValues();
            assertThat(persisted).hasSize(2);
            assertThat(persisted).extracting(EnR002ReductionResult::getEquipmentId)
                    .containsExactly(1, 2); // satu baris per equipment
            assertThat(persisted).allSatisfy(r -> assertThat(r.getIsActive()).isTrue());

            assertThat(saved).isInstanceOf(List.class);
            assertThat((List<?>) saved).hasSize(2);
        }

        @Test
        @DisplayName("UT-SB-402: results が空リストの場合、save を呼ばず空リストを返す")
        void emptyList_returnsEmptyAndSkipsSave() {
            Object saved = service.saveSiteResults(1, 3, 1, PROCESSING_DATE, Collections.emptyList());

            assertThat((List<?>) saved).isEmpty();
            verify(resultRepository, never()).save(any());
        }

        @Test
        @DisplayName("UT-SB-403: results が null の場合でも NPE にならず空リストを返す")
        void nullList_returnsEmptyAndSkipsSave() {
            Object saved = service.saveSiteResults(1, 3, 1, PROCESSING_DATE, null);

            assertThat((List<?>) saved).isEmpty();
            verify(resultRepository, never()).save(any());
        }

        @Test
        @DisplayName("UT-SB-404: 保存中にリポジトリが例外をスローした場合、そのまま伝播される")
        void repositoryThrows_propagates() {
            when(resultRepository.save(any(EnR002ReductionResult.class)))
                    .thenThrow(new DataAccessResourceFailureException("DB down"));

            List<EnR002ReductionResult> results = List.of(resultOf(2, 1, "1.0", "0.5", "0.5"));

            assertThrows(DataAccessResourceFailureException.class,
                    () -> service.saveSiteResults(1, 2, 3, PROCESSING_DATE, results));

            verify(resultRepository, times(1)).save(any());
        }

        /*
         * UT-SB-405 — GAP: baris hasil lama tidak pernah dinonaktifkan.
         *
         * Calculator menyimpan nilai KUMULATIF, tetapi setiap batch menambah baris baru
         * dengan isActive=true tanpa menyentuh baris sebelumnya. Akibatnya
         * getSiteReductionResult() — yang memfilter IsActiveTrue — mengembalikan seluruh
         * baris historis yang masing-masing sudah kumulatif.
         */
        @Test
        @DisplayName("UT-SB-405: 過去の結果行が非活性化されない（欠陥候補）")
        void doesNotDeactivatePreviousRows() {
            when(resultRepository.save(any(EnR002ReductionResult.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.saveSiteResults(1, 2, 3, PROCESSING_DATE,
                    List.of(resultOf(2, 1, "1.0", "0.5", "0.5")));

            verify(resultRepository, times(1)).save(any(EnR002ReductionResult.class));
            // Tidak ada pencarian/pembaruan baris lama sama sekali:
            verifyNoMoreInteractions(resultRepository);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("aggregateEmissions のテスト")
    class AggregateEmissionsTest {

        @Test
        @DisplayName("UT-SB-406: 4拠点分（ダミー day1）の empj / control / battery が合算される")
        void sumsAllFourDummySites() {
            List<EnR002ReductionResult> dayOne = List.of(
                    resultOf(1, 1, "0.018", "0.011", "0.007"),
                    resultOf(2, 2, "0.025", "0.016", "0.009"),
                    resultOf(3, 3, "0.009", "0.009", "0.000"),
                    resultOf(4, 4, "0.029", "0.022", "0.007"));

            EnR002ReductionResultTotal total =
                    (EnR002ReductionResultTotal) service.aggregateEmissions(dayOne);

            assertThat(total.getEmpj()).isEqualByComparingTo("0.081");
            assertThat(total.getEmpjSControl()).isEqualByComparingTo("0.058");
            assertThat(total.getEmpjSBattery()).isEqualByComparingTo("0.023");
            assertThat(total.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("UT-SB-407: null フィールドは 0 として扱われ、例外にならない")
        void nullFieldsTreatedAsZero() {
            // Site tanpa baterai bisa menghasilkan field null
            EnR002ReductionResult noBattery = EnR002ReductionResult.builder()
                    .projectId(1).siteId(3).equipmentId(3)
                    .empj(new BigDecimal("0.009"))
                    .empjSControl(new BigDecimal("0.009"))
                    .empjSBattery(null)
                    .empjM(null)
                    .empjS(null)
                    .build();

            EnR002ReductionResultTotal total =
                    (EnR002ReductionResultTotal) service.aggregateEmissions(List.of(noBattery));

            assertThat(total.getEmpjSBattery()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(total.getEmpjM()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(total.getEmpjS()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(total.getEmpj()).isEqualByComparingTo("0.009");
        }

        @Test
        @DisplayName("UT-SB-408: 空リストの場合、全項目 0 かつ projectId が null になる")
        void emptyList_returnsZeroTotalsWithNullProjectId() {
            EnR002ReductionResultTotal total =
                    (EnR002ReductionResultTotal) service.aggregateEmissions(Collections.emptyList());

            assertThat(total.getEmpj()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(total.getEmpjM()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(total.getEmpjS()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(total.getEmpjSControl()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(total.getEmpjSBattery()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(total.getProjectId()).isNull();
        }

        @Test
        @DisplayName("UT-SB-409: projectId が先頭要素から取得される")
        void projectIdTakenFromFirstElement() {
            List<EnR002ReductionResult> results = List.of(
                    resultOf(1, 1, "0.018", "0.011", "0.007"),
                    resultOf(2, 2, "0.025", "0.016", "0.009"));

            EnR002ReductionResultTotal total =
                    (EnR002ReductionResultTotal) service.aggregateEmissions(results);

            assertThat(total.getProjectId()).isEqualTo(1);
            // Asumsi implisit: semua elemen berasal dari project yang sama.
            // Kalau tercampur, hanya yang pertama menang tanpa validasi apa pun.
        }

        /*
         * UT-SB-410 — GAP PALING BERDAMPAK.
         *
         * aggregateEmissions() hanya menjumlahkan empj / empjM / empjS /
         * empjSControl / empjSBattery. Field elpj / embl / elbl / er TIDAK
         * dijumlahkan dan tidak diset di builder → bernilai null.
         *
         * Padahal ER (式1: ER = EMBL - EMPJ) adalah keluaran akhir metodologi
         * EN-R-002. Dummy total memiliki er=10.499 dan elpj=24069.45; dengan kode
         * saat ini keduanya akan tersimpan sebagai null.
         */
        @Test
        @DisplayName("UT-SB-410: elpj / embl / elbl / er も集計されるべき（欠陥候補・重要度高）")
        void shouldAlsoAggregateEnergyAndErFields() {
            EnR002ReductionResult site1 = EnR002ReductionResult.builder()
                    .projectId(1).siteId(1).equipmentId(1)
                    .empj(new BigDecimal("0.018")).empjM(BigDecimal.ZERO)
                    .empjS(new BigDecimal("0.018"))
                    .empjSControl(new BigDecimal("0.011"))
                    .empjSBattery(new BigDecimal("0.007"))
                    .elpj(new BigDecimal("900.5"))
                    .elbl(new BigDecimal("900.5"))
                    .embl(new BigDecimal("0.408"))
                    .er(new BigDecimal("0.390"))
                    .build();
            EnR002ReductionResult site2 = EnR002ReductionResult.builder()
                    .projectId(1).siteId(2).equipmentId(2)
                    .empj(new BigDecimal("0.025")).empjM(BigDecimal.ZERO)
                    .empjS(new BigDecimal("0.025"))
                    .empjSControl(new BigDecimal("0.016"))
                    .empjSBattery(new BigDecimal("0.009"))
                    .elpj(new BigDecimal("1346.2"))
                    .elbl(new BigDecimal("1346.2"))
                    .embl(new BigDecimal("0.610"))
                    .er(new BigDecimal("0.585"))
                    .build();

            EnR002ReductionResultTotal total =
                    (EnR002ReductionResultTotal) service.aggregateEmissions(List.of(site1, site2));

            // Yang sudah benar
            assertThat(total.getEmpj()).isEqualByComparingTo("0.043");

            // ↓↓↓ Blok pengekspos defect — saat ini keempatnya null ↓↓↓
            assertThat(total.getElpj()).isEqualByComparingTo("2246.7");  // 900.5 + 1346.2
            assertThat(total.getElbl()).isEqualByComparingTo("2246.7");
            assertThat(total.getEmbl()).isEqualByComparingTo("1.018");   // 0.408 + 0.610
            assertThat(total.getEr()).isEqualByComparingTo("0.975");     // 0.390 + 0.585
            // ↑↑↑ Blok pengekspos defect ↑↑↑
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("saveProjectTotal のテスト")
    class SaveProjectTotalTest {

        private EnR002ReductionResultTotal aggregatedOf(String empj, String control, String battery) {
            return EnR002ReductionResultTotal.builder()
                    .empj(new BigDecimal(empj))
                    .empjM(BigDecimal.ZERO)
                    .empjS(new BigDecimal(empj))
                    .empjSControl(new BigDecimal(control))
                    .empjSBattery(new BigDecimal(battery))
                    .build();
        }

        @Test
        @DisplayName("UT-SB-411: 既存行がない場合、新規行が作成される")
        void createsNewRowWhenNoneExists() {
            when(resultTotalRepository.findByProjectId(1)).thenReturn(Optional.empty());
            when(resultTotalRepository.save(any(EnR002ReductionResultTotal.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            EnR002ReductionResultTotal aggregated = aggregatedOf("0.081", "0.058", "0.023");

            Object saved = service.saveProjectTotal(1, 1, PROCESSING_DATE, List.of(), aggregated);

            ArgumentCaptor<EnR002ReductionResultTotal> captor =
                    ArgumentCaptor.forClass(EnR002ReductionResultTotal.class);
            verify(resultTotalRepository).save(captor.capture());
            EnR002ReductionResultTotal persisted = captor.getValue();

            assertThat(persisted.getProjectId()).isEqualTo(1);
            assertThat(persisted.getBatchId()).isEqualTo(1);
            assertThat(persisted.getProcessingDate()).isEqualTo(PROCESSING_DATE);
            assertThat(persisted.getIsActive()).isTrue();
            assertThat(persisted.getStartTime()).isNotNull();
            assertThat(persisted.getEndTime()).isNotNull();
            assertThat(persisted.getEmpj()).isEqualByComparingTo("0.081");
            assertThat(saved).isSameAs(persisted);
        }

        @Test
        @DisplayName("UT-SB-412: 既存行がある場合、同じ行が更新され重複挿入されない")
        void updatesExistingRowInPlace() {
            EnR002ReductionResultTotal existing = EnR002ReductionResultTotal.builder()
                    .projectId(1)
                    .batchId(1) // batch lama
                    .empj(new BigDecimal("0.404"))
                    .build();
            existing.setId(7); // id ada di BaseEntity → set setelah build

            when(resultTotalRepository.findByProjectId(1)).thenReturn(Optional.of(existing));
            when(resultTotalRepository.save(any(EnR002ReductionResultTotal.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            EnR002ReductionResultTotal aggregated = EnR002ReductionResultTotal.builder()
                    .empj(new BigDecimal("0.500"))
                    .empjM(BigDecimal.ZERO)
                    .empjS(new BigDecimal("0.500"))
                    .empjSControl(new BigDecimal("0.350"))
                    .empjSBattery(new BigDecimal("0.150"))
                    .elpj(new BigDecimal("25000.00"))
                    .elbl(new BigDecimal("25000.00"))
                    .embl(new BigDecimal("11.500"))
                    .er(new BigDecimal("11.000"))
                    .build();

            service.saveProjectTotal(1, 9, LocalDate.of(2026, 7, 11), List.of(), aggregated);

            ArgumentCaptor<EnR002ReductionResultTotal> captor =
                    ArgumentCaptor.forClass(EnR002ReductionResultTotal.class);
            verify(resultTotalRepository).save(captor.capture());
            EnR002ReductionResultTotal persisted = captor.getValue();

            // Baris yang sama (id=7) di-update, bukan insert baru
            assertThat(persisted.getId()).isEqualTo(7);
            assertThat(persisted.getBatchId()).isEqualTo(9);
            assertThat(persisted.getProcessingDate()).isEqualTo(LocalDate.of(2026, 7, 11));
            assertThat(persisted.getEmpj()).isEqualByComparingTo("0.500");
            assertThat(persisted.getElbl()).isEqualByComparingTo("25000.00");
            assertThat(persisted.getEr()).isEqualByComparingTo("11.000");
            assertThat(persisted.getIsActive()).isTrue();

            verify(resultTotalRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("UT-SB-413: 更新時、既存の startTime は保持される")
        void updatePreservesOriginalStartTime() {
            Instant originalStart = Instant.parse("2026-07-01T00:00:00Z");
            EnR002ReductionResultTotal existing = EnR002ReductionResultTotal.builder()
                    .projectId(1).batchId(1)
                    .startTime(originalStart)
                    .build();
            existing.setId(7);

            when(resultTotalRepository.findByProjectId(1)).thenReturn(Optional.of(existing));
            when(resultTotalRepository.save(any(EnR002ReductionResultTotal.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.saveProjectTotal(1, 9, PROCESSING_DATE, List.of(),
                    aggregatedOf("0.500", "0.350", "0.150"));

            ArgumentCaptor<EnR002ReductionResultTotal> captor =
                    ArgumentCaptor.forClass(EnR002ReductionResultTotal.class);
            verify(resultTotalRepository).save(captor.capture());

            // Jalur update hanya menyetel endTime — startTime batch pertama dipertahankan
            assertThat(captor.getValue().getStartTime()).isEqualTo(originalStart);
            assertThat(captor.getValue().getEndTime()).isNotNull();
        }

        @Test
        @DisplayName("UT-SB-414: aggregated が想定外の型の場合、ClassCastException が発生する")
        void wrongAggregatedType_throwsClassCastException() {
            // Signature memakai Object (base class generik) → tidak ada type safety saat compile
            assertThrows(ClassCastException.class,
                    () -> service.saveProjectTotal(1, 1, PROCESSING_DATE, List.of(), "not a total"));

            verify(resultTotalRepository, never()).save(any());
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getActiveProjects のテスト")
    class GetActiveProjectsTest {

        @Test
        @DisplayName("UT-SB-415: 非活性プロジェクトが除外され、活性プロジェクトの ID のみ返る")
        void filtersOutInactiveProjects() {
            when(projectRepository.findAll()).thenReturn(List.of(
                    projectWith(1, true),
                    projectWith(2, true),
                    projectWith(3, false)));

            List<Integer> result = service.getActiveProjects();

            assertThat(result).containsExactlyInAnyOrder(1, 2);
            assertThat(result).doesNotContain(3);
        }

        @Test
        @DisplayName("UT-SB-416: プロジェクトが1件もない場合、空リストを返す")
        void noProjects_returnsEmptyList() {
            when(projectRepository.findAll()).thenReturn(Collections.emptyList());

            List<Integer> result = service.getActiveProjects();

            assertThat(result).isEmpty();
            verify(projectRepository, times(1)).findAll();
        }

        /*
         * UT-SB-417 — GAP: filter(project -> project.getIsActive()) melakukan
         * unboxing Boolean. Kolom is_active bisa null di DB → seluruh batch gagal
         * sebelum project mana pun sempat diproses.
         * Perbaikan yang disarankan: Boolean.TRUE.equals(project.getIsActive())
         */
        @Test
        @DisplayName("UT-SB-417: isActive が null のプロジェクトがあると NPE が発生する（欠陥候補）")
        void nullIsActive_currentlyThrowsNpe() {
            when(projectRepository.findAll()).thenReturn(List.of(
                    projectWith(1, true),
                    projectWith(2, null))); // is_active NULL di DB

            assertThrows(NullPointerException.class, () -> service.getActiveProjects());

            // Setelah diperbaiki, ganti dengan:
            // assertThat(service.getActiveProjects()).containsExactly(1);
        }

        /*
         * UT-SB-418 — GAP: tidak ada penyaringan metodologi.
         * Log menulis "active solar projects", tetapi kode mengembalikan SEMUA
         * project aktif. Saat metodologi kedua (mis. EnR003 angin) ditambahkan,
         * batch solar akan ikut memproses project angin.
         */
        @Test
        @DisplayName("UT-SB-418: 方法論による絞り込みが行われていない（欠陥候補）")
        void doesNotFilterByMethodology() {
            when(projectRepository.findAll()).thenReturn(List.of(
                    projectWith(1, true),  // solar
                    projectWith(2, true))); // TODO: set metodologi non-solar bila field-nya ada

            List<Integer> result = service.getActiveProjects();

            // Perilaku SAAT INI: semua project aktif dikembalikan tanpa cek metodologi
            assertThat(result).containsExactlyInAnyOrder(1, 2);

            // Setelah filter metodologi ditambahkan, ganti dengan:
            // assertThat(result).containsExactly(1);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getSitesForProject のテスト")
    class GetSitesForProjectTest {

        @Test
        @DisplayName("UT-SB-419: サイト関連から siteId のみが抽出される（ダミーの4拠点）")
        void mapsRelationsToSiteIds() {
            when(siteProjectRelationRepo.findByProjectId(1)).thenReturn(List.of(
                    relationFor(1), relationFor(2), relationFor(3), relationFor(4)));

            List<Integer> result = service.getSitesForProject(1);

            assertThat(result).containsExactly(1, 2, 3, 4); // urutan dipertahankan
        }

        @Test
        @DisplayName("UT-SB-420: 紐づく拠点がない場合、空リストを返す")
        void noRelations_returnsEmptyList() {
            when(siteProjectRelationRepo.findByProjectId(5)).thenReturn(Collections.emptyList());

            List<Integer> result = service.getSitesForProject(5);

            assertThat(result).isEmpty();
            verify(siteProjectRelationRepo, times(1)).findByProjectId(5);
        }

        @Test
        @DisplayName("UT-SB-421: relation.getSite() が null の場合、NPE が発生する（現行仕様の記録）")
        void relationWithNullSite_throwsNpe() {
            SiteProjectRelation orphan = SiteProjectRelation.builder().site(null).build();
            when(siteProjectRelationRepo.findByProjectId(1))
                    .thenReturn(List.of(relationFor(1), orphan));

            // Relasi yatim (site sudah dihapus tapi relasi tertinggal)
            // akan menggagalkan seluruh project.
            assertThrows(NullPointerException.class, () -> service.getSitesForProject(1));
        }
    }
}