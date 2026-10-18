package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.validation.ValidEmail;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.CorpNumber;
import com.kct.jcredit.core_backend.validation.DateFormat;
import com.kct.jcredit.core_backend.validation.HankakuWithSymbols;
import com.kct.jcredit.core_backend.validation.JapaneseText;
import com.kct.jcredit.core_backend.validation.Katakana;
import com.kct.jcredit.core_backend.validation.NumericOnly;
import com.kct.jcredit.core_backend.validation.PhoneNumber;
import com.kct.jcredit.core_backend.validation.Zenkaku;
import com.kct.jcredit.core_backend.validation.ZenkakuWithSymbols;

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
public class MonitoringReportRequest {



    @JsonIgnore
    private String pk;

    @JsonIgnore
    private String sk;

    // ===== Application Info =====

    @NotBlank
    @Pattern(regexp = "^モニタリング報告書$", message = "申請区分はモニタリング報告書のみです。")
    @JsonProperty("application_category")
    private String applicationCategory;

    @NotNull
    @JsonProperty("is_using_exam_cost_support")
    private Boolean isUsingExamCostSupport;

    // ===== Project Info =====

    @NotBlank
    @Size(max = 10)
    @Pattern(regexp = "^P[0-9]+$", message = "プロジェクト番号はP+数字の形式で入力してください。")
    @JsonProperty("project_number")
    private String projectNumber;

    @NotBlank
    @Size(max = 100)
    @Zenkaku
    @JsonProperty("project_name")
    private String projectName;

    // ===== Operator Info =====

    @NotBlank
    @Size(max = 255)
    @Zenkaku
    @JsonProperty("project_operator_name_ja")
    private String projectOperatorNameJa;

    @NotBlank
    @Size(max = 255)
    @Katakana
    @JsonProperty("project_operator_kana")
    private String projectOperatorKana;

    // ===== Corporation Info =====

    @NotBlank
    @CorpNumber
    @JsonProperty("corp_number")
    private String corpNumber;

    @NotBlank
    @Size(max = 100)
    @Zenkaku
    @JsonProperty("corp_position")
    private String corpPosition;

    @NotBlank
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("corp_name")
    private String corpName;

    // ===== Address =====

    @NotBlank
    @Size(max = 50)
    @Zenkaku
    @JsonProperty("prefecture")
    private String prefecture;

    @NotBlank
    @Size(max = 100)
    @Zenkaku
    @JsonProperty("city")
    private String city;

    @NotBlank
    @Size(max = 500)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("address")
    private String address;

    // ===== Representative =====

    @NotBlank
    @Size(max = 100)
    @Zenkaku
    @JsonProperty("representative_position")
    private String representativePosition;

    @NotBlank
    @Size(max = 50)
    @ZenkakuWithSymbols
    @JsonProperty("representative_name")
    private String representativeName;

    // ===== PIC =====

    @NotBlank
    @Size(max = 100)
    @ZenkakuWithSymbols
    @JsonProperty("pic_position")
    private String picPosition;

    @NotBlank
    @Size(max = 50)
    @ZenkakuWithSymbols
    @JsonProperty("pic_name")
    private String picName;

    @NotBlank
    @Size(max = 254)
    @ValidEmail
    @HankakuWithSymbols(allowedSymbols = "@._+-")
    @JsonProperty("pic_email")
    private String picEmail;

    @NotBlank
    @Size(max = 20)
    @PhoneNumber
    @JsonProperty("pic_phone_number")
    private String picPhoneNumber;

    // ===== Other Contacts (Optional) =====

    @Size(max = 255)
    @Zenkaku
    @JsonProperty("other_contact_buisness_name")
    private String otherContactBuisnessName;

    @Size(max = 100)
    @Zenkaku
    @JsonProperty("other_contact_pic_position")
    private String otherContactPicPosition;

    @Size(max = 50)
    @ZenkakuWithSymbols
    @JsonProperty("other_contacts_pic_name")
    private String otherContactsPicName;

    @Size(max = 254)
    @ValidEmail
    @HankakuWithSymbols(allowedSymbols = "@._+-")
    @JsonProperty("other_contacts_pic_email")
    private String otherContactsPicEmail;

    @Size(max = 20)
    @PhoneNumber
    @JsonProperty("other_contacts_pic_phone_number")
    private String otherContactsPicPhoneNumber;

    // ===== Account & Review =====

    @NotBlank
    @NumericOnly
    @JsonProperty("account_no")
    private String accountNo;

    @NotBlank
    @Size(max = 255)
    @Zenkaku
    @JsonProperty("account_name")
    private String accountName;

    @NotBlank
    @Size(max = 255)
    @Zenkaku
    @JsonProperty("reviewer_name")
    private String reviewerName;

    @DateFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("verification_date")
    private String verificationDate;

    @DateFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("certification_date")
    private String certificationDate;

    // ===== DynamoDB Keys =====

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
