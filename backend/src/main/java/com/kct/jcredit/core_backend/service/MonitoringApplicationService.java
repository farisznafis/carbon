package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.MonitoringReportRequest;
import com.kct.jcredit.core_backend.dto.response.CorporationMasterResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringReportResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectMasterResponse;

public interface MonitoringApplicationService {

    CorporationMasterResponse getCompanyMaster(String companyId, String requesterCompanyId, String role, String username);

    ProjectMasterResponse getProjectMaster(Integer projectId, String companyId, String role, String username);

    MonitoringReportResponse submitMonitoringReport(
            Integer projectId,
            String siteId,
            MonitoringReportRequest request,
            String companyId,
            String role,
            String username);

    MonitoringReportResponse getMonitoringReport(
            Integer projectId,
            String siteId,
            String companyId,
            String role,
            String username);
}
