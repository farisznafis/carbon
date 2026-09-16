package com.kct.jcredit.core_backend.dto.request;

import lombok.Data;

@Data
public class DeleteUserSite {
    private String userId;
    private String siteId;
}