package com.kct.jcredit.core_backend.service.projectplan;

import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

/**
 * Resolves the PostgreSQL project context shared by project-plan forms.
 *
 * The context is authorized only when the caller is an admin or operator,
 * the project belongs to their operator company, and their username is
 * registered in that company.
 * Missing project data and denied access are returned explicitly so callers can
 * preserve the existing API response contract.
 */
@Service
@RequiredArgsConstructor
public class ProjectPlanContextService {

    private final ProjectRepository projectRepository;
    private final OperatingUserRepository operatingUserRepository;

    /**
     * Loads a project and verifies that the requesting user may access it.
     *
     * @param projectId project identifier from the endpoint
     * @param companyId operator company identifier from the authenticated token
     * @param role role from the authenticated token
     * @param username username from the authenticated token
     * @return authorized project context or an explicit failure status
     */
    public ProjectContext resolveProject(
            String projectId,
            String companyId,
            String role,
            String username) {
        Project project = projectRepository.findByPublicIdWithFallback(projectId).orElse(null);
        if (project == null) {
            return ProjectContext.notFound();
        }

        OperatorCompany projectCompany = project.getOperatorCompany();
        if (projectCompany == null
                || companyId == null
                || !companyId.equals(projectCompany.getCompanyId())) {
            return ProjectContext.forbidden();
        }

        if (!canAccessCompany(role, username, projectCompany)) {
            return ProjectContext.forbidden();
        }

        return ProjectContext.authorized(project);
    }

    /**
     * Whether this caller may work on an operator company's plan data.
     *
     * <p>admin and operator are both roles of operating_user, both with a NOT
     * NULL company_id, so the same membership check scopes them equally. Other
     * roles (user, master) have no business in the plan.
     *
     * <p>Public and shared on purpose: this check used to be duplicated in
     * {@link ProjectPlanAutofillService}, and the copies diverged — admin was
     * allowed here and still refused there, so the corporation autofill
     * endpoint returned 403 while the project one returned 200 for the same
     * caller.
     */
    public boolean canAccessCompany(String role, String username, OperatorCompany company) {
        if (company == null) {
            return false;
        }
        if (!RoleConstants.OPERATOR.equals(role) && !RoleConstants.ADMIN.equals(role)) {
            return false;
        }
        return operatingUserRepository
                .findByUsernameAndCompany_Id(username, company.getId())
                .isPresent();
    }

    public enum ProjectContextStatus {
        AUTHORIZED,
        NOT_FOUND,
        FORBIDDEN
    }

    /**
     * Result of resolving the project and its operator authorization context.
     */
    public record ProjectContext(ProjectContextStatus status, Project project) {

        private static ProjectContext authorized(Project project) {
            return new ProjectContext(ProjectContextStatus.AUTHORIZED, project);
        }

        private static ProjectContext notFound() {
            return new ProjectContext(ProjectContextStatus.NOT_FOUND, null);
        }

        private static ProjectContext forbidden() {
            return new ProjectContext(ProjectContextStatus.FORBIDDEN, null);
        }

        public boolean isAuthorized() {
            return status == ProjectContextStatus.AUTHORIZED;
        }
    }
}
