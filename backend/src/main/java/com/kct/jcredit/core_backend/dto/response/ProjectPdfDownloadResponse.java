package com.kct.jcredit.core_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.Column;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectPdfDownloadResponse {
    // Presigned URL dari S3 — berlaku sesuai konfigurasi (default 15 menit)
    private String pdfUrl;

    // Berapa detik URL ini masih berlaku
    private Integer expiresIn;
    
}
