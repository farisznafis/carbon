package com.kct.jcredit.core_backend.service.impl.projectplan;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.ApplicableConditionsRequest;
import com.kct.jcredit.core_backend.dto.response.ApplicableConditionsResponse;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.repository.dynamodb.ApplicableConditionsRepository;
import com.kct.jcredit.core_backend.service.projectplan.ApplicableConditionsService;
import com.kct.jcredit.core_backend.service.impl.projectplan.ProjectPlanContextService.ProjectContext;
import com.kct.jcredit.core_backend.service.impl.projectplan.ProjectPlanContextService.ProjectContextStatus;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the applicable-conditions project-plan form.
 *
 * Editable form data is stored in the shared project-plan DynamoDB table
 * under a static SK. Authoritative methodology and operator-company fields are
 * read from PostgreSQL and overlaid when the form is retrieved.
 */
@Service
@RequiredArgsConstructor
public class ApplicableConditionsServiceImpl implements ApplicableConditionsService {

    private static final String APPLICABLE_CONDITIONS_SK = "APPLICABLE_CONDITIONS";

    private final ProjectPlanContextService projectPlanContextService;
    private final ApplicableConditionsRepository applicableConditionsRepository;

    /**
     * Creates or replaces the applicable-conditions form for a project.
     */
    @Override
    @Transactional
    public ApplicableConditionsResponse submitApplicableConditions(
            String projectId,
            ApplicableConditionsRequest request,
            String companyId,
            String role,
            String username) {
        return validateAndSave(projectId, request, companyId, role, username);
    }

    /**
     * Replaces the existing applicable-conditions form for a project.
     */
    @Override
    @Transactional
    public ApplicableConditionsResponse updateApplicableConditions(
            String projectId,
            ApplicableConditionsRequest request,
            String companyId,
            String role,
            String username) {
        return validateAndSave(projectId, request, companyId, role, username);
    }

    /**
     * Returns the stored form with current PostgreSQL master fields overlaid.
     */
    @Override
    @Transactional(readOnly = true)
    public ApplicableConditionsResponse getApplicableConditions(
            String projectId,
            String companyId,
            String role,
            String username) {
        ApplicableConditionsResponse response = new ApplicableConditionsResponse();

        ProjectContext context = projectPlanContextService.resolveProject(projectId, companyId, role, username);
        if (!context.isAuthorized()) {
            applyContextError(response, context);
            return response;
        }

        String pk = buildPk(projectId);
        ApplicableConditionsRequest data = applicableConditionsRepository
                .findByPkAndSk(pk, APPLICABLE_CONDITIONS_SK)
                .orElseGet(ApplicableConditionsRequest::new);

        // PostgreSQL remains authoritative for master fields shown in this form.
        applySqlMasterFields(data, context.project());
        data.setPk(pk);
        data.setSk(APPLICABLE_CONDITIONS_SK);

        response.setData(data);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    private ApplicableConditionsResponse validateAndSave(
            String projectId,
            ApplicableConditionsRequest request,
            String companyId,
            String role,
            String username) {
        ApplicableConditionsResponse response = new ApplicableConditionsResponse();

        ProjectContext context = projectPlanContextService.resolveProject(projectId, companyId, role, username);
        if (!context.isAuthorized()) {
            applyContextError(response, context);
            return response;
        }

        return saveApplicableConditions(projectId, request);
    }

    private ApplicableConditionsResponse saveApplicableConditions(
            String projectId,
            ApplicableConditionsRequest request) {
        String pk = buildPk(projectId);

        // DynamoDB keys are server-managed and cannot be supplied by clients.
        request.setPk(pk);
        request.setSk(APPLICABLE_CONDITIONS_SK);

        applicableConditionsRepository.save(request);

        ApplicableConditionsResponse response = new ApplicableConditionsResponse();
        response.setData(request);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    private void applySqlMasterFields(ApplicableConditionsRequest request, Project project) {
        request.setMethodologyCode(buildMethodologyCode(project));
        request.setMethodologyVersion(project.getMethodologyVersion());

        OperatorCompany company = project.getOperatorCompany();
        if (company != null) {
            request.setBusinessName(company.getNameJa());
            request.setRepresentativePosition(company.getRepPosition());
            request.setRepresentativeName(buildFullName(company.getRepLastName(), company.getRepFirstName()));
        }
    }

    private void applyContextError(ApplicableConditionsResponse response, ProjectContext context) {
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

    private String buildMethodologyCode(Project project) {
        String category = project.getMethodCategory();
        String number = project.getMethodNumber();
        if (category == null || category.isBlank()) {
            return number;
        }
        if (number == null || number.isBlank()) {
            return category;
        }
        return category + "-" + number;
    }

    private String buildFullName(String lastName, String firstName) {
        String safeLast = lastName == null ? "" : lastName.trim();
        String safeFirst = firstName == null ? "" : firstName.trim();
        return (safeLast + " " + safeFirst).trim();
    }
}
