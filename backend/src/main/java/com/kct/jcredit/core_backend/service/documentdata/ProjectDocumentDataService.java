package com.kct.jcredit.core_backend.service.documentdata;

import java.util.Set;

import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse;
import com.kct.jcredit.core_backend.enums.ProjectFileType;

/**
 * What the system holds for a project document, for the 書類 screen to show
 * beside the uploaded files.
 *
 * <p>Nothing is read from the files: the data is the system's own, the same
 * the operator maintains on the project and monitoring screens.
 */
public interface ProjectDocumentDataService {

    /**
     * The data of one document type of one project.
     *
     * <p>Answered only for an admin or operator of the company the project
     * belongs to, and — where the document shows data guarded by a further
     * permission — only for a caller holding it. A document type the system
     * keeps no data for is answered with no sections.
     */
    ProjectDocumentDataResponse getData(String projectId, ProjectFileType fileType, Caller caller);

    /**
     * Who is asking, as the authenticated token states it.
     *
     * @param permissions the permission codes granted to the caller
     */
    record Caller(String companyId, String role, String username, Set<String> permissions) {
    }
}
