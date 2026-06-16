package com.kct.jcredit.core_backend.dto.request;

import lombok.Data;

@Data
public class CreateDummyUserRequest {
    private String username;
    private String firstName;
    private String firstNameKana;
    private String lastName;
    private String lastNameKana;
    private String email;
    private String phoneNumber;
    private String password;
    private String role;
    private Integer siteId;
    private String participatingCompanyId;
}
