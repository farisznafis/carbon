package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.validation.PhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePartiCompanyRequest {

    private String CompanyId;

    @PhoneNumber
    @Size(max = 20, message = "電話番号は20文字以内で入力してください。")
    private String phoneNumber;   

    @PhoneNumber
    @Size(max = 20, message = "FAX番号は20文字以内で入力してください。")
    private String fax;          

    @Email(message = "正しいメールアドレス形式で入力してください。")
    @Size(max = 254, message = "メールアドレスは254文字以内で入力してください。")
    private String email;       
}
