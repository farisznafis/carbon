package com.kct.jcredit.core_backend.dto.response.details;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileInfoResponse {
    private String id;
    private String fileType;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String url;
    private String description;
    private String uploadedAt;
}

