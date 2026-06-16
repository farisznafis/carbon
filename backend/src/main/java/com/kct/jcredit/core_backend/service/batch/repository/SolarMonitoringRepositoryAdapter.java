package com.kct.jcredit.core_backend.service.batch.repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringTableRecordSolarRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter that implements GenericMonitoringRepository for Solar records.
 * Wraps the existing MonitoringTableRecordSolarRepository.
 * 
 * This allows the batch processing to work generically while maintaining
 * compatibility with existing solar-specific repository implementations.
 * 
 * @author Modernization Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SolarMonitoringRepositoryAdapter implements GenericMonitoringRepository<EnR002MonitoringRecord> {
    
    private final MonitoringTableRecordSolarRepository solarRepository;
    private static final String TABLE_NAME = "solar_monitoring_table_record";
    
    @Override
    public EnR002MonitoringRecord save(EnR002MonitoringRecord record) {
        return solarRepository.save(record);
    }
    
    @Override
    public List<EnR002MonitoringRecord> findUnprocessedRecordsForSite(
            Integer projectId,
            Integer siteId,
            Instant startTime,
            Instant endTime) {
        
        String pk = projectId + "#" + siteId;
        String startSk = startTime.toString();
        String endSk = endTime.toString();
        
        log.debug("Querying solar records for site {}: pk={}, sk between {} and {}",
            siteId, pk, startSk, endSk);
        
        // Use existing repository to get records in time range
        List<EnR002MonitoringRecord> recordsInRange =
            solarRepository.findByPkAndSkBetween(pk, startSk, endSk);
        
        // Filter for unprocessed and active records (in-memory filter is fast)
        List<EnR002MonitoringRecord> unprocessed = recordsInRange.stream()
            .filter(record -> record.getProcessedAt() == null || record.getProcessedAt().isEmpty())
            .filter(record -> Boolean.TRUE.equals(record.getIsActive()))
            .collect(Collectors.toList());
        
        log.debug("Found {} unprocessed records for site {}", unprocessed.size(), siteId);
        return unprocessed;
    }
    
    @Override
    public Map<Integer, List<EnR002MonitoringRecord>> findUnprocessedRecordsForProject(
            Integer projectId,
            List<Integer> siteIds,
            Instant startTime,
            Instant endTime) {
        
        Map<Integer, List<EnR002MonitoringRecord>> result = new HashMap<>();
        int totalRecords = 0;
        
        for (Integer siteId : siteIds) {
            List<EnR002MonitoringRecord> unprocessedRecords =
                findUnprocessedRecordsForSite(projectId, siteId, startTime, endTime);
            
            result.put(siteId, unprocessedRecords);
            totalRecords += unprocessedRecords.size();
            
            log.debug("Site {}: {} unprocessed records", siteId, unprocessedRecords.size());
        }
        
        log.info("Loaded {} total unprocessed solar records for project {} from {} sites",
            totalRecords, projectId, siteIds.size());
        
        return result;
    }
    
    @Override
    public Optional<EnR002MonitoringRecord> findByPrimaryAndSortKey(String pk, String sk) {
        return solarRepository.findByPKandSK(pk, sk);
    }
    
    @Override
    public void deleteByKeys(String pk, String sk) {
        solarRepository.delete(pk, sk);
    }
    
    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

}
