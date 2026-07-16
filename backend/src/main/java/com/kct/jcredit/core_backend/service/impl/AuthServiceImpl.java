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
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatorCompanyRepository;
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
    private final OperatorCompanyRepository operatorCompanyRepository;
    private final ParticipatingCompanyRepository participatingCompanyRepository;

    
    @Override
    public LoginResponse userLogin(LoginRequest request) {
        LoginResponse response = new LoginResponse();

        // ユーザーの入力した企業IDが存在しているかどうか
         // Validate company ID
        ParticipatingCompany company = participatingCompanyRepository.findByCompanyId(request.getCompanyId()).orElse(null);
        if (company == null) {
            response.setStatusCode("INVALID_CREDENTIALS");
            response.setMessage(ResponseConstants.MSG_USER_LOGIN_FAILED);
            return response;
        }

        // 企業がサスペンドされているかどうか
        if (company.getCompanyStatus() != CompanyStatus.ACTIVE) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(company.getCompanyStatus() == CompanyStatus.SUSPENDED
                    ? ResponseConstants.MSG_COMPANY_ACCESS_SUSPENDED
                    : ResponseConstants.MSG_FORBIDDEN);
            return response;
        }

        ParticipatingUser user = userRepository
            .findByUsernameOrEmailAndCompanyId(
                request.getIdentifier(), request.getIdentifier(), company.getId()
            )
            .orElse(null);

        
        //userを、企業ID＆ユーザID/メールアドレスで検索して存在しない場合
        if(user == null) {
            response.setStatusCode("INVALID_CREDENTIALS");
            response.setMessage(ResponseConstants.MSG_USER_LOGIN_FAILED);
            return response;
        }

        //アカウントがロック中か確認し、ロック中なら拒否、期限切れなら解除
        //check if locked from attempts
        if(user.getAccountLockedUntil() != null){
            //現在時刻がロック期限より前は、ACCOUNT_LOCKEDを返して処理終了
            //if still locked
            if(LocalDateTime.now().isBefore(user.getAccountLockedUntil())) {
                response.setStatusCode(ResponseConstants.ACCOUNT_LOCKED);
                response.setMessage(ResponseConstants.MSG_ACCOUNT_LOCKED);
                return response;
            }
            //ロックが解除される
            else{
                //unlock account
                user.setAccountLockedUntil(null);
                user.setLoginAttempts(0);
                userRepository.save(user);
            }
        }

        //パスワード不一致の際のアカウントロック処理
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            Integer attempts = user.getLoginAttempts();
            //失敗回数が５を超えているなら
            if(attempts > 4) {
                //今から12時間後に設定してロックし保存
                user.setAccountLockedUntil(LocalDateTime.now().plusHours(12)); //lock for 12 hours
                //現在の失敗回数を取得
                userRepository.save(user);
                response.setStatusCode(ResponseConstants.ACCOUNT_LOCKED);
                response.setMessage(ResponseConstants.MSG_ACCOUNT_LOCKED);
                return response;
            }
            response.setStatusCode(ResponseConstants.INVALID_CREDENTIALS);
            response.setMessage(ResponseConstants.MSG_USER_LOGIN_FAILED);
            //attemptsが4になったらロック
            user.setLoginAttempts(attempts + 1);
            userRepository.save(user);
            return response;
        }
        //reset attempts on successful login
        user.setLoginAttempts(0);
        userRepository.save(user);

        //ユーザが有効でないならアクセス拒否する
        if(!user.getIsActive()) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }
            
        String token = jwtUtil.generateToken(user.getId(),user.getUsername(), company.getCompanyId(), user.getRole());

        //もしユーザの情報は必要なら、レスポンスにユーザの情報も入れる
        // UserInfoResponse resData = UserInfoResponse.builder()
        //         .id(String.valueOf(user.getId()))
        //         .username(user.getUsername())
        //         .name(user.getLastName() + " " + user.getFirstName())
        //         .firstName(user.getFirstName())
        //         .firstNameKana(user.getFirstNameKana())
        //         .lastName(user.getLastName())
        //         .lastNameKana(user.getLastNameKana())
        //         .phoneNumber(user.getPhoneNumber())
        //         .userPosition(user.getUserPosition())
        //         .role(user.getRole())
        //         .email(user.getEmail())
        //         .siteId(user.getSite() != null ? String.valueOf(user.getSite().getId()) : null)
        //         .siteName(user.getSite() != null ? user.getSite().getSiteName() : null)
        //         .companyId(request.getCompanyId())
        //         .build();
        //response.setUser(resData);
        response.setRole(user.getRole());
        response.setSiteId(user.getSite() != null ? user.getSite().getId() : null);
        response.setCompanyName(participatingCompanyRepository.findByCompanyId(request.getCompanyId()).map(ParticipatingCompany::getNameJa).orElse(null));
        response.setToken(token);
        response.setStatusCode(ResponseConstants.OK);

        return response;
    }

    @Override
    public AdminLoginResponse operatorLogin(AdminLoginRequest request) {
        // Find operator by companyId + identifier (email/username)
        AdminLoginResponse response = new AdminLoginResponse();

        OperatingUser operator = operatingUserRepository
            .findByEmailOrUsername(request.getIdentifier(), request.getIdentifier())
            .orElse(null);
        if(operator == null) {
            response.setStatusCode("INVALID_CREDENTIALS");
            response.setMessage(ResponseConstants.MSG_OPERATOR_LOGIN_FAILED);
            return response;
        }

        // Operator access is governed only by the operator account and operator company.
        if (!Boolean.TRUE.equals(operator.getIsActive())
                || operator.getCompany() == null
                || !Boolean.TRUE.equals(operator.getCompany().getIsActive())) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
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
            response.setMessage(ResponseConstants.MSG_OPERATOR_LOGIN_FAILED);
            operator.setLoginAttempts(attempts + 1);
            operatingUserRepository.save(operator);
            return response;
        }
        //reset attempts on successful login
        operator.setLoginAttempts(0);
        operatingUserRepository.save(operator); 
        
        // Generate JWT
        String token = jwtUtil.generateToken(operator.getId(), operator.getUsername(), operator.getCompany().getCompanyId(), operator.getRole());

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
        Integer targetUserId;

        try {
            targetUserId = parseTargetUserId(request);
        } catch (IllegalArgumentException ex) {
            return badRequestResponse();
        }

        // Case 1: 自分自身のパスワード変更
        if (targetUserId == null) {
            return updateOperatingUserPassword(currentUser, request, token, true);
        }
        
        // Case 2: 他のユーザーのパスワード変更
        // Participating User を探す
        ParticipatingUser targetParticipating = userRepository.findById(targetUserId).orElse(null);
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
        Integer targetUserId;

        try {
            targetUserId = parseTargetUserId(request);
        } catch (IllegalArgumentException ex) {
            return badRequestResponse();
        }
        
        // Case 1: 自分自身のパスワード変更
        if (targetUserId == null || targetUserId.equals(currentUser.getId())) {
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
        ParticipatingUser targetUser = userRepository.findById(targetUserId).orElse(null);
        if (targetUser == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }
        
      
        
        // 権限チェック: 同じ会社のみ
        if (!currentUser.getCompany().getCompanyId().equals(targetUser.getCompany().getCompanyId())) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }
        
        // パスワード変更実行
        return updateParticipatingUserPassword(targetUser, request,token, false);
    }

    private Integer parseTargetUserId(PasswordChangeRequest request) {
        Object rawUserId = request.getUserId();
        if (rawUserId == null) {
            return null;
        }

        if (rawUserId instanceof Number number) {
            int value = number.intValue();
            if (value <= 0) {
                throw new IllegalArgumentException("userId must be positive");
            }
            return value;
        }

        String value = rawUserId.toString().trim();
        if (value.isEmpty() || value.startsWith("COMP")) {
            return null;
        }
        if (!value.matches("\\d+")) {
            throw new IllegalArgumentException("userId must be numeric");
        }

        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("userId is out of range", ex);
        }
    }

    private PasswordChangeResponse badRequestResponse() {
        PasswordChangeResponse response = new PasswordChangeResponse();
        response.setStatusCode(ResponseConstants.BAD_REQUEST);
        response.setMessage(ResponseConstants.MSG_BAD_REQUEST);
        return response;
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
        
         ParticipatingCompany company = participatingCompanyRepository
            .findByCompanyId(req.getParticipatingCompanyId())
            .orElseThrow(() -> new IllegalArgumentException("Participating company not found"));

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
                .company(company)
                .build();

        System.out.println("DEBUG USER " + user);

        return userRepository.save(user);
    }

    @Override
    public OperatingUser createDummyOperatorUser(CreateDummyUserRequest req) {

        OperatorCompany company = operatorCompanyRepository
                .findByCompanyId(req.getParticipatingCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Operator company not found"));
            
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
                .company(company)
                .build();

        System.out.println("DEBUG OPE USER " + user);

        return operatingUserRepository.save(user);
    }

}
