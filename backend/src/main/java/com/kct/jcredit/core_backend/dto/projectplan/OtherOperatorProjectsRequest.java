package com.kct.jcredit.core_backend.dto.projectplan;

import com.kct.jcredit.core_backend.validation.ValidEmail;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormBase;
import com.kct.jcredit.core_backend.validation.CorpNumber;
import com.kct.jcredit.core_backend.validation.RequiredIf;
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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
// 有/無 rules from the official form: these are only required once the
// corresponding toggle is selected. See RequiredIf.
@RequiredIf(field = "emitterCode", when = "isDesignatedEmitter", is = true,
        message = "特定排出者コードは必須です。")
@RequiredIf(field = "energyActOpeNumber", when = "hasEnergyActPeriodReport", is = true,
        message = "省エネ法特定事業者番号又は特定連鎖化事業者番号は必須です。")
@RequiredIf(field = "energyManagFactoryNumber", when = "hasEnergyActPeriodReport", is = true,
        message = "エネルギー管理指定工場番号は必須です。")
public class OtherOperatorProjectsRequest extends ProjectPlanFormBase {

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

    // Required only when isDesignatedEmitter is true — see @RequiredIf above.
    @Size(min = 9, max = 9)
    @NumericOnly
    @JsonProperty("emitterCode")
    private String emitterCode;

    @NotNull
    @JsonProperty("hasEnergyActPeriodReport")
    private Boolean hasEnergyActPeriodReport;

    // Required only when hasEnergyActPeriodReport is true — see @RequiredIf above.
    @Size(max = 50)
    @HankakuWithSymbols(allowedSymbols = "-")
    @JsonProperty("energyActOpeNumber")
    private String energyActOpeNumber;

    // Required only when hasEnergyActPeriodReport is true — see @RequiredIf above.
    @Size(max = 50)
    @Hankaku
    @Pattern(regexp = "^[A-Za-z0-9]+$")
    @JsonProperty("energyManagFactoryNumber")
    private String energyManagFactoryNumber;
}
