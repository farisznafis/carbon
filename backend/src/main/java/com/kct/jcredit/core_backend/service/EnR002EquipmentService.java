package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;

import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;

import java.util.List;

public interface EnR002EquipmentService {

    // Tambah equipment baru ke site yang sudah ada (di luar flow join)
    EnR002EquipmentResponse addEquipment(Integer siteId,
                                         String companyId,
                                         EnR002EquipmentRequest request);

    // List semua equipment milik satu site — untuk dashboard
    List<EnR002EquipmentResponse> getEquipmentsBySite(Integer siteId, String companyId);
}
