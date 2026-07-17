package com.kct.jcredit.core_backend.dto.request;

import com.kct.jcredit.core_backend.validation.ValidSecurityPassword;
import com.kct.jcredit.core_backend.validation.ValidUsername;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MasterAccountSetupRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    @ValidUsername
    private String username;

    @NotBlank
    @ValidSecurityPassword
    private String password;
}
