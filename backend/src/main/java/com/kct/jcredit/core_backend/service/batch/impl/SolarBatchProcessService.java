package com.kct.jcredit.core_backend.service.batch.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.service.batch.BaseBatchProcessService;
import com.kct.jcredit.core_backend.service.batch.BatchHistoryService;
import com.kct.jcredit.core_backend.service.batch.CsvLogService;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.batch.calculator.EmissionsCalculator;
import com.kct.jcredit.core_backend.service.batch.calculator.SolarEmissionsCalculator;
import com.kct.jcredit.core_backend.service.batch.repository.GenericMonitoringRepository;
import com.kct.jcredit.core_backend.service.batch.repository.SolarMonitoringRepositoryAdapter;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultTotalRepository;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResult;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResultTotal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Concrete solar (EnR002) batch processor.
 * 
 * Extends BaseBatchProcessService to provide:
 * - Solar-specific repository (GenericMonitoringRepository<EnR002MonitoringRecord>)
 * - Solar-specific calculator (SolarEmissionsCalculator)
 * - Solar result entity persistence (EnR002ReductionResult, EnR002ReductionResultTotal)
 * 
 * The base class handles all orchestration - this class only provides methodology-specific details.
 * 
 * @author Modernization Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SolarBatchProcessService extends BaseBatchProcessService<EnR002MonitoringRecord, EnR002ReductionResult> {
    
    // Dependencies
    private final SolarMonitoringRepositoryAdapter solarRepository;
    private final SolarEmissionsCalculator solarCalculator;
    private final EnR002ReductionResultRepository resultRepository;
    private final EnR002ReductionResultTotalRepository resultTotalRepository;
    private final ProjectRepository projectRepository;
    private final SiteProjectRelationRepository siteProjectRelationRepo;
    private final BatchHistoryServiceImpl batchHistoryService;
    private final CsvLogServiceImpl csvLogService;
    private final NotificationService notificationService;
    
    @Override
    protected GenericMonitoringRepository<EnR002MonitoringRecord> getMonitoringRepository() {
        return solarRepository;
    }
    
    @Override
    protected EmissionsCalculator<EnR002MonitoringRecord, EnR002ReductionResult> getCalculator() {
        return solarCalculator;
    }
    
    @Override
    protected BatchHistoryService getBatchHistoryService() {
        return batchHistoryService;
    }
    
    @Override
    protected CsvLogService getCsvLogService() {
        return csvLogService;
    }
    
    @Override
    protected NotificationService getNotificationService() {
        return notificationService;
    }
    
    @Override
    public Object saveSiteResults(
            Integer projectId,
            Integer siteId,
            Integer batchId,
            LocalDate processingDate,
            List<EnR002ReductionResult> results) {
        
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        
        List<EnR002ReductionResult> savedResults = results.stream()
            .map(result -> {
                result.setIsActive(true);
                return resultRepository.save(result);
            })
            .collect(Collectors.toList());
        
        log.debug("Saved {} solar result(s) for site {}", savedResults.size(), siteId);
        return savedResults;
    }
    
    @Override
    public Object saveProjectTotal(
            Integer projectId,
            Integer batchId,
            LocalDate processingDate,
            List<Object> siteResults,
            Object aggregated) {

        EnR002ReductionResultTotal projectTotal = (EnR002ReductionResultTotal) aggregated;

        EnR002ReductionResultTotal totalToSave = resultTotalRepository.findByProjectId(projectId)
            .map(existing -> {
                existing.setBatchId(batchId);
                existing.setProcessingDate(processingDate);
                existing.setEndTime(java.time.Instant.now());
                existing.setEmpj(projectTotal.getEmpj());
                existing.setEmpjM(projectTotal.getEmpjM());
                existing.setEmpjS(projectTotal.getEmpjS());
                existing.setEmpjSControl(projectTotal.getEmpjSControl());
                existing.setEmpjSBattery(projectTotal.getEmpjSBattery());
                existing.setElpj(projectTotal.getElpj());
                existing.setEmbl(projectTotal.getEmbl());
                existing.setElbl(projectTotal.getElbl());
                existing.setEr(projectTotal.getEr());
                existing.setIsActive(true);
                return existing;
            })
            .orElseGet(() -> {
                projectTotal.setProjectId(projectId);
                projectTotal.setBatchId(batchId);
                projectTotal.setProcessingDate(processingDate);
                projectTotal.setStartTime(java.time.Instant.now());
                projectTotal.setEndTime(java.time.Instant.now());
                projectTotal.setIsActive(true);
                return projectTotal;
            });

        EnR002ReductionResultTotal saved = resultTotalRepository.save(totalToSave);
        log.debug("Saved solar project total for project {}: empj={} empjSControl={} empjSBattery={}",
            projectId,
            saved.getEmpj(),
            saved.getEmpjSControl(),
            saved.getEmpjSBattery()
        );
        return saved;
    }
    
    @Override
    public Object aggregateEmissions(List<EnR002ReductionResult> results) {
        BigDecimal totalEmpj = BigDecimal.ZERO;
        BigDecimal totalEmpjM = BigDecimal.ZERO;
        BigDecimal totalEmpjS = BigDecimal.ZERO;
        BigDecimal totalEmpjSControl = BigDecimal.ZERO;
        BigDecimal totalEmpjSBattery = BigDecimal.ZERO;

        for (EnR002ReductionResult result : results) {
            if (result.getEmpj() != null) {
                totalEmpj = totalEmpj.add(result.getEmpj());
            }
            if (result.getEmpjM() != null) {
                totalEmpjM = totalEmpjM.add(result.getEmpjM());
            }
            if (result.getEmpjS() != null) {
                totalEmpjS = totalEmpjS.add(result.getEmpjS());
            }
            if (result.getEmpjSControl() != null) {
                totalEmpjSControl = totalEmpjSControl.add(result.getEmpjSControl());
            }
            if (result.getEmpjSBattery() != null) {
                totalEmpjSBattery = totalEmpjSBattery.add(result.getEmpjSBattery());
            }
        }

        EnR002ReductionResultTotal projectTotal = EnR002ReductionResultTotal.builder()
            .projectId(results.isEmpty() ? null : results.get(0).getProjectId())
            .batchId(null)
            .processingDate(null)
            .startTime(java.time.Instant.now())
            .endTime(java.time.Instant.now())
            .empj(totalEmpj)
            .empjM(totalEmpjM)
            .empjS(totalEmpjS)
            .empjSControl(totalEmpjSControl)
            .empjSBattery(totalEmpjSBattery)
            .build();

        projectTotal.setIsActive(true);
        return projectTotal;
    }
    
    @Override
    protected List<Integer> getActiveProjects() {
        List<Project> allProjects = projectRepository.findAll().stream()
            .filter(project -> project.getIsActive())
            .collect(Collectors.toList());
        
        List<Integer> solarProjectIds = allProjects.stream()
            .map(Project::getId)
            .collect(Collectors.toList());
        
        log.info("Found {} active solar projects", solarProjectIds.size());
        return solarProjectIds;
    }
    
    @Override
    protected List<Integer> getSitesForProject(Integer projectId) {
        List<Integer> siteIds = siteProjectRelationRepo.findByProjectId(projectId)
            .stream()
            .map(relation -> relation.getSite().getId())
            .collect(Collectors.toList());
        
        log.debug("Found {} sites for solar project {}", siteIds.size(), projectId);
        return siteIds;
    }
}
