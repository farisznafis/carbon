package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.EnR002ReductionResult;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface EnR002ReductionResultRepository extends JpaRepository<EnR002ReductionResult, Integer> {
    

    List<EnR002ReductionResult> findByProjectIdAndSiteIdAndIsActiveTrueOrderByStartTimeDesc(Integer projectId, Integer siteId);

    EnR002ReductionResult findTopByProjectIdAndSiteIdAndIsActiveTrueOrderByStartTimeDesc(Integer projectId, Integer siteId);


        /**
     * Finds the processing date of the last battery emissions calculation for a site.
     * Returns the most recent date where empjSBattery > 0.
     * 
     * @param projectId Project ID
     * @param siteId Site ID
     * @return Optional containing last battery calculation date, or empty if none found
     */
    @Query("""
        SELECT e.processingDate 
        FROM EnR002ReductionResult e 
        WHERE e.projectId = :projectId 
        AND e.siteId = :siteId 
        AND e.empjSBattery > 0 
        ORDER BY e.processingDate DESC 
        LIMIT 1
        """)
    Optional<LocalDate> findLastBatteryCalculationDate(
        @Param("projectId") Integer projectId,
        @Param("siteId") Integer siteId
    );

    @Modifying
    @Query(value = "UPDATE en_r002_reduction_result r SET deleted_at = CURRENT_TIMESTAMP, deleted_by = :deletedBy "
         + "WHERE r.site_id IN (SELECT s.id FROM site s JOIN participating_company pc ON pc.id = s.participating_company_id WHERE pc.company_id = :companyId)",
         nativeQuery = true)
    void softDeleteByCompanyId(@Param("companyId") String companyId, @Param("deletedBy") String deletedBy);

    Optional<EnR002ReductionResult> findTopByProjectIdAndSiteIdAndEquipmentIdOrderByBatchIdDesc(
        Integer projectId, Integer siteId, Integer equipmentId
    );

    //  List<PeriodSummaryResponse> countByProjectIdAndBatchId (Integer projectId, Integer batchId);
        long countByProjectIdAndBatchId(Integer projectId, Integer batchId);

    Page<EnR002ReductionResult> findByProjectIdAndBatchId(Integer projectId, Integer batchId, Pageable pageable);
    @Query("""
        SELECT r FROM EnR002ReductionResult r
        JOIN EnR002Equipment e ON e.id = r.equipmentId
        WHERE r.projectId = :projectId AND r.batchId = :batchId AND e.site.company.companyId = :companyId
        """)
    Page<EnR002ReductionResult> findByProjectIdAndBatchIdAndCompanyId(
            @Param("projectId") Integer projectId,
            @Param("batchId") Integer batchId,
            @Param("companyId") String companyId,
            Pageable pageable);


    // loadReportingWindows() で使用
    List<EnR002ReductionResult> findByProjectIdAndIsActiveTrue(Integer projectId);

    // loadReportingWindow() で使用
    List<EnR002ReductionResult> findByProjectIdAndEquipmentIdAndIsActiveTrue(
        Integer projectId, Integer equipmentId
    );

        /**
     * Finds the processing date of the last battery emissions calculation for a site.
     * Returns the most recent date where empjSBattery > 0.
     * 
     * @param projectId Project ID
     * @param siteId Site ID
     * @return Optional containing last battery calculation date, or empty if none found
     */
}
