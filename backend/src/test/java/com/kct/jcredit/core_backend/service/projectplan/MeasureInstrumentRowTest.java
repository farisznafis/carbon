package com.kct.jcredit.core_backend.service.projectplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.kct.jcredit.core_backend.dto.projectplan.MeasureOtherInstrumentsRequest;
import com.kct.jcredit.core_backend.dto.projectplan.MeasureSpecifiedInstrumentRequest;
import com.kct.jcredit.core_backend.dto.projectplan.OtherInstrumentRow;
import com.kct.jcredit.core_backend.dto.projectplan.SpecifiedInstrumentRow;
import com.kct.jcredit.core_backend.util.JsonFieldNameResolver;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

/**
 * 12.1（１）計量器の概要, once its two tables hold rows.
 *
 * <p>Values are the solar example's: two 計量器 in each table, ELPV on a
 * 電力量計 at point M1 and ELpvr on a スマートメーター at Ｍ2.
 */
class MeasureInstrumentRowTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void bothTablesTakeEveryInstrumentThePlanMeasures() {
        MeasureSpecifiedInstrumentRequest specified = new MeasureSpecifiedInstrumentRequest();
        specified.setMonitoringPointDiagram("projects/1/plan/measure-specified-instrument/diagram.png");
        specified.setInstruments(List.of(
                specified("ELPV プロジェクト実施後の発電電力量", "電力量計", "M1", "10年間"),
                specified("ELpvr1，ELpvr2 他社に提供した電力量", "スマートメーター", "M2", "10年間")));

        assertThat(errorKeysOf(specified)).isEmpty();
        assertThat(specified.getInstruments()).hasSize(2);
    }

    @Test
    void aCalibrationDescriptionMayBeBlank() {
        // The official example fills 計量器の校正方法の説明 on its first row and
        // leaves it blank on the second. Required, as it was, the page would
        // refuse a table the example itself contains.
        MeasureOtherInstrumentsRequest page = otherPage();
        page.getInstruments().get(1).setInstrumentCalibrationMethodDescription(null);

        assertThat(errorKeysOf(page)).isEmpty();
    }

    @Test
    void aMonitoringPointMayBeFullWidth() {
        // The example writes M1 on one row and Ｍ2 on the next.
        MeasureOtherInstrumentsRequest page = otherPage();
        page.getInstruments().get(0).setMonitoringPoint("Ｍ2");

        assertThat(errorKeysOf(page)).isEmpty();
    }

    @Test
    void anIncompleteInstrumentIsReportedAgainstItsOwnRow() {
        MeasureSpecifiedInstrumentRequest page = new MeasureSpecifiedInstrumentRequest();
        page.setMonitoringPointDiagram("diagram.png");
        page.setInstruments(List.of(
                specified("ELPV 発電電力量", "電力量計", "M1", "10年間"),
                specified("ELpvr 売電電力量", "スマートメーター", "M2", null)));

        assertThat(errorKeysOf(page)).containsExactly("instruments[1].validityPeriodOfCert");
    }

    @Test
    void anEmptyTableIsRefused() {
        MeasureSpecifiedInstrumentRequest page = new MeasureSpecifiedInstrumentRequest();
        page.setMonitoringPointDiagram("diagram.png");

        assertThat(errorKeysOf(page)).containsExactly("instruments");
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

    private static MeasureOtherInstrumentsRequest otherPage() {
        MeasureOtherInstrumentsRequest page = new MeasureOtherInstrumentsRequest();
        page.setMonitoringPointDiagram("diagram.png");
        page.setInstruments(List.of(
                other("ELPV 発電電力量", "パワーコンディショナーの電力量計", "M1",
                        "運営・管理者が各会員から機器情報を入手し、出荷検査を確認する。"),
                other("ELpvr 売電電力量", "電力量計", "M2", "同上")));
        return page;
    }

    private static SpecifiedInstrumentRow specified(
            String item, String type, String point, String validity) {
        SpecifiedInstrumentRow row = new SpecifiedInstrumentRow();
        row.setMonitoringItem(item);
        row.setInstrumentType(type);
        row.setMonitoringPoint(point);
        row.setValidityPeriodOfCert(validity);
        return row;
    }

    private static OtherInstrumentRow other(
            String item, String type, String point, String calibration) {
        OtherInstrumentRow row = new OtherInstrumentRow();
        row.setMonitoringItem(item);
        row.setInstrumentType(type);
        row.setMonitoringPoint(point);
        row.setInstrumentCalibrationMethodDescription(calibration);
        return row;
    }
}
