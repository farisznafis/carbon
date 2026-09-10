package com.kct.jcredit.core_backend.entity;

import com.kct.jcredit.core_backend.enums.FileDirection;
import com.kct.jcredit.core_backend.enums.ProjectFileType;
import com.kct.jcredit.core_backend.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One row = one file exchanged with the VVB or the scheme
 * secretariat/committee, for a given project. The physical file lives
 * in S3; this table only stores the S3 key reference - same pattern
 * as EnR002EquipmentFile.
 *
 * No version/submitted-flag column: "latest" is simply the row with
 * the newest createdAt for a given (projectId, fileType). If a
 * "which one did we actually send" distinction is needed later, add
 * it back then - deliberately left out for now to keep this lean.
 */
@Entity
@Table(name = "project_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFiles extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 30)
    private ProjectFileType fileType;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "original_file_name")
    private String originalFileName;

    /**
     * Always derived from fileType (ProjectFileType#isOwnedByKanrisha())
     * at upload time, never accepted from the request.
     * true  = we authored this document (e.g. PROJECT_PLAN)
     * false = an external body authored it (e.g. VALIDATION_REPORT)
     */
    @Column(name = "owned_by", nullable = false)
    private Boolean ownedByKanrisha;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;
}