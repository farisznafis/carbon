package com.kct.jcredit.core_backend.service.batch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.kct.jcredit.core_backend.dto.batch.ComparisonResult;
import com.kct.jcredit.core_backend.dto.batch.ProcessingResult;
import com.kct.jcredit.core_backend.dto.batch.TargetLogEntry;
import com.kct.jcredit.core_backend.entity.BatchProcessHistory;
import com.kct.jcredit.core_backend.service.batch.calculator.EmissionsCalculator;
import com.kct.jcredit.core_backend.entity.base.MonitoringRecord;
import com.kct.jcredit.core_backend.service.batch.repository.GenericMonitoringRepository;
import com.kct.jcredit.core_backend.service.NotificationService;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base batch processor for all methodologies.
 * 
 * Handles methodology-agnostic orchestration:
 * - Batch creation and status tracking
 * - Project/site iteration
 * - Data loading from DynamoDB
 * - Result aggregation and persistence
 * - CSV logging
 * - Notifications
 * 
 * Subclasses only need to provide:
 * 1. Repository instance (GenericMonitoringRepository<T>)
 * 2. Calculator instance (EmissionsCalculator<T>)
 * 3. Methods to save results to methodology-specific tables
 * 
 * Type parameter T: The monitoring record type for this methodology.
 * Examples: EnR002MonitoringRecord, MonitoringTableRecordWind
 * 
 * @param <T> Monitoring record type (must implement MonitoringRecord)
 * @author Modernization Team
 */
@Slf4j
public abstract class BaseBatchProcessService<T extends MonitoringRecord, R> {
    
    // Configuration
    private static final String LOG_BASE_PATH = "/var/batch/logs/";
    private static final String SERVER_NAME = "batch-server-01";
    private static final BigDecimal PROJECT_THRESHOLD_TONS = new BigDecimal("100.000");
    
    /**
     * Subclasses must provide the monitoring data repository.
     */
    protected abstract GenericMonitoringRepository<T> getMonitoringRepository();
    
    /**
     * Subclasses must provide the emissions calculator.
     */
    protected abstract EmissionsCalculator<T, R> getCalculator();
    
    /**
     * Subclasses must provide batch history repository.
     */
    protected abstract BatchHistoryService getBatchHistoryService();
    
    /**
     * Subclasses must provide CSV logging service.
     */
    protected abstract CsvLogService getCsvLogService();
    
    /**
     * Subclasses must provide notification service.
     */
    protected abstract NotificationService getNotificationService();
    
    /**
     * Subclasses must implement site-level result persistence.
     * Called after calculating emissions for a single site.
     * 
     * @param projectId Project ID
     * @param siteId Site ID
     * @param batchId Batch ID
     * @param processingDate Processing date
     * @param result Calculation result
     * @return Saved result entity
     */
    protected abstract Object saveSiteResult(
        Integer projectId,
        Integer siteId,
        Integer batchId,
        LocalDate processingDate,
        R result
    );
    
    /**
     * Subclasses must implement project-level result persistence.
     * Called after aggregating all site results for a project.
     * 
     * @param projectId Project ID
     * @param batchId Batch ID
     * @param processingDate Processing date
     * @param siteResults List of saved site result IDs
     * @param aggregated Methodology-specific aggregated totals (e.g., SolarEmissionTotals, WindEmissionTotals)
     * @return Saved project total entity
     */
    protected abstract Object saveProjectTotal(
        Integer projectId,
        Integer batchId,
        LocalDate processingDate,
        List<Object> siteResults,
        Object aggregated
    );
    
    /**
     * Main batch processing entry point.
     * Processes all active projects of this methodology sequentially.
     * 
     * @param processingDate Target processing date
     * @throws RuntimeException if all projects fail or unexpected error occurs
     */
    public void executeAllProjects(LocalDate processingDate) {
        checkForRunningBatch();
        log.info("Batch processing started for {} on date: {}", getCalculator().getMethodologyName(), processingDate);
        
        BatchProcessHistory batchHistory = createBatchHistory();
        Integer batchId = batchHistory.getId();
        
        try {
            List<Integer> projectIds = getActiveProjects();
            BatchResult result = processAllProjects(batchId, projectIds, processingDate);
            
            updateBatchHistoryOnCompletion(batchHistory, result);
            
            if (result.isCompleteFailure()) {
                throw new RuntimeException("All projects failed");
            }
            
        } catch (Exception e) {
            log.error("Batch processing failed", e);
            updateBatchHistoryOnFailure(batchHistory);
            throw new RuntimeException("Batch processing failed", e);
        }
    }
    
    /**
     * Creates and saves batch history record.
     */
    private BatchProcessHistory createBatchHistory() {
        BatchProcessHistory history = BatchProcessHistory.builder()
            .startTime(Instant.now())
            .status("RUNNING")
            .claimedBy(SERVER_NAME)
            .claimedAt(Instant.now())
            .build();
        
        history.setIsActive(true);
        history = getBatchHistoryService().saveBatchHistory(history);
        
        log.info("Batch ID {} created", history.getId());
        return history;
    }
    
    /**
     * Checks if another batch is already running.
     */
    private void checkForRunningBatch() {
        List<BatchProcessHistory> runningBatches = getBatchHistoryService().getRunningBatches();
        
        if (!runningBatches.isEmpty()) {
            throw new IllegalStateException(
                "Another batch is already running: " + runningBatches.get(0).getId()
            );
        }
    }
    
    /**
     * Processes all projects and collects results.
     */
    private BatchResult processAllProjects(
            Integer batchId,
            List<Integer> projectIds,
            LocalDate processingDate) {
        
        log.info("Processing {} active projects", projectIds.size());
        
        int successCount = 0;
        int failedCount = 0;
        List<String> failedProjects = new ArrayList<>();
        
        Optional<Instant> batchStartTime = getBatchHistoryService()
            .getBatchById(batchId)
            .map(BatchProcessHistory::getStartTime);
        
        Optional<Instant> lastBatchStartTime = getLastBatchStartTime();
        
        for (Integer projectId : projectIds) {
            try {
                processProject(
                    batchId, projectId, processingDate,
                    batchStartTime.orElse(Instant.EPOCH),
                    lastBatchStartTime.orElse(Instant.EPOCH)
                );
                successCount++;
                
            } catch (Exception e) {
                log.error("Project {} failed: {}", projectId, e.getMessage());
                failedCount++;
                failedProjects.add(String.format("Project %d: %s", projectId, e.getMessage()));
            }
        }
        
        return new BatchResult(projectIds.size(), successCount, failedCount, failedProjects);
    }
    
    /**
     * Processes a single project (all its sites).
     */
    @Transactional
    public void processProject(
            Integer batchId,
            Integer projectId,
            LocalDate processingDate,
            Instant batchStartTime,
            Instant lastBatchStartTime) {
        
        log.info("Processing project {} (batch {})", projectId, batchId);
        
        // Load site IDs
        List<Integer> siteIds = getSitesForProject(projectId);
        
        // Load monitoring data from DynamoDB
        Map<Integer, List<T>>  siteMonitoringData = getMonitoringRepository()
            .findUnprocessedRecordsForProject(projectId, siteIds, lastBatchStartTime, batchStartTime);
        
        log.info("Processing {} sites", siteIds.size());
        
        // Create CSV logs
        CsvPaths csvPaths = createCsvPaths(batchId, projectId, processingDate);
        getCsvLogService().createTargetCsv(
            csvPaths.target,
            createTargetLogEntries(batchId, projectId, siteIds)
        );
        getCsvLogService().createEmptyResultCsv(csvPaths.result);
        
        // Process each site
        List<Object> siteResults = new ArrayList<>();
        List<R> allResults = new ArrayList<>();
        
        for (Integer siteId : siteIds) {
            List<T> siteData = siteMonitoringData.getOrDefault(siteId, Collections.emptyList());
            
            ProcessingResult processingResult = processSite(
                batchId, projectId, siteId, siteData, processingDate, allResults
            );
            
            getCsvLogService().appendToResultCsv(csvPaths.result, processingResult);
        }
        
        // Compare logs
        ComparisonResult comparison = getCsvLogService().compareProcessingLogs(
            csvPaths.target, csvPaths.result, csvPaths.comparison
        );
        
        if (comparison.hasDuplicates()) {
            throw new IllegalArgumentException("Duplicate processing detected for project " + projectId);
        }
        
        // Aggregate and save project totals (methodology-specific aggregation)
        Object aggregated = aggregateEmissions(allResults);
        saveProjectTotal(projectId, batchId, processingDate, siteResults, aggregated);
        
        log.info("Project {} completed", projectId);
    }
    
    /**
     * Processes a single site.
     */
    private ProcessingResult processSite(
            Integer batchId,
            Integer projectId,
            Integer siteId,
            List<T> siteData,
            LocalDate processingDate,
            List<R> allResults) {
        
        Instant startTime = Instant.now();
        
        try {
            // Calculate emissions
            EmissionsCalculator<T, R> calculator = getCalculator();
            R result = calculator.calculateEmissions(
                projectId, siteId, batchId, siteData, processingDate
            );
            
            // Save site result (always present, may contain zeros for no-data case)
            saveSiteResult(projectId, siteId, batchId, processingDate, result);
            allResults.add(result);
            
            return ProcessingResult.success(
                batchId, projectId, siteId,
                startTime, Instant.now(),
                siteData.size(),
                1 // Result ID placeholder
            );
            
        } catch (Exception e) {
            log.error("Site {} processing failed: {}", siteId, e.getMessage());
            return ProcessingResult.failed(
                batchId, projectId, siteId,
                startTime, Instant.now(),
                e.getMessage()
            );
        }
    }
    

    /**
     * Aggregates emissions across all site results.
     * Subclasses must implement this with methodology-specific aggregation logic.
     * 
     * @param results List of calculation results for all sites
     * @return Methodology-specific aggregated totals
     */
    protected abstract Object aggregateEmissions(List<R> results);
    
    /**
     * Updates batch history on successful completion.
     */
    private void updateBatchHistoryOnCompletion(BatchProcessHistory history, BatchResult result) {
        history.setEndTime(Instant.now());
        history.setStatus(result.determineStatus());
        getBatchHistoryService().saveBatchHistory(history);
        
        log.info("Batch completed - Total: {}, Success: {}, Failed: {}",
            result.totalCount, result.successCount, result.failedCount);
        
        if (result.hasFailures()) {
            result.failedProjects.forEach(msg -> log.warn("  - {}", msg));
            notifyBatchErrors(
                result.determineStatus(),
                result.totalCount,
                result.successCount,
                result.failedCount,
                result.failedProjects
            );
        }
    }
    
    /**
     * Updates batch history on failure.
     */
    private void updateBatchHistoryOnFailure(BatchProcessHistory history) {
        history.setEndTime(Instant.now());
        history.setStatus("FAILED");
        getBatchHistoryService().saveBatchHistory(history);
        
        notifyBatchCompleteFailure(history.getId());
    }
    
    /**
     * Notifies operators about batch errors.
     */
    private void notifyBatchErrors(
            String status,
            int totalProjects,
            int successfulProjects,
            int failedProjects,
            List<String> failureDetails) {
        
        String title = "バッチ処理にエラーが発生しました: " + status;
        
        StringBuilder body = new StringBuilder();
        body.append("バッチ処理が完了しましたが、いくつかのエラーが発生しました。\n\n");
        body.append("処理概要:\n");
        body.append("- 総プロジェクト数: ").append(totalProjects).append("\n");
        body.append("- 成功: ").append(successfulProjects).append("\n");
        body.append("- 失敗: ").append(failedProjects).append("\n\n");
        body.append("失敗の詳細:\n");
        
        for (String detail : failureDetails) {
            body.append("  • ").append(detail).append("\n");
        }
        
        body.append("\n対応が必要な場合は、管理者に連絡してください。");
        
        getNotificationService().createNotificationForAllOperatingUsers(title, body.toString());
    }
    
    /**
     * Notifies operators about complete batch failure.
     */
    private void notifyBatchCompleteFailure(Integer batchId) {
        String title = "バッチ処理が失敗しました";
        String body = "バッチID " + batchId +
            " の処理が予期しないエラーにより失敗しました。\n\n" +
            "システム管理者に連絡して、ログを確認し、原因を特定してください。";
        
        getNotificationService().createNotificationForAllOperatingUsers(title, body);
    }
    
    /**
     * Gets the last completed batch start time.
     */
    protected Optional<Instant> getLastBatchStartTime() {
        return getBatchHistoryService().getLastCompletedBatchStartTime();
    }
    
    /**
     * Gets active projects of this methodology.
     * Subclasses may override to add methodology-specific filtering.
     */
    protected abstract List<Integer> getActiveProjects();
    
    /**
     * Gets sites for a project.
     * Subclasses may override if methodology has different site retrieval logic.
     */
    protected abstract List<Integer> getSitesForProject(Integer projectId);
    
    /**
     * Creates CSV file paths for batch processing.
     */
    private CsvPaths createCsvPaths(Integer batchId, Integer projectId, LocalDate processingDate) {
        String dateStr = processingDate.format(DateTimeFormatter.ISO_DATE);
        String methodology = getCalculator().getMethodologyType();
        String prefix = String.format("batch_%d_%s_project_%d", batchId, methodology, projectId);
        
        return new CsvPaths(
            String.format("%starget/%s_target_%s.csv", LOG_BASE_PATH, prefix, dateStr),
            String.format("%sresults/%s_result_%s.csv", LOG_BASE_PATH, prefix, dateStr),
            String.format("%scomparison/%s_comparison_%s.csv", LOG_BASE_PATH, prefix, dateStr)
        );
    }
    
    /**
     * Creates target log entries for all sites.
     */
    private List<TargetLogEntry> createTargetLogEntries(
            Integer batchId,
            Integer projectId,
            List<Integer> siteIds) {
        
        Instant now = Instant.now();
        
        return siteIds.stream()
            .map(siteId -> TargetLogEntry.builder()
                .batchId(batchId)
                .projectId(projectId)
                .siteId(siteId)
                .expectedStartTime(now)
                .status("PENDING")
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * Safely adds BigDecimals (treats null as zero).
     */
    protected BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        return (a == null ? BigDecimal.ZERO : a)
            .add(b == null ? BigDecimal.ZERO : b);
    }
    
    /**
     * Holds CSV file paths.
     */
    protected static class CsvPaths {
        final String target;
        final String result;
        final String comparison;
        
        CsvPaths(String target, String result, String comparison) {
            this.target = target;
            this.result = result;
            this.comparison = comparison;
        }
    }
    

    /**
     * Holds batch processing results.
     */
    protected static class BatchResult {
        final int totalCount;
        final int successCount;
        final int failedCount;
        final List<String> failedProjects;
        
        BatchResult(int total, int success, int failed, List<String> failedProjects) {
            this.totalCount = total;
            this.successCount = success;
            this.failedCount = failed;
            this.failedProjects = failedProjects;
        }
        
        String determineStatus() {
            if (failedCount == totalCount) {
                return "FAILED";
            } else if (failedCount > 0) {
                return "COMPLETED_WITH_ERRORS";
            } else {
                return "COMPLETED";
            }
        }
        
        boolean isCompleteFailure() {
            return failedCount == totalCount && totalCount > 0;
        }
        
        boolean hasFailures() {
            return failedCount > 0;
        }
    }
}
