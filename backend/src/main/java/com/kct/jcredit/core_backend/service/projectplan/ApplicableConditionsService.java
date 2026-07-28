package com.kct.jcredit.core_backend.service.projectplan;

import com.kct.jcredit.core_backend.dto.request.ApplicableConditionsRequest;
import com.kct.jcredit.core_backend.dto.response.ApplicableConditionsResponse;

public interface ApplicableConditionsService {

    ApplicableConditionsResponse submitApplicableConditions(
            String projectId,
            ApplicableConditionsRequest request,
            String companyId,
            String role,
            String username);

    ApplicableConditionsResponse updateApplicableConditions(
            String projectId,
            ApplicableConditionsRequest request,
            String companyId,
            String role,
            String username);

    ApplicableConditionsResponse getApplicableConditions(
            String projectId,
            String companyId,
            String role,
            String username);
}
