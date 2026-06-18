package com.kct.jcredit.core_backend.repository.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kct.jcredit.core_backend.entity.ParticipatingUser;

import java.util.Optional;
import java.util.List;


@Repository
public interface ParticipatingUserRepository extends JpaRepository<ParticipatingUser, Integer> {
    Optional<ParticipatingUser> findByUsername(String username);
    Optional<ParticipatingUser> findByEmail(String email);
    @Query("SELECT u FROM ParticipatingUser u WHERE (u.username = :username OR u.email = :email) AND u.companyId = :companyId")
    Optional<ParticipatingUser> findByUsernameOrEmailAndCompanyId(String username, String email, String companyId);
    List<ParticipatingUser> findByCompanyId(String companyId);
    Optional<ParticipatingUser> findByCompanyIdAndRole(String companyId, String role);
    Optional<ParticipatingUser> findByUsernameAndCompanyId(String username, String companyId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE ParticipatingUser u SET u.deletedAt = CURRENT_TIMESTAMP, u.deletedBy = :deletedBy WHERE u.companyId = :companyId")
    void softDeleteByCompanyId(@Param("companyId") String companyId, @Param("deletedBy") String deletedBy);
}
