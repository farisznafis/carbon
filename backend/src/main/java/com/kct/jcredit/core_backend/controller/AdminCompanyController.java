package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.request.CompanyListQuery;
import com.kct.jcredit.core_backend.dto.request.CreateCompanySuspensionRequest;
import com.kct.jcredit.core_backend.dto.request.RejectCompanyRequest;
import com.kct.jcredit.core_backend.dto.request.ResolveCompanySuspensionRequest;
import com.kct.jcredit.core_backend.dto.response.AdminCompanyListResponse;
import com.kct.jcredit.core_backend.dto.response.ApproveCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.CompanySuspensionListResponse;
import com.kct.jcredit.core_backend.dto.response.CompanySuspensionResponse;
import com.kct.jcredit.core_backend.dto.response.RejectCompanyResponse;
import com.kct.jcredit.core_backend.service.AdminCompanyQueryService;
import com.kct.jcredit.core_backend.service.CompanyApprovalService;
import com.kct.jcredit.core_backend.service.CompanyLifecycleService;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes operator-only company lifecycle and search operations.
 */
@RestController
@RequestMapping("/admin/companies")
@Tag(name = "企業管理", description = "企業の承認、却下、一覧取得、利用停止を管理するAPIです。")
@RequiredArgsConstructor
@Validated
public class AdminCompanyController {

    private final CompanyApprovalService companyApprovalService;
    private final AdminCompanyQueryService adminCompanyQueryService;
    private final CompanyLifecycleService companyLifecycleService;

    /**
     * Approves a pending company and provisions its master account.
     */
    @PostMapping("/{companyId}/approve")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "企業登録承認", description = "申請中の企業を承認し、マスターユーザーを作成します。")
    public ResponseEntity<ApproveCompanyResponse> approveCompany(@PathVariable String companyId) {
        ApproveCompanyResponse response = companyApprovalService.approveCompany(companyId);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    /**
     * Rejects a pending company registration with an auditable explanation.
     */
    @PostMapping("/{companyId}/reject")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "企業登録却下", description = "申請中の企業登録を却下します。")
    public ResponseEntity<RejectCompanyResponse> rejectCompany(
            @PathVariable String companyId,
            @RequestBody @Valid RejectCompanyRequest request) {
        RejectCompanyResponse response = companyApprovalService.rejectCompany(
                companyId,
                request.getRejectionReason()
        );
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    /**
     * Returns a unified, filtered, and paginated company list.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "企業一覧取得", description = "企業ステータス、検索条件、日付範囲で企業一覧を取得します。")
    public ResponseEntity<AdminCompanyListResponse> getCompanies(
            @Valid @ModelAttribute CompanyListQuery query) {
        AdminCompanyListResponse response = adminCompanyQueryService.findCompanies(query);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    /**
     * Creates one operational blocker for an active or already suspended company.
     */
    @PostMapping("/{companyId}/suspensions")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "企業利用停止登録", description = "指定した理由で企業の利用停止を登録します。")
    public ResponseEntity<CompanySuspensionResponse> createSuspension(
            @PathVariable String companyId,
            @Valid @RequestBody CreateCompanySuspensionRequest request) {
        CompanySuspensionResponse response = companyLifecycleService.createSuspension(companyId, request);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    /**
     * Returns active blockers or complete suspension history for a company.
     */
    @GetMapping("/{companyId}/suspensions")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "企業停止履歴取得", description = "企業の有効な停止理由または停止履歴を取得します。")
    public ResponseEntity<CompanySuspensionListResponse> getSuspensions(
            @PathVariable String companyId,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "ページ番号は1以上で指定してください。") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "表示件数は1以上で指定してください。")
            @Max(value = 100, message = "表示件数は100以下で指定してください。") int limit) {
        CompanySuspensionListResponse response = companyLifecycleService.findSuspensions(
                companyId,
                activeOnly,
                page,
                limit
        );
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    /**
     * Resolves one blocker and conditionally reactivates the company.
     */
    @PostMapping("/{companyId}/suspensions/{suspensionId}/resolve")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "企業停止解除", description = "指定した停止理由を解除し、残りの停止理由を確認します。")
    public ResponseEntity<CompanySuspensionResponse> resolveSuspension(
            @PathVariable String companyId,
            @PathVariable Integer suspensionId,
            @Valid @RequestBody ResolveCompanySuspensionRequest request) {
        CompanySuspensionResponse response = companyLifecycleService.resolveSuspension(
                companyId,
                suspensionId,
                request
        );
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }
}
