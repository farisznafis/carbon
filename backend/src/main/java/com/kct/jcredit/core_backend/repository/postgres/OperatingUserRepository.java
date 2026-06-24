package com.kct.jcredit.core_backend.repository.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kct.jcredit.core_backend.entity.OperatingUser;
import java.util.List;

import java.util.Optional;

@Repository
public interface OperatingUserRepository extends JpaRepository<OperatingUser, Integer> {
    // Used in login: find operator by username/email and company ID
    Optional<OperatingUser> findByUsernameOrEmailAndCompany_Id(String username, String email, Integer companyId);
    Optional<OperatingUser> findByUsername(String username);
    Optional<OperatingUser> findByEmailOrUsername(String email, String username);
    List<OperatingUser> findByCompany_Id(Integer companyId);
    Optional<OperatingUser> findByCompany_IdAndRole(Integer companyId, String role);
    Optional<OperatingUser> findByUsernameAndCompany_Id(String username, Integer companyId);
}
