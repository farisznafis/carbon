package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.MonitoringDataDeletionHistory;
import com.kct.jcredit.core_backend.enums.DeletionResult;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonitoringDataDeletionHistoryRepository extends JpaRepository<MonitoringDataDeletionHistory, Integer> {
    Page<MonitoringDataDeletionHistory> findByProjectId(Integer projectId, Pageable pageable);

    Page<MonitoringDataDeletionHistory> findByProjectIdIn(Collection<Integer> projectIds, Pageable pageable);

    long countByProjectIdAndResult(Integer projectId, DeletionResult result);
}
