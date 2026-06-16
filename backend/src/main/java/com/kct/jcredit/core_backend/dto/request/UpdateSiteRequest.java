package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.validation.JapaneseText;
import com.kct.jcredit.core_backend.validation.PostalCode;
import com.kct.jcredit.core_backend.validation.Zenkaku;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSiteRequest {
    
    @PostalCode
    private String postalCode;

    @Zenkaku
    @Size(max = 100, message = "都道府県名は100文字以内で入力してください。")
    private String prefName;

    @Zenkaku
    @Size(max = 100, message = "市区町村名は100文字以内で入力してください。")
    private String cityName;

    @Zenkaku
    @Size(max = 100, message = "町名は100文字以内で入力してください。")
    private String townName;

    @JapaneseText(allowAlphanumeric = true)
    @Size(max = 100, message = "番地は100文字以内で入力してください。")
    private String blockName;

    @Size(max = 500, message = "住所は500文字以内で入力してください。")
    private String address;

    
}
