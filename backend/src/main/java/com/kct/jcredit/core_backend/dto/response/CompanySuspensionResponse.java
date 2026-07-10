package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.CompanySuspensionDetailResponse;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * Returns the company state after a suspension is created or resolved.
 */
@Getter
@Setter
public class CompanySuspensionResponse extends BaseResponse {
    private String companyId;
    private CompanyStatus companyStatus;
    private CompanySuspensionDetailResponse suspension;
    private long activeSuspensionCount;
}
