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
    @JsonProperty("applicationCategory")
    private String applicationCategory;

    @NotNull
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

    @NotBlank
    @Size(max = 20)
    @HankakuWithSymbols(allowedSymbols = "-")
    @JsonProperty("energyActOperationNumber")
    private String energyActOperationNumber;

    @NotBlank
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

    @NotBlank
    @Size(max = 254)
    @Email
    @HankakuWithSymbols(allowedSymbols = "@._+-")
    @JsonProperty("picEmail")
    private String picEmail;

    @JsonProperty("isChangePicEmail")
    private Boolean isChangePicEmail;

    @NotBlank
    @Size(max = 20)
    @PhoneNumber
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

    @NotBlank
    @Size(max = 10)
    @Pattern(regexp = "^P[0-9]+$")
    @JsonProperty("projectNumber")
    private String projectNumber;

    @NotBlank
    @Size(max = 100)
    @Zenkaku
    @JsonProperty("projectName")
    private String projectName;

    @NotBlank
    @Size(max = 10)
    @Pattern(regexp = "^(EN-S|EN-R|IN|AG|WA)$")
    @JsonProperty("methodologyCategory")
    private String methodologyCategory;

    @NotBlank
    @Size(min = 3, max = 3)
    @NumericOnly
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

    @NotBlank
    @Size(max = 50)
    @HankakuWithSymbols(allowedSymbols = " -.,'&")
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
