package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentUpdateRequest;

import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;

import java.util.List;

public interface EnR002EquipmentService {

    EnR002EquipmentResponse addEquipment(Integer siteId,
                                         String companyId,
                                         EnR002EquipmentRequest request);

    List<EnR002EquipmentResponse> getEquipmentsBySite(Integer siteId, String companyId);

    EnR002EquipmentResponse getEquipmentById(Integer siteId,
                                             Integer equipmentId,
                                             String companyId);

    EnR002EquipmentResponse updateEquipment(Integer siteId, Integer equipmentId,
                                            String companyId, EnR002EquipmentUpdateRequest request);
}
