package com.kct.jcredit.core_backend.dto.response;

import lombok.Data;
import java.util.List;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.UserInfoResponse;

@Data
public class UserListResponse extends BaseResponse {
    private List<UserInfoResponse> users;
}
