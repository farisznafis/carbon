package com.kct.jcredit.core_backend.service.documentdata;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kct.jcredit.core_backend.constant.PermissionCode;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse;
import com.kct.jcredit.core_backend.enums.ProjectFileType;
import com.kct.jcredit.core_backend.service.projectplan.ProjectPlanContextService;
import com.kct.jcredit.core_backend.service.projectplan.ProjectPlanContextService.ProjectContext;
import com.kct.jcredit.core_backend.service.projectplan.ProjectPlanContextService.ProjectContextStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Answers for a document type through the provider registered for it.
 *
 * <p>Access is settled here, before any provider runs: the project must belong
 * to the caller's company, and an operator must hold whatever further
 * permission the provider names.
 */
@Service
public class ProjectDocumentDataServiceImpl implements ProjectDocumentDataService {

    static final String MSG_RETRIEVED = "書類データを取得しました。";
    static final String MSG_NO_DATA = "この書類種別にはシステムで保持しているデータがありません。";

    private final ProjectPlanContextService projectContextService;
    private final Map<ProjectFileType, DocumentDataProvider> providers = new EnumMap<>(ProjectFileType.class);

    public ProjectDocumentDataServiceImpl(
            ProjectPlanContextService projectContextService,
            List<DocumentDataProvider> providers) {
        this.projectContextService = projectContextService;
        for (DocumentDataProvider provider : providers) {
            if (this.providers.put(provider.fileType(), provider) != null) {
                throw new IllegalStateException(
                        "More than one document data provider for " + provider.fileType());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDocumentDataResponse getData(String projectId, ProjectFileType fileType, Caller caller) {
        ProjectDocumentDataResponse response = new ProjectDocumentDataResponse();
        response.setProjectId(projectId);
        response.setFileType(fileType);

        ProjectContext context = projectContextService.resolveProject(
                projectId, caller.companyId(), caller.role(), caller.username());
        if (!context.isAuthorized()) {
            return context.status() == ProjectContextStatus.NOT_FOUND
                    ? finish(response, ResponseConstants.NOT_FOUND, ResponseConstants.MSG_PROJECT_NOT_FOUND)
                    : finish(response, ResponseConstants.FORBIDDEN, ResponseConstants.MSG_FORBIDDEN);
        }

        DocumentDataProvider provider = providers.get(fileType);
        if (provider == null) {
            response.setSections(List.of());
            return finish(response, ResponseConstants.OK, MSG_NO_DATA);
        }

        if (!holds(caller, provider.additionalPermission())) {
            return finish(response, ResponseConstants.FORBIDDEN, ResponseConstants.MSG_FORBIDDEN);
        }

        response.setSections(provider.sections(context.project(), projectId, caller.username()));
        return finish(response, ResponseConstants.OK, MSG_RETRIEVED);
    }

    /** An admin holds every permission; an operator only the ones granted. */
    private static boolean holds(Caller caller, Optional<PermissionCode> permission) {
        if (permission.isEmpty() || RoleConstants.ADMIN.equals(caller.role())) {
            return true;
        }
        return caller.permissions() != null && caller.permissions().contains(permission.get().name());
    }

    private static ProjectDocumentDataResponse finish(
            ProjectDocumentDataResponse response, String statusCode, String message) {
        response.setStatusCode(statusCode);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}
