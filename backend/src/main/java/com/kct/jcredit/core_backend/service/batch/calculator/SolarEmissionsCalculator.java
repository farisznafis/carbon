package com.kct.jcredit.core_backend.service.batch.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResult;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Solar (EnR002) methodology emissions calculator.
 * 
 * Implements CDM-based calculations for solar photovoltaic systems.
 * 
 * Key formulas:
 * 1. ELPJ,control = ELPV - ELPVR (Generation - Reverse flow)
 * 2. EM_PJ,M = 0 (Solar has no methane)
 * 3. EM_PJ,S,control = ELPJ,control × CEF(t)
 * 4. EM_PJ,S,battery = ELPJ,battery × CEF(t) (conditional on battery change or 1yr)
 * 5. EM_PJ,S = EM_PJ,S,control + EM_PJ,S,battery
 * 6. EM_PJ = EM_PJ,M + EM_PJ,S
 * 
 * @author Modernization Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SolarEmissionsCalculator implements EmissionsCalculator<EnR002MonitoringRecord, EnR002ReductionResult> {
    
    private final EnR002ReductionResultRepository reductionResultRepo;
    private final SiteProjectRelationRepository siteProjectRelationRepo;
    
    // CEF Configuration
    private static final BigDecimal CMO = new BigDecimal("0.000512"); // Marginal emission factor
    private static final Map<Integer, BigDecimal> CA_MAP = new HashMap<>() {{
        put(2020, new BigDecimal("0.000460"));
        put(2021, new BigDecimal("0.000444"));
        put(2022, new BigDecimal("0.000436"));
        put(2023, new BigDecimal("0.000441"));
        put(2024, new BigDecimal("0.000438"));
    }};
    private static final BigDecimal DEFAULT_CA = new BigDecimal("0.000441");
    
    @Override
    public EnR002ReductionResult calculateEmissions(
            Integer projectId,
            Integer siteId,
            Integer batchId,
            List<EnR002MonitoringRecord> monitoringData,
            LocalDate processingDate) {
        
        log.debug("Calculating emissions for site {} on date {}", siteId, processingDate);
        
        // Validate input - if no data, return zeros to ensure site is included in project total
        String validationError = validateData(monitoringData);
        if (validationError != null) {
            log.debug("No data for site {}, returning zero result", siteId);
            return EnR002ReductionResult.builder()
                .batchId(batchId)
                .processingDate(processingDate)
                .projectId(projectId)
                .siteId(siteId)
                .startTime(Instant.now())
                .endTime(Instant.now())
                .empj(BigDecimal.ZERO)
                .empjM(BigDecimal.ZERO)
                .empjS(BigDecimal.ZERO)
                .empjSControl(BigDecimal.ZERO)
                .empjSBattery(BigDecimal.ZERO)
                .build();
        }
        
        try {
            // Step 1: Get activity start date
            LocalDate activityStartDate = getActivityStartDate(projectId, siteId);
            
            // Step 2: Get CEF based on project age
            BigDecimal cef = getCefElectricity(activityStartDate, processingDate);
            
            // Step 3: Aggregate monitoring data
            SolarAggregation aggregation = aggregateMonitoringData(monitoringData);
            
            // Step 4: Calculate ELPJ,control = ELPV - ELPVR
            BigDecimal elpjControl = aggregation.totalElpv.subtract(aggregation.totalElpvr);
            
            // Step 5: Methane = 0 for solar
            BigDecimal empjM = BigDecimal.ZERO;
            
            // Step 6: Scope controlled = ELPJ,control × CEF
            BigDecimal empjSControl = calculateEmissions(elpjControl, cef);
            
            // Step 7: Battery emissions (conditional)
            BigDecimal empjSBattery = calculateBatteryEmissions(
                projectId, siteId, processingDate,
                aggregation.totalElpjBattery,
                aggregation.batteryChanged,
                cef
            );
            
            // Step 8: Scope total = Control + Battery
            BigDecimal empjS = empjSControl.add(empjSBattery);
            
            // Step 9: Total = Methane + Scope
            BigDecimal empj = empjM.add(empjS);
            
            log.info("Site {} calculated - Total: {} tCO2, Control: {}, Battery: {}",
                siteId, empj, empjSControl, empjSBattery);
            
            EnR002ReductionResult result = EnR002ReductionResult.builder()
                .batchId(batchId)
                .processingDate(processingDate)
                .projectId(projectId)
                .siteId(siteId)
                .startTime(Instant.now())
                .endTime(Instant.now())
                .empj(empj)
                .empjM(empjM)
                .empjS(empjS)
                .empjSControl(empjSControl)
                .empjSBattery(empjSBattery)
                .build();

            result.setIsActive(true);
            return result;

        } catch (Exception e) {
            log.error("Calculation failed for site {}: {}", siteId, e.getMessage(), e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
    
    @Override
    public String getMethodologyName() {
        return "Solar (EnR002)";
    }
    
    @Override
    public String getMethodologyType() {
        return "EnR002";
    }
    
    /**
     * Aggregates monitoring data for solar.
     */
    private SolarAggregation aggregateMonitoringData(List<EnR002MonitoringRecord> records) {
        BigDecimal totalElpv = BigDecimal.ZERO;
        BigDecimal totalElpvr = BigDecimal.ZERO;
        BigDecimal totalBattery = BigDecimal.ZERO;
        boolean batteryChanged = false;
        
        for (EnR002MonitoringRecord record : records) {
            totalElpv = safeAdd(totalElpv, record.getElPv());
            totalElpvr = safeAdd(totalElpvr, record.getElPvr());
            totalBattery = safeAdd(totalBattery, record.getElpjBatteryValue());
            
            if (Boolean.TRUE.equals(record.getChangedBattery())) {
                batteryChanged = true;
            }
        }
        
        return new SolarAggregation(totalElpv, totalElpvr, totalBattery, batteryChanged);
    }
    
    /**
     * Calculates CEF using CDM methodology.
     * Formula: CEF = C_mo × (1 - f(t)) + C_a(t) × f(t)
     */
    private BigDecimal getCefElectricity(LocalDate activityStartDate, LocalDate processingDate) {
        if (activityStartDate.isAfter(processingDate)) {
            throw new IllegalArgumentException(
                "Activity start date cannot be after processing date"
            );
        }
        
        double yearsSinceStart = calculateYearsSinceStart(activityStartDate, processingDate);
        double transitionFactor = calculateTransitionFactor(yearsSinceStart);
        BigDecimal ca = getAllPowerEmissionFactor(processingDate.getYear());
        
        return calculateCef(CMO, ca, transitionFactor);
    }
    
    /**
     * Calculates years since project start (fractional).
     */
    private double calculateYearsSinceStart(LocalDate startDate, LocalDate processingDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, processingDate);
        return daysBetween / 365.25;
    }
    
    /**
     * CDM transition function for project maturity.
     * f(t) = 0.0 (< 1yr), 0.5 (1-2.5yr), 1.0 (≥ 2.5yr)
     */
    private double calculateTransitionFactor(double yearsSinceStart) {
        if (yearsSinceStart < 1.0) {
            return 0.0;
        } else if (yearsSinceStart < 2.5) {
            return 0.5;
        } else {
            return 1.0;
        }
    }
    
    /**
     * Gets all-power emission factor for year.
     */
    private BigDecimal getAllPowerEmissionFactor(int year) {
        BigDecimal ca = CA_MAP.getOrDefault(year, null);
        
        if (ca == null || ca.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("All-power emission factor not found for year {}. Using default: {}",
                year, DEFAULT_CA);
            return DEFAULT_CA;
        }
        
        return ca;
    }
    
    /**
     * Calculates final CEF.
     */
    private BigDecimal calculateCef(BigDecimal cMo, BigDecimal ca, double transitionFactor) {
        BigDecimal fT = BigDecimal.valueOf(transitionFactor);
        BigDecimal oneMinusFt = BigDecimal.ONE.subtract(fT);
        
        BigDecimal marginalPart = cMo.multiply(oneMinusFt);
        BigDecimal allPowerPart = ca.multiply(fT);
        BigDecimal cef = marginalPart.add(allPowerPart);
        
        return cef.setScale(6, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates emissions: Energy (kWh) × CEF (tCO2/kWh)
     */
    private BigDecimal calculateEmissions(BigDecimal energy, BigDecimal cef) {
        return energy.multiply(cef).setScale(3, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculates battery emissions if conditions are met.
     * Conditions: Battery changed OR 1+ year since last calculation.
     */
    private BigDecimal calculateBatteryEmissions(
            Integer projectId,
            Integer siteId,
            LocalDate processingDate,
            BigDecimal totalBattery,
            boolean batteryChanged,
            BigDecimal cef) {
        
        boolean shouldCalculate = batteryChanged ||
            oneYearSinceLastCalculation(projectId, siteId, processingDate);
        
        boolean hasBatteryUsage = totalBattery.compareTo(BigDecimal.ZERO) > 0;
        
        if (shouldCalculate && hasBatteryUsage) {
            return calculateEmissions(totalBattery, cef);
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Checks if 1+ year has passed since last battery calculation.
     */
    private boolean oneYearSinceLastCalculation(Integer projectId, Integer siteId, LocalDate processingDate) {
        Optional<LocalDate> lastBatteryDate =
            reductionResultRepo.findLastBatteryCalculationDate(projectId, siteId);
        
        if (lastBatteryDate.isEmpty()) {
            return true;
        }
        
        long daysSince = ChronoUnit.DAYS.between(lastBatteryDate.get(), processingDate);
        return daysSince >= 365;
    }
    
    /**
     * Gets activity start date for site.
     */
    private LocalDate getActivityStartDate(Integer projectId, Integer siteId) {
        return siteProjectRelationRepo.findByProjectIdAndSiteId(projectId, siteId)
            .getActivityStartDate();
    }
    
    /**
     * Safely adds BigDecimals (treats null as zero).
     */
    private BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        return (a == null ? BigDecimal.ZERO : a)
            .add(b == null ? BigDecimal.ZERO : b);
    }
    
    /**
     * Helper class for aggregated solar data.
     */
    private static class SolarAggregation {
        final BigDecimal totalElpv;
        final BigDecimal totalElpvr;
        final BigDecimal totalElpjBattery;
        final boolean batteryChanged;
        
        SolarAggregation(BigDecimal elPv, BigDecimal elPvr, BigDecimal battery, boolean changed) {
            this.totalElpv = elPv;
            this.totalElpvr = elPvr;
            this.totalElpjBattery = battery;
            this.batteryChanged = changed;
        }
    }
}
