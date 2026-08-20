package com.kct.jcredit.core_backend.dto.projectplan;

import com.kct.jcredit.core_backend.validation.ValidEmail;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormBase;
import com.kct.jcredit.core_backend.validation.CorpNumber;
import com.kct.jcredit.core_backend.validation.DateFormat;
import com.kct.jcredit.core_backend.validation.RequiredIf;
import com.kct.jcredit.core_backend.validation.Hankaku;
import com.kct.jcredit.core_backend.validation.HankakuWithSymbols;
import com.kct.jcredit.core_backend.validation.JapaneseText;
import com.kct.jcredit.core_backend.validation.Katakana;
import com.kct.jcredit.core_backend.validation.NumericOnly;
import com.kct.jcredit.core_backend.validation.PhoneNumber;
import com.kct.jcredit.core_backend.validation.Zenkaku;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
// 有/無 rules from the official 登録申請書 sheet: 特定排出者コード is filled only when
// 特定排出者である, and the 省エネ法 numbers only when 定期報告の対象である.
@RequiredIf(field = "emitterCode", when = "isDesignatedEmitter", is = true,
        message = "特定排出者コードは必須です。")
@RequiredIf(field = "energyActOperationNumber", when = "hasEnergyActPeriodReport", is = true,
        message = "省エネ法特定事業者番号又は特定連鎖化事業者番号は必須です。")
@RequiredIf(field = "energyManageFactoryNumber", when = "hasEnergyActPeriodReport", is = true,
        message = "エネルギー管理指定工場番号は必須です。")
public class ProjectApplicationRequest extends ProjectPlanFormBase {

    @NotBlank(message = "申請区分は必須です。")
    @Size(max = 20, message = "申請区分は20文字以内で入力してください。")
    @Pattern(
        regexp = "^(登録|計画変更|認証権限保有主体変更)$",
        message = "申請区分は「登録」「計画変更」「認証権限保有主体変更」のいずれかを入力してください。"
    )
    @JsonProperty("applicationCategory")
    private String applicationCategory;

    @NotNull(message = "審査費用支援の利用有無は必須です。")
    @JsonProperty("isUsingExamCostSupport")
    private Boolean isUsingExamCostSupport;

    @NotBlank
    @Size(max = 255)
    @Katakana
    @JsonProperty("projectOperatorKana")
    private String projectOperatorKana;

    @NotBlank
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("projectOperatorNameJa")
    private String projectOperatorNameJa;

    @NotBlank
    @Size(max = 255)
    @HankakuWithSymbols(allowedSymbols = "&.,' -")
    @JsonProperty("projectOperatorNameEn")
    private String projectOperatorNameEn;

    @NotBlank
    @CorpNumber
    @JsonProperty("corpNumber")
    private String corpNumber;

    @NotBlank
    @Size(max = 100)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("corpPosition")
    private String corpPosition;

    @NotBlank
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("corpName")
    private String corpName;

    @NotBlank
    @Size(max = 10)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("prefecture")
    private String prefecture;

    // 本社所在地 on the official 登録申請書 is three fields — 都道府県 / 市区町村 /
    // 市区町村以降の住所 — and that is what the frontend and the API contract use.
    // These previously mirrored operator_company's four columns
    // (municipality/blocknum/buildingName), which the frontend never sent, so
    // its 市区町村 and 住所 inputs were bound to fields the backend did not have.
    // Autofill folds blocknum + buildingName into address.
    @NotBlank
    @Size(max = 150)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("city")
    private String city;

    @NotBlank
    @Size(max = 500)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("address")
    private String address;

    @NotNull(message = "特定排出者の該当有無は必須です。")
    @JsonProperty("isDesignatedEmitter")
    private Boolean isDesignatedEmitter;

    // Required only when isDesignatedEmitter is true — see @RequiredIf above.
    @Size(min = 9, max = 9)
    @NumericOnly
    @JsonProperty("emitterCode")
    private String emitterCode;

    @NotNull
    @JsonProperty("hasEnergyActPeriodReport")
    private Boolean hasEnergyActPeriodReport;

    @Size(max = 20)
    @HankakuWithSymbols(allowedSymbols = "-")
    @JsonProperty("energyActOperationNumber")
    private String energyActOperationNumber;

    @Size(max = 20)
    @Hankaku
    @JsonProperty("energyManageFactoryNumber")
    private String energyManageFactoryNumber;

    @NotBlank
    @Size(max = 100)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("representativePosition")
    private String representativePosition;

    @JsonProperty("isChangeRepresentativePosition")
    private Boolean isChangeRepresentativePosition;

    @NotBlank
    @Size(max = 50)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("representativeName")
    private String representativeName;

    @JsonProperty("isChangeRepresentativeName")
    private Boolean isChangeRepresentativeName;

    @NotBlank
    @Size(max = 100)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("picPosition")
    private String picPosition;

    @JsonProperty("isChangePicPosition")
    private Boolean isChangePicPosition;

    @NotBlank
    @Size(max = 50)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("picName")
    private String picName;

    @JsonProperty("isChangePicName")
    private Boolean isChangePicName;

    @NotBlank(message = "担当者メールアドレスは必須です。")
    @Size(max = 254, message = "担当者メールアドレスは254文字以内で入力してください。")
    @ValidEmail(message = "担当者メールアドレスの形式が正しくありません。")
    @HankakuWithSymbols(allowedSymbols = "@._+-", message = "担当者メールアドレスは半角英数字と許可された記号で入力してください。")
    @JsonProperty("picEmail")
    private String picEmail;

    @JsonProperty("isChangePicEmail")
    private Boolean isChangePicEmail;

    @NotBlank(message = "担当者電話番号は必須です。")
    @Size(max = 20, message = "担当者電話番号は20文字以内で入力してください。")
    @PhoneNumber(message = "担当者電話番号の形式が正しくありません。（例: 0312345678）")
    @JsonProperty("picPhoneNumber")
    private String picPhoneNumber;

    @JsonProperty("isChangePicPhoneNumber")
    private Boolean isChangePicPhoneNumber;

    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("otherContactBusinessName")
    private String otherContactBusinessName;

    @JsonProperty("isChangeOtherContactBusinessName")
    private Boolean isChangeOtherContactBusinessName;

    @Size(max = 100)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("otherContactPicPosition")
    private String otherContactPicPosition;

    @JsonProperty("isChangeOtherContactPicPosition")
    private Boolean isChangeOtherContactPicPosition;

    @Size(max = 50)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("otherContactsPicName")
    private String otherContactsPicName;

    @JsonProperty("isChangeOtherContactsPicName")
    private Boolean isChangeOtherContactsPicName;

    @Size(max = 254)
    @ValidEmail
    @HankakuWithSymbols(allowedSymbols = "@._+-")
    @JsonProperty("otherContactsPicEmail")
    private String otherContactsPicEmail;

    @JsonProperty("isChangeOtherContactsPicEmail")
    private Boolean isChangeOtherContactsPicEmail;

    @Size(max = 20)
    @PhoneNumber
    @JsonProperty("otherContactsPicPhoneNumber")
    private String otherContactsPicPhoneNumber;

    @JsonProperty("isChangeOtherContactsPicPhoneNumber")
    private Boolean isChangeOtherContactsPicPhoneNumber;

    @NotBlank(message = "プロジェクト番号は必須です。")
    @Size(max = 10, message = "プロジェクト番号は10文字以内で入力してください。")
    @Pattern(regexp = "^P[0-9]+$", message = "プロジェクト番号はPで始まる数字で入力してください。")
    @JsonProperty("projectNumber")
    private String projectNumber;

    @NotBlank
    @Size(max = 100)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("projectName")
    private String projectName;

    @NotBlank(message = "方法論分類は必須です。")
    @Size(max = 10, message = "方法論分類は10文字以内で入力してください。")
    @Pattern(regexp = "^(EN-S|EN-R|IN|AG|WA)$", message = "方法論分類はEN-S、EN-R、IN、AG、WAのいずれかを入力してください。")
    @JsonProperty("methodologyCategory")
    private String methodologyCategory;

    @NotBlank(message = "方法論番号は必須です。")
    @Size(min = 3, max = 3, message = "方法論番号は3桁で入力してください。")
    @NumericOnly(message = "方法論番号は数字のみで入力してください。")
    @JsonProperty("methodologyNumber")
    private String methodologyNumber;

    @NotBlank
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("methodology")
    private String methodology;

    @NotBlank
    @Size(max = 50)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("location")
    private String location;

    @NotBlank(message = "所在地（英語）は必須です。")
    @Size(max = 50, message = "所在地（英語）は50文字以内で入力してください。")
    @HankakuWithSymbols(allowedSymbols = " -.,'&", message = "所在地（英語）は半角英数字と許可された記号で入力してください。")
    @JsonProperty("locationEn")
    private String locationEn;

    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("projectOutline")
    private String projectOutline;

    @NotBlank
    @Size(max = 10)
    // 地域区分 per the official 【削除不可】プルダウン用 sheet: 九州・沖縄 is a SINGLE
    // value, not two. The previous pattern split it and so rejected the value
    // autofilled from project.regional_category, making the page unsavable for
    // any project in that region.
    @Pattern(
        regexp = "^(北海道|東北|関東|中部|近畿|中国|四国|九州・沖縄)$",
        message = "地域区分の値が不正です。"
    )
    @JsonProperty("regionalCategory")
    private String regionalCategory;

    @NotBlank
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("groupName")
    private String groupName;

    @NotBlank
    @Size(max = 255)
    @Katakana
    @JsonProperty("groupNameFurigana")
    private String groupNameFurigana;

    @NotNull
    @JsonProperty("isParticipatingCnAct")
    private Boolean isParticipatingCnAct;

    @NotBlank
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("validationCheckReviewerName")
    private String validationCheckReviewerName;

    @DateFormat
    @JsonProperty("applicationDate")
    private String applicationDate;

    @DateFormat
    @JsonProperty("registrationDate")
    private String registrationDate;

    // 審査機関名 is picked from the official 審査機関 list, and 6 of those names
    // contain half-width Latin or ASCII spaces/parentheses ("SGS ジャパン株式会社
    // 認証サービス事業部", "株式会社 JACO CDM", "環境省(ダミーの審査機関)", …).
    // @Zenkaku rejects all ASCII, so it made half the dropdown unsavable.
    // Matches validationCheckReviewerName, which is fed by the same list.
    @NotBlank
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("reviewerName")
    private String reviewerName;

    @DateFormat
    @JsonProperty("applicationChangeDate")
    private String applicationChangeDate;
}
