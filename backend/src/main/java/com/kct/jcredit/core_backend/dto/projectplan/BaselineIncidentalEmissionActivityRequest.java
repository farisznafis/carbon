package com.kct.jcredit.core_backend.dto.projectplan;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.Hankaku;
import com.kct.jcredit.core_backend.validation.JapaneseText;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * ベースラインの付随的な排出活動
 *
 * <p>Generated from db/projectplan.xlsx (table {@code baseline_incidental_emission_activity}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class BaselineIncidentalEmissionActivityRequest extends ProjectPlanFormBase {

    /** 考え方 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("concept")
    private String concept;

    /** 排出活動 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("emissionAct")
    private String emissionAct;

    /** 温室効果ガスの種類 */
    @NotBlank
    @Size(max = 20)
    @Hankaku
    @JsonProperty("greenhouseGasType")
    private String greenhouseGasType;

    /** 排出量(t-CO2/年) */
    @NotNull
    @JsonProperty("emissions")
    private BigDecimal emissions;

    /** 排出量の算定実施有無 */
    @NotNull
    @JsonProperty("monitorCalcBeingDone")
    private Boolean monitorCalcBeingDone;

    /** 排出量合計値 */
    @JsonProperty("emissionsTotalValue")
    private BigDecimal emissionsTotalValue;
}
