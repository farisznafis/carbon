package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.dto.response.ProjectMonitoringResponse;
import com.kct.jcredit.core_backend.dto.response.details.ProjectDetailMonitoringResponse.*;
import com.kct.jcredit.core_backend.dto.response.details.ProjectDetailMonitoringResponse;
import com.kct.jcredit.core_backend.service.ProjectMonitoringService;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository; 
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultTotalRepository; 

import com.kct.jcredit.core_backend.entity.Project; 
import com.kct.jcredit.core_backend.entity.EnR002ReductionResultTotal; 
import com.kct.jcredit.core_backend.constant.ResponseConstants;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;

import java.time.ZoneId;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectMonitoringServiceImpl implements ProjectMonitoringService {

    private final ProjectRepository projectRepository;
    private final EnR002ReductionResultTotalRepository reductionRepository;

    @Override
    @Transactional(readOnly = true)
    public ProjectMonitoringResponse getProjectDetail(String projectId) {
        
        ProjectMonitoringResponse response = new ProjectMonitoringResponse();
        Integer idDatabase = Integer.valueOf(projectId); 
        
        // 1. take project data from database
        Project project = projectRepository.findById(idDatabase).orElse(null);
        if (project == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
            return response;
        }

        // 2. graphic data (chart) & total reduction widget
        EnR002ReductionResultTotal probe = new EnR002ReductionResultTotal();
        probe.setProjectId(idDatabase);
        List<EnR002ReductionResultTotal> monitoringData = reductionRepository.findAll(
            Example.of(probe), Sort.by(Sort.Direction.ASC, "processingDate")
        );

        ProjectDetailMonitoringResponse detail = new ProjectDetailMonitoringResponse();

        detail.setHeader(buildHeaderInfo(project, monitoringData)); 
        detail.setChart(buildChartReal(project, monitoringData));   
        detail.setTotalWidget(buildTotalWidgetReal(project));       

        response.setDetail(detail);
        response.setStatusCode(ResponseConstants.OK);
        
        return response;
    }

    private ProjectHeader buildHeaderInfo(Project project, List<EnR002ReductionResultTotal> monitoringData) {
        ProjectHeader header = new ProjectHeader();
        
        // status header
        header.setProjectId(project.getProjectNumber());
        header.setProjectName(project.getProjectName());
        header.setStatus(project.getProjectStatus()); 
        
        if (project.getOperatorCompany() != null) {
            header.setApplicant(project.getOperatorCompany().getNameJa()); 
        } else {
            header.setApplicant("-");
        }

        String methodologyStr = String.format("%s-%s Ver %s (%s)", 
            project.getMethodCategory(), project.getMethodNumber(), 
            project.getMethodologyVersion(), project.getMethodName());
        header.setMethodology(methodologyStr);
        header.setRegistrationNumber(project.getProjectNumber());
        header.setParticipantCount(project.getMembersTotal() != null ? project.getMembersTotal() : 0);
        header.setProjectType(project.getProjectName() != null ? project.getProjectName() : "プログラム事業"); 
        header.setApprovalMethod(project.getProjectStatus() != null ? project.getProjectStatus() : "簡易 (簡易方法)"); 
        header.setCreditStatus(project.getProjectStatus() != null ? project.getProjectStatus() : "未発行"); 

        ProjectPeriod period = new ProjectPeriod();
        if (monitoringData != null && !monitoringData.isEmpty()) {
            EnR002ReductionResultTotal firstData = monitoringData.get(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("Asia/Tokyo"));
            String startDate = firstData.getStartTime() != null ? formatter.format(firstData.getStartTime()) : "-";
            String endDate = firstData.getEndTime() != null ? formatter.format(firstData.getEndTime()) : "-";
            
            period.setStartDate(startDate); 
            period.setEndDate(endDate);
        } else {
            period.setStartDate("-"); 
            period.setEndDate("-");
        }
        header.setProjectPeriod(period);

        return header;
    }

    private ReductionChart buildChartReal(Project project, List<EnR002ReductionResultTotal> monitoringData) {
        ReductionChart chart = new ReductionChart();
        List<ChartData> chartDataList = new ArrayList<>();

        int baselineEmission = project.getBaselineEmission() != null ? project.getBaselineEmission().intValue() : 0;

        if (monitoringData != null) {
            for (EnR002ReductionResultTotal data : monitoringData) {
                ChartData chartData = new ChartData();
                if (data.getProcessingDate() != null) {
                    chartData.setYear(data.getProcessingDate().getYear() + "年");
                } else {
                    chartData.setYear("-年");
                }
                
                chartData.setBaseline(baselineEmission);
                int actualEmpj = data.getEmpj() != null ? data.getEmpj().intValue() : 0;
                chartData.setActualEmission(actualEmpj);
                
                chartData.setReduction(baselineEmission - actualEmpj); 
                chartDataList.add(chartData);
            }
        }
        chart.setChartData(chartDataList);

        TooltipInfo tooltip = new TooltipInfo();
        LocalDate now = LocalDate.now(ZoneId.of("Asia/Tokyo"));
        String dynamicLastUpdated = DateTimeFormatter.ofPattern("yyyy年MM月").format(now);
        tooltip.setLastUpdated(dynamicLastUpdated);
        
        tooltip.setSource("IoTセンサー"); 
        chart.setTooltipInfo(tooltip);

        return chart;
    }

    private TotalReductionWidget buildTotalWidgetReal(Project project) {
        TotalReductionWidget widget = new TotalReductionWidget();
        ReductionValue reductionValue = new ReductionValue();
        
        int totalReduction = project.getEmissionReduction() != null ? project.getEmissionReduction().intValue() : 0;
        int baseline = project.getBaselineEmission() != null ? project.getBaselineEmission().intValue() : 0;
        
        reductionValue.setValue(totalReduction);
        reductionValue.setUnit("t-CO2");
        widget.setTotalReduction(reductionValue);

        ClickThrough clickThrough = new ClickThrough();
        clickThrough.setInitialEstimate(baseline);
        clickThrough.setCurrentTotal(totalReduction);
        clickThrough.setUnit("削減量");
        widget.setClickThrough(clickThrough);

        return widget;
    }
}