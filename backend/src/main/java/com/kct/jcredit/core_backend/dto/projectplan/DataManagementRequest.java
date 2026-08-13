package com.kct.jcredit.core_backend.dto.projectplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.DateFormat;
import com.kct.jcredit.core_backend.validation.JapaneseText;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * データ管理・特記事項
 *
 * <p>Generated from db/projectplan.xlsx (table {@code data_management}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class DataManagementRequest extends ProjectPlanFormBase {

    /** データ管理責任者 */
    @NotBlank
    @Size(max = 100)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("dataManagementPic")
    private String dataManagementPic;

    /** モニタリング担当者 */
    @NotBlank
    @Size(max = 100)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("monitoringPic")
    private String monitoringPic;

    /** 各削減活動におけるモニタリングデータの取得方法 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("moniDataAcquiringMethod")
    private String moniDataAcquiringMethod;

    /** 各削減活動のモニタリングデータの収集方法 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("moniDataCollectionMethod")
    private String moniDataCollectionMethod;

    /** モニタリングデータの記録・保管方法 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("moniDataRecStoreMethod")
    private String moniDataRecStoreMethod;

    /** データ保存期間（認証対象期間終了後何年間） */
    @NotNull
    @JsonProperty("dataStoringPeriod")
    private Integer dataStoringPeriod;

    /** 排出量の削減に影響を与える可能性のあるリスク有無 */
    @NotNull
    @JsonProperty("isRisksForEmissionReduction")
    private Boolean isRisksForEmissionReduction;

    /** （リスク有の場合）リスク要因 */
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("riskFactors")
    private String riskFactors;

    /** 類似制度へプロジェクトを登録有無 */
    @NotNull
    @JsonProperty("isSimilarProjectRegistered")
    private Boolean isSimilarProjectRegistered;

    /** （登録有の場合）類似制度名 */
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("similarSystemName")
    private String similarSystemName;

    /** （登録有の場合）類似制度での認証予定期間（開始）西暦年月日 */
    @DateFormat
    @JsonProperty("similarSystemPeriodStart")
    private String similarSystemPeriodStart;

    /** （登録有の場合）類似制度での認証予定期間（終了）西暦年月日 */
    @DateFormat
    @JsonProperty("similarSystemPeriodEnd")
    private String similarSystemPeriodEnd;

    /** プロジェクトの実施は、法令等の義務履行有無 */
    @NotNull
    @JsonProperty("isIndependentOfLegalCompli")
    private Boolean isIndependentOfLegalCompli;
}
