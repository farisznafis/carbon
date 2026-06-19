package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.request.OtherOperatorProjectsRequest;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtherOperatorProjectsResponse extends BaseResponse {
    private OtherOperatorProjectsRequest data;
}
