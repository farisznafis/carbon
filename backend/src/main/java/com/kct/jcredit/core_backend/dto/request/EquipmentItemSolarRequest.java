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
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentItemSolarRequest {
 
    @NotBlank
    private String tempId;
 
    @NotBlank  private String methodologyNumber;
    @NotNull   private Boolean isEligible;
 
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate equipmentOpDate;
 
    @NotNull   @DecimalMin("0.0") private BigDecimal nominalMaxOutput;
    @NotBlank  private String pcsManufacturer;
    @NotBlank  private String pcsModel;
    @NotNull   @Min(0) private Integer powerCondUnitCount;
    @NotBlank  private String deviceNo;
    @NotNull   @DecimalMin("0.0") private BigDecimal powerCondStandby;
 
    //hasBattery = true
    @NotNull   private Boolean hasBattery;
    private String batteryMaker;
    private String batteryModel;
    private BigDecimal batteryCapacity;
 
    //additionalEquipment = true
    @NotNull   private Boolean additionalEquipment;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate additionalEquipmentDate;
    private String additionalEquipmentType;
    private Integer additionalUnitCount;
    private String additionalDeviceUniqueNo;
 
    private Integer totalInvestment;
 
    @NotNull   private Boolean hasSubsidy;
 
    // hasSubsidy = true
    private String subsidyName;
    private BigDecimal subsidyAmount;
 
    @NotNull   @DecimalMin("0.0") private BigDecimal preProjectUnitPrice;
    @NotNull   @DecimalMin("0.0") private BigDecimal ictEquipmentPowerConsumption;
 
    @NotEmpty  @Valid
    private List<FileItemSolarRequest> files;
}