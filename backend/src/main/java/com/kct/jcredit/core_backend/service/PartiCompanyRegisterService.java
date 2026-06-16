package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.PartiCompanyRegisterRequest;
import com.kct.jcredit.core_backend.dto.response.details.PartiCompanyResponseDetails;

public interface PartiCompanyRegisterService {
    PartiCompanyResponseDetails register(PartiCompanyRegisterRequest request);
}