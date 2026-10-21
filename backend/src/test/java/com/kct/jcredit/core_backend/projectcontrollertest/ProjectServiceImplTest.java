package com.kct.jcredit.core_backend.projectcontrollertest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.request.CreateProjectRequest;
import com.kct.jcredit.core_backend.dto.request.UpdateProjectRequest;
import com.kct.jcredit.core_backend.dto.request.details.ProjectDtoReq;
import com.kct.jcredit.core_backend.dto.response.CreateProjectResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectCompanyListResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectListInfoResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectPdfDownloadResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectResponse;
import com.kct.jcredit.core_backend.dto.response.UpdateProjectStatusResponse;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.exception.ResourceNotFoundException;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatorCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.ProjectParticipationPdfService;
import com.kct.jcredit.core_backend.service.impl.ProjectServiceImpl;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;// Scope note: only the methods reachable from ProjectController are covered.
// getProjectsBySiteIdAndCompanyId, assertProject and hasProjectAccess belong to the
// rounds of whichever controllers call them.
// NOTE: entities and DTOs are mocked rather than built, so the tests do not depend on
// constructors or builders that have not been verified here.
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectServiceImpl のテスト")
class ProjectServiceImplTest {

    @Mock private SiteRepository siteRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private OperatorCompanyRepository operatorCompanyRepository;
    @Mock private ParticipatingCompanyRepository participatingCompanyRepository;
    @Mock private ParticipatingProjectRepository participatingProjectRepository;
    @Mock private ProjectParticipationPdfService pdfService;
    @Mock private SiteProjectRelationRepository siteProjectRelationRepository;
    @Mock private OperatingUserRepository userRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private static final String PROJECT_ID = "PUB-PJ-001";
    private static final Integer PROJECT_DB_ID = 7;
    private static final String OPERATOR_COMPANY_ID = "OP-0001";
    private static final Integer OPERATOR_DB_ID = 3;
    private static final String USERNAME = "operator001";
    private static final String COMPANY_ID = "C-0001";
    private static final String OTHER_COMPANY_ID = "C-9999";
    private static final Integer PARTICIPATION_ID = 55;
    private static final String PROJECT_NUMBER = "PJ-2026-001";
    private static final String METHOD_NUMBER = "EN-R-002";

    // ─────────────────────────────────────────────────────────────
    // Fixtures
    // ─────────────────────────────────────────────────────────────

    private OperatorCompany operatorCompanyMock() {
        OperatorCompany company = mock(OperatorCompany.class);
        lenient().when(company.getId()).thenReturn(OPERATOR_DB_ID);
        lenient().when(company.getCompanyId()).thenReturn(OPERATOR_COMPANY_ID);
        lenient().when(company.getNameJa()).thenReturn("運営株式会社");
        return company;
    }

    private Project projectMock(OperatorCompany operatorCompany) {
        Project project = mock(Project.class);
        lenient().when(project.getId()).thenReturn(PROJECT_DB_ID);
        lenient().when(project.getPublicId()).thenReturn(PROJECT_ID);
        lenient().when(project.getProjectName()).thenReturn("熊本太陽光プロジェクト");
        lenient().when(project.getProjectNumber()).thenReturn(PROJECT_NUMBER);
        lenient().when(project.getProjectStatus()).thenReturn("公開");
        lenient().when(project.getOperatorCompany()).thenReturn(operatorCompany);
        lenient().when(project.getIsActive()).thenReturn(true);
        return project;
    }

    /** Stubs the publicId -> entity lookup every projectId-based method starts with. */
    private Project stubProjectLookup() {
        OperatorCompany operatorCompany = operatorCompanyMock();
        Project project = projectMock(operatorCompany);
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.of(project));
        lenient().when(projectRepository.findById(PROJECT_DB_ID))
                .thenReturn(Optional.of(project));
        return project;
    }

    private ProjectDtoReq projectDto(String projectNumber, String methodNumber) {
        ProjectDtoReq dto = mock(ProjectDtoReq.class);
        lenient().when(dto.getProjectNumber()).thenReturn(projectNumber);
        lenient().when(dto.getMethodNumber()).thenReturn(methodNumber);
        lenient().when(dto.getProjectName()).thenReturn("熊本太陽光プロジェクト");
        return dto;
    }

    private CreateProjectRequest createRequest(List<ProjectDtoReq> dtos) {
        CreateProjectRequest request = mock(CreateProjectRequest.class);
        lenient().when(request.getProject()).thenReturn(dtos);
        lenient().when(request.getCreatedBy()).thenReturn(USERNAME);
        lenient().when(request.getOperatorCompanyId()).thenReturn(OPERATOR_COMPANY_ID);
        return request;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> errorsOf(CreateProjectResponse response) {
        return response.getErrors() == null ? Map.of() : (Map<String, String>) response.getErrors();
    }

    // ==================================================================
    // 01. createProject
    // ==================================================================

    @Test
    @DisplayName("UT-PS-01-01: 正常 - 重複がない場合、プロジェクトが保存されOKを返す")
    void createProject_noDuplicates_savesAndReturnsOk() {
        ProjectDtoReq dto = projectDto(PROJECT_NUMBER, METHOD_NUMBER);
        CreateProjectRequest request = createRequest(List.of(dto));

        when(projectRepository.existsByProjectNumber(PROJECT_NUMBER)).thenReturn(false);
        when(projectRepository.existsByMethodNumber(METHOD_NUMBER)).thenReturn(false);
        // Helpers that stub their own mocks must be built before when(...) opens a
        // stubbing; calling one inside thenReturn(...) trips UnfinishedStubbingException.
        OperatorCompany operatorCompany = operatorCompanyMock();
        Project saved = projectMock(operatorCompanyMock());
        when(operatorCompanyRepository.findByCompanyId(OPERATOR_COMPANY_ID))
                .thenReturn(Optional.of(operatorCompany));
        when(projectRepository.saveAll(anyList())).thenReturn(List.of(saved));

        CreateProjectResponse response = projectService.createProject(request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getProject()).hasSize(1);
    }

    @Test
    @DisplayName("UT-PS-01-02: 異常 - プロジェクト番号がDBに既存の場合CONFLICTを返し保存しない")
    void createProject_projectNumberAlreadyUsed_returnsConflict() {
        ProjectDtoReq dto = projectDto(PROJECT_NUMBER, METHOD_NUMBER);
        CreateProjectRequest request = createRequest(List.of(dto));

        when(projectRepository.existsByProjectNumber(PROJECT_NUMBER)).thenReturn(true);
        when(projectRepository.existsByMethodNumber(METHOD_NUMBER)).thenReturn(false);

        CreateProjectResponse response = projectService.createProject(request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.CONFLICT);
        assertThat(errorsOf(response)).containsEntry("project[0].projectNumber",
                ResponseConstants.MSG_PROJECT_NUMBER_ALREADY_USED);
        verify(projectRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("UT-PS-01-03: 異常 - 方法論番号がDBに既存の場合CONFLICTを返す")
    void createProject_methodNumberAlreadyUsed_returnsConflict() {
        ProjectDtoReq dto = projectDto(PROJECT_NUMBER, METHOD_NUMBER);
        CreateProjectRequest request = createRequest(List.of(dto));

        when(projectRepository.existsByProjectNumber(PROJECT_NUMBER)).thenReturn(false);
        when(projectRepository.existsByMethodNumber(METHOD_NUMBER)).thenReturn(true);

        CreateProjectResponse response = projectService.createProject(request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.CONFLICT);
        assertThat(errorsOf(response)).containsEntry("project[0].methodNumber",
                ResponseConstants.MSG_METHOD_NUMBER_ALREADY_USED);
    }

    @Test
    @DisplayName("UT-PS-01-04: 異常 - 同一リクエスト内でプロジェクト番号が重複する場合、両方の要素にエラーが付く")
    void createProject_duplicateWithinRequest_flagsBothEntries() {
        // The second pass over the list exists precisely so the first occurrence is also
        // reported; without it the user would only see the later row flagged.
        ProjectDtoReq first = projectDto(PROJECT_NUMBER, "M-001");
        ProjectDtoReq second = projectDto(PROJECT_NUMBER, "M-002");
        CreateProjectRequest request = createRequest(List.of(first, second));

        when(projectRepository.existsByMethodNumber("M-001")).thenReturn(false);
        when(projectRepository.existsByMethodNumber("M-002")).thenReturn(false);

        CreateProjectResponse response = projectService.createProject(request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.CONFLICT);
        assertThat(errorsOf(response)).containsKeys(
                "project[0].projectNumber", "project[1].projectNumber");
        verify(projectRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("UT-PS-01-05: 準正常 - 番号が空白の場合は重複検査の対象外となる")
    void createProject_blankNumbers_skipDuplicateCheck() {
        // Blank values are reported by bean validation at the controller boundary, so the
        // duplicate check deliberately ignores them rather than querying for "".
        ProjectDtoReq dto = projectDto("   ", null);
        CreateProjectRequest request = createRequest(List.of(dto));

        // Helpers that stub their own mocks must be built before when(...) opens a
        // stubbing; calling one inside thenReturn(...) trips UnfinishedStubbingException.
        OperatorCompany operatorCompany = operatorCompanyMock();
        Project saved = projectMock(operatorCompanyMock());
        when(operatorCompanyRepository.findByCompanyId(OPERATOR_COMPANY_ID))
                .thenReturn(Optional.of(operatorCompany));
        when(projectRepository.saveAll(anyList())).thenReturn(List.of(saved));

        CreateProjectResponse response = projectService.createProject(request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        verify(projectRepository, never()).existsByProjectNumber(anyString());
        verify(projectRepository, never()).existsByMethodNumber(anyString());
    }

    @Test
    @DisplayName("UT-PS-01-06: 正常 - 排出削減量がベースライン−事業実施後排出量として計算される")
    void createProject_computesEmissionReduction() {
        ProjectDtoReq dto = projectDto(PROJECT_NUMBER, METHOD_NUMBER);
        when(dto.getBaselineEmissions()).thenReturn(500.0);
        when(dto.getPostProjectEmission()).thenReturn(180.0);
        CreateProjectRequest request = createRequest(List.of(dto));

        when(projectRepository.existsByProjectNumber(PROJECT_NUMBER)).thenReturn(false);
        when(projectRepository.existsByMethodNumber(METHOD_NUMBER)).thenReturn(false);
        // Helpers that stub their own mocks must be built before when(...) opens a
        // stubbing; calling one inside thenReturn(...) trips UnfinishedStubbingException.
        OperatorCompany operatorCompany = operatorCompanyMock();
        Project saved = projectMock(operatorCompanyMock());
        when(operatorCompanyRepository.findByCompanyId(OPERATOR_COMPANY_ID))
                .thenReturn(Optional.of(operatorCompany));
        when(projectRepository.saveAll(anyList())).thenReturn(List.of(saved));

        projectService.createProject(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Project>> captor = ArgumentCaptor.forClass(List.class);
        verify(projectRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getEmissionReduction()).isEqualTo(320.0);
    }

    @Test
    @DisplayName("UT-PS-01-07: 異常 - 運営会社が存在しない場合、例外をスローし保存しない")
    void createProject_operatorCompanyNotFound_throws() {
        ProjectDtoReq dto = projectDto(PROJECT_NUMBER, METHOD_NUMBER);
        CreateProjectRequest request = createRequest(List.of(dto));

        when(projectRepository.existsByProjectNumber(PROJECT_NUMBER)).thenReturn(false);
        when(projectRepository.existsByMethodNumber(METHOD_NUMBER)).thenReturn(false);
        when(operatorCompanyRepository.findByCompanyId(OPERATOR_COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> projectService.createProject(request));

        verify(projectRepository, never()).saveAll(anyList());
    }

    // ==================================================================
    // 02. updateProject
    // ==================================================================

    private UpdateProjectRequest updateRequest() {
        UpdateProjectRequest request = mock(UpdateProjectRequest.class);
        lenient().when(request.getId()).thenReturn(PROJECT_ID);
        lenient().when(request.getProcessedBy()).thenReturn(USERNAME);
        return request;
    }

    @Test
    @DisplayName("UT-PS-02-01: 正常 - 指定された項目のみ更新されOKを返す")
    void updateProject_setsOnlyProvidedFields() {
        Project project = stubProjectLookup();
        UpdateProjectRequest request = updateRequest();
        when(request.getProjectName()).thenReturn("新プロジェクト名");
        when(projectRepository.save(project)).thenReturn(project);

        ProjectResponse response = projectService.updateProject(request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        verify(project).setProjectName("新プロジェクト名");
    }

    @Test
    @DisplayName("UT-PS-02-02: 正常 - nullの項目は既存値を上書きしない")
    void updateProject_nullFields_leaveEntityUntouched() {
        // PATCH semantics: a field the client did not send must not be cleared.
        Project project = stubProjectLookup();
        UpdateProjectRequest request = updateRequest();   // every getter returns null
        when(projectRepository.save(project)).thenReturn(project);

        projectService.updateProject(request);

        verify(project, never()).setProjectName(anyString());
        verify(project, never()).setRepEmail(anyString());
        verify(project, never()).setMethodName(anyString());
    }

    @Test
    @DisplayName("UT-PS-02-03: 異常 - プロジェクトが存在しない場合NOT_FOUNDを返し保存しない")
    void updateProject_notFound_returnsNotFound() {
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.empty());
        UpdateProjectRequest request = updateRequest();

        ProjectResponse response = projectService.updateProject(request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-PS-02-04: 正常 - 更新者がprocessedByから記録される")
    void updateProject_recordsUpdatedBy() {
        Project project = stubProjectLookup();
        UpdateProjectRequest request = updateRequest();
        when(projectRepository.save(project)).thenReturn(project);

        projectService.updateProject(request);

        verify(project).setUpdatedBy(USERNAME);
    }

    @Test
    @DisplayName("UT-PS-02-05: 正常 - レスポンスにはリクエストに含まれた項目のみ設定される")
    void updateProject_responseCarriesOnlyRequestedFields() {
        // The response mirrors the request, so the client can see exactly what it changed
        // rather than the whole entity.
        Project project = stubProjectLookup();
        UpdateProjectRequest request = updateRequest();
        when(request.getProjectName()).thenReturn("新プロジェクト名");
        when(projectRepository.save(project)).thenReturn(project);
        when(project.getProjectName()).thenReturn("新プロジェクト名");

        ProjectResponse response = projectService.updateProject(request);

        assertThat(response.getProject().getProjectName()).isEqualTo("新プロジェクト名");
        assertThat(response.getProject().getRepEmail()).isNull();
    }

    // ==================================================================
    // 03. updateProjectStatus
    // ==================================================================

    @Test
    @DisplayName("UT-PS-03-01: 正常 - ステータスが更新され、旧・新ステータスと更新者が記録される")
    void updateProjectStatus_valid_updatesAndRecordsActor() {
        Project project = stubProjectLookup();
        when(project.getProjectStatus()).thenReturn("下書き");
        when(projectRepository.save(project)).thenReturn(project);

        UpdateProjectStatusResponse response =
                projectService.updateProjectStatus("公開", PROJECT_ID, USERNAME);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getOldStatus()).isEqualTo("下書き");
        verify(project).setProjectStatus("公開");
        verify(project).setUpdatedBy(USERNAME);
    }

    @Test
    @DisplayName("UT-PS-03-02: 異常 - プロジェクトが存在しない場合NOT_FOUNDを返す")
    void updateProjectStatus_notFound_returnsNotFound() {
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.empty());

        UpdateProjectStatusResponse response =
                projectService.updateProjectStatus("公開", PROJECT_ID, USERNAME);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-PS-03-03: 異常 - 許可されていないステータス値の場合BAD_REQUESTを返し保存しない")
    void updateProjectStatus_unknownStatus_returnsBadRequest() {
        // The column has no constraint, so an unknown value would be stored as-is and then
        // compared against literals everywhere else.
        stubProjectLookup();

        UpdateProjectStatusResponse response =
                projectService.updateProjectStatus("banana", PROJECT_ID, USERNAME);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-PS-03-04: 異常 - 無効化されたプロジェクトの場合FORBIDDENを返す")
    void updateProjectStatus_inactiveProject_returnsForbidden() {
        Project project = stubProjectLookup();
        when(project.getIsActive()).thenReturn(false);

        UpdateProjectStatusResponse response =
                projectService.updateProjectStatus("公開", PROJECT_ID, USERNAME);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-PS-03-05: 異常 - 同一ステータスへの変更はBAD_REQUESTを返す")
    void updateProjectStatus_sameStatus_returnsBadRequest() {
        Project project = stubProjectLookup();
        when(project.getProjectStatus()).thenReturn("公開");

        UpdateProjectStatusResponse response =
                projectService.updateProjectStatus("公開", PROJECT_ID, USERNAME);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-PS-03-06: 異常 - 終了済みプロジェクトのステータスは変更できない")
    void updateProjectStatus_alreadyClosed_returnsBadRequest() {
        Project project = stubProjectLookup();
        when(project.getProjectStatus()).thenReturn("終了");

        UpdateProjectStatusResponse response =
                projectService.updateProjectStatus("公開", PROJECT_ID, USERNAME);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-PS-03-07: 正常 - 終了へ変更すると終了日時が記録される")
    void updateProjectStatus_toClosed_setsFinalizedAt() {
        Project project = stubProjectLookup();
        when(project.getProjectStatus()).thenReturn("公開");
        when(projectRepository.save(project)).thenReturn(project);

        projectService.updateProjectStatus("終了", PROJECT_ID, USERNAME);

        verify(project).setStatusFinalizedAt(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("UT-PS-03-08: 準正常 - 現在ステータスがnullでもNPEにならない（境界値）")
    void updateProjectStatus_nullCurrentStatus_doesNotThrow() {
        // Rows created before this column was populated carry a null status, and there is
        // no try/catch here to turn an NPE into a clean 500.
        Project project = stubProjectLookup();
        when(project.getProjectStatus()).thenReturn(null);
        when(projectRepository.save(project)).thenReturn(project);

        UpdateProjectStatusResponse response =
                projectService.updateProjectStatus("公開", PROJECT_ID, USERNAME);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
    }

    // ==================================================================
    // 04. getProject
    // ==================================================================

    @Test
    @DisplayName("UT-PS-04-01: 正常 - プロジェクト詳細が取得できる")
    void getProject_valid_returnsDetail() {
        stubProjectLookup();

        ProjectResponse response = projectService.getProject(PROJECT_ID, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getProject().getId()).isEqualTo(PROJECT_ID);
        assertThat(response.getProject().getCompanyName()).isEqualTo("運営株式会社");
    }

    @Test
    @DisplayName("UT-PS-04-02: 異常 - プロジェクトが存在しない場合NOT_FOUNDを返す")
    void getProject_notFound_returnsNotFound() {
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.empty());

        ProjectResponse response = projectService.getProject(PROJECT_ID, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getProject()).isNull();
    }

    @Test
    @DisplayName("UT-PS-04-03: 準正常 - userCompanyIdは一切使用されない（アーキテクチャ上の注記）")
    void getProject_ignoresUserCompanyId() {
        // NOTE: the controller reads companyId from the token and passes it in, but the
        // method never reads it. Either every participating company may view every project
        // - in which case the parameter should go - or the scope check is missing. An
        // unused scope parameter reads as "already scoped" to the next person.
        stubProjectLookup();

        ProjectResponse sameCompany = projectService.getProject(PROJECT_ID, COMPANY_ID);
        ProjectResponse otherCompany = projectService.getProject(PROJECT_ID, OTHER_COMPANY_ID);

        assertThat(sameCompany.getStatusCode()).isEqualTo(otherCompany.getStatusCode());
    }

    @Test
    @DisplayName("UT-PS-04-04: 準正常 - 運営会社がnullの場合NPEになる（要確認）")
    void getProject_operatorCompanyNull_throwsNpe() {
        // NOTE: convertToProjectDetailResponse calls getOperatorCompany().getNameJa() and
        // .getId() unguarded, and getProject has no try/catch. The same field is guarded in
        // ProjectMonitoringServiceImpl.buildHeaderInfo, so whether it may be null has not
        // been decided consistently.
        Project project = mock(Project.class);
        lenient().when(project.getId()).thenReturn(PROJECT_DB_ID);
        lenient().when(project.getOperatorCompany()).thenReturn(null);
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.of(project));
        when(projectRepository.findById(PROJECT_DB_ID)).thenReturn(Optional.of(project));

        assertThrows(NullPointerException.class,
                () -> projectService.getProject(PROJECT_ID, COMPANY_ID));
    }

    // ==================================================================
    // 09. findByFilter
    // ==================================================================

    @SuppressWarnings("unchecked")
    private Page<Project> stubFindAll(List<Project> content, long total) {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<Project> page = mock(Page.class);
        lenient().when(page.getContent()).thenReturn(content);
        lenient().when(page.getTotalElements()).thenReturn(total);
        lenient().when(page.getTotalPages()).thenReturn((int) Math.ceil(total / 20.0));
        when(projectRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        return page;
    }

    @Test
    @DisplayName("UT-PS-09-01: 正常 - pageは0起点に変換され、sortOrder=descで降順になる")
    void findByFilter_buildsPageableWithZeroBasedPageAndDescSort() {
        stubFindAll(List.of(), 0);

        projectService.findByFilter(2, 50, null, null, null, null, null, null,
                null, "updatedAt", "desc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(projectRepository).findAll(any(Specification.class), captor.capture());
        Pageable pageable = captor.getValue();

        // The API is 1-based and Spring Data is 0-based; an off-by-one here silently skips
        // or repeats a page.
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(50);
        assertThat(pageable.getSort().getOrderFor("updatedAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("UT-PS-09-02: 正常 - sortOrder=ascで昇順になる")
    void findByFilter_ascendingSortOrder() {
        stubFindAll(List.of(), 0);

        projectService.findByFilter(1, 20, null, null, null, null, null, null,
                null, "createdAt", "asc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(projectRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("UT-PS-09-03: 準正常 - desc以外の値はすべて昇順として扱われる（境界値）")
    void findByFilter_unknownSortOrderFallsBackToAscending() {
        // "desc".equalsIgnoreCase(sortOrder) means anything else - including a typo - sorts
        // ascending. The @Pattern on the controller normally prevents this, but the service
        // itself accepts any string.
        stubFindAll(List.of(), 0);

        projectService.findByFilter(1, 20, null, null, null, null, null, null,
                null, "updatedAt", "DESCENDING");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(projectRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("updatedAt").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("UT-PS-09-04: 正常 - ページネーション情報がレスポンスに設定される")
    void findByFilter_populatesPagination() {
        stubFindAll(List.of(), 45);

        ProjectListInfoResponse response = projectService.findByFilter(
                2, 20, null, null, null, null, null, null, null, "updatedAt", "desc");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getPagination().getTotalItems()).isEqualTo(45);
    }

    @Test
    @DisplayName("UT-PS-09-05: 正常 - 該当0件の場合も空配列とOKを返す")
    void findByFilter_noResults_returnsEmptyListWithOk() {
        stubFindAll(List.of(), 0);

        ProjectListInfoResponse response = projectService.findByFilter(
                1, 20, null, null, null, null, null, null, null, "updatedAt", "desc");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getProjects()).isEmpty();
    }

    @Test
    @DisplayName("UT-PS-09-06: 正常 - 取得結果がProjectDetailResponseに変換される")
    void findByFilter_mapsContentToDto() {
        stubFindAll(List.of(projectMock(operatorCompanyMock())), 1);

        ProjectListInfoResponse response = projectService.findByFilter(
                1, 20, "公開", null, null, null, null, null, null, "updatedAt", "desc");

        assertThat(response.getProjects()).hasSize(1);
        assertThat(response.getProjects().get(0).getId()).isEqualTo(PROJECT_ID);
    }

    // ==================================================================
    // 11. getProjectCompanies
    // ==================================================================

    private ParticipatingProject participationWithCompany(String companyId, String pdfKey) {
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        lenient().when(company.getCompanyId()).thenReturn(companyId);
        lenient().when(company.getNameJa()).thenReturn("参加企業" + companyId);

        Site site = mock(Site.class);
        lenient().when(site.getCompany()).thenReturn(company);

        EnR002Equipment equipment = mock(EnR002Equipment.class);
        lenient().when(equipment.getSite()).thenReturn(site);

        ParticipatingProject participation = mock(ParticipatingProject.class);
        lenient().when(participation.getId()).thenReturn(PARTICIPATION_ID);
        lenient().when(participation.getEquipment()).thenReturn(equipment);
        lenient().when(participation.getS3KeyPdfMembership()).thenReturn(pdfKey);
        return participation;
    }

    @Test
    @DisplayName("UT-PS-11-01: 正常 - 参加企業一覧が取得でき、PDF有無がREADY/NOT_READYで返る")
    void getProjectCompanies_mapsCompaniesAndPdfStatus() {
        stubProjectLookup();
        ParticipatingProject withPdf =
                participationWithCompany(COMPANY_ID, "pdf/membership/1.pdf");
        ParticipatingProject withoutPdf = participationWithCompany(OTHER_COMPANY_ID, null);
        when(participatingProjectRepository.findByProject_Id(PROJECT_DB_ID))
                .thenReturn(List.of(withPdf, withoutPdf));

        ProjectCompanyListResponse response = projectService.getProjectCompanies(PROJECT_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getCompanies()).hasSize(2);
        assertThat(response.getCompanies()).extracting(ProjectCompanyResponse::getPdfStatus)
                .containsExactly("READY", "NOT_READY");
    }

    @Test
    @DisplayName("UT-PS-11-02: 異常 - プロジェクトが存在しない場合NOT_FOUNDを返す")
    void getProjectCompanies_projectNotFound_returnsNotFound() {
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.empty());

        ProjectCompanyListResponse response = projectService.getProjectCompanies(PROJECT_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        verify(participatingProjectRepository, never()).findByProject_Id(any());
    }

    @Test
    @DisplayName("UT-PS-11-03: 正常 - 参加企業が0件の場合も空配列とOKを返す")
    void getProjectCompanies_noParticipations_returnsEmptyList() {
        stubProjectLookup();
        when(participatingProjectRepository.findByProject_Id(PROJECT_DB_ID))
                .thenReturn(List.of());

        ProjectCompanyListResponse response = projectService.getProjectCompanies(PROJECT_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getCompanies()).isEmpty();
    }

    // ==================================================================
    // 12. getMembershipPdf
    // ==================================================================

    @Test
    @DisplayName("UT-PS-12-01: 正常 - 自社の参加情報であればpresigned URLとOKを返す")
    void getMembershipPdf_ownCompany_returnsUrl() {
        stubProjectLookup();
        ParticipatingProject participation =
                participationWithCompany(COMPANY_ID, "pdf/membership/1.pdf");
        when(participatingProjectRepository.findByIdAndProject_Id(PARTICIPATION_ID, PROJECT_DB_ID))
                .thenReturn(Optional.of(participation));
        when(pdfService.generatePresignedUrl("pdf/membership/1.pdf"))
                .thenReturn("https://s3.example.com/signed");

        ProjectPdfDownloadResponse response = projectService.getMembershipPdf(
                PROJECT_ID, PARTICIPATION_ID, RoleConstants.MASTER, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getPdfUrl()).isEqualTo("https://s3.example.com/signed");
    }

    @Test
    @DisplayName("UT-PS-12-02: 異常 - プロジェクトが存在しない場合NOT_FOUNDをスローする")
    void getMembershipPdf_projectNotFound_throwsNotFound() {
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> projectService.getMembershipPdf(
                        PROJECT_ID, PARTICIPATION_ID, RoleConstants.MASTER, COMPANY_ID));

        assertThat(ex.getMessage()).isEqualTo(ResponseConstants.MSG_PROJECT_NOT_FOUND);
        verifyNoInteractions(pdfService);
    }

    @Test
    @DisplayName("UT-PS-12-03: 異常 - 参加情報が当該プロジェクトのものでない場合NOT_FOUNDをスローする")
    void getMembershipPdf_participationFromAnotherProject_throwsNotFound() {
        // The lookup is keyed on both ids, so a projectId in the path cannot be paired with
        // an arbitrary participationId.
        stubProjectLookup();
        when(participatingProjectRepository.findByIdAndProject_Id(PARTICIPATION_ID, PROJECT_DB_ID))
                .thenReturn(Optional.empty());

        assertThrows(CustomException.class,
                () -> projectService.getMembershipPdf(
                        PROJECT_ID, PARTICIPATION_ID, RoleConstants.MASTER, COMPANY_ID));

        verifyNoInteractions(pdfService);
    }

    @Test
    @DisplayName("UT-PS-12-04: 異常 - 他社の参加情報をmasterが要求した場合FORBIDDENをスローする")
    void getMembershipPdf_masterOtherCompany_throwsForbidden() {
        stubProjectLookup();
        ParticipatingProject participation =
                participationWithCompany(OTHER_COMPANY_ID, "pdf/membership/1.pdf");
        when(participatingProjectRepository.findByIdAndProject_Id(PARTICIPATION_ID, PROJECT_DB_ID))
                .thenReturn(Optional.of(participation));

        CustomException ex = assertThrows(CustomException.class,
                () -> projectService.getMembershipPdf(
                        PROJECT_ID, PARTICIPATION_ID, RoleConstants.MASTER, COMPANY_ID));

        assertThat(ex.getMessage()).isEqualTo(ResponseConstants.MSG_FORBIDDEN);
        verifyNoInteractions(pdfService);
    }

    @Test
    @DisplayName("UT-PS-12-05: 正常 - adminは他社の参加情報も取得できる（設計どおり）")
    void getMembershipPdf_adminCrossCompany_isAllowed() {
        // The operator company oversees every member company, so admin and operator are
        // the allowlisted exemptions from the company-scope check.
        stubProjectLookup();
        ParticipatingProject participation =
                participationWithCompany(OTHER_COMPANY_ID, "pdf/membership/1.pdf");
        when(participatingProjectRepository.findByIdAndProject_Id(PARTICIPATION_ID, PROJECT_DB_ID))
                .thenReturn(Optional.of(participation));
        when(pdfService.generatePresignedUrl("pdf/membership/1.pdf"))
                .thenReturn("https://s3.example.com/signed");

        ProjectPdfDownloadResponse response = projectService.getMembershipPdf(
                PROJECT_ID, PARTICIPATION_ID, RoleConstants.ADMIN, OPERATOR_COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
    }

    @Test
    @DisplayName("UT-PS-12-06: 異常 - PDFが未生成の場合PDF_NOT_READYをスローする")
    void getMembershipPdf_noPdfKey_throwsNotReady() {
        stubProjectLookup();
        ParticipatingProject participation = participationWithCompany(COMPANY_ID, "   ");
        when(participatingProjectRepository.findByIdAndProject_Id(PARTICIPATION_ID, PROJECT_DB_ID))
                .thenReturn(Optional.of(participation));

        assertThrows(CustomException.class,
                () -> projectService.getMembershipPdf(
                        PROJECT_ID, PARTICIPATION_ID, RoleConstants.MASTER, COMPANY_ID));

        verifyNoInteractions(pdfService);
    }
}