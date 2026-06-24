package com.kct.jcredit.core_backend.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.CorpNumber;
import com.kct.jcredit.core_backend.validation.DateFormat;
import com.kct.jcredit.core_backend.validation.Hankaku;
import com.kct.jcredit.core_backend.validation.HankakuWithSymbols;
import com.kct.jcredit.core_backend.validation.JapaneseText;
import com.kct.jcredit.core_backend.validation.Katakana;
import com.kct.jcredit.core_backend.validation.NumericOnly;
import com.kct.jcredit.core_backend.validation.PhoneNumber;
import com.kct.jcredit.core_backend.validation.Zenkaku;
import com.kct.jcredit.core_backend.validation.ZenkakuWithSymbols;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class ProjectApplicationRequest {



    @JsonIgnore
    private String pk;

    @JsonIgnore
    private String sk;

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
    @Zenkaku
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
    @Zenkaku
    @JsonProperty("corpPosition")
    private String corpPosition;

    @NotBlank
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("corpName")
    private String corpName;

    @NotBlank
    @Size(max = 10)
    @Zenkaku
    @JsonProperty("prefecture")
    private String prefecture;

    @NotBlank
    @Size(max = 150)
    @Zenkaku
    @JsonProperty("municipality")
    private String municipality;

    @NotBlank
    @Size(max = 100)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("blocknum")
    private String blocknum;

    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("buildingName")
    private String buildingName;

    @NotNull(message = "特定排出者の該当有無は必須です。")
    @JsonProperty("isDesignatedEmitter")
    private Boolean isDesignatedEmitter;

    @NotBlank
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
    @Zenkaku
    @JsonProperty("representativePosition")
    private String representativePosition;

    @JsonProperty("isChangeRepresentativePosition")
    private Boolean isChangeRepresentativePosition;

    @NotBlank
    @Size(max = 50)
    @ZenkakuWithSymbols
    @JsonProperty("representativeName")
    private String representativeName;

    @JsonProperty("isChangeRepresentativeName")
    private Boolean isChangeRepresentativeName;

    @NotBlank
    @Size(max = 100)
    @ZenkakuWithSymbols
    @JsonProperty("picPosition")
    private String picPosition;

    @JsonProperty("isChangePicPosition")
    private Boolean isChangePicPosition;

    @NotBlank
    @Size(max = 50)
    @ZenkakuWithSymbols
    @JsonProperty("picName")
    private String picName;

    @JsonProperty("isChangePicName")
    private Boolean isChangePicName;

    @NotBlank(message = "担当者メールアドレスは必須です。")
    @Size(max = 254, message = "担当者メールアドレスは254文字以内で入力してください。")
    @Email(message = "担当者メールアドレスの形式が正しくありません。")
    @HankakuWithSymbols(allowedSymbols = "@._+-", message = "担当者メールアドレスは半角英数字と許可された記号で入力してください。")
    @JsonProperty("picEmail")
    private String picEmail;

    @JsonProperty("isChangePicEmail")
    private Boolean isChangePicEmail;

    @NotBlank(message = "担当者電話番号は必須です。")
    @Size(max = 20, message = "担当者電話番号は20文字以内で入力してください。")
    @PhoneNumber(message = "担当者電話番号の形式が正しくありません。（例: 03-1234-5678）")
    @JsonProperty("picPhoneNumber")
    private String picPhoneNumber;

    @JsonProperty("isChangePicPhoneNumber")
    private Boolean isChangePicPhoneNumber;

    @Size(max = 255)
    @Zenkaku
    @JsonProperty("otherContactBusinessName")
    private String otherContactBusinessName;

    @JsonProperty("isChangeOtherContactBusinessName")
    private Boolean isChangeOtherContactBusinessName;

    @Size(max = 100)
    @Zenkaku
    @JsonProperty("otherContactPicPosition")
    private String otherContactPicPosition;

    @JsonProperty("isChangeOtherContactPicPosition")
    private Boolean isChangeOtherContactPicPosition;

    @Size(max = 50)
    @ZenkakuWithSymbols
    @JsonProperty("otherContactsPicName")
    private String otherContactsPicName;

    @JsonProperty("isChangeOtherContactsPicName")
    private Boolean isChangeOtherContactsPicName;

    @Size(max = 254)
    @Email
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
    @Zenkaku
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
    @Zenkaku
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
    @Pattern(regexp = "^(北海道|東北|関東|中部|近畿|中国|四国|九州|沖縄)$")
    @JsonProperty("regionalCategory")
    private String regionalCategory;

    @NotBlank
    @Size(max = 255)
    @Zenkaku
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

    @NotBlank
    @Size(max = 255)
    @Zenkaku
    @JsonProperty("reviewerName")
    private String reviewerName;

    @DateFormat
    @JsonProperty("applicationChangeDate")
    private String applicationChangeDate;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() {
        return pk;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() {
        return sk;
    }
}
