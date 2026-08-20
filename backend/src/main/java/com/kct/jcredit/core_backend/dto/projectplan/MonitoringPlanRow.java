package com.kct.jcredit.core_backend.dto.projectplan;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.JapaneseText;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

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
@Data
@NoArgsConstructor
@DynamoDbBean
public class MonitoringPlanRow implements ProjectPlanFormRow {

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

    /**
     * 分類.
     *
     * <p>Deliberately carries no character-set rule. （１）活動量 uses Ａ・Ｂ・Ｃ
     * and （２）係数 uses <b>Ⅰ・Ⅱ・Ⅲ</b> — the sheet's two ※1 notes say so
     * explicitly, and the solar example reads "B" here and "Ⅲ" on the
     * coefficient table. {@code @JapaneseText} rejects Roman numerals
     * (U+2160–), so the rule that was on both old DTOs made the coefficient
     * table impossible to fill with its own official values.
     *
     * <p>Making this a real Ａ／Ｂ／Ｃ + Ⅰ／Ⅱ／Ⅲ choice is stage F; it also
     * selects whether 12.1 or 12.2 applies.
     */
    @NotBlank
    @Size(max = 200)
    @JsonProperty("classification")
    private String classification;

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
