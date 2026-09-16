package com.kct.jcredit.core_backend.dto.projectplan;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * One 排出活動 of 10.2（１）ベースラインの付随的な排出活動.
 *
 * <p>Columns: 排出活動 / 温室効果ガスの種類 / 排出量 (t-CO2/年) /
 * モニタリング・算定方法. The sheet expects rows to be added — its ※1 reads
 * 「行を追加して記入した場合には、合計の参照範囲を確認すること」 — and the
 * 排出量合計値 beneath the table is the sum of this column.
 *
 * <p>モニタリング・算定方法 here is a two-way choice, 「排出量の算定を行う。」or
 * 「排出量の算定を行わない。」, which is why it is a {@code Boolean}. The
 * equivalent column on 10.2（２）offers three, so that one is a string — see
 * {@link PostImplementIncidentalActivityRow}.
 */
@Data
@NoArgsConstructor
@DynamoDbBean
public class BaselineIncidentalActivityRow implements ProjectPlanFormRow {

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

    /** モニタリング・算定方法 — 排出量の算定を行う／行わない. */
    @NotNull
    @JsonProperty("monitorCalcBeingDone")
    private Boolean monitorCalcBeingDone;
}
