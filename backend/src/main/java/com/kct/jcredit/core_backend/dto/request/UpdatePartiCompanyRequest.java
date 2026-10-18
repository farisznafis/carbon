package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.validation.CorpNumber;
import com.kct.jcredit.core_backend.validation.Hankaku;
import com.kct.jcredit.core_backend.validation.JapaneseText;
import com.kct.jcredit.core_backend.validation.Katakana;
import com.kct.jcredit.core_backend.validation.PhoneNumber;
import com.kct.jcredit.core_backend.validation.PostalCode;
import com.kct.jcredit.core_backend.validation.Zenkaku;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePartiCompanyRequest {

    @Size(max = 255, message = "法人名は255文字以内で入力してください。")
    private String nameJa;

    @Katakana
    @Size(max = 255, message = "法人名カナは255文字以内で入力してください。")
    private String nameFurigana;

    @Hankaku
    @Size(max = 255, message = "英語法人名は255文字以内で入力してください。")
    private String nameEn;

    @PhoneNumber
    @Size(max = 20, message = "電話番号は20文字以内で入力してください。")
    private String phoneNumber;

    @CorpNumber
    @Size(max = 13, message = "法人番号は13文字以内で入力してください。")
    private String corpNumber;

    @PostalCode
    @Size(max = 8, message = "郵便番号は8文字以内で入力してください。")
    private String postalCode;

    @Zenkaku
    @Size(max = 10, message = "都道府県名は10文字以内で入力してください。")
    private String prefecture;

    @Zenkaku
    @Size(max = 150, message = "市区町村名は150文字以内で入力してください。")
    private String municipality;

    @JapaneseText(allowAlphanumeric = true)
    @Size(max = 100, message = "番地は100文字以内で入力してください。")
    private String blocknum;

    @Size(max = 255, message = "建物名は255文字以内で入力してください。")
    private String buildingName;

    @PhoneNumber
    @Size(max = 20, message = "FAX番号は20文字以内で入力してください。")
    private String fax;
}
