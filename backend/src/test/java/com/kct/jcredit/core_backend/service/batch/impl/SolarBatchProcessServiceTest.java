package com.kct.jcredit.core_backend.service.batch.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kct.jcredit.core_backend.entity.EnR002ReductionResult;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResultTotal;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultTotalRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.batch.calculator.SolarEmissionsCalculator;
import com.kct.jcredit.core_backend.service.batch.repository.SolarMonitoringRepositoryAdapter;

@ExtendWith(MockitoExtension.class)
class SolarBatchProcessServiceTest {

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

    @InjectMocks
    private SolarBatchProcessService service;

    @Test
    @DisplayName("persists each equipment result and rolls up totals from multiple rows")
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

        // This mirrors the batch flow more closely: one site can produce multiple equipment rows,
        // and the totals should roll up from all of those rows.
        EnR002ReductionResultTotal total = (EnR002ReductionResultTotal) service.aggregateEmissions(List.of(first, second));
        assertThat(total.getEmpj()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(total.getEmpjSControl()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(total.getEmpjSBattery()).isEqualByComparingTo(new BigDecimal("50.00"));
    }
}
