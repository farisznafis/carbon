package com.kct.jcredit.core_backend.dto.projectplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormBase;
import com.kct.jcredit.core_backend.validation.DateFormat;
import com.kct.jcredit.core_backend.validation.HankakuWithSymbols;
import com.kct.jcredit.core_backend.validation.JapaneseText;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class ApplicableConditionsRequest extends ProjectPlanFormBase {

    @Size(max = 15)
    @HankakuWithSymbols(allowedSymbols = "-.")
    @JsonProperty("methodologyCode")
    private String methodologyCode;

    @NotBlank
    @Size(max = 20)
    @HankakuWithSymbols(allowedSymbols = "-.")
    @JsonProperty("methodologyVersion")
    private String methodologyVersion;

    @NotBlank
    @Size(max = 100)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("esgPledgeCase")
    private String esgPledgeCase;

    @NotBlank
    @DateFormat
    @JsonProperty("esgPledgeCaseDate")
    private String esgPledgeCaseDate;

    @NotBlank
    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("businessName")
    private String businessName;

    @NotBlank
    @Size(max = 100)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("representativePosition")
    private String representativePosition;

    @Size(max = 50)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("representativeName")
    private String representativeName;

    @Size(max = 255)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("regulation")
    private String regulation;
}
