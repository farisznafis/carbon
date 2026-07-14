package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer>, JpaSpecificationExecutor<Project>  {
	java.util.Optional<Project> findByProjectNumber(String projectNumber);
	boolean existsByProjectNumber(String projectNumber);
	boolean existsByMethodNumber(String methodNumber);
	List<Project> findByStatus(String status);
}
