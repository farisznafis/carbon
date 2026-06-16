package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PasswordChangeRequest {
    private Integer userId;  // null = 自分のパスワード変更
    
    @NotBlank(message = "現在のパスワードは必須です")
    private String currentPassword;
    
    @NotBlank(message = "新しいパスワードは必須です")
    @Size(min = 8, max = 255, message = "パスワードは8文字以上255文字以内で入力してください。")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$",
             message = "パスワードは大文字、小文字、数字をそれぞれ1文字以上含む必要があります。")
    private String newPassword;
}

