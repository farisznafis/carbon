package com.kct.jcredit.core_backend.dto.projectplan;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * One 年度 of 4.1 プログラム型プロジェクトの全体計画.
 *
 * <p>Columns are the official sheet's, read from
 * {@code 別紙7.プロジェクト計画書フォーマット.xlsx} row 6 and the solar example:
 *
 * <pre>
 * 年度 | 新規会員数（件） | 認証対象となる累計会員数（件） | 延べ会員数（件）
 *      | ベースライン排出量 + 単位 | プロジェクト実施後排出量 + 単位
 *      | 年度内の認証申請期間日数 | 年度開始日 | 年度終了日 | 年度内日数
 * </pre>
 *
 * <p>The last four, plus 年度 and both 単位, are computed by
 * {@code EmissionReductionPlanDerivation} and arrive read-only. The sheet
 * computes them too; asking the operator to type 38 fiscal-year calendars is
 * asking for arithmetic nothing then checks.
 *
 * <p>The two 単位 columns follow the page's 個々の削減活動の排出量の単位, per the
 * sheet's note 「シート内、「単位」で選択した単位と揃えてください」.
 */
@Data
@NoArgsConstructor
@DynamoDbBean
public class EmissionReductionYearRow implements ProjectPlanFormRow {

    /** 年度. Derived — the sheet fixes the table at 2013–2050 in both workbooks. */
    @JsonProperty("year")
    private Integer year;

    /** 年度開始日 (yyyy-MM-dd). Derived: 4月1日 of 年度. */
    @JsonProperty("fiscalYearStart")
    private String fiscalYearStart;

    /** 年度終了日 (yyyy-MM-dd). Derived: 3月31日 of the following year. */
    @JsonProperty("fiscalYearEnd")
    private String fiscalYearEnd;

    /** 年度内日数. Derived — 366 when the fiscal year spans a leap February. */
    @JsonProperty("fiscalYearDays")
    private Integer fiscalYearDays;

    /** 年度内の認証申請期間日数. Derived: overlap of 認証対象期間 with this 年度. */
    @JsonProperty("certApplicationDays")
    private Integer certApplicationDays;

    /** 新規会員数（件） */
    @NotNull
    @PositiveOrZero
    @JsonProperty("newMembersTotal")
    private Integer newMembersTotal;

    /** 認証対象となる累計会員数（件） */
    @NotNull
    @PositiveOrZero
    @JsonProperty("cumulativeMembersTotal")
    private Integer cumulativeMembersTotal;

    /** 延べ会員数（件） */
    @PositiveOrZero
    @JsonProperty("membersTotal")
    private Integer membersTotal;

    /** ベースライン排出量. In the page's 単位, not necessarily t-CO2. */
    @JsonProperty("baselineEmissions")
    private BigDecimal baselineEmissions;

    /** ベースライン排出量単位. Derived from the page's 単位. */
    @JsonProperty("baselineEmissionsUnit")
    private String baselineEmissionsUnit;

    /** プロジェクト実施後排出量. In the page's 単位. */
    @JsonProperty("afterImplEmissions")
    private BigDecimal afterImplEmissions;

    /** プロジェクト実施後排出量単位. Derived from the page's 単位. */
    @JsonProperty("afterImplEmissionsUnit")
    private String afterImplEmissionsUnit;
}
