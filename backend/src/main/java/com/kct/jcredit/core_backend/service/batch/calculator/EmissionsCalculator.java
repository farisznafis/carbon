package com.kct.jcredit.core_backend.service.batch.calculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.kct.jcredit.core_backend.entity.base.MonitoringRecord;

/**
 * Generic interface for emissions calculation across different methodologies.
 * Each methodology (solar, wind, hydro, etc.) implements this to provide
 * methodology-specific calculation logic.
 * 
 * @param <T> The monitoring record type to calculate from
 * @author Modernization Team
 */
public interface EmissionsCalculator<T extends MonitoringRecord, R> {
    
    /**
     * Calculates emissions for a site.
     * 
     * Implementations may return one or many methodology-specific results per site.
     * For equipment-based methodologies such as solar, this typically returns one
     * result per equipment so the batch layer can persist each row independently.
     * 
     * @param projectId Project ID
     * @param siteId Site ID
     * @param batchId Batch ID
     * @param monitoringData List of monitoring records for the site
     * @param processingDate Date being processed
     * @return List of methodology-specific result objects for the site
     * @throws IllegalArgumentException if data is invalid
     */
    List<R> calculateEmissions(
        Integer projectId,
        Integer siteId,
        Integer batchId,
        List<T> monitoringData,
        LocalDate processingDate
    );
    
    /**
     * Gets the methodology name for logging/identification.
     * Examples: "Solar (EnR002)", "Wind (EnR003)"
     */
    String getMethodologyName();
    
    /**
     * Gets the methodology type code.
     * Examples: "EnR002", "EnR003"
     */
    String getMethodologyType();
    
    /**
     * Validates that monitoring data is suitable for processing.
     * Can be used before calculation to fail fast.
     * 
     * @param monitoringData Data to validate
     * @return Error message if invalid, null if valid
     */
    default String validateData(List<T> monitoringData) {
        if (monitoringData == null || monitoringData.isEmpty()) {
            return "No monitoring data available";
        }
        return null;
    }
}
