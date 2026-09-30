package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.validation.ValidSecurityPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordChangeRequest {
    private Object userId; // null or company ID value = self password change

    private String currentPassword;

    @NotBlank(message = "新しいパスワードは必須です")
    @ValidSecurityPassword
    private String newPassword;
}
