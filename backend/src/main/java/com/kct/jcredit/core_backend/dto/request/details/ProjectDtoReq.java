package com.kct.jcredit.core_backend.dto.request.details;

import com.kct.jcredit.core_backend.validation.Katakana;
import com.kct.jcredit.core_backend.validation.PhoneNumber;
import com.kct.jcredit.core_backend.validation.Zenkaku;
import com.kct.jcredit.core_backend.validation.ZenkakuWithSymbols;
import com.kct.jcredit.core_backend.validation.AnyCharacter;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDtoReq {
    
    @NotBlank
    @AnyCharacter
    private String projectName;

    @NotBlank
    private String projectNumber;

    @NotBlank
    @Zenkaku
    private String projectStatus;

    @NotNull
    @DecimalMin("0")
    private Double baselineEmissions;

    @NotNull
    @DecimalMin("0")
    private Double postProjectEmission;

    // rep
    @NotBlank
    @ZenkakuWithSymbols
    private String repFirstName;
    @NotBlank
    @Katakana
    private String repFirstNameKana;
    @NotBlank
    @ZenkakuWithSymbols
    private String repLastName;
    @NotBlank
    @Katakana
    private String repLastNameKana;
    @Email
    @NotBlank
    private String repEmail;
    @NotBlank
    @PhoneNumber
    private String repPhoneNumber;
    @NotBlank
    @Zenkaku
    private String repPosition;

    // pic
    @NotBlank
    @ZenkakuWithSymbols
    private String picFirstName;
    @NotBlank
    @Katakana
    private String picFirstNameKana;
    @NotBlank
    @ZenkakuWithSymbols
    private String picLastName;
    @NotBlank
    @Katakana
    private String picLastNameKana;
    @Email
    @NotBlank
    private String picEmail;
    @NotBlank
    @PhoneNumber
    private String picPhoneNumber;
    @NotBlank
    @Zenkaku
    private String picPosition;
    @NotBlank
    @ZenkakuWithSymbols
    private String dataManagementPic;

    @NotBlank
    @Zenkaku
    private String regionalCategory;

    @NotNull private Integer newMembersTotal;
    @NotNull private Integer withdrawnMembers;
    @NotNull private Integer cumulativeMembers;
    private Integer membersTotal;

    @NotBlank
    @Pattern(regexp = "^Ver\\.[0-9]+(\\.[0-9]+)?$", message = "^Ver.[0-9]+(.[0-9]+)?$ の形式で入力してください。")
    @Size(max = 10)
    private String methodologyVersion;

    @NotBlank
    @Pattern(regexp = "^(EN-S|EN-R|IN|AG|WA)$", message = "EN-S, EN-R, IN, AG, WA のいずれかで入力してください。")
    @Size(max = 5)
    private String methodCategory;

    @NotBlank
    private String methodNumber;

    @NotBlank
    @ZenkakuWithSymbols
    private String methodName;
    
    @NotBlank
    @Size(max = 255)
    private String methodMainEmission;

    @NotNull
    private Boolean methodCombiFeasibility;

    @NotBlank
    @Size(max = 255)
    private String methodDetails;

    private String operatorCompanyId;
}
