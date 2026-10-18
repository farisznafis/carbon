package com.kct.jcredit.core_backend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoRequest {
    
    @NotBlank(message = "fileIdを入力してください。")
    @JsonProperty("fileId")
    private String fileId;
    
    @NotBlank(message = "fieldReferenceを入力してください。")
    @JsonProperty("fieldReference")
    private String fieldReference;
    
    @NotBlank(message = "urlを入力してください。")
    @JsonProperty("url")
    private String url;
}