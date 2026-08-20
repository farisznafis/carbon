package com.kct.jcredit.core_backend.dto.projectplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.JapaneseText;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * 係数の実測方法に関する説明
 *
 * <p>Generated from db/projectplan.xlsx (table {@code coefficient_measure_explaination}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class CoefficientMeasureExplanationRequest extends ProjectPlanFormBase {

    /** モニタリング項目 */
    @NotBlank
    @Size(max = 200)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("monitoringItem")
    private String monitoringItem;

    /** モニタリング項目説明 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("monitoringItemDescription")
    private String monitoringItemDescription;

    /** 推定・概算方法 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("estimationMethod")
    private String estimationMethod;
}
