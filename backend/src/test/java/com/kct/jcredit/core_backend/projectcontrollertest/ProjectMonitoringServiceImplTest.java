package com.kct.jcredit.core_backend.projectcontrollertest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResultTotal;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultTotalRepository;
import com.kct.jcredit.core_backend.service.impl.ProjectMonitoringServiceImpl;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.dto.response.ProjectMonitoringResponse;
import com.kct.jcredit.core_backend.dto.response.details.ProjectDetailMonitoringResponse.*;
// Scope note: getProjectDetail is the only method of this service, and it is reachable
// from ProjectController.
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectMonitoringServiceImpl のテスト")
class ProjectMonitoringServiceImplTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private EnR002ReductionResultTotalRepository reductionRepository;

    @InjectMocks
    private ProjectMonitoringServiceImpl monitoringService;

    private static final String PROJECT_ID = "PUB-PJ-001";
    private static final Integer PROJECT_DB_ID = 7;
    private static final String PROJECT_NAME = "熊本太陽光プロジェクト";
    private static final String PROJECT_STATUS = "公開";

    // ─────────────────────────────────────────────────────────────
    // Fixtures
    // ─────────────────────────────────────────────────────────────

    private Project projectMock(OperatorCompany operatorCompany) {
        Project project = mock(Project.class);
        lenient().when(project.getId()).thenReturn(PROJECT_DB_ID);
        lenient().when(project.getProjectName()).thenReturn(PROJECT_NAME);
        lenient().when(project.getProjectNumber()).thenReturn("PJ-2026-001");
        lenient().when(project.getProjectStatus()).thenReturn(PROJECT_STATUS);
        lenient().when(project.getOperatorCompany()).thenReturn(operatorCompany);
        lenient().when(project.getMembersTotal()).thenReturn(12);
        lenient().when(project.getMethodCategory()).thenReturn("EN");
        lenient().when(project.getMethodNumber()).thenReturn("R-002");
        lenient().when(project.getMethodologyVersion()).thenReturn("1.0");
        lenient().when(project.getMethodName()).thenReturn("太陽光発電");
        lenient().when(project.getBaselineEmission()).thenReturn(100.0);
        lenient().when(project.getEmissionReduction()).thenReturn(999.0);
        return project;
    }

    private OperatorCompany operatorCompanyMock() {
        OperatorCompany company = mock(OperatorCompany.class);
        lenient().when(company.getNameJa()).thenReturn("運営株式会社");
        return company;
    }

    private EnR002ReductionResultTotal reductionRow(int year, String empj) {
        EnR002ReductionResultTotal row = mock(EnR002ReductionResultTotal.class);
        lenient().when(row.getProcessingDate()).thenReturn(LocalDate.of(year, 4, 1));
        lenient().when(row.getEmpj()).thenReturn(new BigDecimal(empj));
        lenient().when(row.getStartTime())
                .thenReturn(LocalDate.of(year, 1, 1).atStartOfDay(ZoneId.of("Asia/Tokyo")).toInstant());
        lenient().when(row.getEndTime())
                .thenReturn(LocalDate.of(year, 12, 31).atStartOfDay(ZoneId.of("Asia/Tokyo")).toInstant());
        return row;
    }

    @SuppressWarnings("unchecked")
    private void stubReductionRows(List<EnR002ReductionResultTotal> rows) {
        when(reductionRepository.findAll(any(Example.class), any(Sort.class))).thenReturn(rows);
    }

    private Project stubProjectWithRows(List<EnR002ReductionResultTotal> rows) {
        Project project = projectMock(operatorCompanyMock());
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.of(project));
        stubReductionRows(rows);
        return project;
    }

    // ==================================================================
    // 10. getProjectDetail
    // ==================================================================

    @Test
    @DisplayName("UT-PS-10-01: 正常 - ヘッダー・グラフ・ウィジェットが揃って返る")
    void getProjectDetail_valid_returnsAllSections() {
        stubProjectWithRows(List.of(reductionRow(2026, "40.0")));

        ProjectMonitoringResponse response = monitoringService.getProjectDetail(PROJECT_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getDetail().getHeader()).isNotNull();
        assertThat(response.getDetail().getChart()).isNotNull();
        assertThat(response.getDetail().getTotalWidget()).isNotNull();
    }

    @Test
    @DisplayName("UT-PS-10-02: 異常 - プロジェクトが存在しない場合NOT_FOUNDを返し、集計も行わない")
    void getProjectDetail_notFound_returnsNotFoundWithoutQueryingData() {
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.empty());

        ProjectMonitoringResponse response = monitoringService.getProjectDetail(PROJECT_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getDetail()).isNull();
        verifyNoInteractions(reductionRepository);
    }

    @Test
    @DisplayName("UT-PS-10-03: 正常 - モニタリングデータが0件の場合、期間は「-」でグラフは空になる")
    void getProjectDetail_noMonitoringData_returnsPlaceholders() {
        stubProjectWithRows(List.of());

        ProjectMonitoringResponse response = monitoringService.getProjectDetail(PROJECT_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getDetail().getHeader().getProjectPeriod().getStartDate())
                .isEqualTo("-");
        assertThat(response.getDetail().getChart().getChartData()).isEmpty();
    }

    @Test
    @DisplayName("UT-PS-10-04: 正常 - 運営会社がnullの場合、申請者は「-」になる")
    void getProjectDetail_operatorCompanyNull_usesPlaceholder() {
        // This is the one place in the codebase that guards getOperatorCompany(); the same
        // field is dereferenced unguarded in ProjectServiceImpl.
        Project project = projectMock(null);
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.of(project));
        stubReductionRows(List.of());

        ProjectMonitoringResponse response = monitoringService.getProjectDetail(PROJECT_ID);

        assertThat(response.getDetail().getHeader().getApplicant()).isEqualTo("-");
    }

    @Test
    @DisplayName("UT-PS-10-05: 正常 - グラフは期間ごとに「ベースライン−実績」を削減量として返す")
    void getProjectDetail_chartComputesReductionPerPeriod() {
        stubProjectWithRows(List.of(reductionRow(2026, "40.0"), reductionRow(2027, "30.0")));

        ProjectMonitoringResponse response = monitoringService.getProjectDetail(PROJECT_ID);

        List<ChartData> chart = response.getDetail().getChart().getChartData();
        assertThat(chart).hasSize(2);
        assertThat(chart.get(0).getYear()).isEqualTo("2026年");
        assertThat(chart.get(0).getReduction()).isEqualTo(60);   // 100 - 40
        assertThat(chart.get(1).getReduction()).isEqualTo(70);   // 100 - 30
    }

    @Test
    @DisplayName("UT-PS-10-06: 異常 - プロジェクト種別・承認方法・クレジット状況は別々の値であるべき【要修正】")
    void getProjectDetail_headerFieldsShouldNotBeCopiesOfOtherFields() {
        // Pattern B: projectType is filled from the project NAME, and approvalMethod and
        // creditStatus are both filled from the project STATUS. Three labelled fields on
        // the monitoring screen therefore show two values copied from elsewhere. The
        // fallback literals in the code ("プログラム事業", "簡易 (簡易方法)", "未発行")
        // show the intent was three separate fields that the entity does not yet carry.
        stubProjectWithRows(List.of());

        ProjectHeader header =
                monitoringService.getProjectDetail(PROJECT_ID).getDetail().getHeader();

        assertThat(header.getProjectType())
                .as("プロジェクト種別はプロジェクト名の写しであってはならない")
                .isNotEqualTo(PROJECT_NAME);
        assertThat(header.getApprovalMethod())
                .as("承認方法とクレジット状況が同一の値であってはならない")
                .isNotEqualTo(header.getCreditStatus());
    }

    @Test
    @DisplayName("UT-PS-10-07: 異常 - 合計削減量はモニタリング実績を反映すべき【要修正】")
    void getProjectDetail_totalWidgetShouldReflectMonitoredData() {
        // Pattern B: totalReduction and clickThrough.currentTotal are both filled from
        // project.emissionReduction, which is computed at creation time as
        // baseline - postProjectEmission - a planned estimate, not a measured result.
        // buildTotalWidgetReal is not even given the monitoring rows. Showing planned
        // figures under the label "current total" misstates the position of a carbon
        // credit project.
        // Here the plan says 999 while the monitored periods add up to 60 + 70 = 130.
        stubProjectWithRows(List.of(reductionRow(2026, "40.0"), reductionRow(2027, "30.0")));

        TotalReductionWidget widget =
                monitoringService.getProjectDetail(PROJECT_ID).getDetail().getTotalWidget();

        assertThat(widget.getTotalReduction().getValue())
                .as("合計削減量は計画値ではなくモニタリング実績の合計であるべき")
                .isEqualTo(130);
        assertThat(widget.getClickThrough().getCurrentTotal())
                .as("currentTotalは実績であり、initialEstimateと別の出所であるべき")
                .isEqualTo(130);
    }

    @Test
    @DisplayName("UT-PS-10-08: 準正常 - Query by Exampleはプリミティブ項目を無視できない（要確認）")
    void getProjectDetail_queryByExampleProbeIsNotNullSafeForPrimitives() {
        // NOTE: the probe is a bare entity with only projectId set. QBE ignores null
        // fields, but a primitive field cannot be null, so any int/double/boolean column on
        // EnR002ReductionResultTotal silently becomes part of the filter at its zero value
        // and the query returns less than it should. Recorded until the entity is checked.
        Project project = stubProjectWithRows(List.of());

        monitoringService.getProjectDetail(PROJECT_ID);

        ArgumentCaptor<Example<EnR002ReductionResultTotal>> captor =
                ArgumentCaptor.forClass(Example.class);
        verify(reductionRepository).findAll(captor.capture(), any(Sort.class));
        assertThat(captor.getValue().getProbe().getProjectId()).isEqualTo(project.getId());
    }
}