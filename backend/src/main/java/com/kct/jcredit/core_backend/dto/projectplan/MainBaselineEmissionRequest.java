package com.kct.jcredit.core_backend.dto.projectplan;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.JapaneseText;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * ベースライン排出量（主要排出活動）
 *
 * <p>Generated from db/projectplan.xlsx (table {@code main_baseline_emission}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class MainBaselineEmissionRequest extends ProjectPlanFormBase {

    /** 方法論の評価式 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("evaluationFormula")
    private String evaluationFormula;

    /** 方法論の式番号 */
    @NotNull
    @JsonProperty("formulaNumber")
    private Integer formulaNumber;

    /** 記号 */
    @Size(max = 50)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("symbol")
    private String symbol;

    /** 定義 */
    @Size(max = 500)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("definition")
    private String definition;

    /** 単位 */
    @Size(max = 50)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("unit")
    private String unit;

    /** 想定値 */
    @NotNull
    @JsonProperty("expectedValue")
    private BigDecimal expectedValue;
}
