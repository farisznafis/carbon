package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.RecordCancelRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

@Repository
public interface RecordCancelRequestRepository extends JpaRepository<RecordCancelRequest, Integer> {
    
    
    /**
     * Find all active cancel requests by recordId
     */
    List<RecordCancelRequest> findByRecordPkAndRecordSkAndIsActiveTrue(
        String recordPk, 
        String recordSk  
    );
    
    /**
     * Check if pending request exists for recordId
     */
    @Query("SELECT COUNT(cr) > 0 FROM RecordCancelRequest cr " +
           "WHERE cr.recordPk = :recordPk AND cr.recordSk = :recordSk " +
           "AND cr.status = 'PENDING' " +
           "AND cr.isActive = true")
    boolean existsPendingRequestForRecord(
        @Param("recordPk") String recordPk,
        @Param("recordSk") String recordSk  
    );
    
}
