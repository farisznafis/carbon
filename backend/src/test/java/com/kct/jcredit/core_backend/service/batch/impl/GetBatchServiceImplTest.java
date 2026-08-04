package com.kct.jcredit.core_backend.service.batch.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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


/**
 * GetBatchServiceImpl hanya membaca dan memetakan entity -> DTO. Untuk site/project
 * reduction result, saya pakai angka yang sama dengan baris pertama data dummy
 * (en_r002_reduction_result id=1, site 1, project 1) yang sudah kita verifikasi terhadap
 * rumus EN-R-002 sebelumnya: empj=0.018, empjSControl=0.011, empjSBattery=0.007.
 * Untuk project total, dipakai baris en_r002_reduction_result_total: empj=0.404, embl=10.903.
 */
@ExtendWith(MockitoExtension.class)
class GetBatchServiceImplTest {

    @Mock
    private EnR002ReductionResultRepository reductionResultRepository;
    @Mock
    private EnR002ReductionResultTotalRepository reductionResultTotalRepository;
    @Mock
    private BatchProcessHistoryRepository batchProcessHistoryRepository;
    @Mock
    private SiteRepository siteRepository;

    private GetBatchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GetBatchServiceImpl(
            reductionResultRepository, reductionResultTotalRepository,
            batchProcessHistoryRepository, siteRepository);
    }

    // ---------- getBatchProcessHistory ----------

    @Test
    @DisplayName("getBatchProcessHistory applies default page=1 and limit=20 when nulls are passed")
    void getBatchProcessHistory_appliesDefaults() {
        BatchProcessHistory history = BatchProcessHistory.builder()
            // .id(1)
            .status("SUCCESS").build();
        Page<BatchProcessHistory> page = new PageImpl<>(List.of(history), PageRequest.of(0, 20), 1);

        when(batchProcessHistoryRepository.findAll(any(Pageable.class))).thenReturn(page);

        BatchProcessHistoryResponse response = service.getBatchProcessHistory(null, null, null);

        assertThat(response.getPagination().getPage()).isEqualTo(1);
        assertThat(response.getPagination().getLimit()).isEqualTo(20);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getId()).isEqualTo(1);

        var pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(batchProcessHistoryRepository).findAll(pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(0); // page 1 -> zero-indexed 0
        assertThat(used.getPageSize()).isEqualTo(20);
        assertThat(used.getSort()).isEqualTo(Sort.by("startTime").descending());
    }

    @Test
    @DisplayName("getBatchProcessHistory caps limit at 100 even if a larger value is requested")
    void getBatchProcessHistory_capsLimitAt100() {
        Page<BatchProcessHistory> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 100), 0);
        when(batchProcessHistoryRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        BatchProcessHistoryResponse response = service.getBatchProcessHistory(1, 500, null);

        assertThat(response.getPagination().getLimit()).isEqualTo(100);
        var pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(batchProcessHistoryRepository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("getBatchProcessHistory falls back to page 1 for zero or negative page values")
    void getBatchProcessHistory_invalidPageFallsBackToOne() {
        Page<BatchProcessHistory> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        when(batchProcessHistoryRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        BatchProcessHistoryResponse response = service.getBatchProcessHistory(-3, 20, null);

        assertThat(response.getPagination().getPage()).isEqualTo(1);
    }

    // ---------- getSiteReductionResult ----------

    @Test
    @DisplayName("getSiteReductionResult returns empty items when the site public ID is not found")
    void getSiteReductionResult_siteNotFound_returnsEmpty() {
        when(siteRepository.findByPublicIdWithFallback("unknown-site")).thenReturn(Optional.empty());

        SiteReductionResultResponse response = service.getSiteReductionResult("unknown-site", 1);

        assertThat(response.getItems()).isEmpty();
        verify(reductionResultRepository, never())
            .findByProjectIdAndSiteIdAndIsActiveTrueOrderByStartTimeDesc(any(), any());
    }

    @Test
    @DisplayName("getSiteReductionResult returns empty items when the site has no active results")
    void getSiteReductionResult_siteFoundButNoResults_returnsEmpty() {
        Site site = siteWithId(1);
        when(siteRepository.findByPublicIdWithFallback("site-1")).thenReturn(Optional.of(site));
        when(reductionResultRepository.findByProjectIdAndSiteIdAndIsActiveTrueOrderByStartTimeDesc(1, 1))
            .thenReturn(Collections.emptyList());

        SiteReductionResultResponse response = service.getSiteReductionResult("site-1", 1);

        assertThat(response.getItems()).isEmpty();
    }

    @Test
    @DisplayName("getSiteReductionResult maps entity fields to DTO using the site-1/day-1 dummy values")
    void getSiteReductionResult_mapsFieldsCorrectly() {
        Site site = siteWithId(1);
        when(siteRepository.findByPublicIdWithFallback("site-1")).thenReturn(Optional.of(site));

        EnR002ReductionResult dummyResult = EnR002ReductionResult.builder()
            // .id(1)
            // .isActive(true)
            .batchId(1)
            .processingDate(LocalDate.of(2026, 7, 1))
            .projectId(1)
            .siteId(1)
            .empj(new BigDecimal("0.018"))
            .empjM(new BigDecimal("0.000"))
            .empjS(new BigDecimal("0.018"))
            .empjSControl(new BigDecimal("0.011"))
            .empjSBattery(new BigDecimal("0.007"))
            .build();

        when(reductionResultRepository.findByProjectIdAndSiteIdAndIsActiveTrueOrderByStartTimeDesc(1, 1))
            .thenReturn(List.of(dummyResult));

        SiteReductionResultResponse response = service.getSiteReductionResult("site-1", 1);

        assertThat(response.getItems()).hasSize(1);
        SiteReductionResultItemDto dto = response.getItems().get(0);
        assertThat(dto.getSiteId()).isEqualTo(1);
        assertThat(dto.getEmpj()).isEqualByComparingTo("0.018");
        assertThat(dto.getEmpjSControl()).isEqualByComparingTo("0.011");
        assertThat(dto.getEmpjSBattery()).isEqualByComparingTo("0.007");
        // empjSControl + empjSBattery == empjS untuk baris ini (0.011 + 0.007 = 0.018)
        assertThat(dto.getEmpjSControl().add(dto.getEmpjSBattery())).isEqualByComparingTo(dto.getEmpjS());
    }

    // ---------- getProjectReductionResult ----------

    @Test
    @DisplayName("getProjectReductionResult returns OK with null data when no total exists yet")
    void getProjectReductionResult_notFound_returnsNullData() {
        when(reductionResultTotalRepository.findByProjectId(99)).thenReturn(Optional.empty());

        ProjectReductionResultResponse response = service.getProjectReductionResult(99);

        assertThat(response.getStatusCode()).isEqualTo("OK");
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("getProjectReductionResult maps the aggregated project total using the batch dummy total row")
    void getProjectReductionResult_found_mapsTotals() {
        EnR002ReductionResultTotal total = EnR002ReductionResultTotal.builder()
            // .id(1)
            // .isActive(true)
            .batchId(1)
            .processingDate(LocalDate.of(2026, 7, 10))
            .projectId(1)
            .empj(new BigDecimal("0.404"))
            .empjM(new BigDecimal("0.000"))
            .empjS(new BigDecimal("0.404"))
            .empjSControl(new BigDecimal("0.291"))
            .empjSBattery(new BigDecimal("0.113"))
            .embl(new BigDecimal("10.903"))
            .build();

        when(reductionResultTotalRepository.findByProjectId(1)).thenReturn(Optional.of(total));

        ProjectReductionResultResponse response = service.getProjectReductionResult(1);

        assertThat(response.getStatusCode()).isEqualTo("OK");
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getEmpj()).isEqualByComparingTo("0.404");
        assertThat(response.getData().getEmbl()).isEqualByComparingTo("10.903");
    }

    /**
     * id ada di BaseEntity (parent class), sementara Site hanya pakai @Builder biasa
     * (bukan @SuperBuilder) sehingga SiteBuilder tidak punya method id(...).
     * Maka di-set lewat setter setelah object dibangun.
     */
    private static Site siteWithId(int id) {
        Site site = Site.builder().build();
        site.setId(id);
        return site;
    }
}