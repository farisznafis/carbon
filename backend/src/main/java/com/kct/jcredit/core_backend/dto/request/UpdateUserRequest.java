package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.validation.Katakana;
import com.kct.jcredit.core_backend.validation.PhoneNumber;
import com.kct.jcredit.core_backend.validation.ZenkakuWithSymbols;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateUserRequest {

    // サイトID 
    @Min(value = 1, message = "サイトIDは1以上の数値を指定してください。")
    private Integer UserId;  // Target user ID to update. Not to be changed.

    // 名前（名）

    @Size(max = 50, message = "名前（名）は50文字以内で入力してください。")
    @ZenkakuWithSymbols
    private String firstName;
    
   
    // 名前カナ（名）
   
    @Size(max = 50, message = "名前カナ（名）は50文字以内で入力してください。")
    @Katakana
    private String firstNameKana;
    
   
    // 名前（姓）
   
    @Size(max = 50, message = "名前（姓）は50文字以内で入力してください。")
    @ZenkakuWithSymbols
    private String lastName;
    
   
    // 名前カナ（姓）
   
    @Size(max = 50, message = "名前カナ（姓）は50文字以内で入力してください。")
    @Katakana
    private String lastNameKana;
    
   
    // メールアドレス
   
    @Email(message = "正しいメールアドレス形式で入力してください。")
    @Size(max = 254, message = "メールアドレスは254文字以内で入力してください。")
    private String email;
    
    // 電話番号
   
    @PhoneNumber
    @Size(max = 20, message = "電話番号は20文字以内で入力してください。")
    private String phoneNumber;

    @Size(max = 100, message = "役職は100文字以内で入力してください。")
    @ZenkakuWithSymbols
    private String userPosition;
   
    
    // サイトID 
    @Min(value = 1, message = "サイトIDは1以上の数値を指定してください。")
    private Integer siteId;  // Nullable - role="master" の場合は不要
    
}
