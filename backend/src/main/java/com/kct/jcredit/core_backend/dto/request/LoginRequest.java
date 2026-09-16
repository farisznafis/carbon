package com.kct.jcredit.core_backend.dto.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String companyId;
    private String identifier; // username or email
    private String password;
}