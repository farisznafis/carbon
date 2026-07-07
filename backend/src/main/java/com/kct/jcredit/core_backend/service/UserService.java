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

public interface UserService {
    UserProfileResponse getUserProfile(String username);
    UserListResponse getUsersByCompany(String companyId);
    DeleteUserSiteResponse removeUserSiteConnection(DeleteUserSite request);
    BaseResponse validateCreateUser(String companyId, String username, CreateUserRequest request);
    CreateUserResponse createUser(String companyId, String username, CreateUserRequest request);
    BaseResponse validateUpdateUser(String companyId, String username, UpdateUserRequest request);
    UpdateUserResponse updateUser(String companyId, String username, UpdateUserRequest request);
    void changePassword(String targetUsername, String newPassword);
    UserProfileResponse getUserProfileById(Integer userId, String requestingRole, String requestingCompanyId);
}
