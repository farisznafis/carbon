package com.kct.jcredit.core_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import jakarta.persistence.Column;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnR002EquipmentResponse extends BaseResponse {

    private Integer id;
    private String siteId;

    private LocalDate equipmentOpDate;
    private BigDecimal nominalMaxOutput;
    private String pcsManufacturer;
    private String pcsModel;
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

    private Integer participationId;

    private LocalDate changeDate;
    private Integer previousEquipmentId;
    
    private List<EnR002EquipmentFileResponse> files;
}
