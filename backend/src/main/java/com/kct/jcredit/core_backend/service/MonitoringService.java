package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.RegisterMonitoringRecordSolarRequest;
import com.kct.jcredit.core_backend.dto.response.MonitoringRecordSolarResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringRecordSolarListResponse;
import com.kct.jcredit.core_backend.dto.response.FileUploadResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringEvidenceItemsResponse;
import org.springframework.web.multipart.MultipartFile;



public interface MonitoringService {
    /**
     * Register monitoring record for a company-specific site/project
     */
    MonitoringRecordSolarResponse registerMonitoringRecordSolar(
        String username,
        String companyId,
        RegisterMonitoringRecordSolarRequest request
    );

    BaseResponse validateMonitoringRecordSolar(
        String username,
        String companyId,
        RegisterMonitoringRecordSolarRequest request
    );
    
    /**
     * Read last monitoring record for a company-specific site/project
     */
    MonitoringRecordSolarResponse readLastMonitoringRecordSolar(
        String username,
        String companyId,
        String projectId,
        String siteId
    );

    MonitoringRecordSolarListResponse readAllMonitoringRecordsSolar(
        String username,
        Integer page,
        Integer limit,
        String projectId,
        String nextPageToken
    );
    

    /**
     * Read monitoring record by ID for a company-specific site/project
     */   
     MonitoringRecordSolarListResponse readSiteMonitoringRecordsSolar(
        String username,
        String companyId,
        String projectId,
        String siteId,
        Integer page,
        Integer limit,
        String nextPageToken
    );
    

    /**
     * 4 Evidence Upload APIs - company-scoped
     * ProjectId is derived from siteId via SiteProjectRelation
     */
    FileUploadResponse uploadElpvEvidence(
        String username,
        String companyId,
        String projectId,
        String siteId,
        String description,
        MultipartFile file
    );

    FileUploadResponse uploadElpvrEvidence(
        String username,
        String companyId,
        String projectId,
        String siteId,
        String description,
        MultipartFile file
    );

    FileUploadResponse uploadElpjBatterySpecificationEvidence(
        String username,
        String companyId,
        String projectId,
        String siteId,
        String description,
        MultipartFile file
    );

    FileUploadResponse uploadElpjControlEvidence(
        String username,
        String companyId,
        String projectId,
        String siteId,
        String description,
        MultipartFile file
    );

    MonitoringEvidenceItemsResponse readMonitoringEvidenceItems(
            String username,
            String companyId,
            String projectId,
            String siteId,
            Integer equipmentId
    );

}
