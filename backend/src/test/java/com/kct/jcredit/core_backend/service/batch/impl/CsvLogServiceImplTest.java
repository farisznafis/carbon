package com.kct.jcredit.core_backend.service.batch.impl;

import com.kct.jcredit.core_backend.dto.batch.ComparisonEntry;
import com.kct.jcredit.core_backend.dto.batch.ComparisonResult;
import com.kct.jcredit.core_backend.dto.batch.ProcessingResult;
import com.kct.jcredit.core_backend.dto.batch.TargetLogEntry;
import com.kct.jcredit.core_backend.service.batch.CsvLogService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CsvLogServiceImpl は外部依存を持たないためモックは使わず、
 * @TempDir による実ファイル I/O で検証する。
 */
@DisplayName("CsvLogServiceImpl のテスト")
class CsvLogServiceImplTest {

    private final CsvLogServiceImpl service = new CsvLogServiceImpl();

    private static final String TARGET_HEADER =
            "batch_id,project_id,site_id,expected_start_time,status";
    private static final String RESULT_HEADER =
            "batch_id,project_id,site_id,start_time,end_time,status,records_processed,monitoring_item_id,error_message";
    private static final String COMPARISON_HEADER =
            "site_id,target_status,result_status,match_status,issue_type,details";

    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");

    private List<TargetLogEntry> targetsFor(Integer... siteIds) {
        return Stream.of(siteIds)
                .map(siteId -> TargetLogEntry.builder()
                        .batchId(1).projectId(1).siteId(siteId)
                        .expectedStartTime(NOW).status("PENDING").build())
                .collect(Collectors.toList());
    }

    private ProcessingResult resultOf(Integer siteId, String status) {
        return ProcessingResult.builder()
                .batchId(1).projectId(1).siteId(siteId)
                .status(status).recordsProcessed(5).build();
    }

    // =========================================================================
    @Nested
    @DisplayName("createTargetCsv のテスト")
    class CreateTargetCsvTest {

        @Test
        @DisplayName("UT-CL-201: ヘッダー行 + サイト件数分のデータ行が書き込まれる")
        void createTargetCsv_writesHeaderAndOneRowPerSite(@TempDir Path tempDir) throws IOException {
            Path targetFile = tempDir.resolve("target.csv");

            service.createTargetCsv(targetFile.toString(), targetsFor(1, 2));

            List<String> lines = Files.readAllLines(targetFile);
            assertThat(lines).hasSize(3);
            assertThat(lines.get(0)).isEqualTo(TARGET_HEADER);
            assertThat(lines.get(1)).startsWith("1,1,1,").endsWith("PENDING");
            assertThat(lines.get(2)).startsWith("1,1,2,").endsWith("PENDING");
        }

        @Test
        @DisplayName("UT-CL-202: entries が空リストの場合、ヘッダーのみが書き込まれる")
        void createTargetCsv_emptyEntries_writesHeaderOnly(@TempDir Path tempDir) throws IOException {
            Path targetFile = tempDir.resolve("target.csv");

            service.createTargetCsv(targetFile.toString(), List.of());

            List<String> lines = Files.readAllLines(targetFile);
            assertThat(lines).hasSize(1);
            assertThat(lines.get(0)).isEqualTo(TARGET_HEADER);
        }

        @Test
        @DisplayName("UT-CL-203: 親ディレクトリが存在しない場合、自動生成される")
        void createTargetCsv_createsMissingParentDirectories(@TempDir Path tempDir) {
            Path nested = tempDir.resolve("target/2026-07-10/report.csv");

            service.createTargetCsv(nested.toString(), targetsFor(1));

            assertThat(Files.exists(nested)).isTrue();
            assertThat(Files.exists(nested.getParent())).isTrue();
        }

        @Test
        @DisplayName("UT-CL-204: ディレクトリ生成に失敗する場合、RuntimeException にラップされる")
        void createTargetCsv_ioFailure_wrappedInRuntimeException(@TempDir Path tempDir) throws IOException {
            // Membuat FILE bernama "blocker", lalu meminta path yang memperlakukannya
            // sebagai DIRECTORY → Files.createDirectories gagal dengan IOException.
            Path blocker = tempDir.resolve("blocker");
            Files.createFile(blocker);
            Path impossible = blocker.resolve("sub/target.csv");

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.createTargetCsv(impossible.toString(), targetsFor(1)));

            assertThat(thrown.getMessage()).isEqualTo("CSV creation failed");
            assertThat(thrown.getCause()).isInstanceOf(IOException.class);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("createEmptyResultCsv のテスト")
    class CreateEmptyResultCsvTest {

        @Test
        @DisplayName("UT-CL-205: ヘッダー行のみのファイルが生成される（9カラム）")
        void createEmptyResultCsv_writesOnlyHeader(@TempDir Path tempDir) throws IOException {
            Path resultFile = tempDir.resolve("result.csv");

            service.createEmptyResultCsv(resultFile.toString());

            List<String> lines = Files.readAllLines(resultFile);
            assertThat(lines).hasSize(1);
            assertThat(lines.get(0)).isEqualTo(RESULT_HEADER);
            // readResultCsv() mensyaratkan parts.length >= 9, jadi jumlah kolom harus tetap 9
            assertThat(lines.get(0).split(",", -1)).hasSize(9);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("appendToResultCsv のテスト")
    class AppendToResultCsvTest {

        @Test
        @DisplayName("UT-CL-206: ヘッダーを壊さずに行が追記される")
        void appendToResultCsv_appendsWithoutTouchingHeader(@TempDir Path tempDir) throws IOException {
            Path resultFile = tempDir.resolve("result.csv");
            service.createEmptyResultCsv(resultFile.toString());

            service.appendToResultCsv(resultFile.toString(), resultOf(1, "SUCCESS"));
            service.appendToResultCsv(resultFile.toString(), resultOf(2, "SUCCESS"));

            List<String> lines = Files.readAllLines(resultFile);
            assertThat(lines).hasSize(3);
            assertThat(lines.get(0)).isEqualTo(RESULT_HEADER); // header tidak berubah
            assertThat(lines.get(1)).startsWith("1,1,1,");
            assertThat(lines.get(2)).startsWith("1,1,2,");
        }

        @Test
        @DisplayName("UT-CL-207: null フィールドが空文字として出力され、カラム数が維持される")
        void appendToResultCsv_nullFields_keepColumnCount(@TempDir Path tempDir) throws IOException {
            Path resultFile = tempDir.resolve("result.csv");
            service.createEmptyResultCsv(resultFile.toString());

            ProcessingResult sparse = ProcessingResult.builder()
                    .batchId(1).projectId(1).siteId(1)
                    .status("NO_DATA")
                    .startTime(null).endTime(null)
                    .recordsProcessed(null).monitoringItemId(null).errorMessage(null)
                    .build();

            service.appendToResultCsv(resultFile.toString(), sparse);

            List<String> lines = Files.readAllLines(resultFile);
            // split dengan limit -1 agar kolom kosong di akhir tidak dibuang
            String[] parts = lines.get(1).split(",", -1);
            assertThat(parts).hasSize(9);
            assertThat(parts[3]).isEmpty(); // startTime
            assertThat(parts[4]).isEmpty(); // endTime
            assertThat(parts[5]).isEqualTo("NO_DATA");
            assertThat(parts[6]).isEmpty(); // recordsProcessed
            assertThat(parts[8]).isEmpty(); // errorMessage
        }

        @Test
        @DisplayName("UT-CL-208: カンマを含むエラーメッセージがダブルクォートでエスケープされる")
        void appendToResultCsv_errorMessageWithCommas_isQuoted(@TempDir Path tempDir) throws IOException {
            Path resultFile = tempDir.resolve("result.csv");
            service.createEmptyResultCsv(resultFile.toString());

            ProcessingResult failed = ProcessingResult.builder()
                    .batchId(1).projectId(1).siteId(3)
                    .status("FAILED")
                    .errorMessage("timeout, retrying, gave up")
                    .build();

            service.appendToResultCsv(resultFile.toString(), failed);

            List<String> lines = Files.readAllLines(resultFile);
            assertThat(lines.get(1)).contains("\"timeout, retrying, gave up\"");
        }

        @Test
        @DisplayName("UT-CL-209: 対象ファイルが存在しない場合、RuntimeException にラップされる")
        void appendToResultCsv_fileMissing_wrappedInRuntimeException(@TempDir Path tempDir) {
            // APPEND dipakai tanpa opsi CREATE → file harus sudah ada lebih dulu
            Path missing = tempDir.resolve("never-created.csv");

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.appendToResultCsv(missing.toString(), resultOf(1, "SUCCESS")));

            assertThat(thrown.getMessage()).isEqualTo("CSV append failed");
            assertThat(thrown.getCause()).isInstanceOf(IOException.class);
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("compareProcessingLogs のテスト")
    class CompareProcessingLogsTest {

        private Path targetFile;
        private Path resultFile;
        private Path comparisonFile;

        private void setUpFiles(Path tempDir, List<TargetLogEntry> targets) {
            targetFile = tempDir.resolve("target.csv");
            resultFile = tempDir.resolve("result.csv");
            comparisonFile = tempDir.resolve("comparison.csv");
            service.createTargetCsv(targetFile.toString(), targets);
            service.createEmptyResultCsv(resultFile.toString());
        }

        private ComparisonResult compare() {
            return service.compareProcessingLogs(
                    targetFile.toString(), resultFile.toString(), comparisonFile.toString());
        }

        @Test
        @DisplayName("UT-CL-210: 全サイト成功の場合、warning/error が 0 件")
        void compareProcessingLogs_allSuccess_noIssues(@TempDir Path tempDir) {
            setUpFiles(tempDir, targetsFor(1, 2));
            service.appendToResultCsv(resultFile.toString(), resultOf(1, "SUCCESS"));
            service.appendToResultCsv(resultFile.toString(), resultOf(2, "SUCCESS"));

            ComparisonResult result = compare();

            assertThat(result.getSuccessCount()).isEqualTo(2);
            assertThat(result.getWarningCount()).isZero();
            assertThat(result.getErrorCount()).isZero();
            assertThat(result.getDuplicateSites()).isEmpty();
            assertThat(result.getMissingSites()).isEmpty();
        }

        @Test
        @DisplayName("UT-CL-211: NO_DATA ステータスは warning として集計される")
        void compareProcessingLogs_noData_countedAsWarning(@TempDir Path tempDir) {
            setUpFiles(tempDir, targetsFor(2));
            service.appendToResultCsv(resultFile.toString(), resultOf(2, "NO_DATA"));

            ComparisonResult result = compare();

            assertThat(result.getWarningCount()).isEqualTo(1);
            assertThat(result.getSuccessCount()).isZero();
            assertThat(result.getErrorCount()).isZero();

            ComparisonEntry entry = result.getEntries().get(0);
            assertThat(entry.getIssueType()).isEqualTo("WARNING");
            assertThat(entry.getDetails()).isEqualTo("No monitoring data available");
            assertThat(entry.getMatchStatus()).isEqualTo("MATCH");
        }

        @Test
        @DisplayName("UT-CL-212: FAILED ステータスは error として集計され、errorMessage が details に入る")
        void compareProcessingLogs_failed_countedAsErrorWithMessage(@TempDir Path tempDir) {
            setUpFiles(tempDir, targetsFor(3));
            service.appendToResultCsv(resultFile.toString(), ProcessingResult.builder()
                    .batchId(1).projectId(1).siteId(3)
                    .status("FAILED").errorMessage("calc error").build());

            ComparisonResult result = compare();

            assertThat(result.getErrorCount()).isEqualTo(1);
            ComparisonEntry entry = result.getEntries().get(0);
            assertThat(entry.getIssueType()).isEqualTo("ERROR");
            assertThat(entry.getDetails()).isEqualTo("calc error");
        }

        @Test
        @DisplayName("UT-CL-213: 同一サイトが複数回処理された場合、DUPLICATE として検出される")
        void compareProcessingLogs_duplicateSite_detected(@TempDir Path tempDir) {
            setUpFiles(tempDir, targetsFor(4));
            service.appendToResultCsv(resultFile.toString(), resultOf(4, "SUCCESS"));
            service.appendToResultCsv(resultFile.toString(), resultOf(4, "SUCCESS"));

            ComparisonResult result = compare();

            assertThat(result.getDuplicateSites()).containsExactly(4);
            assertThat(result.getErrorCount()).isEqualTo(1);

            ComparisonEntry entry = result.getEntries().get(0);
            assertThat(entry.getMatchStatus()).isEqualTo("DUPLICATE");
            assertThat(entry.getResultStatus()).isEqualTo("DUPLICATE");
            assertThat(entry.getDetails()).isEqualTo("Processed 2 times");
        }

        @Test
        @DisplayName("UT-CL-214: target にあるが result にないサイトは MISSING として検出される")
        void compareProcessingLogs_missingSite_detected(@TempDir Path tempDir) {
            setUpFiles(tempDir, targetsFor(1, 5));
            service.appendToResultCsv(resultFile.toString(), resultOf(1, "SUCCESS"));
            // site 5 sengaja tidak pernah di-append

            ComparisonResult result = compare();

            assertThat(result.getMissingSites()).containsExactly(5);
            assertThat(result.getErrorCount()).isEqualTo(1);

            ComparisonEntry missing = result.getEntries().stream()
                    .filter(e -> e.getSiteId().equals(5)).findFirst().orElseThrow();
            assertThat(missing.getResultStatus()).isEqualTo("MISSING");
            assertThat(missing.getDetails()).isEqualTo("Site was not processed");
        }

        @Test
        @DisplayName("UT-CL-215: result にあるが target にないサイトは検出されない（欠陥候補）")
        void compareProcessingLogs_unexpectedSiteInResult_isIgnored(@TempDir Path tempDir) {
            setUpFiles(tempDir, targetsFor(1, 2));
            service.appendToResultCsv(resultFile.toString(), resultOf(1, "SUCCESS"));
            service.appendToResultCsv(resultFile.toString(), resultOf(2, "SUCCESS"));
            service.appendToResultCsv(resultFile.toString(), resultOf(99, "SUCCESS")); // ← tidak ada di target

            ComparisonResult result = compare();

            // Perilaku SAAT INI: site 99 diabaikan total karena loop hanya menelusuri targetMap.keySet()
            assertThat(result.getEntries()).hasSize(2);
            assertThat(result.getEntries()).extracting(ComparisonEntry::getSiteId)
                    .containsExactlyInAnyOrder(1, 2);
            assertThat(result.getSuccessCount()).isEqualTo(2);

            // GAP: idealnya site tak terduga terdeteksi sebagai anomali.
            // Aktifkan assertion berikut setelah perbaikan diterapkan:
            // assertThat(result.getEntries()).extracting(ComparisonEntry::getSiteId).contains(99);
        }

        @Test
        @DisplayName("UT-CL-216: SUCCESS/NO_DATA/FAILED 以外のステータスはどのカウンタにも計上されない（欠陥候補）")
        void compareProcessingLogs_unknownStatus_notCountedAnywhere(@TempDir Path tempDir) {
            setUpFiles(tempDir, targetsFor(1));
            service.appendToResultCsv(resultFile.toString(), resultOf(1, "SKIPPED"));

            ComparisonResult result = compare();

            // Perilaku SAAT INI: if / else-if tanpa else → tidak terhitung di manapun
            assertThat(result.getSuccessCount()).isZero();
            assertThat(result.getWarningCount()).isZero();
            assertThat(result.getErrorCount()).isZero();

            // Namun entri tetap ada → total counter (0) ≠ jumlah entri (1)
            assertThat(result.getEntries()).hasSize(1);
            int totalCounted = result.getSuccessCount() + result.getWarningCount() + result.getErrorCount();
            assertThat(totalCounted).isNotEqualTo(result.getEntries().size());
        }

        @Test
        @DisplayName("UT-CL-217: カンマ入りエラーメッセージの往復で details が破損する（欠陥候補）")
        void compareProcessingLogs_commaInErrorMessage_detailsCorruptedOnRoundTrip(@TempDir Path tempDir) {
            String originalMessage = "timeout, retrying, gave up";
            setUpFiles(tempDir, targetsFor(3));
            service.appendToResultCsv(resultFile.toString(), ProcessingResult.builder()
                    .batchId(1).projectId(1).siteId(3)
                    .status("FAILED").errorMessage(originalMessage).build());

            ComparisonResult result = compare();

            ComparisonEntry entry = result.getEntries().get(0);
            // Penulisan memakai escapeCommas (quoted), tetapi pembacaan memakai
            // split(",", -1) polos yang tidak mengerti quoted field → pesan terpotong.
            assertThat(entry.getDetails()).isNotEqualTo(originalMessage);

            // Setelah diperbaiki dengan CSV parser sungguhan, assertion di atas diganti dengan:
            // assertThat(entry.getDetails()).isEqualTo(originalMessage);
        }

        @Test
        @DisplayName("UT-CL-218: target CSV が存在しない場合、RuntimeException にラップされる")
        void compareProcessingLogs_targetFileMissing_wrappedInRuntimeException(@TempDir Path tempDir) {
            Path missingTarget = tempDir.resolve("no-target.csv");
            Path result = tempDir.resolve("result.csv");
            Path comparison = tempDir.resolve("comparison.csv");
            service.createEmptyResultCsv(result.toString());

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.compareProcessingLogs(
                            missingTarget.toString(), result.toString(), comparison.toString()));

            assertThat(thrown.getMessage()).isEqualTo("Log comparison failed");
            assertThat(thrown.getCause()).isInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("UT-CL-210b: comparison CSV がヘッダー + エントリ件数分書き出される")
        void compareProcessingLogs_writesComparisonCsvFile(@TempDir Path tempDir) throws IOException {
            setUpFiles(tempDir, targetsFor(1, 2, 3, 4, 5));
            service.appendToResultCsv(resultFile.toString(), resultOf(1, "SUCCESS"));
            service.appendToResultCsv(resultFile.toString(), resultOf(2, "NO_DATA"));
            service.appendToResultCsv(resultFile.toString(), ProcessingResult.builder()
                    .batchId(1).projectId(1).siteId(3)
                    .status("FAILED").errorMessage("calc error").build());
            service.appendToResultCsv(resultFile.toString(), resultOf(4, "SUCCESS"));
            service.appendToResultCsv(resultFile.toString(), resultOf(4, "SUCCESS"));
            // site 5 → missing

            ComparisonResult result = compare();

            assertThat(result.getSuccessCount()).isEqualTo(1);
            assertThat(result.getWarningCount()).isEqualTo(1);
            // 1 (FAILED) + 1 (DUPLICATE) + 1 (MISSING)
            assertThat(result.getErrorCount()).isEqualTo(3);
            assertThat(result.getEntries()).hasSize(5);

            assertThat(Files.exists(comparisonFile)).isTrue();
            List<String> lines = Files.readAllLines(comparisonFile);
            assertThat(lines.get(0)).isEqualTo(COMPARISON_HEADER);
            assertThat(lines).hasSize(6); // header + 5 entri
        }
    }
}