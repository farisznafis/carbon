package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.Project;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer>, JpaSpecificationExecutor<Project>  {
	java.util.Optional<Project> findByPublicId(String publicId);
	java.util.Optional<Project> findByProjectNumber(String projectNumber);
	boolean existsByProjectNumber(String projectNumber);
	boolean existsByMethodNumber(String methodNumber);

	List<Project> findByProjectStatus(String status);

	default java.util.Optional<Project> findByPublicIdWithFallback(String publicId) {
		try {
			java.util.Optional<Project> proj = findByPublicId(publicId);
			if (proj.isPresent()) {
				return proj;
			}
		} catch (Exception e) {
			// Catching DataIntegrityViolationException or SQL exception if column doesn't exist
		}
		
		if (publicId != null && publicId.startsWith("PROJ")) {
			try {
				Integer id = Integer.parseInt(publicId.substring(4));
				return findById(id);
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		if (publicId != null && publicId.matches("\\d+")) {
			try {
				return findById(Integer.parseInt(publicId));
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		return java.util.Optional.empty();
	}

	/** Row lock (SELECT ... FOR UPDATE) to serialize uploads per project. Must run inside a transaction. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM Project p WHERE p.id = :id")
	Optional<Project> findByIdForUpdate(@Param("id") Integer id);

}
