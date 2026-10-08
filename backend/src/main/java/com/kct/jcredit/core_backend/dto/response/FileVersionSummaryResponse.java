package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.enums.ProjectFileType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileVersionSummaryResponse {
    private ProjectFileType fileType;
    private long activeCount;
    private int limit;
    private boolean canUpload;
}
