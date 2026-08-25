package com.kct.jcredit.core_backend.dto.projectplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.Hankaku;
import com.kct.jcredit.core_backend.validation.JapaneseText;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * モニタリング項目_計画（係数）
 *
 * <p>Generated from db/projectplan.xlsx (table {@code monitoring_item_plan_coefficient}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class MonitoringItemPlanCoefficientRequest extends ProjectPlanFormBase {

    /** 記号 */
    @NotBlank
    @Size(max = 200)
    @Hankaku
    @JsonProperty("symbol")
    private String symbol;

    /** 定義 */
    @NotBlank
    @Size(max = 500)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("definition")
    private String definition;

    /** 単位 */
    @NotBlank
    @Size(max = 50)
    @JsonProperty("unit")
    private String unit;
}
