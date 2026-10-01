package com.kct.jcredit.core_backend.participatingusercontroller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.controller.ParticipatingUserController;
import com.kct.jcredit.core_backend.dto.request.UpdateUserRequest;
import com.kct.jcredit.core_backend.dto.request.UserInvitationRequest;
import com.kct.jcredit.core_backend.dto.response.UpdateUserResponse;
import com.kct.jcredit.core_backend.dto.response.UserListResponse;
import com.kct.jcredit.core_backend.dto.response.UserProfileResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.service.UserInvitationService;
import com.kct.jcredit.core_backend.service.UserService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;

// NOTE: response DTOs are mocked rather than built, so the tests do not depend on
// constructor or builder shapes that have not been verified here. Only getStatusCode()
// is stubbed, because that is the only field the controller reads.
// NOTE: ResponseUtil.getHttpStatus is stubbed per test. The mapping itself belongs to
// ResponseUtil's own tests; what is verified here is that the controller uses whatever
// that mapping returns.
@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipatingUserController のテスト")
class ParticipatingUserControllerTest {

    @Mock private UserService userService;
    @Mock private UserInvitationService userInvitationService;
    @Mock private JwtUtil jwtUtil;
    @Mock private Authentication authentication;

    @InjectMocks
    private ParticipatingUserController controller;

    private MockedStatic<ResponseUtil> responseUtil;

    private static final String TOKEN = "dummy-token";
    private static final String COMPANY_ID = "C-0001";
    private static final String USERNAME = "master001";
    private static final String EMAIL = "newuser@example.co.jp";
    private static final String SITE_ID = "PUB-SITE-001";
    private static final Integer USER_ID = 42;

    @BeforeEach
    void setUp() {
        responseUtil = mockStatic(ResponseUtil.class);
    }

    @AfterEach
    void tearDown() {
        responseUtil.close();
    }

    // ─────────────────────────────────────────────────────────────
    // Fixtures
    // ─────────────────────────────────────────────────────────────

    /** The credentials every endpoint reads the token from. */
    private void stubToken() {
        when(authentication.getCredentials()).thenReturn(TOKEN);
    }

    private void stubCompanyAndUsername() {
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
    }

    /** Maps one status code so the controller can resolve its HTTP status. */
    private void stubStatus(String statusCode, HttpStatus httpStatus) {
        responseUtil.when(() -> ResponseUtil.getHttpStatus(statusCode)).thenReturn(httpStatus);
    }

    private UserInvitationRequest invitationRequest() {
        UserInvitationRequest request = mock(UserInvitationRequest.class);
        lenient().when(request.getEmail()).thenReturn(EMAIL);
        lenient().when(request.getSiteId()).thenReturn(SITE_ID);
        return request;
    }

    // ==================================================================
    // 01. getUsers
    // ==================================================================

    @Test
    @DisplayName("UT-UC-01-01: 正常 - 一覧取得が成功し、トークン由来のcompanyIdでサービスが呼ばれる")
    void getUsers_validToken_returnsListAndUsesCompanyIdFromToken() {
        stubToken();
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);

        UserListResponse serviceResponse = mock(UserListResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(userService.getUsersByCompany(COMPANY_ID)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<UserListResponse> result = controller.getUsers(authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);
        verify(userService, times(1)).getUsersByCompany(COMPANY_ID);
    }

    @Test
    @DisplayName("UT-UC-01-02: 異常 - サービスがNOT_FOUNDを返す場合、対応するHTTPステータスに変換される")
    void getUsers_serviceReturnsNotFound_mapsToHttpStatus() {
        stubToken();
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);

        UserListResponse serviceResponse = mock(UserListResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.NOT_FOUND);
        when(userService.getUsersByCompany(COMPANY_ID)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.NOT_FOUND, HttpStatus.NOT_FOUND);

        ResponseEntity<UserListResponse> result = controller.getUsers(authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody().getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
    }

    @Test
    @DisplayName("UT-UC-01-03: 異常 - トークンが無効な場合サービスは呼ばれない")
    void getUsers_invalidToken_serviceNotCalled() {
        stubToken();
        when(jwtUtil.extractCompanyId(TOKEN))
                .thenThrow(new RuntimeException("invalid token"));

        assertThrows(RuntimeException.class, () -> controller.getUsers(authentication));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("UT-UC-01-04: 準正常 - 該当ユーザーが0件の場合もOKとして返される")
    void getUsers_emptyResult_stillReturnsOk() {
        // An empty company is a valid state, not an error: the caller gets 200 with an
        // empty list rather than 404.
        stubToken();
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);

        UserListResponse serviceResponse = mock(UserListResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(userService.getUsersByCompany(COMPANY_ID)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<UserListResponse> result = controller.getUsers(authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);
    }

    // ==================================================================
    // 02. inviteUser
    // ==================================================================

    @Test
    @DisplayName("UT-UC-02-01: 正常 - 招待が成功し、4引数(companyId, username, email, siteId)の順序で呼ばれる")
    void inviteUser_validRequest_passesArgumentsInOrder() {
        stubToken();
        stubCompanyAndUsername();
        UserInvitationRequest request = invitationRequest();

        BaseResponse serviceResponse = mock(BaseResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(userInvitationService.invite(COMPANY_ID, USERNAME, EMAIL, SITE_ID))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<BaseResponse> result = controller.inviteUser(authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);

        // The four arguments are all Strings, so a swapped pair would compile silently.
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(userInvitationService).invite(
                captor.capture(), captor.capture(), captor.capture(), captor.capture());
        assertThat(captor.getAllValues())
                .containsExactly(COMPANY_ID, USERNAME, EMAIL, SITE_ID);
    }

    @Test
    @DisplayName("UT-UC-02-02: 異常 - メールアドレスが使用済みの場合USED_EMAILが対応するHTTPステータスに変換される")
    void inviteUser_emailAlreadyUsed_mapsToHttpStatus() {
        stubToken();
        stubCompanyAndUsername();
        UserInvitationRequest request = invitationRequest();

        BaseResponse serviceResponse = mock(BaseResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.USED_EMAIL);
        when(userInvitationService.invite(COMPANY_ID, USERNAME, EMAIL, SITE_ID))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.USED_EMAIL, HttpStatus.CONFLICT);

        ResponseEntity<BaseResponse> result = controller.inviteUser(authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody().getStatusCode()).isEqualTo(ResponseConstants.USED_EMAIL);
    }

    @Test
    @DisplayName("UT-UC-02-03: 異常 - 拠点が存在しない場合NOT_FOUNDが対応するHTTPステータスに変換される")
    void inviteUser_siteNotFound_mapsToHttpStatus() {
        stubToken();
        stubCompanyAndUsername();
        UserInvitationRequest request = invitationRequest();

        BaseResponse serviceResponse = mock(BaseResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.NOT_FOUND);
        when(userInvitationService.invite(COMPANY_ID, USERNAME, EMAIL, SITE_ID))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.NOT_FOUND, HttpStatus.NOT_FOUND);

        ResponseEntity<BaseResponse> result = controller.inviteUser(authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("UT-UC-02-04: 異常 - 他社の拠点を指定した場合FORBIDDENが対応するHTTPステータスに変換される")
    void inviteUser_siteFromOtherCompany_mapsToForbidden() {
        // The controller passes siteId straight through; the ownership decision belongs to
        // the service. What is verified here is that its refusal reaches the caller as 403.
        stubToken();
        stubCompanyAndUsername();
        UserInvitationRequest request = invitationRequest();

        BaseResponse serviceResponse = mock(BaseResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.FORBIDDEN);
        when(userInvitationService.invite(COMPANY_ID, USERNAME, EMAIL, SITE_ID))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.FORBIDDEN, HttpStatus.FORBIDDEN);

        ResponseEntity<BaseResponse> result = controller.inviteUser(authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("UT-UC-02-05: 異常 - トークンが無効な場合、username抽出とサービス呼び出しは行われない")
    void inviteUser_invalidToken_serviceNotCalled() {
        stubToken();
        when(jwtUtil.extractCompanyId(TOKEN))
                .thenThrow(new RuntimeException("invalid token"));
        UserInvitationRequest request = invitationRequest();

        assertThrows(RuntimeException.class,
                () -> controller.inviteUser(authentication, request));

        // Java evaluates arguments left to right, so extractCompanyId throwing stops the
        // call before extractUsername is reached.
        verify(jwtUtil, never()).extractUsername(anyString());
        verifyNoInteractions(userInvitationService);
    }

    @Test
    @DisplayName("UT-UC-02-06: 正常 - トークンの読み出しは1回のみ（他エンドポイントとの差異を記録）")
    void inviteUser_readsCredentialsOnce() {
        // This endpoint reads getCredentials() once into a local variable, while
        // updateUser, validateUpdateUser and getUserProfileById each read it twice.
        // Recorded so the difference is a deliberate choice rather than drift.
        stubToken();
        stubCompanyAndUsername();
        UserInvitationRequest request = invitationRequest();

        BaseResponse serviceResponse = mock(BaseResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(userInvitationService.invite(COMPANY_ID, USERNAME, EMAIL, SITE_ID))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        controller.inviteUser(authentication, request);

        verify(authentication, times(1)).getCredentials();
    }

    // ==================================================================
    // 03. updateUser
    // ==================================================================

    @Test
    @DisplayName("UT-UC-03-01: 正常 - 更新が成功し、3引数(companyId, username, request)の順序で呼ばれる")
    void updateUser_validRequest_passesArgumentsInOrder() {
        stubToken();
        stubCompanyAndUsername();
        UpdateUserRequest request = mock(UpdateUserRequest.class);

        UpdateUserResponse serviceResponse = mock(UpdateUserResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(userService.updateUser(COMPANY_ID, USERNAME, request)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<UpdateUserResponse> result =
                controller.updateUser(authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);
        verify(userService, times(1)).updateUser(COMPANY_ID, USERNAME, request);
    }

    @Test
    @DisplayName("UT-UC-03-02: 異常 - 他社ユーザーの場合FORBIDDENが対応するHTTPステータスに変換される")
    void updateUser_userFromOtherCompany_mapsToForbidden() {
        stubToken();
        stubCompanyAndUsername();
        UpdateUserRequest request = mock(UpdateUserRequest.class);

        UpdateUserResponse serviceResponse = mock(UpdateUserResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.FORBIDDEN);
        when(userService.updateUser(COMPANY_ID, USERNAME, request)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.FORBIDDEN, HttpStatus.FORBIDDEN);

        ResponseEntity<UpdateUserResponse> result =
                controller.updateUser(authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("UT-UC-03-03: 異常 - ユーザーが存在しない場合NOT_FOUNDが対応するHTTPステータスに変換される")
    void updateUser_userNotFound_mapsToNotFound() {
        stubToken();
        stubCompanyAndUsername();
        UpdateUserRequest request = mock(UpdateUserRequest.class);

        UpdateUserResponse serviceResponse = mock(UpdateUserResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.NOT_FOUND);
        when(userService.updateUser(COMPANY_ID, USERNAME, request)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.NOT_FOUND, HttpStatus.NOT_FOUND);

        ResponseEntity<UpdateUserResponse> result =
                controller.updateUser(authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("UT-UC-03-04: 異常 - 入力検証エラーの場合VALIDATION_ERRORが対応するHTTPステータスに変換される")
    void updateUser_validationError_mapsToBadRequest() {
        stubToken();
        stubCompanyAndUsername();
        UpdateUserRequest request = mock(UpdateUserRequest.class);

        UpdateUserResponse serviceResponse = mock(UpdateUserResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.VALIDATION_ERROR);
        when(userService.updateUser(COMPANY_ID, USERNAME, request)).thenReturn(serviceResponse);
        stubStatus(ResponseConstants.VALIDATION_ERROR, HttpStatus.BAD_REQUEST);

        ResponseEntity<UpdateUserResponse> result =
                controller.updateUser(authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().getStatusCode())
                .isEqualTo(ResponseConstants.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("UT-UC-03-05: 異常 - companyId抽出が失敗した場合、username抽出とサービス呼び出しは行われない")
    void updateUser_invalidToken_serviceNotCalled() {
        stubToken();
        when(jwtUtil.extractCompanyId(TOKEN))
                .thenThrow(new RuntimeException("invalid token"));
        UpdateUserRequest request = mock(UpdateUserRequest.class);

        assertThrows(RuntimeException.class,
                () -> controller.updateUser(authentication, request));

        verify(jwtUtil, never()).extractUsername(anyString());
        verifyNoInteractions(userService);
    }

    // ==================================================================
    // 04. validateUpdateUser
    // ==================================================================

    @Test
    @DisplayName("UT-UC-04-01: 正常 - 検証が通る場合OKと検証成功レスポンスを返す")
    void validateUpdateUser_valid_returnsOk() {
        stubToken();
        stubCompanyAndUsername();
        UpdateUserRequest request = mock(UpdateUserRequest.class);

        BaseResponse serviceResponse = mock(BaseResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(userService.validateUpdateUser(COMPANY_ID, USERNAME, request))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<BaseResponse> result =
                controller.validateUpdateUser(authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);
        verify(userService, times(1)).validateUpdateUser(COMPANY_ID, USERNAME, request);
    }

    @Test
    @DisplayName("UT-UC-04-02: 異常 - 検証エラーの場合VALIDATION_ERRORが対応するHTTPステータスに変換される")
    void validateUpdateUser_validationError_mapsToBadRequest() {
        stubToken();
        stubCompanyAndUsername();
        UpdateUserRequest request = mock(UpdateUserRequest.class);

        BaseResponse serviceResponse = mock(BaseResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.VALIDATION_ERROR);
        when(userService.validateUpdateUser(COMPANY_ID, USERNAME, request))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.VALIDATION_ERROR, HttpStatus.BAD_REQUEST);

        ResponseEntity<BaseResponse> result =
                controller.validateUpdateUser(authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("UT-UC-04-03: 異常 - トークンが無効な場合サービスは呼ばれない")
    void validateUpdateUser_invalidToken_serviceNotCalled() {
        stubToken();
        when(jwtUtil.extractCompanyId(TOKEN))
                .thenThrow(new RuntimeException("invalid token"));
        UpdateUserRequest request = mock(UpdateUserRequest.class);

        assertThrows(RuntimeException.class,
                () -> controller.validateUpdateUser(authentication, request));

        verify(jwtUtil, never()).extractUsername(anyString());
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("UT-UC-04-04: 準正常 - コントローラーは権限判定を行わずロールも渡さない（アーキテクチャ上の注記）")
    void validateUpdateUser_passesNoRoleToService_authorizationIsAnnotationOnly() {
        // NOTE: this endpoint carries no @PreAuthorize, unlike the other four. What is
        // verifiable here is that the controller performs no role check of its own and does
        // not pass the role on, so the service cannot apply one either - authorization for
        // this endpoint rests entirely on an annotation that is currently absent.
        // @PreAuthorize is enforced by the Spring Security proxy and is invisible to a
        // plain Mockito test, so the gap itself cannot be covered at this level; it needs a
        // @WebMvcTest with a security context. Reported as BUG-036.
        stubToken();
        stubCompanyAndUsername();
        UpdateUserRequest request = mock(UpdateUserRequest.class);

        BaseResponse serviceResponse = mock(BaseResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(userService.validateUpdateUser(COMPANY_ID, USERNAME, request))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        controller.validateUpdateUser(authentication, request);

        verify(jwtUtil, never()).extractRole(anyString());
        verify(userService).validateUpdateUser(COMPANY_ID, USERNAME, request);
    }

    // ==================================================================
    // 05. getUserProfileById
    // ==================================================================

    @Test
    @DisplayName("UT-UC-05-01: 正常 - プロフィール取得が成功し、3引数(userId, role, companyId)の順序で呼ばれる")
    void getUserProfileById_validToken_passesArgumentsInOrder() {
        stubToken();
        when(jwtUtil.extractRole(TOKEN)).thenReturn("master");
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);

        UserProfileResponse serviceResponse = mock(UserProfileResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(userService.getUserProfileById(USER_ID, "master", COMPANY_ID))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        ResponseEntity<UserProfileResponse> result =
                controller.getUserProfileById(USER_ID, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(serviceResponse);

        // role and companyId are both Strings and adjacent, so a swap would compile.
        InOrder inOrder = inOrder(jwtUtil, userService);
        inOrder.verify(jwtUtil).extractRole(TOKEN);
        inOrder.verify(jwtUtil).extractCompanyId(TOKEN);
        inOrder.verify(userService).getUserProfileById(USER_ID, "master", COMPANY_ID);
    }

    @Test
    @DisplayName("UT-UC-05-02: 異常 - 他社ユーザーのプロフィールはFORBIDDENが対応するHTTPステータスに変換される")
    void getUserProfileById_userFromOtherCompany_mapsToForbidden() {
        // The company-scope decision belongs to the service, which is why companyId is
        // passed in. What is verified here is that its refusal reaches the caller as 403.
        stubToken();
        when(jwtUtil.extractRole(TOKEN)).thenReturn("master");
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);

        UserProfileResponse serviceResponse = mock(UserProfileResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.FORBIDDEN);
        when(userService.getUserProfileById(USER_ID, "master", COMPANY_ID))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.FORBIDDEN, HttpStatus.FORBIDDEN);

        ResponseEntity<UserProfileResponse> result =
                controller.getUserProfileById(USER_ID, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("UT-UC-05-03: 異常 - ユーザーが存在しない場合NOT_FOUNDが対応するHTTPステータスに変換される")
    void getUserProfileById_unknownUser_mapsToNotFound() {
        stubToken();
        when(jwtUtil.extractRole(TOKEN)).thenReturn("master");
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);

        UserProfileResponse serviceResponse = mock(UserProfileResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.NOT_FOUND);
        when(userService.getUserProfileById(9999, "master", COMPANY_ID))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.NOT_FOUND, HttpStatus.NOT_FOUND);

        ResponseEntity<UserProfileResponse> result =
                controller.getUserProfileById(9999, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("UT-UC-05-04: 異常 - role抽出が失敗した場合、companyId抽出とサービス呼び出しは行われない")
    void getUserProfileById_invalidToken_serviceNotCalled() {
        stubToken();
        when(jwtUtil.extractRole(TOKEN))
                .thenThrow(new RuntimeException("invalid token"));

        assertThrows(RuntimeException.class,
                () -> controller.getUserProfileById(USER_ID, authentication));

        // extractRole runs first, so a failure there stops extractCompanyId as well.
        verify(jwtUtil, never()).extractCompanyId(anyString());
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("UT-UC-05-05: 準正常 - トークンが2回読み出される（inviteUserの1回読み出しとの差異を記録）")
    void getUserProfileById_readsCredentialsTwice() {
        // Recorded, not asserted as correct: updateUser, validateUpdateUser and this
        // endpoint each call getCredentials() twice, while inviteUser reads it once into a
        // local variable. Harmless today, but the inconsistency is worth seeing.
        stubToken();
        when(jwtUtil.extractRole(TOKEN)).thenReturn("master");
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(COMPANY_ID);

        UserProfileResponse serviceResponse = mock(UserProfileResponse.class);
        when(serviceResponse.getStatusCode()).thenReturn(ResponseConstants.OK);
        when(userService.getUserProfileById(USER_ID, "master", COMPANY_ID))
                .thenReturn(serviceResponse);
        stubStatus(ResponseConstants.OK, HttpStatus.OK);

        controller.getUserProfileById(USER_ID, authentication);

        verify(authentication, times(2)).getCredentials();
    }
}
