package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.validation.CorpNumber;
import com.kct.jcredit.core_backend.validation.Hankaku;
import com.kct.jcredit.core_backend.validation.JapaneseText;
import com.kct.jcredit.core_backend.validation.Katakana;
import com.kct.jcredit.core_backend.validation.PhoneNumber;
import com.kct.jcredit.core_backend.validation.PostalCode;
import com.kct.jcredit.core_backend.validation.Zenkaku;
import com.kct.jcredit.core_backend.validation.ZenkakuWithSymbols;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartiCompanyInfoValidateRequest {

    @NotBlank(message = "法人番号は13桁の数字で入力してください。")
    @CorpNumber
    private String corporateNumber;

    @NotBlank(message = "法人名を入力してください。")
    private String corporateName;

    @NotBlank(message = "法人名カナは全角カタカナで入力してください。")
    @Katakana
    private String corporateNameKana;

    @Hankaku
    private String companyNameEnglish;

    @NotBlank(message = "電話番号の形式が正しくありません。（例: 0312345678）")
    @PhoneNumber
    private String corporatePhoneNumber;

    @NotBlank(message = "郵便番号を入力してください。")
    @PostalCode
    private String postalCode;

    @NotBlank(message = "都道府県を選択してください。")
    @Zenkaku
    @Size(max = 10, message = "都道府県名は100文字以内で入力してください。")
    private String prefecture;

    @NotBlank(message = "市区町村を選択してください。")
    @Zenkaku
    @Size(max = 150, message = "市区町村名は150文字以内で入力してください。")
    private String municipality;

    @NotBlank(message = "番地を入力してください。")
    @JapaneseText(allowAlphanumeric = true)
    @Size(max = 100, message = "番地は100文字以内で入力してください。")
    private String blocknum;

    @NotBlank(message = "建物名を入力してください。")
    @Size(max = 255, message = "建物名は255文字以内で入力してください。")
    private String buildingName;

    @NotBlank(message = "姓を入力してください。")
    @ZenkakuWithSymbols
    private String lastName;

    @NotBlank(message = "名を入力してください。")
    @ZenkakuWithSymbols
    private String firstName;

    @NotBlank(message = "姓（カナ）は全角カタカナで入力してください。")
    @Katakana
    private String lastNameKana;

    @NotBlank(message = "名（カナ）は全角カタカナで入力してください。")
    @Katakana
    private String firstNameKana;

    @NotBlank(message = "役職を入力してください。")
    @Zenkaku
    private String position;

    @NotBlank(message = "電話番号の形式が正しくありません。（例: 08023456789）")
    @PhoneNumber
    private String phoneNumber;

    @PhoneNumber
    private String faxNumber;

    @NotBlank(message = "メールアドレスの形式が正しくありません。")
    @Email(message = "メールアドレスの形式が正しくありません。")
    private String email;
}
