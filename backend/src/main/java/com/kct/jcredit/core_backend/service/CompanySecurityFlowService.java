package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.config.SecurityFlowProperties;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyRegisterRequest;
import com.kct.jcredit.core_backend.dto.response.OtpChallengeResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.enums.OtpPurpose;
import com.kct.jcredit.core_backend.enums.SecurityActionPurpose;
import com.kct.jcredit.core_backend.enums.SecurityActionSubjectType;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanySecurityFlowService {
    private final SecurityFlowProperties properties;
    private final OtpChallengeService otpChallengeService;
    private final SecurityActionLinkService actionLinkService;
    private final NotificationService notificationService;
    private final SecurityEmailDeliveryService securityEmailDeliveryService;
    private final ParticipatingCompanyRepository companyRepository;
    private final PartiCompanySignupValidator signupValidator;
    private final NotificationMessages messages;

    public OtpChallengeResponse startSignupEmailVerification(String email) {
        requireFeature(properties.getFeatures().isSignupEmailVerification());
        return sendOtp(otpChallengeService.create(
                OtpPurpose.SIGNUP_EMAIL_VERIFICATION, "public-signup", email));
    }

    public OtpChallengeResponse resendSignupEmailVerification(UUID challengeId) {
        requireFeature(properties.getFeatures().isSignupEmailVerification());
        return sendOtp(otpChallengeService.resend(
                challengeId, OtpPurpose.SIGNUP_EMAIL_VERIFICATION));
    }

    public BaseResponse verifySignupEmail(UUID challengeId, String code) {
        requireFeature(properties.getFeatures().isSignupEmailVerification());
        OtpChallengeService.OtpData data = otpChallengeService.verify(challengeId, code);
        if (data.purpose() != OtpPurpose.SIGNUP_EMAIL_VERIFICATION) {
            throw invalidToken();
        }
        return BaseResponse.success("Email verified.");
    }

    @Transactional
    public SecurityActionLinkService.IssuedActionLink issueMasterSetupLink(
            ParticipatingCompany company, String actor) {
        SecurityActionLinkService.IssuedActionLink issued = actionLinkService.issue(
                SecurityActionPurpose.MASTER_ACCOUNT_SETUP,
                SecurityActionSubjectType.COMPANY,
                company.getId(), actor);
        afterCommit(() -> deliverLink(issued, company.getEmail(),
                messages.of("link.master-setup",
                        company.getCompanyId(),
                        issued.url(),
                        messages.duration(properties.getMasterSetupLinkTtl()),
                        messages.duration(properties.getMasterSetupSessionTtl()))));
        return issued;
    }

    @Transactional
    public SecurityActionLinkService.IssuedActionLink issueRevisionLink(
            ParticipatingCompany company, String rejectionReason, String actor) {
        company.setLastRejectionReason(rejectionReason);
        company.setRejectedAt(LocalDateTime.now());
        companyRepository.save(company);
        SecurityActionLinkService.IssuedActionLink issued = actionLinkService.issue(
                SecurityActionPurpose.REJECTED_SIGNUP_REVISION,
                SecurityActionSubjectType.COMPANY,
                company.getId(), actor);
        afterCommit(() -> deliverLink(issued, company.getEmail(),
                messages.of("link.revision",
                        rejectionReason,
                        issued.url(),
                        messages.duration(properties.getRevisionLinkTtl()),
                        messages.duration(properties.getRevisionSessionTtl()))));
        return issued;
    }

    @Transactional(readOnly = true)
    public PartiCompanyRegisterRequest getRevisionDraft(String sessionToken) {
        requireFeature(properties.getFeatures().isRejectedSignupRevision());
        SecurityActionLinkService.ActionSessionData session = actionLinkService.requireSession(
                sessionToken, SecurityActionPurpose.REJECTED_SIGNUP_REVISION);
        ParticipatingCompany company = companyRepository.findById(session.subjectId())
                .orElseThrow(this::invalidToken);
        if (company.getCompanyStatus() != CompanyStatus.REJECTED) {
            throw invalidToken();
        }
        return toRequest(company);
    }

    @Transactional
    public BaseResponse reviseRejectedSignup(String sessionToken, PartiCompanyRegisterRequest request) {
        requireFeature(properties.getFeatures().isRejectedSignupRevision());
        SecurityActionLinkService.ActionSessionData session = actionLinkService.requireSession(
                sessionToken, SecurityActionPurpose.REJECTED_SIGNUP_REVISION);
        ParticipatingCompany company = companyRepository.findById(session.subjectId())
                .orElseThrow(this::invalidToken);
        if (company.getCompanyStatus() != CompanyStatus.REJECTED) {
            throw invalidToken();
        }

        companyRepository.findByCorpNumber(request.getCorporateNumber())
                .filter(existing -> !existing.getId().equals(company.getId()))
                .ifPresent(existing -> {
                    throw new CustomException(ResponseConstants.CONFLICT,
                            ResponseConstants.MSG_CORP_NUMBER_ALREADY_USED);
                });
        companyRepository.findByEmailIgnoreCase(request.getEmail())
                .filter(existing -> !existing.getId().equals(company.getId()))
                .ifPresent(existing -> {
                    throw new CustomException(ResponseConstants.CONFLICT,
                            ResponseConstants.MSG_EMAIL_ALREADY_USED);
                });

        boolean emailChanged = !company.getEmail().equalsIgnoreCase(request.getEmail());
        boolean requiresEmailVerification = properties.getFeatures().isSignupEmailVerification()
                && (emailChanged || company.getEmailVerifiedAt() == null);
        if (requiresEmailVerification) {
            if (request.getEmailVerificationChallengeId() == null) {
                throw new CustomException(ResponseConstants.OTP_REQUIRED, ResponseConstants.MSG_OTP_REQUIRED);
            }
            otpChallengeService.requireVerified(request.getEmailVerificationChallengeId(),
                    OtpPurpose.SIGNUP_EMAIL_VERIFICATION, request.getEmail());
        }
        signupValidator.validateCompanyInfo(request);
        apply(company, request);
        company.setCompanyStatus(CompanyStatus.PENDING);
        company.setIsActive(false);
        company.setLastRejectionReason(null);
        company.setRejectedAt(null);
        if (requiresEmailVerification) {
            company.setEmailVerifiedAt(LocalDateTime.now());
        }
        companyRepository.save(company);
        actionLinkService.complete(sessionToken, SecurityActionPurpose.REJECTED_SIGNUP_REVISION);
        if (request.getEmailVerificationChallengeId() != null && requiresEmailVerification) {
            UUID challengeId = request.getEmailVerificationChallengeId();
            afterCommit(() -> otpChallengeService.consumeVerified(
                    challengeId, OtpPurpose.SIGNUP_EMAIL_VERIFICATION));
        }
        return BaseResponse.success("Registration revision submitted.");
    }

    private OtpChallengeResponse sendOtp(OtpChallengeService.IssuedOtp issued) {
        notificationService.sendOtpEmail(issued.email(),
                issued.code(), properties.getOtpTtl().toMinutes());
        OtpChallengeResponse response = new OtpChallengeResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("Verification code sent.");
        response.setChallengeId(issued.challengeId());
        response.setMaskedEmail(issued.maskedEmail());
        response.setExpiresInSeconds(issued.expiresInSeconds());
        return response;
    }

    private void deliverLink(SecurityActionLinkService.IssuedActionLink issued,
                             String email, NotificationMessages.Content content) {
        securityEmailDeliveryService.deliverActionLink(
                issued.linkId(), email, content.subject(), content.body());
    }

    private PartiCompanyRegisterRequest toRequest(ParticipatingCompany company) {
        return PartiCompanyRegisterRequest.builder()
                .rejectionReason(company.getLastRejectionReason())
                .rejectedAt(company.getRejectedAt())
                .corporateNumber(company.getCorpNumber())
                .corporateName(company.getNameJa())
                .corporateNameKana(company.getNameFurigana())
                .companyNameEnglish(company.getNameEn())
                .corporatePhoneNumber(company.getPhoneNumber())
                .postalCode(company.getPostalCode())
                .prefecture(company.getPrefecture())
                .municipality(company.getMunicipality())
                .blocknum(company.getBlocknum())
                .buildingName(company.getBuildingName())
                .lastName(company.getRepLastName())
                .firstName(company.getRepFirstName())
                .lastNameKana(company.getRepLastNameKana())
                .firstNameKana(company.getRepFirstNameKana())
                .position(company.getRepPosition())
                .phoneNumber(company.getRepPhoneNumber())
                .faxNumber(company.getFax())
                .email(company.getEmail())
                .methodology(company.getRequestedMethodologies())
                .activityInformation(company.getActivityInformation())
                .build();
    }

    private void apply(ParticipatingCompany company, PartiCompanyRegisterRequest request) {
        company.setCorpNumber(request.getCorporateNumber());
        company.setNameJa(request.getCorporateName());
        company.setNameFurigana(request.getCorporateNameKana());
        company.setNameEn(request.getCompanyNameEnglish());
        company.setEmail(request.getEmail());
        company.setPhoneNumber(request.getCorporatePhoneNumber());
        company.setPostalCode(request.getPostalCode());
        company.setPrefecture(request.getPrefecture());
        company.setMunicipality(request.getMunicipality());
        company.setBlocknum(request.getBlocknum());
        company.setBuildingName(request.getBuildingName());
        company.setRepFirstName(request.getFirstName());
        company.setRepLastName(request.getLastName());
        company.setRepFirstNameKana(request.getFirstNameKana());
        company.setRepLastNameKana(request.getLastNameKana());
        company.setRepPosition(request.getPosition());
        company.setRepPhoneNumber(request.getPhoneNumber());
        company.setFax(request.getFaxNumber());
        company.setRequestedMethodologies(request.getMethodology());
        company.setActivityInformation(request.getActivityInformation());
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
}
