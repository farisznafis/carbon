package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.AdminCompanyListItemResponse;
import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Wraps a filtered company page and its pagination metadata.
 */
@Getter
@Setter
public class AdminCompanyListResponse extends BaseResponse {
    private List<AdminCompanyListItemResponse> companies;
    private PaginationDetailResponse pagination;
}
