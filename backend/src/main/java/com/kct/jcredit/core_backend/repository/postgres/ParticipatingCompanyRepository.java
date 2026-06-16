package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipatingCompanyRepository extends JpaRepository<ParticipatingCompany, Integer> {
    
    Optional<ParticipatingCompany> findByCompanyId(String companyId);

    Optional<ParticipatingCompany> findByCorpNumber(String corpNumber);
    
    @Query("SELECT pc FROM ParticipatingCompany pc WHERE pc.contractEndDate <= :date AND pc.subscriptionStatus = :status")
    List<ParticipatingCompany> findByContractEndDateBeforeAndSubscriptionStatus(
            @Param("date") LocalDate date, 
            @Param("status") SubscriptionStatus status);
    
    @Query("SELECT pc FROM ParticipatingCompany pc WHERE pc.subscriptionStatus = :status")
    List<ParticipatingCompany> findBySubscriptionStatus(@Param("status") SubscriptionStatus status);

    List<ParticipatingCompany> findByRegStatus(String regStatus);
}
