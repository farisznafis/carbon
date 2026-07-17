package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pc FROM ParticipatingCompany pc WHERE pc.id = :id")
    Optional<ParticipatingCompany> findByIdForUpdate(@Param("id") Integer id);

    Optional<ParticipatingCompany> findByCorpNumber(String corpNumber);

    Optional<ParticipatingCompany> findByEmailIgnoreCase(String email);

    @Modifying
    @Query("UPDATE ParticipatingCompany pc SET pc.deletedAt = CURRENT_TIMESTAMP, pc.deletedBy = :deletedBy WHERE pc.companyId = :companyId")
    void softDeleteByCompanyId(@Param("companyId") String companyId, @Param("deletedBy") String deletedBy);
}
