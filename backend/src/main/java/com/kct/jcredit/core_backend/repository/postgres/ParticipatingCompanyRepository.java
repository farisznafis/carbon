package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.enums.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipatingCompanyRepository extends JpaRepository<ParticipatingCompany, Integer>,
        JpaSpecificationExecutor<ParticipatingCompany> {
    
    Optional<ParticipatingCompany> findByCompanyId(String companyId);

    /**
     * Locks the company row while lifecycle transitions are evaluated and persisted.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pc FROM ParticipatingCompany pc WHERE pc.companyId = :companyId")
    Optional<ParticipatingCompany> findByCompanyIdForUpdate(@Param("companyId") String companyId);

    Optional<ParticipatingCompany> findByCorpNumber(String corpNumber);
    
    @Query("SELECT pc FROM ParticipatingCompany pc WHERE pc.contractEndDate <= :date AND pc.subscriptionStatus = :status")
    List<ParticipatingCompany> findByContractEndDateBeforeAndSubscriptionStatus(
            @Param("date") LocalDate date, 
            @Param("status") SubscriptionStatus status);
    
    @Query("SELECT pc FROM ParticipatingCompany pc WHERE pc.subscriptionStatus = :status")
    List<ParticipatingCompany> findBySubscriptionStatus(@Param("status") SubscriptionStatus status);

    @Modifying
    @Query("UPDATE ParticipatingCompany pc SET pc.deletedAt = CURRENT_TIMESTAMP, pc.deletedBy = :deletedBy WHERE pc.companyId = :companyId")
    void softDeleteByCompanyId(@Param("companyId") String companyId, @Param("deletedBy") String deletedBy);
}
