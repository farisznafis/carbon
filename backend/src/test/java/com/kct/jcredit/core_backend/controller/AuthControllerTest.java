package com.kct.jcredit.core_backend.controller;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.AdminLoginRequest;
import com.kct.jcredit.core_backend.dto.request.LoginRequest;
import com.kct.jcredit.core_backend.dto.request.PasswordChangeRequest;
import com.kct.jcredit.core_backend.dto.response.AdminLoginResponse;
import com.kct.jcredit.core_backend.dto.response.LoginResponse;
import com.kct.jcredit.core_backend.dto.response.PasswordChangeResponse;
import com.kct.jcredit.core_backend.dto.response.UserProfileResponse;
import com.kct.jcredit.core_backend.dto.response.details.AdminResponse;
import com.kct.jcredit.core_backend.dto.response.details.CompanyInfoResponse;
import com.kct.jcredit.core_backend.dto.response.details.UserInfoResponse;
import com.kct.jcredit.core_backend.service.AuthService;
import com.kct.jcredit.core_backend.service.UserService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    //authServiceの偽物作成
    //実質的にはAuthServiceImpl
    @Mock
    private AuthService authService;

    //authServiceの偽物作成
    //実質的にはAuthServiceImpl
    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    //トークン作成を行うクラスの偽物
    @Mock
    private JwtUtil jwtUtil;

    //@Mockで作った偽物をAuthControllerに注入
    @InjectMocks
    private AuthController authController;

    private UserProfileResponse mockSuccessResponse;
    private UserInfoResponse mockUserInfo;
    private CompanyInfoResponse mockCompanyInfo;

    @BeforeEach
    void setUp() {
        // 会社情報のモック作成
        mockCompanyInfo = CompanyInfoResponse.builder()
            .companyId("C001")
            .nameJA("テスト株式会社")
            .nameFurigana("テストカブシキガイシャ")
            .nameEN("Test Company")
            .email("company@test.com")
            .phoneNumber("03-1234-5678")
            .fax("03-1234-5679")
            .corpNumber("1234567890123")
            .address("東京都渋谷区テスト1-2-3")
            .build();

        // ユーザー情報のモック作成
        mockUserInfo = UserInfoResponse.builder()
            .id("1")
            .username("testuser")
            .firstName("太郎")
            .firstNameKana("タロウ")
            .lastName("山田")
            .lastNameKana("ヤマダ")
            .email("testuser@example.com")
            .phoneNumber("090-1234-5678")
            .role("user")
            .siteId("S001")
            .company(mockCompanyInfo)
            .build();

        // 成功レスポンスのモック作成
        mockSuccessResponse = UserProfileResponse.builder()
            .profile(mockUserInfo)
            .build();
        mockSuccessResponse.setStatusCode(ResponseConstants.OK);
        mockSuccessResponse.setMessage(null);
    }

    @Test
    void userLogin_success() throws Exception {
        // まずはrequestにログイン情報をセット
        LoginRequest request = new LoginRequest();
        request.setCompanyId("C01");
        request.setIdentifier("ID01");
        request.setPassword("PW01");

        // resにログインレスポンスをセット
        LoginResponse res = new LoginResponse();
        res.setStatusCode(ResponseConstants.OK);
        res.setToken("token01");
        //res.setUser(UserInfoResponse.builder().username("username01").build());

        // authService.userLogin(request)の際、
        // thenReturn(res)の内容が返ってくる
        when(authService.userLogin(request)).thenReturn(res); // ← スタブ


        ResponseEntity<LoginResponse> response = authController.userLogin(request);

        //テストケースが通ってるかの確認なので消してもいい↓
        System.out.println(request);
        

        assertEquals(HttpStatus.OK, response.getStatusCode(),"NG");
        assertEquals(res, response.getBody(), "ログインが失敗しました");
    }

    @Test
    void userLogin_invalidCredentials_returns401() throws Exception {
        // まずはrequestにログイン情報をセット
        LoginRequest request = new LoginRequest();
        request.setCompanyId("C01");
        request.setIdentifier("ID01");
        request.setPassword("PW01");

        // resにログインレスポンスをセット
        LoginResponse res = new LoginResponse();
        res.setStatusCode("INVALID_CREDENTIALS");
        res.setToken("token01");
        //res.setUser(UserInfoResponse.builder().username("username01").build());

        // authService.userLogin(request)の際、
        // thenReturn(res)の内容が返ってくる
        when(authService.userLogin(request)).thenReturn(res);

        
        ResponseEntity<LoginResponse> response = authController.userLogin(request);

        //テストケースが通ってるかの確認なので消してもいい↓
        System.out.println(request);
        System.out.println("derunoka");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(res, response.getBody());
    }

    @Test
    void adminLogin_success() throws Exception {
        // まずはrequestにログイン情報をセット
        AdminLoginRequest request = new AdminLoginRequest();
        request.setIdentifier("ID01");
        request.setPassword("PW01");

        // resにログインレスポンスをセット
        AdminLoginResponse res = new AdminLoginResponse();
        res.setStatusCode(ResponseConstants.OK);
        res.setToken("token01");
        res.setAdmin(AdminResponse.builder().username("username01").build());

        // authService.userLogin(request)の際、
        // thenReturn(res)の内容が返ってくる
        when(authService.operatorLogin(request)).thenReturn(res); // ← スタブ


        ResponseEntity<AdminLoginResponse> response = authController.adminLogin(request);

        //テストケースが通ってるかの確認なので消してもいい↓
        System.out.println(request);
        

        assertEquals(HttpStatus.OK, response.getStatusCode(),"NG");
        assertEquals(res, response.getBody(), "ログインが失敗しました");
    }

 // ============================================
    // getUserProfile のテスト
    // ============================================

    @Test
    void getUserProfile_succes() {
        // Given
        String username = "testuser";
        when(authentication.getName()).thenReturn(username);
        when(userService.getUserProfile(username)).thenReturn(mockSuccessResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                       .thenReturn(HttpStatus.OK);

            // When
            ResponseEntity<UserProfileResponse> response = authController.getUserProfile(authentication);

            // Then
            assertNotNull(response, "レスポンスがnullです");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "ステータスコードがOKではありません");
            
            assertNotNull(response.getBody(), "レスポンスボディがnullです");
            assertEquals(ResponseConstants.OK, response.getBody().getStatusCode(), 
                "レスポンスのstatusCodeがOKではありません");
            
            // プロフィール情報の検証
            assertNotNull(response.getBody().getProfile(), "プロフィール情報がnullです");
            UserInfoResponse profile = response.getBody().getProfile();
            
            assertEquals("1", profile.getId(), "ユーザーIDが一致しません");
            assertEquals("testuser", profile.getUsername(), "ユーザー名が一致しません");
            assertEquals("太郎", profile.getFirstName(), "名が一致しません");
            assertEquals("タロウ", profile.getFirstNameKana(), "名（カナ）が一致しません");
            assertEquals("山田", profile.getLastName(), "姓が一致しません");
            assertEquals("ヤマダ", profile.getLastNameKana(), "姓（カナ）が一致しません");
            assertEquals("testuser@example.com", profile.getEmail(), "メールアドレスが一致しません");
            assertEquals("090-1234-5678", profile.getPhoneNumber(), "電話番号が一致しません");
            assertEquals("user", profile.getRole(), "ロールが一致しません");
            assertEquals("S001", profile.getSiteId(), "サイトIDが一致しません");

            // 会社情報の検証
            assertNotNull(profile.getCompany(), "会社情報がnullです");
            CompanyInfoResponse company = profile.getCompany();
            
            assertEquals("C001", company.getCompanyId(), "会社IDが一致しません");
            assertEquals("テスト株式会社", company.getNameJA(), "会社名（日本語）が一致しません");
            assertEquals("テストカブシキガイシャ", company.getNameFurigana(), "会社名（フリガナ）が一致しません");
            assertEquals("Test Company", company.getNameEN(), "会社名（英語）が一致しません");
            assertEquals("company@test.com", company.getEmail(), "会社メールアドレスが一致しません");
            assertEquals("03-1234-5678", company.getPhoneNumber(), "会社電話番号が一致しません");

            // モックメソッドの呼び出し検証
            verify(authentication, times(1)).getName();
            verify(userService, times(1)).getUserProfile(username);

        }
    }

    @Test
    void changePassword_success_self() {
        // Given - 自分自身のパスワード変更
        String username = "testuser";
        String token = "test-jwt-token";
        String authHeader = "Bearer " + token; 
        
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(null);  // null = 自分自身のパスワード変更
        request.setCurrentPassword("CurrentPass123");
        request.setNewPassword("NewPass123");

        PasswordChangeResponse expectedResponse = new PasswordChangeResponse();
        expectedResponse.setStatusCode(ResponseConstants.OK);
        expectedResponse.setMessage("パスワードが正常に変更されました。");

        when(authentication.getName()).thenReturn(username);
        when(authService.changePassword(eq(username), eq(token), any(PasswordChangeRequest.class)))
                .thenReturn(expectedResponse);

        try (MockedStatic<ResponseUtil> mockedStatic = mockStatic(ResponseUtil.class)) {
            mockedStatic.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
                       .thenReturn(HttpStatus.OK);

            // When
            ResponseEntity<PasswordChangeResponse> response = 
                    authController.changePassword(authentication, authHeader, request);

            // Then
            assertNotNull(response, "レスポンスがnullです");
            assertEquals(HttpStatus.OK, response.getStatusCode(), "ステータスコードがOKではありません");
            
            assertNotNull(response.getBody(), "レスポンスボディがnullです");
            assertEquals(ResponseConstants.OK, response.getBody().getStatusCode(), 
                "レスポンスのstatusCodeがOKではありません");
            assertEquals("パスワードが正常に変更されました。", response.getBody().getMessage(),
                "メッセージが一致しません");

            // モックメソッドの呼び出し検証
            verify(authentication, times(1)).getName();
            verify(authService, times(1)).changePassword(eq(username), eq(token), any(PasswordChangeRequest.class));
        }
    }

}
