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
 * One row of 11.モニタリング計画 （１）活動量.
 *
 * <p>Everything but 分類 comes from {@link MonitoringPlanRow}. This table's
 * 分類 is <b>Ａ・Ｂ・Ｃ</b>; the other table's is different, and a value from
 * the wrong list is refused rather than stored.
 *
 * <p>Sheet ※1: 「モニタリング・算定規程に沿って、分類Ａ・Ｂ・Ｃのいずれかの方法を選択すること。分類Ｂ（計量器）を用いる場合には、12.1において計量器やモニタリングポイントの説明を行うこと。分類Ｃ（概算等）を用いる場合には、12.2において概算・推定方法の詳細について説明すること。」
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class MonitoringActivityPlanRow extends MonitoringPlanRow {

    /** 分類 — Ａ・Ｂ・Ｃ. */
    @NotBlank
    @Size(max = 200)
    @Pattern(regexp = "A|B|C", message = "分類は Ａ・Ｂ・Ｃ から選択してください。")
    @JsonProperty("classification")
    private String classification;
}
