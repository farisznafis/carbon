package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.validation.CorpNumber;
import com.kct.jcredit.core_backend.validation.JapaneseText;
import com.kct.jcredit.core_backend.validation.Katakana;
import com.kct.jcredit.core_backend.validation.PhoneNumber;
import com.kct.jcredit.core_backend.validation.PostalCode;
import com.kct.jcredit.core_backend.validation.Zenkaku;
import com.kct.jcredit.core_backend.validation.ZenkakuWithSymbols;
import com.kct.jcredit.core_backend.validation.Hankaku;
import com.kct.jcredit.core_backend.validation.HankakuWithSymbols;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartiCompanyRegisterRequest {

    @NotBlank(message = "法人番号は13桁の数字で入力してください。")
    @CorpNumber
    private String corporateNumber;

    @NotBlank(message = "法人名を入力してください。")
    private String corporateName;

    @NotBlank(message = "法人名カナは全角カタカナで入力してください。")
    @Katakana
    private String corporateNameKana;

    @NotBlank(message = "会社名（英語）は英数字で入力してください。")
    @Hankaku
    private String companyNameEnglish;

    @NotBlank(message = "メールアドレスの形式が正しくありません。")
    @Email(message = "メールアドレスの形式が正しくありません。")
    private String corporateEmail;

    @NotBlank(message = "電話番号の形式が正しくありません。")
    @PhoneNumber
    private String corporatePhoneNumber;

    @NotBlank(message = "郵便番号は7桁の数字で入力してください。（例: 1234567）")
    @PostalCode
    private String postalCode;

    @NotBlank(message = "都道府県を選択してください。")
    @Zenkaku
    private String prefecture;

    @NotBlank(message = "市区町村を入力してください。")
    @Zenkaku
    private String city;

    @NotBlank(message = "住所を入力してください。")
    @JapaneseText(allowAlphanumeric = true)
    private String address;

    @NotBlank(message = "市区町村を入力してください")
    @JapaneseText(allowAlphanumeric = true)
    private String town;

    @NotBlank(message = "番地を入力してください")
    @JapaneseText(allowAlphanumeric = true)
    private String blocknum;

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

    @NotBlank(message = "電話番号の形式が正しくありません。（例: 03-1234-5678）")
    @PhoneNumber
    private String phoneNumber;

    @NotBlank(message = "FAX番号の形式が正しくありません。（例: 03-1234-5679）")
    @PhoneNumber
    private String faxNumber;

    @NotBlank(message = "メールアドレスの形式が正しくありません。")
    @Email(message = "メールアドレスの形式が正しくありません。")
    private String email;

    private List<String> methodology;

    private String activityInformation;
}
