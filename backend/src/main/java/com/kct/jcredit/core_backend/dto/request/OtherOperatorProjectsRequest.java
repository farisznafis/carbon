package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.validation.ValidEmail;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.CorpNumber;
import com.kct.jcredit.core_backend.validation.Hankaku;
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
public class OtherOperatorProjectsRequest {

    @JsonIgnore
    private String pk;

    @JsonIgnore
    private String sk;

    @NotBlank
    @Size(max = 50)
    @Katakana
    @JsonProperty("projectOperatorKana")
    private String projectOperatorKana;

    @NotBlank
    @Size(max = 255)
    @Zenkaku
    @JsonProperty("projectOperatorNameJa")
    private String projectOperatorNameJa;

    @NotBlank
    @CorpNumber
    @JsonProperty("otherCorpNumber")
    private String otherCorpNumber;

    @NotBlank
    @Size(max = 50)
    @Zenkaku
    @JsonProperty("prefecture")
    private String prefecture;

    @NotBlank
    @Size(max = 50)
    @Zenkaku
    @JsonProperty("city")
    private String city;

    @NotBlank
    @Size(max = 500)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("address")
    private String address;

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
    @Zenkaku
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
    @ValidEmail
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
    @Size(max = 50)
    @HankakuWithSymbols(allowedSymbols = "-")
    @JsonProperty("energyActOpeNumber")
    private String energyActOpeNumber;

    @NotBlank
    @Size(max = 50)
    @Hankaku
    @Pattern(regexp = "^[A-Za-z0-9]+$")
    @JsonProperty("energyManagFactoryNumber")
    private String energyManagFactoryNumber;

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
