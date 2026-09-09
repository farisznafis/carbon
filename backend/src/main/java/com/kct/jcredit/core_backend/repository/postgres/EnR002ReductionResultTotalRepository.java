package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.EnR002ReductionResultTotal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnR002ReductionResultTotalRepository extends JpaRepository<EnR002ReductionResultTotal, Integer> {
    boolean existsByProjectIdAndProcessingDate(Integer projectId, LocalDate processingDate);

    Optional<EnR002ReductionResultTotal> findByProjectId(Integer projectId);

    @Modifying
    @Query(value = "UPDATE en_r002_reduction_result_total t SET deleted_at = CURRENT_TIMESTAMP, deleted_by = :deletedBy "
         + "WHERE t.project_id IN (SELECT spr.project_id FROM site_project_relation spr "
         + "JOIN site s ON s.id = spr.site_id "
         + "JOIN participating_company pc ON pc.id = s.participating_company_id "
         + "WHERE pc.company_id = :companyId)",
         nativeQuery = true)
    void softDeleteByCompanyId(@Param("companyId") String companyId, @Param("deletedBy") String deletedBy);

    List<EnR002ReductionResultTotal> findByProjectIdOrderByEndTimeAsc(Integer projectId);

    Optional<EnR002ReductionResultTotal> findTopByProjectIdOrderByEndTimeDesc(Integer projectId);

}
