package com.kct.jcredit.core_backend.dto.projectplan;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.DateFormat;
import com.kct.jcredit.core_backend.validation.Hankaku;
import com.kct.jcredit.core_backend.validation.JapaneseText;
import com.kct.jcredit.core_backend.validation.Katakana;
import com.kct.jcredit.core_backend.validation.NumericOnly;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * 【HP公開】1.プロジェクト概要
 *
 * <p>Generated from db/projectplan.xlsx (table {@code project_overview}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class ProjectOverviewRequest extends ProjectPlanFormBase {

    /** プログラム型運営・管理者名（フリガナ) */
    @Size(max = 255)
    @Katakana
    @JsonProperty("projectOperatorKana")
    private String projectOperatorKana;

    /** プログラム型運営・管理者名 */
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("projectOperatorName")
    private String projectOperatorName;

    /** 法人番号 */
    @NotBlank
    @Size(max = 13)
    @NumericOnly
    @JsonProperty("corpNumber")
    private String corpNumber;

    /** 本社所在地／都道府県 */
    @Size(max = 50)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("prefecture")
    private String prefecture;

    /** 本社所在地／市区町村 */
    @Size(max = 50)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("city")
    private String city;

    /** 本社所在地／市区町村以外の住所 */
    @Size(max = 500)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("address")
    private String address;

    /** 事業者概要 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("overview")
    private String overview;

    /** 実施者名（本社所在地） */
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("companyName")
    private String companyName;

    /** 団体、組織、活動等の名称（フリガナ） */
    @Size(max = 255)
    @Katakana
    @JsonProperty("reductionActivityNameKana")
    private String reductionActivityNameKana;

    /** 団体、組織、活動等の名称 */
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("reductionActivityName")
    private String reductionActivityName;

    /** 会員のカーボンニュートラル参加の有無実施者削減活動有無 */
    @JsonProperty("reductionActivityIncluded")
    private Boolean reductionActivityIncluded;

    /** カーボンニュートラル行動計画への参加状況の確認方法 */
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("cnActParticipationDetails")
    private String cnActParticipationDetails;

    /** Ｊ－クレジット保有者 口座法人名（フリガナ） */
    @NotBlank
    @Size(max = 255)
    @Katakana
    @JsonProperty("holderAccountNameKana")
    private String holderAccountNameKana;

    /** Ｊ－クレジット保有者 口座法人名 */
    @NotBlank
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("holderAccountName")
    private String holderAccountName;

    /** Ｊ－クレジット保有者 法人番号 */
    @Size(max = 13)
    @NumericOnly
    @JsonProperty("holderCorpNumber")
    private String holderCorpNumber;

    /** Ｊ－クレジット保有者 住所／都道府県 */
    @NotBlank
    @Size(max = 50)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("holderPrefecture")
    private String holderPrefecture;

    /** Ｊ－クレジット保有者 住所／市区町村 */
    @NotBlank
    @Size(max = 50)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("holderCity")
    private String holderCity;

    /** Ｊ－クレジット保有者 住所／市区町村以外の住所 */
    @NotBlank
    @Size(max = 500)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("holderAddress")
    private String holderAddress;

    /** Ｊ－クレジット保有予定者決定有無 */
    @NotNull
    @JsonProperty("isHolderUndecided")
    private Boolean isHolderUndecided;

    /** プロジェクト実施場所の分類 */
    @Size(max = 10)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("location")
    private String location;

    /** プロジェクト実施場所の分類（英語表記） */
    @Size(max = 20)
    @Hankaku
    @JsonProperty("projectLocationClassificatonEn")
    private String projectLocationClassificatonEn;

    /** 地域区分 */
    @Size(max = 50)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("regionalCategory")
    private String regionalCategory;

    /** 対象とする地域 */
    @NotBlank
    @Size(max = 100)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("targetArea")
    private String targetArea;

    /** クレジット収益の配分／活用計画 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("creditUtilizationPlan")
    private String creditUtilizationPlan;

    /** プロジェクト全体の概念図 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("projectConceptDiagram")
    private String projectConceptDiagram;

    /** 認証対象期間（開始）(西暦年月日) */
    @NotBlank
    @DateFormat
    @JsonProperty("certTargetPeriodStart")
    private String certTargetPeriodStart;

    /** 認証対象期間（終了）(西暦年月日) */
    @NotBlank
    @DateFormat
    @JsonProperty("certTargetPeriodEnd")
    private String certTargetPeriodEnd;

    /** 認証対象期間／何年間（0.0年） */
    @JsonProperty("certTargetPeriodYears")
    private BigDecimal certTargetPeriodYears;

    /** 認証対象期間が適用されない方法論を用いたプロジェクト有無 */
    @NotNull
    @JsonProperty("isMethodologyValid")
    private Boolean isMethodologyValid;
}
