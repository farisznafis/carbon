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
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultRepository;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.stream.Collectors;
import java.util.ArrayList;
/**
 * Solar (EnR002) methodology emissions calculator.
 *
 * Key formulas:
 * 1.  ELPJ       = ELPV - ELPVR
 * 2.  EM_PJ,M    = 0
 * 3.  EM_PJ,S,battery = ELPJ,battery × CEF(t)
 * 4.  EM_PJ,S    = EM_PJ,S,battery
 * 5.  EM_PJ      = EM_PJ,M + EM_PJ,S
 * 6.  ELBL       = ELPJ
 * 7.  EM_BL      = ELBL × CEF(t)
 * 8.  ER         = EM_BL - EM_PJ
 *
 * Aggregation strategy:
 * - All monitoring records belonging to the same equipmentId are summed first.
 * - Past EnR002ReductionResults for the same (projectId, siteId, equipmentId) are
 *   fetched and their cumulative totals are added to the current batch totals.
 * - One EnR002ReductionResult is persisted per equipment (not per raw record).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SolarEmissionsCalculator implements EmissionsCalculator<EnR002MonitoringRecord, EnR002ReductionResult> {

    private final SiteProjectRelationRepository siteProjectRelationRepo;
    private final EnR002ReductionResultRepository reductionResultRepo; // ✅ RE-ADDED for historical lookup


    //TODO: find a way of obtaining these actual values
    private static final BigDecimal CMO = new BigDecimal("0.000512");
    private static final Map<Integer, BigDecimal> CA_MAP = new HashMap<>() {{
        put(2020, new BigDecimal("0.000460"));
        put(2021, new BigDecimal("0.000444"));
        put(2022, new BigDecimal("0.000436"));
        put(2023, new BigDecimal("0.000441"));
        put(2024, new BigDecimal("0.000438"));
    }};
    private static final BigDecimal DEFAULT_CA = new BigDecimal("0.000441");

    /**
     * Main entry point.
     *
     * Flow:
     * 1. Group all monitoring records by equipmentId.
     * 2. For each equipment group, sum all current-batch values.
     * 3. Fetch all past results for the same (projectId, siteId, equipmentId).
     * 4. Accumulate past totals into the current batch totals.
     * 5. Run the methodology formulas on the fully accumulated totals.
     * 6. Return one EnR002ReductionResult per equipment.
     */
    public List<EnR002ReductionResult> calculateEmissions(
            Integer projectId,
            Integer siteId,
            Integer batchId,
            List<EnR002MonitoringRecord> monitoringData,
            LocalDate processingDate) {

        // ✅ Step A: Group records by equipmentId
        Map<Integer, List<EnR002MonitoringRecord>> recordsByEquipment = monitoringData.stream()
            .filter(r -> r.getEquipmentID() != null)
            .collect(Collectors.groupingBy(EnR002MonitoringRecord::getEquipmentID));

        List<EnR002ReductionResult> results = new ArrayList<>();

        for (Map.Entry<Integer, List<EnR002MonitoringRecord>> entry : recordsByEquipment.entrySet()) {
            Integer equipmentId = entry.getKey();
            List<EnR002MonitoringRecord> equipmentRecords = entry.getValue();

            results.add(calculateForEquipment(
                projectId, siteId, batchId,
                equipmentId, equipmentRecords,
                processingDate
            ));
        }

        return results;
    }

    /**
     * Calculates one aggregated EnR002ReductionResult for a single equipment,
     * combining all current-batch records AND all historical past results.
     */
    private EnR002ReductionResult calculateForEquipment(
            Integer projectId,
            Integer siteId,
            Integer batchId,
            Integer equipmentId,
            List<EnR002MonitoringRecord> equipmentRecords,
            LocalDate processingDate) {

        // ✅ Step B: Aggregate current-batch raw values for this equipment
        EquipmentRawTotals currentBatch = aggregateRawRecords(equipmentRecords);
        
        try {
            // ✅ Step C: Fetch historical past result BEFORE the early-exit check
            EquipmentHistoricalTotals history = fetchHistoricalTotals(projectId, siteId, equipmentId);

            boolean currentBatchIsEmpty =
                currentBatch.elpv.compareTo(BigDecimal.ZERO) == 0
                && currentBatch.elpvr.compareTo(BigDecimal.ZERO) == 0
                && currentBatch.elpjBattery.compareTo(BigDecimal.ZERO) == 0;

            boolean historyIsEmpty =
                history.er.compareTo(BigDecimal.ZERO) == 0
                && history.elpj.compareTo(BigDecimal.ZERO) == 0;

            if (currentBatchIsEmpty && historyIsEmpty) {
                log.debug("No meaningful data (current or historical) for equipment {}, returning zero result",
                    equipmentId);
                return zeroResult(batchId, processingDate, projectId, siteId, equipmentId);
            }

            if (currentBatchIsEmpty) {
                // No new data, but carry historical totals forward into the new result row
                log.debug("No new batch data for equipment {}, carrying forward historical totals", equipmentId);
                 EnR002ReductionResult result = EnR002ReductionResult.builder()
                    .batchId(batchId)
                    .processingDate(processingDate)
                    .projectId(projectId)
                    .siteId(siteId)
                    .equipmentId(equipmentId)
                    .startTime(Instant.now())
                    .endTime(Instant.now())
                    .elpj(history.elpj)
                    .elbl(history.elbl)
                    .embl(history.embl)
                    .er(history.er)
                    .empj(history.empj)
                    .empjM(history.empjM)
                    .empjS(history.empjS)
                    .empjSBattery(history.empjSBattery)
                    .empjSControl(history.empjSControl)
                    .build();
                result.setIsActive(true);
                return result;
            }


            // ✅ Step D: Compute CEF for this processing date
            LocalDate activityStartDate = getActivityStartDate(projectId, siteId);
            BigDecimal cef = getCefElectricity(activityStartDate, processingDate);

            // ─── Current batch formula execution ───────────────────────────

            // Step 1: ELPJ = ELPV - ELPVR  (current batch only)
            BigDecimal currentElpj = currentBatch.elpv.subtract(currentBatch.elpvr);

            // Step 2: EM_PJ,M = 0
            BigDecimal currentEmpjM = BigDecimal.ZERO;

            // Step 3: EM_PJ,S,control = ELPJ,control × CEF(t)
            // Per the methodology, if no explicit control-energy value is monitored,
            // the default proxy is 0.02 kWh/kWh of the project generation.
            BigDecimal currentEmpjSControl = calculateControlEmissions(currentBatch.elpjControl, currentBatch.elpv, cef);

            // Step 4: EM_PJ,S,battery = ELPJ,battery × CEF
            BigDecimal currentEmpjSBattery = calculateBatteryEmissions(currentBatch.elpjBattery, cef);

            // Step 5: EM PJ,S = EM PJ,S,control + EM PJ,S,battery 
            BigDecimal currentEmpjS = currentEmpjSControl.add(currentEmpjSBattery);

            // Step 6: EM_PJ = EM PJ,M + EM_PJ,S
            BigDecimal currentEmpj = currentEmpjM.add(currentEmpjS);

            // Step 7: ELBL = ELPJ
            BigDecimal currentElbl = currentElpj;

            // Step 8: EM_BL = ELBL × CEF
            BigDecimal currentEmbl = calculateEmissionsValue(currentElbl, cef);

            // Step 9: ER = EM_BL - EM_PJ
            BigDecimal currentEr = currentEmbl.subtract(currentEmpj);

            // ─── Accumulate with historical totals ─────────────────────────
            // ✅ Step E: Add past cumulative values to current batch results

            BigDecimal totalElpj          = currentElpj.add(history.elpj);
            BigDecimal totalElbl          = currentElbl.add(history.elbl);
            BigDecimal totalEmbl          = currentEmbl.add(history.embl);
            BigDecimal totalEmpj          = currentEmpj.add(history.empj);
            BigDecimal totalEmpjM         = currentEmpjM.add(history.empjM);
            BigDecimal totalEmpjS         = currentEmpjS.add(history.empjS);
            BigDecimal totalEmpjSBattery  = currentEmpjSBattery.add(history.empjSBattery);
            BigDecimal totalEmpjSControl   = currentEmpjSControl.add(history.empjSControl);
            BigDecimal totalEr            = currentEr.add(history.er);

            log.info("Equipment {} — batch ER: {}, cumulative ER: {} tCO2",
                equipmentId, currentEr, totalEr);

            EnR002ReductionResult result = EnR002ReductionResult.builder()
                .batchId(batchId)
                .processingDate(processingDate)
                .projectId(projectId)
                .siteId(siteId)
                .equipmentId(equipmentId)
                .startTime(Instant.now())
                .endTime(Instant.now())
                // ── Cumulative (current batch + all past) ──
                .elpj(totalElpj)
                .elbl(totalElbl)
                .embl(totalEmbl)
                .er(totalEr)
                .empj(totalEmpj)
                .empjM(totalEmpjM)
                .empjS(totalEmpjS)
                .empjSBattery(totalEmpjSBattery)
                .empjSControl(totalEmpjSControl)
                .build();

            result.setIsActive(true);
            return result;

        } catch (Exception e) {
            log.error("Calculation failed for equipment {}: {}", equipmentId, e.getMessage(), e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Aggregation helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * ✅ Sums raw monitoring fields across all records belonging to one equipment
     * within the current batch.
     */
    private EquipmentRawTotals aggregateRawRecords(List<EnR002MonitoringRecord> records) {
        BigDecimal elpv        = BigDecimal.ZERO;
        BigDecimal elpvr       = BigDecimal.ZERO;
        BigDecimal elpjBattery = BigDecimal.ZERO;
        BigDecimal elpjControl = BigDecimal.ZERO;

        for (EnR002MonitoringRecord r : records) {
            elpv        = elpv.add(nullToZero(r.getElPv()));
            elpvr       = elpvr.add(nullToZero(r.getElPvr()));
            elpjBattery = elpjBattery.add(nullToZero(r.getElpjBatteryValue()));
            elpjControl = elpjControl.add(nullToZero(r.getElpjControlValue()));
        }

        return new EquipmentRawTotals(elpv, elpvr, elpjBattery, elpjControl);
    }

    /**
     * ✅ Fetches all past EnR002ReductionResults for the given equipment
     * and sums their stored calculated fields into one historical total.
     *
     * Uses the most recent batchId per equipment to avoid double-counting
     * if re-processing scenarios occur — adjust the query to your needs.
     */
 // In fetchHistoricalTotals — replace findAll with findLatest
    private EquipmentHistoricalTotals fetchHistoricalTotals(
            Integer projectId, Integer siteId, Integer equipmentId) {

        // ✅ Fetch only the single most recent result — it already contains
        //    the full cumulative total up to the last batch.
        Optional<EnR002ReductionResult> latestOpt =
            reductionResultRepo.findTopByProjectIdAndSiteIdAndEquipmentIdOrderByBatchIdDesc(
                projectId, siteId, equipmentId);

        if (latestOpt.isEmpty()) {
            log.debug("No historical result found for equipment {}", equipmentId);
            return emptyHistory();
        }

        EnR002ReductionResult latest = latestOpt.get();
        return new EquipmentHistoricalTotals(
            nullToZero(latest.getElpj()),
            nullToZero(latest.getElbl()),
            nullToZero(latest.getEmbl()),
            nullToZero(latest.getEmpj()),
            nullToZero(latest.getEmpjM()),
            nullToZero(latest.getEmpjS()),
            nullToZero(latest.getEmpjSBattery()),
            nullToZero(latest.getEmpjSControl()),
            nullToZero(latest.getEr())
        );
    }

    private EquipmentHistoricalTotals emptyHistory() {
        return new EquipmentHistoricalTotals(
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Value objects (raw totals & historical totals)
    // ─────────────────────────────────────────────────────────────────────────

    /** Holds summed raw input fields for one equipment within the current batch. */
    @lombok.Value
    private static class EquipmentRawTotals {
        BigDecimal elpv;
        BigDecimal elpvr;
        BigDecimal elpjBattery;
        BigDecimal elpjControl;
    }

    /** Holds summed calculated fields from all past results for one equipment. */
    @lombok.Value
    private static class EquipmentHistoricalTotals {
        BigDecimal elpj;
        BigDecimal elbl;
        BigDecimal embl;
        BigDecimal empj;
        BigDecimal empjM;
        BigDecimal empjS;
        BigDecimal empjSBattery;
        BigDecimal empjSControl;
        BigDecimal er;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Battery / emissions helpers
    // ─────────────────────────────────────────────────────────────────────────

    private BigDecimal calculateBatteryEmissions(BigDecimal elpjBattery, BigDecimal cef) {
        if (elpjBattery == null || elpjBattery.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return calculateEmissionsValue(elpjBattery, cef);
    }


    //!!!!! if the users iserts 0 for control value then we can assume this is valid value if null do the default calculation
    private BigDecimal calculateControlEmissions(BigDecimal elpjControl, BigDecimal elpv, BigDecimal cef) {
        if (elpjControl != null) {
            return calculateEmissionsValue(elpjControl, cef);
        }

        BigDecimal PCS_DEFAULT_RATIO = new BigDecimal("0.02");
        BigDecimal elpjControlDefault = elpv.multiply(PCS_DEFAULT_RATIO);
        log.debug("No explicit control-energy value supplied; using default proxy {} kWh/kWh of generation", PCS_DEFAULT_RATIO);
        return calculateEmissionsValue(elpjControlDefault, cef);
    }

    private BigDecimal calculateEmissionsValue(BigDecimal energy, BigDecimal cef) {
        return energy.multiply(cef).setScale(3, RoundingMode.HALF_UP);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Zero result
    // ─────────────────────────────────────────────────────────────────────────

    private EnR002ReductionResult zeroResult(
            Integer batchId, LocalDate processingDate,
            Integer projectId, Integer siteId, Integer equipmentId) {
        return EnR002ReductionResult.builder()
            .batchId(batchId)
            .processingDate(processingDate)
            .projectId(projectId)
            .siteId(siteId)
            .equipmentId(equipmentId)
            .startTime(Instant.now())
            .endTime(Instant.now())
            .elpj(BigDecimal.ZERO)
            .elbl(BigDecimal.ZERO)
            .embl(BigDecimal.ZERO)
            .er(BigDecimal.ZERO)
            .empj(BigDecimal.ZERO)
            .empjM(BigDecimal.ZERO)
            .empjS(BigDecimal.ZERO)
            .empjSBattery(BigDecimal.ZERO)
            .build();
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CEF methods (unchanged)
    // ─────────────────────────────────────────────────────────────────────────

    private BigDecimal getCefElectricity(LocalDate activityStartDate, LocalDate processingDate) {
        if (activityStartDate.isAfter(processingDate)) {
            throw new IllegalArgumentException("Activity start date cannot be after processing date");
        }
        double yearsSinceStart  = calculateYearsSinceStart(activityStartDate, processingDate);
        double transitionFactor = calculateTransitionFactor(yearsSinceStart);
        BigDecimal ca           = getAllPowerEmissionFactor(processingDate.getYear());
        return calculateCef(CMO, ca, transitionFactor);
    }

    private double calculateYearsSinceStart(LocalDate startDate, LocalDate processingDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, processingDate);
        return daysBetween / 365.25;
    }

    private double calculateTransitionFactor(double yearsSinceStart) {
        if (yearsSinceStart < 1.0)      return 0.0;
        else if (yearsSinceStart < 2.5) return 0.5;
        else                            return 1.0;
    }

    private BigDecimal getAllPowerEmissionFactor(int year) {
        BigDecimal ca = CA_MAP.getOrDefault(year, null);
        if (ca == null || ca.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("All-power factor not found for year {}. Using default: {}", year, DEFAULT_CA);
            return DEFAULT_CA;
        }
        return ca;
    }

    private BigDecimal calculateCef(BigDecimal cMo, BigDecimal ca, double transitionFactor) {
        BigDecimal fT         = BigDecimal.valueOf(transitionFactor);
        BigDecimal oneMinusFt = BigDecimal.ONE.subtract(fT);
        return cMo.multiply(oneMinusFt).add(ca.multiply(fT))
                  .setScale(6, RoundingMode.HALF_UP);
    }

    private LocalDate getActivityStartDate(Integer projectId, Integer siteId) {
        return siteProjectRelationRepo.findByProjectIdAndSiteId(projectId, siteId)
            .getActivityStartDate();
    }

    @Override
    public String getMethodologyName() { return "Solar (EnR002)"; }

    @Override
    public String getMethodologyType() { return "EnR002"; }
}