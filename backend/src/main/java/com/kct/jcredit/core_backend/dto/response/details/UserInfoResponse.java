package com.kct.jcredit.core_backend.dto.response.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoResponse {
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
    private String userPosition;
    private String siteId;
    private String siteName;
    private String companyId;
    private List<String> permissions;
    private CompanyInfoResponse company;
}
