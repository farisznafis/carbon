package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.enums.ProjectFileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * One entry inside ProjectFileListResponse#files. Not a BaseResponse
 * itself - statusCode/message live once on the parent list response,
 * not repeated per item.
 */
@Getter
@Builder
@AllArgsConstructor
public class ProjectFileItem {
    private Integer id;
    private ProjectFileType fileType;
    private Boolean ownedByKanrisha;
    private String originalFileName;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
}