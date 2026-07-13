package com.kct.jcredit.core_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import java.util.List;
import lombok.Builder;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentSolarRegisterResponse extends BaseResponse {
 
    // Mapping tempId → equipmentId permanent
    private List<EnR002EquipmentResponse> equipments;
}

