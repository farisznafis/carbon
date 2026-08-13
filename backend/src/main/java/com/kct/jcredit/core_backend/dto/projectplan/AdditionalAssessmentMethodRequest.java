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
 * 追加性の評価
 *
 * <p>Generated from db/projectplan.xlsx (table {@code additional_assesment_method}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class AdditionalAssessmentMethodRequest extends ProjectPlanFormBase {

    /** 追加性の評価方法 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("assessingAdditionalityMethod")
    private String assessingAdditionalityMethod;

    /** 追加性の評価方法（画像） */
    @NotBlank
    @Size(max = 1000)
    @JsonProperty("assessingAdditionalityMethodImg")
    private String assessingAdditionalityMethodImg;
}
