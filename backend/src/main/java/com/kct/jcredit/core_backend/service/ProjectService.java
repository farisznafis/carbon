package com.kct.jcredit.core_backend.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kct.jcredit.core_backend.dto.request.UpdateProjectStatusRequest;
import com.kct.jcredit.core_backend.dto.request.CreateProjectRequest;
import com.kct.jcredit.core_backend.dto.request.UpdateProjectRequest;
import com.kct.jcredit.core_backend.dto.response.CreateProjectResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectListInfoResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectResponse;
import com.kct.jcredit.core_backend.dto.response.UpdateProjectStatusResponse;


public interface ProjectService {
    ProjectListInfoResponse getProjectsBySiteIdAndCompanyId(String siteId, String companyId);

    CreateProjectResponse createProject(CreateProjectRequest request);

    ProjectResponse updateProject(UpdateProjectRequest request);

    UpdateProjectStatusResponse updateProjectStatus(String newStatus, Integer projectId);

    ProjectResponse getProject(Integer projectId, String userCompanyId);

    ProjectListInfoResponse findByFilter(
            int page,
            int limit,
            String status,
            String methodology,
            LocalDate createdFrom,
            LocalDate createdTo,
            LocalDate updatedFrom,
            LocalDate updatedTo,
            String search,
            String sort,
            String sortOrder);

}