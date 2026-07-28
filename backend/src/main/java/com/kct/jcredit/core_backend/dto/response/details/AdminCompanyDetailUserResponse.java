package com.kct.jcredit.core_backend.dto.response.details;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminCompanyDetailUserResponse {
    private String username;
    private String fullName;
    private String email;
    private String role;
    private String siteName;
}
