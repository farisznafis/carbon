package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProjectFileListResponse extends BaseResponse {
    private List<ProjectFileItem> files;
    private List<FileVersionSummaryResponse> versionSummary;   
}
