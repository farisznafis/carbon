package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.EquipmentRegisterSolarRequest;
import com.kct.jcredit.core_backend.dto.response.EquipmentSolarRegisterResponse;
import com.kct.jcredit.core_backend.entity.SiteR002MemberData;
import com.kct.jcredit.core_backend.entity.SiteR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.dto.response.FileSolarUploadResponse;
import com.kct.jcredit.core_backend.dto.request.FilePreRegisSolarRequest;
import com.kct.jcredit.core_backend.dto.response.details.EquipmentItemSolarResponse;

public interface EquipmentSolarService {

     /**
     * Registrasi equipment beserta TnC ke site yang sudah terdaftar dalam project.
     * SQL (equipment + files) dan DynamoDB (TnC) di-insert dalam satu operasi.
     *
     * @param siteId    ID site dari path variable
     * @param projectId ID project dari path variable
     * @param request   Payload lengkap termasuk semua equipment dan TnC
     * @return Response berisi participationId dan mapping tempId → equipmentId permanen
     */
    EquipmentSolarRegisterResponse registerEquipments(
            Integer siteId,
            Integer projectId,
                
            String company_id,
            EquipmentRegisterSolarRequest request
    );
}
