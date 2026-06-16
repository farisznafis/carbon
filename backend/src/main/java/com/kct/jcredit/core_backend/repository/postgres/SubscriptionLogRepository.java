package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.SubscriptionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionLogRepository extends JpaRepository<SubscriptionLog, Integer> {
    List<SubscriptionLog> findByCompanyCompanyIdOrderByActionDateDesc(String companyId);
}