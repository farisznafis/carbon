package com.kct.jcredit.core_backend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter 
@Builder
public class ProjectCompanyResponse {
    private String companyId;
    private String companyName;
    private Integer participationId;
    private String pdfStatus;  // "READY" / "NOT_READY"
    private String pdfUrl; // presigned URL, null when the PDF is not ready
}