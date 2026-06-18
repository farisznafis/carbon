package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.EquipmentRegisterSolarRequest;
import com.kct.jcredit.core_backend.dto.response.EquipmentSolarRegisterResponse;
import com.kct.jcredit.core_backend.entity.SiteR002MemberData;
import com.kct.jcredit.core_backend.entity.SiteR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.dto.response.FileSolarUploadResponse;
import com.kct.jcredit.core_backend.dto.request.FilePreRegisSolarRequest;
import com.kct.jcredit.core_backend.dto.response.details.EquipmentItemSolarResponse;

public interface SiteProjectService {

    EquipmentSolarRegisterResponse registerEquipments(
            Integer siteId,
            Integer projectId,
                
            String company_id,
            EquipmentRegisterSolarRequest request
    );
}
