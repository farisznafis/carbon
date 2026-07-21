package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.CompanyDeletionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyDeletionRequestRepository extends JpaRepository<CompanyDeletionRequest, Integer> {

    Optional<CompanyDeletionRequest> findByCompanyIdAndStatus(String companyId, String status);

    boolean existsByCompanyIdAndStatus(String companyId, String status);
}
