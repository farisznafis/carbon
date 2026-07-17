package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class OtpChallengeResponse extends BaseResponse {
    private UUID challengeId;
    private String maskedEmail;
    private Long expiresInSeconds;
}
