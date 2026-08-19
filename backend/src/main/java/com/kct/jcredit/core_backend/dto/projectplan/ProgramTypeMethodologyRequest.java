package com.kct.jcredit.core_backend.dto.projectplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.JapaneseText;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * プログラム型プロジェクトの運営・管理
 *
 * <p>Generated from db/projectplan.xlsx (table {@code program_type_methodology}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class ProgramTypeMethodologyRequest extends ProjectPlanFormBase {

    /** 運営・管理体制 */
    @NotBlank
    @Size(max = 3000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("opeManagStructure")
    private String opeManagStructure;

    /** 体制図 */
    @Size(max = 1000)
    // Holds the uploaded file's key, not prose — no character-set rule.
    @JsonProperty("structureDiagram")
    private String structureDiagram;

    /** 削減活動の実施者との合意の方法／内容 */
    @NotBlank
    @Size(max = 2000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("methodContentOfAggreement")
    private String methodContentOfAggreement;

    /** 日本国内で実施されること */
    @NotBlank
    @Size(max = 2000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("infoCollManagMethod")
    private String infoCollManagMethod;

    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("domesticImplementation")
    private String domesticImplementation;

    /** プロジェクト登録時に記入 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("registrationInput")
    private String registrationInput;

    /** ベースライン再設定による認証対象期間の延長時に記入 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("certExtensionInput")
    private String certExtensionInput;

    /** 入会申込日の2年前の日以降に実施されたものであること */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("mustImplAfterTwoYears")
    private String mustImplAfterTwoYears;

    /** 追加性を有すること */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("additionality")
    private String additionality;

    /** 本制度で承認された方法論に基づいて実施されること */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("implByApprovedMethod")
    private String implByApprovedMethod;

    /** 削減活動の二重登録がないこと / Ｊ－クレジット制度の他のプロジェクト */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("otherProjectJcredit")
    private String otherProjectJcredit;

    /** 削減活動の二重登録がないこと / 他の類似制度 */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("otherSimilarSystem")
    private String otherSimilarSystem;

    /** 方法論が共通であること */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("methodIsCommonInfo")
    private String methodIsCommonInfo;

    /** モニタリング項目が共通であること */
    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("moniItemIsCommonInfo")
    private String moniItemIsCommonInfo;
}
