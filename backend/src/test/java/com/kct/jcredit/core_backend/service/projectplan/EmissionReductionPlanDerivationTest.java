package com.kct.jcredit.core_backend.service.projectplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.kct.jcredit.core_backend.dto.projectplan.EmissionReductionPlanRequest;
import com.kct.jcredit.core_backend.dto.projectplan.EmissionReductionYearRow;

import org.junit.jupiter.api.Test;

/**
 * 4.排出削減計画's derived values, checked against the official workbooks.
 *
 * <p>The numbers here are not invented: the 2013–2050 range and the fiscal-year
 * calendar come from {@code 別紙7.プロジェクト計画書フォーマット.xlsx}, and the
 * totals reproduce the solar example
 * ({@code sakuseirei_keikaku_solar_program.xlsx}, 方法論① 蓄電池あり) to the digit.
 */
class EmissionReductionPlanDerivationTest {

    private final EmissionReductionPlanDerivation derivation = new EmissionReductionPlanDerivation();

    /** 認証対象期間 of the solar example: 2025-10-01 through 2050-10-01, 25.0 年. */
    private static final String PERIOD_START = "2025-10-01";
    private static final String PERIOD_END = "2050-10-01";

    // --- the skeleton ---

    @Test
    void buildsTheThirtyEightFiscalYearsTheSheetHas() {
        EmissionReductionPlanRequest page = page("t-CO2");

        derivation.derive(page, null);

        assertThat(page.getYears()).hasSize(38);
        assertThat(page.getYears()).extracting(EmissionReductionYearRow::getYear)
                .startsWith(2013, 2014)
                .endsWith(2049, 2050);
    }

    @Test
    void derivesTheJapaneseFiscalYearCalendar() {
        EmissionReductionPlanRequest page = page("t-CO2");

        derivation.derive(page, null);

        EmissionReductionYearRow first = rowFor(page, 2013);
        assertThat(first.getFiscalYearStart()).isEqualTo("2013-04-01");
        assertThat(first.getFiscalYearEnd()).isEqualTo("2014-03-31");
        assertThat(first.getFiscalYearDays()).isEqualTo(365);

        // FY2015 runs to 2016-03-31 and so contains a leap February.
        assertThat(rowFor(page, 2015).getFiscalYearDays()).isEqualTo(366);
    }

    @Test
    void countsOnlyTheDaysOfEachYearTheCertificationPeriodCovers() {
        EmissionReductionPlanRequest page = page("kg-CO2");
        page.setCertTargetPeriodStart(PERIOD_START);
        page.setCertTargetPeriodEnd(PERIOD_END);

        derivation.derive(page, null);

        // Before the period starts.
        assertThat(rowFor(page, 2024).getCertApplicationDays()).isZero();
        // The period opens 2025-10-01, half way through FY2025.
        assertThat(rowFor(page, 2025).getCertApplicationDays()).isEqualTo(182);
        // A year fully inside it.
        assertThat(rowFor(page, 2030).getCertApplicationDays()).isEqualTo(365);
        // The sheet last row reads 184: 2050-04-01 through 2050-10-01.
        assertThat(rowFor(page, 2050).getCertApplicationDays()).isEqualTo(184);
    }

    @Test
    void leavesEveryYearAtZeroWhenThereIsNoCertificationPeriodYet() {
        EmissionReductionPlanRequest page = page("t-CO2");

        derivation.derive(page, null);

        assertThat(page.getYears())
                .allSatisfy(row -> assertThat(row.getCertApplicationDays()).isZero());
    }

    @Test
    void keepsWhatTheOperatorEnteredAndMatchesItBackByYear() {
        EmissionReductionPlanRequest page = page("t-CO2");
        // Deliberately out of order and incomplete, as a client might send it.
        page.setYears(List.of(row(2031, 260, 2540), row(2029, 300, 2000)));

        derivation.derive(page, null);

        assertThat(rowFor(page, 2029).getNewMembersTotal()).isEqualTo(300);
        assertThat(rowFor(page, 2031).getCumulativeMembersTotal()).isEqualTo(2540);
        // Untouched years arrive at the zeros the blank official format ships.
        assertThat(rowFor(page, 2030).getNewMembersTotal()).isZero();
    }

    @Test
    void aClientCannotAddAYearTheSheetDoesNotHave() {
        EmissionReductionPlanRequest page = page("t-CO2");
        page.setYears(List.of(row(2051, 999, 999), row(2029, 300, 2000)));

        derivation.derive(page, null);

        assertThat(page.getYears()).hasSize(38);
        assertThat(page.getYears()).extracting(EmissionReductionYearRow::getYear)
                .doesNotContain(2051);
    }

    @Test
    void everyRowCarriesThePagesUnit() {
        EmissionReductionPlanRequest page = page("kg-CO2");
        // Whatever the client claimed per row is replaced: the sheet says
        // 「シート内、「単位」で選択した単位と揃えてください」.
        EmissionReductionYearRow tampered = row(2029, 1, 1);
        tampered.setBaselineEmissionsUnit("t-CO2");
        page.setYears(List.of(tampered));

        derivation.derive(page, null);

        assertThat(page.getYears()).allSatisfy(row -> {
            assertThat(row.getBaselineEmissionsUnit()).isEqualTo("kg-CO2");
            assertThat(row.getAfterImplEmissionsUnit()).isEqualTo("kg-CO2");
        });
    }

    // --- the 合計 row ---

    @Test
    void totalsAreAlwaysTonnesWhateverUnitTheRowsUse() {
        // The solar example's column sums, in kg-CO2. Its sheet reads
        // 合計 43105.5 / 7788.5 t-CO2, 総量 35317, 年平均 1412.68.
        EmissionReductionPlanRequest page = page("kg-CO2");
        page.setCertTargetPeriodYears(new BigDecimal("25.0"));
        page.setYears(List.of(emissions(2029, "43105545.8630137", "7788463.643835616")));

        derivation.derive(page, null);

        assertThat(page.getBaselineEmissionsTotal()).isEqualByComparingTo("43105.5");
        assertThat(page.getAfterImplEmissionsUnitTotal()).isEqualByComparingTo("7788.5");
        // 35317, not 35317.1 — the sheet subtracts the rounded totals, and the
        // raw difference is 35317.0822.
        assertThat(page.getEmissionReductionTotal()).isEqualByComparingTo("35317.0");
        assertThat(page.getEmissionReductionAnnualAverage()).isEqualByComparingTo("1412.68");
    }

    @Test
    void tonneRowsAreNotConverted() {
        EmissionReductionPlanRequest page = page("t-CO2");
        page.setYears(List.of(emissions(2029, "1200.25", "200.05")));

        derivation.derive(page, null);

        assertThat(page.getBaselineEmissionsTotal()).isEqualByComparingTo("1200.3");
        assertThat(page.getAfterImplEmissionsUnitTotal()).isEqualByComparingTo("200.1");
        assertThat(page.getEmissionReductionTotal()).isEqualByComparingTo("1000.2");
    }

    @Test
    void whateverTotalTheClientPostsIsReplaced() {
        EmissionReductionPlanRequest page = page("t-CO2");
        page.setYears(List.of(emissions(2029, "100", "40")));
        page.setBaselineEmissionsTotal(new BigDecimal("999999"));
        page.setEmissionReductionTotal(new BigDecimal("999999"));

        derivation.derive(page, null);

        assertThat(page.getBaselineEmissionsTotal()).isEqualByComparingTo("100.0");
        assertThat(page.getEmissionReductionTotal()).isEqualByComparingTo("60.0");
    }

    @Test
    void anUnrecognisedUnitLeavesTheTotalsUnsetRatherThanWrong() {
        // Reporting kilogrammes as tonnes would be a thousandfold error nothing
        // downstream could spot. @Pattern rejects the page a moment later.
        EmissionReductionPlanRequest page = page("t-CO2/年");
        page.setYears(List.of(emissions(2029, "100", "40")));

        derivation.derive(page, null);

        assertThat(page.getBaselineEmissionsTotal()).isNull();
        assertThat(page.getEmissionReductionTotal()).isNull();
        assertThat(page.getEmissionReductionAnnualAverage()).isNull();
    }

    @Test
    void theAnnualAverageWaitsForTheCertificationPeriodLength() {
        EmissionReductionPlanRequest page = page("t-CO2");
        page.setYears(List.of(emissions(2029, "100", "40")));

        derivation.derive(page, null);

        assertThat(page.getEmissionReductionTotal()).isEqualByComparingTo("60.0");
        assertThat(page.getEmissionReductionAnnualAverage()).isNull();
    }

    @Test
    void isIdempotent() {
        // Save applies it twice — once before validation, once after uploads
        // are promoted and the payload re-read.
        EmissionReductionPlanRequest page = page("kg-CO2");
        page.setCertTargetPeriodStart(PERIOD_START);
        page.setCertTargetPeriodEnd(PERIOD_END);
        page.setCertTargetPeriodYears(new BigDecimal("25.0"));
        page.setYears(List.of(emissions(2029, "3124800", "564600")));

        derivation.derive(page, null);
        BigDecimal firstPass = page.getEmissionReductionTotal();
        List<EmissionReductionYearRow> firstRows = page.getYears();

        derivation.derive(page, null);

        assertThat(page.getEmissionReductionTotal()).isEqualByComparingTo(firstPass);
        assertThat(page.getYears()).hasSameSizeAs(firstRows);
        assertThat(rowFor(page, 2029).getBaselineEmissions()).isEqualByComparingTo("3124800");
    }

    // --- helpers ---

    private static EmissionReductionPlanRequest page(String unit) {
        EmissionReductionPlanRequest page = new EmissionReductionPlanRequest();
        page.setUnitEmission(unit);
        return page;
    }

    private static EmissionReductionYearRow rowFor(EmissionReductionPlanRequest page, int year) {
        return page.getYears().stream()
                .filter(row -> row.getYear() == year)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no row for 年度 " + year));
    }

    private static EmissionReductionYearRow row(int year, int newMembers, int cumulative) {
        EmissionReductionYearRow row = new EmissionReductionYearRow();
        row.setYear(year);
        row.setNewMembersTotal(newMembers);
        row.setCumulativeMembersTotal(cumulative);
        return row;
    }

    private static EmissionReductionYearRow emissions(int year, String baseline, String afterImpl) {
        EmissionReductionYearRow row = row(year, 0, 0);
        row.setBaselineEmissions(new BigDecimal(baseline));
        row.setAfterImplEmissions(new BigDecimal(afterImpl));
        return row;
    }
}
