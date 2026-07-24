package com.kct.jcredit.core_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.BatchProcessHistory;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResultTotal;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResult;
import com.kct.jcredit.core_backend.repository.postgres.BatchProcessHistoryRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultTotalRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringTableRecordSolarRepository;
import com.kct.jcredit.core_backend.dto.batch.ProcessingResult;
import com.kct.jcredit.core_backend.dto.batch.ComparisonResult;
import com.kct.jcredit.core_backend.dto.batch.TargetLogEntry;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * バッチ計算処理サービス
 * 太陽光発電モニタリングデータの集計・計算を行うバッチ処理
 * 
 * Flow:
 * 1. 現在時刻取得
 * 2. 処理履歴テーブルの読み込み
 * 3. NoSQLデータ読み込み (DynamoDB monitoring table)
 * 4. 処理対象ログの作成 (CSV)
 * 5. 計算処理 (サイト別排出量計算)
 * 6. 処理後ログの作成 (CSV)
 * 7. ログ比較 (重複・欠落チェック)
 * 8. 各プロジェクトの削減実績テーブルの更新 (monitoring_total)
 * 9. 処理履歴テーブルの更新
 * 10. 次のプロジェクトへ (loop)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchProcessServiceSolar {

    // Repository Dependencies
    private final BatchProcessHistoryRepository batchHistoryRepo;
    private final EnR002ReductionResultRepository reductionResultRepo;
    private final EnR002ReductionResultTotalRepository reductionResultTotalRepo;
    private final MonitoringTableRecordSolarRepository monitoringTableRepo; // DynamoDB access
    private final ProjectRepository projectRepository;
    private final SiteProjectRelationRepository siteProjectRelationRepo;
    private final SiteRepository siteRepository;
    
    // Service Dependencies
    private final CsvLogService csvLogService;
    private final NotificationService notificationService;
    
    // Configuration
    private static final String LOG_BASE_PATH = "/var/batch/logs/";
    private static final String SERVER_NAME = "batch-server-01"; // TODO: Get from config（サーバー名称はまだ不明）
    private static final BigDecimal PROJECT_THRESHOLD_TONS = new BigDecimal("100.000");

    /**
     * Main batch processing entry point.
     * Processes all active solar projects sequentially.
     * 
     * @param processingDate Target processing date (yyyy-MM-dd)
     * @throws RuntimeException if all projects fail or unexpected error occurs
     */
    public void executeAllProjects(LocalDate processingDate) {
        checkForRunningBatch();
        log.info("Batch processing started for date: {}", processingDate);
        
        // Create batch history record
        BatchProcessHistory batchHistory = createBatchHistory();
        Integer batchId = batchHistory.getId();
        
        try {
            // Get active projects and process
            List<Integer> projectIds = getActiveSolarProjectIds();
            BatchResult result = processAllProjects(batchId, projectIds, processingDate);
            
            // Update batch history with final status
            updateBatchHistoryOnCompletion(batchHistory, result);
            
            // Fail if all projects failed
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
     * Creates and saves initial batch history record.
     */
    private BatchProcessHistory createBatchHistory() {
        BatchProcessHistory history = BatchProcessHistory.builder()
            .startTime(Instant.now())
            .status("RUNNING")
            .claimedBy(SERVER_NAME)
            .claimedAt(Instant.now())
            .build();
        
        history.setIsActive(true);
        history = batchHistoryRepo.save(history);
        
        log.info("Batch ID {} created", history.getId());
        return history;
    }

    /**
     * Checks if another batch is already running.
     * Prevents multiple batches from running simultaneously.
     * 
     * Should be called BEFORE creating batch history in executeAllProjects().
     * 
     * @throws IllegalStateException if another batch is running
     */
    void checkForRunningBatch() {
        List<BatchProcessHistory> runningBatches = batchHistoryRepo
            .findByStatus("RUNNING");
        
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
        Optional<BatchProcessHistory> batch = batchHistoryRepo.findById(batchId);
        Instant batchStartTime = batch.map(BatchProcessHistory::getStartTime).orElse(Instant.EPOCH);
        Instant lastBatchStartTime = getLastBatchStartTime().orElse(Instant.EPOCH);
        
        for (Integer projectId : projectIds) {
            try {
                processProject(batchId, projectId, processingDate, batchStartTime, lastBatchStartTime);
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
     * Updates batch history after successful completion.
     * Notifies operators if batch has errors (partial failures).
     */
    private void updateBatchHistoryOnCompletion(BatchProcessHistory history, BatchResult result) {
        history.setEndTime(Instant.now());
        history.setStatus(result.determineStatus());
        batchHistoryRepo.save(history);
        
        log.info("Batch completed - Total: {}, Success: {}, Failed: {}", 
            result.totalCount, result.successCount, result.failedCount);
        
        if (result.hasFailures()) {
            result.failedProjects.forEach(msg -> log.warn("  - {}", msg));
            
            // Notify operators about partial failures
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
     * Updates batch history on unexpected failure.
     * Notifies all operators about the batch processing error.
     */
    private void updateBatchHistoryOnFailure(BatchProcessHistory history) {
        history.setEndTime(Instant.now());
        history.setStatus("FAILED");
        batchHistoryRepo.save(history);
        
        // Notify operators about complete failure
        notifyBatchCompleteFailure(history.getId());
    }

    /**
     * Holds batch processing results.
     */
    private static class BatchResult {
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







    /**
     * Processes a single project by calculating emissions for all its sites
     * and aggregating the results.
     * 
     * @param batchId Batch ID from parent process
     * @param projectId Target project ID
     * @param processingDate Target processing date
     * @throws IllegalArgumentException if duplicate processing detected
     * @throws RuntimeException if processing fails
     */
    @Transactional
    public void processProject(Integer batchId, Integer projectId, LocalDate processingDate, Instant batchStartTime, Instant lastBatchStartTime) {
        log.info("Processing project {} (batch {})", projectId, batchId);  
        // Load monitoring data
        List<Integer> siteIds = getSitesForProject(projectId);
         Map<Integer, List<EnR002MonitoringRecord>> siteMonitoringData = 
        loadMonitoringDataFromDynamoDB(projectId, siteIds, batchStartTime, lastBatchStartTime);
        
        log.info("Processing {} sites", siteIds.size());
        
        // Create CSV logs
        CsvPaths csvPaths = createCsvPaths(batchId, projectId, processingDate);
        csvLogService.createTargetCsv(csvPaths.target, createTargetLogEntries(batchId, projectId, siteIds));
        csvLogService.createEmptyResultCsv(csvPaths.result);
        
        // Process each site
        List<EnR002ReductionResult> reductionResultList = 
            processSites(batchId, projectId, siteIds, siteMonitoringData, processingDate, csvPaths.result, lastBatchStartTime);
        
        // Compare logs
        ComparisonResult comparison = csvLogService.compareProcessingLogs(
            csvPaths.target, csvPaths.result, csvPaths.comparison);
        
        if (comparison.hasDuplicates()) {
            throw new IllegalArgumentException("Duplicate processing detected for project " + projectId);
        }
        
        // Aggregate and save totals
        EnR002ReductionResultTotal total = aggregateProjectTotals(
            batchId, projectId, processingDate, reductionResultList);
        total.setIsActive(true);
        reductionResultTotalRepo.save(total);
        
        log.info("Project {} completed", projectId);
    }

    /**
     * Processes all sites for a project.
     */
    private List<EnR002ReductionResult> processSites(
            Integer batchId,
            Integer projectId,
            List<Integer> siteIds,
            Map<Integer, List<EnR002MonitoringRecord>> siteMonitoringData,
            LocalDate processingDate,
            String resultCsvPath,
            Instant lastBatchStartTime) {
        
        List<EnR002ReductionResult> reductionResultList = new ArrayList<>();
        
        for (Integer siteId : siteIds) {
            ProcessingResult result = processSingleSite(
                batchId, projectId, siteId, 
                siteMonitoringData.getOrDefault(siteId, Collections.emptyList()), 
                processingDate, 
                reductionResultList,
                lastBatchStartTime
            );
            
            csvLogService.appendToResultCsv(resultCsvPath, result);
        }
        
        return reductionResultList;
    }

    /**
     * Processes a single site and returns processing result of nodata / success/ failure
     */
    //TODO: consider a site level total table to add to at each batch so the site can see all contributuions
    private ProcessingResult processSingleSite(
            Integer batchId,
            Integer projectId,
            Integer siteId,
            List<EnR002MonitoringRecord> siteData,
            LocalDate processingDate,
            List<EnR002ReductionResult> reductionResultList,
            Instant lastBatchStartTime) {
        
        Instant startTime = Instant.now();
        
        try {
            if (siteData.isEmpty()) {
                log.debug("No data for site {}", siteId);

                EnR002ReductionResult lastResult = lastReductionResult(projectId, siteId);
                EnR002ReductionResult noDataResult;
                if (lastResult != null) {
                    noDataResult = buildMonitoringItem(
                        batchId,
                        projectId,
                        siteId,
                        processingDate,
                        startTime,
                        Instant.now(),
                        lastResult.getEmpjSControl(),
                        lastResult.getEmpjM(),
                        lastResult.getEmpjSControl(),
                        lastResult.getEmpjSBattery(),
                        lastResult.getEmpjS(),
                        lastResult.getEmpj()
                    );
                } else {
                    noDataResult = buildMonitoringItem(
                        batchId,
                        projectId,
                        siteId,
                        processingDate,
                        startTime,
                        Instant.now(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                    );
                }
                noDataResult.setIsActive(true);
                noDataResult = reductionResultRepo.save(noDataResult);
                reductionResultList.add(noDataResult);
                return ProcessingResult.noData(batchId, projectId, siteId);
            }
            
            // Calculate emissions
            EnR002ReductionResult reductionResult = 
                calculateSiteEmissions(batchId, projectId, siteId, siteData, processingDate, lastBatchStartTime);
            reductionResult.setIsActive(true);

            //add to previous reduction result if exists for the same site
            EnR002ReductionResult lastResult = lastReductionResult(projectId, siteId);
            if (lastResult != null) {
                reductionResult.setEmpj(reductionResult.getEmpj().add(lastResult.getEmpj()));
                reductionResult.setEmpjM(reductionResult.getEmpjM().add(lastResult.getEmpjM()));
                reductionResult.setEmpjS(reductionResult.getEmpjS().add(lastResult.getEmpjS()));
                reductionResult.setEmpjSControl(reductionResult.getEmpjSControl().add(lastResult.getEmpjSControl()));
                reductionResult.setEmpjSBattery(reductionResult.getEmpjSBattery().add(lastResult.getEmpjSBattery()));
            }
            
            // Save to database
            reductionResult = reductionResultRepo.save(reductionResult);
            reductionResultList.add(reductionResult);
            
            return ProcessingResult.success(
                batchId, projectId, siteId, 
                startTime, Instant.now(),
                siteData.size(), 
                reductionResult.getId()
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
     * Creates CSV file paths for batch processing.
     */
    private CsvPaths createCsvPaths(Integer batchId, Integer projectId, LocalDate processingDate) {
        String dateStr = processingDate.format(DateTimeFormatter.ISO_DATE);
        String prefix = String.format("batch_%d_project_%d", batchId, projectId);
        
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
     * Holds CSV file paths for batch processing.
     */
    private static class CsvPaths {
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
     * Loads unprocessed monitoring data from DynamoDB within batch time range.
     * 
     * Retrieves records that:
     * 1. Were registered between last batch start and current batch start (time range query)
     * 2. Have processingDate = null (not yet processed)
     * 3. Have isActive = true (active records only)
     * 
     * This approach is more efficient than scanning all records.
     * 
     * @param projectId Project ID
     * @param siteIds List of site IDs belonging to the project
     * @param currentBatchStartTime Start time of current batch
     * @return Map of site ID to list of unprocessed monitoring records
     */
    Map<Integer, List<EnR002MonitoringRecord>> loadMonitoringDataFromDynamoDB(
            Integer projectId, 
            List<Integer> siteIds, 
            Instant currentBatchStartTime, 
            Instant lastBatchStartTime) {
    
        log.info("Loading data registered between {} and {}", 
            lastBatchStartTime, currentBatchStartTime);
        
        Map<Integer, List<EnR002MonitoringRecord>> result = new HashMap<>();
        int totalRecords = 0;
        
        for (Integer siteId : siteIds) {
            List<EnR002MonitoringRecord> unprocessedRecords = 
                loadUnprocessedRecordsForSite(projectId, siteId, lastBatchStartTime, currentBatchStartTime);
            
            result.put(siteId, unprocessedRecords);
            totalRecords += unprocessedRecords.size();
            
            log.debug("Site {}: {} unprocessed records", siteId, unprocessedRecords.size());
        }
        
        log.info("Loaded {} total unprocessed records", totalRecords);
        return result;
    }

    /**
     * Gets the start time of the last completed batch.
     * 
     * @return Last batch start time, or empty if no previous batch
     */
    Optional<Instant> getLastBatchStartTime() {
        BatchProcessHistory lastBatch = batchHistoryRepo.findFirstByStatusInOrderByStartTimeDesc(
            Arrays.asList("COMPLETED", "COMPLETED_WITH_ERRORS")
        );
        return Optional.ofNullable(lastBatch).map(BatchProcessHistory::getStartTime);
    }

    /**
     * Loads unprocessed records for a single site within the time range.
     * 
     * Uses DynamoDB time-range query for efficiency, then filters in-memory.
     * 
     * @param projectId Project ID
     * @param siteId Site ID
     * @param lastBatchStart Previous batch start time
     * @param currentBatchStart Current batch start time
     * @return List of unprocessed monitoring records
     */
    private List<EnR002MonitoringRecord> loadUnprocessedRecordsForSite(
            Integer projectId,
            Integer siteId,
            Instant lastBatchStart,
            Instant currentBatchStart) {
        
        Site site = siteRepository.findById(siteId).orElse(null);
        String sitePublicId = site != null ? site.getPublicId() : String.valueOf(siteId);
        String projectPublicId = projectRepository.findById(projectId)
                .map(Project::getPublicId)
                .orElse(String.valueOf(projectId));
        String pk = sitePublicId + "#" + projectPublicId;
        String startSk = lastBatchStart.toString();
        String endSk = currentBatchStart.toString();
        
        // Query DynamoDB with time range (efficient - uses sort key)
        List<EnR002MonitoringRecord> recordsInRange = 
            monitoringTableRepo.findByPkAndSkBetween(pk, startSk, endSk);
        
        // Filter for unprocessed and active records only (in-memory - fast)
        return recordsInRange.stream()
            .filter(record -> record.getProcessedAt() == null)
            .filter(record -> Boolean.TRUE.equals(record.getIsActive()))
            .collect(Collectors.toList());
    }


        /**
     * Calculates site-level emissions from monitoring data.
     * 
     * Calculation formulas:
     * 1. ELPJ,control = ELPV - ELPVR (Solar generation - Reverse power flow)
     * 2. EM_PJ,M = 0 (Solar has no methane emissions)
     * 3. EM_PJ,S,control = ELPJ,control × CEF_electricity,t
     * 4. EM_PJ,S,battery = ELPJ,battery × CEF_electricity,t (conditional)
     * 5. EM_PJ,S = EM_PJ,S,control + EM_PJ,S,battery
     * 6. EM_PJ = EM_PJ,M + EM_PJ,S
     * 
     * @param batchId Current batch ID
     * @param projectId Project ID
     * @param siteId Site ID
     * @param monitoringData List of monitoring records for the site
     * @param processingDate Date being processed
     * @param lastBatchStartTime Start time of last completed batch
     * @return Calculated monitoring item record
     * @throws IllegalArgumentException if monitoring data is empty
     */
    EnR002ReductionResult calculateSiteEmissions(
            Integer batchId,
            Integer projectId,
            Integer siteId,
            List<EnR002MonitoringRecord> monitoringData,
            LocalDate processingDate,
            Instant lastBatchStartTime) {
        
        log.debug("Calculating emissions for site {} on date {}", siteId, processingDate);


        
        // Validate input
        if (monitoringData == null || monitoringData.isEmpty()) {
            return buildMonitoringItem(
            batchId, projectId, siteId, processingDate,
            lastBatchStartTime, Instant.now(),
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        
        // STEP 1: Get activity start date
        LocalDate activityStartDate = getActivityStartDate(projectId, siteId);
        log.debug("Activity start date for site {}: {}", siteId, activityStartDate);
        
        // STEP 2: Get CEF electricity value based on activity start date
        BigDecimal cefElectricity = getCefElectricity(activityStartDate, processingDate);
        log.debug("CEF electricity: {}", cefElectricity);
        
        // STEP 3: Aggregate monitoring data (ELPV, ELPVR, Battery)
        MonitoringAggregation aggregation = aggregateMonitoringData(monitoringData);
        
        log.debug("Site {} aggregation - ELPV: {}, ELPVR: {}, Battery: {}, Changed: {}", 
            siteId, aggregation.totalElpv, aggregation.totalElpvr, 
            aggregation.totalElpjBattery, aggregation.batteryChanged);
        
        // STEP 4: Calculate ELPJ,control = ELPV - ELPVR
        BigDecimal elpjControl = aggregation.totalElpv.subtract(aggregation.totalElpvr);
        log.debug("ELPJ,control: {} kWh", elpjControl);
        
        // STEP 5: EM_PJ,M = 0 (Solar has no methane emissions)
        BigDecimal empjM = BigDecimal.ZERO;
        
        // STEP 6: Calculate EM_PJ,S,control = ELPJ,control × CEF
        BigDecimal empjSControl = calculateEmissions(elpjControl, cefElectricity);
        log.debug("EM_PJ,S,control: {} tCO2", empjSControl);
        
        // STEP 7: Calculate battery emissions (conditional)
        BigDecimal empjSBattery = calculateBatteryEmissions(
            projectId, siteId, processingDate,
            aggregation.totalElpjBattery, 
            aggregation.batteryChanged,
            cefElectricity
        );
        
        if (empjSBattery.compareTo(BigDecimal.ZERO) > 0) {
            log.debug("EM_PJ,S,battery: {} tCO2", empjSBattery);
        }
        
        // STEP 8: EM_PJ,S = EM_PJ,S,control + EM_PJ,S,battery
        BigDecimal empjS = empjSControl.add(empjSBattery);
        
        // STEP 9: EM_PJ = EM_PJ,M + EM_PJ,S
        BigDecimal empj = empjM.add(empjS);
        
        log.info("Site {} emissions - Total: {}, Control: {}, Battery: {}",
            siteId, empj, empjSControl, empjSBattery);
        
        // STEP 10: Build monitoring item entity
        return buildMonitoringItem(
            batchId, projectId, siteId, processingDate,
            lastBatchStartTime, Instant.now(),
           elpjControl, empjM, empjSControl, empjSBattery, empjS, empj
        );
    }

    // finds the last Result for site and project
    private EnR002ReductionResult lastReductionResult(Integer projectId, Integer siteId) {
        return reductionResultRepo.findTopByProjectIdAndSiteIdAndIsActiveTrueOrderByStartTimeDesc(projectId, siteId);
    }

    /**
     * Aggregates monitoring data for emissions calculation.
     */
    private MonitoringAggregation aggregateMonitoringData(
            List<EnR002MonitoringRecord> monitoringData) {
        
        BigDecimal totalElpv = BigDecimal.ZERO;
        BigDecimal totalElpvr = BigDecimal.ZERO;
        BigDecimal totalElpjBattery = BigDecimal.ZERO;
        boolean batteryChanged = false;
        
        for (EnR002MonitoringRecord record : monitoringData) {
            totalElpv = safeAdd(totalElpv, record.getElPv());
            totalElpvr = safeAdd(totalElpvr, record.getElPvr());
            totalElpjBattery = safeAdd(totalElpjBattery, record.getElpjBatteryValue());
            
            if (Boolean.TRUE.equals(record.getChangedBattery())) {
                batteryChanged = true;
            }
        }
        
        return new MonitoringAggregation(
            totalElpv, totalElpvr, totalElpjBattery, batteryChanged);
    }

    /**
     * Calculates emissions from energy value and CEF.
     * 
     * Formula: Energy (kWh) × CEF (tCO2/kWh)
     * Result scale: 3 decimal places (tCO2)
     */
    private BigDecimal calculateEmissions(BigDecimal energy, BigDecimal cef) {
        return energy.multiply(cef)
            .setScale(3, RoundingMode.HALF_UP);
    }

    /**
     * Calculates battery emissions based on conditions.
     * 
     * Battery emissions are calculated when:
     * - Battery was changed during this period, OR
     * - One year has passed since last calculation
     * 
     * @return Battery emissions in tCO2, or ZERO if conditions not met
     */
    private BigDecimal calculateBatteryEmissions(
            Integer projectId,
            Integer siteId,
            LocalDate processingDate,
            BigDecimal totalElpjBattery,
            boolean batteryChanged,
            BigDecimal cefElectricity) {
        
        // Check if battery calculation needed
        boolean shouldCalculate = batteryChanged || 
            oneYearSinceLastCalculation(projectId, siteId, processingDate);
        
        // Must have battery usage data
        boolean hasBatteryUsage = totalElpjBattery.compareTo(BigDecimal.ZERO) > 0;
        
        if (shouldCalculate && hasBatteryUsage) {
            return calculateEmissions(totalElpjBattery, cefElectricity);
        }
        
        log.debug("Battery emissions calculation skipped for site {} " +
            "(changed: {}, should_calc: {}, has_usage: {})",
            siteId, batteryChanged, shouldCalculate, hasBatteryUsage);
        
        return BigDecimal.ZERO;
    }

    /**
     * Builds EnR002reductionResult entity from calculated values.
     */
    private EnR002ReductionResult buildMonitoringItem(
            Integer batchId,
            Integer projectId,
            Integer siteId,
            LocalDate processingDate,
            Instant startTime,
            Instant endTime,
            BigDecimal elpjControl,
            BigDecimal empjM,
            BigDecimal empjSControl,
            BigDecimal empjSBattery,
            BigDecimal empjS,
            BigDecimal empj) {
        
        return EnR002ReductionResult.builder()
            .batchId(batchId)
            .processingDate(processingDate)
            .projectId(projectId)
            .siteId(siteId)
            .startTime(startTime)
            .endTime(endTime)
            // Emissions
            .empj(empj)
            .empjM(empjM)
            .empjS(empjS)
            .empjSControl(empjSControl)
            .empjSBattery(empjSBattery)
            // Note: Other fields (batteryMemberCount, etc.) need to be calculated separately
            // or passed in if required
            .build();
    }

    /**
     * Helper class to hold aggregated monitoring data.
     */
    private static class MonitoringAggregation {
        final BigDecimal totalElpv;
        final BigDecimal totalElpvr;
        final BigDecimal totalElpjBattery;
        final boolean batteryChanged;
        
        MonitoringAggregation(BigDecimal totalElpv, BigDecimal totalElpvr,
                            BigDecimal totalElpjBattery, boolean batteryChanged) {
            this.totalElpv = totalElpv;
            this.totalElpvr = totalElpvr;
            this.totalElpjBattery = totalElpjBattery;
            this.batteryChanged = batteryChanged;
        }
    }

/**
 * 活動開始日を取得
 * site_project_relation テーブルから取得
 */
    LocalDate getActivityStartDate(Integer projectId, Integer siteId) {
        SiteProjectRelation relation = siteProjectRelationRepo.findByProjectIdAndSiteId(projectId, siteId);
        return relation.getActivityStartDate();
    }








    /**
     * Marginal CO2 emission factor (tCO2/kWh).
     * Represents emissions from marginal/peak power generation (typically fossil fuel plants).
     * 
     * Source: [Add CDM methodology reference]
     * Region: Japan
     */
    private static final BigDecimal CMO = new BigDecimal("0.000512"); // TODO: Set official value

    /**
     * All-power CO2 emission factors by year (tCO2/kWh).
     * Represents average emissions from entire grid mix.
     * 
     * Source: Ministry of Environment, Japan / Electric Power Council
     * Updated: [Add date]
     */
    private static final Map<Integer, BigDecimal> CA_MAP = new HashMap<>() {{
        put(2020, new BigDecimal("0.000460")); // TODO: Fill with official values
        put(2021, new BigDecimal("0.000444"));
        put(2022, new BigDecimal("0.000436"));
        put(2023, new BigDecimal("0.000441"));
        put(2024, new BigDecimal("0.000438"));
    }};

    /**
     * Default all-power emission factor when year not found.
     */
    private static final BigDecimal DEFAULT_CA = new BigDecimal("0.000441");

    /**
     * Calculates CEF (Carbon Emission Factor) using CDM methodology.
     * 
     * Formula: CEF_electricity,t = C_mo × (1 - f(t)) + C_a(t) × f(t)
     * 
     * Where:
     * - t: Years since project start (activity start date)
     * - C_mo: Marginal power CO2 emission factor
     * - C_a(t): All-power CO2 emission factor for year t
     * - f(t): Transition function
     *   - 0.0   if 0 ≤ t < 1 year
     *   - 0.5   if 1 year ≤ t < 2.5 years
     *   - 1.0   if t ≥ 2.5 years
     * 
     * Logic:
     * - Early projects (< 1 year): Use marginal factor (higher, conservative)
     * - Transition period (1-2.5 years): Blend marginal and all-power
     * - Mature projects (≥ 2.5 years): Use all-power factor (grid average)
     * 
     * @param activityStartDate Project activity start date
     * @param processingDate Current processing date
     * @return CEF value in tCO2/kWh
     */
    BigDecimal getCefElectricity(LocalDate activityStartDate, LocalDate processingDate) {
        
        // Validation
        if (activityStartDate.isAfter(processingDate)) {
            throw new IllegalArgumentException(
                "Activity start date cannot be after processing date: " + 
                activityStartDate + " > " + processingDate);
        }
        
        // Calculate t: years since project start
        double yearsSinceStart = calculateYearsSinceStart(activityStartDate, processingDate);
        
        // Calculate f(t): transition function
        double transitionFactor = calculateTransitionFactor(yearsSinceStart);
        
        // Get C_a(t): all-power emission factor for processing year
        BigDecimal ca = getAllPowerEmissionFactor(processingDate.getYear());
        
        // Apply formula: CEF = C_mo × (1 - f(t)) + C_a(t) × f(t)
        BigDecimal cef = calculateCef(CMO, ca, transitionFactor);
        
        log.info("CEF calculation: start={}, processing={}, years={}, f(t)={}, C_mo={}, C_a={}, CEF={}",
            activityStartDate, processingDate, 
            String.format("%.2f", yearsSinceStart),
            String.format("%.1f", transitionFactor),
            CMO, ca, cef);
        
        return cef;
    }

    /**
     * Calculates years since project start (fractional).
     * 
     * Example:
     * - Start: 2023-01-01, Processing: 2024-07-01 → 1.5 years
     * 
     * @param startDate Project start date
     * @param processingDate Processing date
     * @return Years as decimal (e.g., 1.5)
     */
    private double calculateYearsSinceStart(LocalDate startDate, LocalDate processingDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, processingDate);
        double years = daysBetween / 365.25; 
        
        log.debug("Days since start: {}, Years: {}", daysBetween, String.format("%.2f", years));
        
        return years;
    }

    /**
     * Calculates transition factor f(t) based on years since start.
     * 
     * CDM Transition Function:
     * - f(t) = 0.0   if 0 ≤ t < 1 year       (Use marginal factor only)
     * - f(t) = 0.5   if 1 year ≤ t < 2.5 years (50/50 blend)
     * - f(t) = 1.0   if t ≥ 2.5 years         (Use all-power factor only)
     * 
     * @param yearsSinceStart Years since project start
     * @return Transition factor (0.0, 0.5, or 1.0)
     */
    private double calculateTransitionFactor(double yearsSinceStart) {
        if (yearsSinceStart < 1.0) {
            log.debug("Project age < 1 year: using marginal factor only (f=0.0)");
            return 0.0;
        } else if (yearsSinceStart < 2.5) {
            log.debug("Project age 1-2.5 years: using blended factor (f=0.5)");
            return 0.5;
        } else {
            log.debug("Project age ≥ 2.5 years: using all-power factor only (f=1.0)");
            return 1.0;
        }
    }

    /**
     * Gets all-power CO2 emission factor for a specific year.
     * 
     * @param year Processing year
     * @return C_a(t) in tCO2/kWh
     */
    private BigDecimal getAllPowerEmissionFactor(int year) {
        BigDecimal ca = CA_MAP.get(year);
        
        if (ca == null || ca.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("All-power emission factor not found for year {}. Using default: {}", 
                year, DEFAULT_CA);
            return DEFAULT_CA;
        }
        
        return ca;
    }

    /**
     * Calculates final CEF using the CDM formula.
     * 
     * Formula: CEF = C_mo × (1 - f(t)) + C_a(t) × f(t)
     * 
     * @param cMo Marginal emission factor
     * @param ca All-power emission factor
     * @param transitionFactor f(t) value
     * @return CEF in tCO2/kWh
     */
    private BigDecimal calculateCef(
            BigDecimal cMo, 
            BigDecimal ca, 
            double transitionFactor) {
        
        BigDecimal fT = BigDecimal.valueOf(transitionFactor);
        BigDecimal oneMinusFt = BigDecimal.ONE.subtract(fT);
        
        // CEF = C_mo × (1 - f(t)) + C_a(t) × f(t)
        BigDecimal marginalPart = cMo.multiply(oneMinusFt);
        BigDecimal allPowerPart = ca.multiply(fT);
        BigDecimal cef = marginalPart.add(allPowerPart);
        
        // Round to 6 decimal places for precision
        cef = cef.setScale(6, RoundingMode.HALF_UP);
        
        log.debug("CEF calculation breakdown: C_mo×(1-f)={}, C_a×f={}, Total={}",
            marginalPart, allPowerPart, cef);
        
        return cef;
    }


    /**
     * Checks if one year has passed since last battery emissions calculation.
     * 
     * Battery emissions are recalculated when:
     * - No previous battery calculation exists (first time), OR
     * - More than 1 year (365 days) has passed since last calculation
     * 
     * @param projectId Project ID
     * @param siteId Site ID
     * @param processingDate Current processing date
     * @return true if battery calculation should be performed
     */
    boolean oneYearSinceLastCalculation(
            Integer projectId, 
            Integer siteId, 
            LocalDate processingDate) {
        
        // Find last battery calculation (where empjSBattery > 0)
        Optional<LocalDate> lastBatteryDate = 
            reductionResultRepo.findLastBatteryCalculationDate(projectId, siteId);
        
        if (lastBatteryDate.isEmpty()) {
            return true;
        }
        
        // Calculate days since last battery calculation
        LocalDate lastDate = lastBatteryDate.get();
        long daysSince = ChronoUnit.DAYS.between(lastDate, processingDate);
        
        return daysSince >= 365;
    }



    /**
     * Aggregates and updates project totals cumulatively.
     * Creates new total record if none exists, otherwise adds to existing totals.
     * 
     * @param batchId Current batch ID
     * @param projectId Project ID
     * @param processingDate Processing date
     * @param reductionResultList Processed site data for this batch
     * @return Updated total record
     */
    //TODO: consider storing total at each batch timestep for history and chart representation

    EnR002ReductionResultTotal aggregateProjectTotals(
            Integer batchId,
            Integer projectId,
            LocalDate processingDate,
            List<EnR002ReductionResult> reductionResultList) {
        
        // Get or create total record
        EnR002ReductionResultTotal previousTotal = reductionResultTotalRepo
            .findByProjectId(projectId)
            .orElse(null);
        EnR002ReductionResultTotal reductionResultTotal = previousTotal;
        boolean isNewRecord = false;
        if (reductionResultTotal == null) {
            isNewRecord = true;
            reductionResultTotal = createNewTotal(projectId, batchId, processingDate);
            reductionResultTotal.setIsActive(true);
        }
        

        
        // Sum batch emissions //update batch totals is the total sum replace values in Project total.
        EmissionTotals batchTotals = sumBatchEmissions(reductionResultList);
        
        // Add batch totals to cumulative totals
        reductionResultTotal.setEmpj(batchTotals.empj);
        reductionResultTotal.setEmpjM(batchTotals.empjM);
        reductionResultTotal.setEmpjS(batchTotals.empjS);
        reductionResultTotal.setEmpjSControl(batchTotals.empjSControl);
        reductionResultTotal.setEmpjSBattery( batchTotals.empjSBattery);
        
        // Update metadata (for both new and existing records)
        updateMetadata(reductionResultTotal, batchId, processingDate, isNewRecord);
        
        
        log.info("Project {} totals updated: batch +{} tCO2, cumulative {} tCO2 ({})",
            projectId, batchTotals.empj, reductionResultTotal.getEmpj(),
            isNewRecord ? "new" : "updated");
        EnR002ReductionResultTotal savedTotal = reductionResultTotalRepo.save(reductionResultTotal);
        if (shouldSendProjectThresholdNotification(previousTotal, savedTotal)) {
            notifyProjectThresholdReached(projectId, savedTotal.getEmpj());
        }
        return savedTotal;
    }

    private boolean shouldSendProjectThresholdNotification(
            EnR002ReductionResultTotal previousTotal,
            EnR002ReductionResultTotal currentTotal) {
        if (currentTotal == null || currentTotal.getEmpj() == null) {
            return false;
        }
        if (currentTotal.getEmpj().compareTo(PROJECT_THRESHOLD_TONS) < 0) {
            return false;
        }
        if (previousTotal == null) {
            return true;
        }
        BigDecimal previousEmpj = previousTotal.getEmpj() == null
                ? BigDecimal.ZERO
                : previousTotal.getEmpj();
        return previousEmpj.compareTo(PROJECT_THRESHOLD_TONS) < 0;
    }

    private void notifyProjectThresholdReached(Integer projectId, BigDecimal currentEmpj) {
        String title = "プロジェクト排出量が100トンに到達しました";
        String body = "プロジェクトID " + projectId + " の排出量が "
                + currentEmpj.setScale(3, RoundingMode.HALF_UP)
                + " tCO2 に達しました。";
        notificationService.createNotificationForAllOperatingUsers(title, body);
        notificationService.sendEmailToAllOperatingUsers(title, body);
        log.info("Sent 100-ton threshold notification and email for project {}", projectId);
    }

    /**
     * Notifies all operators about batch processing completion with errors.
     * Called when batch completes but some projects fail.
     * 
     * @param status Batch status (COMPLETED_WITH_ERRORS or FAILED)
     * @param totalProjects Total number of projects attempted
     * @param successfulProjects Number of projects successfully processed
     * @param failedProjects Number of failed projects
     * @param failureDetails List of failure messages
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
        
        notificationService.createNotificationForAllOperatingUsers(title, body.toString());
        notificationService.sendEmailToAllOperatingUsers(title, body.toString());
        log.warn("Sent batch error notification and email - Status: {}, Failed: {}/{}", 
            status, failedProjects, totalProjects);
    }

    /**
     * Notifies all operators about complete batch failure.
     * Called when batch processing fails unexpectedly.
     * 
     * @param batchId Batch ID that failed
     */
    private void notifyBatchCompleteFailure(Integer batchId) {
        String title = "バッチ処理が失敗しました";
        String body = "バッチID " + batchId + " の処理が予期しないエラーにより失敗しました。\n\n" +
            "システム管理者に連絡して、ログを確認し、原因を特定してください。";
        
        notificationService.createNotificationForAllOperatingUsers(title, body);
        notificationService.sendEmailToAllOperatingUsers(title, body);
        log.error("Sent batch complete failure notification and email for batch {}", batchId);
    }

    /**
     * Creates new monitoring total record with all required fields.
     * Initializes emission values to zero.
     * 
     * @param projectId Project ID
     * @param batchId Batch ID for first calculation
     * @param processingDate Processing date for first calculation
     * @return New total record with all fields populated
     */
    private EnR002ReductionResultTotal createNewTotal(
            Integer projectId, 
            Integer batchId, 
            LocalDate processingDate) {
        
        Instant now = Instant.now();
        
        return EnR002ReductionResultTotal.builder()
            // Identity fields
            .projectId(projectId)
            .batchId(batchId)
            .processingDate(processingDate)
            
            // Timestamp fields (nullable = false)
            .startTime(now)
            .endTime(now)
            
            // Emission totals (nullable = false for empj, empjM)
            .empj(BigDecimal.ZERO)
            .empjM(BigDecimal.ZERO)
            
            // Optional emission fields (nullable = true)
            .empjS(BigDecimal.ZERO)
            .empjSControl(BigDecimal.ZERO)
            .empjSBattery(BigDecimal.ZERO)   
            .build();
    }

    /**
     * Sums emissions across all monitoring items.
     */
    private EmissionTotals sumBatchEmissions(List<EnR002ReductionResult> items) {
        
        BigDecimal totalEmpj = BigDecimal.ZERO;
        BigDecimal totalEmpjM = BigDecimal.ZERO;
        BigDecimal totalEmpjS = BigDecimal.ZERO;
        BigDecimal totalEmpjSControl = BigDecimal.ZERO;
        BigDecimal totalEmpjSBattery = BigDecimal.ZERO;
        
        for (EnR002ReductionResult item : items) {
            if (item.getEmpj() != null) {
                totalEmpj = totalEmpj.add(item.getEmpj());
            }
            if (item.getEmpjM() != null) {
                totalEmpjM = totalEmpjM.add(item.getEmpjM());
            }
            if (item.getEmpjS() != null) {
                totalEmpjS = totalEmpjS.add(item.getEmpjS());
            }
            if (item.getEmpjSControl() != null) {
                totalEmpjSControl = totalEmpjSControl.add(item.getEmpjSControl());
            }
            if (item.getEmpjSBattery() != null) {
                totalEmpjSBattery = totalEmpjSBattery.add(item.getEmpjSBattery());
            }
        }
        
        return new EmissionTotals(totalEmpj, totalEmpjM, totalEmpjS, 
                                totalEmpjSControl, totalEmpjSBattery);
    }

    /**
     * Helper class to hold batch emission totals.
     */
    private static class EmissionTotals {
        final BigDecimal empj;
        final BigDecimal empjM;
        final BigDecimal empjS;
        final BigDecimal empjSControl;
        final BigDecimal empjSBattery;
        
        EmissionTotals(BigDecimal empj, BigDecimal empjM, BigDecimal empjS,
                    BigDecimal empjSControl, BigDecimal empjSBattery) {
            this.empj = empj;
            this.empjM = empjM;
            this.empjS = empjS;
            this.empjSControl = empjSControl;
            this.empjSBattery = empjSBattery;
        }
    }


    /**
     * Updates metadata fields on total record.
     * For new records: sets startTime and endTime
     * For updates: only updates endTime, batchId, and processingDate
     */
    private void updateMetadata(
            EnR002ReductionResultTotal total,
            Integer batchId,
            LocalDate processingDate,
            boolean isNewRecord) {
        
        Instant now = Instant.now();
        
        // For new records, startTime is already set in createNewTotal()
        // For updates, keep original startTime
        if (isNewRecord) {
            total.setStartTime(now);
        }
        
        // Always update these fields
        total.setEndTime(now);
        total.setBatchId(batchId);
        total.setProcessingDate(processingDate);
    }

    /**
     * Safely adds two BigDecimal values, treating nulls as zero.
     */
    BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        return (a == null ? BigDecimal.ZERO : a)
            .add(b == null ? BigDecimal.ZERO : b);
    }








    /**
     * プロジェクトに属するサイトIDリストを取得
     */
    List<Integer> getSitesForProject(Integer projectId) {
        // SELECT site_id FROM sites WHERE project_id = ? AND is_active = true
        List<Integer> siteIds = siteProjectRelationRepo.findByProjectId(projectId)
            .stream()
            .map(relation -> relation.getSite().getId())
            .collect(Collectors.toList());
        return siteIds;
    }
    
    /**
     * アクティブなプロジェクトIDリストを取得
     */
    List<Integer> getActiveSolarProjectIds() {
     // すべてのアクティブなプロジェクトを取得
    List<Project> allProjects = projectRepository.findAll().stream().filter(project -> project.getIsActive()).collect(Collectors.toList());
    //get all 太陽光発電プロジェクト
   // List<Project> solarProjects = projectRepository.findByMethodType("enR002");
    // 太陽光発電方法論のプロジェクトのみフィルタリング
    List<Integer> solarProjectIds = allProjects.stream()
        //.filter(project -> "solar".equals(project.getMethodCategory())) TODO: Fix once method category is 対象
        .map(Project::getId)
        .collect(Collectors.toList());
    
    log.info("Found {} active solar projects", solarProjectIds.size());
    
    return solarProjectIds;
    }
}