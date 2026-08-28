package com.kct.jcredit.core_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import jakarta.persistence.Column;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectPdfDownloadResponse extends BaseResponse {
    // Presigned URL 
    private String pdfUrl;

    private Integer expiresIn;
}
