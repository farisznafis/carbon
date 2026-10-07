package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.enums.FileDirection;
import com.kct.jcredit.core_backend.enums.ProjectFileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter 
public class ProjectFileResponse extends BaseResponse {
    private Integer id;
    private ProjectFileType fileType;
    private Boolean ownedByKanrisha;
    private String originalFileName;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private Integer versionNo;
}