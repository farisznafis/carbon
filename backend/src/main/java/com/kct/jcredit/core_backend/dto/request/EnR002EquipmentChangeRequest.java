package com.kct.jcredit.core_backend.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EnR002EquipmentChangeRequest {

    @NotNull
    private LocalDate changeDate;

    private BigDecimal nominalMaxOutput;
    private String pcsManufacturer;
    private String pcsModel;
    // private Integer pcsUnitCount;
    private String deviceNo;
    private BigDecimal pcsStandby;
    private Boolean hasBattery;
    private String batteryMaker;
    private String batteryModel;
    private BigDecimal batteryCapacity;
    private Integer totalInvestment;
    private Boolean hasSubsidy;
    private String subsidyName;
    private BigDecimal subsidyAmount;
    private BigDecimal preProjectUnitPrice;

    private List<EnR002FileItemRequest> files;
}
