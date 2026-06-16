package com.kct.jcredit.core_backend.service.batch;

import com.kct.jcredit.core_backend.dto.batch.ProcessingResult;
import com.kct.jcredit.core_backend.dto.batch.ComparisonResult;
import com.kct.jcredit.core_backend.dto.batch.TargetLogEntry;

import java.util.List;

/**
 * Service for CSV logging operations during batch processing.
 * Used by BaseBatchProcessService to abstract logging implementation.
 * 
 * @author Modernization Team
 */
public interface CsvLogService {
    
    /**
     * Creates target CSV file with expected entries.
     */
    void createTargetCsv(String filePath, List<TargetLogEntry> entries);
    
    /**
     * Creates empty result CSV file.
     */
    void createEmptyResultCsv(String filePath);
    
    /**
     * Appends a processing result to result CSV.
     */
    void appendToResultCsv(String filePath, ProcessingResult result);
    
    /**
     * Compares target and result CSVs.
     * Detects duplicates, missing entries, and writes comparison log.
     */
    ComparisonResult compareProcessingLogs(String targetPath, String resultPath, String comparisonPath);
}
