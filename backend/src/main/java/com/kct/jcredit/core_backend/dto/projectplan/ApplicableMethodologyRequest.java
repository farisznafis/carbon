package com.kct.jcredit.core_backend.dto.projectplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.Hankaku;
import com.kct.jcredit.core_backend.validation.JapaneseText;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * 適用する方法論
 *
 * <p>Generated from db/projectplan.xlsx (table {@code applicable_methodology}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class ApplicableMethodologyRequest extends ProjectPlanFormBase {

    /** 適用方法論／区分 */
    @Size(max = 10)
    @Hankaku
    @JsonProperty("methodologyCategory")
    private String methodologyCategory;

    /** 適用方法論／番号 */
    @NotBlank
    @Size(max = 3)
    @Hankaku
    @JsonProperty("methodologyNumber")
    private String methodologyNumber;

    /** バージョン */
    @NotBlank
    @Size(max = 10)
    @Hankaku
    @JsonProperty("methodologyVersion")
    private String methodologyVersion;

    /** 更新プロジェクト/新設プロジェクト */
    @NotBlank
    @Size(max = 50)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("projectType")
    private String projectType;

    /**
     * 各削減活動への更新／新設プロジェクトの適用の考え方.
     *
     * <p>Does not apply when プロジェクトタイプ is 更新プロジェクトのみを対象とする,
     * where the wizard hides it — an unconditional @NotBlank then demanded a
     * field the operator could not see. The condition keys on a string, which
     * @RequiredIf cannot express yet, so it is enforced in the frontend schema
     * for now.
     */
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("approachDetails")
    private String approachDetails;

    /** 概要（削減方法） */
    @NotBlank
    @Size(max = 2000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("reductionMethodSummary")
    private String reductionMethodSummary;

    /** プロジェクト実施前の燃料種 */
    @NotBlank
    @Size(max = 100)
    @JsonProperty("fuelTypeBeforeImpl")
    private String fuelTypeBeforeImpl;

    /** プロジェクト実施前の対象設備 */
    @NotBlank
    @Size(max = 100)
    @JsonProperty("targetEquipmentBeforeImpl")
    private String targetEquipmentBeforeImpl;

    /** 削減活動実施前の概要図 */
    @NotBlank
    @Size(max = 1000)
    // Holds the uploaded file's key, not prose — no character-set rule.
    @JsonProperty("overviewDiagramBeforeAct")
    private String overviewDiagramBeforeAct;

    /** プロジェクト実施後の燃料種 */
    @NotBlank
    @Size(max = 100)
    @JsonProperty("fuelTypeAfterImpl")
    private String fuelTypeAfterImpl;

    /** プロジェクト実施後の対象設備 */
    @NotBlank
    @Size(max = 100)
    @JsonProperty("targetEquipmentAfterImpl")
    private String targetEquipmentAfterImpl;

    /** 削減活動実施後の概要図 */
    @NotBlank
    @Size(max = 1000)
    // Holds the uploaded file's key, not prose — no character-set rule.
    @JsonProperty("overviewDiagramAfterAct")
    private String overviewDiagramAfterAct;

    /** 各削減活動の方法論の適用条件 */
    @NotBlank
    @Size(max = 2000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("reductionActivityMethodCondition")
    private String reductionActivityMethodCondition;
}
