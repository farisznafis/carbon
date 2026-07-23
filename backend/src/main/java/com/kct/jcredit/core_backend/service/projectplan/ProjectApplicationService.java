package com.kct.jcredit.core_backend.service.projectplan;

import com.kct.jcredit.core_backend.dto.request.ProjectApplicationRequest;
import com.kct.jcredit.core_backend.dto.response.CorporationMasterResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectApplicationResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectMasterResponse;

public interface ProjectApplicationService {
        CorporationMasterResponse getCorporationMaster(String requesterCompanyId, String role, String username);

        ProjectMasterResponse getProjectMaster(String projectId, String companyId, String role, String username);

    ProjectApplicationResponse submitProjectApplication(
            String projectId,
            ProjectApplicationRequest request,
            String companyId,
            String role,
            String username);

    ProjectApplicationResponse updateProjectApplication(
            String projectId,
            ProjectApplicationRequest request,
            String companyId,
            String role,
            String username);

    ProjectApplicationResponse getProjectApplication(
            String projectId,
            String companyId,
            String role,
            String username);
}
