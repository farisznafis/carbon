package com.kct.jcredit.core_backend.dto.projectplan;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * One 排出活動 of 10.2（２）プロジェクト実施後の付随的な排出活動.
 *
 * <p>Columns: 排出活動 / 温室効果ガスの種類 / 排出量 / 影響度(％) /
 * モニタリング・算定方法. Both 排出量 and 影響度 are totalled beneath the table.
 */
@Data
@NoArgsConstructor
@DynamoDbBean
public class PostImplementIncidentalActivityRow implements ProjectPlanFormRow {

    /** 排出活動 */
    @NotBlank
    @Size(max = 1000)
    @JsonProperty("emissionAct")
    private String emissionAct;

    /** 温室効果ガスの種類 */
    @NotBlank
    @Size(max = 20)
    @JsonProperty("greenhouseGasType")
    private String greenhouseGasType;

    /** 排出量(t-CO2/年) */
    @NotNull
    @JsonProperty("emissions")
    private BigDecimal emissions;

    /**
     * 影響度(％) — this activity's share of the 排出削減量(ER) from 10.1, as the
     * sheet's ※1 defines it: 「10.1で算定した排出削減量(ER)に対する比率(％)」.
     *
     * <p>Was free text up to 1000 characters, which is how a value above 100
     * got in. It is a number and is compared as one: 10.3(2) applies to every
     * activity 「影響度が１％以上」. Its own sibling 影響度合計値 is already a
     * {@code BigDecimal}, and free text cannot be totalled.
     *
     * <p><b>Percentage points, 0–100.</b> The spreadsheet cell holds the raw
     * ratio (the solar example stores 0.0407) and Excel renders it as 4.07%
     * because the cell is percent-formatted. The operator reads and types the
     * percentage, so that is what is stored — a factor of 100 apart from the
     * workbook's underlying value.
     */
    @NotNull
    @DecimalMin(value = "0", message = "影響度は0以上で入力してください。")
    @DecimalMax(value = "100", message = "影響度は100以下で入力してください。")
    @JsonProperty("impact")
    private BigDecimal impact;

    /**
     * モニタリング・算定方法 — a three-way choice, unlike 10.2（１）'s two:
     * 排出量の算定を行う。／ 同、ただし影響度により評価する。／ 算定を省略する。
     */
    @NotBlank
    @Size(max = 255)
    @JsonProperty("monitorCalcBeingDone")
    private String monitorCalcBeingDone;
}
