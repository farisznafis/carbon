package com.kct.jcredit.core_backend.service.batch.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kct.jcredit.core_backend.entity.EnR002ReductionResult;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResultTotal;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultTotalRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.batch.calculator.SolarEmissionsCalculator;
import com.kct.jcredit.core_backend.service.batch.repository.SolarMonitoringRepositoryAdapter;
import com.kct.jcredit.core_backend.entity.BatchProcessHistory;
import com.kct.jcredit.core_backend.repository.postgres.BatchProcessHistoryRepository;
import com.kct.jcredit.core_backend.service.batch.BatchHistoryService;

@ExtendWith(MockitoExtension.class)

class BatchHistoryServiceImplTest {

    @Mock
    private BatchProcessHistoryRepository batchHistoryRepository;

    private BatchHistoryServiceImpl service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new BatchHistoryServiceImpl(batchHistoryRepository);
    }

    @Test
    @DisplayName("saveBatchHistory delegates directly to repository.save and returns saved entity")
    void saveBatchHistory_delegatesToRepository() {
        BatchProcessHistory toSave = BatchProcessHistory.builder()
            .startTime(Instant.parse("2026-07-10T12:00:00Z"))
            .status("RUNNING")
            .claimedBy("batch-server-01")
            .claimedAt(Instant.parse("2026-07-10T12:00:00Z"))
            .build();
        BatchProcessHistory saved = BatchProcessHistory.builder()
            .startTime(toSave.getStartTime())
            .status("RUNNING")
            .build();
        saved.setId(1); // id ada di BaseEntity, tidak ter-expose lewat builder

        when(batchHistoryRepository.save(toSave)).thenReturn(saved);

        BatchProcessHistory result = service.saveBatchHistory(toSave);

        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo("RUNNING");
        verify(batchHistoryRepository, times(1)).save(toSave);
    }

    @Test
    @DisplayName("getBatchById returns populated Optional when found")
    void getBatchById_found() {
        BatchProcessHistory history = BatchProcessHistory.builder().status("SUCCESS").build();
        history.setId(1);
        when(batchHistoryRepository.findById(1)).thenReturn(Optional.of(history));

        Optional<BatchProcessHistory> result = service.getBatchById(1);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("getBatchById returns empty Optional when not found")
    void getBatchById_notFound() {
        when(batchHistoryRepository.findById(999)).thenReturn(Optional.empty());

        Optional<BatchProcessHistory> result = service.getBatchById(999);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getRunningBatches queries repository with status RUNNING")
    void getRunningBatches_queriesWithRunningStatus() {
        BatchProcessHistory running = BatchProcessHistory.builder().status("RUNNING").build();
        running.setId(2);
        when(batchHistoryRepository.findByStatus("RUNNING")).thenReturn(List.of(running));

        List<BatchProcessHistory> result = service.getRunningBatches();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2);
        verify(batchHistoryRepository).findByStatus("RUNNING");
    }

    @Test
    @DisplayName("getRunningBatches returns empty list when nothing is running")
    void getRunningBatches_emptyWhenNoneRunning() {
        when(batchHistoryRepository.findByStatus("RUNNING")).thenReturn(Collections.emptyList());

        List<BatchProcessHistory> result = service.getRunningBatches();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getLastCompletedBatchStartTime returns empty Optional when repository returns null")
    void getLastCompletedBatchStartTime_returnsEmptyWhenRepositoryReturnsNull() {
        // Kode asli: BatchProcessHistory lastBatch = repository.findFirstByStatusInOrderByStartTimeDesc(...)
        // Method ini TIDAK mengembalikan Optional, jadi harus dites bahwa null dari repo
        // tetap aman dibungkus jadi Optional.empty() dan tidak melempar NPE.
        when(batchHistoryRepository.findFirstByStatusInOrderByStartTimeDesc(anyList()))
            .thenReturn(null);

        Optional<Instant> result = service.getLastCompletedBatchStartTime();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getLastCompletedBatchStartTime returns startTime and queries with COMPLETED statuses")
    void getLastCompletedBatchStartTime_returnsStartTimeWhenFound() {
        Instant expectedStartTime = Instant.parse("2026-07-09T12:00:00Z");
        BatchProcessHistory lastBatch = BatchProcessHistory.builder()
            .status("COMPLETED")
            .startTime(expectedStartTime)
            .build();
        lastBatch.setId(5);

        when(batchHistoryRepository.findFirstByStatusInOrderByStartTimeDesc(anyList()))
            .thenReturn(lastBatch);

        Optional<Instant> result = service.getLastCompletedBatchStartTime();

        assertThat(result).contains(expectedStartTime);

        // Pastikan status yang dipakai untuk query adalah COMPLETED dan COMPLETED_WITH_ERRORS,
        // bukan misalnya hanya "SUCCESS" atau "RUNNING" — ini penting karena
        // processProject() di BaseBatchProcessService memakai nilai ini sebagai batas bawah
        // rentang data yang diambil dari DynamoDB.
        ArgumentCaptor<List<String>> statusCaptor = ArgumentCaptor.forClass(List.class);
        verify(batchHistoryRepository).findFirstByStatusInOrderByStartTimeDesc(statusCaptor.capture());
        assertThat(statusCaptor.getValue())
            .containsExactlyInAnyOrder("COMPLETED", "COMPLETED_WITH_ERRORS");
    }
}