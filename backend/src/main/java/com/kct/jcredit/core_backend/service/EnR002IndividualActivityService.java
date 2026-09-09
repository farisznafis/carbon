package com.kct.jcredit.core_backend.service;

import java.util.List;

import com.kct.jcredit.core_backend.dto.response.EnR002IndividualActivityDetailResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002IndividualActivityRowResponse;
import com.kct.jcredit.core_backend.dto.response.CompanyActivityGroupResponse;
import com.kct.jcredit.core_backend.service.EnR002IndividualActivityService;


public interface EnR002IndividualActivityService {
 
    /** Rows for one project, optionally narrowed to a single member company. */
    List<EnR002IndividualActivityRowResponse> getRowsByProject(
            String projectPublicId, String companyIdFilter, String currentUserId);
 
    /** Rows for one member company across every project the current user may access. */
    List<CompanyActivityGroupResponse> getRowsByCompany(
            String companyId, String projectPublicIdFilter, String currentUserId);
 
    /** Full detail of a single row, shared by both entry points above. */
    EnR002IndividualActivityDetailResponse getRowDetail(Integer equipmentId, String currentUserId);
}
 