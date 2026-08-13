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
 * 設備情報の収集・確認方法（実施前 - 新設）
 *
 * <p>Generated from db/projectplan.xlsx (table {@code equipment_informatio_verification_method}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class EquipmentVerificationBeforeNewRequest extends ProjectPlanFormBase {

    /** 方法論の要求事項に沿って標準的な設備の設定の記載 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("standardEquipmentSetting")
    private String standardEquipmentSetting;
}
