package com.kct.jcredit.core_backend.dto.response;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Builder;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class PendingReviewParticipationResponse extends BaseResponse {
    private Integer participationId;
    private String  projectName;
    private String  siteName;
    private String  companyName;
    private String  submittedAt;
}
