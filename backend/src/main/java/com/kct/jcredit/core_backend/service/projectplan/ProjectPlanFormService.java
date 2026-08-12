package com.kct.jcredit.core_backend.service.projectplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.kct.jcredit.core_backend.dto.response.ProjectPlanFormResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectPlanProgressResponse;

/**
 * Serves every page of the project plan from the form registry.
 *
 * <p>Pages are atomic: a page is persisted only when it validates in full, so
 * an interrupted operator resumes at the first page they have not completed.
 */
public interface ProjectPlanFormService {

    ProjectPlanFormResponse getForm(
            String projectId,
            String formSlug,
            Integer methodologyIndex,
            String companyId,
            String role,
            String username);

    /** Validates the whole page, then stores it. Nothing is written if invalid. */
    ProjectPlanFormResponse saveForm(
            String projectId,
            String formSlug,
            Integer methodologyIndex,
            JsonNode body,
            String companyId,
            String role,
            String username);

    ProjectPlanProgressResponse getProgress(
            String projectId,
            String companyId,
            String role,
            String username);
}
