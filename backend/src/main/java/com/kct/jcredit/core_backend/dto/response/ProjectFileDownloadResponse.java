package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter 
public class ProjectFileDownloadResponse extends BaseResponse{
    private String downloadUrl;
    private String originalFileName;
}