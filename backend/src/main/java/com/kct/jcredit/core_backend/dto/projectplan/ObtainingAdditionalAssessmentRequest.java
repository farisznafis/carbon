package com.kct.jcredit.core_backend.dto.projectplan;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * 追加性評価に必要な情報の入手方法
 *
 * <p>Generated from db/projectplan.xlsx (table {@code obtaining_additional_assesment}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class ObtainingAdditionalAssessmentRequest extends ProjectPlanFormBase {

    /** 項目 */
    @NotBlank
    @Size(max = 100)
    @JsonProperty("item")
    private String item;

    /** 入手方法 */
    @NotBlank
    @Size(max = 255)
    @JsonProperty("acquisitionMethod")
    private String acquisitionMethod;
}
