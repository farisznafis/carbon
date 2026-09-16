package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import lombok.Builder;
import lombok.Getter;

@Getter 
@Builder
public class PdfDownloadResponse extends BaseResponse{
    private String pdfUrl;
    private Integer expiresIn;
}
