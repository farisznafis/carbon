package com.kct.jcredit.core_backend.service.documentdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.kct.jcredit.core_backend.constant.PermissionCode;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse.Section;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.enums.ProjectFileType;
import com.kct.jcredit.core_backend.service.documentdata.ProjectDocumentDataService.Caller;
import com.kct.jcredit.core_backend.service.projectplan.ProjectPlanContextService;
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
 * Who gets document data, and for what.
 *
 * <p>The load-bearing guarantees: nothing is collected for a project the
 * caller may not see, and a document built on monitoring data is not a way
 * around the monitoring permission.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectDocumentDataServiceImplTest {

    private static final String PROJECT_ID = "PROJ1";
    private static final List<Section> PLAN_SECTIONS =
            List.of(new Section("project", "プロジェクト概要", List.of(), null));
    private static final List<Section> ACTIVITY_SECTIONS =
            List.of(new Section("activities", "個別活動一覧", List.of(), null));

    @Mock private ProjectPlanContextService contextService;
    @Mock private Project project;

    private DocumentDataProvider planProvider;
    private DocumentDataProvider activityProvider;
    private ProjectDocumentDataServiceImpl service;

    @BeforeEach
    void setUp() {
        planProvider = provider(ProjectFileType.PROJECT_PLAN, Optional.empty(), PLAN_SECTIONS);
        activityProvider = provider(
                ProjectFileType.ACTIVITY_RESULT_LIST,
                Optional.of(PermissionCode.MONITORING_READ),
                ACTIVITY_SECTIONS);
        service = new ProjectDocumentDataServiceImpl(contextService, List.of(planProvider, activityProvider));
        projectIs(ProjectContextStatus.AUTHORIZED);
    }

    @Test
    void returnsTheSectionsOfTheRequestedDocument() {
        ProjectDocumentDataResponse response =
                service.getData(PROJECT_ID, ProjectFileType.PROJECT_PLAN, operator("PROJECT_READ"));

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(response.getFileType()).isEqualTo(ProjectFileType.PROJECT_PLAN);
        assertThat(response.getSections()).isEqualTo(PLAN_SECTIONS);
        verify(planProvider).sections(project, PROJECT_ID, "operator01");
        verify(activityProvider, never()).sections(any(), any(), any());
    }

    @Test
    void identifiesTheCallerToTheProjectCheckByTheToken() {
        service.getData(PROJECT_ID, ProjectFileType.PROJECT_PLAN, operator("PROJECT_READ"));

        verify(contextService).resolveProject(PROJECT_ID, "COMP-001", "operator", "operator01");
    }

    @Test
    void collectsNothingForAProjectOfAnotherCompany() {
        projectIs(ProjectContextStatus.FORBIDDEN);

        ProjectDocumentDataResponse response =
                service.getData(PROJECT_ID, ProjectFileType.PROJECT_PLAN, operator("PROJECT_READ"));

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        assertThat(response.getSections()).isNull();
        verify(planProvider, never()).sections(any(), any(), any());
    }

    @Test
    void reportsAProjectThatDoesNotExist() {
        projectIs(ProjectContextStatus.NOT_FOUND);

        ProjectDocumentDataResponse response =
                service.getData(PROJECT_ID, ProjectFileType.PROJECT_PLAN, operator("PROJECT_READ"));

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getSections()).isNull();
        verify(planProvider, never()).sections(any(), any(), any());
    }

    @Test
    void refusesMonitoringDataToAnOperatorWithoutTheMonitoringPermission() {
        ProjectDocumentDataResponse response =
                service.getData(PROJECT_ID, ProjectFileType.ACTIVITY_RESULT_LIST, operator("PROJECT_READ"));

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        assertThat(response.getSections()).isNull();
        verify(activityProvider, never()).sections(any(), any(), any());
    }

    @Test
    void givesMonitoringDataToAnOperatorHoldingTheMonitoringPermission() {
        ProjectDocumentDataResponse response = service.getData(
                PROJECT_ID, ProjectFileType.ACTIVITY_RESULT_LIST, operator("PROJECT_READ", "MONITORING_READ"));

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getSections()).isEqualTo(ACTIVITY_SECTIONS);
    }

    @Test
    void givesAnAdminEveryDocumentWithoutListedPermissions() {
        ProjectDocumentDataResponse response = service.getData(
                PROJECT_ID,
                ProjectFileType.ACTIVITY_RESULT_LIST,
                new Caller("COMP-001", "admin", "admin01", Set.of("ROLE_admin")));

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getSections()).isEqualTo(ACTIVITY_SECTIONS);
    }

    @Test
    void aRoleNameIsNotAPermission() {
        // "admin" as a permission string must not stand in for the admin role.
        ProjectDocumentDataResponse response = service.getData(
                PROJECT_ID,
                ProjectFileType.ACTIVITY_RESULT_LIST,
                new Caller("COMP-001", "operator", "operator01", Set.of("admin", "ROLE_admin")));

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
    }

    @Test
    void answersWithNoSectionsForADocumentTheSystemKeepsNoDataFor() {
        ProjectDocumentDataResponse response =
                service.getData(PROJECT_ID, ProjectFileType.VALIDATION_REPORT, operator("PROJECT_READ"));

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getSections()).isEmpty();
        assertThat(response.getMessage()).isEqualTo(ProjectDocumentDataServiceImpl.MSG_NO_DATA);
    }

    @Test
    void checksTheProjectBeforeSayingWhetherADocumentHasData() {
        projectIs(ProjectContextStatus.FORBIDDEN);

        ProjectDocumentDataResponse response =
                service.getData(PROJECT_ID, ProjectFileType.VALIDATION_REPORT, operator("PROJECT_READ"));

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
    }

    @Test
    void refusesTwoProvidersForOneDocumentType() {
        DocumentDataProvider second = provider(ProjectFileType.PROJECT_PLAN, Optional.empty(), List.of());

        assertThatThrownBy(() -> new ProjectDocumentDataServiceImpl(contextService, List.of(planProvider, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PROJECT_PLAN");
    }

    private void projectIs(ProjectContextStatus status) {
        when(contextService.resolveProject(any(), any(), any(), any()))
                .thenReturn(new ProjectContext(
                        status, status == ProjectContextStatus.AUTHORIZED ? project : null));
    }

    private static Caller operator(String... permissions) {
        return new Caller("COMP-001", "operator", "operator01", Set.of(permissions));
    }

    private static DocumentDataProvider provider(
            ProjectFileType fileType, Optional<PermissionCode> permission, List<Section> sections) {
        DocumentDataProvider provider = mock(DocumentDataProvider.class);
        when(provider.fileType()).thenReturn(fileType);
        when(provider.additionalPermission()).thenReturn(permission);
        when(provider.sections(any(), any(), any())).thenReturn(sections);
        return provider;
    }
}
