package com.kct.jcredit.core_backend.service.batch.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kct.jcredit.core_backend.entity.EnR002ReductionResult;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResultTotal;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultTotalRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.batch.calculator.SolarEmissionsCalculator;
import com.kct.jcredit.core_backend.service.batch.repository.SolarMonitoringRepositoryAdapter;
import com.kct.jcredit.core_backend.entity.BatchProcessHistory;
import com.kct.jcredit.core_backend.repository.postgres.BatchProcessHistoryRepository;
import com.kct.jcredit.core_backend.service.batch.BatchHistoryService;


/**
 * SolarBatchProcessService adalah orchestrator, jadi test difokuskan pada 4 hook method yang
 * dia implementasikan dari BaseBatchProcessService: saveSiteResults, saveProjectTotal,
 * aggregateEmissions, getActiveProjects, getSitesForProject.
 *
 * Nilai-nilai dummy pada aggregateEmissions memakai 4 site dari project 1 (data DynamoDB/SQL
 * yang sudah diverifikasi sebelumnya) supaya totalnya bisa dicek manual:
 *  site1 empj=0.018, site2 empj=0.025, site3 empj=0.009, site4 empj=0.029 -> total 0.081
 * (di sini saya pakai angka hari pertama tiap site sebagai representasi satu baris per site).
 */
@ExtendWith(MockitoExtension.class)
class BatchProcessServiceTest {

    @Mock
    private SolarMonitoringRepositoryAdapter solarRepository;
    @Mock
    private SolarEmissionsCalculator solarCalculator;
    @Mock
    private EnR002ReductionResultRepository resultRepository;
    @Mock
    private EnR002ReductionResultTotalRepository resultTotalRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private SiteProjectRelationRepository siteProjectRelationRepo;
    @Mock
    private BatchHistoryServiceImpl batchHistoryService;
    @Mock
    private CsvLogServiceImpl csvLogService;
    @Mock
    private NotificationService notificationService;

    @org.mockito.InjectMocks
    private SolarBatchProcessService service;

    // ---------- saveSiteResults ----------

    @Test
    @DisplayName("saveSiteResults persists each equipment result and rolls up totals from multiple rows")
    void saveSiteResults_persistsEachEquipmentResultAndBuildsAggregateTotals() {
        EnR002ReductionResult first = EnR002ReductionResult.builder()
            .projectId(1)
            .siteId(2)
            .batchId(3)
            .processingDate(LocalDate.of(2026, 7, 22))
            .empj(new BigDecimal("120.50"))
            .empjSControl(new BigDecimal("80.25"))
            .empjSBattery(new BigDecimal("40.25"))
            .build();
        EnR002ReductionResult second = EnR002ReductionResult.builder()
            .projectId(1)
            .siteId(2)
            .batchId(3)
            .processingDate(LocalDate.of(2026, 7, 22))
            .empj(new BigDecimal("79.50"))
            .empjSControl(new BigDecimal("19.75"))
            .empjSBattery(new BigDecimal("9.75"))
            .build();

        when(resultRepository.save(any(EnR002ReductionResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Object saved = service.saveSiteResults(1, 2, 3, LocalDate.of(2026, 7, 22), List.of(first, second));

        ArgumentCaptor<EnR002ReductionResult> captor = ArgumentCaptor.forClass(EnR002ReductionResult.class);
        verify(resultRepository, times(2)).save(captor.capture());

        List<EnR002ReductionResult> persisted = captor.getAllValues();
        assertThat(persisted).hasSize(2);
        assertThat(persisted).extracting(EnR002ReductionResult::getProjectId).containsExactly(1, 1);
        assertThat(persisted).extracting(EnR002ReductionResult::getSiteId).containsExactly(2, 2);
        assertThat(persisted).allSatisfy(result -> assertThat(result.getIsActive()).isTrue());

        assertThat(saved).isInstanceOf(List.class);
        assertThat((List<?>) saved).hasSize(2);

        EnR002ReductionResultTotal total = (EnR002ReductionResultTotal) service.aggregateEmissions(List.of(first, second));
        assertThat(total.getEmpj()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(total.getEmpjSControl()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(total.getEmpjSBattery()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("saveSiteResults returns an empty list and never calls save when there are no results")
    void saveSiteResults_emptyList_returnsEmptyAndSkipsSave() {
        Object saved = service.saveSiteResults(1, 3, 1, LocalDate.of(2026, 7, 1), Collections.emptyList());

        assertThat((List<?>) saved).isEmpty();
        verify(resultRepository, never()).save(any());
    }

    @Test
    @DisplayName("saveSiteResults returns an empty list when results is null")
    void saveSiteResults_nullList_returnsEmptyAndSkipsSave() {
        Object saved = service.saveSiteResults(1, 3, 1, LocalDate.of(2026, 7, 1), null);

        assertThat((List<?>) saved).isEmpty();
        verify(resultRepository, never()).save(any());
    }

    // ---------- aggregateEmissions ----------

    @Test
    @DisplayName("aggregateEmissions sums empj/empjSControl/empjSBattery across all 4 dummy sites (day 1)")
    void aggregateEmissions_sumsAllFourSites() {
        List<EnR002ReductionResult> dayOneResults = List.of(
            resultOf(1, "0.018", "0.011", "0.007"),
            resultOf(2, "0.025", "0.016", "0.009"),
            resultOf(3, "0.009", "0.009", "0.000"),
            resultOf(4, "0.029", "0.022", "0.007")
        );

        EnR002ReductionResultTotal total = (EnR002ReductionResultTotal) service.aggregateEmissions(dayOneResults);

        assertThat(total.getEmpj()).isEqualByComparingTo("0.081");
        assertThat(total.getEmpjSControl()).isEqualByComparingTo("0.058");
        assertThat(total.getEmpjSBattery()).isEqualByComparingTo("0.023");
        assertThat(total.getProjectId()).isEqualTo(1);
    }

    @Test
    @DisplayName("aggregateEmissions treats null numeric fields as zero instead of throwing")
    void aggregateEmissions_treatsNullFieldsAsZero() {
        // Site 3 tidak punya changedBattery/empjSBattery pada data dummy -> field bisa null,
        // bukan cuma BigDecimal.ZERO, tergantung bagaimana calculator mengisinya.
        EnR002ReductionResult noBattery = EnR002ReductionResult.builder()
            .projectId(1).siteId(3)
            .empj(new BigDecimal("0.009"))
            .empjSControl(new BigDecimal("0.009"))
            .empjSBattery(null)
            .build();

        EnR002ReductionResultTotal total = (EnR002ReductionResultTotal) service.aggregateEmissions(List.of(noBattery));

        assertThat(total.getEmpjSBattery()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(total.getEmpj()).isEqualByComparingTo("0.009");
    }

    @Test
    @DisplayName("aggregateEmissions on an empty list returns all-zero totals with a null projectId")
    void aggregateEmissions_emptyList_returnsZeroTotals() {
        EnR002ReductionResultTotal total = (EnR002ReductionResultTotal) service.aggregateEmissions(Collections.emptyList());

        assertThat(total.getEmpj()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(total.getEmpjSControl()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(total.getEmpjSBattery()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(total.getProjectId()).isNull();
    }

    private static EnR002ReductionResult resultOf(int siteId, String empj, String control, String battery) {
        return EnR002ReductionResult.builder()
            .projectId(1)
            .siteId(siteId)
            .empj(new BigDecimal(empj))
            .empjSControl(new BigDecimal(control))
            .empjSBattery(new BigDecimal(battery))
            .build();
    }

    // ---------- saveProjectTotal ----------

    @Test
    @DisplayName("saveProjectTotal creates a brand-new row when the project has no existing total")
    void saveProjectTotal_createsNewRowWhenNoneExists() {
        when(resultTotalRepository.findByProjectId(1)).thenReturn(Optional.empty());
        when(resultTotalRepository.save(any(EnR002ReductionResultTotal.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        EnR002ReductionResultTotal aggregated = EnR002ReductionResultTotal.builder()
            .empj(new BigDecimal("0.081"))
            .empjSControl(new BigDecimal("0.058"))
            .empjSBattery(new BigDecimal("0.023"))
            .build();

        Object saved = service.saveProjectTotal(1, 1, LocalDate.of(2026, 7, 10), List.of(), aggregated);

        ArgumentCaptor<EnR002ReductionResultTotal> captor = ArgumentCaptor.forClass(EnR002ReductionResultTotal.class);
        verify(resultTotalRepository).save(captor.capture());
        EnR002ReductionResultTotal persisted = captor.getValue();

        assertThat(persisted.getProjectId()).isEqualTo(1);
        assertThat(persisted.getBatchId()).isEqualTo(1);
        assertThat(persisted.getIsActive()).isTrue();
        assertThat(persisted.getEmpj()).isEqualByComparingTo("0.081");
        assertThat(saved).isSameAs(persisted);
    }

    @Test
    @DisplayName("saveProjectTotal updates the existing row in place instead of inserting a duplicate")
    void saveProjectTotal_updatesExistingRow() {
        EnR002ReductionResultTotal existing = EnR002ReductionResultTotal.builder()
            .batchId(7)
            .projectId(1)
            .batchId(1) // batch lama
            .empj(new BigDecimal("0.404")) // total kumulatif sebelumnya
            .build();

        when(resultTotalRepository.findByProjectId(1)).thenReturn(Optional.of(existing));
        when(resultTotalRepository.save(any(EnR002ReductionResultTotal.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        EnR002ReductionResultTotal aggregated = EnR002ReductionResultTotal.builder()
            .empj(new BigDecimal("0.500"))
            .empjSControl(new BigDecimal("0.350"))
            .empjSBattery(new BigDecimal("0.150"))
            .elbl(new BigDecimal("25000.00"))
            .er(new BigDecimal("11.000"))
            .build();

        service.saveProjectTotal(1, 9, LocalDate.of(2026, 7, 11), List.of(), aggregated);

        ArgumentCaptor<EnR002ReductionResultTotal> captor = ArgumentCaptor.forClass(EnR002ReductionResultTotal.class);
        verify(resultTotalRepository).save(captor.capture());
        EnR002ReductionResultTotal persisted = captor.getValue();

        // Baris yang sama (id=7) di-update, bukan insert baru
        assertThat(persisted.getId()).isEqualTo(7);
        assertThat(persisted.getBatchId()).isEqualTo(9);
        assertThat(persisted.getEmpj()).isEqualByComparingTo("0.500");
        assertThat(persisted.getElbl()).isEqualByComparingTo("25000.00");
        assertThat(persisted.getEr()).isEqualByComparingTo("11.000");
    }

    // ---------- getActiveProjects ----------

    @Test
    @DisplayName("getActiveProjects filters out inactive projects and returns only active project IDs")
    void getActiveProjects_filtersInactiveProjects() {
        Project active1 = Project.builder().Id(1).isActive(true).build();
        Project active2 = Project.builder().Id(2).isActive(true).build();
        Project inactive = Project.builder().Id(3).isActive(false).build();

        when(projectRepository.findAll()).thenReturn(List.of(active1, active2, inactive));

        List<Integer> result = service.getActiveProjects();

        assertThat(result).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    @DisplayName("getActiveProjects returns an empty list when there are no projects at all")
    void getActiveProjects_noProjects_returnsEmpty() {
        when(projectRepository.findAll()).thenReturn(Collections.emptyList());

        List<Integer> result = service.getActiveProjects();

        assertThat(result).isEmpty();
    }

    // ---------- getSitesForProject ----------

    @Test
    @DisplayName("getSitesForProject maps site-project relations to their site IDs, matching the 4 dummy sites")
    void getSitesForProject_mapsRelationsToSiteIds() {
        SiteProjectRelation r1 = relationFor(1);
        SiteProjectRelation r2 = relationFor(2);
        SiteProjectRelation r3 = relationFor(3);
        SiteProjectRelation r4 = relationFor(4);

        when(siteProjectRelationRepo.findByProjectId(1)).thenReturn(List.of(r1, r2, r3, r4));

        List<Integer> result = service.getSitesForProject(1);

        assertThat(result).containsExactly(1, 2, 3, 4);
    }

    @Test
    @DisplayName("getSitesForProject returns an empty list for a project with no linked sites")
    void getSitesForProject_noRelations_returnsEmpty() {
        when(siteProjectRelationRepo.findByProjectId(5)).thenReturn(Collections.emptyList());

        List<Integer> result = service.getSitesForProject(5);

        assertThat(result).isEmpty();
    }

    private static SiteProjectRelation relationFor(int siteId) {
        Site site = Site.builder().id(siteId).build();
        return SiteProjectRelation.builder().site(site).build();
    }
}