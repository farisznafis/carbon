package com.kct.jcredit.core_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.Column;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnR002EquipmentResponse {
    
     private Integer id;
    private Integer siteId;

    private LocalDate equipmentOpDate;
    private BigDecimal nominalMaxOutput;
    private String pcsManufacturer;
    private String pcsModel;
    private Integer powerCondUnitCount;
    private String deviceNo;
    private BigDecimal powerCondStandby;

    private Boolean hasBattery;
    private String batteryMaker;
    private String batteryModel;
    private BigDecimal batteryCapacity;

    private BigDecimal ictEquipmentPowerConsumption;

    private Boolean additionalEquipment;
    private LocalDate additionalEquipmentDate;
    private String equipmentType;
    private Integer unitCount;
    private String deviceUniqueNo;

    private Integer totalInvestment;
    private Boolean hasSubsidy;
    private String subsidyName;
    private BigDecimal subsidyAmount;
    private BigDecimal preProjectUnitPrice;
}