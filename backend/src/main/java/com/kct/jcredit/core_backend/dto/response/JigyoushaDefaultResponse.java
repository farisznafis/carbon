package com.kct.jcredit.core_backend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JigyoushaDefaultResponse {
    private String companyName;
    private String repName;
    private String repRole;
}
