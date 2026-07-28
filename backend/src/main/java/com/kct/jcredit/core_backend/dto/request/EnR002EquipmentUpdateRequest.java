package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Getter 
@Setter
public class EnR002EquipmentUpdateRequest {

    private LocalDate equipmentOpDate;
    private BigDecimal nominalMaxOutput;
    private String pcsManufacturer;
    private String pcsModel;
    private Integer pcsUnitCount;
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

    // field s3?
}
