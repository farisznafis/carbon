package com.kct.jcredit.core_backend.dto.response.details;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminCompanyDetailSiteResponse {
    private String siteName;
    private String picEmail;
    private LocalDateTime createdAt;
    private String address;
    private String postalCode;
    private String prefecture;
    private String municipality;
    private String blocknum;
    private String buildingName;
    private List<String> projectNames;
}
