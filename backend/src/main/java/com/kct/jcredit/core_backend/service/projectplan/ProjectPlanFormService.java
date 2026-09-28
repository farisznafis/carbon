package com.kct.jcredit.core_backend.service.projectplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFileDownloadResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFileResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanBatchProgressResponse;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanProgressResponse;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

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
    /**
     * Stores one file provisionally against a page. The returned key belongs in
     * the page body; it is promoted only when that page saves.
     */
    ProjectPlanFileResponse uploadFormFile(
            String projectId,
            String formSlug,
            String fieldName,
            MultipartFile file,
            String companyId,
            String role,
            String username);

    /**
     * A short-lived URL to view a file stored on one of this project's pages.
     * Keys belonging to anything else are refused.
     */
    ProjectPlanFileDownloadResponse getFileDownloadUrl(
            String projectId,
            String s3Key,
            String companyId,
            String role,
            String username);

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

    /** Progress for several projects in one round trip, for list views. */
    ProjectPlanBatchProgressResponse getProgressForProjects(
            List<String> projectIds,
            String companyId,
            String role,
            String username);
}
