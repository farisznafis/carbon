package com.kct.jcredit.core_backend.dto.projectplan;

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
 * モニタリング・算定方法
 *
 * <p>Generated from db/projectplan.xlsx (table {@code monitoring_calculation}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class MonitoringCalculationRequest extends ProjectPlanFormBase {

    /** 全削減活動においてモニタリング・算定を実施する排出活動を統一可否 */
    @NotNull
    @JsonProperty("isStandardizeEmissionAct")
    private Boolean isStandardizeEmissionAct;

    /** 活動量のモニタリング項目 */
    @NotBlank
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("actMonitoringItems")
    private String actMonitoringItems;

    /** 全削減活動においてモニタリング・算定を実施する排出活動の確認 */
    @NotNull
    @JsonProperty("isStandardMonitorCalcEmissionAct")
    private Boolean isStandardMonitorCalcEmissionAct;

    /** その他（モニタリング・算定を実施する排出活動の確認方法） */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("otherMethod")
    private String otherMethod;

    /** サンプリングの利用有無 */
    @NotNull
    @JsonProperty("isUsingSampling")
    private Boolean isUsingSampling;

    /** 適用するサンプル抽出法 */
    @Size(max = 500)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("samplingMethod")
    private String samplingMethod;

    /** サンプリングの目的 */
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("samplingPurpose")
    private String samplingPurpose;

    /** 収集データ及び実測対象 */
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("collectDataAndMeasurementTarget")
    private String collectDataAndMeasurementTarget;

    /** 母集団の構成・リスト及びその特性 */
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("populationCompositionCharacter")
    private String populationCompositionCharacter;

    /** 適用したサンプル抽出法と当該抽出法の利用が妥当であることの合理的な説明 */
    @Size(max = 2000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("explOfRationality")
    private String explOfRationality;

    /** サンプル数（評価式を含む） */
    @Size(max = 2000)
    @JsonProperty("sampleSizeAndFormula")
    private String sampleSizeAndFormula;

    /** サンプル数（画像データ） */
    @Size(max = 1000)
    @JsonProperty("sampleSizeAndFormulaImg")
    private String sampleSizeAndFormulaImg;

    /** データ収集・管理・分析方法及び非標本誤差への対応方法 */
    @Size(max = 2000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("dataAnalysisNonsamplErrHandling")
    private String dataAnalysisNonsamplErrHandling;

    /** 実施方法（スケジュールと対応者等） */
    @Size(max = 2000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("implMethodScheduleAssignee")
    private String implMethodScheduleAssignee;

    /** 実施方法（画像データ） */
    @Size(max = 1000)
    @JsonProperty("implMethodScheduleAssigneeImg")
    private String implMethodScheduleAssigneeImg;
}
