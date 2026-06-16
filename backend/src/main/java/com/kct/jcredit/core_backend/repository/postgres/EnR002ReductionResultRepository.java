package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.EnR002ReductionResult;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface EnR002ReductionResultRepository extends JpaRepository<EnR002ReductionResult, Integer> {
    

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

}
