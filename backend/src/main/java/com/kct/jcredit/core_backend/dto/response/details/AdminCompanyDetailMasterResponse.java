package com.kct.jcredit.core_backend.dto.response.details;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminCompanyDetailMasterResponse {
    private String username;
    private String fullName;
    private String fullNameKana;
    private String firstName;
    private String lastName;
    private String firstNameKana;
    private String lastNameKana;
    private String phoneNumber;
    private String email;
    private String userPosition;
    private String role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
