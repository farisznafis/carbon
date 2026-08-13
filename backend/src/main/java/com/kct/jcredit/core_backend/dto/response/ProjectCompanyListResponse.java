package com.kct.jcredit.core_backend.dto.response;

import java.util.List;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class ProjectCompanyListResponse extends BaseResponse {
    private List<ProjectCompanyResponse> companies;
}
