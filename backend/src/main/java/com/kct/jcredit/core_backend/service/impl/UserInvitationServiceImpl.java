package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.config.SecurityFlowProperties;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.request.UserInvitationCompleteRequest;
import com.kct.jcredit.core_backend.dto.response.UserInvitationPrefillResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.SecurityActionLink;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.enums.SecurityActionPurpose;
import com.kct.jcredit.core_backend.enums.SecurityActionSubjectType;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.SecurityActionLinkRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.NotificationMessages;
import com.kct.jcredit.core_backend.service.SecurityActionLinkService;
import com.kct.jcredit.core_backend.service.SecurityEmailDeliveryService;
import com.kct.jcredit.core_backend.service.SecurityRateLimitService;
import com.kct.jcredit.core_backend.service.UserInvitationService;
import com.kct.jcredit.core_backend.service.UsernameAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserInvitationServiceImpl implements UserInvitationService {

    private static final int MAX_USERS_PER_SITE = 5;
    private static final int MAX_USERS_PER_COMPANY = 50;

    private final SecurityActionLinkService actionLinkService;
    private final SecurityActionLinkRepository actionLinkRepository;
    private final SecurityEmailDeliveryService emailDeliveryService;
    private final SecurityRateLimitService rateLimitService;
    private final UsernameAvailabilityService usernameAvailabilityService;
    private final ParticipatingCompanyRepository companyRepository;
    private final ParticipatingUserRepository userRepository;
    private final SiteRepository siteRepository;
    private final SecurityFlowProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final NotificationMessages messages;

    @Override
    @Transactional
    public BaseResponse invite(String companyId, String actor, String email, String sitePublicId) {
        requireFeature();
        String normalizedEmail = normalizeEmail(email);
        rateLimitService.require("user-invitation", normalizedEmail, 3, Duration.ofHours(1));

        ParticipatingCompany company = requireCompany(companyId);
        Site site = resolveSiteByPublicId(company, sitePublicId);
        requireEmailAvailable(normalizedEmail);
        requireCapacity(company, site == null ? null : site.getId());

        issueAndSend(company, site, normalizedEmail, actor);
        return BaseResponse.success(ResponseConstants.MSG_USER_INVITATION_SENT);
    }

    @Override
    @Transactional(readOnly = true)
    public UserInvitationPrefillResponse getPrefill(String sessionToken) {
        requireFeature();
        SecurityActionLinkService.ActionSessionData session = actionLinkService.requireSession(
                sessionToken, SecurityActionPurpose.USER_INVITATION);
        SecurityActionLink link = actionLinkRepository.findById(session.linkId())
                .orElseThrow(this::invalidToken);
        ParticipatingCompany company = companyRepository.findById(session.subjectId())
                .orElseThrow(this::invalidToken);
        Site site = link.getInviteSiteId() == null
                ? null
                : siteRepository.findById(link.getInviteSiteId()).orElse(null);

        UserInvitationPrefillResponse response = new UserInvitationPrefillResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setTimestamp(LocalDateTime.now());
        response.setEmail(link.getInviteEmail());
        response.setCompanyName(company.getNameJa());
        response.setSiteId(site == null ? null : site.getPublicId());
        response.setSiteName(site == null ? null : site.getSiteName());
        return response;
    }

    @Override
    @Transactional
    public BaseResponse complete(String sessionToken, UserInvitationCompleteRequest request) {
        requireFeature();
        SecurityActionLinkService.ActionSessionData session = actionLinkService.requireSession(
                sessionToken, SecurityActionPurpose.USER_INVITATION);
        SecurityActionLink link = actionLinkRepository.findById(session.linkId())
                .orElseThrow(this::invalidToken);
        ParticipatingCompany company = companyRepository.findByIdForUpdate(session.subjectId())
                .orElseThrow(this::invalidToken);
        if (company.getCompanyStatus() != CompanyStatus.ACTIVE) {
            throw invalidToken();
        }

        String normalizedEmail = normalizeEmail(link.getInviteEmail());
        Site site = resolveSiteById(company, link.getInviteSiteId());

        requireEmailAvailable(normalizedEmail);
        requireCapacity(company, link.getInviteSiteId());

        String availableUsername =
                usernameAvailabilityService.lockAndRequireAvailable(request.getUsername());
        ParticipatingUser user = ParticipatingUser.builder()
                .username(availableUsername)
                .firstName(request.getFirstName())
                .firstNameKana(request.getFirstNameKana())
                .lastName(request.getLastName())
                .lastNameKana(request.getLastNameKana())
                .email(normalizedEmail)
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(RoleConstants.USER)
                .userPosition(request.getUserPosition())
                .loginAttempts(0)
                .accountLockedUntil(null)
                .site(site)
                .company(company)
                .build();
        userRepository.save(user);

        actionLinkService.complete(sessionToken, SecurityActionPurpose.USER_INVITATION);
        return BaseResponse.success(ResponseConstants.MSG_USER_INVITATION_COMPLETED);
    }

    private void issueAndSend(ParticipatingCompany company, Site site, String email, String actor) {
        SecurityActionLinkService.IssuedActionLink issued = actionLinkService.issue(
                SecurityActionPurpose.USER_INVITATION,
                SecurityActionSubjectType.COMPANY,
                company.getId(), actor,
                email, site == null ? null : site.getId());

        String siteLine = site == null
                ? ""
                : messages.text("link.user-invitation.site-line", site.getSiteName());
        NotificationMessages.Content content = messages.of(
                "link.user-invitation",
                company.getNameJa(),
                siteLine,
                issued.url(),
                messages.duration(properties.getUserInvitationLinkTtl()),
                messages.duration(properties.getUserInvitationSessionTtl()));

        afterCommit(() -> emailDeliveryService.deliverActionLink(
                issued.linkId(), email, content.subject(), content.body()));
    }

    private ParticipatingCompany requireCompany(String companyId) {
        return companyRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        ResponseConstants.MSG_COMPANY_NOT_FOUND));
    }

    private Site resolveSiteByPublicId(ParticipatingCompany company, String sitePublicId) {
        if (sitePublicId == null || sitePublicId.isBlank()) {
            return null;
        }
        Site site = siteRepository.findByPublicIdWithFallback(sitePublicId.trim())
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        ResponseConstants.MSG_SITE_NOT_FOUND));
        return requireOwnedByCompany(company, site);
    }

    private Site resolveSiteById(ParticipatingCompany company, Integer siteId) {
        if (siteId == null) {
            return null;
        }
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        ResponseConstants.MSG_SITE_NOT_FOUND));
        return requireOwnedByCompany(company, site);
    }

    private Site requireOwnedByCompany(ParticipatingCompany company, Site site) {
        if (!company.getCompanyId().equals(site.getCompany().getCompanyId())) {
            throw new CustomException(
                    ResponseConstants.FORBIDDEN,
                    ResponseConstants.MSG_FORBIDDEN);
        }
        return site;
    }

    private void requireEmailAvailable(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new CustomException(
                    ResponseConstants.USED_EMAIL,
                    ResponseConstants.MSG_USED_EMAIL);
        }
    }

    private void requireCapacity(ParticipatingCompany company, Integer siteId) {
        if (userRepository.countByCompany_Id(company.getId()) >= MAX_USERS_PER_COMPANY) {
            throw new CustomException(
                    ResponseConstants.LIMIT_EXCEEDED,
                    ResponseConstants.MSG_LIMIT_EXCEEDED);
        }
        if (siteId != null
                && userRepository.countByCompany_IdAndSite_Id(company.getId(), siteId)
                        >= MAX_USERS_PER_SITE) {
            throw new CustomException(
                    ResponseConstants.LIMIT_EXCEEDED,
                    ResponseConstants.MSG_LIMIT_EXCEEDED);
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new CustomException(
                    ResponseConstants.BAD_REQUEST,
                    ResponseConstants.MSG_BAD_REQUEST);
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void afterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            runnable.run();
                        }
                    });
        } else {
            runnable.run();
        }
    }

    private void requireFeature() {
        if (!properties.getFeatures().isUserInvitation()) {
            throw new CustomException(
                    ResponseConstants.NOT_FOUND,
                    ResponseConstants.MSG_NOT_FOUND);
        }
    }

    private CustomException invalidToken() {
        return new CustomException(
                ResponseConstants.INVALID_OR_EXPIRED_TOKEN,
                ResponseConstants.MSG_INVALID_OR_EXPIRED_TOKEN);
    }
}
