package com.kct.jcredit.core_backend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter 
@Builder
public class PdfDownloadResponse {
    private String pdfUrl;
    private Integer expiresIn;
}
