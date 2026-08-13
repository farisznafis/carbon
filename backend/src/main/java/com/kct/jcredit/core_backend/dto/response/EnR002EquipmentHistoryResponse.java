package com.kct.jcredit.core_backend.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter 
@Builder
public class EnR002EquipmentHistoryResponse {
    private Integer id;
    private LocalDate changeDate;
    private Integer previousEquipmentId;

    private LocalDate monitoringStartDate;
    private LocalDate monitoringEndDate;
    private Boolean isActive;

    private String registeredBy;
    private LocalDateTime registeredAt;
    private EnR002EquipmentResponse equipment;
}