package com.kct.jcredit.core_backend.service.batch.impl;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.kct.jcredit.core_backend.dto.batch.ComparisonEntry;
import com.kct.jcredit.core_backend.dto.batch.ComparisonResult;
import com.kct.jcredit.core_backend.dto.batch.ProcessingResult;
import com.kct.jcredit.core_backend.dto.batch.TargetLogEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.kct.jcredit.core_backend.service.batch.CsvLogService;

/**
 * CsvLogServiceImpl bekerja langsung dengan filesystem (bukan mock repository), jadi kita
 * pakai @TempDir supaya setiap test punya direktori sendiri dan tidak saling mengotori,
 * serta tidak menyentuh /var/batch/logs/ yang asli.
 *
 * Skenario compareProcessingLogs() dirancang meniru 5 site dari data dummy J-Credit
 * (project 1, site 1-4) ditambah satu site "hilang" (site 5) untuk menguji semua cabang:
 *  - site 1 -> SUCCESS   -> MATCH
 *  - site 2 -> NO_DATA   -> MATCH tapi WARNING
 *  - site 3 -> FAILED    -> MATCH tapi ERROR
 *  - site 4 -> diproses 2x -> DUPLICATE -> ERROR
 *  - site 5 -> tidak diproses sama sekali -> MISSING -> ERROR
 */
class CsvLogServiceImplTest {

    private final CsvLogServiceImpl service = new CsvLogServiceImpl();

    @Test
    @DisplayName("createTargetCsv writes header and one row per site")
    void createTargetCsv_writesHeaderAndRows(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("target.csv");
        Instant expected = Instant.parse("2026-07-10T12:00:00Z");

        List<TargetLogEntry> entries = List.of(
            TargetLogEntry.builder().batchId(1).projectId(1).siteId(1).expectedStartTime(expected).status("PENDING").build(),
            TargetLogEntry.builder().batchId(1).projectId(1).siteId(2).expectedStartTime(expected).status("PENDING").build()
        );

        service.createTargetCsv(targetFile.toString(), entries);

        List<String> lines = Files.readAllLines(targetFile);
        assertThat(lines).hasSize(3); // header + 2 rows
        assertThat(lines.get(0)).isEqualTo("batch_id,project_id,site_id,expected_start_time,status");
        assertThat(lines.get(1)).contains("1,1,1,").contains("PENDING");
        assertThat(lines.get(2)).contains("1,1,2,").contains("PENDING");
    }

    @Test
    @DisplayName("createTargetCsv creates parent directories that do not yet exist")
    void createTargetCsv_createsParentDirectories(@TempDir Path tempDir) throws IOException {
        Path nested = tempDir.resolve("target/2026-07-10/report.csv");

        service.createTargetCsv(nested.toString(), List.of());

        assertThat(Files.exists(nested)).isTrue();
        assertThat(Files.exists(nested.getParent())).isTrue();
    }

    @Test
    @DisplayName("createEmptyResultCsv writes only the header row")
    void createEmptyResultCsv_writesOnlyHeader(@TempDir Path tempDir) throws IOException {
        Path resultFile = tempDir.resolve("result.csv");

        service.createEmptyResultCsv(resultFile.toString());

        List<String> lines = Files.readAllLines(resultFile);
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0)).isEqualTo(
            "batch_id,project_id,site_id,start_time,end_time,status,records_processed,monitoring_item_id,error_message");
    }

    @Test
    @DisplayName("appendToResultCsv appends rows without touching the header, and escapes commas in error messages")
    void appendToResultCsv_appendsRowsAndEscapesCommas(@TempDir Path tempDir) throws IOException {
        Path resultFile = tempDir.resolve("result.csv");
        service.createEmptyResultCsv(resultFile.toString());

        ProcessingResult success = ProcessingResult.builder()
            .batchId(1).projectId(1).siteId(1)
            .startTime(Instant.parse("2026-07-10T12:00:00Z"))
            .endTime(Instant.parse("2026-07-10T12:00:05Z"))
            .status("SUCCESS")
            .recordsProcessed(5)
            .build();
        ProcessingResult failed = ProcessingResult.builder()
            .batchId(1).projectId(1).siteId(3)
            .status("FAILED")
            .errorMessage("timeout, retrying, gave up")
            .build();

        service.appendToResultCsv(resultFile.toString(), success);
        service.appendToResultCsv(resultFile.toString(), failed);

        List<String> lines = Files.readAllLines(resultFile);
        assertThat(lines).hasSize(3); // header + 2 rows
        assertThat(lines.get(1)).contains("1,1,1,").contains("SUCCESS").contains(",5,");
        // Pesan error yang mengandung koma harus dibungkus tanda kutip agar tidak merusak kolom CSV.
        assertThat(lines.get(2)).contains("\"timeout, retrying, gave up\"");
    }

    @Test
    @DisplayName("compareProcessingLogs correctly classifies MATCH/WARNING/ERROR/DUPLICATE/MISSING")
    void compareProcessingLogs_classifiesAllBranches(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("target.csv");
        Path resultFile = tempDir.resolve("result.csv");
        Path comparisonFile = tempDir.resolve("comparison.csv");
        Instant now = Instant.parse("2026-07-10T12:00:00Z");

        // Target: 5 site diharapkan diproses (site 1-5), meniru project 1 pada data dummy
        // ditambah satu site tambahan (5) yang sengaja tidak akan ada hasilnya.
        List<TargetLogEntry> targets = List.of(1, 2, 3, 4, 5).stream()
            .map(siteId -> TargetLogEntry.builder()
                .batchId(1).projectId(1).siteId(siteId).expectedStartTime(now).status("PENDING").build())
            .toList();
        service.createTargetCsv(targetFile.toString(), targets);
        service.createEmptyResultCsv(resultFile.toString());

        service.appendToResultCsv(resultFile.toString(),
            ProcessingResult.builder().batchId(1).projectId(1).siteId(1).status("SUCCESS").recordsProcessed(5).build());
        service.appendToResultCsv(resultFile.toString(),
            ProcessingResult.builder().batchId(1).projectId(1).siteId(2).status("NO_DATA").build());
        service.appendToResultCsv(resultFile.toString(),
            ProcessingResult.builder().batchId(1).projectId(1).siteId(3).status("FAILED").errorMessage("calc error").build());
        // Site 4 diproses dua kali -> duplicate
        service.appendToResultCsv(resultFile.toString(),
            ProcessingResult.builder().batchId(1).projectId(1).siteId(4).status("SUCCESS").recordsProcessed(5).build());
        service.appendToResultCsv(resultFile.toString(),
            ProcessingResult.builder().batchId(1).projectId(1).siteId(4).status("SUCCESS").recordsProcessed(5).build());
        // Site 5 sengaja tidak pernah di-append -> missing

        ComparisonResult result = service.compareProcessingLogs(
            targetFile.toString(), resultFile.toString(), comparisonFile.toString());

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getWarningCount()).isEqualTo(1);
        // errorCount = 1 (FAILED site 3) + 1 (DUPLICATE site 4) + 1 (MISSING site 5) = 3
        assertThat(result.getErrorCount()).isEqualTo(3);
        assertThat(result.getDuplicateSites()).containsExactly(4);
        assertThat(result.getMissingSites()).containsExactly(5);
        assertThat(result.getEntries()).hasSize(5);
        assertThat(Files.exists(comparisonFile)).isTrue();

        List<String> comparisonLines = Files.readAllLines(comparisonFile);
        assertThat(comparisonLines.get(0)).isEqualTo(
            "site_id,target_status,result_status,match_status,issue_type,details");
        assertThat(comparisonLines).hasSize(6);
    }

    @Test
    @DisplayName("compareProcessingLogs with a fully clean run reports zero warnings and errors")
    void compareProcessingLogs_allSuccess_noIssues(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("target.csv");
        Path resultFile = tempDir.resolve("result.csv");
        Path comparisonFile = tempDir.resolve("comparison.csv");
        Instant now = Instant.parse("2026-07-10T12:00:00Z");

        List<TargetLogEntry> targets = List.of(1, 2).stream()
            .map(siteId -> TargetLogEntry.builder()
                .batchId(1).projectId(1).siteId(siteId).expectedStartTime(now).status("PENDING").build())
            .toList();
        service.createTargetCsv(targetFile.toString(), targets);
        service.createEmptyResultCsv(resultFile.toString());
        service.appendToResultCsv(resultFile.toString(),
            ProcessingResult.builder().batchId(1).projectId(1).siteId(1).status("SUCCESS").recordsProcessed(5).build());
        service.appendToResultCsv(resultFile.toString(),
            ProcessingResult.builder().batchId(1).projectId(1).siteId(2).status("SUCCESS").recordsProcessed(5).build());

        ComparisonResult result = service.compareProcessingLogs(
            targetFile.toString(), resultFile.toString(), comparisonFile.toString());

        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getWarningCount()).isZero();
        assertThat(result.getErrorCount()).isZero();
        assertThat(result.getDuplicateSites()).isEmpty();
        assertThat(result.getMissingSites()).isEmpty();
    }
}