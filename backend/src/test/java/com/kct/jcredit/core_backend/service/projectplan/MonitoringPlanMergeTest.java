package com.kct.jcredit.core_backend.service.projectplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.kct.jcredit.core_backend.dto.projectplan.MonitoringActivityPlanRequest;
import com.kct.jcredit.core_backend.dto.projectplan.MonitoringActivityPlanRow;
import com.kct.jcredit.core_backend.dto.projectplan.MonitoringCoefficientPlanRequest;
import com.kct.jcredit.core_backend.dto.projectplan.MonitoringCoefficientPlanRow;

import com.kct.jcredit.core_backend.util.JsonFieldNameResolver;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 11.モニタリング計画 after the six-page merge.
 *
 * <p>Values are the solar example's
 * ({@code sakuseirei_keikaku_solar_program.xlsx}, 方法論①), because the point of
 * the merge is that a real plan can be stored — and the previous shape could
 * not hold one.
 */
class MonitoringPlanMergeTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private final ProjectPlanFormRegistry registry = new ProjectPlanFormRegistry();

    /** The example's longest 根拠, 194 characters — over the old @Size(max = 200)'s head. */
    private static final String LONG_BASIS = "以下の式から算定：".repeat(25) + "。";

    // --- the rows are joinable again ---

    @Test
    void allNineColumnsLiveOnOneRow() {
        MonitoringActivityPlanRequest page = new MonitoringActivityPlanRequest();
        page.setPk("project-1");
        page.setSk("MONITORING_PLAN_ACTIVITY");
        page.setItems(List.of(elpv("B"), elpvContinuation("C")));

        TableSchema<MonitoringActivityPlanRequest> schema =
                TableSchema.fromBean(MonitoringActivityPlanRequest.class);
        Map<String, AttributeValue> stored = schema.itemToMap(page, true);
        MonitoringActivityPlanRequest read = schema.mapToItem(stored);

        // Split across three pages, nothing said which 定義 went with which 分類.
        MonitoringActivityPlanRow first = read.getItems().get(0);
        assertThat(first.getSymbol()).isEqualTo("ELPV");
        assertThat(first.getDefinition()).startsWith("プロジェクト実施後");
        assertThat(first.getUnit()).isEqualTo("kwh/年");
        assertThat(first.getClassification()).isEqualTo("B");
        assertThat(first.getFrequency()).isEqualTo("年毎");
        assertThat(first.getExpectedValue()).isEqualByComparingTo("6000");
    }

    @Test
    void oneItemMayTakeSeveralRowsForItsSeveralMethods() {
        // 「複数のモニタリング方法を用いるモニタリング項目については、想定される
        // 手法を全て記載すること」 — ELPV carries 分類Ｂ and 分類Ｃ in the example,
        // the second row leaving 定義 / 単位 / 根拠 / 備考 blank.
        MonitoringActivityPlanRequest page = new MonitoringActivityPlanRequest();
        page.setItems(List.of(elpv("B"), elpvContinuation("C")));

        assertThat(errorKeysOf(page)).isEmpty();
    }

    // --- the bug the old shape had ---

    @Test
    void theCoefficientTableAcceptsItsOwnRomanNumeralClassification() {
        // （２）係数 uses 分類Ⅰ・Ⅱ・Ⅲ, not the Ａ・Ｂ・Ｃ of （１）活動量. Both old
        // DTOs put @JapaneseText on 分類, which rejects U+2160 onwards — so the
        // coefficient page rejected every value its own ※1 note offers.
        MonitoringCoefficientPlanRequest page = new MonitoringCoefficientPlanRequest();
        page.setItems(List.of(coefficientRow("Ⅲ")));

        assertThat(errorKeysOf(page)).isEmpty();
    }

    @Test
    void aBasisLongerThanTheOldCapIsAccepted() {
        MonitoringActivityPlanRow row = elpv("B");
        row.setBasis(LONG_BASIS);
        MonitoringActivityPlanRequest page = new MonitoringActivityPlanRequest();
        page.setItems(List.of(row));

        assertThat(LONG_BASIS.length()).isGreaterThan(200);
        assertThat(errorKeysOf(page)).isEmpty();
    }

    @Test
    void aSymbolWithACommaIsAccepted() {
        // The example's symbols include ELPJ,control and CEFelectricity,t.
        MonitoringActivityPlanRow row = elpv("C");
        row.setSymbol("ELPJ,control");
        MonitoringActivityPlanRequest page = new MonitoringActivityPlanRequest();
        page.setItems(List.of(row));

        assertThat(errorKeysOf(page)).isEmpty();
    }

    // --- errors still land on the right row ---

    @Test
    void aRowMissingItsClassificationIsReportedAgainstThatRow() {
        MonitoringActivityPlanRow incomplete = elpvContinuation(null);
        MonitoringActivityPlanRequest page = new MonitoringActivityPlanRequest();
        page.setItems(List.of(elpv("B"), incomplete));

        assertThat(errorKeysOf(page)).containsExactly("items[1].classification");
    }

    @Test
    void anEmptyTableIsRefused() {
        assertThat(errorKeysOf(new MonitoringActivityPlanRequest())).containsExactly("items");
    }

    // --- the registry ---

    @Test
    void theSixSplitPagesAreGoneAndTwoTablesStandInTheirPlace() {
        List.of("monitoring-item-plan",
                        "monitoring-method-plan",
                        "expected-project-plan",
                        "monitoring-item-plan-coefficient",
                        "monitoring-method-plan-coefficient",
                        "expected-project-plan-coefficient")
                .forEach(slug -> assertThat(registry.bySlug(slug))
                        .as("retired slug %s", slug)
                        .isEmpty());

        assertThat(registry.bySlug("monitoring-plan-activity")).isPresent();
        assertThat(registry.bySlug("monitoring-plan-coefficient")).isPresent();
        // （３）モニタリングデータの収集方法 keeps its page and its sort key.
        assertThat(registry.bySlug("monitoring-plan")).isPresent();
    }

    @Test
    void sectionElevenIsThreePagesNotSeven() {
        // Counting section 11 rather than the whole wizard, so that adding a
        // page elsewhere does not fail a test about this merge.
        assertThat(registry.all())
                .filteredOn(definition -> definition.slug().startsWith("monitoring-plan"))
                .hasSize(3);
    }

    @Test
    void theThreeTablesOfSectionElevenRunInSheetOrder() {
        assertThat(orderOf("monitoring-plan-activity"))
                .isLessThan(orderOf("monitoring-plan-coefficient"));
        assertThat(orderOf("monitoring-plan-coefficient"))
                .isLessThan(orderOf("monitoring-plan"));
    }

    // --- helpers ---

    private int orderOf(String slug) {
        return registry.bySlug(slug).orElseThrow().order();
    }

    private List<String> errorKeysOf(Object page) {
        return validator.validate(page).stream()
                .map(violation -> JsonFieldNameResolver.resolve(
                        page.getClass(), violation.getPropertyPath().toString()))
                .distinct()
                .sorted()
                .toList();
    }

    /** ELPV as the solar example's first row has it. */
    private static MonitoringActivityPlanRow elpv(String classification) {
        MonitoringActivityPlanRow row = new MonitoringActivityPlanRow();
        row.setSymbol("ELPV");
        row.setDefinition("プロジェクト実施後の太陽光発電設備による発電電力量");
        row.setUnit("kwh/年");
        row.setClassification(classification);
        row.setOverview("特定計量器もしくは業界規格によって検定がなされている電力量計で計測を行う。");
        row.setFrequency("年毎");
        row.setExpectedValue(new BigDecimal("6000"));
        row.setBasis("以下の式から算定： 発電電力量（kwh/年）=太陽光発電設備の容量（kw）×24");
        row.setRemarks("【想定値の算定にあたって】・太陽光発電設備の容量（kw）：5kw");
        return row;
    }

    /** The same item's second モニタリング方法: 定義 / 単位 / 根拠 / 備考 all blank. */
    private static MonitoringActivityPlanRow elpvContinuation(String classification) {
        MonitoringActivityPlanRow row = new MonitoringActivityPlanRow();
        row.setSymbol("ELPV");
        row.setClassification(classification);
        row.setOverview("特定計量器でなく、また、業界標準規格によって検定がされていない電力量計で計測を行う。");
        row.setFrequency("年毎");
        row.setExpectedValue(new BigDecimal("6000"));
        return row;
    }

    private static MonitoringCoefficientPlanRow coefficientRow(String classification) {
        MonitoringCoefficientPlanRow row = new MonitoringCoefficientPlanRow();
        row.setSymbol("CEFelectricity,t");
        row.setDefinition("系統電力のＣＯ2排出係数");
        row.setUnit("kg-CO2/kwh");
        row.setClassification(classification);
        row.setOverview("デフォルト値");
        row.setFrequency("検証申請時");
        row.setExpectedValue(new BigDecimal("0.434"));
        row.setBasis("デフォルト値 （2023年度全電源）");
        return row;
    }
}
