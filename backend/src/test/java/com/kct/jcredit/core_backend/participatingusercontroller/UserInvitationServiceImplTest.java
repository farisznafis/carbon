package com.kct.jcredit.core_backend.participatingusercontroller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.kct.jcredit.core_backend.config.SecurityFlowProperties;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.SecurityActionLinkRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.NotificationMessages;
import com.kct.jcredit.core_backend.service.SecurityActionLinkService;
import com.kct.jcredit.core_backend.service.SecurityEmailDeliveryService;
import com.kct.jcredit.core_backend.service.SecurityRateLimitService;
import com.kct.jcredit.core_backend.service.UsernameAvailabilityService;
import com.kct.jcredit.core_backend.service.impl.UserInvitationServiceImpl;
// Scope note: only invite(...) is covered here, because that is the single method
// reachable from ParticipatingUserController (POST /user/invitations). getPrefill and
// complete are driven by the invitation-link endpoints and belong to that round.
// NOTE: SecurityFlowProperties is a deep-stub mock so the test does not need the name of
// its nested features type, which has not been verified here.
@ExtendWith(MockitoExtension.class)
@DisplayName("UserInvitationServiceImpl のテスト")
class UserInvitationServiceImplTest {

    @Mock private SecurityActionLinkService actionLinkService;
    @Mock private SecurityActionLinkRepository actionLinkRepository;
    @Mock private SecurityEmailDeliveryService emailDeliveryService;
    @Mock private SecurityRateLimitService rateLimitService;
    @Mock private UsernameAvailabilityService usernameAvailabilityService;
    @Mock private ParticipatingCompanyRepository companyRepository;
    @Mock private ParticipatingUserRepository userRepository;
    @Mock private SiteRepository siteRepository;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private SecurityFlowProperties properties;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private NotificationMessages messages;

    @InjectMocks
    private UserInvitationServiceImpl invitationService;

    private static final String COMPANY_ID = "C-0001";
    private static final String OTHER_COMPANY_ID = "C-9999";
    private static final Integer COMPANY_DB_ID = 10;
    private static final String ACTOR = "master001";
    private static final String RAW_EMAIL = "  NewUser@Example.co.JP  ";
    private static final String NORMALIZED_EMAIL = "newuser@example.co.jp";
    private static final String SITE_PUBLIC_ID = "PUB-SITE-001";
    private static final Integer SITE_DB_ID = 100;
    private static final int MAX_USERS_PER_SITE = 5;
    private static final int MAX_USERS_PER_COMPANY = 50;
    private static final UUID LINK_ID = UUID.randomUUID();

    // ─────────────────────────────────────────────────────────────
    // Fixtures
    // ─────────────────────────────────────────────────────────────

    private void enableFeature() {
        when(properties.getFeatures().isUserInvitation()).thenReturn(true);
    }

    private ParticipatingCompany companyMock(String companyId) {
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        lenient().when(company.getCompanyId()).thenReturn(companyId);
        lenient().when(company.getId()).thenReturn(COMPANY_DB_ID);
        lenient().when(company.getNameJa()).thenReturn("株式会社テスト");
        return company;
    }

    private Site siteMock(ParticipatingCompany owner) {
        Site site = mock(Site.class);
        lenient().when(site.getId()).thenReturn(SITE_DB_ID);
        lenient().when(site.getCompany()).thenReturn(owner);
        lenient().when(site.getSiteName()).thenReturn("熊本第一拠点");
        return site;
    }

    /** Stubs everything issueAndSend needs so the happy path can run to the end. */
    private SecurityActionLinkService.IssuedActionLink stubIssueAndSend() {
        SecurityActionLinkService.IssuedActionLink issued =
                mock(SecurityActionLinkService.IssuedActionLink.class);
        lenient().when(issued.url()).thenReturn("https://example.com/invite/abc");
        lenient().when(issued.linkId()).thenReturn(LINK_ID);
        lenient().when(actionLinkService.issue(any(), any(), any(), anyString(),
                anyString(), any())).thenReturn(issued);

        NotificationMessages.Content content = mock(NotificationMessages.Content.class);
        lenient().when(content.subject()).thenReturn("招待のお知らせ");
        lenient().when(content.body()).thenReturn("本文");
        lenient().when(messages.of(anyString(), any(Object[].class))).thenReturn(content);
        lenient().when(messages.text(anyString(), any(Object[].class))).thenReturn("拠点行");
        lenient().when(messages.duration(any())).thenReturn("24時間");
        return issued;
    }

    // ==================================================================
    // 02. invite
    // ==================================================================

    @Test
    @DisplayName("UT-US-02-01: 正常 - 招待が成功し、メールアドレスは小文字・前後空白除去で正規化される")
    void invite_validRequest_succeedsAndNormalizesEmail() {
        enableFeature();
        ParticipatingCompany company = companyMock(COMPANY_ID);
        when(companyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));

        Site site = siteMock(company);
        when(siteRepository.findByPublicIdWithFallback(SITE_PUBLIC_ID))
                .thenReturn(Optional.of(site));
        when(userRepository.existsByEmailIgnoreCase(NORMALIZED_EMAIL)).thenReturn(false);
        when(userRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(3L);
        when(userRepository.countByCompany_IdAndSite_Id(COMPANY_DB_ID, SITE_DB_ID))
                .thenReturn(1L);
        stubIssueAndSend();

        BaseResponse response =
                invitationService.invite(COMPANY_ID, ACTOR, RAW_EMAIL, SITE_PUBLIC_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getMessage())
                .isEqualTo(ResponseConstants.MSG_USER_INVITATION_SENT);
        // The raw input had mixed case and padding; every downstream check must see the
        // normalized form, otherwise duplicate detection can be bypassed by casing.
        verify(userRepository).existsByEmailIgnoreCase(NORMALIZED_EMAIL);
        verify(rateLimitService).require(eq("user-invitation"), eq(NORMALIZED_EMAIL),
                eq(3), any(Duration.class));
    }

    @Test
    @DisplayName("UT-US-02-02: 正常 - sitePublicIdが未指定の場合、拠点なしで招待され拠点上限は検査されない")
    void invite_withoutSite_skipsSiteCapacityCheck() {
        enableFeature();
        ParticipatingCompany company = companyMock(COMPANY_ID);
        when(companyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(userRepository.existsByEmailIgnoreCase(NORMALIZED_EMAIL)).thenReturn(false);
        when(userRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(3L);
        stubIssueAndSend();

        BaseResponse response =
                invitationService.invite(COMPANY_ID, ACTOR, RAW_EMAIL, "   ");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        verify(siteRepository, never()).findByPublicIdWithFallback(anyString());
        verify(userRepository, never()).countByCompany_IdAndSite_Id(any(), any());
    }

    @Test
    @DisplayName("UT-US-02-03: 異常 - 機能が無効の場合NOT_FOUNDをスローし、レート制限も消費しない")
    void invite_featureDisabled_throwsNotFoundBeforeRateLimit() {
        when(properties.getFeatures().isUserInvitation()).thenReturn(false);

        CustomException ex = assertThrows(CustomException.class,
                () -> invitationService.invite(COMPANY_ID, ACTOR, RAW_EMAIL, SITE_PUBLIC_ID));

        assertThat(ex.getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);
        // requireFeature runs first, so a disabled feature cannot be used to burn
        // another address's rate-limit budget.
        verifyNoInteractions(rateLimitService, companyRepository, userRepository);
    }

    @Test
    @DisplayName("UT-US-02-04: 異常 - 会社が存在しない場合NOT_FOUNDをスローする")
    void invite_companyNotFound_throwsNotFound() {
        enableFeature();
        when(companyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> invitationService.invite(COMPANY_ID, ACTOR, RAW_EMAIL, SITE_PUBLIC_ID));

        assertThat(ex.getMessage()).isEqualTo(ResponseConstants.MSG_COMPANY_NOT_FOUND);
        verifyNoInteractions(actionLinkService, emailDeliveryService);
    }

    @Test
    @DisplayName("UT-US-02-05: 異常 - 拠点が存在しない場合NOT_FOUNDをスローする")
    void invite_siteNotFound_throwsNotFound() {
        enableFeature();
        ParticipatingCompany company = companyMock(COMPANY_ID);
        when(companyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(siteRepository.findByPublicIdWithFallback(SITE_PUBLIC_ID))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> invitationService.invite(COMPANY_ID, ACTOR, RAW_EMAIL, SITE_PUBLIC_ID));

        assertThat(ex.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
        verifyNoInteractions(actionLinkService, emailDeliveryService);
    }

    @Test
    @DisplayName("UT-US-02-06: 異常 - 他社の拠点を指定した場合FORBIDDENをスローし、招待リンクは発行されない")
    void invite_siteFromOtherCompany_throwsForbidden() {
        enableFeature();
        ParticipatingCompany company = companyMock(COMPANY_ID);
        when(companyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));

        ParticipatingCompany otherCompany = companyMock(OTHER_COMPANY_ID);
        Site foreignSite = siteMock(otherCompany);
        when(siteRepository.findByPublicIdWithFallback(SITE_PUBLIC_ID))
                .thenReturn(Optional.of(foreignSite));

        CustomException ex = assertThrows(CustomException.class,
                () -> invitationService.invite(COMPANY_ID, ACTOR, RAW_EMAIL, SITE_PUBLIC_ID));

        assertThat(ex.getMessage()).isEqualTo(ResponseConstants.MSG_FORBIDDEN);
        verifyNoInteractions(actionLinkService, emailDeliveryService);
    }

    @Test
    @DisplayName("UT-US-02-07: 異常 - メールアドレスが使用済みの場合USED_EMAILをスローする")
    void invite_emailAlreadyUsed_throwsUsedEmail() {
        enableFeature();
        ParticipatingCompany company = companyMock(COMPANY_ID);
        when(companyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));

        Site site = siteMock(company);
        when(siteRepository.findByPublicIdWithFallback(SITE_PUBLIC_ID))
                .thenReturn(Optional.of(site));
        when(userRepository.existsByEmailIgnoreCase(NORMALIZED_EMAIL)).thenReturn(true);

        CustomException ex = assertThrows(CustomException.class,
                () -> invitationService.invite(COMPANY_ID, ACTOR, RAW_EMAIL, SITE_PUBLIC_ID));

        assertThat(ex.getMessage()).isEqualTo(ResponseConstants.MSG_USED_EMAIL);
        verifyNoInteractions(actionLinkService, emailDeliveryService);
    }

    @Test
    @DisplayName("UT-US-02-08: 異常 - 会社ユーザー数が上限50名に達している場合LIMIT_EXCEEDEDをスローする")
    void invite_companyAtCapacity_throwsLimitExceeded() {
        enableFeature();
        ParticipatingCompany company = companyMock(COMPANY_ID);
        when(companyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(userRepository.existsByEmailIgnoreCase(NORMALIZED_EMAIL)).thenReturn(false);
        // Boundary: the check is >=, so exactly MAX is already full.
        when(userRepository.countByCompany_Id(COMPANY_DB_ID))
                .thenReturn((long) MAX_USERS_PER_COMPANY);

        CustomException ex = assertThrows(CustomException.class,
                () -> invitationService.invite(COMPANY_ID, ACTOR, RAW_EMAIL, null));

        assertThat(ex.getMessage()).isEqualTo(ResponseConstants.MSG_LIMIT_EXCEEDED);
        verifyNoInteractions(actionLinkService, emailDeliveryService);
    }

    @Test
    @DisplayName("UT-US-02-09: 異常 - 拠点ユーザー数が上限5名に達している場合LIMIT_EXCEEDEDをスローする")
    void invite_siteAtCapacity_throwsLimitExceeded() {
        enableFeature();
        ParticipatingCompany company = companyMock(COMPANY_ID);
        when(companyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));

        Site site = siteMock(company);
        when(siteRepository.findByPublicIdWithFallback(SITE_PUBLIC_ID))
                .thenReturn(Optional.of(site));
        when(userRepository.existsByEmailIgnoreCase(NORMALIZED_EMAIL)).thenReturn(false);
        when(userRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(3L);
        when(userRepository.countByCompany_IdAndSite_Id(COMPANY_DB_ID, SITE_DB_ID))
                .thenReturn((long) MAX_USERS_PER_SITE);

        CustomException ex = assertThrows(CustomException.class,
                () -> invitationService.invite(COMPANY_ID, ACTOR, RAW_EMAIL, SITE_PUBLIC_ID));

        assertThat(ex.getMessage()).isEqualTo(ResponseConstants.MSG_LIMIT_EXCEEDED);
        verifyNoInteractions(actionLinkService, emailDeliveryService);
    }

    @Test
    @DisplayName("UT-US-02-10: 異常 - メールアドレスが空の場合BAD_REQUESTをスローし、レート制限も消費しない")
    void invite_blankEmail_throwsBadRequest() {
        enableFeature();

        CustomException ex = assertThrows(CustomException.class,
                () -> invitationService.invite(COMPANY_ID, ACTOR, "   ", SITE_PUBLIC_ID));

        assertThat(ex.getMessage()).isEqualTo(ResponseConstants.MSG_BAD_REQUEST);
        verifyNoInteractions(rateLimitService, companyRepository);
    }

    @Test
    @DisplayName("UT-US-02-11: 正常 - 招待メールはコミット後に送信される")
    void invite_emailIsDeliveredAfterCommit() {
        enableFeature();
        ParticipatingCompany company = companyMock(COMPANY_ID);
        when(companyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(userRepository.existsByEmailIgnoreCase(NORMALIZED_EMAIL)).thenReturn(false);
        when(userRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(3L);
        SecurityActionLinkService.IssuedActionLink issued = stubIssueAndSend();

        try (MockedStatic<TransactionSynchronizationManager> tsm =
                     mockStatic(TransactionSynchronizationManager.class)) {
            tsm.when(TransactionSynchronizationManager::isSynchronizationActive)
                    .thenReturn(true);

            invitationService.invite(COMPANY_ID, ACTOR, RAW_EMAIL, null);

            // Nothing is sent while the transaction is still open: an email about a record
            // that then rolls back cannot be recalled.
            verifyNoInteractions(emailDeliveryService);

            ArgumentCaptor<TransactionSynchronization> captor =
                    ArgumentCaptor.forClass(TransactionSynchronization.class);
            tsm.verify(() -> TransactionSynchronizationManager
                    .registerSynchronization(captor.capture()));

            captor.getValue().afterCommit();
        }

        verify(emailDeliveryService, times(1)).deliverActionLink(
                eq(issued.linkId()), eq(NORMALIZED_EMAIL), anyString(), anyString());
    }

    @Test
    @DisplayName("UT-US-02-12: 準正常 - エラーは例外でありstatusCodeを返さない（要確認）")
    void invite_reportsErrorsByThrowing_notByStatusCode() {
        // NOTE: every failure path throws CustomException and only the success path
        // returns a BaseResponse, so the controller's
        // ResponseUtil.getHttpStatus(response.getStatusCode()) never runs for an error on
        // this endpoint - the global exception handler answers instead. UserServiceImpl,
        // reached from the same controller, uses the opposite convention (response +
        // statusCode). Recorded so the split is a deliberate choice rather than drift.
        enableFeature();
        when(companyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(CustomException.class,
                () -> invitationService.invite(COMPANY_ID, ACTOR, RAW_EMAIL, null));
    }
}