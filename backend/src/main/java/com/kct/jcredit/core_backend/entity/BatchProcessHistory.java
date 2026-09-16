package com.kct.jcredit.core_backend.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import com.kct.jcredit.core_backend.entity.base.BaseEntity;

@Entity
@Table(name = "batch_process_history")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchProcessHistory extends BaseEntity {
    
    @Column(name = "start_time", nullable = false)
    private Instant startTime;
    
    @Column(name = "end_time", nullable = false)
    private Instant endTime;
    
    @Column(name = "status", nullable = false, length = 10)
    private String status;
    
    @Column(name = "claimed_by", nullable = false, length = 50)
    private String claimedBy;
    
    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt;
}