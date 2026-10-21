package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.UserInvitationCompleteRequest;
import com.kct.jcredit.core_backend.dto.response.UserInvitationPrefillResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

public interface UserInvitationService {

    BaseResponse invite(String companyId, String actor, String email, String sitePublicId);

    UserInvitationPrefillResponse getPrefill(String sessionToken);

    BaseResponse complete(String sessionToken, UserInvitationCompleteRequest request);
}