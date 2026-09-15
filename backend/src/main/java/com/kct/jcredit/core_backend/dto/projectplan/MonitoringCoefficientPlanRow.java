package com.kct.jcredit.core_backend.dto.projectplan;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * One row of 11.モニタリング計画 （２）係数.
 *
 * <p>Everything but 分類 comes from {@link MonitoringPlanRow}. This table's
 * 分類 is <b>Ⅰ・Ⅱ・Ⅲ</b>; the other table's is different, and a value from
 * the wrong list is refused rather than stored.
 *
 * <p>Sheet ※1: 「モニタリング・算定規程に沿って、分類Ⅰ・Ⅱ・Ⅲのいずれかの方法を選択すること。分類Ⅰ（実測）を用いる場合には、12.3において実測方法の説明を行うこと。分類Ⅱ（第三者提供値）を用いる場合には、提供事業者名を概要欄に記載すること。」
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class MonitoringCoefficientPlanRow extends MonitoringPlanRow {

    /** 分類 — Ⅰ・Ⅱ・Ⅲ. */
    @NotBlank
    @Size(max = 200)
    @Pattern(regexp = "Ⅰ|Ⅱ|Ⅲ", message = "分類は Ⅰ・Ⅱ・Ⅲ から選択してください。")
    @JsonProperty("classification")
    private String classification;
}
