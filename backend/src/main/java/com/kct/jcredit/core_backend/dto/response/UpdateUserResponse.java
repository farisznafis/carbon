package com.kct.jcredit.core_backend.dto.response;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.SiteResponse;
import com.kct.jcredit.core_backend.dto.response.details.UserInfoResponse;
import lombok.Data; 

import java.util.List;
@Data
public class UpdateUserResponse extends BaseResponse {
    private UserInfoResponse user;
    private List<SiteResponse> updatedSites;
}