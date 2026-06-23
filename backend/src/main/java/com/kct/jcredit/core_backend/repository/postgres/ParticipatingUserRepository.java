package com.kct.jcredit.core_backend.repository.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.kct.jcredit.core_backend.entity.ParticipatingUser;

import java.util.Optional;
import java.util.List;


@Repository
public interface ParticipatingUserRepository extends JpaRepository<ParticipatingUser, Integer> {
    Optional<ParticipatingUser> findByUsername(String username);
    Optional<ParticipatingUser> findByEmail(String email);
    @Query("SELECT u FROM ParticipatingUser u WHERE (u.username = :username OR u.email = :email) AND u.companyId = :companyId")
    Optional<ParticipatingUser> findByUsernameOrEmailAndCompanyId(String username, String email, Integer companyId);
    List<ParticipatingUser> findByCompanyId(Integer companyId);
    Optional<ParticipatingUser> findByCompanyIdAndRole(Integer companyId, String role);
    Optional<ParticipatingUser> findByUsernameAndCompanyId(String username, Integer companyId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByCompany_IdAndSite_Id(Integer companyId, Integer siteId);
}
