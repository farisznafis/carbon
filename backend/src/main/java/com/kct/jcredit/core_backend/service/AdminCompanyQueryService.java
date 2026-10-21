package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.CompanyListQuery;
import com.kct.jcredit.core_backend.dto.response.AdminCompanyDetailResponse;
import com.kct.jcredit.core_backend.dto.response.AdminCompanyListResponse;

/**
 * Provides read-only administrator queries for participating companies.
 */
public interface AdminCompanyQueryService {

    /**
     * Returns one validated and deterministically sorted page of companies.
     */
    AdminCompanyListResponse findCompanies(CompanyListQuery query, String projectId);

    /**
     * Returns the operator-facing detail view for one participating company.
     */
    AdminCompanyDetailResponse findCompanyDetail(String companyId);
}
