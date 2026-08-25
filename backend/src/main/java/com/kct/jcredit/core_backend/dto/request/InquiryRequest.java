package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.validation.ValidEmail;
import com.kct.jcredit.core_backend.validation.ZenkakuWithSymbols;
import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class InquiryRequest {

    // 会社名
    @NotBlank(message = "会社名を入力してください。")
    @Size(max = 255, message = "会社名は255文字以内で入力してください。")
    @ZenkakuWithSymbols
    private String companyName;

    // 氏名
    @NotBlank(message = "氏名を入力してください。")
    @Size(max = 100, message = "氏名は100文字以内で入力してください。")
    @ZenkakuWithSymbols
    private String name;

    // メールアドレス
    @NotBlank(message = "メールアドレスを入力してください。")
    @ValidEmail(message = "正しいメールアドレス形式で入力してください。")
    @Size(max = 254, message = "メールアドレスは254文字以内で入力してください。")
    private String email;


    // お問い合わせ内容
    @NotBlank(message = "お問い合わせ内容を入力してください。")
    @Size(max = 1000, message = "お問い合わせ内容は1000文字以内で入力してください。")
    private String content;
}