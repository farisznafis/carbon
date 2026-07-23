package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.enums.SecurityActionPurpose;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActionLinkExchangeResponse extends BaseResponse {
    private String actionSessionToken;
    private SecurityActionPurpose purpose;
    private Long expiresInSeconds;
}
