package com.kct.jcredit.core_backend.dto.projectplan;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * 11.モニタリング計画 （１）活動量（燃料消費量、生成熱量、生産量等）.
 *
 * <p>Replaces three pages — monitoring-item-plan, monitoring-method-plan and
 * expected-project-plan — which split one table by column group and stored one
 * row each. That split was along the wrong axis: the form's unit is the row,
 * and three items with no row identifier cannot be joined back into one.
 *
 * <p>分類 on this table is Ａ・Ｂ・Ｃ. The 係数 table uses Ⅰ・Ⅱ・Ⅲ; see
 * {@link MonitoringCoefficientPlanRequest}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class MonitoringActivityPlanRequest extends ProjectPlanFormBase {

    /** モニタリング項目の表。1 行 = 1 モニタリング方法。 */
    @Valid
    @NotEmpty
    @JsonProperty("items")
    private List<MonitoringPlanRow> items;
}
