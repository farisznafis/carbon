package com.kct.jcredit.core_backend.service.batch.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.kct.jcredit.core_backend.entity.base.MonitoringRecord;

/**
 * Generic repository interface for accessing monitoring data from DynamoDB.
 * Implementations handle methodology-specific table access.
 * 
 * T is bounded to MonitoringRecord to ensure type safety.
 * 
 * @param <T> The monitoring record type (must implement MonitoringRecord)
 * @author Modernization Team
 */
public interface GenericMonitoringRepository<T extends MonitoringRecord> {
    
    /**
     * Saves a monitoring record.
     * 
     * @param record Record to save
     * @return Saved record
     */
    T save(T record);
    
    /**
     * Finds unprocessed records for a site within a time range.
     * 
     * Filters for:
     * - Records with processingDate = null (not yet processed)
     * - Records with isActive = true
     * - Records with timeUpdated between startTime and endTime
     * 
     * @param projectId Project ID
     * @param siteId Site ID
     * @param startTime Start of time range (inclusive)
     * @param endTime End of time range (inclusive)
     * @return List of unprocessed records in the time range
     */
    List<T> findUnprocessedRecordsForSite(
        Integer projectId,
        Integer siteId,
        Instant startTime,
        Instant endTime
    );
    
    /**
     * Finds all unprocessed records for a project within a time range.
     * Used for bulk loading across multiple sites.
     * 
     * @param projectId Project ID
     * @param siteIds List of site IDs belonging to the project
     * @param startTime Start of time range (inclusive)
     * @param endTime End of time range (inclusive)
     * @return Map of siteId to list of unprocessed records
     */
    java.util.Map<Integer, List<T>> findUnprocessedRecordsForProject(
        Integer projectId,
        List<Integer> siteIds,
        Instant startTime,
        Instant endTime
    );
    
    /**
     * Finds a record by its primary and sort keys.
     * 
     * @param pk Partition key
     * @param sk Sort key
     * @return Record if found
     */
    Optional<T> findByPrimaryAndSortKey(String pk, String sk);
    
    /**
     * Deletes a record by its primary and sort keys.
     * 
     * @param pk Partition key
     * @param sk Sort key
     */
    void deleteByKeys(String pk, String sk);
    
    /**
     * Gets the table name this repository manages.
     * Used for logging and diagnostics.
     */
    String getTableName();
}
