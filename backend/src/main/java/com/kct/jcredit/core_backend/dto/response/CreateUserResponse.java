package com.kct.jcredit.core_backend.dto.response;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.SiteResponse;
import com.kct.jcredit.core_backend.dto.response.details.UserInfoResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserResponse extends BaseResponse {
    private UserInfoResponse user;
    private SiteResponse site;

}
