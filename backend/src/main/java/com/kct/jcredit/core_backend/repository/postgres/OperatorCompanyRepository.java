package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.OperatorCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OperatorCompanyRepository extends JpaRepository<OperatorCompany, Integer> {
    
    // Find by company ID (if you have a company_id field string type)
    Optional<OperatorCompany> findByCompanyId(String companyId);
}
