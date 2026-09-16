package com.kct.jcredit.core_backend.dto.projectplan;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.JapaneseText;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * One row of 11.モニタリング計画 — of either （１）活動量 or （２）係数.
 *
 * <p>The sheet is one table with grouped column headers:
 *
 * <pre>
 * モニタリング項目      | モニタリング方法   | プロジェクト計画での想定 | 備考
 * 記号 / 定義 / 単位    | 分類 / 概要 / 頻度 | 想定値 / 根拠            |
 * </pre>
 *
 * <p>分類 is <b>not</b> here. （１）活動量 uses Ａ・Ｂ・Ｃ and （２）係数 uses
 * Ⅰ・Ⅱ・Ⅲ — the sheet carries a separate ※1 note under each table — so each
 * concrete row declares its own, with its own permitted values. Everything else
 * is identical, which is why this base exists at all.
 *
 * <p>Deliberately not {@code @DynamoDbBean}: only the concrete subclasses are.
 * {@code TableSchema.fromBean} walks inherited getters, so the columns here are
 * mapped on both.
 *
 * <p>We used to store those three groups as three separate pages, each its own
 * DynamoDB item with no row identifier — so nothing said which 定義 belonged to
 * which 分類, and the sheet could not be reproduced from what we stored. All
 * nine columns live on one row here, which is what makes the rows joinable
 * again.
 *
 * <p><b>One モニタリング項目 may take several rows.</b> The sheet's own note
 * says 「複数のモニタリング方法を用いるモニタリング項目については、想定される
 * 手法を全て記載すること」, and the solar example does exactly that: ELPV appears
 * with 分類Ｂ on one row and 分類Ｃ on the next, the second leaving 定義 and 単位
 * blank. So those two are optional, and 記号 is required on every row — a
 * continuation row repeats it rather than leaving it empty, so a row still says
 * what it belongs to when read on its own.
 */
@Getter
@Setter
public abstract class MonitoringPlanRow implements ProjectPlanFormRow {

    /** 記号. Repeated on a continuation row, so every row identifies its item. */
    @NotBlank
    @Size(max = 200)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("symbol")
    private String symbol;

    /** 定義. Blank on a row that adds another モニタリング方法 to the item above. */
    @Size(max = 500)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("definition")
    private String definition;

    /**
     * 単位. Blank on a continuation row.
     *
     * <p>No character-set rule: real values are {@code kwh/年},
     * {@code kg-CO2/kwh} — half-width symbols mixed with kanji, which every
     * one-sided rule rejects half of.
     */
    @Size(max = 50)
    @JsonProperty("unit")
    private String unit;

    /** 概要 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("overview")
    private String overview;

    /** 頻度 — 年毎, 検証申請時 and the like. */
    @NotBlank
    @Size(max = 50)
    @JsonProperty("frequency")
    private String frequency;

    /** 想定値 */
    @NotNull
    @JsonProperty("expectedValue")
    private BigDecimal expectedValue;

    /**
     * 根拠. Optional — a continuation row states the method, not the arithmetic.
     *
     * <p>Capped at 1000, not the 200 the old DTO used. These hold the whole
     * calculation: the solar example's longest is already <b>194 characters</b>,
     * so one slightly longer formula would have made a real plan unsaveable.
     */
    @Size(max = 1000)
    @JsonProperty("basis")
    private String basis;

    /** 備考. Optional; the solar example's longest is 312 characters. */
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("remarks")
    private String remarks;
}
