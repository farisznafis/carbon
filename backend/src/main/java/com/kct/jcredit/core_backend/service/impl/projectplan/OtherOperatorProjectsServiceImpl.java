package com.kct.jcredit.core_backend.service.impl.projectplan;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.OtherOperatorProjectsRequest;
import com.kct.jcredit.core_backend.dto.response.OtherOperatorProjectsResponse;
import com.kct.jcredit.core_backend.repository.dynamodb.OtherOperatorProjectsRepository;
import com.kct.jcredit.core_backend.service.projectplan.OtherOperatorProjectsService;
import com.kct.jcredit.core_backend.service.impl.projectplan.ProjectPlanContextService.ProjectContext;
import com.kct.jcredit.core_backend.service.impl.projectplan.ProjectPlanContextService.ProjectContextStatus;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the other-operator-projects project-plan form.
 *
 * Project ownership is resolved from PostgreSQL, while form data is stored
 * in the shared project-plan DynamoDB table under a form-specific static SK.
 */
@Service
@RequiredArgsConstructor
public class OtherOperatorProjectsServiceImpl implements OtherOperatorProjectsService {

    private static final String OTHER_OPERATOR_PROJECTS_SK = "OTHER_OPERATOR_PROJECTS";

    private final ProjectPlanContextService projectPlanContextService;
    private final OtherOperatorProjectsRepository otherOperatorProjectsRepository;

    /**
     * Creates or replaces the other-operator-projects form for a project.
     */
    @Override
    @Transactional
    public OtherOperatorProjectsResponse submitOtherOperatorProjects(
            String projectId,
            OtherOperatorProjectsRequest request,
            String companyId,
            String role,
            String username) {
        return validateAndSave(projectId, request, companyId, role, username);
    }

    /**
     * Replaces the existing other-operator-projects form for a project.
     */
    @Override
    @Transactional
    public OtherOperatorProjectsResponse updateOtherOperatorProjects(
            String projectId,
            OtherOperatorProjectsRequest request,
            String companyId,
            String role,
            String username) {
        return validateAndSave(projectId, request, companyId, role, username);
    }

    /**
     * Returns the stored form, or an empty keyed form when none has been saved.
     */
    @Override
    @Transactional(readOnly = true)
    public OtherOperatorProjectsResponse getOtherOperatorProjects(
            String projectId,
            String companyId,
            String role,
            String username) {
        OtherOperatorProjectsResponse response = new OtherOperatorProjectsResponse();

        ProjectContext context = projectPlanContextService.resolveProject(projectId, companyId, role, username);
        if (!context.isAuthorized()) {
            applyContextError(response, context);
            return response;
        }

        String pk = buildPk(projectId);
        OtherOperatorProjectsRequest data = otherOperatorProjectsRepository
                .findByPkAndSk(pk, OTHER_OPERATOR_PROJECTS_SK)
                .orElseGet(OtherOperatorProjectsRequest::new);

        data.setPk(pk);
        data.setSk(OTHER_OPERATOR_PROJECTS_SK);

        response.setData(data);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    private OtherOperatorProjectsResponse validateAndSave(
            String projectId,
            OtherOperatorProjectsRequest request,
            String companyId,
            String role,
            String username) {
        OtherOperatorProjectsResponse response = new OtherOperatorProjectsResponse();

        ProjectContext context = projectPlanContextService.resolveProject(projectId, companyId, role, username);
        if (!context.isAuthorized()) {
            applyContextError(response, context);
            return response;
        }

        return saveOtherOperatorProjects(projectId, request);
    }

    private OtherOperatorProjectsResponse saveOtherOperatorProjects(
            String projectId,
            OtherOperatorProjectsRequest request) {
        String pk = buildPk(projectId);

        // DynamoDB keys are server-managed and cannot be supplied by clients.
        request.setPk(pk);
        request.setSk(OTHER_OPERATOR_PROJECTS_SK);

        otherOperatorProjectsRepository.save(request);

        OtherOperatorProjectsResponse response = new OtherOperatorProjectsResponse();
        response.setData(request);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    private void applyContextError(OtherOperatorProjectsResponse response, ProjectContext context) {
        if (context.status() == ProjectContextStatus.NOT_FOUND) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
            return;
        }
        response.setStatusCode(ResponseConstants.FORBIDDEN);
        response.setMessage(ResponseConstants.MSG_FORBIDDEN);
    }

    private String buildPk(String projectId) {
        return String.valueOf(projectId);
    }
}
