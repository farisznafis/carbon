package com.kct.jcredit.core_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileSolarUploadResponse {

    private String statusCode;
    private String s3Key;
    private String fileId;
    private String originalName;
}
 
