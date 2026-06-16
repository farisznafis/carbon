package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.ProjectFiles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProjectFilesRepository extends JpaRepository<ProjectFiles, Integer> {
    Optional<ProjectFiles> findById(Integer projectId);
}