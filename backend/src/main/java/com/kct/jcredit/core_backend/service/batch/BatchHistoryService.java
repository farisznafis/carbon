package com.kct.jcredit.core_backend.service.batch;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.kct.jcredit.core_backend.entity.BatchProcessHistory;

/**
 * Service for batch history operations.
 * Used by BaseBatchProcessService to abstract batch history access.
 * 
 * @author Modernization Team
 */
public interface BatchHistoryService {
    
    /**
     * Saves or updates a batch history record.
     */
    BatchProcessHistory saveBatchHistory(BatchProcessHistory history);
    
    /**
     * Finds a batch by ID.
     */
    Optional<BatchProcessHistory> getBatchById(Integer batchId);
    
    /**
     * Gets all currently running batches.
     */
    List<BatchProcessHistory> getRunningBatches();
    
    /**
     * Gets the last completed batch start time.
     */
    Optional<Instant> getLastCompletedBatchStartTime();
}
