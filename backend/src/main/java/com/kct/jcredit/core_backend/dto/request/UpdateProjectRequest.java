package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.validation.ValidEmail;
import com.kct.jcredit.core_backend.dto.request.base.BaseRequest;
import com.kct.jcredit.core_backend.validation.Hankaku;
import com.kct.jcredit.core_backend.validation.Katakana;
import com.kct.jcredit.core_backend.validation.PhoneNumber;
import com.kct.jcredit.core_backend.validation.Zenkaku;
import com.kct.jcredit.core_backend.validation.ZenkakuWithSymbols;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectRequest extends BaseRequest{
    @ZenkakuWithSymbols
    private String projectName;

    @DecimalMin("0")
    private Double baselineEmissions;

    @DecimalMin("0")
    private Double postProjectEmission;

    // rep
    @ZenkakuWithSymbols
    private String repFirstName;
    @Katakana
    private String repFirstNameKana;
    @ZenkakuWithSymbols
    private String repLastName;
    @Katakana
    private String repLastNameKana;
    @ValidEmail
    private String repEmail;
    @PhoneNumber
    private String repPhoneNumber;
    @Zenkaku
    private String repPosition;

    // pic
    @ZenkakuWithSymbols
    private String picFirstName;
    @Katakana
    private String picFirstNameKana;
    @ZenkakuWithSymbols
    private String picLastName;
    @Katakana
    private String picLastNameKana;
    @ValidEmail
    private String picEmail;
    @PhoneNumber
    private String picPhoneNumber;
    @Zenkaku
    private String picPosition;
    @ZenkakuWithSymbols
    private String dataManagementPic;

    @Zenkaku
    private String regionalCategory;

    //method related
    @Hankaku
    @Pattern(regexp = "^Ver\\.[0-9]+(\\.[0-9]+)?$", message = "^Ver.[0-9]+(.[0-9]+)?$ の形式で入力してください。")
    @Size(max = 10)
    private String methodologyVersion;
    @Pattern(regexp = "^(EN-S|EN-R|IN|AG|WA)$", message = "EN-S, EN-R, IN, AG, WA のいずれかで入力してください。")
    @Size(max = 5)
    private String methodCategory;
    @Hankaku
    @Pattern(regexp = "^[0-9]{3}$", message = "方法論番号は3桁の数字で入力してください。")
    private String methodNumber;
    @ZenkakuWithSymbols
    private String methodName;
    @Size(max = 255)
    private String methodMainEmission;

    private Boolean methodCombiFeasibility;

    @Size(max = 1000, message = "方法論詳細は1000文字以内で入力してください。")
    private String methodDetails;
}
