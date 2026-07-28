package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.BatchProcessHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface BatchProcessHistoryRepository extends JpaRepository<BatchProcessHistory, Integer> {
    java.util.List<BatchProcessHistory> findByStatusAndClaimedByNot(String status, String claimedBy);
    List<BatchProcessHistory> findByStatus(String status);
    BatchProcessHistory findFirstByStatusInOrderByStartTimeDesc(List<String> statuses);
}
