package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.validation.JapaneseText;
import com.kct.jcredit.core_backend.validation.PostalCode;
import com.kct.jcredit.core_backend.validation.Zenkaku;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSiteRequest {
    
    @NotBlank(message = "サイト名を入力してください。")
    @Size(max = 255, message = "サイト名は255文字以内で入力してください。")
    @Pattern(regexp = ".*\\S.*", message = "サイト名を入力してください。")
    private String siteName;

    @NotBlank(message = "郵便番号を入力してください。")
    @PostalCode
    @Pattern(regexp = ".*\\S.*", message = "郵便番号を入力してください。")
    private String postalCode;

    @Zenkaku
    @NotBlank(message = "都道府県を入力してください。")
    @Size(max = 10, message = "都道府県名は100文字以内で入力してください。")
    @Pattern(regexp = ".*\\S.*", message = "都道府県を選択してください。")
    private String prefecture;

    @Zenkaku
    @NotBlank(message = "市区町村を入力してください。")
    @Size(max = 150, message = "市区町村名は150文字以内で入力してください。")
    @Pattern(regexp = ".*\\S.*", message = "市区町村を選択してください。")
    private String municipality;

    @JapaneseText(allowAlphanumeric = true)
    @NotBlank(message = "番地を入力してください。")
    @Size(max = 100, message = "番地は100文字以内で入力してください。")
    @Pattern(regexp = ".*\\S.*", message = "番地を入力してください。")
    private String blocknum;

    @NotBlank(message = "建物名を入力してください。")
    @Size(max = 255, message = "建物名は255文字以内で入力してください。")
    @Pattern(regexp = ".*\\S.*", message = "建物名を入力してください。")
    private String buildingName;

    @Min(value = 1, message = "ユーザIDは1以上の数値を指定してください。")
    private Integer PICUserId;  

    // サイトID 
    @Min(value = 1, message = "サイトIDは1以上の数値を指定してください。")
    private Integer siteId;  // Nullable - role="master" の場合は不要

    
    
}
