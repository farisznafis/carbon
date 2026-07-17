package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.config.SecurityFlowProperties;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.ForgotPasswordRequest;
import com.kct.jcredit.core_backend.dto.response.ActionLinkExchangeResponse;
import com.kct.jcredit.core_backend.dto.response.AdminLoginResponse;
import com.kct.jcredit.core_backend.dto.response.LoginResponse;
import com.kct.jcredit.core_backend.dto.response.OtpChallengeResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.AdminResponse;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.enums.OtpPurpose;
import com.kct.jcredit.core_backend.enums.SecurityActionPurpose;
import com.kct.jcredit.core_backend.enums.SecurityActionSubjectType;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Locale;
import java.util.UUID;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAccountService {
    private final SecurityFlowProperties properties;
    private final SecurityActionLinkService actionLinkService;
    private final OtpChallengeService otpChallengeService;
    private final NotificationService notificationService;
    private final ParticipatingCompanyRepository companyRepository;
    private final ParticipatingUserRepository participatingUserRepository;
    private final OperatingUserRepository operatingUserRepository;
    private final UsernameAvailabilityService usernameAvailabilityService;
    private final CompanyLifecycleService companyLifecycleService;
    private final PasswordEncoder passwordEncoder;
    private final AuthVersionService authVersionService;
    private final JwtUtil jwtUtil;
    private final SecurityRateLimitService rateLimitService;
    private final SecurityEmailDeliveryService securityEmailDeliveryService;

    public ActionLinkExchangeResponse exchangeActionLink(String rawToken) {
        SecurityActionLinkService.ActionSession session = actionLinkService.exchange(rawToken);
        ActionLinkExchangeResponse response = new ActionLinkExchangeResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("Action link opened.");
        response.setActionSessionToken(session.sessionToken());
        response.setPurpose(session.purpose());
        response.setExpiresInSeconds(session.expiresInSeconds());
        return response;
    }

    @Transactional
    public BaseResponse setupMasterAccount(String sessionToken, String username, String password) {
        requireFeature(properties.getFeatures().isMasterSetupLink());
        SecurityActionLinkService.ActionSessionData session = actionLinkService.requireSession(
                sessionToken, SecurityActionPurpose.MASTER_ACCOUNT_SETUP);
        ParticipatingCompany company = companyRepository.findByIdForUpdate(session.subjectId())
                .orElseThrow(this::invalidToken);
        if (company.getCompanyStatus() != CompanyStatus.APPROVED_PENDING_SETUP
                || participatingUserRepository.findByCompany_IdAndRole(company.getId(), "master").isPresent()) {
            throw invalidToken();
        }
        String availableUsername = usernameAvailabilityService.lockAndRequireAvailable(username);
        ParticipatingUser master = ParticipatingUser.builder()
                .username(availableUsername)
                .firstName(company.getRepFirstName())
                .firstNameKana(company.getRepFirstNameKana())
                .lastName(company.getRepLastName())
                .lastNameKana(company.getRepLastNameKana())
                .email(company.getEmail())
                .phoneNumber(company.getRepPhoneNumber())
                .password(passwordEncoder.encode(password))
                .role("master")
                .userPosition(company.getRepPosition())
                .loginAttempts(0)
                .accountLockedUntil(null)
                .emailOtpLoginEnabled(true)
                .authVersion(0)
                .site(null)
                .company(company)
                .build();
        participatingUserRepository.save(master);
        companyLifecycleService.activateApprovedCompany(company);
        actionLinkService.complete(sessionToken, SecurityActionPurpose.MASTER_ACCOUNT_SETUP);
        return BaseResponse.success("Master account created. Company is active.");
    }

    @Transactional
    public BaseResponse requestPasswordReset(ForgotPasswordRequest request) {
        requireFeature(properties.getFeatures().isPasswordReset());
        rateLimitService.require("password-reset-account",
                request.getAccountType() + ":" + request.getCompanyId() + ":"
                        + request.getIdentifier().trim().toLowerCase(Locale.ROOT),
                3, Duration.ofHours(1));
        if ("OPERATING".equals(request.getAccountType())) {
            operatingUserRepository.findByEmailOrUsername(request.getIdentifier(), request.getIdentifier())
                    .ifPresent(user -> issuePasswordReset(
                            SecurityActionSubjectType.OPERATING_USER, user.getId(), user.getEmail(), "self-service"));
        } else if (request.getCompanyId() != null && !request.getCompanyId().isBlank()) {
            companyRepository.findByCompanyId(request.getCompanyId()).ifPresent(company ->
                    participatingUserRepository.findByUsernameOrEmailAndCompanyId(
                                    request.getIdentifier(), request.getIdentifier(), company.getId())
                            .ifPresent(user -> issuePasswordReset(
                                    SecurityActionSubjectType.PARTICIPATING_USER,
                                    user.getId(), user.getEmail(), "self-service")));
        }
        return BaseResponse.success("If the account exists, a password reset email will be sent.");
    }

    @Transactional
    public BaseResponse operatorRequestPasswordReset(String accountType, Integer accountId, String actor) {
        requireFeature(properties.getFeatures().isPasswordReset());
        if ("OPERATING".equalsIgnoreCase(accountType)) {
            OperatingUser user = operatingUserRepository.findById(accountId)
                    .orElseThrow(() -> new CustomException(ResponseConstants.NOT_FOUND, ResponseConstants.MSG_NOT_FOUND));
            issuePasswordReset(SecurityActionSubjectType.OPERATING_USER, user.getId(), user.getEmail(), actor);
        } else if ("PARTICIPATING".equalsIgnoreCase(accountType)) {
            ParticipatingUser user = participatingUserRepository.findById(accountId)
                    .orElseThrow(() -> new CustomException(ResponseConstants.NOT_FOUND, ResponseConstants.MSG_NOT_FOUND));
            issuePasswordReset(SecurityActionSubjectType.PARTICIPATING_USER, user.getId(), user.getEmail(), actor);
        } else {
            throw new CustomException(ResponseConstants.BAD_REQUEST, ResponseConstants.MSG_BAD_REQUEST);
        }
        return BaseResponse.success("Password reset link queued.");
    }

    @Transactional
    public BaseResponse completePasswordReset(String sessionToken, String newPassword) {
        requireFeature(properties.getFeatures().isPasswordReset());
        SecurityActionLinkService.ActionSessionData session = actionLinkService.requireSession(
                sessionToken, SecurityActionPurpose.PASSWORD_RESET);
        if (session.subjectType() == SecurityActionSubjectType.PARTICIPATING_USER) {
            ParticipatingUser user = participatingUserRepository.findById(session.subjectId())
                    .orElseThrow(this::invalidToken);
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                throw new CustomException(ResponseConstants.SAME_PASSWORD, ResponseConstants.MSG_SAME_PASSWORD);
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setLoginAttempts(0);
            user.setAccountLockedUntil(null);
            authVersionService.increment(user);
        } else if (session.subjectType() == SecurityActionSubjectType.OPERATING_USER) {
            OperatingUser user = operatingUserRepository.findById(session.subjectId())
                    .orElseThrow(this::invalidToken);
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                throw new CustomException(ResponseConstants.SAME_PASSWORD, ResponseConstants.MSG_SAME_PASSWORD);
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setLoginAttempts(0);
            user.setAccountLockedUntil(null);
            authVersionService.increment(user);
        } else {
            throw invalidToken();
        }
        actionLinkService.complete(sessionToken, SecurityActionPurpose.PASSWORD_RESET);
        return BaseResponse.success("Password reset completed. Please sign in again.");
    }

    public boolean requiresParticipatingOtp(ParticipatingUser user) {
        return properties.getFeatures().isLoginEmailOtp()
                && ("master".equals(user.getRole()) || Boolean.TRUE.equals(user.getEmailOtpLoginEnabled()));
    }

    public boolean requiresOperatingOtp() {
        return properties.getFeatures().isLoginEmailOtp();
    }

    public LoginResponse beginParticipatingLogin(ParticipatingUser user, ParticipatingCompany company) {
        OtpChallengeService.IssuedOtp issued = otpChallengeService.create(
                OtpPurpose.PARTICIPATING_LOGIN,
                "P:" + user.getId() + ":" + safeVersion(user.getAuthVersion()),
                user.getEmail());
        notificationService.sendOtpEmail(issued.email(), issued.code(), properties.getOtpTtl().toMinutes());
        LoginResponse response = new LoginResponse();
        response.setStatusCode(ResponseConstants.OTP_REQUIRED);
        response.setMessage(ResponseConstants.MSG_OTP_REQUIRED);
        response.setRole(user.getRole());
        response.setCompanyName(company.getNameJa());
        response.setChallengeId(issued.challengeId());
        response.setMaskedEmail(issued.maskedEmail());
        response.setExpiresInSeconds(issued.expiresInSeconds());
        return response;
    }

    public AdminLoginResponse beginOperatingLogin(OperatingUser user) {
        OtpChallengeService.IssuedOtp issued = otpChallengeService.create(
                OtpPurpose.OPERATING_LOGIN,
                "O:" + user.getId() + ":" + safeVersion(user.getAuthVersion()),
                user.getEmail());
        notificationService.sendOtpEmail(issued.email(), issued.code(), properties.getOtpTtl().toMinutes());
        AdminLoginResponse response = new AdminLoginResponse();
        response.setStatusCode(ResponseConstants.OTP_REQUIRED);
        response.setMessage(ResponseConstants.MSG_OTP_REQUIRED);
        response.setChallengeId(issued.challengeId());
        response.setMaskedEmail(issued.maskedEmail());
        response.setExpiresInSeconds(issued.expiresInSeconds());
        return response;
    }

    public LoginResponse verifyParticipatingLogin(UUID challengeId, String code) {
        OtpChallengeService.OtpData challenge = otpChallengeService.verify(challengeId, code);
        LoginSubject subject = parseSubject(challenge, "P", OtpPurpose.PARTICIPATING_LOGIN);
        ParticipatingUser user = participatingUserRepository.findById(subject.userId()).orElseThrow(this::invalidToken);
        ParticipatingCompany company = user.getCompany();
        if (subject.authVersion() != safeVersion(user.getAuthVersion())
                || !Boolean.TRUE.equals(user.getIsActive())
                || company.getCompanyStatus() != CompanyStatus.ACTIVE) {
            throw invalidToken();
        }
        otpChallengeService.consumeVerified(challengeId, OtpPurpose.PARTICIPATING_LOGIN);
        LoginResponse response = new LoginResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setRole(user.getRole());
        response.setSiteId(user.getSite() == null ? null : user.getSite().getId());
        response.setCompanyName(company.getNameJa());
        response.setToken(jwtUtil.generateToken(user.getId(), user.getUsername(), company.getCompanyId(),
                user.getRole(), user.getAuthVersion()));
        return response;
    }

    public AdminLoginResponse verifyOperatingLogin(UUID challengeId, String code) {
        OtpChallengeService.OtpData challenge = otpChallengeService.verify(challengeId, code);
        LoginSubject subject = parseSubject(challenge, "O", OtpPurpose.OPERATING_LOGIN);
        OperatingUser user = operatingUserRepository.findById(subject.userId()).orElseThrow(this::invalidToken);
        if (subject.authVersion() != safeVersion(user.getAuthVersion())
                || !Boolean.TRUE.equals(user.getIsActive())
                || user.getCompany() == null || !Boolean.TRUE.equals(user.getCompany().getIsActive())) {
            throw invalidToken();
        }
        otpChallengeService.consumeVerified(challengeId, OtpPurpose.OPERATING_LOGIN);
        AdminLoginResponse response = new AdminLoginResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setToken(jwtUtil.generateToken(user.getId(), user.getUsername(), user.getCompany().getCompanyId(),
                user.getRole(), user.getAuthVersion()));
        response.setAdmin(toAdmin(user));
        return response;
    }

    public OtpChallengeResponse resendLoginOtp(UUID challengeId, OtpPurpose expectedPurpose) {
        OtpChallengeService.IssuedOtp issued = otpChallengeService.resend(challengeId, expectedPurpose);
        if (issued == null) {
            throw invalidToken();
        }
        notificationService.sendOtpEmail(issued.email(), issued.code(), properties.getOtpTtl().toMinutes());
        return toChallengeResponse(issued);
    }

    public OtpChallengeResponse startEmailOtpSetting(
            Integer userId, String role, boolean enabled, String currentPassword) {
        requireFeature(properties.getFeatures().isLoginEmailOtp());
        if (("operator".equals(role) || "master".equals(role)) && !enabled) {
            throw new CustomException(ResponseConstants.FORBIDDEN, ResponseConstants.MSG_FORBIDDEN);
        }
        String email;
        String passwordHash;
        if ("operator".equals(role)) {
            OperatingUser user = operatingUserRepository.findById(userId).orElseThrow(this::invalidToken);
            email = user.getEmail();
            passwordHash = user.getPassword();
        } else {
            ParticipatingUser user = participatingUserRepository.findById(userId).orElseThrow(this::invalidToken);
            email = user.getEmail();
            passwordHash = user.getPassword();
        }
        if (!passwordEncoder.matches(currentPassword, passwordHash)) {
            throw new CustomException(ResponseConstants.INVALID_CURRENT_PASSWORD,
                    ResponseConstants.MSG_INVALID_CURRENT_PASSWORD);
        }
        OtpChallengeService.IssuedOtp issued = otpChallengeService.create(
                OtpPurpose.EMAIL_LOGIN_SETTING,
                "S:" + role + ":" + userId + ":" + enabled,
                email);
        notificationService.sendOtpEmail(issued.email(), issued.code(), properties.getOtpTtl().toMinutes());
        return toChallengeResponse(issued);
    }

    @Transactional
    public BaseResponse confirmEmailOtpSetting(Integer userId, String role, UUID challengeId, String code) {
        requireFeature(properties.getFeatures().isLoginEmailOtp());
        OtpChallengeService.OtpData challenge = otpChallengeService.verify(challengeId, code);
        String[] parts = challenge.subjectReference().split(":");
        if (challenge.purpose() != OtpPurpose.EMAIL_LOGIN_SETTING || parts.length != 4
                || !"S".equals(parts[0]) || !role.equals(parts[1])
                || userId != Integer.parseInt(parts[2])) {
            throw invalidToken();
        }
        boolean enabled = Boolean.parseBoolean(parts[3]);
        if ("operator".equals(role)) {
            OperatingUser user = operatingUserRepository.findById(userId).orElseThrow(this::invalidToken);
            user.setEmailOtpLoginEnabled(enabled);
            authVersionService.increment(user);
        } else {
            ParticipatingUser user = participatingUserRepository.findById(userId).orElseThrow(this::invalidToken);
            if ("master".equals(user.getRole()) && !enabled) {
                throw new CustomException(ResponseConstants.FORBIDDEN, ResponseConstants.MSG_FORBIDDEN);
            }
            user.setEmailOtpLoginEnabled(enabled);
            authVersionService.increment(user);
        }
        otpChallengeService.consumeVerified(challengeId, OtpPurpose.EMAIL_LOGIN_SETTING);
        return BaseResponse.success("Email login verification setting updated.");
    }

    private void issuePasswordReset(SecurityActionSubjectType type, Integer id, String email, String actor) {
        SecurityActionLinkService.IssuedActionLink issued = actionLinkService.issue(
                SecurityActionPurpose.PASSWORD_RESET, type, id, actor);
        afterCommit(() -> deliverResetLink(issued, email));
    }

    private void deliverResetLink(SecurityActionLinkService.IssuedActionLink issued, String email) {
        securityEmailDeliveryService.deliverActionLink(
                issued.linkId(),
                email,
                "Reset your password",
                "Use this single-use link to reset your password:\n" + issued.url());
    }

    private OtpChallengeResponse toChallengeResponse(OtpChallengeService.IssuedOtp issued) {
        OtpChallengeResponse response = new OtpChallengeResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("Verification code sent.");
        response.setChallengeId(issued.challengeId());
        response.setMaskedEmail(issued.maskedEmail());
        response.setExpiresInSeconds(issued.expiresInSeconds());
        return response;
    }

    private LoginSubject parseSubject(OtpChallengeService.OtpData challenge,
                                      String prefix, OtpPurpose purpose) {
        String[] parts = challenge.subjectReference().split(":");
        if (challenge.purpose() != purpose || parts.length != 3 || !prefix.equals(parts[0])) {
            throw invalidToken();
        }
        try {
            return new LoginSubject(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException ex) {
            throw invalidToken();
        }
    }

    private AdminResponse toAdmin(OperatingUser user) {
        return AdminResponse.builder()
                .username(user.getUsername())
                .name(user.getLastName() + " " + user.getFirstName())
                .role(user.getRole())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    private int safeVersion(Integer version) {
        return version == null ? 0 : version;
    }

    private void requireFeature(boolean enabled) {
        if (!enabled) {
            throw new CustomException(ResponseConstants.NOT_FOUND, ResponseConstants.MSG_NOT_FOUND);
        }
    }

    private CustomException invalidToken() {
        return new CustomException(ResponseConstants.INVALID_OR_EXPIRED_TOKEN,
                ResponseConstants.MSG_INVALID_OR_EXPIRED_TOKEN);
    }

    private void afterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
        } else {
            runnable.run();
        }
    }

    private record LoginSubject(int userId, int authVersion) {}
}
