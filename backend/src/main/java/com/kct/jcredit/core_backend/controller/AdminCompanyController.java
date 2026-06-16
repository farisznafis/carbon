package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.request.RejectCompanyRequest;
import com.kct.jcredit.core_backend.dto.response.ApproveCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.PendingCompanyListResponse;
import com.kct.jcredit.core_backend.dto.response.RejectCompanyResponse;
import com.kct.jcredit.core_backend.service.CompanyApprovalService;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/companies")
@RequiredArgsConstructor
public class AdminCompanyController {

    private final CompanyApprovalService companyApprovalService;

    @PostMapping("/{companyId}/approve")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApproveCompanyResponse> approveCompany(@PathVariable String companyId) {
        ApproveCompanyResponse response = companyApprovalService.approveCompany(companyId);
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/{companyId}/reject")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<RejectCompanyResponse> rejectCompany(
            @PathVariable String companyId,
            @RequestBody @Valid RejectCompanyRequest request) {
        RejectCompanyResponse response = companyApprovalService.rejectCompany(companyId, request.getRejectionReason());
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PendingCompanyListResponse> getPendingCompanies() {
        PendingCompanyListResponse response = companyApprovalService.getPendingCompanies();
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }
}
