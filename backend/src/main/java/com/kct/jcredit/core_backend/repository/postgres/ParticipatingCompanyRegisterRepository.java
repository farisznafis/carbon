package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
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
           "FROM ParticipatingCompany c WHERE c.email = :email AND c.regStatus <> :regStatus")
    boolean existsByEmailAndRegStatusNot(@Param("email") String email, @Param("regStatus") String regStatus);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM ParticipatingCompany c WHERE c.repEmail = :repEmail AND c.regStatus <> :regStatus")
    boolean existsByRepEmailAndRegStatusNot(@Param("repEmail") String repEmail, @Param("regStatus") String regStatus);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM ParticipatingCompany c WHERE c.corpNumber = :corpNumber AND c.regStatus <> :regStatus")
    boolean existsByCorpNumberAndRegStatusNot(@Param("corpNumber") String corpNumber, @Param("regStatus") String regStatus);

    Optional<ParticipatingCompany> findByEmailAndRegStatus(String email, String regStatus);
    Optional<ParticipatingCompany> findByRepEmailAndRegStatus(String repEmail, String regStatus);
    Optional<ParticipatingCompany> findByCorpNumberAndRegStatus(String corpNumber, String regStatus);
}
