package com.kct.jcredit.core_backend.service.batch.impl;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kct.jcredit.core_backend.entity.BatchProcessHistory;
import com.kct.jcredit.core_backend.repository.postgres.BatchProcessHistoryRepository;
import com.kct.jcredit.core_backend.service.batch.BatchHistoryService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BatchHistoryServiceImpl implements BatchHistoryService {
    
    private final BatchProcessHistoryRepository batchHistoryRepository;
    
    @Override
    public BatchProcessHistory saveBatchHistory(BatchProcessHistory history) {
        return batchHistoryRepository.save(history);
    }
    
    @Override
    public Optional<BatchProcessHistory> getBatchById(Integer batchId) {
        return batchHistoryRepository.findById(batchId);
    }
    
    @Override
    public List<BatchProcessHistory> getRunningBatches() {
        return batchHistoryRepository.findByStatus("RUNNING");
    }
    
    @Override
    public Optional<Instant> getLastCompletedBatchStartTime() {
        BatchProcessHistory lastBatch = batchHistoryRepository
            .findFirstByStatusInOrderByStartTimeDesc(
                Arrays.asList("COMPLETED", "COMPLETED_WITH_ERRORS")
            );
        return Optional.ofNullable(lastBatch).map(BatchProcessHistory::getStartTime);
    }
}
