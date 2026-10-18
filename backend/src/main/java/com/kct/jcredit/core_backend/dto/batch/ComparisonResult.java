package com.kct.jcredit.core_backend.dto.batch;

import lombok.Builder;
import lombok.Data;
import java.util.Collections;
import java.util.List;

@Data
@Builder
public class ComparisonResult {
    private Integer successCount;
    private Integer warningCount;
    private Integer errorCount;
    private List<Integer> duplicateSites;
    private List<Integer> missingSites;
    private List<ComparisonEntry> entries;
    
    // ========================================
    // 🆕 ファクトリーメソッド（テスト用）
    // ========================================
    
    /**
     * 成功結果を作成（テスト用）
     */
    public static ComparisonResult success(int successCount, int warningCount, int errorCount) {
        return ComparisonResult.builder()
            .successCount(successCount)
            .warningCount(warningCount)
            .errorCount(errorCount)
            .duplicateSites(Collections.emptyList())
            .missingSites(Collections.emptyList())
            .entries(Collections.emptyList())
            .build();
    }
    
    /**
     * 完全成功（警告・エラーなし）
     */
    public static ComparisonResult allSuccess(int successCount) {
        return success(successCount, 0, 0);
    }
    
    /**
     * 重複検出ありの結果を作成
     */
    public static ComparisonResult withDuplicates(int successCount, int warningCount, int errorCount) {
        return ComparisonResult.builder()
            .successCount(successCount)
            .warningCount(warningCount)
            .errorCount(errorCount)
            .duplicateSites(List.of(999)) // ダミーの重複サイトID
            .missingSites(Collections.emptyList())
            .entries(Collections.emptyList())
            .build();
    }
    
    // ========================================
    // 既存のメソッド
    // ========================================
    
    public boolean hasErrors() {
        return errorCount > 0;
    }
    
    public boolean hasWarnings() {
        return warningCount > 0;
    }
    
    public boolean hasDuplicates() {
        return duplicateSites != null && !duplicateSites.isEmpty();
    }
}