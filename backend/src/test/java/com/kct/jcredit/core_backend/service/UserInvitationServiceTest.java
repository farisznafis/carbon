package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.config.SecurityFlowProperties;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.UserInvitationCompleteRequest;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.SecurityActionLink;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.enums.SecurityActionDeliveryStatus;
import com.kct.jcredit.core_backend.enums.SecurityActionPurpose;
import com.kct.jcredit.core_backend.enums.SecurityActionStatus;
import com.kct.jcredit.core_backend.enums.SecurityActionSubjectType;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.SecurityActionLinkRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.impl.UserInvitationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserInvitationServiceTest {

    private static final String COMPANY_ID = "1234567890";
    private static final Integer COMPANY_PK = 7;
    private static final Integer SITE_ID = 42;
    // Real shape: SITE + 16 random base62 chars, so it can never be parsed back into a numeric id.
    private static final String SITE_PUBLIC_ID = "SITEa8Kd93jFnQ2xLp0w";

    private final List<SecurityActionLink> links = new ArrayList<>();

    private SecurityActionLinkRepository actionLinkRepository;
    private ParticipatingUserRepository userRepository;
    private SecurityActionLinkService actionLinkService;
    private UserInvitationService service;
    private ParticipatingCompany company;
    private Site site;

    @BeforeEach
    void setUp() {
        SecurityFlowProperties properties = new SecurityFlowProperties();
        properties.setTokenPepper("test-pepper-with-enough-randomness");
        properties.getFeatures().setUserInvitation(true);

        company = new ParticipatingCompany();
        company.setId(COMPANY_PK);
        company.setCompanyId(COMPANY_ID);
        company.setNameJa("テスト株式会社");
        company.setCompanyStatus(CompanyStatus.ACTIVE);

        site = Site.builder().siteName("東京工場").company(company).build();
        site.setId(SITE_ID);
        site.setPublicId(SITE_PUBLIC_ID);

        actionLinkRepository = mock(SecurityActionLinkRepository.class);
        userRepository = mock(ParticipatingUserRepository.class);
        ParticipatingCompanyRepository companyRepository = mock(ParticipatingCompanyRepository.class);
        SiteRepository siteRepository = mock(SiteRepository.class);
        SecurityEmailDeliveryService emailDeliveryService = mock(SecurityEmailDeliveryService.class);
        SecurityRateLimitService rateLimitService = mock(SecurityRateLimitService.class);
        UsernameAvailabilityService usernameAvailabilityService = mock(UsernameAvailabilityService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        actionLinkService = mock(SecurityActionLinkService.class);

        when(companyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(companyRepository.findById(COMPANY_PK)).thenReturn(Optional.of(company));
        when(companyRepository.findByIdForUpdate(COMPANY_PK)).thenReturn(Optional.of(company));
        when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(site));
        when(siteRepository.findByPublicIdWithFallback(SITE_PUBLIC_ID)).thenReturn(Optional.of(site));
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.countByCompany_Id(anyInt())).thenReturn(0L);
        when(userRepository.countByCompany_IdAndSite_Id(anyInt(), anyInt())).thenReturn(0L);
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(usernameAvailabilityService.lockAndRequireAvailable(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(actionLinkRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Stand-in for the real link service: records issued links and applies the same
        // email-scoped revocation the production code performs.
        when(actionLinkService.issue(any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    String inviteEmail = invocation.getArgument(4);
                    Integer inviteSiteId = invocation.getArgument(5);
                    links.stream()
                            .filter(link -> link.getInviteEmail().equalsIgnoreCase(inviteEmail))
                            .filter(link -> link.getStatus() == SecurityActionStatus.PENDING
                                    || link.getStatus() == SecurityActionStatus.OPENED)
                            .forEach(link -> link.setStatus(SecurityActionStatus.REVOKED));
                    SecurityActionLink link = newLink(inviteEmail, inviteSiteId);
                    links.add(link);
                    return new SecurityActionLinkService.IssuedActionLink(
                            link.getId(), "raw-token", link.getExpiresAt(), "https://app.test/x#token=raw");
                });
        when(actionLinkRepository.findById(any())).thenAnswer(invocation ->
                links.stream().filter(link -> link.getId().equals(invocation.getArgument(0))).findFirst());

        service = new UserInvitationServiceImpl(
                actionLinkService, actionLinkRepository, emailDeliveryService, rateLimitService,
                usernameAvailabilityService, companyRepository, userRepository, siteRepository,
                properties, passwordEncoder, new NotificationMessages());
    }

    @Test
    void invitingTwoDifferentAddressesLeavesBothInvitationsLive() {
        service.invite(COMPANY_ID, "master1", "first@example.test", SITE_PUBLIC_ID);
        service.invite(COMPANY_ID, "master1", "second@example.test", SITE_PUBLIC_ID);

        assertThat(links).hasSize(2);
        assertThat(links).allMatch(link -> link.getStatus() == SecurityActionStatus.PENDING);
    }

    @Test
    void invitingTheSameAddressAgainSupersedesTheEarlierInvitation() {
        service.invite(COMPANY_ID, "master1", "again@example.test", SITE_PUBLIC_ID);
        service.invite(COMPANY_ID, "master1", "again@example.test", SITE_PUBLIC_ID);

        assertThat(links).hasSize(2);
        assertThat(links.get(0).getStatus()).isEqualTo(SecurityActionStatus.REVOKED);
        assertThat(links.get(1).getStatus()).isEqualTo(SecurityActionStatus.PENDING);
    }

    @Test
    void invitingAnAddressThatAlreadyHasAnAccountIsRejected() {
        when(userRepository.existsByEmailIgnoreCase("taken@example.test")).thenReturn(true);

        assertThatThrownBy(() -> service.invite(COMPANY_ID, "master1", "taken@example.test", SITE_PUBLIC_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ResponseConstants.USED_EMAIL);
    }

    @Test
    void invitingIntoAFullSiteIsRejected() {
        when(userRepository.countByCompany_IdAndSite_Id(COMPANY_PK, SITE_ID)).thenReturn(5L);

        assertThatThrownBy(() -> service.invite(COMPANY_ID, "master1", "late@example.test", SITE_PUBLIC_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ResponseConstants.LIMIT_EXCEEDED);
    }

    @Test
    void completingCreatesTheUserWithTheInvitedAddressNotOneTheInviteeChose() {
        service.invite(COMPANY_ID, "master1", "Invitee@Example.test", SITE_PUBLIC_ID);
        SecurityActionLink link = links.get(0);
        stubSession(link);

        service.complete("session-token", completeRequest());

        org.mockito.ArgumentCaptor<ParticipatingUser> captor =
                org.mockito.ArgumentCaptor.forClass(ParticipatingUser.class);
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        ParticipatingUser saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("invitee@example.test");
        assertThat(saved.getRole()).isEqualTo("user");
        assertThat(saved.getSite()).isSameAs(site);
        assertThat(saved.getPassword()).isNotEqualTo("Correct-Horse-9-Battery");
    }

    @Test
    void completingIsRejectedWhenTheAddressGotTakenInTheMeantime() {
        service.invite(COMPANY_ID, "master1", "racer@example.test", SITE_PUBLIC_ID);
        stubSession(links.get(0));
        when(userRepository.existsByEmailIgnoreCase("racer@example.test")).thenReturn(true);

        assertThatThrownBy(() -> service.complete("session-token", completeRequest()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ResponseConstants.USED_EMAIL);
    }

    @Test
    void completingIsRejectedWhenTheSiteFilledUpAfterTheInvitationWasSent() {
        service.invite(COMPANY_ID, "master1", "slow@example.test", SITE_PUBLIC_ID);
        stubSession(links.get(0));
        when(userRepository.countByCompany_IdAndSite_Id(COMPANY_PK, SITE_ID)).thenReturn(5L);

        assertThatThrownBy(() -> service.complete("session-token", completeRequest()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ResponseConstants.LIMIT_EXCEEDED);
    }

    @Test
    void everyEndpointIsHiddenWhileTheFeatureIsOff() {
        SecurityFlowProperties off = new SecurityFlowProperties();
        off.setTokenPepper("test-pepper-with-enough-randomness");
        UserInvitationService disabled = new UserInvitationServiceImpl(
                actionLinkService, actionLinkRepository, mock(SecurityEmailDeliveryService.class),
                mock(SecurityRateLimitService.class), mock(UsernameAvailabilityService.class),
                mock(ParticipatingCompanyRepository.class), userRepository,
                mock(SiteRepository.class), off, mock(PasswordEncoder.class), new NotificationMessages());

        assertThatThrownBy(() -> disabled.invite(COMPANY_ID, "master1", "x@example.test", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ResponseConstants.NOT_FOUND);
    }

    private void stubSession(SecurityActionLink link) {
        when(actionLinkService.requireSession("session-token", SecurityActionPurpose.USER_INVITATION))
                .thenReturn(new SecurityActionLinkService.ActionSessionData(
                        link.getId(), SecurityActionPurpose.USER_INVITATION,
                        SecurityActionSubjectType.COMPANY, COMPANY_PK));
    }

    private UserInvitationCompleteRequest completeRequest() {
        UserInvitationCompleteRequest request = new UserInvitationCompleteRequest();
        request.setUsername("newuser");
        request.setFirstName("太郎");
        request.setFirstNameKana("タロウ");
        request.setLastName("山田");
        request.setLastNameKana("ヤマダ");
        request.setPhoneNumber("03-1234-5678");
        request.setUserPosition("担当者");
        request.setPassword("Correct-Horse-9-Battery");
        return request;
    }

    private SecurityActionLink newLink(String inviteEmail, Integer inviteSiteId) {
        return SecurityActionLink.builder()
                .id(UUID.randomUUID())
                .purpose(SecurityActionPurpose.USER_INVITATION)
                .subjectType(SecurityActionSubjectType.COMPANY)
                .subjectId(COMPANY_PK)
                .tokenHash(UUID.randomUUID().toString())
                .status(SecurityActionStatus.PENDING)
                .deliveryStatus(SecurityActionDeliveryStatus.PENDING)
                .deliveryAttempts(0)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .createdBy("master1")
                .inviteEmail(inviteEmail)
                .inviteSiteId(inviteSiteId)
                .build();
    }
}
