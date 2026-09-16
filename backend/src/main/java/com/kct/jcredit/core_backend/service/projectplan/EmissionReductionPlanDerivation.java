package com.kct.jcredit.core_backend.service.projectplan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.kct.jcredit.core_backend.dto.projectplan.EmissionReductionPlanRequest;
import com.kct.jcredit.core_backend.dto.projectplan.EmissionReductionYearRow;

/**
 * Everything 4.排出削減計画 computes instead of asking for.
 *
 * <p>Read straight off the official sheets rather than inferred:
 *
 * <ul>
 *   <li>the 年度 table is <b>fixed at 2013–2050</b>, 38 rows, in both
 *       {@code 別紙7} and the solar example — it is the scheme's lifetime, not
 *       something derived from this project's 認証対象期間;</li>
 *   <li>年度開始日 / 年度終了日 are 4月1日 – 翌3月31日, and 年度内日数 follows
 *       (365, or 366 when the fiscal year spans a leap February);</li>
 *   <li>年度内の認証申請期間日数 is the overlap of 認証対象期間 with that fiscal
 *       year — the solar example ends mid-2050 and its last row reads 184;</li>
 *   <li>合計 converts to t-CO2 whatever unit the rows carry, rounded to one
 *       decimal, and 排出削減量の総量 is the difference of the two <i>rounded</i>
 *       totals. Verified against the solar example: rows sum to
 *       43,105,545.86 kg and 7,788,463.64 kg, the sheet reads 43105.5 and
 *       7788.5, and 総量 reads 35317 — which is the difference of the rounded
 *       figures, not of the raw ones.</li>
 * </ul>
 */
public class EmissionReductionPlanDerivation
        implements FormDerivation<EmissionReductionPlanRequest> {

    /** Both workbooks put the table at rows 7–44: 年度 2013 through 2050. */
    static final int FIRST_YEAR = 2013;
    static final int LAST_YEAR = 2050;

    /** 合計 is always t-CO2, 「選択内容に関わらず」. */
    private static final BigDecimal KG_TO_TONNE = new BigDecimal("0.001");
    private static final int TOTAL_SCALE = 1;
    private static final int AVERAGE_SCALE = 2;

    private static final List<String> DERIVED = List.of(
            "years[].year",
            "years[].fiscalYearStart",
            "years[].fiscalYearEnd",
            "years[].fiscalYearDays",
            "years[].certApplicationDays",
            "years[].baselineEmissionsUnit",
            "years[].afterImplEmissionsUnit",
            "baselineEmissionsTotal",
            "afterImplEmissionsUnitTotal",
            "emissionReductionTotal",
            "emissionReductionAnnualAverage");

    @Override
    public List<String> derivedFields() {
        return DERIVED;
    }

    @Override
    public void derive(EmissionReductionPlanRequest form, ProjectPlanSqlContext context) {
        List<EmissionReductionYearRow> rows = rebuildSkeleton(form);
        form.setYears(rows);
        applyTotals(form, rows);
    }

    /**
     * The 38 fiscal years, carrying over whatever the operator has entered.
     *
     * <p>Rebuilt rather than trusted, so a client cannot drop a year, reorder
     * them, or invent a 2051. Rows are matched back by 年度, which means a
     * client that reorders them loses nothing.
     */
    private static List<EmissionReductionYearRow> rebuildSkeleton(
            EmissionReductionPlanRequest form) {

        Map<Integer, EmissionReductionYearRow> entered = byYear(form.getYears());
        LocalDate periodStart = parseDate(form.getCertTargetPeriodStart());
        LocalDate periodEnd = parseDate(form.getCertTargetPeriodEnd());
        String unit = form.getUnitEmission();

        List<EmissionReductionYearRow> rows = new ArrayList<>(LAST_YEAR - FIRST_YEAR + 1);
        for (int year = FIRST_YEAR; year <= LAST_YEAR; year++) {
            EmissionReductionYearRow row = entered.get(year);
            if (row == null) {
                row = blankRow();
            }

            LocalDate start = LocalDate.of(year, 4, 1);
            LocalDate end = LocalDate.of(year + 1, 3, 31);

            row.setYear(year);
            row.setFiscalYearStart(start.toString());
            row.setFiscalYearEnd(end.toString());
            row.setFiscalYearDays(inclusiveDays(start, end));
            row.setCertApplicationDays(overlapDays(start, end, periodStart, periodEnd));
            row.setBaselineEmissionsUnit(unit);
            row.setAfterImplEmissionsUnit(unit);

            rows.add(row);
        }
        return rows;
    }

    /**
     * A year the operator has not reached yet.
     *
     * <p>Seeded with zeros because that is what the blank official format
     * ships: rows 7–44 of {@code 別紙7} already read 0. Leaving them null
     * would make a page of 38 required rows unsaveable until every year is
     * typed, for a plan that usually concerns a handful of them.
     */
    private static EmissionReductionYearRow blankRow() {
        EmissionReductionYearRow row = new EmissionReductionYearRow();
        row.setNewMembersTotal(0);
        row.setCumulativeMembersTotal(0);
        row.setMembersTotal(0);
        row.setBaselineEmissions(BigDecimal.ZERO);
        row.setAfterImplEmissions(BigDecimal.ZERO);
        return row;
    }

    private static Map<Integer, EmissionReductionYearRow> byYear(
            List<EmissionReductionYearRow> rows) {

        Map<Integer, EmissionReductionYearRow> byYear = new LinkedHashMap<>();
        if (rows == null) {
            return byYear;
        }
        for (EmissionReductionYearRow row : rows) {
            if (row != null && row.getYear() != null) {
                // First wins: a duplicated 年度 keeps the earlier row rather
                // than silently preferring whichever came last.
                byYear.putIfAbsent(row.getYear(), row);
            }
        }
        return byYear;
    }

    private static void applyTotals(
            EmissionReductionPlanRequest form, List<EmissionReductionYearRow> rows) {

        BigDecimal factor = tonneFactor(form.getUnitEmission());
        if (factor == null) {
            // An unrecognised 単位 cannot be converted, and guessing 1 would
            // report kilogrammes as tonnes. @Pattern on unitEmission rejects
            // the page a moment later; leave the totals unset until it does.
            form.setBaselineEmissionsTotal(null);
            form.setAfterImplEmissionsUnitTotal(null);
            form.setEmissionReductionTotal(null);
            form.setEmissionReductionAnnualAverage(null);
            return;
        }

        BigDecimal baseline = totalOf(rows, true, factor);
        BigDecimal afterImpl = totalOf(rows, false, factor);
        BigDecimal reduction = baseline.subtract(afterImpl);

        form.setBaselineEmissionsTotal(baseline);
        form.setAfterImplEmissionsUnitTotal(afterImpl);
        form.setEmissionReductionTotal(reduction);
        form.setEmissionReductionAnnualAverage(annualAverage(reduction, form.getCertTargetPeriodYears()));
    }

    /** Σ of one column, converted to t-CO2 and rounded the way the sheet does. */
    private static BigDecimal totalOf(
            List<EmissionReductionYearRow> rows, boolean baseline, BigDecimal factor) {

        BigDecimal sum = BigDecimal.ZERO;
        for (EmissionReductionYearRow row : rows) {
            BigDecimal value = baseline ? row.getBaselineEmissions() : row.getAfterImplEmissions();
            if (value != null) {
                // A cleared cell counts as zero, exactly as SUM() treats a
                // blank cell in the sheet.
                sum = sum.add(value);
            }
        }
        return sum.multiply(factor).setScale(TOTAL_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal annualAverage(BigDecimal reduction, BigDecimal years) {
        if (years == null || years.signum() <= 0) {
            return null;
        }
        return reduction.divide(years, AVERAGE_SCALE, RoundingMode.HALF_UP);
    }

    /** {@code null} when the unit is not one this sheet can total. */
    private static BigDecimal tonneFactor(String unit) {
        if (unit == null) {
            return null;
        }
        return switch (unit.trim()) {
            case "t-CO2" -> BigDecimal.ONE;
            case "kg-CO2" -> KG_TO_TONNE;
            default -> null;
        };
    }

    /** Days in {@code [from, to]}, counting both ends as the sheet does. */
    private static int inclusiveDays(LocalDate from, LocalDate to) {
        return (int) ChronoUnit.DAYS.between(from, to) + 1;
    }

    /** 年度内の認証申請期間日数 — zero when the period does not reach this year. */
    private static int overlapDays(
            LocalDate yearStart, LocalDate yearEnd, LocalDate periodStart, LocalDate periodEnd) {

        if (periodStart == null || periodEnd == null || periodEnd.isBefore(periodStart)) {
            return 0;
        }
        LocalDate from = periodStart.isAfter(yearStart) ? periodStart : yearStart;
        LocalDate to = periodEnd.isBefore(yearEnd) ? periodEnd : yearEnd;
        return to.isBefore(from) ? 0 : inclusiveDays(from, to);
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            // @DateFormat rejects the page for this; the skeleton is still
            // built so the operator can see and fix the date.
            return null;
        }
    }
}
