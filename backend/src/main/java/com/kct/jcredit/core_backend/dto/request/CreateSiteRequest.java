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
    @Size(max = 10, message = "都道府県名は100文字以内で入力してください。")
    private String prefecture;

    @NotNull(message = "市区町村を選択してください。")
    @Zenkaku
    @Size(max = 150, message = "市区町村名は150文字以内で入力してください。")
    private String municipality;

    @NotNull(message = "番地を入力してください。")
    @JapaneseText(allowAlphanumeric = true)
    @Size(max = 100, message = "番地は100文字以内で入力してください。")
    private String blocknum;

    @NotBlank(message = "建物名を入力してください。")
    @Size(max = 255, message = "建物名は255文字以内で入力してください。")
    private String buildingName;

    @NotNull(message = "ユーザIDを入力してください。")
    private Integer userId;
}
