package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.ProjectFiles;
import com.kct.jcredit.core_backend.enums.ProjectFileType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectFilesRepository extends JpaRepository<ProjectFiles, Integer> {

    /** Full document list for a project, newest first - backs the list screen. */
    List<ProjectFiles> findByProjectIdAndIsActiveTrueOrderByCreatedAtDesc(Integer projectId);

    /** Every upload of one document type, newest first. The first item is
     *  always "the latest version" - no separate flag needed for that. */
    List<ProjectFiles> findByProjectIdAndFileTypeAndIsActiveTrueOrderByCreatedAtDesc(
            Integer projectId, ProjectFileType fileType);
}