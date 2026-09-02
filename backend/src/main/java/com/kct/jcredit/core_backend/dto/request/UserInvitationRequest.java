package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.validation.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserInvitationRequest {

    // 招待するメールアドレス
    @NotBlank(message = "メールアドレスを入力してください。")
    @ValidEmail(message = "正しいメールアドレス形式で入力してください。")
    @Size(max = 254, message = "メールアドレスは254文字以内で入力してください。")
    private String email;

    // サイトID（任意）
    private Integer siteId;
}
