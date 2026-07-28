package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentUpdateRequest;

import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;

import java.util.List;

public interface EnR002EquipmentService {

    EnR002EquipmentResponse addEquipment(String siteId,
                                         String companyId,
                                         EnR002EquipmentRequest request);

    // List semua equipment milik satu site — untuk dashboard
    List<EnR002EquipmentResponse> getEquipmentsBySite(String siteId, String companyId);

    EnR002EquipmentResponse getEquipmentById(String siteId,
                                             Integer equipmentId,
                                             String companyId);

    EnR002EquipmentResponse updateEquipment(String siteId, Integer equipmentId,
                                            String companyId, EnR002EquipmentUpdateRequest request);
}
