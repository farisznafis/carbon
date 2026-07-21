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
     * Calculates emissions for a single site based on monitoring data.
     * 
     * This is the main calculation entry point. Implementations must:
     * 1. Aggregate monitoring data (sum, averages, etc.)
     * 2. Apply methodology-specific formulas
     * 3. Handle edge cases (no data, invalid data, etc.)
     * 4. Return a fully calculated result object for the methodology
     * 
     * @param projectId Project ID
     * @param siteId Site ID
     * @param batchId Batch ID
     * @param monitoringData List of monitoring records for the site
     * @param processingDate Date being processed
     * @return Methodology-specific result object, or null when no data is available
     * @throws IllegalArgumentException if data is invalid
     */
    R calculateEmissions(
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
