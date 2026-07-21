package com.kct.jcredit.core_backend.dto.request;

import lombok.Data;

@Data
public class AdminLoginRequest {
    private String identifier; // username or email
    private String password;
}