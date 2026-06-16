package com.kct.jcredit.core_backend.dto.response.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminResponse {
    private String id;
    private String username;
    private String name;
    private String role;
    private String email;
    private String firstName;
    private String firstNameKana;
    private String lastName;
    private String lastNameKana;
    private String phoneNumber;
}