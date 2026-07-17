package com.kct.jcredit.core_backend.repository.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kct.jcredit.core_backend.entity.OperatingUser;
import java.util.List;

import java.util.Optional;

@Repository
public interface OperatingUserRepository extends JpaRepository<OperatingUser, Integer> {
    // Used in login: find operator by username/email and company ID
    Optional<OperatingUser> findByUsernameOrEmailAndCompany_Id(String username, String email, Integer companyId);
    Optional<OperatingUser> findByUsername(String username);
    Optional<OperatingUser> findByUsernameIgnoreCase(String username);
    @Query("SELECT u FROM OperatingUser u WHERE LOWER(u.email) = LOWER(:email) OR LOWER(u.username) = LOWER(:username)")
    Optional<OperatingUser> findByEmailOrUsername(@Param("email") String email, @Param("username") String username);
    Optional<OperatingUser> findByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCase(String username);

    List<OperatingUser> findByCompany_Id(Integer companyId);
    Optional<OperatingUser> findByCompany_IdAndRole(Integer companyId, String role);
    Optional<OperatingUser> findByUsernameAndCompany_Id(String username, Integer companyId);

    /**
     * Resolves an operator strictly through the operating-user and operator-company domain.
     */
    @Query("""
            SELECT ou
            FROM OperatingUser ou
            JOIN FETCH ou.company oc
            WHERE ou.username = :username
              AND oc.companyId = :companyId
            """)
    Optional<OperatingUser> findForAuthorization(
            @Param("username") String username,
            @Param("companyId") String companyId);
}
