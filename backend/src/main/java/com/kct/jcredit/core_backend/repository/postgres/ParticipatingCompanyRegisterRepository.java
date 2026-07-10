package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParticipatingCompanyRegisterRepository extends JpaRepository<ParticipatingCompany, Long> {
    
    Optional<ParticipatingCompany> findByCompanyId(String companyId);
    boolean existsByCompanyId(String companyId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM ParticipatingCompany c WHERE c.email = :email AND c.companyStatus <> :status")
    boolean existsByEmailAndCompanyStatusNot(
            @Param("email") String email,
            @Param("status") CompanyStatus status);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM ParticipatingCompany c WHERE c.corpNumber = :corpNumber AND c.companyStatus <> :status")
    boolean existsByCorpNumberAndCompanyStatusNot(
            @Param("corpNumber") String corpNumber,
            @Param("status") CompanyStatus status);

    Optional<ParticipatingCompany> findByEmailAndCompanyStatus(String email, CompanyStatus status);
    Optional<ParticipatingCompany> findByCorpNumberAndCompanyStatus(String corpNumber, CompanyStatus status);
}
