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
 * 11.モニタリング計画 （２）係数（単位発熱量、排出係数、エネルギー消費効率、物性値等）.
 *
 * <p>Replaces three pages — monitoring-item-plan-coefficient,
 * monitoring-method-plan-coefficient and expected-project-plan-coefficient —
 * which split one table by column group and stored one
 * row each. That split was along the wrong axis: the form's unit is the row,
 * and three items with no row identifier cannot be joined back into one.
 *
 * <p>分類 on this table is <b>Ⅰ・Ⅱ・Ⅲ</b>, not the Ａ・Ｂ・Ｃ of
 * {@link MonitoringActivityPlanRequest} — the sheet carries a separate ※1 note
 * under each table saying so.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class MonitoringCoefficientPlanRequest extends ProjectPlanFormBase {

    /** モニタリング項目の表。1 行 = 1 モニタリング方法。 */
    @Valid
    @NotEmpty
    @JsonProperty("items")
    private List<MonitoringPlanRow> items;
}
