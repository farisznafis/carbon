package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.CreateOperatorUserRequest;
import com.kct.jcredit.core_backend.dto.request.DeleteUserSite;
import com.kct.jcredit.core_backend.dto.request.UpdateUserRequest;
import com.kct.jcredit.core_backend.dto.response.CreateUserResponse;
import com.kct.jcredit.core_backend.dto.response.DeleteUserSiteResponse;
import com.kct.jcredit.core_backend.dto.response.UpdateUserResponse;
import com.kct.jcredit.core_backend.dto.response.UserListResponse;
import com.kct.jcredit.core_backend.dto.response.UserProfileResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

public interface OperatorUserService {
    UserListResponse getOperatorUsersByCompany(String companyId);
    BaseResponse validateCreateOperatorUser(String companyId, String username, CreateOperatorUserRequest request);
    CreateUserResponse createOperatorUser(String companyId, String username, CreateOperatorUserRequest request);
    BaseResponse validateUpdateOperatorUser(String companyId, String username, UpdateUserRequest request);
    UpdateUserResponse updateUser(String companyId, String username, UpdateUserRequest request, String role, Integer currentId);
    UserProfileResponse getOperatorUserProfileById(Integer userId, Integer requestingId, String requestingRole, String requestingCompanyId);
}
