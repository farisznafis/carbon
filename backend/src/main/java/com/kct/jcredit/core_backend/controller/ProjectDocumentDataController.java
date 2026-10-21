package com.kct.jcredit.core_backend.controller;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse;
import com.kct.jcredit.core_backend.enums.ProjectFileType;
import com.kct.jcredit.core_backend.service.documentdata.ProjectDocumentDataService;
import com.kct.jcredit.core_backend.service.documentdata.ProjectDocumentDataService.Caller;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 書類データ取得: what the system holds for a project document, shown beside
 * the uploaded files so it can be checked and copied into the official Excel.
 *
 * <p>Read-only, and for the operating side only: an admin, or an operator
 * holding PROJECT_READ, of the company the project belongs to. The documents
 * built on monitoring data additionally require MONITORING_READ; that check
 * and the company check are the service's.
 */
@RestController
@RequestMapping("/project")
@Tag(name = "書類データ取得",
        description = "プロジェクト書類（計画書・モニタリング報告書・個別活動実績報告リスト）について、システムが保持しているデータを取得するAPIです。")
@RequiredArgsConstructor
public class ProjectDocumentDataController {

    private final ProjectDocumentDataService projectDocumentDataService;
    private final JwtUtil jwtUtil;

    @GetMapping("/{projectId}/document-data/{fileType}")
    @PreAuthorize("hasRole('admin') or (hasRole('operator') and hasAuthority(T(com.kct.jcredit.core_backend.constant.PermissionCode).PROJECT_READ.name()))")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "書類データ取得",
            description = "指定した書類種別について、システムが保持しているデータをセクション単位で取得します。"
                    + "ファイルの内容は読み取りません。MONITORING_REPORT と ACTIVITY_RESULT_LIST は、"
                    + "オペレーターの場合 MONITORING_READ 権限も必要です。"
                    + "データを保持していない書類種別は sections が空で返ります。")
    public ResponseEntity<ProjectDocumentDataResponse> getDocumentData(Authentication authentication,
            @PathVariable String projectId,
            @PathVariable ProjectFileType fileType) {
        String token = authentication.getCredentials().toString();
        Set<String> permissions = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        ProjectDocumentDataResponse response = projectDocumentDataService.getData(
                projectId,
                fileType,
                new Caller(
                        jwtUtil.extractCompanyId(token),
                        jwtUtil.extractRole(token),
                        jwtUtil.extractUsername(token),
                        permissions));
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }
}
