package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.CompanyInfoResponse;
import lombok.Data;

import java.util.List;

@Data
public class PendingCompanyListResponse extends BaseResponse {
    private List<CompanyInfoResponse> companies;
}
