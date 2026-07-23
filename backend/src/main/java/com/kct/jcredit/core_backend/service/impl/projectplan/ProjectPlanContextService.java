package com.kct.jcredit.core_backend.service.impl.projectplan;

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
 * The context is authorized only when the project belongs to the requesting
 * operator company and the requesting username is registered in that company.
 * Missing project data and denied access are returned explicitly so callers can
 * preserve the existing API response contract.
 */
@Service
@RequiredArgsConstructor
public class ProjectPlanContextService {

    private final ProjectRepository projectRepository;
    private final OperatingUserRepository operatingUserRepository;

    /**
     * Loads a project and verifies that the requesting operator may access it.
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
        Project project = projectRepository.findByPublicId(projectId).orElse(null);
        if (project == null) {
            return ProjectContext.notFound();
        }

        if (!RoleConstants.OPERATOR.equals(role)) {
            return ProjectContext.forbidden();
        }

        OperatorCompany projectCompany = project.getOperatorCompany();
        if (projectCompany == null
                || companyId == null
                || !companyId.equals(projectCompany.getCompanyId())) {
            return ProjectContext.forbidden();
        }

        boolean userBelongsToCompany = operatingUserRepository
                .findByUsernameAndCompany_Id(username, projectCompany.getId())
                .isPresent();
        if (!userBelongsToCompany) {
            return ProjectContext.forbidden();
        }

        return ProjectContext.authorized(project);
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
