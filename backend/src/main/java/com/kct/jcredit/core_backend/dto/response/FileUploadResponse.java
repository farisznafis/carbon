package com.kct.jcredit.core_backend.dto.response;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.FileErrorDetailsResponse;
import com.kct.jcredit.core_backend.dto.response.details.FileInfoResponse;

import lombok.Data;

@Data
public class FileUploadResponse extends BaseResponse {
    private FileInfoResponse file;
    private FileErrorDetailsResponse details; 
}

