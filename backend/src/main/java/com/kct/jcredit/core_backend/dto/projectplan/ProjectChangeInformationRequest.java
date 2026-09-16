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
 * プロジェクト計画変更情報
 *
 * <p>Generated from db/projectplan.xlsx (table {@code project_change_information}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class ProjectChangeInformationRequest extends ProjectPlanFormBase {

    /** 変更回数 */
    @NotNull
    @JsonProperty("numberOfChanges")
    private Integer numberOfChanges;

    /** 変更申請日（西暦年月日） */
    @DateFormat
    @JsonProperty("applicationChangeDate")
    private String applicationChangeDate;

    /** 変更内容の適用開始日（西暦年月日） */
    @DateFormat
    @JsonProperty("effectiveDate")
    private String effectiveDate;

    /** 変更種別：プログラム型運営・管理者の社名の変更 */
    @NotNull
    @JsonProperty("changeTypeOperatorName")
    private Boolean changeTypeOperatorName;

    /** 変更種別：クレジット取得予定者の変更 */
    @NotNull
    @JsonProperty("changeTypeCreditHolder")
    private Boolean changeTypeCreditHolder;

    /** 変更種別：プログラム型プロジェクトにおける認証対象期間の延長 */
    @NotNull
    @JsonProperty("changeTypeExtension")
    private Boolean changeTypeExtension;

    /** 変更種別：形式的な変更以外 */
    @NotNull
    @JsonProperty("changeTypeOther")
    private Boolean changeTypeOther;

    /** 変更概要 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("overview")
    private String overview;

    /** 再妥当性確認要否 */
    @NotNull
    @JsonProperty("isNeedRevalidation")
    private Boolean isNeedRevalidation;

    /** 審査機関名 */
    @Size(max = 50)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("reviewerName")
    private String reviewerName;

    /** [変更申請履歴]変更回数 */
    @NotNull
    @JsonProperty("historyNumberOfChanges")
    private Integer historyNumberOfChanges;

    /** [変更申請履歴]変更申請日（西暦年月日） */
    @NotBlank
    @DateFormat
    @JsonProperty("historyApplicationChangeDate")
    private String historyApplicationChangeDate;

    /** [変更申請履歴]変更種別：プログラム型運営・管理者の社名の変更 */
    @NotNull
    @JsonProperty("historyChangeTypeOperatorName")
    private Boolean historyChangeTypeOperatorName;

    /** [変更申請履歴]変更種別：クレジット取得予定者の変更 */
    @NotNull
    @JsonProperty("historyChangeTypeCreditHolder")
    private Boolean historyChangeTypeCreditHolder;

    /** [変更申請履歴]変更種別：プログラム型プロジェクトにおける認証対象期間の延長 */
    @NotNull
    @JsonProperty("historyChangeTypeExtension")
    private Boolean historyChangeTypeExtension;

    /** [変更申請履歴]変更種別：形式的な変更以外 */
    @NotNull
    @JsonProperty("historyChangeTypeOther")
    private Boolean historyChangeTypeOther;

    /** [変更申請履歴]変更概要 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("historyOverview")
    private String historyOverview;
}
