package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentChangeRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentChangeRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentFileResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentHistoryListResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentListResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentHistoryResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;

import java.util.List;

public interface EnR002EquipmentService {

    EnR002EquipmentResponse addEquipment(String siteId,
                                          String companyId,
                                          String projectId,
                                          EnR002EquipmentRequest request);

    // 変更点: List<EnR002EquipmentResponse> → EnR002EquipmentListResponse
    // statusCodeを載せるためのラッパー型に変更（SiteListResponseと同じ考え方）
    EnR002EquipmentListResponse getEquipmentsBySite(String siteId, String companyId);

    EnR002EquipmentResponse getEquipmentById(String siteId,
                                              Integer equipmentId,
                                              String companyId);

    EnR002EquipmentResponse changeEquipment(String siteId,
                                             Integer equipmentId,
                                             String companyId,
                                             EnR002EquipmentChangeRequest request);

    // 変更点: List<EnR002EquipmentHistoryResponse> → EnR002EquipmentHistoryListResponse
    EnR002EquipmentHistoryListResponse getEquipmentHistory(String siteId,
                                                            Integer equipmentId,
                                                            String companyId);
}