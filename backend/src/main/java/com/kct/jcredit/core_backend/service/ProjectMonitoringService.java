package com.kct.jcredit.core_backend.service;


import com.kct.jcredit.core_backend.dto.response.ProjectMonitoringResponse;

public interface ProjectMonitoringService {
    ProjectMonitoringResponse getProjectDetail(String projectNumber);
    
}
