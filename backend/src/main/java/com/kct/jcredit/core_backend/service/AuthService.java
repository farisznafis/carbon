package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.CreateDummyUserRequest;
import com.kct.jcredit.core_backend.dto.request.LoginRequest;
import com.kct.jcredit.core_backend.dto.request.PasswordChangeRequest;
import com.kct.jcredit.core_backend.dto.response.AdminLoginResponse;
import com.kct.jcredit.core_backend.dto.request.AdminLoginRequest;
import com.kct.jcredit.core_backend.dto.response.LoginResponse;
import com.kct.jcredit.core_backend.dto.response.PasswordChangeResponse;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;

public interface AuthService {
    LoginResponse userLogin(LoginRequest request);
    AdminLoginResponse operatorLogin(AdminLoginRequest request);
    PasswordChangeResponse changePassword(String username, String token, PasswordChangeRequest request);
    ParticipatingUser createDummyUser(CreateDummyUserRequest req);
    OperatingUser createDummyOperatorUser(CreateDummyUserRequest req);
}
