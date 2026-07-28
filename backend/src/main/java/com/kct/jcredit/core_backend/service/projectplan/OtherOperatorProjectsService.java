package com.kct.jcredit.core_backend.service.projectplan;

import com.kct.jcredit.core_backend.dto.request.OtherOperatorProjectsRequest;
import com.kct.jcredit.core_backend.dto.response.OtherOperatorProjectsResponse;

public interface OtherOperatorProjectsService {

    OtherOperatorProjectsResponse submitOtherOperatorProjects(
            String projectId,
            OtherOperatorProjectsRequest request,
            String companyId,
            String role,
            String username);

    OtherOperatorProjectsResponse updateOtherOperatorProjects(
            String projectId,
            OtherOperatorProjectsRequest request,
            String companyId,
            String role,
            String username);

    OtherOperatorProjectsResponse getOtherOperatorProjects(
            String projectId,
            String companyId,
            String role,
            String username);
}
