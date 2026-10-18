package com.kct.jcredit.core_backend.service.batch.factory;

import org.springframework.stereotype.Component;

import com.kct.jcredit.core_backend.service.batch.BaseBatchProcessService;
import com.kct.jcredit.core_backend.service.batch.impl.SolarBatchProcessService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating batch process services based on methodology type.
 * 
 * This allows selecting the correct processor at runtime based on project type.
 * 
 * Usage:
 * ```
 * BaseBatchProcessService<?> processor = factory.getProcessor("EnR002");
 * processor.executeAllProjects(LocalDate.now());
 * ```
 * 
 * To add a new methodology:
 * 1. Create new monitoring record entity (e.g., MonitoringTableRecordWind)
 * 2. Create new repository adapter implementing GenericMonitoringRepository<T>
 * 3. Create new calculator implementing EmissionsCalculator<T>
 * 4. Create new concrete service extending BaseBatchProcessService<T>
 * 5. Add case in getProcessor() method below
 * 
 * @author Modernization Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchProcessServiceFactory {
    
    private final SolarBatchProcessService solarBatchProcessService;
    // Add more service injections here as new methodologies are added
    // private final WindBatchProcessService windBatchProcessService;
    // private final HydroBatchProcessService hydroBatchProcessService;
    
    /**
     * Gets the appropriate batch processor for a methodology type.
     * 
     * @param methodologyType Code like "EnR002", "EnR003", "EnR004"
     * @return Configured batch processor service
     * @throws IllegalArgumentException if methodology type is unknown
     */
    public BaseBatchProcessService<?, ?> getProcessor(String methodologyType) {
        switch (methodologyType.toUpperCase()) {
            case "ENR002":
                log.debug("Creating Solar (EnR002) batch processor");
                return solarBatchProcessService;
            
            // Add new methodologies here:
            // case "ENR003":
            //     log.debug("Creating Wind (EnR003) batch processor");
            //     return windBatchProcessService;
            //
            // case "ENR004":
            //     log.debug("Creating Hydro (EnR004) batch processor");
            //     return hydroBatchProcessService;
            
            default:
                throw new IllegalArgumentException(
                    "Unknown methodology type: " + methodologyType +
                    ". Supported: EnR002"
                );
        }
    }
    
    /**
     * Gets processor for a project based on its methodology type.
     * 
     * @param project Project entity
     * @return Configured batch processor service
     * @throws IllegalArgumentException if project's methodology is unsupported
     */
    public BaseBatchProcessService<?, ?> getProcessorForProject(
            com.kct.jcredit.core_backend.entity.Project project) {
        
        String methodology = extractMethodologyType(project);
        return getProcessor(methodology);
    }
    
    /**
     * Extracts methodology type from project.
     * Subclasses should override this if project stores methodology differently.
     * 
     * @param project Project to extract type from
     * @return Methodology type code (e.g., "EnR002")
     */
    private String extractMethodologyType(com.kct.jcredit.core_backend.entity.Project project) {
        // TODO: Update this based on how your Project entity stores methodology
        // This is a placeholder - adjust based on your actual schema
        return "EnR002"; // Hardcoded for now, but should come from project
    }
}
