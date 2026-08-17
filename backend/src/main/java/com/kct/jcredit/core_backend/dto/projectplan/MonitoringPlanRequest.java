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
 * モニタリング計画
 *
 * <p>Generated from db/projectplan.xlsx (table {@code monitoring_plan}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class MonitoringPlanRequest extends ProjectPlanFormBase {

    /** モニタリング項目 */
    @NotBlank
    @Size(max = 1000)
    @JsonProperty("monitoringItemList")
    private String monitoringItemList;

    /** モニタリングデータの収集方法の説明 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("methodCollDescription")
    private String methodCollDescription;

    /** モニタリングデータの収集方法の説明（画像） */
    @NotBlank
    @Size(max = 1000)
    @JsonProperty("methodCollDescriptionImg")
    private String methodCollDescriptionImg;
}
