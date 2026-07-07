package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.dto.request.CancelRequestDecisionRequest;
import com.kct.jcredit.core_backend.dto.request.CreateCancelRequestRequest;
import com.kct.jcredit.core_backend.dto.request.RegisterMonitoringRecordSolarRequest;
import com.kct.jcredit.core_backend.dto.response.CancelRequestResponse;
import com.kct.jcredit.core_backend.dto.response.FileUploadResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringRecordSolarListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringRecordSolarResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectResponse;
import com.kct.jcredit.core_backend.dto.response.CancelRequestDecisionResponse;
import com.kct.jcredit.core_backend.dto.response.CancelRequestListResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.service.MonitoringService;
import com.kct.jcredit.core_backend.service.CancelRequestService;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import com.kct.jcredit.core_backend.util.JwtUtil;

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
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/monitoring")
@Tag(name = "モニタリング", description = "モニタリング記録、証跡ファイル、取消申請を管理するAPIです。")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;
        private final CancelRequestService cancelRequestService;
    private final JwtUtil jwtUtil;

    /*CompanyId validated against token for security*/

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "モニタリング記録登録", description = "太陽光プロジェクトのモニタリング記録を登録します。")
    public ResponseEntity<MonitoringRecordSolarResponse> RegisterMonitoringRecordSolar(
            Authentication authentication,
            @RequestBody @Valid RegisterMonitoringRecordSolarRequest request) {
        String username = jwtUtil.extractUsername(authentication.getCredentials().toString());
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());
        
        System.err.println("DEBUG RegisterMonitoringRecordSolar called with companyId: " + companyId + ", request: " + request);
        
        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(username, companyId, request);
        
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/validate")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "モニタリング記録検証", description = "モニタリング記録リクエストを保存せずに検証します。")
    public ResponseEntity<BaseResponse> validateMonitoringRecordSolar(
            Authentication authentication,
            @RequestBody @Valid RegisterMonitoringRecordSolarRequest request) {
        String username = jwtUtil.extractUsername(authentication.getCredentials().toString());
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());

        BaseResponse response = monitoringService.validateMonitoringRecordSolar(username, companyId, request);

        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "最新モニタリング記録取得", description = "指定されたプロジェクトと拠点の最新モニタリング記録を取得します。")
    public ResponseEntity<MonitoringRecordSolarResponse> ReadLastMonitoringRecordSolar(
            Authentication authentication,
            @RequestParam Integer projectId,
            @RequestParam Integer siteId) {
        String username = jwtUtil.extractUsername(authentication.getCredentials().toString());
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());
        
        System.err.println("DEBUG ReadLastMonitoringRecordSolar called with companyId: " + companyId + ", projectId: " + projectId + ", siteId: " + siteId);
        
        MonitoringRecordSolarResponse response = monitoringService.readLastMonitoringRecordSolar(username, companyId, projectId, siteId);
        
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }



        /**
     * Get all monitoring records (paginated)
     */
    @GetMapping(value = "/list")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "モニタリング記録一覧取得", description = "指定されたプロジェクトと拠点のモニタリング記録一覧を取得します。")
    public ResponseEntity<MonitoringRecordSolarListResponse> listMonitoringRecords(
            @RequestParam Integer siteId,
            @RequestParam Integer projectId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer limit,
            @RequestParam(required = false) String nextPageToken,
            Authentication authentication
    ) {
        String username = jwtUtil.extractUsername(authentication.getCredentials().toString());
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());
         System.err.println("DEBUG listMonitoringRecords called with companyId: " + companyId + ", projectId: " + projectId + ", siteId: " + siteId);
        MonitoringRecordSolarListResponse response = monitoringService
            .readAllMonitoringRecordsSolar(
                username, 
                companyId, 
                projectId, 
                siteId, 
                page, 
                limit, 
                nextPageToken
            );
        
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }



    @PostMapping(value = "/upload-elpv-evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "ELPV証跡アップロード", description = "ELPVの証跡ファイルをアップロードします。")
    public ResponseEntity<FileUploadResponse> uploadElpvEvidence(
            Authentication authentication,
            @RequestParam Integer projectId,
            @RequestParam Integer siteId,
            @RequestParam(required = false) String description,
            @RequestParam MultipartFile file) {
        
        String username = jwtUtil.extractUsername(authentication.getCredentials().toString());
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());
        
        FileUploadResponse response = monitoringService.uploadElpvEvidence(
                username, companyId, projectId, siteId, description, file);
        
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping(value = "/upload-elpvr-evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "ELPVR証跡アップロード", description = "ELPVRの証跡ファイルをアップロードします。")
    public ResponseEntity<FileUploadResponse> uploadElpvrEvidence(
            Authentication authentication,
            @RequestParam Integer projectId,
            @RequestParam Integer siteId,
            @RequestParam(required = false) String description,
            @RequestParam MultipartFile file) {
        
        String username = jwtUtil.extractUsername(authentication.getCredentials().toString());
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());
        
        FileUploadResponse response = monitoringService.uploadElpvrEvidence(
                username, companyId, projectId, siteId, description, file);
        
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping(value = "/upload-elpj-battery-specification-evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "ELPJ蓄電池仕様証跡アップロード", description = "ELPJ蓄電池仕様の証跡ファイルをアップロードします。")
    public ResponseEntity<FileUploadResponse> uploadElpjBatterySpecificationEvidence(
            Authentication authentication,
            @RequestParam Integer projectId,
            @RequestParam Integer siteId,
            @RequestParam(required = false) String description,
            @RequestParam MultipartFile file) {
        
        String username = jwtUtil.extractUsername(authentication.getCredentials().toString());
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());
        
        FileUploadResponse response = monitoringService.uploadElpjBatterySpecificationEvidence(
                username, companyId, projectId, siteId, description, file);
        
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping(value = "/upload-elpj-control-evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "ELPJ制御証跡アップロード", description = "ELPJ制御の証跡ファイルをアップロードします。")
    public ResponseEntity<FileUploadResponse> uploadElpjControlEvidence(
            Authentication authentication,
            @RequestParam Integer projectId,
            @RequestParam Integer siteId,
            @RequestParam(required = false) String description,
            @RequestParam MultipartFile file) {
        
        String username = jwtUtil.extractUsername(authentication.getCredentials().toString());
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());
        
        FileUploadResponse response = monitoringService.uploadElpjControlEvidence(
                username, companyId, projectId, siteId, description, file);
        
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }
     // ==================== 取消申請関連 ====================
    
    /**
     * POST /api/monitoring/cancel-requests
     * Create cancel request for monitoring record
     */
    @PostMapping("/cancel-request")
    @PreAuthorize("hasAnyRole('master', 'user')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "取消申請作成", description = "モニタリングレコードの取消申請を作成")
    public ResponseEntity<CancelRequestResponse> createCancelRequest(
            Authentication authentication,
            @RequestBody @Valid CreateCancelRequestRequest request) {
        
        String username = jwtUtil.extractUsername(authentication.getCredentials().toString());
        String companyId = jwtUtil.extractCompanyId(authentication.getCredentials().toString());
        
        CancelRequestResponse response = cancelRequestService.createCancelRequest(
                username, companyId, request
        );
        
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    //get record cancel request list
    @GetMapping("/cancel-requests")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "取消申請一覧取得", description = "モニタリング記録の取消申請一覧を取得します。")
    public ResponseEntity<CancelRequestListResponse> getCancelRequests(Authentication authentication) {
        
        CancelRequestListResponse response = cancelRequestService.listCancelRequests();
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @PostMapping("/{cancelRequestId}/cancel-decision")
    @PreAuthorize("hasAnyRole('operator')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "取消申請を承認または却下", 
               description = "承認時は相殺レコードを自動作成し、NoSQLに登録します")
    public ResponseEntity<CancelRequestDecisionResponse> makeDecision(
            Authentication authentication,
            @PathVariable Integer cancelRequestId,
            @Valid @RequestBody CancelRequestDecisionRequest request) {
        //print out log
        System.err.println("DEBUG makeDecision called with cancelRequestId: " + cancelRequestId + ", request: " + request); 
        String operatorUsername = jwtUtil.extractUsername(authentication.getCredentials().toString());   
        CancelRequestDecisionResponse response = cancelRequestService.processDecision(
            cancelRequestId, 
            request,
            operatorUsername
        );
        
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    


}
