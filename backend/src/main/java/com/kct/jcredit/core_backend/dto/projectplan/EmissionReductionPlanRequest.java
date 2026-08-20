package com.kct.jcredit.core_backend.dto.projectplan;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.DateFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * 4.排出削減計画 — 4.1 プログラム型プロジェクトの全体計画.
 *
 * <p>The sheet is a table of fiscal years, 2013–2050 in both official
 * workbooks, with a 合計 row beneath it. Everything per-year lives in
 * {@link EmissionReductionYearRow}; what stays here is the page's own header
 * (単位, 認証対象期間) and the totals under the table.
 *
 * <p>Was previously generated from db/projectplan.xlsx (table
 * {@code emission_reduction_plan}), which has no way to express a repeating
 * table and so flattened all 38 years into one set of columns — a project
 * could record a single year of its plan and nothing more.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class EmissionReductionPlanRequest extends ProjectPlanFormBase {

    /**
     * The 算定単位 values this sheet uses. The pulldown sheet lists these
     * separately from {@code t-CO2/年} / {@code kg-CO2/年}, and only an
     * absolute unit can be summed into the 合計 row.
     */
    public static final String EMISSION_UNIT_PATTERN = "t-CO2|kg-CO2";

    /**
     * 個々の削減活動の排出量の単位.
     *
     * <p>Governs every row: the sheet says 「シート内、「単位」で選択した単位と
     * 揃えてください。なお最終的な合計は選択内容に関わらず t-CO2 となります。」 —
     * so the rows carry this unit and the totals below are converted.
     */
    @NotBlank
    @Pattern(regexp = EMISSION_UNIT_PATTERN, message = "単位は t-CO2 または kg-CO2 を選択してください。")
    @JsonProperty("unitEmission")
    private String unitEmission;

    /** 認証対象期間（開始）(西暦年月日) */
    @DateFormat
    @JsonProperty("certTargetPeriodStart")
    private String certTargetPeriodStart;

    /** 認証対象期間（終了）(西暦年月日) */
    @DateFormat
    @JsonProperty("certTargetPeriodEnd")
    private String certTargetPeriodEnd;

    /** 認証対象期間　年間（0.0年）. Divides 排出削減量の総量 into the 年平均. */
    @JsonProperty("certTargetPeriodYears")
    private BigDecimal certTargetPeriodYears;

    /**
     * 4.1 の年度表。1 行 = 1 年度。
     *
     * <p>Never empty in practice: the server rebuilds the 2013–2050 skeleton on
     * every read and every save, so a client cannot drop or add a year.
     */
    @Valid
    @NotEmpty
    @JsonProperty("years")
    private List<EmissionReductionYearRow> years;

    /** 合計／ベースライン排出量. Derived, always t-CO2 whatever the rows use. */
    @JsonProperty("baselineEmissionsTotal")
    private BigDecimal baselineEmissionsTotal;

    /** 合計／プロジェクト実施後排出量. Derived, always t-CO2. */
    @JsonProperty("afterImplEmissionsUnitTotal")
    private BigDecimal afterImplEmissionsUnitTotal;

    /** 排出削減量の総量 (計画値). Derived: 合計ベースライン − 合計実施後. */
    @JsonProperty("emissionReductionTotal")
    private BigDecimal emissionReductionTotal;

    /** 排出削減量の年平均. Derived: 総量 ÷ 認証対象期間（年）. */
    @JsonProperty("emissionReductionAnnualAverage")
    private BigDecimal emissionReductionAnnualAverage;
}
