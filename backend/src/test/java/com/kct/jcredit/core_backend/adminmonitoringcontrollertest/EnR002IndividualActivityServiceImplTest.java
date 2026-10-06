package com.kct.jcredit.core_backend.adminmonitoringcontrollertest;
// Scope note: EnR002IndividualActivityServiceImpl - the three public methods:
//   getRowsByProject(projectPublicId, companyIdFilter, userId)         -> block 05
//   getRowsByCompany(companyId, projectPublicIdFilter, userId)         -> block 06
//   getRowDetail(equipmentId, projectPublicId, userId)                 -> block 07 (lookup, sections),
//                                                                         08 (registration requirements),
//                                                                         09 (reporting window)
// Prefix UT-AMS continues the numbering of MonitoringDataDeletionServiceImplTest (blocks 01-04),
// because both services belong to AdminMonitoringController.
//
// DECIDED RULES (Pattern B, red until the production code follows):
//  - getRowDetail takes the project id as a required argument. One equipment belongs to exactly one
//    project, so the participation is looked up inside that project and anything else is NotFound.
//    The access check runs first, so a forbidden caller cannot tell whether an equipment exists.
//  - 2年以内: the operation start date must be on or after (application date - 2 years) and at most
//    the application date itself. A future start date is not valid.
//
// NOT covered here:
//  - The "SOURCE MISSING" fields: they are always null today; there is nothing to assert yet.
//  - @Transactional(readOnly = true).
//
// ROOT CAUSE OF MOST RED TESTS BEFORE THE FIX:
//   ReportingWindow.from() calls results.stream().filter(null). Stream.filter(null) throws a
//   NullPointerException immediately, even for an empty list, so getRowDetail always fails and
//   getRowsByProject fails as soon as one reduction result exists. UT-AMS-09-01 and UT-AMS-09-05
//   are the two Pattern B tests that prove it. Every other test that goes through getRowDetail
//   (blocks 07, 08, 09) also stays red until the one-line fix is applied, with the same cause.
//
// Entities are Mockito mocks with lenient getter stubs, so the tests do not depend on setters.
// Strict stubs are on. Repositories are left unstubbed when the default is what the test needs
// (empty List, Optional.empty(), 0), which keeps each test focused on what it changes.
//
// IMPORTS: copy the project-specific imports (entities, repositories, DTOs, EligibilityStatus,
// ReportReadiness, EquipmentFileType, ProjectService, ...) from EnR002IndividualActivityServiceImpl.
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kct.jcredit.core_backend.dto.response.CompanyActivityGroupResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002IndividualActivityDetailResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002IndividualActivityRowResponse;
import com.kct.jcredit.core_backend.entity.EnR002ApplicationConfirmation;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResult;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.enums.EligibilityStatus;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.enums.ReportReadiness;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002ApplicationConfirmationRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.service.ProjectService;
import com.kct.jcredit.core_backend.service.impl.EnR002IndividualActivityServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnR002IndividualActivityServiceImpl のテスト")
class EnR002IndividualActivityServiceImplTest {

    private static final String USER_ID = "operator01";
    private static final String PROJECT_PUBLIC_ID = "PROJPUBLIC1";
    private static final Integer PROJECT_ID = 1;
    private static final Integer EQUIPMENT_ID = 100;
    private static final Integer PARTICIPATION_ID = 500;
    private static final LocalDateTime APPLIED_AT = LocalDateTime.of(2026, 4, 1, 9, 0);
    private static final LocalDate APPLIED_ON = APPLIED_AT.toLocalDate();

    @Mock private ProjectRepository projectRepository;
    @Mock private ParticipatingProjectRepository participatingProjectRepository;
    @Mock private ParticipatingCompanyRepository participatingCompanyRepository;
    @Mock private EnR002EquipmentRepository equipmentRepository;
    @Mock private EnR002EquipmentFileRepository equipmentFileRepository;
    @Mock private EnR002ReductionResultRepository reductionResultRepository;
    @Mock private EnR002ApplicationConfirmationRepository applicationConfirmationRepository;
    @Mock private ProjectService projectService;

    @InjectMocks
    private EnR002IndividualActivityServiceImpl service;

    // ==================================================================
    // 05. getRowsByProject
    // ==================================================================

    @Test
    @DisplayName("UT-AMS-05-01: 正常 - 設備ごとに1行が返り、会社・サイト・住所・蓄電池・適格性が設定される")
    void getRowsByProject_valid_returnsRowWithDetails() {
        Project project = givenProject();
        EnR002Equipment equipment = defaultEquipment();
        ParticipatingProject participation = participation(PARTICIPATION_ID, project, equipment, true);
        when(participatingProjectRepository.findByProject_Id(PROJECT_ID)).thenReturn(List.of(participation));

        List<EnR002IndividualActivityRowResponse> rows =
                service.getRowsByProject(PROJECT_PUBLIC_ID, null, USER_ID);

        assertThat(rows).hasSize(1);
        EnR002IndividualActivityRowResponse row = rows.get(0);
        assertThat(row.equipmentId()).isEqualTo(EQUIPMENT_ID);
        assertThat(row.companyId()).isEqualTo("C001");
        assertThat(row.companyName()).isEqualTo("株式会社テスト");
        assertThat(row.siteId()).isEqualTo("SITEPUBLIC1");
        assertThat(row.siteAddress()).isEqualTo("熊本県 熊本市 1-1 ビルA");
        assertThat(row.hasBattery()).isFalse();
        assertThat(row.eligibilityStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(row.reportReadiness()).isEqualTo(ReportReadiness.NO_BATCH_RESULT);
        verify(projectService).assertProject(PROJECT_PUBLIC_ID, USER_ID);
    }

    @Test
    @DisplayName("UT-AMS-05-02: 正常 - 会社IDフィルタが無い場合は、プロジェクト全体の参加情報を取得する")
    void getRowsByProject_noCompanyFilter_loadsWholeProject() {
        givenProject();
        when(participatingProjectRepository.findByProject_Id(PROJECT_ID)).thenReturn(List.of());

        service.getRowsByProject(PROJECT_PUBLIC_ID, null, USER_ID);

        verify(participatingProjectRepository).findByProject_Id(PROJECT_ID);
        verify(participatingProjectRepository, never())
                .findByProject_IdAndEquipment_Site_Company_CompanyId(any(), anyString());
    }

    @Test
    @DisplayName("UT-AMS-05-03: 正常 - 会社IDフィルタがある場合は、その会社の参加情報だけを取得する")
    void getRowsByProject_withCompanyFilter_loadsOnlyThatCompany() {
        givenProject();
        when(participatingProjectRepository
                .findByProject_IdAndEquipment_Site_Company_CompanyId(PROJECT_ID, "C001"))
                .thenReturn(List.of());

        service.getRowsByProject(PROJECT_PUBLIC_ID, "C001", USER_ID);

        verify(participatingProjectRepository, never()).findByProject_Id(any());
    }

    @Test
    @DisplayName("UT-AMS-05-04: 境界 - 参加情報が0件の場合、空リストを返し、削減結果の検索は行わない")
    void getRowsByProject_noParticipations_returnsEmptyWithoutResultQuery() {
        givenProject();
        when(participatingProjectRepository.findByProject_Id(PROJECT_ID)).thenReturn(List.of());

        List<EnR002IndividualActivityRowResponse> rows =
                service.getRowsByProject(PROJECT_PUBLIC_ID, null, USER_ID);

        assertThat(rows).isEmpty();
        verifyNoInteractions(reductionResultRepository);
    }

    @Test
    @DisplayName("UT-AMS-05-05: 異常 - プロジェクトが存在しない場合、IllegalArgumentExceptionになる")
    void getRowsByProject_projectNotFound_throws() {
        when(projectRepository.findByPublicId("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRowsByProject("UNKNOWN", null, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    @DisplayName("UT-AMS-05-06: 異常 - プロジェクトへのアクセス権が無い場合、例外が伝わり、何も検索しない")
    void getRowsByProject_accessDenied_propagatesAndReadsNothing() {
        doThrow(new IllegalStateException("denied"))
                .when(projectService).assertProject(PROJECT_PUBLIC_ID, USER_ID);

        assertThatThrownBy(() -> service.getRowsByProject(PROJECT_PUBLIC_ID, null, USER_ID))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(projectRepository, participatingProjectRepository, reductionResultRepository);
    }

    @Test
    @DisplayName("UT-AMS-05-07: 正常 - 削減結果はプロジェクト単位で1回だけ取得する（設備ごとに検索しない）")
    void getRowsByProject_loadsReductionResultsOncePerProject() {
        Project project = givenProject();
        ParticipatingProject p1 = participation(501, project, equipment(101, defaultSite()), true);
        ParticipatingProject p2 = participation(502, project, equipment(102, defaultSite()), true);
        ParticipatingProject p3 = participation(503, project, equipment(103, defaultSite()), true);
        when(participatingProjectRepository.findByProject_Id(PROJECT_ID)).thenReturn(List.of(p1, p2, p3));

        service.getRowsByProject(PROJECT_PUBLIC_ID, null, USER_ID);

        verify(reductionResultRepository, times(1)).findByProjectIdAndIsActiveTrue(PROJECT_ID);
        verify(reductionResultRepository, never())
                .findByProjectIdAndEquipmentIdAndIsActiveTrue(any(), any());
    }

    @Test
    @DisplayName("UT-AMS-05-08: 正常 - 設備ごとに準備状況が決まる（結果なし/証憑なし/証憑あり）")
    void getRowsByProject_readinessIsResolvedPerEquipment() {
        Project project = givenProject();
        ParticipatingProject ready = participation(501, project, equipment(101, defaultSite()), true);
        ParticipatingProject missingEvidence = participation(502, project, equipment(102, defaultSite()), true);
        ParticipatingProject noResult = participation(503, project, equipment(103, defaultSite()), true);
        when(participatingProjectRepository.findByProject_Id(PROJECT_ID))
                .thenReturn(List.of(ready, missingEvidence, noResult));
        EnR002ReductionResult result101 = result(101, 1, APPLIED_ON);
        EnR002ReductionResult result102 = result(102, 1, APPLIED_ON);
        when(reductionResultRepository.findByProjectIdAndIsActiveTrue(PROJECT_ID))
                .thenReturn(List.of(result101, result102));
        when(equipmentFileRepository.countByEquipmentId(101)).thenReturn(2L);
        when(equipmentFileRepository.countByEquipmentId(102)).thenReturn(0L);

        List<EnR002IndividualActivityRowResponse> rows =
                service.getRowsByProject(PROJECT_PUBLIC_ID, null, USER_ID);

        assertThat(rows).extracting(EnR002IndividualActivityRowResponse::reportReadiness)
                .containsExactly(
                        ReportReadiness.READY,
                        ReportReadiness.MISSING_EVIDENCE,
                        ReportReadiness.NO_BATCH_RESULT);
    }

    @Test
    @DisplayName("UT-AMS-05-09: 境界 - サイトに会社が紐づいていない場合、会社IDと会社名はnullになる")
    void getRowsByProject_siteWithoutCompany_companyFieldsAreNull() {
        Project project = givenProject();
        EnR002Equipment equipment = equipment(EQUIPMENT_ID, site("SITEPUBLIC1", null));
        ParticipatingProject participation = participation(PARTICIPATION_ID, project, equipment, true);
        when(participatingProjectRepository.findByProject_Id(PROJECT_ID))
                .thenReturn(List.of(participation));

        EnR002IndividualActivityRowResponse row =
                service.getRowsByProject(PROJECT_PUBLIC_ID, null, USER_ID).get(0);

        assertThat(row.companyId()).isNull();
        assertThat(row.companyName()).isNull();
    }

    @Test
    @DisplayName("UT-AMS-05-10: 境界 - 蓄電池の有無がnullの場合は「なし」、適格性がnullの場合はPENDINGになる")
    void getRowsByProject_nullFlags_defaultToNoBatteryAndPending() {
        Project project = givenProject();
        EnR002Equipment equipment = defaultEquipment();
        lenient().when(equipment.getHasBattery()).thenReturn(null);
        ParticipatingProject participation = participation(PARTICIPATION_ID, project, equipment, null);
        when(participatingProjectRepository.findByProject_Id(PROJECT_ID))
                .thenReturn(List.of(participation));

        EnR002IndividualActivityRowResponse row =
                service.getRowsByProject(PROJECT_PUBLIC_ID, null, USER_ID).get(0);

        assertThat(row.hasBattery()).isFalse();
        assertThat(row.eligibilityStatus()).isEqualTo(EligibilityStatus.PENDING);
    }

    @Test
    @DisplayName("UT-AMS-05-11: 正常 - 適格がfalseの場合はNOT_ELIGIBLEになる")
    void getRowsByProject_notEligible_mapsToNotEligible() {
        Project project = givenProject();
        EnR002Equipment equipment = defaultEquipment();
        ParticipatingProject participation = participation(PARTICIPATION_ID, project, equipment, false);
        when(participatingProjectRepository.findByProject_Id(PROJECT_ID))
                .thenReturn(List.of(participation));

        EnR002IndividualActivityRowResponse row =
                service.getRowsByProject(PROJECT_PUBLIC_ID, null, USER_ID).get(0);

        assertThat(row.eligibilityStatus()).isEqualTo(EligibilityStatus.NOT_ELIGIBLE);
    }

    // ==================================================================
    // 06. getRowsByCompany
    // ==================================================================

    @Test
    @DisplayName("UT-AMS-06-01: 正常 - 参加情報がプロジェクトごとにまとめられ、最初に現れた順で返る")
    void getRowsByCompany_groupsByProjectInFirstSeenOrder() {
        ParticipatingCompany company = givenCompany();
        Project projectB = project(2, "PROJPUBLIC2", "P-002");
        Project projectA = project(PROJECT_ID, PROJECT_PUBLIC_ID, "P-001");
        ParticipatingProject b1 = participation(601, projectB, equipment(201, defaultSite()), true);
        ParticipatingProject a1 = participation(602, projectA, equipment(202, defaultSite()), true);
        ParticipatingProject b2 = participation(603, projectB, equipment(203, defaultSite()), true);
        when(participatingProjectRepository.findByEquipment_Site_Company_Id(company.getId()))
                .thenReturn(List.of(b1, a1, b2));
        givenAccessibleProjects("PROJPUBLIC1", "PROJPUBLIC2");

        List<CompanyActivityGroupResponse> groups =
                service.getRowsByCompany("C001", null, USER_ID);

        assertThat(groups).extracting(CompanyActivityGroupResponse::projectId)
                .containsExactly("PROJPUBLIC2", "PROJPUBLIC1");
        assertThat(groups.get(0).rows()).hasSize(2);
        assertThat(groups.get(1).rows()).hasSize(1);
    }

    @Test
    @DisplayName("UT-AMS-06-02: 異常 - 会社が存在しない場合、IllegalArgumentExceptionになり、参加情報は検索しない")
    void getRowsByCompany_companyNotFound_throws() {
        when(participatingCompanyRepository.findByCompanyId("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRowsByCompany("UNKNOWN", null, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNKNOWN");

        verifyNoInteractions(participatingProjectRepository);
    }

    @Test
    @DisplayName("UT-AMS-06-03: 正常 - プロジェクトIDフィルタがある場合、そのプロジェクトだけが返り、他は権限確認もしない")
    void getRowsByCompany_projectFilter_returnsOnlyThatProject() {
        ParticipatingCompany company = givenCompany();
        Project projectA = project(PROJECT_ID, PROJECT_PUBLIC_ID, "P-001");
        Project projectB = project(2, "PROJPUBLIC2", "P-002");
        ParticipatingProject a1 = participation(601, projectA, equipment(201, defaultSite()), true);
        ParticipatingProject b1 = participation(602, projectB, equipment(202, defaultSite()), true);
        when(participatingProjectRepository.findByEquipment_Site_Company_Id(company.getId()))
                .thenReturn(List.of(a1, b1));
        givenAccessibleProjects("PROJPUBLIC1", "PROJPUBLIC2");

        List<CompanyActivityGroupResponse> groups =
                service.getRowsByCompany("C001", "PROJPUBLIC1", USER_ID);

        assertThat(groups).extracting(CompanyActivityGroupResponse::projectId)
                .containsExactly("PROJPUBLIC1");
        verify(projectService, never()).hasProjectAccess(eq("PROJPUBLIC2"), anyString());
    }

    @Test
    @DisplayName("UT-AMS-06-04: 正常 - 権限の無いプロジェクトは、エラーにせず結果から除外される")
    void getRowsByCompany_inaccessibleProject_isSilentlyDropped() {
        ParticipatingCompany company = givenCompany();
        Project projectA = project(PROJECT_ID, PROJECT_PUBLIC_ID, "P-001");
        Project projectB = project(2, "PROJPUBLIC2", "P-002");
        ParticipatingProject a1 = participation(601, projectA, equipment(201, defaultSite()), true);
        ParticipatingProject b1 = participation(602, projectB, equipment(202, defaultSite()), true);
        when(participatingProjectRepository.findByEquipment_Site_Company_Id(company.getId()))
                .thenReturn(List.of(a1, b1));
        givenAccessibleProjects("PROJPUBLIC2");

        List<CompanyActivityGroupResponse> groups =
                service.getRowsByCompany("C001", null, USER_ID);

        assertThat(groups).extracting(CompanyActivityGroupResponse::projectId)
                .containsExactly("PROJPUBLIC2");
    }

    @Test
    @DisplayName("UT-AMS-06-05: 境界 - 全プロジェクトに権限が無い場合、例外ではなく空リストを返す")
    void getRowsByCompany_noAccessToAnyProject_returnsEmpty() {
        ParticipatingCompany company = givenCompany();
        Project projectA = project(PROJECT_ID, PROJECT_PUBLIC_ID, "P-001");
        ParticipatingProject a1 = participation(601, projectA, equipment(201, defaultSite()), true);
        when(participatingProjectRepository.findByEquipment_Site_Company_Id(company.getId()))
                .thenReturn(List.of(a1));
        givenAccessibleProjects();

        assertThat(service.getRowsByCompany("C001", null, USER_ID)).isEmpty();
    }

    @Test
    @DisplayName("UT-AMS-06-06: 境界 - 会社に参加情報が無い場合、空リストを返す")
    void getRowsByCompany_noParticipations_returnsEmpty() {
        ParticipatingCompany company = givenCompany();
        when(participatingProjectRepository.findByEquipment_Site_Company_Id(company.getId()))
                .thenReturn(List.of());

        assertThat(service.getRowsByCompany("C001", null, USER_ID)).isEmpty();
    }

    @Test
    @DisplayName("UT-AMS-06-07: 正常 - 削減結果はプロジェクトごとに、そのプロジェクトのIDで取得する")
    void getRowsByCompany_loadsReductionResultsPerProject() {
        ParticipatingCompany company = givenCompany();
        Project projectA = project(PROJECT_ID, PROJECT_PUBLIC_ID, "P-001");
        Project projectB = project(2, "PROJPUBLIC2", "P-002");
        ParticipatingProject a1 = participation(601, projectA, equipment(201, defaultSite()), true);
        ParticipatingProject b1 = participation(602, projectB, equipment(202, defaultSite()), true);
        when(participatingProjectRepository.findByEquipment_Site_Company_Id(company.getId()))
                .thenReturn(List.of(a1, b1));
        givenAccessibleProjects("PROJPUBLIC1", "PROJPUBLIC2");

        service.getRowsByCompany("C001", null, USER_ID);

        verify(reductionResultRepository, times(1)).findByProjectIdAndIsActiveTrue(PROJECT_ID);
        verify(reductionResultRepository, times(1)).findByProjectIdAndIsActiveTrue(2);
    }


    @Test
    @DisplayName("UT-AMS-06-08: 異常 - グループのprojectNameには、プロジェクト番号ではなくプロジェクト名が入る【要修正】")
    void getRowsByCompany_groupProjectName_shouldBeProjectNameNotNumber() {
        // Pattern B: the component is projectName, but the code passes getProjectNumber().
        // If the screen really needs the number, rename the field instead.
        ParticipatingCompany company = givenCompany();
        Project projectA = project(PROJECT_ID, PROJECT_PUBLIC_ID, "P-001");
        ParticipatingProject a1 = participation(601, projectA, equipment(201, defaultSite()), true);
        when(participatingProjectRepository.findByEquipment_Site_Company_Id(company.getId()))
                .thenReturn(List.of(a1));
        givenAccessibleProjects("PROJPUBLIC1");

        List<CompanyActivityGroupResponse> groups = service.getRowsByCompany("C001", null, USER_ID);

        assertThat(groups.get(0).projectName()).isEqualTo("project-1");
    }
    // ==================================================================
    // 07. getRowDetail - lookup and sections
    // ==================================================================

    @Test
    @DisplayName("UT-AMS-07-01: 異常 - 設備が存在しない場合、IndividualActivityNotFoundExceptionになる")
    void getRowDetail_equipmentNotFound_throws() {
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID))
                .isInstanceOf(EnR002IndividualActivityServiceImpl.IndividualActivityNotFoundException.class)
                .hasMessageContaining(String.valueOf(EQUIPMENT_ID));
    }

    @Test
    @DisplayName("UT-AMS-07-02: 異常 - 設備はあるが参加情報が無い場合、NotFoundExceptionになる（権限確認は先に行われる）")
    void getRowDetail_noParticipation_throwsAfterAccessCheck() {
        EnR002Equipment equipment = defaultEquipment();
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment));
        when(participatingProjectRepository.findByEquipment_Id(EQUIPMENT_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID))
                .isInstanceOf(EnR002IndividualActivityServiceImpl.IndividualActivityNotFoundException.class);

        verify(projectService).assertProject(PROJECT_PUBLIC_ID, USER_ID);
    }

    @Test
    @DisplayName("UT-AMS-07-03: 異常 - アクセス権が無い場合、設備の存在有無を知らせず、何も検索しない【要修正】")
    void getRowDetail_accessDenied_shouldNotTouchRepositories() {
        // Pattern B: the access check must come first. Today the equipment and participation are
        // read before it, so a forbidden caller can tell "equipment does not exist" (NotFound) from
        // "exists but forbidden" (access error) and enumerate equipment ids.
        doThrow(new IllegalStateException("denied"))
                .when(projectService).assertProject(PROJECT_PUBLIC_ID, USER_ID);

        assertThatThrownBy(() -> service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(equipmentRepository, participatingProjectRepository, reductionResultRepository);
    }

    @Test
    @DisplayName("UT-AMS-07-04: 正常 - 権限確認は、指定されたプロジェクト公開IDとユーザーIDで行われる")
    void getRowDetail_checksAccessWithRequestedProject() {
        givenDetailTarget(defaultEquipment());

        service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID);

        verify(projectService).assertProject(PROJECT_PUBLIC_ID, USER_ID);
    }

    @Test
    @DisplayName("UT-AMS-07-05: 正常 - 会員情報に会社・サイト・住所・申込日（作成日）・適格性が設定される")
    void getRowDetail_memberInfo_isFilled() {
        givenDetailTarget(defaultEquipment());

        EnR002IndividualActivityDetailResponse.MemberInfo member =
                service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).member();

        assertThat(member.companyId()).isEqualTo("C001");
        assertThat(member.companyName()).isEqualTo("株式会社テスト");
        assertThat(member.siteId()).isEqualTo("SITEPUBLIC1");
        assertThat(member.address()).isEqualTo("熊本県 熊本市 1-1 ビルA");
        assertThat(member.membershipApplicationDate()).isEqualTo(APPLIED_ON);
        assertThat(member.eligibilityStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
    }

    @Test
    @DisplayName("UT-AMS-07-06: 正常 - 入会届の確認情報がある場合、事業者区分フラグが設定される")
    void getRowDetail_confirmationPresent_flagsAreMapped() {
        ParticipatingProject participation = givenDetailTarget(defaultEquipment());
        EnR002ApplicationConfirmation confirmation = mock(EnR002ApplicationConfirmation.class);
        lenient().when(confirmation.getIsKeidanrenParticipation()).thenReturn(true);
        lenient().when(confirmation.getIsPeriodicReportTypeOne()).thenReturn(true);
        lenient().when(confirmation.getIsEmitter()).thenReturn(false);
        lenient().when(confirmation.getEmitterCode()).thenReturn("EM-001");
        when(applicationConfirmationRepository.findByParticipationId(participation.getId()))
                .thenReturn(Optional.of(confirmation));

        EnR002IndividualActivityDetailResponse.BusinessOperatorFlags flags =
                service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).member().businessOperatorFlags();

        assertThat(flags).isNotNull();
        assertThat(flags.carbonNeutralActionPlan()).isTrue();
        assertThat(flags.energySavingActSpecifiedOperator()).isTrue();
        assertThat(flags.warmingActSpecifiedEmitter()).isFalse();
        assertThat(flags.emitterCode()).isEqualTo("EM-001");
    }

    @Test
    @DisplayName("UT-AMS-07-07: 境界 - 入会届の確認情報が無い場合、事業者区分フラグはnullになる")
    void getRowDetail_confirmationMissing_flagsAreNull() {
        givenDetailTarget(defaultEquipment());

        assertThat(service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).member().businessOperatorFlags()).isNull();
    }

    @Test
    @DisplayName("UT-AMS-07-08: 正常 - 蓄電池ありの場合、蓄電池情報が設定される")
    void getRowDetail_withBattery_batteryInfoIsFilled() {
        EnR002Equipment equipment = defaultEquipment();
        lenient().when(equipment.getHasBattery()).thenReturn(true);
        lenient().when(equipment.getBatteryMaker()).thenReturn("MakerX");
        lenient().when(equipment.getBatteryModel()).thenReturn("ModelY");
        givenDetailTarget(equipment);

        EnR002IndividualActivityDetailResponse.BatteryInfo battery =
                service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).equipment().battery();

        assertThat(battery).isNotNull();
        assertThat(battery.manufacturer()).isEqualTo("MakerX");
        assertThat(battery.model()).isEqualTo("ModelY");
    }

    @Test
    @DisplayName("UT-AMS-07-09: 境界 - 蓄電池なし（false/null）の場合、蓄電池情報はnullになる")
    void getRowDetail_noBattery_batteryInfoIsNull() {
        EnR002Equipment equipment = defaultEquipment();
        lenient().when(equipment.getHasBattery()).thenReturn(null);
        givenDetailTarget(equipment);

        assertThat(service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).equipment().battery()).isNull();
    }

    @Test
    @DisplayName("UT-AMS-07-10: 正常 - 変更日がある場合は変更日、無い場合は運用開始日が有効開始日になる")
    void getRowDetail_effectiveFrom_usesChangeDateThenOperationDate() {
        EnR002Equipment changed = defaultEquipment();
        LocalDate changeDate = LocalDate.of(2026, 1, 15);
        lenient().when(changed.getChangeDate()).thenReturn(changeDate);
        givenDetailTarget(changed);

        assertThat(service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).equipment().effectiveFrom())
                .isEqualTo(changeDate);
    }

    @Test
    @DisplayName("UT-AMS-07-11: 境界 - 変更日が無い場合、有効開始日は運用開始日になる")
    void getRowDetail_effectiveFrom_fallsBackToOperationDate() {
        EnR002Equipment equipment = defaultEquipment();
        givenDetailTarget(equipment);

        assertThat(service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).equipment().effectiveFrom())
                .isEqualTo(equipment.getEquipmentOpDate());
    }

    @Test
    @DisplayName("UT-AMS-07-12: 正常 - 後継バージョンがある場合、その変更日が有効終了日になる")
    void getRowDetail_effectiveTo_isSuccessorChangeDate() {
        EnR002Equipment equipment = defaultEquipment();
        givenDetailTarget(equipment);
        EnR002Equipment successor = mock(EnR002Equipment.class);
        LocalDate successorChange = LocalDate.of(2026, 6, 1);
        lenient().when(successor.getChangeDate()).thenReturn(successorChange);
        when(equipmentRepository.findByPreviousEquipmentId(EQUIPMENT_ID)).thenReturn(Optional.of(successor));

        assertThat(service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).equipment().effectiveTo())
                .isEqualTo(successorChange);
    }

    @Test
    @DisplayName("UT-AMS-07-13: 境界 - 後継バージョンが無い場合、有効終了日はnullになる")
    void getRowDetail_noSuccessor_effectiveToIsNull() {
        givenDetailTarget(defaultEquipment());

        assertThat(service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).equipment().effectiveTo()).isNull();
    }

    @Test
    @DisplayName("UT-AMS-07-14: 異常 - 指定プロジェクトに参加していない設備は、他プロジェクトの情報を返さずNotFoundになる【要修正】")
    void getRowDetail_equipmentOfOtherProject_shouldThrowNotFound() {
        // Pattern B: the project id is required now. An equipment of project 2 must not be shown
        // through project 1. Today findFirst() returns whatever participation comes first.
        EnR002Equipment equipment = defaultEquipment();
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment));
        Project other = project(2, "PROJPUBLIC2", "P-002");
        ParticipatingProject participation = participation(501, other, equipment, true);
        when(participatingProjectRepository.findByEquipment_Id(EQUIPMENT_ID))
                .thenReturn(List.of(participation));

        assertThatThrownBy(() -> service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID))
                .isInstanceOf(EnR002IndividualActivityServiceImpl.IndividualActivityNotFoundException.class);
    }

    @Test
    @DisplayName("UT-AMS-07-15: 正常 - 参加情報が複数あっても、指定プロジェクトの参加情報で権限確認と削減結果の取得を行う")
    void getRowDetail_usesParticipationOfRequestedProject() {
        EnR002Equipment equipment = defaultEquipment();
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment));
        Project other = project(2, "PROJPUBLIC2", "P-002");
        Project requested = project(PROJECT_ID, PROJECT_PUBLIC_ID, "P-001");
        ParticipatingProject otherParticipation = participation(501, other, equipment, true);
        ParticipatingProject requestedParticipation = participation(502, requested, equipment, true);
        when(participatingProjectRepository.findByEquipment_Id(EQUIPMENT_ID))
                .thenReturn(List.of(otherParticipation, requestedParticipation));

        service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID);

        verify(projectService).assertProject(PROJECT_PUBLIC_ID, USER_ID);
        verify(projectService, never()).assertProject(eq("PROJPUBLIC2"), anyString());
        verify(reductionResultRepository)
                .findByProjectIdAndEquipmentIdAndIsActiveTrue(PROJECT_ID, EQUIPMENT_ID);
    }

    @Test
    @DisplayName("UT-AMS-07-16: 異常 - 証憑ファイルの種類は、APIの他の箇所と同じ値（solar_contract）で返す【要修正】")
    void getRowDetail_evidenceFileType_shouldUseApiValueNotEnumName() {
        // Pattern B: EquipmentFileType is serialised as "solar_contract" (@JsonValue) everywhere
        // else, but FileRef.fileType is filled with name() = "SOLAR_CONTRACT": two spellings
        // of the same type. Fix: use file.getFileType().getValue().
        givenDetailTarget(defaultEquipment());
        EnR002EquipmentFile file = mock(EnR002EquipmentFile.class);
        lenient().when(file.getFileType()).thenReturn(EquipmentFileType.SOLAR_CONTRACT);
        lenient().when(file.getS3Key()).thenReturn("equipment/100/contract.pdf");
        when(equipmentFileRepository.findByEquipmentId(EQUIPMENT_ID)).thenReturn(List.of(file));

        List<EnR002IndividualActivityDetailResponse.FileRef> files =
                service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).equipment().evidenceFiles();

        assertThat(files).hasSize(1);
        assertThat(files.get(0).fileType()).isEqualTo("solar_contract");
    }

    @Test
    @DisplayName("UT-AMS-07-17: 正常 - 証憑ファイルのurlには、現状S3キーがそのまま入る【要確認】")
    void getRowDetail_evidenceFileUrl_currentlyHoldsRawS3Key() {
        // 要確認: the component is named url but receives the raw S3 key. Decide whether the API
        // returns a presigned URL or the field is renamed to s3Key, then flip or rename.
        givenDetailTarget(defaultEquipment());
        EnR002EquipmentFile file = mock(EnR002EquipmentFile.class);
        lenient().when(file.getFileType()).thenReturn(EquipmentFileType.SOLAR_CONTRACT);
        lenient().when(file.getS3Key()).thenReturn("equipment/100/contract.pdf");
        when(equipmentFileRepository.findByEquipmentId(EQUIPMENT_ID)).thenReturn(List.of(file));

        List<EnR002IndividualActivityDetailResponse.FileRef> files =
                service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).equipment().evidenceFiles();

        assertThat(files.get(0).url()).isEqualTo("equipment/100/contract.pdf");
    }

    // ==================================================================
    // 08. getRowDetail - registration requirements
    // ==================================================================

    @Test
    @DisplayName("UT-AMS-08-01: 正常 - 都道府県が「熊本県」の場合、日本国内で実施はtrueになる")
    void getRowDetail_japanesePrefecture_inJapanIsTrue() {
        givenDetailTarget(defaultEquipment());

        assertThat(service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).registrationRequirements().operatingInJapan())
                .isTrue();
    }

    @Test
    @DisplayName("UT-AMS-08-02: 異常 - 都道府県が日本の形式でない場合、日本国内で実施はfalseになる")
    void getRowDetail_nonJapanesePrefecture_inJapanIsFalse() {
        Site foreign = site("SITEPUBLIC1", defaultCompany());
        lenient().when(foreign.getPrefecture()).thenReturn("California");
        givenDetailTarget(equipment(EQUIPMENT_ID, foreign));

        assertThat(service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).registrationRequirements().operatingInJapan())
                .isFalse();
    }

    @Test
    @DisplayName("UT-AMS-08-03: 境界 - 都道府県がnullの場合、日本国内で実施はfalseになる（例外にならない）")
    void getRowDetail_nullPrefecture_inJapanIsFalse() {
        Site noPrefecture = site("SITEPUBLIC1", defaultCompany());
        lenient().when(noPrefecture.getPrefecture()).thenReturn(null);
        givenDetailTarget(equipment(EQUIPMENT_ID, noPrefecture));

        assertThat(service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).registrationRequirements().operatingInJapan())
                .isFalse();
    }

    @Test
    @DisplayName("UT-AMS-08-04: 境界 - 運用開始日が申込日のちょうど2年前の場合、2年以内はtrueになる")
    void getRowDetail_operationExactlyTwoYearsBefore_withinTwoYearsIsTrue() {
        EnR002Equipment equipment = defaultEquipment();
        lenient().when(equipment.getEquipmentOpDate()).thenReturn(APPLIED_ON.minusYears(2));
        givenDetailTarget(equipment);

        assertThat(service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).registrationRequirements().withinTwoYearsOfApplication())
                .isTrue();
    }

    @Test
    @DisplayName("UT-AMS-08-05: 境界 - 運用開始日が申込日の2年と1日前の場合、2年以内はfalseになる")
    void getRowDetail_operationOneDayOverTwoYears_withinTwoYearsIsFalse() {
        EnR002Equipment equipment = defaultEquipment();
        lenient().when(equipment.getEquipmentOpDate()).thenReturn(APPLIED_ON.minusYears(2).minusDays(1));
        givenDetailTarget(equipment);

        assertThat(service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).registrationRequirements().withinTwoYearsOfApplication())
                .isFalse();
    }

    @Test
    @DisplayName("UT-AMS-08-06: 境界 - 運用開始日がnullの場合、2年以内はfalseになる（例外にならない）")
    void getRowDetail_nullOperationDate_withinTwoYearsIsFalse() {
        EnR002Equipment equipment = defaultEquipment();
        lenient().when(equipment.getEquipmentOpDate()).thenReturn(null);
        givenDetailTarget(equipment);

        assertThat(service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).registrationRequirements().withinTwoYearsOfApplication())
                .isFalse();
    }

    @Test
    @DisplayName("UT-AMS-08-07: 異常 - 運用開始日が申込日より後（未来）の場合、2年以内はfalseになる【要修正】")
    void getRowDetail_operationAfterApplication_withinTwoYearsShouldBeFalse() {
        // Pattern B: the rule is "2 years before the application date, at most the same day".
        // isWithinTwoYears only has the lower bound, so a future start date counts as valid.
        EnR002Equipment equipment = defaultEquipment();
        lenient().when(equipment.getEquipmentOpDate()).thenReturn(APPLIED_ON.plusDays(1));
        givenDetailTarget(equipment);

        assertThat(service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID)
                .registrationRequirements().withinTwoYearsOfApplication())
                .as("運用開始日は申込日と同日までしか認められない")
                .isFalse();
    }

    @Test
    @DisplayName("UT-AMS-08-08: 境界 - 運用開始日が申込日と同日の場合、2年以内はtrueになる")
    void getRowDetail_operationOnApplicationDay_withinTwoYearsIsTrue() {
        EnR002Equipment equipment = defaultEquipment();
        lenient().when(equipment.getEquipmentOpDate()).thenReturn(APPLIED_ON);
        givenDetailTarget(equipment);

        assertThat(service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID)
                .registrationRequirements().withinTwoYearsOfApplication())
                .isTrue();
    }

    // ==================================================================
    // 09. getRowDetail / getRowsByProject - reporting window
    // ==================================================================

    @Test
    @DisplayName("UT-AMS-09-01: 異常 - 削減結果が0件でも詳細は取得でき、開始日・終了日ブロックはゼロになる【要修正】")
    void getRowDetail_noReductionResults_shouldNotThrowAndReportsZeroBlocks() {
        // Pattern B: ReportingWindow.from() calls results.stream().filter(null). Stream.filter(null)
        // throws NullPointerException at once, even for an empty list, so no detail can be built.
        givenDetailTarget(defaultEquipment());

        assertThatCode(() -> service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID)).doesNotThrowAnyException();

        EnR002IndividualActivityDetailResponse.MonitoringInfo monitoring =
                service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).monitoring();
        assertThat(monitoring.windowStart().generationKwh()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(monitoring.windowStart().saleKwh()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(monitoring.windowEnd().generationKwh()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(monitoring.windowEnd().saleKwh()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("UT-AMS-09-02: 正常 - 終了日ブロックは最新バッチの結果になり、開始日ブロック（未報告扱い）はゼロになる")
    void getRowDetail_withResults_latestFillsEndAndStartStaysZero() {
        givenDetailTarget(defaultEquipment());
        LocalDate older = LocalDate.of(2026, 1, 31);
        LocalDate newer = LocalDate.of(2026, 2, 28);
        EnR002ReductionResult batch1 = result(EQUIPMENT_ID, 1, older);
        EnR002ReductionResult batch2 = result(EQUIPMENT_ID, 2, newer);
        when(reductionResultRepository.findByProjectIdAndEquipmentIdAndIsActiveTrue(PROJECT_ID, EQUIPMENT_ID))
                .thenReturn(List.of(batch1, batch2));

        EnR002IndividualActivityDetailResponse.MonitoringInfo monitoring =
                service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).monitoring();

        assertThat(monitoring.windowEnd().readingDate()).isEqualTo(newer);
        // isReported is not in the schema yet, so every row is a first-time report.
        assertThat(monitoring.windowStart().readingDate()).isNull();
        assertThat(monitoring.windowStart().generationKwh()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("UT-AMS-09-03: 正常 - 最新は、リストの並びに関係なく最大のバッチIDの結果になる")
    void getRowDetail_latestIsHighestBatchRegardlessOfOrder() {
        givenDetailTarget(defaultEquipment());
        LocalDate newest = LocalDate.of(2026, 3, 31);
        EnR002ReductionResult batch3 = result(EQUIPMENT_ID, 3, newest);
        EnR002ReductionResult batch1 = result(EQUIPMENT_ID, 1, LocalDate.of(2026, 1, 31));
        EnR002ReductionResult batch2 = result(EQUIPMENT_ID, 2, LocalDate.of(2026, 2, 28));
        when(reductionResultRepository.findByProjectIdAndEquipmentIdAndIsActiveTrue(PROJECT_ID, EQUIPMENT_ID))
                .thenReturn(List.of(batch1, batch3, batch2));

        assertThat(service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).monitoring().windowEnd().readingDate())
                .isEqualTo(newest);
    }

    @Test
    @DisplayName("UT-AMS-09-04: 境界 - バッチIDがnullの結果は最古として扱われ、非nullの結果が最新になる")
    void getRowDetail_nullBatchId_isTreatedAsOldest() {
        givenDetailTarget(defaultEquipment());
        LocalDate dated = LocalDate.of(2026, 2, 28);
        EnR002ReductionResult noBatch = result(EQUIPMENT_ID, null, LocalDate.of(2026, 3, 31));
        EnR002ReductionResult batch2 = result(EQUIPMENT_ID, 2, dated);
        when(reductionResultRepository.findByProjectIdAndEquipmentIdAndIsActiveTrue(PROJECT_ID, EQUIPMENT_ID))
                .thenReturn(List.of(noBatch, batch2));

        assertThat(service.getRowDetail(EQUIPMENT_ID, PROJECT_PUBLIC_ID, USER_ID).monitoring().windowEnd().readingDate())
                .isEqualTo(dated);
    }

    @Test
    @DisplayName("UT-AMS-09-05: 異常 - 削減結果がある設備を含む一覧でも、例外にならず行が返る【要修正】")
    void getRowsByProject_withReductionResults_shouldNotThrow() {
        // Pattern B: same root cause as 05-01. loadReportingWindows() calls ReportingWindow.from()
        // for each equipment that has results, so the whole list fails with NullPointerException.
        Project project = givenProject();
        EnR002Equipment equipment = defaultEquipment();
        ParticipatingProject participation = participation(PARTICIPATION_ID, project, equipment, true);
        when(participatingProjectRepository.findByProject_Id(PROJECT_ID))
                .thenReturn(List.of(participation));
        EnR002ReductionResult batch1 = result(EQUIPMENT_ID, 1, LocalDate.of(2026, 1, 31));
        when(reductionResultRepository.findByProjectIdAndIsActiveTrue(PROJECT_ID))
                .thenReturn(List.of(batch1));

        assertThatCode(() -> service.getRowsByProject(PROJECT_PUBLIC_ID, null, USER_ID))
                .doesNotThrowAnyException();
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private Project givenProject() {
        Project project = project(PROJECT_ID, PROJECT_PUBLIC_ID, "P-001");
        when(projectRepository.findByPublicId(PROJECT_PUBLIC_ID)).thenReturn(Optional.of(project));
        return project;
    }

    private ParticipatingCompany givenCompany() {
        ParticipatingCompany company = defaultCompany();
        when(participatingCompanyRepository.findByCompanyId("C001")).thenReturn(Optional.of(company));
        return company;
    }

    /** Stubs the project lookup for getRowDetail and returns the participation it will find. */
    private ParticipatingProject givenDetailTarget(EnR002Equipment equipment) {
        Project project = project(PROJECT_ID, PROJECT_PUBLIC_ID, "P-001");
        ParticipatingProject participation = participation(PARTICIPATION_ID, project, equipment, true);
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment));
        when(participatingProjectRepository.findByEquipment_Id(EQUIPMENT_ID)).thenReturn(List.of(participation));
        return participation;
    }

    /** One stub for every call, so strict stubs never see a call without a matching stub. */
    private void givenAccessibleProjects(String... accessiblePublicIds) {
        Set<String> accessible = Set.of(accessiblePublicIds);
        when(projectService.hasProjectAccess(anyString(), eq(USER_ID)))
                .thenAnswer(invocation -> accessible.contains(invocation.<String>getArgument(0)));
    }

    private Project project(Integer id, String publicId, String projectNumber) {
        Project project = mock(Project.class);
        lenient().when(project.getId()).thenReturn(id);
        lenient().when(project.getPublicId()).thenReturn(publicId);
        lenient().when(project.getProjectNumber()).thenReturn(projectNumber);
        lenient().when(project.getProjectName()).thenReturn("project-" + id);
        return project;
    }

    private ParticipatingCompany defaultCompany() {
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        lenient().when(company.getId()).thenReturn(10);
        lenient().when(company.getCompanyId()).thenReturn("C001");
        lenient().when(company.getNameJa()).thenReturn("株式会社テスト");
        return company;
    }

    private Site site(String publicId, ParticipatingCompany company) {
        Site site = mock(Site.class);
        lenient().when(site.getPublicId()).thenReturn(publicId);
        lenient().when(site.getCompany()).thenReturn(company);
        lenient().when(site.getPrefecture()).thenReturn("熊本県");
        lenient().when(site.getMunicipality()).thenReturn("熊本市");
        lenient().when(site.getBlocknum()).thenReturn("1-1");
        lenient().when(site.getBuildingName()).thenReturn("ビルA");
        return site;
    }

    private Site defaultSite() {
        ParticipatingCompany company = defaultCompany();
        return site("SITEPUBLIC1", company);
    }

    private EnR002Equipment defaultEquipment() {
        Site site = defaultSite();
        return equipment(EQUIPMENT_ID, site);
    }

    private EnR002Equipment equipment(Integer id, Site site) {
        EnR002Equipment equipment = mock(EnR002Equipment.class);
        lenient().when(equipment.getId()).thenReturn(id);
        lenient().when(equipment.getSite()).thenReturn(site);
        lenient().when(equipment.getHasBattery()).thenReturn(false);
        lenient().when(equipment.getEquipmentOpDate()).thenReturn(APPLIED_ON.minusMonths(6));
        return equipment;
    }

    private ParticipatingProject participation(
            Integer id, Project project, EnR002Equipment equipment, Boolean eligible) {
        ParticipatingProject participation = mock(ParticipatingProject.class);
        lenient().when(participation.getId()).thenReturn(id);
        lenient().when(participation.getProject()).thenReturn(project);
        lenient().when(participation.getEquipment()).thenReturn(equipment);
        lenient().when(participation.getIsEligible()).thenReturn(eligible);
        lenient().when(participation.getCreatedAt()).thenReturn(APPLIED_AT);
        return participation;
    }

    private EnR002ReductionResult result(Integer equipmentId, Integer batchId, LocalDate processingDate) {
        EnR002ReductionResult result = mock(EnR002ReductionResult.class);
        lenient().when(result.getEquipmentId()).thenReturn(equipmentId);
        lenient().when(result.getBatchId()).thenReturn(batchId);
        lenient().when(result.getProcessingDate()).thenReturn(processingDate);
        return result;
    }
}