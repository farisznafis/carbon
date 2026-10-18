package com.kct.jcredit.core_backend.service.projectplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.kct.jcredit.core_backend.dto.projectplan.MonitoringActivityPlanRequest;
import com.kct.jcredit.core_backend.dto.projectplan.MonitoringActivityPlanRow;
import com.kct.jcredit.core_backend.dto.projectplan.MonitoringCoefficientPlanRequest;
import com.kct.jcredit.core_backend.dto.projectplan.MonitoringCoefficientPlanRow;
import com.kct.jcredit.core_backend.util.JsonFieldNameResolver;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

/**
 * 分類 on the two 11.モニタリング計画 tables.
 *
 * <p>The sheet carries a ※1 note under each, and they name different lists:
 * 「分類Ａ・Ｂ・Ｃのいずれかの方法を選択すること」 under （１）活動量, and
 * 「分類Ⅰ・Ⅱ・Ⅲのいずれかの方法を選択すること」 under （２）係数. The solar
 * example stores half-width {@code B} on one and the Roman numeral {@code Ⅲ}
 * on the other.
 */
class MonitoringClassificationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void theActivityTableTakesAAndBAndC() {
        List.of("A", "B", "C").forEach(value ->
                assertThat(errorKeysOf(activityPage(value))).as("分類 %s", value).isEmpty());
    }

    @Test
    void theCoefficientTableTakesTheRomanNumerals() {
        List.of("Ⅰ", "Ⅱ", "Ⅲ").forEach(value ->
                assertThat(errorKeysOf(coefficientPage(value))).as("分類 %s", value).isEmpty());
    }

    @Test
    void aValueFromTheOtherTablesListIsRefused() {
        // The two used to share one free-text field, so 分類Ⅲ could be filed
        // against 活動量 and nothing said otherwise.
        assertThat(errorKeysOf(activityPage("Ⅲ")))
                .containsExactly("items[0].classification");
        assertThat(errorKeysOf(coefficientPage("B")))
                .containsExactly("items[0].classification");
    }

    @Test
    void anEmptyClassificationIsStillRefused() {
        assertThat(errorKeysOf(activityPage(null)))
                .containsExactly("items[0].classification");
    }

    @Test
    void theSharedColumnsStillValidateOnBoth() {
        // The eight columns live on the abstract base now; a subclass that
        // stopped inheriting them would pass everything silently.
        MonitoringActivityPlanRow row = activityRow("B");
        row.setSymbol("  ");
        MonitoringActivityPlanRequest page = new MonitoringActivityPlanRequest();
        page.setItems(List.of(row));

        assertThat(errorKeysOf(page)).containsExactly("items[0].symbol");
    }

    // --- helpers ---

    private List<String> errorKeysOf(Object page) {
        return validator.validate(page).stream()
                .map(violation -> JsonFieldNameResolver.resolve(
                        page.getClass(), violation.getPropertyPath().toString()))
                .distinct()
                .sorted()
                .toList();
    }

    private static MonitoringActivityPlanRequest activityPage(String classification) {
        MonitoringActivityPlanRequest page = new MonitoringActivityPlanRequest();
        page.setItems(List.of(activityRow(classification)));
        return page;
    }

    private static MonitoringCoefficientPlanRequest coefficientPage(String classification) {
        MonitoringCoefficientPlanRow row = new MonitoringCoefficientPlanRow();
        fill(row);
        row.setSymbol("CEFelectricity,t");
        row.setClassification(classification);
        MonitoringCoefficientPlanRequest page = new MonitoringCoefficientPlanRequest();
        page.setItems(List.of(row));
        return page;
    }

    private static MonitoringActivityPlanRow activityRow(String classification) {
        MonitoringActivityPlanRow row = new MonitoringActivityPlanRow();
        fill(row);
        row.setClassification(classification);
        return row;
    }

    private static void fill(com.kct.jcredit.core_backend.dto.projectplan.MonitoringPlanRow row) {
        row.setSymbol("ELPV");
        row.setDefinition("プロジェクト実施後の太陽光発電設備による発電電力量");
        row.setUnit("kwh/年");
        row.setOverview("電力量計で計測を行う。");
        row.setFrequency("年毎");
        row.setExpectedValue(new BigDecimal("6000"));
    }
}
