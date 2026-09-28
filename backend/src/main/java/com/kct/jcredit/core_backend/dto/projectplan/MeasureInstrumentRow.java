package com.kct.jcredit.core_backend.dto.projectplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.JapaneseText;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * One 計量器 of 12.1（１）計量器の概要.
 *
 * <p>The sub-section is two tables — ①特定計量器の場合 and
 * ②特定計量器以外の計量器の場合 — sharing three columns and differing in the
 * fourth. The three are here; each concrete row adds its own.
 *
 * <p>Both pages held a single 計量器 before, while the solar example lists two
 * in each table, so a plan measuring more than one thing could not be recorded.
 *
 * <p>Deliberately not {@code @DynamoDbBean}: only the concrete rows are.
 * {@code TableSchema.fromBean} walks inherited getters.
 */
@Getter
@Setter
public abstract class MeasureInstrumentRow implements ProjectPlanFormRow {

    /** モニタリング項目 — 記号 and its description, e.g. 「ELPV プロジェクト実施後の…」. */
    @NotBlank
    @Size(max = 200)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("monitoringItem")
    private String monitoringItem;

    /** 計量器の種類, e.g. 電力量計 / スマートメーター. */
    @NotBlank
    @Size(max = 200)
    @JsonProperty("instrumentType")
    private String instrumentType;

    /**
     * モニタリングポイント. The sheet's ※1:
     * 「モニタリングポイントは(2)と整合する番号を記載すること。」 — so it is a
     * label keyed to the diagram, {@code M1} / {@code Ｍ2} in the example, and
     * the example mixes half- and full-width M.
     */
    @NotBlank
    @Size(max = 50)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("monitoringPoint")
    private String monitoringPoint;
}
