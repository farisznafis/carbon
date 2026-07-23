package com.kct.jcredit.core_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.kct.jcredit.core_backend.dto.response.MonitoringRecordSolarResponse;
import com.kct.jcredit.core_backend.dto.response.SiteReductionResultResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectReductionResultResponse;
import com.kct.jcredit.core_backend.dto.response.BatchProcessHistoryResponse;
import com.kct.jcredit.core_backend.service.batch.GetBatchService;


@RestController
@RequestMapping("/batch")
@Tag(name = "バッチ処理", description = "バッチ処理関連のAPIです。")
@RequiredArgsConstructor
public class BatchProcessController {

    private final GetBatchService batchService;
    private final com.kct.jcredit.core_backend.repository.postgres.ProjectRepository projectRepository;

    @GetMapping("/history")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "バッチ履歴", description = "バッチ履歴を取得します。")
    public ResponseEntity<BatchProcessHistoryResponse> BatchProcessHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String nextPageToken) {
        
        BatchProcessHistoryResponse response = batchService.getBatchProcessHistory(page, limit, nextPageToken);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{siteId}/{projectId}/reduction-result")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "プロジェクト貢献データ", description = "プロジェクト詳細のグラフ為のデータ取得。")
    public ResponseEntity<SiteReductionResultResponse> SiteReductionResult(
            @PathVariable String siteId,
            @PathVariable String projectId) {
        
        
        Integer projDbId = projectRepository.findByPublicId(projectId)
            .map(com.kct.jcredit.core_backend.entity.Project::getId)
            .orElseThrow(() -> new RuntimeException("Project Not Found"));
        SiteReductionResultResponse response = batchService.getSiteReductionResult(siteId, projDbId);
    
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}/reduction-result-total")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "プロジェクト排出量合計", description = "プロジェクト詳細のグラフ為のデータ取得。")
    public ResponseEntity<ProjectReductionResultResponse> ProjectReductionResult(
            @PathVariable String projectId) {
        
        
        Integer projDbId = projectRepository.findByPublicId(projectId)
            .map(com.kct.jcredit.core_backend.entity.Project::getId)
            .orElseThrow(() -> new RuntimeException("Project Not Found"));
        ProjectReductionResultResponse response = batchService.getProjectReductionResult(projDbId);
    
        return ResponseEntity.ok(response);
    }


}
