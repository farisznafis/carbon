package com.kct.jcredit.core_backend.repository.postgres;

import com.kct.jcredit.core_backend.entity.EnR002ReductionResultTotal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface EnR002ReductionResultTotalRepository extends JpaRepository<EnR002ReductionResultTotal, Integer> {
    boolean existsByProjectIdAndProcessingDate(Integer projectId, LocalDate processingDate);

    Optional<EnR002ReductionResultTotal> findByProjectId(Integer projectId);

}
