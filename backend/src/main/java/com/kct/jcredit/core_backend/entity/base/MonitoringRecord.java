package com.kct.jcredit.core_backend.entity.base;

import java.math.BigDecimal;
import java.time.Instant;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * Generic interface for all monitoring records across different methodologies.
 * Implementations must handle methodology-specific fields.
 * 
 * Examples:
 * - EnR002MonitoringRecord implements this with EL_PV, EL_PVR fields
 * - MonitoringTableRecordWind implements this with wind speed, turbine capacity
 * - MonitoringTableRecordHydro implements this with water flow fields
 * 
 * @author Modernization Team
 */
public interface MonitoringRecord {
    
        // Partition Key
    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getId();
    
    // Sort Key (use timeUpdated for efficient query by timestamp)
    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    String getTimeUpdated();

    @DynamoDbAttribute("date_recorder")
    public String getDateRecorded();

    @DynamoDbAttribute("projectID")
    public Integer getProjectID();
    
    @DynamoDbAttribute("siteID")
    public Integer getSiteID();
    
    @DynamoDbAttribute("participatingUserID")
    public Integer getParticipatingUserID();

    @DynamoDbAttribute("isActive")
    public Boolean getIsActive();
    
    @DynamoDbAttribute("processedAt")
    public String getProcessedAt();

    public void setProcessedAt(String timestamp);

     default String getMethodologyType() {
        // Default implementation - override in concrete classes if needed
        return this.getClass().getSimpleName()
            .replace("MonitoringTableRecord", "");
    }
    
}
