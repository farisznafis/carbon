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
  private Integer projectId;
  private Integer siteId;
  private Integer userId;
  private String projectType; 
  private String timeUpdated;
  
  @JsonProperty("date_recorder")
  private String dateRecorded;
  
  @JsonProperty("ELpv_value")
  private BigDecimal elPv;
  
  @JsonProperty("ELpvr_value")
  private BigDecimal elPvr;
  
  private Boolean changedBattery;
  
  @JsonProperty("ELPJ_battery_value")
  private BigDecimal elpjBatteryValue;
  
  @JsonProperty("ELPJ_control_value")
  private BigDecimal elpjControl;
  
  @JsonProperty("ELPJ_accuracy")
  private BigDecimal elpjAccuracy;

}
