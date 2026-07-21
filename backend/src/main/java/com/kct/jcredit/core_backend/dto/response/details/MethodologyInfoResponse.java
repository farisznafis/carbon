package com.kct.jcredit.core_backend.dto.response.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MethodologyInfoResponse {
    private String methodology;
    private Boolean hasSubsidy;
    private String subsidyName;
    private String remarks;
    private List<EquipmentItemSolarResponse> equipmentItems;
}
