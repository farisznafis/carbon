package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * What the invitation screen shows before the invitee types anything: the address that was invited
 * and where the account will land. All read-only on the client.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserInvitationPrefillResponse extends BaseResponse {
    private String email;
    private String companyName;
    // Public id, matching what clients send when the invitation is created.
    private String siteId;
    private String siteName;
}
