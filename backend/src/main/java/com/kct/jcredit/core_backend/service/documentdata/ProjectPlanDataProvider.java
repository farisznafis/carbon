package com.kct.jcredit.core_backend.service.documentdata;

import java.util.List;

import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse.Section;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.enums.ProjectFileType;

import org.springframework.stereotype.Component;

/**
 * プロジェクト計画書: the project record, which is what the system keeps of the
 * plan.
 */
@Component
public class ProjectPlanDataProvider implements DocumentDataProvider {

    @Override
    public ProjectFileType fileType() {
        return ProjectFileType.PROJECT_PLAN;
    }

    @Override
    public List<Section> sections(Project project, String projectId, String username) {
        return List.of(
                ProjectRecordSections.overview(project),
                ProjectRecordSections.methodology(project),
                ProjectRecordSections.emissions(project),
                ProjectRecordSections.operator(project),
                ProjectRecordSections.representative(project),
                ProjectRecordSections.personInCharge(project));
    }
}
