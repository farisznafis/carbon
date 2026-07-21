package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.CompanySuspensionDetailResponse;
import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Wraps a paginated company suspension history.
 */
@Getter
@Setter
public class CompanySuspensionListResponse extends BaseResponse {
    private String companyId;
    private List<CompanySuspensionDetailResponse> suspensions;
    private PaginationDetailResponse pagination;
}
