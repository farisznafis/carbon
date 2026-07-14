package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.CreateSiteRequest;
import com.kct.jcredit.core_backend.dto.request.UpdateSiteRequest;
import com.kct.jcredit.core_backend.dto.response.CreateSiteResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentRegisterResponse;
import com.kct.jcredit.core_backend.dto.response.SiteInfoResponse;
import com.kct.jcredit.core_backend.dto.response.SiteListResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

public interface SiteService {
    SiteListResponse getSitesByCompanyId(String companyId);
    SiteInfoResponse getSiteById(int siteId);

    BaseResponse validateCreateSite(CreateSiteRequest request, String companyId);
    CreateSiteResponse createSite(CreateSiteRequest request, String companyId);
    BaseResponse validateUpdateSite(String siteId, String companyId, UpdateSiteRequest request);
    SiteInfoResponse updateSite(String siteId, String companyId, UpdateSiteRequest request);

    EnR002EquipmentRegisterResponse getSiteProjectEquipment(
            Integer siteId,
            Integer projectId,
            String companyId,
            String role,
            String username);
}
