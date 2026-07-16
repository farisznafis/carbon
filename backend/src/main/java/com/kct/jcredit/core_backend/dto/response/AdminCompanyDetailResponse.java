package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.AdminCompanyDetailCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.details.AdminCompanyDetailMasterResponse;
import com.kct.jcredit.core_backend.dto.response.details.AdminCompanyDetailSiteResponse;
import com.kct.jcredit.core_backend.dto.response.details.AdminCompanyDetailUserResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AdminCompanyDetailResponse extends BaseResponse {
    private AdminCompanyDetailMasterResponse masterAccount;
    private AdminCompanyDetailCompanyResponse company;
    private List<AdminCompanyDetailUserResponse> users;
    private List<AdminCompanyDetailSiteResponse> sites;
}
