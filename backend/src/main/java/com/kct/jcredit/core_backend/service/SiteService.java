package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.CreateSiteRequest;
import com.kct.jcredit.core_backend.dto.request.UpdateSiteRequest;
import com.kct.jcredit.core_backend.dto.response.CreateSiteResponse;
import com.kct.jcredit.core_backend.dto.response.EquipmentSolarRegisterResponse;
import com.kct.jcredit.core_backend.dto.response.SiteInfoResponse;
import com.kct.jcredit.core_backend.dto.response.SiteListResponse;

public interface SiteService {
    SiteListResponse getSitesByCompanyId(String companyId);
    SiteInfoResponse getSiteById(int siteId);

    CreateSiteResponse createSite(CreateSiteRequest request, String companyId);
    SiteInfoResponse updateSite(String siteId, String companyId, UpdateSiteRequest request);

    EquipmentSolarRegisterResponse getSiteProjectEquipment(
            Integer siteId,
            Integer projectId,
            String companyId,
            String role,
            String username);
}
