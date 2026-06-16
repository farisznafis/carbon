package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.UpdatePartiCompanyRequest;
import com.kct.jcredit.core_backend.dto.response.UpdatePartiCompanyResponse;

public interface PartiCompanyService {
    UpdatePartiCompanyResponse updateCompany(String companyId, UpdatePartiCompanyRequest request);
}
