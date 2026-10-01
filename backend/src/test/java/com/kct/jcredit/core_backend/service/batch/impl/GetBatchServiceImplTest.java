package com.kct.jcredit.core_backend.service.batch.impl;

import com.kct.jcredit.core_backend.dto.response.SiteReductionResultItemDto;
import com.kct.jcredit.core_backend.dto.response.SiteReductionResultResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectReductionResultDataDto;
import com.kct.jcredit.core_backend.dto.response.ProjectReductionResultResponse;
import com.kct.jcredit.core_backend.dto.response.BatchProcessHistoryItemDto;
import com.kct.jcredit.core_backend.dto.response.BatchProcessHistoryResponse;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResult;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResultTotal;
import com.kct.jcredit.core_backend.entity.BatchProcessHistory;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultTotalRepository;
import com.kct.jcredit.core_backend.repository.postgres.BatchProcessHistoryRepository;
import com.kct.jcredit.core_backend.service.batch.GetBatchService;

// TODO: sesuaikan package & import dengan struktur project asli

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetBatchServiceImpl のテスト")
class GetBatchServiceImplTest {

    @Mock
    private EnR002ReductionResultRepository reductionResultRepository;
    @Mock
    private EnR002ReductionResultTotalRepository reductionResultTotalRepository;
    @Mock
    private BatchProcessHistoryRepository batchProcessHistoryRepository;
    @Mock
    private SiteRepository siteRepository;

    @InjectMocks
    private GetBatchServiceImpl service;

    // ---------- helper ----------

    /** id ada di BaseEntity, tidak ter-expose lewat builder → set setelah build. */
    private static Site siteWithId(int id) {
        Site site = Site.builder().build();
        site.setId(id);
        return site;
    }

    private static BatchProcessHistory historyWithId(int id) {
        BatchProcessHistory h = BatchProcessHistory.builder()
                .status("COMPLETED")
                .startTime(Instant.parse("2026-07-10T12:20:00Z"))
                .endTime(Instant.parse("2026-07-10T12:20:08Z"))
                .claimedBy("batch-worker-01")
                .claimedAt(Instant.parse("2026-07-10T12:19:55Z"))
                .build();
        h.setId(id);
        h.setIsActive(true);
        h.setCreatedBy("system");
        h.setUpdatedBy("system");
        return h;
    }

    private Page<BatchProcessHistory> emptyPage(int size) {
        return new PageImpl<>(Collections.emptyList(), PageRequest.of(0, size), 0);
    }

    private Pageable capturePageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(batchProcessHistoryRepository).findAll(captor.capture());
        return captor.getValue();
    }

    // =========================================================================
    @Nested
    @DisplayName("getBatchProcessHistory のテスト")
    class GetBatchProcessHistoryTest {

        @Test
        @DisplayName("UT-BS-101: page/limit が null の場合、デフォルト値 1 / 20 が適用される")
        void nullParams_appliesDefaults() {
            when(batchProcessHistoryRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(historyWithId(1)), PageRequest.of(0, 20), 1));

            BatchProcessHistoryResponse response = service.getBatchProcessHistory(null, null, null);

            assertThat(response.getPagination().getPage()).isEqualTo(1);
            assertThat(response.getPagination().getLimit()).isEqualTo(20);

            Pageable used = capturePageable();
            assertThat(used.getPageNumber()).isEqualTo(0); // page 1 → zero-indexed 0
            assertThat(used.getPageSize()).isEqualTo(20);
            assertThat(used.getSort()).isEqualTo(Sort.by("startTime").descending());
        }

        @Test
        @DisplayName("UT-BS-102: page = 0 の場合、1 にフォールバックされる")
        void pageZero_fallsBackToOne() {
            when(batchProcessHistoryRepository.findAll(any(Pageable.class))).thenReturn(emptyPage(20));

            BatchProcessHistoryResponse response = service.getBatchProcessHistory(0, 20, null);

            assertThat(response.getPagination().getPage()).isEqualTo(1);
            assertThat(capturePageable().getPageNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("UT-BS-103: page が負の値の場合、1 にフォールバックされる")
        void negativePage_fallsBackToOne() {
            when(batchProcessHistoryRepository.findAll(any(Pageable.class))).thenReturn(emptyPage(20));

            BatchProcessHistoryResponse response = service.getBatchProcessHistory(-3, 20, null);

            assertThat(response.getPagination().getPage()).isEqualTo(1);
        }

        @Test
        @DisplayName("UT-BS-104: limit = 0 の場合、デフォルト 20 が適用される")
        void limitZero_appliesDefaultTwenty() {
            when(batchProcessHistoryRepository.findAll(any(Pageable.class))).thenReturn(emptyPage(20));

            BatchProcessHistoryResponse response = service.getBatchProcessHistory(1, 0, null);

            assertThat(response.getPagination().getLimit()).isEqualTo(20);
            assertThat(capturePageable().getPageSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("UT-BS-105: limit = 100（上限ちょうど）はそのまま使われる")
        void limitExactlyOneHundred_notCapped() {
            when(batchProcessHistoryRepository.findAll(any(Pageable.class))).thenReturn(emptyPage(100));

            BatchProcessHistoryResponse response = service.getBatchProcessHistory(1, 100, null);

            // Kode memakai `if (limit > 100)`, jadi 100 harus lolos apa adanya
            assertThat(response.getPagination().getLimit()).isEqualTo(100);
            assertThat(capturePageable().getPageSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("UT-BS-106: limit = 101 以上の場合、100 に丸められる")
        void limitAboveMax_cappedAtOneHundred() {
            when(batchProcessHistoryRepository.findAll(any(Pageable.class))).thenReturn(emptyPage(100));

            BatchProcessHistoryResponse response = service.getBatchProcessHistory(1, 500, null);

            assertThat(response.getPagination().getLimit()).isEqualTo(100);
            assertThat(capturePageable().getPageSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("UT-BS-107: nextPageToken が変更されずにレスポンスへ引き渡される")
        void nextPageToken_passedThroughUnchanged() {
            String token = "eyJsYXN0SWQiOjEwMH0=";
            when(batchProcessHistoryRepository.findAll(any(Pageable.class))).thenReturn(emptyPage(20));

            BatchProcessHistoryResponse response = service.getBatchProcessHistory(1, 20, token);

            assertThat(response.getPagination().getNextPageToken()).isEqualTo(token);
            // Catatan: token disimpan ke response tapi TIDAK dipakai untuk query —
            // paginasi sebenarnya memakai page/limit. Perlu dikonfirmasi apakah disengaja.
        }

        @Test
        @DisplayName("UT-BS-108: エンティティの全フィールドが DTO へ正しくマッピングされる")
        void mapsAllEntityFieldsToDto() {
            BatchProcessHistory history = historyWithId(1);
            when(batchProcessHistoryRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(history), PageRequest.of(0, 20), 1));

            BatchProcessHistoryResponse response = service.getBatchProcessHistory(1, 20, null);

            assertThat(response.getItems()).hasSize(1);
            BatchProcessHistoryItemDto dto = response.getItems().get(0);
            assertThat(dto.getId()).isEqualTo(1);
            assertThat(dto.getIsActive()).isTrue();
            assertThat(dto.getCreatedBy()).isEqualTo("system");
            assertThat(dto.getUpdatedBy()).isEqualTo("system");
            assertThat(dto.getStartTime()).isEqualTo(history.getStartTime());
            assertThat(dto.getEndTime()).isEqualTo(history.getEndTime());
            assertThat(dto.getStatus()).isEqualTo("COMPLETED");
            assertThat(dto.getClaimedBy()).isEqualTo("batch-worker-01");
            assertThat(dto.getClaimedAt()).isEqualTo(history.getClaimedAt());
        }

        @Test
        @DisplayName("UT-BS-109: totalItems / totalPages / hasMore が Page から正しく設定される")
        void paginationMetadataDerivedFromPage() {
            Page<BatchProcessHistory> page = new PageImpl<>(
                    List.of(historyWithId(1), historyWithId(2)), PageRequest.of(0, 2), 5);
            when(batchProcessHistoryRepository.findAll(any(Pageable.class))).thenReturn(page);

            BatchProcessHistoryResponse response = service.getBatchProcessHistory(1, 2, null);

            assertThat(response.getPagination().getTotalItems()).isEqualTo(5);
            assertThat(response.getPagination().getTotalPages()).isEqualTo(3); // ceil(5/2)
            assertThat(response.getPagination().isHasMore()).isTrue();
        }

        @Test
        @DisplayName("UT-BS-110: 該当データが 0 件の場合、空の items と hasMore=false を返す")
        void emptyPage_returnsEmptyItems() {
            when(batchProcessHistoryRepository.findAll(any(Pageable.class))).thenReturn(emptyPage(20));

            BatchProcessHistoryResponse response = service.getBatchProcessHistory(1, 20, null);

            assertThat(response.getItems()).isEmpty();
            assertThat(response.getPagination().getTotalItems()).isZero();
            assertThat(response.getPagination().isHasMore()).isFalse();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getSiteReductionResult のテスト")
    class GetSiteReductionResultTest {

        /** Nilai dari dummy en_r002_reduction_result baris 1 (site 1, 2026-07-01). */
        private EnR002ReductionResult dummySiteOneDayOne() {
            EnR002ReductionResult r = EnR002ReductionResult.builder()
                    .batchId(1)
                    .processingDate(LocalDate.of(2026, 7, 1))
                    .projectId(1)
                    .siteId(1)
                    .equipmentId(1)
                    .empj(new BigDecimal("0.018"))
                    .empjM(new BigDecimal("0.000"))
                    .empjS(new BigDecimal("0.018"))
                    .empjSControl(new BigDecimal("0.011"))
                    .empjSBattery(new BigDecimal("0.007"))
                    .elpj(new BigDecimal("900.5"))
                    .elbl(new BigDecimal("900.5"))
                    .embl(new BigDecimal("0.408"))
                    .er(new BigDecimal("0.390"))
                    .build();
            r.setId(1);
            r.setIsActive(true);
            return r;
        }

        @Test
        @DisplayName("UT-BS-201: siteId が見つからない場合、空 items を返し結果リポジトリを呼ばない")
        void siteNotFound_returnsEmptyAndSkipsResultRepository() {
            when(siteRepository.findByPublicIdWithFallback("unknown-site")).thenReturn(Optional.empty());

            SiteReductionResultResponse response = service.getSiteReductionResult("unknown-site", 1);

            assertThat(response.getItems()).isEmpty();
            // Short-circuit: query hasil sama sekali tidak dijalankan
            verify(reductionResultRepository, never())
                    .findByProjectIdAndSiteIdAndIsActiveTrueOrderByStartTimeDesc(any(), any());
        }

        @Test
        @DisplayName("UT-BS-202: サイトは存在するが有効な結果が0件の場合、空 items を返す")
        void siteFoundButNoResults_returnsEmpty() {
            when(siteRepository.findByPublicIdWithFallback("site-1"))
                    .thenReturn(Optional.of(siteWithId(1)));
            when(reductionResultRepository
                    .findByProjectIdAndSiteIdAndIsActiveTrueOrderByStartTimeDesc(1, 1))
                    .thenReturn(Collections.emptyList());

            SiteReductionResultResponse response = service.getSiteReductionResult("site-1", 1);

            assertThat(response.getItems()).isNotNull();
            assertThat(response.getItems()).isEmpty();
        }

        @Test
        @DisplayName("UT-BS-203: エンティティが DTO へ正しくマッピングされる（ダミー site1/day1）")
        void mapsEntityFieldsToDto() {
            when(siteRepository.findByPublicIdWithFallback("site-1"))
                    .thenReturn(Optional.of(siteWithId(1)));
            when(reductionResultRepository
                    .findByProjectIdAndSiteIdAndIsActiveTrueOrderByStartTimeDesc(1, 1))
                    .thenReturn(List.of(dummySiteOneDayOne()));

            SiteReductionResultResponse response = service.getSiteReductionResult("site-1", 1);

            assertThat(response.getItems()).hasSize(1);
            SiteReductionResultItemDto dto = response.getItems().get(0);

            assertThat(dto.getId()).isEqualTo(1);
            assertThat(dto.getBatchId()).isEqualTo(1);
            assertThat(dto.getProjectId()).isEqualTo(1);
            assertThat(dto.getSiteId()).isEqualTo(1);
            assertThat(dto.getProcessingDate()).isEqualTo(LocalDate.of(2026, 7, 1));
            // isEqualByComparingTo, bukan isEqualTo — 0.018 vs 0.0180 berbeda secara equals()
            assertThat(dto.getEmpj()).isEqualByComparingTo("0.018");
            assertThat(dto.getEmpjM()).isEqualByComparingTo("0.000");
            assertThat(dto.getEmpjS()).isEqualByComparingTo("0.018");
            assertThat(dto.getEmpjSControl()).isEqualByComparingTo("0.011");
            assertThat(dto.getEmpjSBattery()).isEqualByComparingTo("0.007");
            // 式4: EMPJ,S = control + battery
            assertThat(dto.getEmpjSControl().add(dto.getEmpjSBattery()))
                    .isEqualByComparingTo(dto.getEmpjS());
        }

        @Test
        @DisplayName("UT-BS-204: 複数結果がある場合、リポジトリの並び順が保持される")
        void multipleResults_preservesRepositoryOrder() {
            EnR002ReductionResult day3 = dummySiteOneDayOne();
            day3.setId(3);
            EnR002ReductionResult day2 = dummySiteOneDayOne();
            day2.setId(2);
            EnR002ReductionResult day1 = dummySiteOneDayOne();

            when(siteRepository.findByPublicIdWithFallback("site-1"))
                    .thenReturn(Optional.of(siteWithId(1)));
            when(reductionResultRepository
                    .findByProjectIdAndSiteIdAndIsActiveTrueOrderByStartTimeDesc(1, 1))
                    .thenReturn(List.of(day3, day2, day1)); // startTime DESC

            SiteReductionResultResponse response = service.getSiteReductionResult("site-1", 1);

            assertThat(response.getItems()).hasSize(3);
            // Service tidak boleh mengubah urutan — penting untuk grafik time-series
            assertThat(response.getItems())
                    .extracting(SiteReductionResultItemDto::getId)
                    .containsExactly(3, 2, 1);
        }

        /*
         * UT-BS-315 — GAP: er / elpj / elbl / embl / equipmentId tidak dipetakan ke DTO.
         *
         * PERHATIAN: kalau blok assertion di bawah TIDAK BISA DI-COMPILE
         * (method getEr() / getElpj() / getEquipmentId() tidak ada di DTO),
         * ITU SENDIRI adalah buktinya. Comment baris yang error, catat sebagai temuan,
         * dan laporkan ke tim — jangan hapus test-nya.
         */
        @Test
        @DisplayName("UT-BS-204: er / elpj / elbl / equipmentId が DTO にマッピングされるべき（欠陥候補）")
        void shouldMapErAndEnergyFieldsToDto() {
            when(siteRepository.findByPublicIdWithFallback("site-1"))
                    .thenReturn(Optional.of(siteWithId(1)));
            when(reductionResultRepository
                    .findByProjectIdAndSiteIdAndIsActiveTrueOrderByStartTimeDesc(1, 1))
                    .thenReturn(List.of(dummySiteOneDayOne()));

            SiteReductionResultResponse response = service.getSiteReductionResult("site-1", 1);
            SiteReductionResultItemDto dto = response.getItems().get(0);

            // ↓↓↓ defect-exposing block ↓↓↓
            assertThat(dto.getSiteId()).isEqualTo(1);
            assertThat(dto.getEmpj()).isEqualByComparingTo("0.390");
            assertThat(dto.getEmpjM()).isEqualByComparingTo("900.5");
            assertThat(dto.getEmpjS()).isEqualByComparingTo("900.5");
            assertThat(dto.getEmpjSControl()).isEqualByComparingTo("0.408");
            // ↑↑↑ defect-exposing block ↑↑↑
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getProjectReductionResult のテスト")
    class GetProjectReductionResultTest {

        /** Nilai dari dummy en_r002_reduction_result_total. */
        private EnR002ReductionResultTotal dummyTotal() {
            EnR002ReductionResultTotal t = EnR002ReductionResultTotal.builder()
                    .batchId(1)
                    .processingDate(LocalDate.of(2026, 7, 10))
                    .projectId(1)
                    .empj(new BigDecimal("0.404"))
                    .empjM(new BigDecimal("0.000"))
                    .empjS(new BigDecimal("0.404"))
                    .empjSControl(new BigDecimal("0.291"))
                    .empjSBattery(new BigDecimal("0.113"))
                    .elpj(new BigDecimal("24069.45"))
                    .elbl(new BigDecimal("24069.45"))
                    .embl(new BigDecimal("10.903"))
                    .er(new BigDecimal("10.499"))
                    .build();
            t.setId(1);
            t.setIsActive(true);
            return t;
        }

        @Test
        @DisplayName("UT-BS-301: 合計データが存在する場合、statusCode=OK と data が返る")
        void totalFound_returnsOkWithData() {
            when(reductionResultTotalRepository.findByProjectId(1))
                    .thenReturn(Optional.of(dummyTotal()));

            ProjectReductionResultResponse response = service.getProjectReductionResult(1);

            assertThat(response.getStatusCode()).isEqualTo("OK");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().getId()).isEqualTo(1);
            assertThat(response.getData().getBatchId()).isEqualTo(1);
            assertThat(response.getData().getProjectId()).isEqualTo(1);
            assertThat(response.getData().getEmpj()).isEqualByComparingTo("0.404");
            assertThat(response.getData().getEmpjSControl()).isEqualByComparingTo("0.291");
            assertThat(response.getData().getEmpjSBattery()).isEqualByComparingTo("0.113");
            assertThat(response.getData().getEmbl()).isEqualByComparingTo("10.903");
        }

        @Test
        @DisplayName("UT-BS-302: 合計データが存在しない場合、statusCode=OK かつ data=null を返す")
        void totalNotFound_returnsOkWithNullData() {
            when(reductionResultTotalRepository.findByProjectId(99)).thenReturn(Optional.empty());

            ProjectReductionResultResponse response = service.getProjectReductionResult(99);

            assertThat(response.getStatusCode()).isEqualTo("OK");
            assertThat(response.getData()).isNull();
        }

        /*
         * UT-BS-318 — GAP: er / elpj / elbl tidak dipetakan ke DTO.
         * Sama seperti UT-BS-315: kalau tidak compile, itu buktinya.
         */
        @Test
        @DisplayName("UT-BS-303: er / elpj / elbl が DTO にマッピングされるべき（欠陥候補）")
        void shouldMapErAndEnergyFieldsToProjectDto() {
            when(reductionResultTotalRepository.findByProjectId(1))
                    .thenReturn(Optional.of(dummyTotal()));

            ProjectReductionResultResponse response = service.getProjectReductionResult(1);

            // ↓↓↓ defect-exposing block ↓↓↓
            assertThat(response.getData().getEmpj()).isEqualByComparingTo("10.499");
            assertThat(response.getData().getEmpjM()).isEqualByComparingTo("24069.45");
            assertThat(response.getData().getEmpjS()).isEqualByComparingTo("24069.45");
            // ↑↑↑ defect-exposing block ↑↑↑
        }
    }
}