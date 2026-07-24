package com.kct.jcredit.core_backend.dto.response.details;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonitoringDataSolarResponse {
  private String id;
  private String projectId;
  private String siteId;
  private Integer userId;
  private String projectType;
  private String timeUpdated;
  private String dateRecorded;
  private BigDecimal elPv;
  private String elPvEvidence;
  private BigDecimal elPvr;
  private String elPvrEvidence;
  private String elPvEvidenceUrl;
  private String elPvrEvidenceUrl;
  private Boolean changedBattery;
  private BigDecimal elpjBatteryValue;
  private String elpjBatteryEvidence;
  private String elpjBatterySpecificationUrl;

  @JsonProperty("elpjControlValue")
  private BigDecimal elpjControl;

  private String elpjControlEquipmentSpecFiles;
  private String elpjControlEvidenceUrl;
  private BigDecimal elpjAccuracy;
  private Boolean isActive;
  private String processedAt;
  private String methodologyType;
}
