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
 * プロジェクト実施後の付随的な排出活動
 *
 * <p>Generated from db/projectplan.xlsx (table {@code post_impliment_incedental_activity}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class PostImplementIncidentalActivityRequest extends ProjectPlanFormBase {

    /** 考え方 */
    @NotBlank
    @Size(max = 2000)
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
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("greenhouseGasType")
    private String greenhouseGasType;

    /** 排出量(t-CO2/年) */
    @NotNull
    @JsonProperty("emissions")
    private BigDecimal emissions;

    /** 影響度(％) */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("impact")
    private String impact;

    /**
     * モニタリング・算定方法 - 排出量の算定実施.
     *
     * <p>Three choices on the official form, not two: alongside 行う and
     * 省略する there is 「排出量の算定を行う。ただし、排出量のモニタリングを省略し、
     * 影響度により排出量を評価する。」. The DB spec flattened that to a boolean,
     * which left the third choice with nowhere to go.
     */
    @NotBlank
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("monitorCalcBeingDone")
    private String monitorCalcBeingDone;

    /** 排出量合計値 */
    @JsonProperty("emissionsTotalValue")
    private BigDecimal emissionsTotalValue;

    /** 影響度合計値 */
    @JsonProperty("impactTotalValue")
    private BigDecimal impactTotalValue;
}
