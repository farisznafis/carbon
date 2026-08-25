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
 * 設備情報の収集・確認方法（実施前 - 更新）
 *
 * <p>Generated from db/projectplan.xlsx (table {@code equipment_informatio_verification_method}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class EquipmentVerificationBeforeUpdateRequest extends ProjectPlanFormBase {

    /** メーカー */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("manufacturer")
    private String manufacturer;

    /** 型番 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("modelNumber")
    private String modelNumber;

    /** 機器製造番号 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("equipmentSerialNumber")
    private String equipmentSerialNumber;

    /** 出力 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("output")
    private String output;

    /** 導入日 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("launchDate")
    private String launchDate;

    /** 稼働開始日 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("operationStartDate")
    private String operationStartDate;

    /** 法定耐用年数 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("legalUsefulLife")
    private String legalUsefulLife;

    /** 効率 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("efficiency")
    private String efficiency;

    /** 燃料 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("fuel")
    private String fuel;

    /** 用途 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("usage")
    private String usage;
}
