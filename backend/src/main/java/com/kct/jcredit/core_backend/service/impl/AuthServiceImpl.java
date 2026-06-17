package com.kct.jcredit.core_backend.service.impl;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.AdminLoginRequest;
import com.kct.jcredit.core_backend.dto.request.CreateDummyUserRequest;
import com.kct.jcredit.core_backend.dto.request.LoginRequest;
import com.kct.jcredit.core_backend.dto.request.PasswordChangeRequest;
import com.kct.jcredit.core_backend.dto.response.AdminLoginResponse;
import com.kct.jcredit.core_backend.dto.response.LoginResponse;
import com.kct.jcredit.core_backend.dto.response.PasswordChangeResponse;
import com.kct.jcredit.core_backend.dto.response.details.AdminResponse;
import com.kct.jcredit.core_backend.dto.response.details.UserInfoResponse;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.AuthService;
import com.kct.jcredit.core_backend.service.TokenBlacklistService;
import com.kct.jcredit.core_backend.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;
    private final PasswordEncoder passwordEncoder;

    private final ParticipatingUserRepository userRepository;
    private final SiteRepository siteRepository;
    private final OperatingUserRepository operatingUserRepository;
    private final ParticipatingCompanyRepository participatingCompanyRepository;

   gi

    @Override
    public AdminLoginResponse operatorLogin(AdminLoginRequest request) {
        // Find operator by companyId + identifier (email/username)
        AdminLoginResponse response = new AdminLoginResponse();

        OperatingUser operator = operatingUserRepository
            .findByEmailOrUsername(request.getIdentifier(), request.getIdentifier())
            .orElse(null);
        if(operator == null) {
            response.setStatusCode("INVALID_CREDENTIALS");
            response.setMessage(ResponseConstants.MSG_LOGIN_FAILED);
            return response;
        }


        //check if locked from attempts
        if(operator.getAccountLockedUntil() != null){
            //if still locked
            if(LocalDateTime.now().isBefore(operator.getAccountLockedUntil())) {
                response.setStatusCode(ResponseConstants.ACCOUNT_LOCKED);
                response.setMessage(ResponseConstants.MSG_ACCOUNT_LOCKED);
                return response;
            }else{
                //unlock account
                operator.setAccountLockedUntil(null);
                operator.setLoginAttempts(0);
                operatingUserRepository.save(operator);
            }
        }
        if (!passwordEncoder.matches(request.getPassword(), operator.getPassword())) {
            Integer attempts = operator.getLoginAttempts();
            if(attempts > 4) {
                operator.setAccountLockedUntil(LocalDateTime.now().plusHours(12)); //lock for 12 hours
                operatingUserRepository.save(operator);
                response.setStatusCode(ResponseConstants.ACCOUNT_LOCKED);
                response.setMessage(ResponseConstants.MSG_ACCOUNT_LOCKED);
                return response;
            }
            response.setStatusCode(ResponseConstants.INVALID_CREDENTIALS);
            response.setMessage(ResponseConstants.MSG_LOGIN_FAILED);
            operator.setLoginAttempts(attempts + 1);
            operatingUserRepository.save(operator);
            return response;
        }
        //reset attempts on successful login
        operator.setLoginAttempts(0);
        operatingUserRepository.save(operator); 
        
        // Generate JWT
        String token = jwtUtil.generateToken(operator.getId(), operator.getUsername(), operator.getCompanyId(), operator.getRole());

        // Build response
        AdminResponse admin = AdminResponse.builder()
                .username(operator.getUsername())
                .name(operator.getLastName() + " " + operator.getFirstName()) // full name
                .role(operator.getRole())
                .email(operator.getEmail())
                .firstName(operator.getFirstName()) // from your previous request
                .lastName(operator.getLastName())
                .phoneNumber(operator.getPhoneNumber())
                .build();
        
        response.setToken(token);
        response.setAdmin(admin);
        response.setStatusCode(ResponseConstants.OK);


        return response;
    }

    
    @Override
    public PasswordChangeResponse changePassword(String username, String token, PasswordChangeRequest request) {
        //invalidate toke of the changed password account
        // 1. 現在ログイン中のユーザーを特定
        PasswordChangeResponse response = new PasswordChangeResponse();
        ParticipatingUser currentParticipating = userRepository.findByUsername(username).orElse(null);
        OperatingUser currentOperating = operatingUserRepository.findByUsername(username).orElse(null);
        
        if (currentParticipating == null && currentOperating == null) {
            //throw new Exception("想定外エラーが発せしました。");
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }
        
        // 2. ユーザータイプに応じて処理を分岐
        //what if operating and participating hold the same id
        if (currentOperating != null) {
            return changePasswordForOperatingUser(currentOperating, request, token);
        } else {
            return changePasswordForParticipatingUser(currentParticipating, request, token);
        }
    }

     /**
     * Operating User のパスワード変更処理
     * - 自分自身のパスワード変更可能
     * - 同じ会社のParticipating Master のパスワード変更可能
     */
    private PasswordChangeResponse changePasswordForOperatingUser(
            OperatingUser currentUser, 
            PasswordChangeRequest request, String token) {
        PasswordChangeResponse response = new PasswordChangeResponse();

        // Case 1: 自分自身のパスワード変更
        if (request.getUserId() == null) {
            return updateOperatingUserPassword(currentUser, request, token, true);
        }
        
        // Case 2: 他のユーザーのパスワード変更
        // Participating User を探す
        ParticipatingUser targetParticipating = userRepository.findById(request.getUserId()).orElse(null);
        if (targetParticipating == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }
        
        ParticipatingUser targetUser = targetParticipating;
        
        // 権限チェック: Master ユーザのみ変更可能
        if (!"master".equals(targetUser.getRole())) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }
        
        // パスワード変更実行
        return updateParticipatingUserPassword(targetUser, request, token, false);
    }

     /**
     * Participating User のパスワード変更処理
     * - 自分自身のパスワード変更可能
     * - Master の場合: 同じ会社の全Participating User のパスワード変更可能
     * - User の場合: 自分以外は変更不可
     */
    private PasswordChangeResponse changePasswordForParticipatingUser(
            ParticipatingUser currentUser, 
            PasswordChangeRequest request, String token) {

        PasswordChangeResponse response = new PasswordChangeResponse();
        
        // Case 1: 自分自身のパスワード変更
        if (request.getUserId() == null || request.getUserId().equals(currentUser.getId())) {
            return updateParticipatingUserPassword(currentUser, request,token, true);
        }
        
        // Case 2: 他のユーザーのパスワード変更
        // 一般ユーザー（user）は自分以外変更不可
        if ("user".equals(currentUser.getRole())) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }
        
        // Master ユーザーではない場合実行
        if (!"master".equals(currentUser.getRole())) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }
        
        // 対象ユーザーを取得（Participating User のみ）
        ParticipatingUser targetUser = userRepository.findById(request.getUserId()).orElse(null);
        if (targetUser == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }
        
      
        
        // 権限チェック: 同じ会社のみ
        if (!currentUser.getCompanyId().equals(targetUser.getCompanyId())) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }
        
        // パスワード変更実行
        return updateParticipatingUserPassword(targetUser, request,token, false);
    }

     private PasswordChangeResponse updateOperatingUserPassword(
            OperatingUser targetUser, 
            PasswordChangeRequest request, String token, boolean isSelfChange) {
        
        PasswordChangeResponse response = new PasswordChangeResponse();
        // 1. 現在のパスワード検証
        if (!passwordEncoder.matches(request.getCurrentPassword(), targetUser.getPassword())) {
            response.setStatusCode(ResponseConstants.INVALID_CURRENT_PASSWORD);
            response.setMessage(ResponseConstants.MSG_INVALID_CURRENT_PASSWORD);
            return response;
        }
        
        // 2. 新旧パスワード同一チェック
        if (passwordEncoder.matches(request.getNewPassword(), targetUser.getPassword())) {
            response.setStatusCode(ResponseConstants.SAME_PASSWORD);
            response.setMessage(ResponseConstants.MSG_SAME_PASSWORD);
            return response;
        }
        
        // 3. パスワード更新
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        targetUser.setPassword(encodedPassword);
        operatingUserRepository.save(targetUser);
        
       // 4. Refresh cache so old tokens are rejected by lazy validation
        blacklistService.recordPasswordChange(targetUser.getUsername());
        

        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("パスワードが正常に変更されました。");
        return response;
    }


    /**
     * Participating User のパスワード更新
     */
    private PasswordChangeResponse updateParticipatingUserPassword(
            ParticipatingUser targetUser, 
            PasswordChangeRequest request, String token, boolean isSelfChange) {

        PasswordChangeResponse response = new PasswordChangeResponse();
        
        // 1. 現在のパスワード検証
        if (!passwordEncoder.matches(request.getCurrentPassword(), targetUser.getPassword())) {
            response.setStatusCode(ResponseConstants.INVALID_CURRENT_PASSWORD);
            response.setMessage(ResponseConstants.MSG_INVALID_CURRENT_PASSWORD);
            return response;
        }
        
        // 2. 新旧パスワード同一チェック
        if (passwordEncoder.matches(request.getNewPassword(), targetUser.getPassword())) {
            response.setStatusCode(ResponseConstants.SAME_PASSWORD);
            response.setMessage(ResponseConstants.MSG_SAME_PASSWORD);
            return response;
        }
        
        // 3. パスワード更新
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        targetUser.setPassword(encodedPassword);
        userRepository.save(targetUser);
        
       // 4. Refresh cache so old tokens are rejected by lazy validation
        blacklistService.recordPasswordChange(targetUser.getUsername());
        
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("パスワードが正常に変更されました。");
        return response;
    }

    /**
     * ユーザーの全トークンをブラックリスト化
     * （パスワード変更されたユーザーは再ログインが必要）
     */
    //warning does not blacklist if changing other users password

    /*implimentation idea: on the server keep a list of all the active tokens for each user,
     blacklist needed and periodically clean up expired tokens from the list
    */
 
    private void blacklistUserTokens(String token) {


        try {
            LocalDateTime expiresAt = jwtUtil.extractExpirationAsLocalDateTime(token);
            blacklistService.blacklistToken(token, expiresAt);
            System.out.println("Token blacklisted for requesting user");
        } catch (Exception e) {
            System.err.println("Failed to blacklist token: " + e.getMessage());
        }
    }

    @Override
    public ParticipatingUser createDummyUser(CreateDummyUserRequest req) {
        Site site = siteRepository.findById(req.getSiteId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

        ParticipatingUser user = ParticipatingUser.builder()
                .username(req.getUsername())
                .firstName(req.getFirstName())
                .firstNameKana(req.getFirstNameKana())
                .lastName(req.getLastName())
                .lastNameKana(req.getLastNameKana())
                .email(req.getEmail())
                .phoneNumber(req.getPhoneNumber())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .site(site)
                .companyId(req.getParticipatingCompanyId())
                .build();

        System.out.println("DEBUG USER " + user);

        return userRepository.save(user);
    }

    @Override
    public OperatingUser createDummyOperatorUser(CreateDummyUserRequest req) {
        OperatingUser user = OperatingUser.builder()
                .username(req.getUsername())
                .firstName(req.getFirstName())
                .firstNameKana(req.getFirstNameKana())
                .lastName(req.getLastName())
                .lastNameKana(req.getLastNameKana())
                .email(req.getEmail())
                .phoneNumber(req.getPhoneNumber())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .companyId(req.getParticipatingCompanyId())
                .build();

        System.out.println("DEBUG OPE USER " + user);

        return operatingUserRepository.save(user);
    }

}