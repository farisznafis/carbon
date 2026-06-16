package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.CreateUserRequest;
import com.kct.jcredit.core_backend.dto.request.DeleteUserSite;
import com.kct.jcredit.core_backend.dto.request.UpdateUserRequest;
import com.kct.jcredit.core_backend.dto.response.CreateUserResponse;
import com.kct.jcredit.core_backend.dto.response.DeleteUserSiteResponse;
import com.kct.jcredit.core_backend.dto.response.UpdateUserResponse;
import com.kct.jcredit.core_backend.dto.response.UserListResponse;
import com.kct.jcredit.core_backend.dto.response.UserProfileResponse;

public interface UserService {
    UserProfileResponse getUserProfile(String username);
    UserListResponse getUsersByCompany(String companyId);
    DeleteUserSiteResponse removeUserSiteConnection(DeleteUserSite request);
    CreateUserResponse createUser(String companyId, String username, CreateUserRequest request);
    UpdateUserResponse updateUser(String companyId, String username, UpdateUserRequest request);
    void changePassword(String targetUsername, String newPassword);
    UserProfileResponse getUserProfileById(Integer userId, String requestingRole, String requestingCompanyId);
}
