package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.response.ProjectFileDownloadResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectFileListResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectFileResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.enums.ProjectFileType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectFileService {

    /** Uploads one file of the given type for a project. Never overwrites
     *  an existing file - every upload is a new row, preserving history. */
    ProjectFileResponse uploadFile(String projectPublicId, ProjectFileType fileType, MultipartFile file, String username);

    /** All active documents for a project, newest first. */
    ProjectFileListResponse listFiles(String projectPublicId, ProjectFileType fileTypeFilter);

    // /** All active documents for a project, filtered to one type - e.g. the
    //  *  full revision history of PROJECT_PLAN. */
    // List<ProjectFileResponse> listFilesByType(String projectPublicId, ProjectFileType fileType);

    /** Generates a short-lived presigned URL to download one file. */
    ProjectFileDownloadResponse getDownloadUrl(String projectPublicId, Integer fileId);

    /** Soft-deletes one uploaded file (e.g. wrong file uploaded by mistake). */
    BaseResponse deleteFile(String projectPublicId, Integer fileId, String username);
}