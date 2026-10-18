package com.kct.jcredit.core_backend.service.project;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.ProjectMonitoringResponse;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResultTotal;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultTotalRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.service.impl.ProjectMonitoringServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProjectMonitoringService - Unit Tests")
class ProjectMonitoringTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private EnR002ReductionResultTotalRepository reductionRepository;

    @InjectMocks
    private ProjectMonitoringServiceImpl monitoringService;

    private Project dummyProject;
    private OperatorCompany dummyCompany;
    private EnR002ReductionResultTotal dummyMonitoringData;

    @BeforeEach
    void setUp() {
        dummyCompany = new OperatorCompany();
        dummyCompany.setNameJa("テスト株式会社");

        dummyProject = new Project();
        dummyProject.setId(123);
        dummyProject.setProjectNumber("P001");
        dummyProject.setProjectName("熊本太陽光プロジェクト");
        dummyProject.setProjectStatus("承認済");
        dummyProject.setOperatorCompany(dummyCompany);
        dummyProject.setMethodCategory("EN");
        dummyProject.setMethodNumber("R002");
        dummyProject.setMethodologyVersion("1.0");
        dummyProject.setMethodName("太陽光発電");
        dummyProject.setMembersTotal(5);
        dummyProject.setBaselineEmission(1000.0);
        dummyProject.setEmissionReduction(450.0);

        dummyMonitoringData = new EnR002ReductionResultTotal();
        dummyMonitoringData = new EnR002ReductionResultTotal();
        dummyMonitoringData.setProjectId(123);
        dummyMonitoringData.setProcessingDate(LocalDate.of(2026, 4, 1));
        dummyMonitoringData.setStartTime(Instant.parse("2026-03-01T00:00:00Z"));
        dummyMonitoringData.setEndTime(Instant.parse("2026-03-31T23:59:59Z"));
        
        dummyMonitoringData.setEmpj(new BigDecimal("550.0"));
    }

    // ==========================================================
    // 1. SKENARIO NORMAL (HAPPY PATH)
    // ==========================================================

    @Test
    @DisplayName("TC-MON-01: Sukses mengambil detail monitoring dengan data lengkap")
    void getProjectDetail_success_withFullData() {
        // Given
        when(projectRepository.findById(123)).thenReturn(Optional.of(dummyProject));
        when(reductionRepository.findAll(any(), any(Sort.class)))
                        .thenReturn(List.of(dummyMonitoringData));

        // When
        ProjectMonitoringResponse response = monitoringService.getProjectDetail("123");

        // Then
        assertNotNull(response);
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertNotNull(response.getDetail());
        assertEquals("P001", response.getDetail().getHeader().getProjectId());
        assertEquals("テスト株式会社", response.getDetail().getHeader().getApplicant());
        assertEquals("熊本太陽光プロジェクト", response.getDetail().getHeader().getProjectType()); 
        assertEquals("承認済", response.getDetail().getHeader().getApprovalMethod()); 
        assertEquals("承認済", response.getDetail().getHeader().getCreditStatus()); 
        assertEquals("2026-03-01", response.getDetail().getHeader().getProjectPeriod().getStartDate());
        assertEquals(1, response.getDetail().getChart().getChartData().size());
        assertEquals(1000, response.getDetail().getChart().getChartData().get(0).getBaseline());
        assertEquals(550, response.getDetail().getChart().getChartData().get(0).getActualEmission());
        assertEquals(450, response.getDetail().getChart().getChartData().get(0).getReduction());
        assertEquals("2026年", response.getDetail().getChart().getChartData().get(0).getYear());
        assertNotNull(response.getDetail().getChart().getTooltipInfo().getLastUpdated());
        assertEquals(450, response.getDetail().getTotalWidget().getTotalReduction().getValue());
    }

    // ==========================================================
    // 2. SCENARIO ERROR 
    // ==========================================================

    @Test
    @DisplayName("TC-MON-02: Gagal karena Project tidak ditemukan")
    void getProjectDetail_fails_whenProjectNotFound() {
        // Given
        when(projectRepository.findById(999)).thenReturn(Optional.empty());

        // When
        ProjectMonitoringResponse response = monitoringService.getProjectDetail("999");

        // Then
        assertNotNull(response);
        assertEquals(ResponseConstants.NOT_FOUND, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_PROJECT_NOT_FOUND, response.getMessage());
        assertNull(response.getDetail());

        verify(reductionRepository, never()).findAll(any(), any(Sort.class));
    }

    // ==========================================================
    // 3. SKENARIO EKSTREM / DATA MINIMAL 
    // ==========================================================

    @Test
    @DisplayName("TC-MON-03: Sukses walau data null semua (Project tanpa company & tanpa monitoring)")
    void getProjectDetail_success_withMinimalNullData() {
        // Given
        dummyProject.setOperatorCompany(null);
        dummyProject.setProjectName(null);
        dummyProject.setProjectStatus(null);
        dummyProject.setMembersTotal(null);
        dummyProject.setBaselineEmission(null);
        dummyProject.setEmissionReduction(null);

        when(projectRepository.findById(123)).thenReturn(Optional.of(dummyProject));
        when(reductionRepository.findAll(any(), any(Sort.class)))
                .thenReturn(new ArrayList<>()); //monitoring dta = null 

        // When
        ProjectMonitoringResponse response = monitoringService.getProjectDetail("123");

        // Then
        assertEquals(ResponseConstants.OK, response.getStatusCode());
    
        assertEquals("-", response.getDetail().getHeader().getApplicant());
        assertEquals("プログラム事業", response.getDetail().getHeader().getProjectType());
        assertEquals("簡易 (簡易方法)", response.getDetail().getHeader().getApprovalMethod());
        assertEquals("未発行", response.getDetail().getHeader().getCreditStatus());
   
        assertEquals(0, response.getDetail().getHeader().getParticipantCount());
        assertEquals(0, response.getDetail().getTotalWidget().getTotalReduction().getValue());
        assertEquals(0, response.getDetail().getTotalWidget().getClickThrough().getInitialEstimate());
        
        assertEquals(0, response.getDetail().getChart().getChartData().size());
        assertEquals("-", response.getDetail().getHeader().getProjectPeriod().getStartDate());
    }

    @Test
    @DisplayName("TC-MON-04: Sukses walau data tanggal monitoring null (Mencegah NullPointerException)")
    void getProjectDetail_success_whenMonitoringDatesAreNull() {
        // Given
        dummyMonitoringData.setStartTime(null);
        dummyMonitoringData.setEndTime(null);
        dummyMonitoringData.setProcessingDate(null);
        dummyMonitoringData.setEmpj(null); // Actual emission null

        when(projectRepository.findById(123)).thenReturn(Optional.of(dummyProject));
        when(reductionRepository.findAll(any(), any(Sort.class)))
                .thenReturn(List.of(dummyMonitoringData));

        // When
        ProjectMonitoringResponse response = monitoringService.getProjectDetail("123");

        // Then
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertEquals("-", response.getDetail().getHeader().getProjectPeriod().getStartDate());
        assertEquals("-", response.getDetail().getHeader().getProjectPeriod().getEndDate());
        assertEquals("-年", response.getDetail().getChart().getChartData().get(0).getYear());
        assertEquals(0, response.getDetail().getChart().getChartData().get(0).getActualEmission());
    }
}
