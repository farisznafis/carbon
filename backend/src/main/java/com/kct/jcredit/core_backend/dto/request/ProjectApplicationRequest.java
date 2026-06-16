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

    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = "^(登録|計画変更|認証権限保有主体変更)$")
    @JsonProperty("application_category")
    private String applicationCategory;

    @NotNull
    @JsonProperty("is_using_exam_cost_support")
    private Boolean isUsingExamCostSupport;

    @NotBlank
    @Size(max = 255)
    @Katakana
    @JsonProperty("project_operator_kana")
    private String projectOperatorKana;

    @NotBlank
    @Size(max = 255)
    @Zenkaku
    @JsonProperty("project_operator_name_ja")
    private String projectOperatorNameJa;

    @NotBlank
    @Size(max = 255)
    @HankakuWithSymbols(allowedSymbols = "&.,' -")
    @JsonProperty("project_operator_name_en")
    private String projectOperatorNameEn;

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

    @NotNull
    @JsonProperty("is_designated_emitter")
    private Boolean isDesignatedEmitter;

    @NotBlank
    @Size(min = 9, max = 9)
    @NumericOnly
    @JsonProperty("emitter_code")
    private String emitterCode;

    @NotNull
    @JsonProperty("has_energy_act_period_report")
    private Boolean hasEnergyActPeriodReport;

    @NotBlank
    @Size(max = 20)
    @HankakuWithSymbols(allowedSymbols = "-")
    @JsonProperty("energy_act_operation_number")
    private String energyActOperationNumber;

    @NotBlank
    @Size(max = 20)
    @Hankaku
    @JsonProperty("energy_manage_factory_number")
    private String energyManageFactoryNumber;

    @NotBlank
    @Size(max = 100)
    @Zenkaku
    @JsonProperty("representative_position")
    private String representativePosition;

    @JsonProperty("is_change_representative_position")
    private Boolean isChangeRepresentativePosition;

    @NotBlank
    @Size(max = 50)
    @ZenkakuWithSymbols
    @JsonProperty("representative_name")
    private String representativeName;

    @JsonProperty("is_change_representative_name")
    private Boolean isChangeRepresentativeName;

    @NotBlank
    @Size(max = 100)
    @ZenkakuWithSymbols
    @JsonProperty("pic_position")
    private String picPosition;

    @JsonProperty("is_change_pic_position")
    private Boolean isChangePicPosition;

    @NotBlank
    @Size(max = 50)
    @ZenkakuWithSymbols
    @JsonProperty("pic_name")
    private String picName;

    @JsonProperty("is_change_pic_name")
    private Boolean isChangePicName;

    @NotBlank
    @Size(max = 254)
    @Email
    @HankakuWithSymbols(allowedSymbols = "@._+-")
    @JsonProperty("pic_email")
    private String picEmail;

    @JsonProperty("is_change_pic_email")
    private Boolean isChangePicEmail;

    @NotBlank
    @Size(max = 20)
    @PhoneNumber
    @JsonProperty("pic_phone_number")
    private String picPhoneNumber;

    @JsonProperty("is_change_pic_phone_number")
    private Boolean isChangePicPhoneNumber;

    @Size(max = 255)
    @Zenkaku
    @JsonProperty("other_contact_business_name")
    private String otherContactBusinessName;

    @JsonProperty("is_change_other_contact_business_name")
    private Boolean isChangeOtherContactBusinessName;

    @Size(max = 100)
    @Zenkaku
    @JsonProperty("other_contact_pic_position")
    private String otherContactPicPosition;

    @JsonProperty("is_change_other_contact_pic_position")
    private Boolean isChangeOtherContactPicPosition;

    @Size(max = 50)
    @ZenkakuWithSymbols
    @JsonProperty("other_contacts_pic_name")
    private String otherContactsPicName;

    @JsonProperty("is_change_other_contacts_pic_name")
    private Boolean isChangeOtherContactsPicName;

    @Size(max = 254)
    @Email
    @HankakuWithSymbols(allowedSymbols = "@._+-")
    @JsonProperty("other_contacts_pic_email")
    private String otherContactsPicEmail;

    @JsonProperty("is_change_other_contacts_pic_email")
    private Boolean isChangeOtherContactsPicEmail;

    @Size(max = 20)
    @PhoneNumber
    @JsonProperty("other_contacts_pic_phone_number")
    private String otherContactsPicPhoneNumber;

    @JsonProperty("is_change_other_contacts_pic_phone_number")
    private Boolean isChangeOtherContactsPicPhoneNumber;

    @NotBlank
    @Size(max = 10)
    @Pattern(regexp = "^P[0-9]+$")
    @JsonProperty("project_number")
    private String projectNumber;

    @NotBlank
    @Size(max = 100)
    @Zenkaku
    @JsonProperty("project_name")
    private String projectName;

    @NotBlank
    @Size(max = 10)
    @Pattern(regexp = "^(EN-S|EN-R|IN|AG|WA)$")
    @JsonProperty("methodology_category")
    private String methodologyCategory;

    @NotBlank
    @Size(min = 3, max = 3)
    @NumericOnly
    @JsonProperty("methodology_number")
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

    @NotBlank
    @Size(max = 50)
    @HankakuWithSymbols(allowedSymbols = " -.,'&")
    @JsonProperty("location_en")
    private String locationEn;

    @NotBlank
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("project_outline")
    private String projectOutline;

    @NotBlank
    @Size(max = 10)
    @Pattern(regexp = "^(北海道|東北|関東|中部|近畿|中国|四国|九州|沖縄)$")
    @JsonProperty("regional_category")
    private String regionalCategory;

    @NotBlank
    @Size(max = 255)
    @Zenkaku
    @JsonProperty("group_name")
    private String groupName;

    @NotBlank
    @Size(max = 255)
    @Katakana
    @JsonProperty("group_name_furigana")
    private String groupNameFurigana;

    @NotNull
    @JsonProperty("is_participating_cn_act")
    private Boolean isParticipatingCnAct;

    @NotBlank
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("validation_check_reviewer_name")
    private String validationCheckReviewerName;

    @DateFormat
    @JsonProperty("application_date")
    private String applicationDate;

    @DateFormat
    @JsonProperty("registration_date")
    private String registrationDate;

    @NotBlank
    @Size(max = 255)
    @Zenkaku
    @JsonProperty("reviewer_name")
    private String reviewerName;

    @DateFormat
    @JsonProperty("application_change_date")
    private String applicationChangeDate;

    @Size(max = 20)
    @PhoneNumber
    @JsonProperty("operator_phone_number")
    private String operatorPhoneNumber;

    @Size(max = 255)
    @Zenkaku
    @JsonProperty("implimentation_area")
    private String implimentationArea;

    @DateFormat
    @JsonProperty("cert_target_period_start")
    private String certTargetPeriodStart;

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
