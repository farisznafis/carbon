package com.kct.jcredit.core_backend.service.projectplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.service.projectplan.ProjectPlanContextService.ProjectContext;
import com.kct.jcredit.core_backend.service.projectplan.ProjectPlanContextService.ProjectContextStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Who may reach a project plan.
 *
 * <p>Every plan endpoint funnels through here, so a role rejected in this
 * class is forbidden across the whole feature no matter what
 * {@code @PreAuthorize} allows on the controller.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectPlanContextServiceTest {

    private static final String PROJECT_ID = "PROJ1";
    private static final String COMPANY_ID = "COMP1";
    private static final String USERNAME = "taro";

    @Mock private ProjectRepository projectRepository;
    @Mock private OperatingUserRepository operatingUserRepository;
    @Mock private Project project;
    @Mock private OperatorCompany company;
    @Mock private OperatingUser member;

    private ProjectPlanContextService service;

    @BeforeEach
    void setUp() {
        service = new ProjectPlanContextService(projectRepository, operatingUserRepository);

        when(company.getId()).thenReturn(7);
        when(company.getCompanyId()).thenReturn(COMPANY_ID);
        when(project.getOperatorCompany()).thenReturn(company);
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.of(project));
        when(operatingUserRepository.findByUsernameAndCompany_Id(USERNAME, 7))
                .thenReturn(Optional.of(member));
    }

    private ProjectContext resolveAs(String role) {
        return service.resolveProject(PROJECT_ID, COMPANY_ID, role, USERNAME);
    }

    @Test
    void operatorOfTheOwningCompanyIsAuthorized() {
        assertThat(resolveAs(RoleConstants.OPERATOR).isAuthorized()).isTrue();
    }

    @Test
    void adminOfTheOwningCompanyIsAuthorized() {
        // admin and operator are both operating_user roles with a NOT NULL
        // company_id. Rejecting admin here forbade the admin dashboard from
        // every plan endpoint, including 計画書 progress.
        assertThat(resolveAs(RoleConstants.ADMIN).isAuthorized()).isTrue();
    }

    @Test
    void rolesOutsideTheOperatingCompanyAreForbidden() {
        assertThat(resolveAs(RoleConstants.USER).status())
                .isEqualTo(ProjectContextStatus.FORBIDDEN);
        assertThat(resolveAs(RoleConstants.MASTER).status())
                .isEqualTo(ProjectContextStatus.FORBIDDEN);
    }

    @Test
    void adminOfAnotherCompanyIsStillForbidden() {
        // Widening the role check must not widen the company scoping with it.
        ProjectContext context =
                service.resolveProject(PROJECT_ID, "OTHER", RoleConstants.ADMIN, USERNAME);

        assertThat(context.status()).isEqualTo(ProjectContextStatus.FORBIDDEN);
    }

    @Test
    void adminNotRegisteredInTheCompanyIsForbidden() {
        when(operatingUserRepository.findByUsernameAndCompany_Id("stranger", 7))
                .thenReturn(Optional.empty());

        ProjectContext context =
                service.resolveProject(PROJECT_ID, COMPANY_ID, RoleConstants.ADMIN, "stranger");

        assertThat(context.status()).isEqualTo(ProjectContextStatus.FORBIDDEN);
    }

    @Test
    void unknownProjectIsNotFound() {
        when(projectRepository.findByPublicIdWithFallback("nope")).thenReturn(Optional.empty());

        ProjectContext context =
                service.resolveProject("nope", COMPANY_ID, RoleConstants.ADMIN, USERNAME);

        assertThat(context.status()).isEqualTo(ProjectContextStatus.NOT_FOUND);
    }
}
