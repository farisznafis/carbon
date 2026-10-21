package com.kct.jcredit.core_backend.service.documentdata;

import java.util.List;
import java.util.Optional;

import com.kct.jcredit.core_backend.constant.PermissionCode;
import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse.Section;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.enums.ProjectFileType;

/**
 * Collects what the system holds for one kind of project document.
 *
 * <p>One implementation per document type. Adding a document type, or the
 * same document for another methodology, is adding a provider; nothing else
 * changes.
 */
public interface DocumentDataProvider {

    /** The document type this provider answers for. */
    ProjectFileType fileType();

    /**
     * A permission an operator must hold on top of the one the endpoint
     * requires. A provider that reads data guarded elsewhere by its own
     * permission names it here, so this endpoint is not a way around it.
     */
    default Optional<PermissionCode> additionalPermission() {
        return Optional.empty();
    }

    /**
     * The sections of the document. The caller's right to see the project has
     * already been established.
     *
     * @param projectId the identifier the project was requested by
     * @param username  the caller, for services that record or re-check who reads
     */
    List<Section> sections(Project project, String projectId, String username);
}
