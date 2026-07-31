package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.CreateUserRequest;
import com.kct.jcredit.core_backend.dto.request.DeleteUserSite;
import com.kct.jcredit.core_backend.dto.request.UpdateUserRequest;
import com.kct.jcredit.core_backend.dto.response.CreateUserResponse;
import com.kct.jcredit.core_backend.dto.response.DeleteUserSiteResponse;
import com.kct.jcredit.core_backend.dto.response.UpdateUserResponse;
import com.kct.jcredit.core_backend.dto.response.UserListResponse;
import com.kct.jcredit.core_backend.dto.response.UserProfileResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

public interface OperatorUserService {
    UserProfileResponse getOperatorUserProfile(String username);
    UserListResponse getOperatorUsersByCompany(String companyId);
    BaseResponse validateCreateOperatorUser(String companyId, String username, CreateUserRequest request);
    CreateUserResponse createOperatorUser(String companyId, String username, CreateUserRequest request);
    BaseResponse validateUpdateOperatorUser(String companyId, String username, UpdateUserRequest request);
    UpdateUserResponse updateUser(String companyId, String username, UpdateUserRequest request);
    void changePassword(String targetUsername, String newPassword);
    UserProfileResponse getOperatorUserProfileById(Integer userId, String requestingRole, String requestingCompanyId);
}
