package com.kct.jcredit.core_backend.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.math.BigDecimal;
import com.kct.jcredit.core_backend.entity.base.MonitoringRecord;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class EnR002MonitoringRecord implements MonitoringRecord {
    
    private String id;
    private Integer projectID;
    private Integer siteID;
    private Integer participatingUserID;
    private String timeUpdated;
    private String dateRecorded;
    
    // 設備の仕様書の値
    private BigDecimal elpjControlValue;
    
    // 証憑設備の仕様書
    private String elpjControlEquipmentSpecFiles;
    
    // 計測器（電力計）に表示する値
    private BigDecimal elpjBatteryValue;
    
    // ELPJ計測精度
    private BigDecimal elpjAccuracy;
    
    // 検診時の写真証憑
    private String elpjBatteryEvidence;
    
    // ELpv値
    private BigDecimal elPv;
    
    // 計測器又はログ
    private String elPvEvidence;
    
    // ELpvr値
    private BigDecimal elPvr;
    
    // 電力会社からの「購入電力量のおしらせ」資料
    private String elPvrEvidence;
    
    // Update 1: 4 Evidence URLs for manual data registration
    private String elPvEvidenceUrl;
    private String elPvrEvidenceUrl;
    private String elpjBatterySpecificationUrl;
    private String elpjControlEvidenceUrl;

    // For IF Battery交換あり condition
    private Boolean changedBattery; 
    
    private Boolean isActive; // レコードの有効/無効を示すフラグ

    private String processedAt; // レコードが処理された日時を保存するフィールド
    
    
    // Partition Key
    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getId() {
        return id;
    }
    
    // Sort Key (use timeUpdated for efficient query by timestamp)
    @Override
    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getTimeUpdated() {
        return timeUpdated;
    }

    @Override
    @DynamoDbAttribute("date_recorder")
    public String getDateRecorded() {
        return dateRecorded;
    }

    @Override
    @DynamoDbAttribute("projectID")
    public Integer getProjectID() {
        return projectID;
    }
    
    @Override
    @DynamoDbAttribute("siteID")
    public Integer getSiteID() {
        return siteID;
    }
    
    @Override
    @DynamoDbAttribute("participatingUserID")
    public Integer getParticipatingUserID() {
        return participatingUserID;
    }

    @Override
    @DynamoDbAttribute("isActive")
    public Boolean getIsActive() {
        return isActive != null ? isActive : true;
    }
    
    @Override
    @DynamoDbAttribute("processedAt")
    public String getProcessedAt() {
        return processedAt;
    }

    @Override
    public void setProcessedAt(String processedAt) {
        this.processedAt = processedAt;
    }

    @Override
    public String getMethodologyType() {
        return "EnR002"; // Solar methodology
    }
    

    // Methodology-specific fields with DynamoDB attributes
    
    @DynamoDbAttribute("ELPJ_control_value")
    public BigDecimal getElpjControlValue() {
        return elpjControlValue;
    }
    
    @DynamoDbAttribute("ELPJ_control_equipment_spec_files")
    public String getElpjControlEquipmentSpecFiles() {
        return elpjControlEquipmentSpecFiles;
    }
    
    @DynamoDbAttribute("ELPJ_battery_value")
    public BigDecimal getElpjBatteryValue() {
        return elpjBatteryValue;
    }
    
    @DynamoDbAttribute("ELPJ_accuracy")
    public BigDecimal getElpjAccuracy() {
        return elpjAccuracy;
    }
    
    @DynamoDbAttribute("ELPJ_battery_evidence")
    public String getElpjBatteryEvidence() {
        return elpjBatteryEvidence;
    }
    
    @DynamoDbAttribute("ELpv")
    public BigDecimal getElPv() {
        return elPv;
    }
    
    @DynamoDbAttribute("ELpv_evidence")
    public String getElPvEvidence() {
        return elPvEvidence;
    }
    
    @DynamoDbAttribute("ELpvr")
    public BigDecimal getElPvr() {
        return elPvr;
    }
    
    @DynamoDbAttribute("ELpvr_evidence")
    public String getElPvrEvidence() {
        return elPvrEvidence;
    }

    @DynamoDbAttribute("changedBattery")
    public Boolean getChangedBattery() {
        return changedBattery != null ? changedBattery : false;
    }
    
    @DynamoDbAttribute("elPvEvidenceUrl")
    public String getElPvEvidenceUrl() {
        return elPvEvidenceUrl;
    }
    
    @DynamoDbAttribute("elPvrEvidenceUrl")
    public String getElPvrEvidenceUrl() {
        return elPvrEvidenceUrl;
    }
    
    @DynamoDbAttribute("elpjBatterySpecificationUrl")
    public String getElpjBatterySpecificationUrl() {
        return elpjBatterySpecificationUrl;
    }
    
    @DynamoDbAttribute("elpjControlEvidenceUrl")
    public String getElpjControlEvidenceUrl() {
        return elpjControlEvidenceUrl;
    }    
}