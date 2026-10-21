package com.kct.jcredit.core_backend.service.batch.impl;

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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) 
@DisplayName("BatchHistoryServiceImpl のテスト")
class BatchHistoryServiceImplTest {

    @Mock
    private BatchProcessHistoryRepository batchHistoryRepository;

    @InjectMocks
    private BatchHistoryServiceImpl service;
    private BatchProcessHistory historyWith(Integer id, String status, Instant startTime) {
        BatchProcessHistory h = BatchProcessHistory.builder()
                .status(status)
                .startTime(startTime)
                .build();
        if (id != null) {
            h.setId(id);
        }
        return h;
    }

    // =========================================================================
    @Nested
    @DisplayName("saveBatchHistory のテスト")
    class SaveBatchHistoryTest {

        @Test
        @DisplayName("UT-BS-606: リポジトリに委譲し、保存されたエンティティをそのまま返す")
        void saveBatchHistory_delegatesToRepositoryAndReturnsSaved() {
            // Arrange
            BatchProcessHistory toSave = BatchProcessHistory.builder()
                    .startTime(Instant.parse("2026-07-10T12:00:00Z"))
                    .status("RUNNING")
                    .claimedBy("batch-server-01")
                    .claimedAt(Instant.parse("2026-07-10T12:00:00Z"))
                    .build();
            BatchProcessHistory saved = historyWith(1, "RUNNING", toSave.getStartTime());

            when(batchHistoryRepository.save(toSave)).thenReturn(saved);

            // Act
            BatchProcessHistory result = service.saveBatchHistory(toSave);

            // Assert
            assertThat(result).isSameAs(saved);
            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getStatus()).isEqualTo("RUNNING");

            verify(batchHistoryRepository, times(1)).save(toSave);
        }

        @Test
        @DisplayName("UT-BS-602: リポジトリが例外をスローした場合、そのまま伝播される")
        void saveBatchHistory_repositoryThrows_propagates() {
            // Arrange
            BatchProcessHistory toSave = BatchProcessHistory.builder().status("RUNNING").build();
            when(batchHistoryRepository.save(any(BatchProcessHistory.class)))
                    .thenThrow(new DataAccessResourceFailureException("DB down"));

            // Act & Assert — service tidak punya try-catch, exception harus lolos apa adanya
            DataAccessResourceFailureException thrown = assertThrows(
                    DataAccessResourceFailureException.class,
                    () -> service.saveBatchHistory(toSave));

            assertThat(thrown.getMessage()).isEqualTo("DB down");
            verify(batchHistoryRepository, times(1)).save(toSave);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getBatchById のテスト")
    class GetBatchByIdTest {

        @Test
        @DisplayName("UT-BS-603: 該当バッチが存在する場合、値の入った Optional を返す")
        void getBatchById_found_returnsPresentOptional() {
            BatchProcessHistory history = historyWith(1, "SUCCESS", null);
            when(batchHistoryRepository.findById(1)).thenReturn(Optional.of(history));

            Optional<BatchProcessHistory> result = service.getBatchById(1);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1);
            assertThat(result.get().getStatus()).isEqualTo("SUCCESS");
            verify(batchHistoryRepository, times(1)).findById(1);
        }

        @Test
        @DisplayName("UT-BS-604: 該当バッチが存在しない場合、空の Optional を返す")
        void getBatchById_notFound_returnsEmptyOptional() {
            when(batchHistoryRepository.findById(999)).thenReturn(Optional.empty());

            Optional<BatchProcessHistory> result = service.getBatchById(999);

            assertThat(result).isEmpty();
            verify(batchHistoryRepository, times(1)).findById(999);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getRunningBatches のテスト")
    class GetRunningBatchesTest {

        @Test
        @DisplayName("UT-BS-605: クエリ引数が正確に \"RUNNING\" であることを検証する")
        void getRunningBatches_queriesWithExactRunningLiteral() {
            BatchProcessHistory running = historyWith(2, "RUNNING", null);
            when(batchHistoryRepository.findByStatus("RUNNING")).thenReturn(List.of(running));

            List<BatchProcessHistory> result = service.getRunningBatches();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(2);

            // String status di-hardcode di kode produksi. Kalau ada typo (mis. "Running"),
            // query tidak akan pernah cocok dan batch bentrok tidak terdeteksi.
            ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
            verify(batchHistoryRepository, times(1)).findByStatus(statusCaptor.capture());
            assertThat(statusCaptor.getValue()).isEqualTo("RUNNING");
        }

        @Test
        @DisplayName("UT-BS-606: 実行中バッチが1件もない場合、空リストを返す")
        void getRunningBatches_noneRunning_returnsEmptyList() {
            when(batchHistoryRepository.findByStatus("RUNNING")).thenReturn(Collections.emptyList());

            List<BatchProcessHistory> result = service.getRunningBatches();

            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
            verify(batchHistoryRepository, times(1)).findByStatus("RUNNING");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("getLastCompletedBatchStartTime のテスト")
    class GetLastCompletedBatchStartTimeTest {

        @Test
        @DisplayName("UT-BS-607: 検索対象ステータスが COMPLETED と COMPLETED_WITH_ERRORS の2件であること")
        void getLastCompletedBatchStartTime_found_queriesWithBothCompletedStatuses() {
            Instant expectedStartTime = Instant.parse("2026-07-09T12:00:00Z");
            BatchProcessHistory lastBatch = historyWith(5, "COMPLETED", expectedStartTime);

            when(batchHistoryRepository.findFirstByStatusInOrderByStartTimeDesc(anyList()))
                    .thenReturn(lastBatch);

            Optional<Instant> result = service.getLastCompletedBatchStartTime();

            assertThat(result).contains(expectedStartTime);

            // Nilai ini dipakai sebagai batas bawah rentang pengambilan data DynamoDB
            // di processProject(), jadi salah daftar status = salah rentang data.
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> statusCaptor = ArgumentCaptor.forClass(List.class);
            verify(batchHistoryRepository, times(1))
                    .findFirstByStatusInOrderByStartTimeDesc(statusCaptor.capture());
            assertThat(statusCaptor.getValue())
                    .containsExactlyInAnyOrder("COMPLETED", "COMPLETED_WITH_ERRORS");
        }

        @Test
        @DisplayName("UT-BS-608: リポジトリが null を返した場合、NPEにならず空の Optional を返す")
        void getLastCompletedBatchStartTime_repositoryReturnsNull_returnsEmpty() {
            // Method repo ini mengembalikan entity langsung (bukan Optional),
            // jadi null bisa terjadi. Optional.ofNullable() harus melindungi dari NPE.
            when(batchHistoryRepository.findFirstByStatusInOrderByStartTimeDesc(anyList()))
                    .thenReturn(null);

            Optional<Instant> result = service.getLastCompletedBatchStartTime();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("UT-BS-609: エンティティは存在するが startTime が null の場合、空の Optional を返す")
        void getLastCompletedBatchStartTime_entityFoundButStartTimeNull_returnsEmpty() {
            BatchProcessHistory batchWithNullStartTime = historyWith(6, "COMPLETED", null);
            when(batchHistoryRepository.findFirstByStatusInOrderByStartTimeDesc(anyList()))
                    .thenReturn(batchWithNullStartTime);

            Optional<Instant> result = service.getLastCompletedBatchStartTime();

            // Optional.map() yang menerima null menghasilkan Optional.empty()
            assertThat(result).isEmpty();
        }
    }
}