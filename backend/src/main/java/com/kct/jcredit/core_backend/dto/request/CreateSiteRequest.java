package com.kct.jcredit.core_backend.dto.request;
import com.kct.jcredit.core_backend.validation.JapaneseText;
import com.kct.jcredit.core_backend.validation.Katakana;
import com.kct.jcredit.core_backend.validation.PhoneNumber;
import com.kct.jcredit.core_backend.validation.PostalCode;
import com.kct.jcredit.core_backend.validation.Zenkaku;
import com.kct.jcredit.core_backend.validation.ZenkakuWithSymbols;
import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class CreateSiteRequest {

    @NotBlank(message = "サイト名を入力してください。")
    @Size(max = 255, message = "サイト名は255文字以内で入力してください。")
    private String siteName;

    @Size(max = 255, message = "サイト名カナは255文字以内で入力してください。")
    private String siteNameKana; // optional

    @NotBlank(message = "郵便番号を入力してください。")
    @PostalCode
    private String postalCode;

    @NotNull(message = "都道府県を選択してください。")
    @Zenkaku
    @Size(max = 100, message = "都道府県名は100文字以内で入力してください。")
    private String prefName;

    @NotNull(message = "市区町村を選択してください。")
    @Zenkaku
    @Size(max = 100, message = "市区町村名は100文字以内で入力してください。")
    private String cityName;

    @NotNull(message = "町名を入力してください。")
    @Zenkaku
    @Size(max = 100, message = "町名は100文字以内で入力してください。")
    private String townName;

    @NotNull(message = "番地を入力してください。")
    @JapaneseText(allowAlphanumeric = true)
    @Size(max = 100, message = "番地は100文字以内で入力してください。")
    private String blockName;

    @NotBlank(message = "住所を入力してください。")
    @Size(max = 500, message = "住所は500文字以内で入力してください。")
    private String address;

    @NotBlank(message = "担当者名（名）を入力してください。")
    @Size(max = 50, message = "担当者名（名）は50文字以内で入力してください。")
    @ZenkakuWithSymbols
    private String picFirstName;

    @NotBlank(message = "担当者名カナ（名）を入力してください。")
    @Size(max = 50, message = "担当者名カナ（名）は50文字以内で入力してください。")
    @Katakana
    private String picFirstNameKana;

    @NotBlank(message = "担当者名（姓）を入力してください。")
    @Size(max = 50, message = "担当者名（姓）は50文字以内で入力してください。")
    @ZenkakuWithSymbols
    private String picLastName;

    @NotBlank(message = "担当者名カナ（姓）を入力してください。")
    @Size(max = 50, message = "担当者名カナ（姓）は50文字以内で入力してください。")
    @Katakana
    private String picLastNameKana;

    @NotBlank(message = "担当者のメールアドレスを入力してください。")
    @Email(message = "正しいメールアドレス形式で入力してください。")
    @Size(max = 254, message = "メールアドレスは254文字以内で入力してください。")
    private String picEmail;

    @NotBlank(message = "担当者の電話番号を入力してください。")
    @PhoneNumber
    @Size(max = 20, message = "電話番号は20文字以内で入力してください。")
    private String picPhoneNumber;

    @NotBlank(message = "担当者の役職を入力してください。")
    @Size(max = 100, message = "役職は100文字以内で入力してください。")
    @Zenkaku
    private String picPosition;
}
