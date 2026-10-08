package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.ProjectFiles;
import com.kct.jcredit.core_backend.enums.ProjectFileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectFilesRepository extends JpaRepository<ProjectFiles, Integer> {

    /** Full document list for a project, newest first - backs the list screen. */
    List<ProjectFiles> findByProjectIdAndIsActiveTrueOrderByCreatedAtDesc(Integer projectId);

    /** Every upload of one document type, newest first. The first item is
     *  always "the latest version" - no separate flag needed for that. */
    List<ProjectFiles> findByProjectIdAndFileTypeAndIsActiveTrueOrderByCreatedAtDesc(
            Integer projectId, ProjectFileType fileType);

    /** Active versions of one type - checked against the version limit before upload. */
    long countByProjectIdAndFileTypeAndIsActiveTrue(Integer projectId, ProjectFileType fileType);

    /** Highest version ever issued, including deleted ones, so numbers never go backwards. */
    @Query("SELECT COALESCE(MAX(f.versionNo), 0) FROM ProjectFiles f " +
           "WHERE f.projectId = :projectId AND f.fileType = :fileType")
    int findMaxVersionNo(@Param("projectId") Integer projectId,
                         @Param("fileType") ProjectFileType fileType);

    interface FileTypeCount {
        ProjectFileType getFileType();
        long getActiveCount();
    }

    /** Active file count per type for one project - backs the upload-availability summary. */
    @Query("SELECT f.fileType AS fileType, COUNT(f) AS activeCount FROM ProjectFiles f " +
           "WHERE f.projectId = :projectId AND f.isActive = true GROUP BY f.fileType")
    List<FileTypeCount> countActiveByFileType(@Param("projectId") Integer projectId);
}