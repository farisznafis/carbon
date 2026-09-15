package com.kct.jcredit.core_backend.dto.projectplan;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.JapaneseText;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * ベースラインの付随的な排出活動
 *
 * <p>10.2（１）. A 考え方 paragraph, a table of 排出活動 rows, and the
 * 排出量合計値 under it.
 *
 * <p>Generated from db/projectplan.xlsx (table
 * {@code baseline_incidental_emission_activity}), which had no way to express
 * a repeating table — so the page held one 排出活動 where the sheet's own ※1
 * says 「行を追加して記入した場合には、合計の参照範囲を確認すること」.
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

    /** 排出活動の表。1 行 = 1 排出活動。 */
    @Valid
    @NotEmpty
    @JsonProperty("activities")
    private List<BaselineIncidentalActivityRow> activities;

    /** 排出量合計値. Derived — the sum of the rows' 排出量. */
    @JsonProperty("emissionsTotalValue")
    private BigDecimal emissionsTotalValue;
}
