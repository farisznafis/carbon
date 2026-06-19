package com.kct.jcredit.core_backend.service.projectplan;

import com.kct.jcredit.core_backend.dto.request.ApplicableConditionsRequest;
import com.kct.jcredit.core_backend.dto.response.ApplicableConditionsResponse;

public interface ApplicableConditionsService {

    ApplicableConditionsResponse submitApplicableConditions(
            Integer projectId,
            ApplicableConditionsRequest request,
            String companyId,
            String role,
            String username);

    ApplicableConditionsResponse updateApplicableConditions(
            Integer projectId,
            ApplicableConditionsRequest request,
            String companyId,
            String role,
            String username);

    ApplicableConditionsResponse getApplicableConditions(
            Integer projectId,
            String companyId,
            String role,
            String username);
}
