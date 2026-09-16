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
 * プロジェクト実施後の付随的な排出活動
 *
 * <p>10.2（２）. Like 10.2（１）but with a 影響度(％) column, and a 合計 row
 * that totals both 排出量 and 影響度.
 *
 * <p>影響度 is what 10.3（２）keys on: 「10.2(2)において、影響度が１％以上で
 * あった付随的な排出活動に全てについて記載する」.
 *
 * <p>Generated from db/projectplan.xlsx (table
 * {@code post_impliment_incedental_activity}).
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

    /** 排出活動の表。1 行 = 1 排出活動。 */
    @Valid
    @NotEmpty
    @JsonProperty("activities")
    private List<PostImplementIncidentalActivityRow> activities;

    /** 排出量合計値. Derived — the sum of the rows' 排出量. */
    @JsonProperty("emissionsTotalValue")
    private BigDecimal emissionsTotalValue;

    /** 影響度合計値. Derived — the sum of the rows' 影響度. */
    @JsonProperty("impactTotalValue")
    private BigDecimal impactTotalValue;
}
