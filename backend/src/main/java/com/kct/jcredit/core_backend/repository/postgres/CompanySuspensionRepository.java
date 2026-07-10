package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.CompanySuspension;
import com.kct.jcredit.core_backend.enums.SuspensionReasonCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Provides persistence operations for active and historical company blockers.
 */
@Repository
public interface CompanySuspensionRepository extends JpaRepository<CompanySuspension, Integer> {

    /**
     * Checks whether the same blocker category is already active for a company.
     */
    boolean existsByCompany_IdAndReasonCodeAndResolvedAtIsNull(
            Integer companyId,
            SuspensionReasonCode reasonCode);

    /**
     * Counts all unresolved blockers for a company.
     */
    long countByCompany_IdAndResolvedAtIsNull(Integer companyId);

    /**
     * Loads a suspension with a write lock before it is resolved.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT suspension
            FROM CompanySuspension suspension
            JOIN FETCH suspension.company company
            WHERE suspension.id = :suspensionId
              AND company.companyId = :companyId
            """)
    Optional<CompanySuspension> findForUpdate(
            @Param("companyId") String companyId,
            @Param("suspensionId") Integer suspensionId);

    /**
     * Loads a suspension and its company for pre-payment validation.
     */
    @Query("""
            SELECT suspension
            FROM CompanySuspension suspension
            JOIN FETCH suspension.company company
            WHERE suspension.id = :suspensionId
              AND company.companyId = :companyId
            """)
    Optional<CompanySuspension> findDetails(
            @Param("companyId") String companyId,
            @Param("suspensionId") Integer suspensionId);

    /**
     * Returns only unresolved blockers when the active-only filter is selected.
     */
    Page<CompanySuspension> findByCompany_CompanyIdAndResolvedAtIsNull(
            String companyId,
            Pageable pageable);

    /**
     * Returns complete suspension history for a company.
     */
    Page<CompanySuspension> findByCompany_CompanyId(String companyId, Pageable pageable);
}
