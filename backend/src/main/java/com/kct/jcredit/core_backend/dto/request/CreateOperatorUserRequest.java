package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.validation.ValidEmail;
import com.kct.jcredit.core_backend.validation.ValidUsername;
import com.kct.jcredit.core_backend.validation.Katakana;
import com.kct.jcredit.core_backend.validation.PhoneNumber;
import com.kct.jcredit.core_backend.validation.Zenkaku;
import com.kct.jcredit.core_backend.validation.ZenkakuWithSymbols;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreateOperatorUserRequest {
    
   
    // ユーザー名
   
    @NotBlank(message = "ユーザー名を入力してください。")
    @Size(min = 3, max = 50, message = "ユーザー名は3文字以上50文字以内で入力してください。")
    @ValidUsername
    private String username;
    
   
    // 名前（名）
   
    @NotBlank(message = "名前（名）を入力してください。")
    @Size(max = 50, message = "名前（名）は50文字以内で入力してください。")
    @ZenkakuWithSymbols
    private String firstName;
    
   
    // 名前カナ（名）
   
    @NotBlank(message = "名前カナ（名）を入力してください。")
    @Size(max = 50, message = "名前カナ（名）は50文字以内で入力してください。")
    @Katakana
    private String firstNameKana;
    
   
    // 名前（姓）
   
    @NotBlank(message = "名前（姓）を入力してください。")
    @Size(max = 50, message = "名前（姓）は50文字以内で入力してください。")
    @ZenkakuWithSymbols
    private String lastName;
    
   
    // 名前カナ（姓）
   
    @NotBlank(message = "名前カナ（姓）を入力してください。")
    @Size(max = 50, message = "名前カナ（姓）は50文字以内で入力してください。")
    @Katakana
    private String lastNameKana;
    
   
    // メールアドレス
   
    @NotBlank(message = "メールアドレスを入力してください。")
    @ValidEmail(message = "正しいメールアドレス形式で入力してください。")
    @Size(max = 254, message = "メールアドレスは254文字以内で入力してください。")
    private String email;
    
   
    // 電話番号
   
    @NotBlank(message = "電話番号を入力してください。")
    @PhoneNumber
    @Size(min = 7, max = 16, message = "電話番号は7文字以上16文字以内で入力してください。")
    private String phoneNumber;
    

   
    // パスワード
   
    @NotBlank(message = "パスワードを入力してください。")
    @Size(min = 8, max = 255, message = "パスワードは8文字以上255文字以内で入力してください。")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$",
             message = "パスワードは大文字、小文字、数字をそれぞれ1文字以上含む必要があります。")
    private String password;
    

    private List<String> permissionCodes;

}
