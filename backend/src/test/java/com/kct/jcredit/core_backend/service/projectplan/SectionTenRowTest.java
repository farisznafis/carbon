package com.kct.jcredit.core_backend.service.projectplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.kct.jcredit.core_backend.dto.projectplan.BaselineIncidentalActivityRow;
import com.kct.jcredit.core_backend.dto.projectplan.BaselineIncidentalEmissionActivityRequest;
import com.kct.jcredit.core_backend.dto.projectplan.FormulaSymbolRow;
import com.kct.jcredit.core_backend.dto.projectplan.MainActivityProjectEmissionRequest;
import com.kct.jcredit.core_backend.dto.projectplan.PostImplementIncidentalActivityRequest;
import com.kct.jcredit.core_backend.dto.projectplan.PostImplementIncidentalActivityRow;
import com.kct.jcredit.core_backend.util.JsonFieldNameResolver;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

/**
 * 10.排出削減量の算定方法 once its sub-tables hold rows.
 *
 * <p>Formula and symbol values are the solar example's
 * ({@code sakuseirei_keikaku_solar_program.xlsx}, 方法論①): its 式 5 is
 * {@code EMPJ,S,control = ELPJ,control × CEFelectricity,t}, three symbols in
 * one table, which the single-symbol pages could not hold.
 */
class SectionTenRowTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private final ProjectPlanFormRegistry registry = new ProjectPlanFormRegistry();

    // --- a formula's symbols ---

    @Test
    void aFormulaMayNameEverySymbolItUses() {
        MainActivityProjectEmissionRequest page = new MainActivityProjectEmissionRequest();
        page.setConcept("太陽光発電設備の導入ではプロジェクト実施後の主要排出量は0である。");
        page.setEvaluationFormula("EMPJ,S,control=ELPJ,control×CEFelectricity,t");
        page.setFormulaNumber(5);
        page.setSymbols(List.of(
                symbol("EMPJ,S,control", "電力制御装置の使用によるプロジェクト実施後排出量", "kg-CO2/年", "52.08"),
                symbol("ELPJ,control", "プロジェクト実施後の電力制御装置における電力使用量", "kWh/年", "120"),
                symbol("CEFelectricity,t", "電力のCO2排出係数", "kg-CO2/kWh", "0.434")));

        assertThat(errorKeysOf(page)).isEmpty();
        assertThat(page.getSymbols()).hasSize(3);
    }

    @Test
    void theFormulasTheWorkbooksActuallyContainAreAccepted() {
        // 方法論の評価式 carried @JapaneseText, which allows neither = nor ×,
        // so every formula in either workbook was rejected by the page meant
        // to hold it. These four are copied out of the sheets.
        List.of("ER=EMBL - EMPJ",
                        "ER1=EMBL1 - EMPJ",
                        "EMPJ,S1=EMPJ,S,control+EMPJ,S,battery",
                        "EMPJ,S,control=ELPJ,control×CEFelectricity,t")
                .forEach(formula -> {
                    MainActivityProjectEmissionRequest page = new MainActivityProjectEmissionRequest();
                    page.setConcept("考え方");
                    page.setEvaluationFormula(formula);
                    page.setFormulaNumber(1);
                    page.setSymbols(List.of(symbol("EMPJ", "定義", "kg-CO2/年", "0")));

                    assertThat(errorKeysOf(page)).as("formula %s", formula).isEmpty();
                });
    }

    @Test
    void aFormulaWithNoSymbolTableIsRefused() {
        MainActivityProjectEmissionRequest page = new MainActivityProjectEmissionRequest();
        page.setConcept("考え方");
        page.setEvaluationFormula("EMPJ = 0");
        page.setFormulaNumber(3);

        assertThat(errorKeysOf(page)).containsExactly("symbols");
    }

    @Test
    void anIncompleteSymbolIsReportedAgainstItsOwnRow() {
        MainActivityProjectEmissionRequest page = new MainActivityProjectEmissionRequest();
        page.setConcept("考え方");
        page.setEvaluationFormula("EMPJ = 0");
        page.setFormulaNumber(3);
        page.setSymbols(List.of(
                symbol("EMPJ", "プロジェクト実施後の主要排出量", "kg-CO2/年", "0"),
                symbol("", "定義だけある行", "kg-CO2/年", "1")));

        assertThat(errorKeysOf(page)).containsExactly("symbols[1].symbol");
    }

    @Test
    void aSymbolWithoutAUnitStillSaves() {
        // Requiring 単位 would block the page over a dimensionless symbol, and
        // this form has already lost several pages to required fields the
        // operator had nothing to put in.
        MainActivityProjectEmissionRequest page = new MainActivityProjectEmissionRequest();
        page.setConcept("考え方");
        page.setEvaluationFormula("EMPJ = 0");
        page.setFormulaNumber(3);
        page.setSymbols(List.of(symbol("EMPJ", "プロジェクト実施後の主要排出量", null, "0")));

        assertThat(errorKeysOf(page)).isEmpty();
    }

    // --- 10.2(1): rows and their 排出量合計値 ---

    @Test
    void baselineIncidentalActivitiesAreTotalled() {
        BaselineIncidentalEmissionActivityRequest page = new BaselineIncidentalEmissionActivityRequest();
        page.setActivities(List.of(
                baselineActivity("パワーコンディショナー等の使用", "52.08", true),
                baselineActivity("蓄電池の使用", "178.11", false)));

        derive("baseline-incidental-emission-activity", page);

        assertThat(page.getEmissionsTotalValue()).isEqualByComparingTo("230.19");
    }

    @Test
    void aPageWithNoActivitiesHasNoTotalRatherThanZero() {
        // Zero would be a claim — that the operator listed no 付随的な排出活動
        // and the total really is nothing.
        BaselineIncidentalEmissionActivityRequest page = new BaselineIncidentalEmissionActivityRequest();

        derive("baseline-incidental-emission-activity", page);

        assertThat(page.getEmissionsTotalValue()).isNull();
    }

    // --- 10.2(2): two totals, and 影響度 as a number ---

    @Test
    void postImplementActivitiesTotalBothColumns() {
        PostImplementIncidentalActivityRequest page = new PostImplementIncidentalActivityRequest();
        page.setActivities(List.of(
                postActivity("パワーコンディショナー等の使用", "52.08", "4.07"),
                postActivity("蓄電池の使用", "178.11", "13.92")));

        derive("post-implement-incidental-activity", page);

        assertThat(page.getEmissionsTotalValue()).isEqualByComparingTo("230.19");
        assertThat(page.getImpactTotalValue()).isEqualByComparingTo("17.99");
    }

    @Test
    void whateverTotalTheClientPostsIsReplaced() {
        PostImplementIncidentalActivityRequest page = new PostImplementIncidentalActivityRequest();
        page.setActivities(List.of(postActivity("蓄電池の使用", "100", "5")));
        page.setEmissionsTotalValue(new BigDecimal("999999"));
        page.setImpactTotalValue(new BigDecimal("999999"));

        derive("post-implement-incidental-activity", page);

        assertThat(page.getEmissionsTotalValue()).isEqualByComparingTo("100");
        assertThat(page.getImpactTotalValue()).isEqualByComparingTo("5");
    }

    @Test
    void anImpactAboveOneHundredPercentIsRefused() {
        // This is what the operator hit: 影響度 was free text up to 1000
        // characters, so 5000 went in without complaint. It is a share of the
        // 排出削減量 from 10.1.
        PostImplementIncidentalActivityRequest page = validPostImplementPage();
        page.getActivities().get(0).setImpact(new BigDecimal("100.01"));

        assertThat(errorKeysOf(page)).containsExactly("activities[0].impact");
    }

    @Test
    void anImpactWithinRangeIsAccepted() {
        PostImplementIncidentalActivityRequest page = validPostImplementPage();

        assertThat(errorKeysOf(page)).isEmpty();

        page.getActivities().get(0).setImpact(BigDecimal.ZERO);
        assertThat(errorKeysOf(page)).isEmpty();

        page.getActivities().get(0).setImpact(new BigDecimal("100"));
        assertThat(errorKeysOf(page)).isEmpty();
    }

    @Test
    void theTwoTablesDisagreeOnHowManyMonitoringChoicesThereAre() {
        // 10.2(1) offers 行う／行わない, 10.2(2) offers three. The sheet lists
        // them under each table, and the old DTOs already differed this way.
        BaselineIncidentalActivityRow baseline = baselineActivity("活動", "1", true);
        PostImplementIncidentalActivityRow post = postActivity("活動", "1", "1");

        assertThat(baseline.getMonitorCalcBeingDone()).isInstanceOf(Boolean.class);
        assertThat(post.getMonitorCalcBeingDone()).isInstanceOf(String.class);
    }

    // --- 10.1 is deliberately untouched ---

    @Test
    void tenPointOneKeepsItsThreeNamedRows() {
        // ER / EMBL / EMPJ are fixed by the sheet — symbol and 定義 pre-printed,
        // only 単位 and 数値 entered. A row list would replace three named
        // fields with three rows that can never be anything else.
        assertThat(registry.bySlug("emission-reduction-calculation-method")).isPresent();
        assertThat(registry.bySlug("emission-reduction-calculation-method").orElseThrow()
                .derivation()).isNull();
    }

    // --- helpers ---

    private void derive(String slug, com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormBase page) {
        registry.bySlug(slug).orElseThrow().applyDerivation(page, null);
    }

    private List<String> errorKeysOf(Object page) {
        return validator.validate(page).stream()
                .map(violation -> JsonFieldNameResolver.resolve(
                        page.getClass(), violation.getPropertyPath().toString()))
                .distinct()
                .sorted()
                .toList();
    }

    private static PostImplementIncidentalActivityRequest validPostImplementPage() {
        PostImplementIncidentalActivityRequest page = new PostImplementIncidentalActivityRequest();
        page.setConcept("電力制御装置等の使用による排出を考慮する。");
        page.setActivities(List.of(postActivity("パワーコンディショナー等の使用", "52.08", "4.07")));
        return page;
    }

    private static FormulaSymbolRow symbol(
            String symbol, String definition, String unit, String expectedValue) {
        FormulaSymbolRow row = new FormulaSymbolRow();
        row.setSymbol(symbol);
        row.setDefinition(definition);
        row.setUnit(unit);
        row.setExpectedValue(new BigDecimal(expectedValue));
        return row;
    }

    private static BaselineIncidentalActivityRow baselineActivity(
            String act, String emissions, boolean calculated) {
        BaselineIncidentalActivityRow row = new BaselineIncidentalActivityRow();
        row.setEmissionAct(act);
        row.setGreenhouseGasType("CO2");
        row.setEmissions(new BigDecimal(emissions));
        row.setMonitorCalcBeingDone(calculated);
        return row;
    }

    private static PostImplementIncidentalActivityRow postActivity(
            String act, String emissions, String impact) {
        PostImplementIncidentalActivityRow row = new PostImplementIncidentalActivityRow();
        row.setEmissionAct(act);
        row.setGreenhouseGasType("CO2");
        row.setEmissions(new BigDecimal(emissions));
        row.setImpact(new BigDecimal(impact));
        row.setMonitorCalcBeingDone("排出量の算定を行う。");
        return row;
    }
}
