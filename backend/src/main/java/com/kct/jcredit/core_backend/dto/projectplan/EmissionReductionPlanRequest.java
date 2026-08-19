package com.kct.jcredit.core_backend.dto.projectplan;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.JapaneseText;
import com.kct.jcredit.core_backend.validation.DateFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * 排出削減計画
 *
 * <p>Generated from db/projectplan.xlsx (table {@code emission_reduction_plan}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class EmissionReductionPlanRequest extends ProjectPlanFormBase {

    /** 個々の削減活動の排出量の単位 */
    @NotBlank
    @Size(max = 20)
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

    /** 認証対象期間　年間（0.0年） */
    @JsonProperty("certTargetPeriodYears")
    private BigDecimal certTargetPeriodYears;

    /** 年度 */
    @JsonProperty("year")
    private Integer year;

    /** 新規会員数（件） */
    @NotNull
    @JsonProperty("newMembersTotal")
    private Integer newMembersTotal;

    /** 認証対象となる累計会員数（件） */
    @NotNull
    @JsonProperty("cumulativeMembersTotal")
    private Integer cumulativeMembersTotal;

    /** 延べ会員数（件） */
    @JsonProperty("membersTotal")
    private Integer membersTotal;

    /** ベースライン排出量 */
    @JsonProperty("baselineEmissions")
    private BigDecimal baselineEmissions;

    /** ベースライン排出量単位 */
    @Size(max = 20)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("baselineEmissionsUnit")
    private String baselineEmissionsUnit;

    /** プロジェクト実施後排出量 */
    @JsonProperty("afterImplEmissions")
    private BigDecimal afterImplEmissions;

    /** プロジェクト実施後排出量単位 */
    @Size(max = 20)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("afterImplEmissionsUnit")
    private String afterImplEmissionsUnit;

    /** 合計／ベースライン排出量 */
    @JsonProperty("baselineEmissionsTotal")
    private BigDecimal baselineEmissionsTotal;

    /** 合計／プロジェクト実施後排出量 */
    @JsonProperty("afterImplEmissionsUnitTotal")
    private BigDecimal afterImplEmissionsUnitTotal;

    /** 排出削減量の総量 */
    @JsonProperty("emissionReductionTotal")
    private BigDecimal emissionReductionTotal;
}
