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
 * 排出削減量の算定方法
 *
 * <p>Generated from db/projectplan.xlsx (table {@code emission_reduction_calculation_method}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class EmissionReductionCalculationMethodRequest extends ProjectPlanFormBase {

    /** 排出削減量 */
    @JsonProperty("emissionReduction")
    private BigDecimal emissionReduction;

    /** 排出削減量単位 */
    @NotBlank
    @Size(max = 10)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("emissionReductionUnit")
    private String emissionReductionUnit;

    /** ベースライン排出量 */
    @NotNull
    @JsonProperty("baselineEmission")
    private BigDecimal baselineEmission;

    /** ベースライン排出量単位 */
    @NotBlank
    @Size(max = 10)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("baselineEmissionUnit")
    private String baselineEmissionUnit;

    /** プロジェクト実施後排出量 */
    @NotNull
    @JsonProperty("postProjectEmission")
    private BigDecimal postProjectEmission;

    /** プロジェクト実施後排出量単位 */
    @NotBlank
    @Size(max = 10)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("postProjectEmissionUnit")
    private String postProjectEmissionUnit;
}
