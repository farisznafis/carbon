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
 * モニタリング方法_計画（係数）
 *
 * <p>Generated from db/projectplan.xlsx (table {@code monitoring_item_plan_coefficient}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class MonitoringMethodPlanCoefficientRequest extends ProjectPlanFormBase {

    /** 分類 */
    @NotBlank
    @Size(max = 200)
    @Hankaku
    @JsonProperty("classification")
    private String classification;

    /** 概要 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("overview")
    private String overview;

    /** 頻度 */
    @NotBlank
    @Size(max = 50)
    @JsonProperty("frequency")
    private String frequency;
}
