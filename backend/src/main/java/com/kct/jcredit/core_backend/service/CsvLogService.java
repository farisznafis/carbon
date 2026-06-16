package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.batch.ComparisonEntry;
import com.kct.jcredit.core_backend.dto.batch.ComparisonResult;
import com.kct.jcredit.core_backend.dto.batch.ProcessingResult;
import com.kct.jcredit.core_backend.dto.batch.TargetLogEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

@Slf4j
@Service
public class CsvLogService {
    
    private static final String TARGET_HEADER = "batch_id,project_id,site_id,expected_start_time,status";
    private static final String RESULT_HEADER = "batch_id,project_id,site_id,start_time,end_time,status,records_processed,monitoring_item_id,error_message";
    private static final String COMPARISON_HEADER = "site_id,target_status,result_status,match_status,issue_type,details";
    
    /**
     * ターゲットCSV作成
     */
    public void createTargetCsv(String filePath, List<TargetLogEntry> entries) {
        try {
            ensureDirectoryExists(filePath);
            
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath))) {
                // Header
                writer.write(TARGET_HEADER);
                writer.newLine();
                
                // Data rows
                for (TargetLogEntry entry : entries) {
                    writer.write(String.format("%d,%d,%d,%s,%s",
                        entry.getBatchId(),
                        entry.getProjectId(),
                        entry.getSiteId(),
                        entry.getExpectedStartTime().toString(),
                        entry.getStatus()
                    ));
                    writer.newLine();
                }
            }
            
            log.info("Target CSV created: {} ({} entries)", filePath, entries.size());
            
        } catch (IOException e) {
            log.error("Failed to create target CSV: {}", filePath, e);
            throw new RuntimeException("CSV creation failed", e);
        }
    }
    
    /**
     * 結果CSV作成（空ファイル）
     */
    public void createEmptyResultCsv(String filePath) {
        try {
            ensureDirectoryExists(filePath);
            
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath))) {
                writer.write(RESULT_HEADER);
                writer.newLine();
            }
            
            log.info("Empty result CSV created: {}", filePath);
            
        } catch (IOException e) {
            log.error("Failed to create result CSV: {}", filePath, e);
            throw new RuntimeException("CSV creation failed", e);
        }
    }
    
    /**
     * 結果CSVに追記（各サイト処理後に呼ばれる）
     */
    public void appendToResultCsv(String filePath, ProcessingResult result) {
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(
                    Paths.get(filePath), 
                    StandardOpenOption.APPEND)) {
                
                writer.write(String.format("%d,%d,%d,%s,%s,%s,%s,%s,%s",
                    result.getBatchId(),
                    result.getProjectId(),
                    result.getSiteId(),
                    result.getStartTime() != null ? result.getStartTime().toString() : "",
                    result.getEndTime() != null ? result.getEndTime().toString() : "",
                    result.getStatus(),
                    result.getRecordsProcessed() != null ? result.getRecordsProcessed() : "",
                    result.getMonitoringItemId() != null ? result.getMonitoringItemId() : "",
                    result.getErrorMessage() != null ? escapeCommas(result.getErrorMessage()) : ""
                ));
                writer.newLine();
            }
            
            log.debug("Appended result for site: {}", result.getSiteId());
            
        } catch (IOException e) {
            log.error("Failed to append to result CSV: {}", filePath, e);
            throw new RuntimeException("CSV append failed", e);
        }
    }
    
    /**
     * ターゲットログと結果ログを比較
     */
    public ComparisonResult compareProcessingLogs(
            String targetCsvPath, 
            String resultCsvPath, 
            String comparisonCsvPath) {
        
        try {
            // Read target log
            Map<Integer, TargetLogEntry> targetMap = readTargetCsv(targetCsvPath);
            
            // Read result log
            Map<Integer, List<ProcessingResult>> resultMap = readResultCsv(resultCsvPath);
            
            // Compare
            List<ComparisonEntry> entries = new ArrayList<>();
            List<Integer> duplicateSites = new ArrayList<>();
            List<Integer> missingSites = new ArrayList<>();
            int successCount = 0;
            int warningCount = 0;
            int errorCount = 0;
            
            // Check each target site
            for (Integer siteId : targetMap.keySet()) {
                List<ProcessingResult> results = resultMap.get(siteId);
                
                if (results == null || results.isEmpty()) {
                    // Missing: Site was not processed
                    entries.add(ComparisonEntry.builder()
                        .siteId(siteId)
                        .targetStatus("PENDING")
                        .resultStatus("MISSING")
                        .matchStatus("MISMATCH")
                        .issueType("ERROR")
                        .details("Site was not processed")
                        .build());
                    missingSites.add(siteId);
                    errorCount++;
                    
                } else if (results.size() > 1) {
                    // Duplicate: Site was processed multiple times
                    entries.add(ComparisonEntry.builder()
                        .siteId(siteId)
                        .targetStatus("PENDING")
                        .resultStatus("DUPLICATE")
                        .matchStatus("DUPLICATE")
                        .issueType("ERROR")
                        .details(String.format("Processed %d times", results.size()))
                        .build());
                    duplicateSites.add(siteId);
                    errorCount++;
                    
                } else {
                    // Single result
                    ProcessingResult result = results.get(0);
                    String issueType = "";
                    String details = "";
                    
                    if ("SUCCESS".equals(result.getStatus())) {
                        successCount++;
                    } else if ("NO_DATA".equals(result.getStatus())) {
                        issueType = "WARNING";
                        details = "No monitoring data available";
                        warningCount++;
                    } else if ("FAILED".equals(result.getStatus())) {
                        issueType = "ERROR";
                        details = result.getErrorMessage();
                        errorCount++;
                    }
                    
                    entries.add(ComparisonEntry.builder()
                        .siteId(siteId)
                        .targetStatus("PENDING")
                        .resultStatus(result.getStatus())
                        .matchStatus("MATCH")
                        .issueType(issueType)
                        .details(details)
                        .build());
                }
            }
            
            // Write comparison CSV
            writeComparisonCsv(comparisonCsvPath, entries);
            
            return ComparisonResult.builder()
                .successCount(successCount)
                .warningCount(warningCount)
                .errorCount(errorCount)
                .duplicateSites(duplicateSites)
                .missingSites(missingSites)
                .entries(entries)
                .build();
            
        } catch (IOException e) {
            log.error("Failed to compare logs", e);
            throw new RuntimeException("Log comparison failed", e);
        }
    }
    
    /**
     * ターゲットCSV読み込み
     */
    private Map<Integer, TargetLogEntry> readTargetCsv(String filePath) throws IOException {
        Map<Integer, TargetLogEntry> map = new HashMap<>();
        
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line = reader.readLine(); // Skip header
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5) {
                    Integer siteId = Integer.parseInt(parts[2]);
                    // Store entry (we mainly just need the site ID list)
                    map.put(siteId, null);
                }
            }
        }
        
        return map;
    }
    
    /**
     * 結果CSV読み込み
     */
    private Map<Integer, List<ProcessingResult>> readResultCsv(String filePath) throws IOException {
        Map<Integer, List<ProcessingResult>> map = new HashMap<>();
        
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line = reader.readLine(); // Skip header
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1); // -1 keeps empty trailing fields
                if (parts.length >= 9) {
                    Integer siteId = Integer.parseInt(parts[2]);
                    String status = parts[5];
                    String errorMessage = parts.length > 8 ? parts[8] : "";
                    
                    ProcessingResult result = ProcessingResult.builder()
                        .siteId(siteId)
                        .status(status)
                        .errorMessage(errorMessage)
                        .build();
                    
                    map.computeIfAbsent(siteId, k -> new ArrayList<>()).add(result);
                }
            }
        }
        
        return map;
    }
    
    /**
     * 比較結果CSV書き込み
     */
    private void writeComparisonCsv(String filePath, List<ComparisonEntry> entries) throws IOException {
        ensureDirectoryExists(filePath);
        
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath))) {
            writer.write(COMPARISON_HEADER);
            writer.newLine();
            
            for (ComparisonEntry entry : entries) {
                writer.write(String.format("%d,%s,%s,%s,%s,%s",
                    entry.getSiteId(),
                    entry.getTargetStatus(),
                    entry.getResultStatus(),
                    entry.getMatchStatus(),
                    entry.getIssueType() != null ? entry.getIssueType() : "",
                    entry.getDetails() != null ? escapeCommas(entry.getDetails()) : ""
                ));
                writer.newLine();
            }
        }
        
        log.info("Comparison CSV created: {} ({} entries)", filePath, entries.size());
    }
    
    /**
     * ディレクトリ存在確認
     */
    private void ensureDirectoryExists(String filePath) throws IOException {
        Path path = Paths.get(filePath).getParent();
        if (path != null && !Files.exists(path)) {
            Files.createDirectories(path);
        }
    }
    
    /**
     * CSV用にカンマをエスケープ
     */
    private String escapeCommas(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}