package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.RegisterMonitoringRecordSolarRequest;
import com.kct.jcredit.core_backend.dto.response.MonitoringRecordSolarResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringRecordSolarListResponse;
import com.kct.jcredit.core_backend.dto.response.FileUploadResponse;
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
    
    /**
     * Read last monitoring record for a company-specific site/project
     */
    MonitoringRecordSolarResponse readLastMonitoringRecordSolar(
        String username,
        String companyId,
        Integer projectId,
        Integer siteId
    );

    MonitoringRecordSolarListResponse readAllMonitoringRecordsSolar(
        String username,
        String companyId,
        Integer projectId,
        Integer siteId,
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
        Integer projectId,
        Integer siteId,
        String description,
        MultipartFile file
    );

    FileUploadResponse uploadElpvrEvidence(
        String username,
        String companyId,
        Integer projectId,
        Integer siteId,
        String description,
        MultipartFile file
    );

    FileUploadResponse uploadElpjBatterySpecificationEvidence(
        String username,
        String companyId,
        Integer projectId,
        Integer siteId,
        String description,
        MultipartFile file
    );

    FileUploadResponse uploadElpjControlEvidence(
        String username,
        String companyId,
        Integer projectId,
        Integer siteId,
        String description,
        MultipartFile file
    );

}
