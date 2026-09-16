package com.kct.jcredit.core_backend.dto.projectplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.DateFormat;
import com.kct.jcredit.core_backend.validation.Hankaku;
import com.kct.jcredit.core_backend.validation.JapaneseText;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * 【HP公開】プロジェクト計画書表紙
 *
 * <p>Generated from db/projectplan.xlsx (table {@code project_plan_cover}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class ProjectPlanCoverRequest extends ProjectPlanFormBase {

    /** 様式Ver */
    @Size(max = 20)
    @Hankaku
    @JsonProperty("version")
    private String version;

    /** プロジェクトの名称 */
    @Size(max = 100)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("projectName")
    private String projectName;

    /** プロジェクト番号 */
    @Size(max = 10)
    @Hankaku
    @JsonProperty("projectNumber")
    private String projectNumber;

    /** プログラム型運営・管理者名 */
    @Size(max = 50)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("projectOperatorName")
    private String projectOperatorName;

    /** 妥当性確認申請日（西暦年月日） */
    @DateFormat
    @JsonProperty("applicationDate")
    private String applicationDate;

    /** プロジェクト登録申請日（西暦年月日） */
    @DateFormat
    @JsonProperty("projectRegistrationDate")
    private String projectRegistrationDate;
}
