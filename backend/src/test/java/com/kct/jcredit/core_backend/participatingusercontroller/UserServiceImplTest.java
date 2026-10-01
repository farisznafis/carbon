package com.kct.jcredit.core_backend.participatingusercontroller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.UpdateUserRequest;
import com.kct.jcredit.core_backend.dto.response.UpdateUserResponse;
import com.kct.jcredit.core_backend.dto.response.UserListResponse;
import com.kct.jcredit.core_backend.dto.response.UserProfileResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.UserInfoResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.TokenBlacklistService;
import com.kct.jcredit.core_backend.service.UsernameAvailabilityService;
import com.kct.jcredit.core_backend.service.impl.UserServiceImpl;
// Scope note: only the methods reachable from ParticipatingUserController are covered.
// getUserProfile(username) and changePassword(...) belong to other controllers' rounds.
// NOTE: entities are mocked rather than built, so the tests do not depend on entity
// constructors or builders that have not been verified here.
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl のテスト")
class UserServiceImplTest {

    @Mock private ParticipatingUserRepository userRepository;
    @Mock private ParticipatingCompanyRepository participatingCompanyRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private UsernameAvailabilityService usernameAvailabilityService;

    @InjectMocks
    private UserServiceImpl userService;

    private static final String COMPANY_ID = "C-0001";
    private static final String OTHER_COMPANY_ID = "C-9999";
    private static final Integer COMPANY_DB_ID = 10;
    private static final String MASTER_USERNAME = "master001";
    private static final Integer MASTER_DB_ID = 1;
    private static final Integer TARGET_DB_ID = 2;
    private static final Integer SITE_DB_ID = 100;
    private static final Integer OTHER_SITE_DB_ID = 200;
    private static final String PIC_EMAIL = "pic@example.co.jp";
    private static final int MAX_USERS_PER_SITE = 5;

    // ─────────────────────────────────────────────────────────────
    // Fixtures
    // ─────────────────────────────────────────────────────────────

    private ParticipatingCompany companyMock(String companyId) {
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        lenient().when(company.getCompanyId()).thenReturn(companyId);
        lenient().when(company.getId()).thenReturn(COMPANY_DB_ID);
        return company;
    }

    private Site siteMock(Integer id, ParticipatingCompany company, String picEmail) {
        Site site = mock(Site.class);
        lenient().when(site.getId()).thenReturn(id);
        lenient().when(site.getCompany()).thenReturn(company);
        lenient().when(site.getPicEmail()).thenReturn(picEmail);
        lenient().when(site.getSiteName()).thenReturn("拠点" + id);
        // getProjectsForSite tolerates null and returns an empty list.
        lenient().when(site.getSiteProjectRelations()).thenReturn(null);
        return site;
    }

    private ParticipatingUser userMock(Integer id, String role, String email,
                                       ParticipatingCompany company, Site site) {
        ParticipatingUser user = mock(ParticipatingUser.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getRole()).thenReturn(role);
        lenient().when(user.getEmail()).thenReturn(email);
        lenient().when(user.getCompany()).thenReturn(company);
        lenient().when(user.getSite()).thenReturn(site);
        lenient().when(user.getUsername()).thenReturn("user" + id);
        lenient().when(user.getFirstName()).thenReturn("太郎");
        lenient().when(user.getLastName()).thenReturn("熊本");
        return user;
    }

    /**
     * An UpdateUserRequest with both nullable ids explicitly set to null.
     * Mockito's default answer returns 0 - not null - for a method declared to return
     * Integer, and the service branches on "!= null", so an unstubbed getUserId() would
     * be read as "target user 0" and an unstubbed getSiteId() as "move to site 0".
     */
    private UpdateUserRequest request() {
        UpdateUserRequest request = mock(UpdateUserRequest.class);
        lenient().when(request.getUserId()).thenReturn(null);
        lenient().when(request.getSiteId()).thenReturn(null);
        return request;
    }

    // ==================================================================
    // 01. getUsersByCompany
    // ==================================================================

    @Test
    @DisplayName("UT-US-01-01: 正常 - 会社が存在する場合、所属ユーザー一覧をOKで返す")
    void getUsersByCompany_companyExists_returnsMappedUsers() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.of(company));

        ParticipatingUser u1 = userMock(1, "master", "a@example.co.jp", company, null);
        ParticipatingUser u2 = userMock(2, "user", "b@example.co.jp", company, null);
        when(userRepository.findByCompany_Id(COMPANY_DB_ID)).thenReturn(List.of(u1, u2));

        UserListResponse response = userService.getUsersByCompany(COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getUsers()).hasSize(2);
        assertThat(response.getUsers()).extracting(UserInfoResponse::getUsername)
                .containsExactly("user1", "user2");
    }

    @Test
    @DisplayName("UT-US-01-02: 正常 - 所属ユーザーが0件の場合も空配列とOKを返す")
    void getUsersByCompany_noUsers_returnsEmptyListWithOk() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.of(company));
        when(userRepository.findByCompany_Id(COMPANY_DB_ID)).thenReturn(List.of());

        UserListResponse response = userService.getUsersByCompany(COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getUsers()).isEmpty();
    }

    @Test
    @DisplayName("UT-US-01-03: 異常 - 会社が存在しない場合NOT_FOUNDを返し、ユーザー検索は行わない")
    void getUsersByCompany_companyNotFound_returnsNotFoundWithoutQueryingUsers() {
        when(participatingCompanyRepository.findByCompanyId(OTHER_COMPANY_ID))
                .thenReturn(Optional.empty());

        UserListResponse response = userService.getUsersByCompany(OTHER_COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        verify(userRepository, never()).findByCompany_Id(any());
    }

    @Test
    @DisplayName("UT-US-01-04: 正常 - 拠点未割り当てのユーザーはsiteId・siteNameがnullで返る")
    void getUsersByCompany_userWithoutSite_mapsSiteFieldsAsNull() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.of(company));

        ParticipatingUser noSite = userMock(1, "user", "a@example.co.jp", company, null);
        when(userRepository.findByCompany_Id(COMPANY_DB_ID)).thenReturn(List.of(noSite));

        UserListResponse response = userService.getUsersByCompany(COMPANY_ID);

        UserInfoResponse mapped = response.getUsers().get(0);
        assertThat(mapped.getSiteId()).isNull();
        assertThat(mapped.getSiteName()).isNull();
    }

    @Test
    @DisplayName("UT-US-01-05: 準正常 - 例外処理がなくメッセージもハードコードされている（要確認）")
    void getUsersByCompany_hasNoTryCatchAndHardcodedMessage() {
        // NOTE: unlike getUserProfileById and updateUser, this method has no try/catch, so
        // a failure inside the mapping propagates as a raw 500 instead of
        // INTERNAL_SERVER_ERROR. Its NOT_FOUND message is also a literal rather than a
        // ResponseConstants entry. Recorded, not asserted as correct.
        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.empty());

        UserListResponse response = userService.getUsersByCompany(COMPANY_ID);

        assertThat(response.getMessage()).isEqualTo("該当する会社が見つかりませんでした。");
        assertThat(response.getMessage()).isNotEqualTo(ResponseConstants.MSG_NOT_FOUND);
    }

    // ==================================================================
    // 03. updateUser
    // ==================================================================

    /** Stubs the two lookups every updateUser call starts with. */
    private ParticipatingUser stubRequestingMaster(ParticipatingCompany company) {
        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.of(company));
        ParticipatingUser master =
                userMock(MASTER_DB_ID, "master", "master@example.co.jp", company, null);
        when(userRepository.findByUsernameAndCompany_Id(MASTER_USERNAME, COMPANY_DB_ID))
                .thenReturn(Optional.of(master));
        return master;
    }

    @Test
    @DisplayName("UT-US-03-01: 正常 - マスターが他ユーザーを拠点変更なしで更新するとOKを返す")
    void updateUser_masterEditsOtherUserNoSiteChange_returnsOk() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        stubRequestingMaster(company);

        Site site = siteMock(SITE_DB_ID, company, "other@example.co.jp");
        ParticipatingUser target =
                userMock(TARGET_DB_ID, "user", "target@example.co.jp", company, site);
        UpdateUserRequest request = request();
        when(request.getUserId()).thenReturn(TARGET_DB_ID);
        when(userRepository.findById(TARGET_DB_ID)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        UpdateUserResponse response =
                userService.updateUser(COMPANY_ID, MASTER_USERNAME, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        verify(userRepository, times(1)).save(target);
        // Not the PIC of that site, so the site is left alone.
        verify(siteRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-US-03-02: 正常 - マスターが自分を更新するとPIC拠点と法人情報にも反映される")
    void updateUser_masterEditsSelf_cascadesToPicSitesAndCompany() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.of(company));
        ParticipatingUser master =
                userMock(MASTER_DB_ID, "master", PIC_EMAIL, company, null);
        when(userRepository.findByUsernameAndCompany_Id(MASTER_USERNAME, COMPANY_DB_ID))
                .thenReturn(Optional.of(master));

        Site picSite = siteMock(SITE_DB_ID, company, PIC_EMAIL);
        when(siteRepository.findByCompanyIdAndPicEmail(COMPANY_DB_ID, PIC_EMAIL))
                .thenReturn(List.of(picSite));
        when(siteRepository.save(picSite)).thenReturn(picSite);
        when(userRepository.save(master)).thenReturn(master);

        UpdateUserRequest request = request();   // no userId → targets self

        UpdateUserResponse response =
                userService.updateUser(COMPANY_ID, MASTER_USERNAME, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getUpdatedSites()).hasSize(1);
        // The master's details are also the company's representative details.
        verify(participatingCompanyRepository, times(1)).save(company);
        verify(company).setUpdatedBy(master.getUsername());
    }

    @Test
    @DisplayName("UT-US-03-03: 正常 - マスターが他ユーザーを別拠点へ移動すると旧拠点PICがマスターに引き継がれる")
    void updateUser_masterMovesPicUserToAnotherSite_transfersOldSitePic() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        ParticipatingUser master = stubRequestingMaster(company);

        Site currentSite = siteMock(SITE_DB_ID, company, "target@example.co.jp");
        ParticipatingUser target =
                userMock(TARGET_DB_ID, "user", "target@example.co.jp", company, currentSite);
        Site newSite = siteMock(OTHER_SITE_DB_ID, company, "someone@example.co.jp");

        UpdateUserRequest request = request();
        when(request.getUserId()).thenReturn(TARGET_DB_ID);
        when(request.getSiteId()).thenReturn(OTHER_SITE_DB_ID);
        when(userRepository.findById(TARGET_DB_ID)).thenReturn(Optional.of(target));
        when(siteRepository.findById(OTHER_SITE_DB_ID)).thenReturn(Optional.of(newSite));
        when(userRepository.countByCompany_IdAndSite_Id(COMPANY_DB_ID, OTHER_SITE_DB_ID))
                .thenReturn(1L);
        when(siteRepository.save(currentSite)).thenReturn(currentSite);
        when(userRepository.save(target)).thenReturn(target);

        UpdateUserResponse response =
                userService.updateUser(COMPANY_ID, MASTER_USERNAME, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        // The user was the PIC of the old site, so the master takes that role over.
        verify(currentSite).setPicEmail(master.getEmail());
        verify(target).setSite(newSite);
    }

    @Test
    @DisplayName("UT-US-03-04: 異常 - マスターが自身の拠点を変更しようとするとBAD_REQUESTを返す")
    void updateUser_masterChangesOwnSite_returnsBadRequest() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        stubRequestingMaster(company);

        UpdateUserRequest request = request();
        when(request.getSiteId()).thenReturn(SITE_DB_ID);   // no userId → self

        UpdateUserResponse response =
                userService.updateUser(COMPANY_ID, MASTER_USERNAME, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-US-03-05: 異常 - 対象ユーザーが存在しない場合NOT_FOUNDを返す")
    void updateUser_targetNotFound_returnsNotFound() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        stubRequestingMaster(company);

        UpdateUserRequest request = request();
        when(request.getUserId()).thenReturn(9999);
        when(userRepository.findById(9999)).thenReturn(Optional.empty());

        UpdateUserResponse response =
                userService.updateUser(COMPANY_ID, MASTER_USERNAME, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-US-03-06: 異常 - 対象ユーザーが他社所属の場合FORBIDDENを返す")
    void updateUser_targetFromOtherCompany_returnsForbidden() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        stubRequestingMaster(company);

        ParticipatingCompany otherCompany = companyMock(OTHER_COMPANY_ID);
        ParticipatingUser foreignTarget =
                userMock(TARGET_DB_ID, "user", "x@example.co.jp", otherCompany, null);

        UpdateUserRequest request = request();
        when(request.getUserId()).thenReturn(TARGET_DB_ID);
        when(userRepository.findById(TARGET_DB_ID)).thenReturn(Optional.of(foreignTarget));

        UpdateUserResponse response =
                userService.updateUser(COMPANY_ID, MASTER_USERNAME, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-US-03-07: 異常 - 移動先拠点が他社のものである場合FORBIDDENを返す")
    void updateUser_requestedSiteFromOtherCompany_returnsForbidden() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        stubRequestingMaster(company);

        ParticipatingUser target =
                userMock(TARGET_DB_ID, "user", "target@example.co.jp", company, null);
        ParticipatingCompany otherCompany = companyMock(OTHER_COMPANY_ID);
        Site foreignSite = siteMock(OTHER_SITE_DB_ID, otherCompany, "z@example.co.jp");

        UpdateUserRequest request = request();
        when(request.getUserId()).thenReturn(TARGET_DB_ID);
        when(request.getSiteId()).thenReturn(OTHER_SITE_DB_ID);
        when(userRepository.findById(TARGET_DB_ID)).thenReturn(Optional.of(target));
        when(siteRepository.findById(OTHER_SITE_DB_ID)).thenReturn(Optional.of(foreignSite));

        UpdateUserResponse response =
                userService.updateUser(COMPANY_ID, MASTER_USERNAME, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-US-03-08: 異常 - 移動先拠点が上限5名に達している場合LIMIT_EXCEEDEDを返す")
    void updateUser_requestedSiteAtCapacity_returnsLimitExceeded() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        stubRequestingMaster(company);

        ParticipatingUser target =
                userMock(TARGET_DB_ID, "user", "target@example.co.jp", company, null);
        Site newSite = siteMock(OTHER_SITE_DB_ID, company, "z@example.co.jp");

        UpdateUserRequest request = request();
        when(request.getUserId()).thenReturn(TARGET_DB_ID);
        when(request.getSiteId()).thenReturn(OTHER_SITE_DB_ID);
        when(userRepository.findById(TARGET_DB_ID)).thenReturn(Optional.of(target));
        when(siteRepository.findById(OTHER_SITE_DB_ID)).thenReturn(Optional.of(newSite));
        // Boundary: the check is >=, so exactly MAX is already full.
        when(userRepository.countByCompany_IdAndSite_Id(COMPANY_DB_ID, OTHER_SITE_DB_ID))
                .thenReturn((long) MAX_USERS_PER_SITE);

        UpdateUserResponse response =
                userService.updateUser(COMPANY_ID, MASTER_USERNAME, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.LIMIT_EXCEEDED);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-US-03-09: 正常 - 空文字・null項目は既存値を上書きしない")
    void updateUser_blankFields_doNotOverwriteExistingValues() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        stubRequestingMaster(company);

        ParticipatingUser target =
                userMock(TARGET_DB_ID, "user", "target@example.co.jp", company, null);
        UpdateUserRequest request = request();
        when(request.getUserId()).thenReturn(TARGET_DB_ID);
        when(request.getFirstName()).thenReturn("   ");   // blank → ignored
        // every other getter returns null → ignored
        when(userRepository.findById(TARGET_DB_ID)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        userService.updateUser(COMPANY_ID, MASTER_USERNAME, request);

        verify(target, never()).setFirstName(anyString());
        verify(target, never()).setLastName(anyString());
        verify(target, never()).setEmail(anyString());
    }

    @Test
    @DisplayName("UT-US-03-10: 異常 - 非マスターが他人のuserIdを指定した場合FORBIDDENを返すべき【要修正】")
    void updateUser_nonMasterTargetsAnotherUser_shouldReturnForbidden() {
        // Pattern B: GATE 1 (!isMaster && !isSelf) can never fire, because
        // resolveTargetUser returns requestingUser for every non-master. The request is
        // therefore silently redirected onto the caller's own record and answered with
        // 200 OK - the caller believes they edited user 2 while user 1 was changed.
        // validateUpdateUser returns FORBIDDEN for exactly this input, so validate and
        // execute disagree. Currently unreachable through PATCH /user (master-only), but
        // the service must not depend on the annotation for this.
        ParticipatingCompany company = companyMock(COMPANY_ID);
        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.of(company));
        ParticipatingUser regularUser =
                userMock(MASTER_DB_ID, "user", "me@example.co.jp", company, null);
        when(userRepository.findByUsernameAndCompany_Id("user001", COMPANY_DB_ID))
                .thenReturn(Optional.of(regularUser));

        UpdateUserRequest request = request();
        // lenient: "isMaster && request.getUserId() != null" short-circuits on isMaster,
        // so for a non-master this getter is never read - which is itself the point.
        lenient().when(request.getUserId()).thenReturn(TARGET_DB_ID);   // someone else

        UpdateUserResponse response =
                userService.updateUser(COMPANY_ID, "user001", request);

        assertThat(response.getStatusCode())
                .as("非マスターが他人を指定した場合はFORBIDDENで拒否されるべき")
                .isEqualTo(ResponseConstants.FORBIDDEN);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-US-03-11: 準正常 - リクエスト全体がSystem.outに出力される（要確認）")
    void updateUser_printsWholeRequestToStdout() {
        // NOTE: updateUser starts with System.out.println("UpdateUserRequest: " + request)
        // and handleMasterEditUser prints isPIC. The first prints the whole request object,
        // which carries name, email and phone number. Personal data should not reach
        // stdout, and the class already has @Slf4j for anything that must be logged.
        // Recorded here; the test itself only confirms the normal path still works.
        ParticipatingCompany company = companyMock(COMPANY_ID);
        stubRequestingMaster(company);

        ParticipatingUser target =
                userMock(TARGET_DB_ID, "user", "target@example.co.jp", company, null);
        UpdateUserRequest request = request();
        when(request.getUserId()).thenReturn(TARGET_DB_ID);
        when(userRepository.findById(TARGET_DB_ID)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        UpdateUserResponse response =
                userService.updateUser(COMPANY_ID, MASTER_USERNAME, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
    }

    // ==================================================================
    // 04. validateUpdateUser
    // ==================================================================

    @SuppressWarnings("unchecked")
    private Map<String, String> errorsOf(BaseResponse response) {
        // BaseResponse.validationError stores the field errors in its error map; read it
        // reflectively so the test does not depend on the accessor name.
        for (Class<?> c = response.getClass(); c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    try {
                        Map<String, String> m = (Map<String, String>) f.get(response);
                        return m == null ? Map.of() : m;
                    } catch (IllegalAccessException e) {
                        throw new AssertionError("error map not readable", e);
                    }
                }
            }
        }
        throw new AssertionError("no error map found on " + response.getClass().getName());
    }

    @Test
    @DisplayName("UT-US-04-01: 正常 - マスターが他ユーザーを有効な内容で更新する場合、検証成功を返す")
    void validateUpdateUser_validMasterRequest_returnsSuccess() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        stubRequestingMaster(company);

        ParticipatingUser target =
                userMock(TARGET_DB_ID, "user", "target@example.co.jp", company, null);
        UpdateUserRequest request = request();
        when(request.getUserId()).thenReturn(TARGET_DB_ID);
        when(userRepository.findById(TARGET_DB_ID)).thenReturn(Optional.of(target));

        BaseResponse response =
                userService.validateUpdateUser(COMPANY_ID, MASTER_USERNAME, request);

        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_VALIDATION_SUCCESS);
        assertThat(errorsOf(response)).isEmpty();
    }

    @Test
    @DisplayName("UT-US-04-02: 異常 - 会社が存在しない場合companyIdのエラーを返す")
    void validateUpdateUser_companyNotFound_returnsCompanyIdError() {
        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.empty());

        BaseResponse response =
                userService.validateUpdateUser(COMPANY_ID, MASTER_USERNAME, request());

        assertThat(errorsOf(response)).containsKey("companyId");
        verify(userRepository, never()).findByUsernameAndCompany_Id(anyString(), any());
    }

    @Test
    @DisplayName("UT-US-04-03: 異常 - 要求ユーザーが存在しない場合usernameのエラーを返す")
    void validateUpdateUser_requestingUserNotFound_returnsUsernameError() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.of(company));
        when(userRepository.findByUsernameAndCompany_Id(MASTER_USERNAME, COMPANY_DB_ID))
                .thenReturn(Optional.empty());

        BaseResponse response =
                userService.validateUpdateUser(COMPANY_ID, MASTER_USERNAME, request());

        assertThat(errorsOf(response)).containsKey("username");
    }

    @Test
    @DisplayName("UT-US-04-04: 異常 - 対象ユーザーが存在しない場合UserIdのエラーを返す")
    void validateUpdateUser_targetNotFound_returnsUserIdError() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        stubRequestingMaster(company);

        UpdateUserRequest request = request();
        when(request.getUserId()).thenReturn(9999);
        when(userRepository.findById(9999)).thenReturn(Optional.empty());

        BaseResponse response =
                userService.validateUpdateUser(COMPANY_ID, MASTER_USERNAME, request);

        assertThat(errorsOf(response)).containsKey("UserId");
    }

    @Test
    @DisplayName("UT-US-04-05: 異常 - 非マスターが他人のuserIdを指定した場合UserIdのFORBIDDENエラーを返す")
    void validateUpdateUser_nonMasterTargetsAnotherUser_returnsForbiddenError() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.of(company));
        ParticipatingUser regularUser =
                userMock(MASTER_DB_ID, "user", "me@example.co.jp", company, null);
        when(userRepository.findByUsernameAndCompany_Id("user001", COMPANY_DB_ID))
                .thenReturn(Optional.of(regularUser));

        UpdateUserRequest request = request();
        when(request.getUserId()).thenReturn(TARGET_DB_ID);

        BaseResponse response =
                userService.validateUpdateUser(COMPANY_ID, "user001", request);

        assertThat(errorsOf(response)).containsEntry("UserId", ResponseConstants.MSG_FORBIDDEN);
    }

    @Test
    @DisplayName("UT-US-04-06: 異常 - 非マスターが拠点変更を指定した場合siteIdのFORBIDDENエラーを返す")
    void validateUpdateUser_nonMasterRequestsSiteChange_returnsForbiddenError() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.of(company));
        ParticipatingUser regularUser =
                userMock(MASTER_DB_ID, "user", "me@example.co.jp", company, null);
        when(userRepository.findByUsernameAndCompany_Id("user001", COMPANY_DB_ID))
                .thenReturn(Optional.of(regularUser));

        UpdateUserRequest request = request();
        when(request.getSiteId()).thenReturn(SITE_DB_ID);

        BaseResponse response =
                userService.validateUpdateUser(COMPANY_ID, "user001", request);

        assertThat(errorsOf(response)).containsEntry("siteId", ResponseConstants.MSG_FORBIDDEN);
    }

    @Test
    @DisplayName("UT-US-04-07: 異常 - マスターが自身の拠点変更を指定した場合siteIdのBAD_REQUESTエラーを返す")
    void validateUpdateUser_masterChangesOwnSite_returnsBadRequestError() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        stubRequestingMaster(company);

        UpdateUserRequest request = request();
        when(request.getSiteId()).thenReturn(SITE_DB_ID);   // no userId → self

        BaseResponse response =
                userService.validateUpdateUser(COMPANY_ID, MASTER_USERNAME, request);

        assertThat(errorsOf(response))
                .containsEntry("siteId", ResponseConstants.MSG_BAD_REQUEST);
    }

    @Test
    @DisplayName("UT-US-04-08: 異常 - 移動先拠点が存在しない場合siteIdのエラーを返す")
    void validateUpdateUser_siteNotFound_returnsSiteIdError() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        stubRequestingMaster(company);

        ParticipatingUser target =
                userMock(TARGET_DB_ID, "user", "target@example.co.jp", company, null);
        UpdateUserRequest request = request();
        when(request.getUserId()).thenReturn(TARGET_DB_ID);
        when(request.getSiteId()).thenReturn(OTHER_SITE_DB_ID);
        when(userRepository.findById(TARGET_DB_ID)).thenReturn(Optional.of(target));
        when(siteRepository.findById(OTHER_SITE_DB_ID)).thenReturn(Optional.empty());

        BaseResponse response =
                userService.validateUpdateUser(COMPANY_ID, MASTER_USERNAME, request);

        assertThat(errorsOf(response))
                .containsEntry("siteId", ResponseConstants.MSG_SITE_NOT_FOUND);
    }

    @Test
    @DisplayName("UT-US-04-09: 異常 - 移動先拠点が他社のものである場合siteIdのFORBIDDENエラーを返す")
    void validateUpdateUser_siteFromOtherCompany_returnsForbiddenError() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        stubRequestingMaster(company);

        ParticipatingUser target =
                userMock(TARGET_DB_ID, "user", "target@example.co.jp", company, null);
        ParticipatingCompany otherCompany = companyMock(OTHER_COMPANY_ID);
        Site foreignSite = siteMock(OTHER_SITE_DB_ID, otherCompany, "z@example.co.jp");

        UpdateUserRequest request = request();
        when(request.getUserId()).thenReturn(TARGET_DB_ID);
        when(request.getSiteId()).thenReturn(OTHER_SITE_DB_ID);
        when(userRepository.findById(TARGET_DB_ID)).thenReturn(Optional.of(target));
        when(siteRepository.findById(OTHER_SITE_DB_ID)).thenReturn(Optional.of(foreignSite));

        BaseResponse response =
                userService.validateUpdateUser(COMPANY_ID, MASTER_USERNAME, request);

        assertThat(errorsOf(response)).containsEntry("siteId", ResponseConstants.MSG_FORBIDDEN);
    }

    @Test
    @DisplayName("UT-US-04-10: 異常 - 移動先拠点が上限5名に達している場合siteIdのLIMIT_EXCEEDEDエラーを返す")
    void validateUpdateUser_siteAtCapacity_returnsLimitExceededError() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        stubRequestingMaster(company);

        ParticipatingUser target =
                userMock(TARGET_DB_ID, "user", "target@example.co.jp", company, null);
        Site newSite = siteMock(OTHER_SITE_DB_ID, company, "z@example.co.jp");

        UpdateUserRequest request = request();
        when(request.getUserId()).thenReturn(TARGET_DB_ID);
        when(request.getSiteId()).thenReturn(OTHER_SITE_DB_ID);
        when(userRepository.findById(TARGET_DB_ID)).thenReturn(Optional.of(target));
        when(siteRepository.findById(OTHER_SITE_DB_ID)).thenReturn(Optional.of(newSite));
        when(userRepository.countByCompany_IdAndSite_Id(COMPANY_DB_ID, OTHER_SITE_DB_ID))
                .thenReturn((long) MAX_USERS_PER_SITE);

        BaseResponse response =
                userService.validateUpdateUser(COMPANY_ID, MASTER_USERNAME, request);

        assertThat(errorsOf(response))
                .containsEntry("siteId", ResponseConstants.MSG_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("UT-US-04-11: 準正常 - try/catchがなく想定外の例外はそのまま伝播する（要確認）")
    void validateUpdateUser_hasNoTryCatch_propagatesUnexpectedException() {
        // NOTE: updateUser wraps everything in try/catch and answers with
        // INTERNAL_SERVER_ERROR, but validateUpdateUser does not. isCompanyAligned calls
        // targetUser.getCompany().getCompanyId() unguarded, so a user row with no company
        // produces a raw NullPointerException here while the same data yields a clean 500
        // from updateUser. The inconsistency is the point, not the NPE itself.
        ParticipatingCompany company = companyMock(COMPANY_ID);
        stubRequestingMaster(company);

        ParticipatingUser orphan = mock(ParticipatingUser.class);
        lenient().when(orphan.getId()).thenReturn(TARGET_DB_ID);
        lenient().when(orphan.getCompany()).thenReturn(null);

        UpdateUserRequest request = request();
        when(request.getUserId()).thenReturn(TARGET_DB_ID);
        when(userRepository.findById(TARGET_DB_ID)).thenReturn(Optional.of(orphan));

        assertThrows(NullPointerException.class,
                () -> userService.validateUpdateUser(COMPANY_ID, MASTER_USERNAME, request));
    }

    // ==================================================================
    // 05. getUserProfileById
    // ==================================================================

    @Test
    @DisplayName("UT-US-05-01: 正常 - マスターが自社ユーザーのプロフィールを取得できる")
    void getUserProfileById_masterSameCompany_returnsProfile() {
        ParticipatingCompany company = companyMock(COMPANY_ID);
        ParticipatingUser user =
                userMock(TARGET_DB_ID, "user", "target@example.co.jp", company, null);
        when(userRepository.findById(TARGET_DB_ID)).thenReturn(Optional.of(user));

        UserProfileResponse response =
                userService.getUserProfileById(TARGET_DB_ID, "master", COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getProfile()).isNotNull();
        assertThat(response.getProfile().getCompany()).isNotNull();
        assertThat(response.getProfile().getCompany().getCompanyId()).isEqualTo(COMPANY_ID);
    }

    @Test
    @DisplayName("UT-US-05-02: 異常 - ユーザーが存在しない場合NOT_FOUNDを返す")
    void getUserProfileById_unknownUser_returnsNotFound() {
        when(userRepository.findById(9999)).thenReturn(Optional.empty());

        UserProfileResponse response =
                userService.getUserProfileById(9999, "master", COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getProfile()).isNull();
    }

    @Test
    @DisplayName("UT-US-05-03: 異常 - マスターが他社ユーザーを指定した場合FORBIDDENを返す")
    void getUserProfileById_masterOtherCompany_returnsForbidden() {
        ParticipatingCompany otherCompany = companyMock(OTHER_COMPANY_ID);
        ParticipatingUser foreignUser =
                userMock(TARGET_DB_ID, "user", "x@example.co.jp", otherCompany, null);
        when(userRepository.findById(TARGET_DB_ID)).thenReturn(Optional.of(foreignUser));

        UserProfileResponse response =
                userService.getUserProfileById(TARGET_DB_ID, "master", COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        assertThat(response.getProfile()).isNull();
    }

    @Test
    @DisplayName("UT-US-05-04: 正常 - adminは他社ユーザーのプロフィールも取得できる（設計どおり）")
    void getUserProfileById_adminCrossCompany_isAllowedByDesign() {
        // The operator company oversees every member company, so admin is deliberately
        // exempt from the company-scope check. Recorded so the exemption is visible.
        ParticipatingCompany otherCompany = companyMock(OTHER_COMPANY_ID);
        ParticipatingUser foreignUser =
                userMock(TARGET_DB_ID, "user", "x@example.co.jp", otherCompany, null);
        when(userRepository.findById(TARGET_DB_ID)).thenReturn(Optional.of(foreignUser));

        UserProfileResponse response =
                userService.getUserProfileById(TARGET_DB_ID, "admin", COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
    }

    @Test
    @DisplayName("UT-US-05-05: 正常 - 会社情報が未設定の場合companyはnullのままOKを返す")
    void getUserProfileById_userWithoutCompany_returnsOkWithNullCompany() {
        ParticipatingUser user = mock(ParticipatingUser.class);
        lenient().when(user.getId()).thenReturn(TARGET_DB_ID);
        lenient().when(user.getUsername()).thenReturn("user2");
        lenient().when(user.getCompany()).thenReturn(null);
        when(userRepository.findById(TARGET_DB_ID)).thenReturn(Optional.of(user));

        UserProfileResponse response =
                userService.getUserProfileById(TARGET_DB_ID, "admin", COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getProfile().getCompany()).isNull();
    }

    @Test
    @DisplayName("UT-US-05-06: 異常 - 想定外の例外はINTERNAL_SERVER_ERRORとして返る")
    void getUserProfileById_unexpectedException_returnsInternalServerError() {
        when(userRepository.findById(TARGET_DB_ID))
                .thenThrow(new RuntimeException("db down"));

        UserProfileResponse response =
                userService.getUserProfileById(TARGET_DB_ID, "master", COMPANY_ID);

        assertThat(response.getStatusCode())
                .isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("UT-US-05-07: 異常 - 未知のroleは会社スコープ検査を受けるべき【要修正】")
    void getUserProfileById_unknownRole_shouldStillBeCompanyScoped() {
        // Pattern B: the scope check runs only inside if ("master".equals(requestingRole)),
        // so every other role value reads any user of any company. That is intended for
        // admin and operator, but the shape is a denylist of one: a typo in the role claim,
        // or a role added later, silently inherits unrestricted cross-company read. The
        // check belongs on an allowlist of the roles that are meant to be exempt.
        ParticipatingCompany otherCompany = companyMock(OTHER_COMPANY_ID);
        ParticipatingUser foreignUser =
                userMock(TARGET_DB_ID, "user", "x@example.co.jp", otherCompany, null);
        when(userRepository.findById(TARGET_DB_ID)).thenReturn(Optional.of(foreignUser));

        UserProfileResponse response =
                userService.getUserProfileById(TARGET_DB_ID, "viewer", COMPANY_ID);

        assertThat(response.getStatusCode())
                .as("admin・operator以外のroleは他社ユーザーを参照できないべき")
                .isEqualTo(ResponseConstants.FORBIDDEN);
    }
}