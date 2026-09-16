// package com.kct.jcredit.core_backend.controller;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.ArgumentMatchers.eq;
// import static org.mockito.Mockito.lenient;
// import static org.mockito.Mockito.mockStatic;
// import static org.mockito.Mockito.times;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;

// import java.util.ArrayList;
// import java.util.List;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.MockedStatic;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.mockito.junit.jupiter.MockitoSettings;
// import org.mockito.quality.Strictness;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.core.Authentication;

// import com.kct.jcredit.core_backend.constant.ResponseConstants;
// import com.kct.jcredit.core_backend.dto.request.CreateUserRequest;
// import com.kct.jcredit.core_backend.dto.request.DeleteUserSite;
// import com.kct.jcredit.core_backend.dto.request.UpdateUserRequest;
// import com.kct.jcredit.core_backend.dto.response.CreateUserResponse;
// import com.kct.jcredit.core_backend.dto.response.DeleteUserSiteResponse;
// import com.kct.jcredit.core_backend.dto.response.UpdateUserResponse;
// import com.kct.jcredit.core_backend.dto.response.UserListResponse;
// import com.kct.jcredit.core_backend.dto.response.details.SiteResponse;
// import com.kct.jcredit.core_backend.dto.response.details.UserInfoResponse;
// import com.kct.jcredit.core_backend.service.UserService;
// import com.kct.jcredit.core_backend.util.JwtUtil;
// import com.kct.jcredit.core_backend.util.ResponseUtil;

// @ExtendWith(MockitoExtension.class)
// @MockitoSettings(strictness = Strictness.LENIENT)
// @DisplayName("ParticipatingUserController 単体テスト")
// class ParticipatingUserControllerTest {

//     @Mock
//     private UserService userService;

//     @Mock
//     private JwtUtil jwtUtil;

//     @Mock
//     private Authentication authentication;

//     @InjectMocks
//     private ParticipatingUserController participatingUserController;

//     private static final String TEST_TOKEN = "test.jwt.token";
//     private static final String TEST_COMPANY_ID = "company123";
//     private static final String TEST_USERNAME = "testuser";

//     @BeforeEach
//     void setUp() {
//         lenient().when(authentication.getCredentials()).thenReturn((Object) TEST_TOKEN);
//         lenient().when(jwtUtil.extractCompanyId(anyString())).thenReturn(TEST_COMPANY_ID);
//     }

//     //テストケース: マスターユーザーが同じ企業の全ユーザー情報を取得できること
//     @Test
//     @DisplayName("マスターが同企業のユーザ情報すべて取得できること")
//     void testGetUsers_Success() {
        
//         // ResponseUtilの静的メソッドをモック化するためのtry-with-resources
//         try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {

//                 // === Arrange（準備フェーズ）===
            
//                 // テスト用のユーザーリストを作成
//                 // Arrange
//                 List<UserInfoResponse> userList = new ArrayList<>();

//                 // ユーザー1: 山田太郎
//                 UserInfoResponse user1 = UserInfoResponse.builder()
//                         .id("1")
//                         .firstName("山田")
//                         .firstNameKana("ヤマダ")
//                         .lastName("太郎")
//                         .lastNameKana("タロウ")
//                         .email("yamada@test.com")
//                         .role("user")
//                         .siteName("東京本社")
//                         .build();
        
//                 // ユーザー2: 田中花子
//                 UserInfoResponse user2 = UserInfoResponse.builder()
//                         .id("2")
//                         .firstName("田中")
//                         .firstNameKana("タナカ")
//                         .lastName("花子")
//                         .lastNameKana("ハナコ")
//                         .email("tanaka@test.com")
//                         .role("user")
//                         .siteName("大阪支社")
//                         .build();
        
//                 // リストに2人のユーザーを追加
//                 userList.add(user1);
//                 userList.add(user2);

//                 // モックレスポンスの作成（サービス層が返すべきレスポンス）
//                 UserListResponse mockResponse = new UserListResponse();
//                         mockResponse.setStatusCode(ResponseConstants.OK);
//                         mockResponse.setMessage("ユーザー一覧取得成功");
//                         mockResponse.setUsers(userList);

//                 // ユーザーサービスのモック設定: getUsersByCompanyが呼ばれたらmockResponseを返す
//                 when(userService.getUsersByCompany(TEST_COMPANY_ID)).thenReturn(mockResponse);

//                 // ResponseUtilの静的メソッドをモック: OKステータスコードをHttpStatus.OKに変換
//                 mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
//                                 .thenReturn(HttpStatus.OK);

//                 // === Act（実行フェーズ）===
//                 // コントローラーのgetUsersメソッドを実行
//                 ResponseEntity<UserListResponse> response = participatingUserController.getUsers(authentication);

//                 // === Assert（検証フェーズ）===
            
//                 // レスポンスがnullでないことを確認
//                 assertThat(response).isNotNull();

//                 // HTTPステータスが200 OKであることを確認
//                 assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

//                 // レスポンスボディがnullでないことを確認
//                 assertThat(response.getBody()).isNotNull();

//                 // レスポンスボディのステータスコードがOKであることを確認
//                 assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.OK);

//                 // ユーザーリストのサイズが2であることを確認
//                 assertThat(response.getBody().getUsers()).hasSize(2);

//                 // 1人目のユーザーのメールアドレスを確認
//                 assertThat(response.getBody().getUsers().get(0).getEmail()).isEqualTo("yamada@test.com");
//                 // 2人目のユーザーのメールアドレスを確認
//                 assertThat(response.getBody().getUsers().get(1).getEmail()).isEqualTo("tanaka@test.com");

//                 // === Verify（呼び出し検証フェーズ）===
            
//                 // jwtUtil.extractCompanyIdが1回だけ呼ばれたことを確認
//                 verify(jwtUtil, times(1)).extractCompanyId(eq(TEST_TOKEN));
                
//                 // userService.getUsersByCompanyが1回だけ呼ばれたことを確認
//                 verify(userService, times(1)).getUsersByCompany(eq(TEST_COMPANY_ID));
//         }
//     }

//     @Test
//     @DisplayName("ユーザと拠点のつながりを切ることができること")
//     void testDeleteUserSite_Success() {
//         try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
//                 // === テストデータの準備 ===
//                 // リクエストオブジェクトを作成し、削除対象のユーザIDと拠点IDを設定
//                 // Arrange
//                 DeleteUserSite request = new DeleteUserSite();
//                         request.setUserId("123");
//                         request.setSiteId("456");

//                 // サービス層から返却される期待レスポンスをモック作成
//                 DeleteUserSiteResponse mockResponse = new DeleteUserSiteResponse();
//                         mockResponse.setStatusCode(ResponseConstants.OK);
//                         mockResponse.setMessage("ユーザーと拠点の紐付けを削除しました");

//                 // === モックの振る舞いを定義 ===
//                 // userServiceのメソッド呼び出しに対してモックレスポンスを返却
//                 when(userService.removeUserSiteConnection(any(DeleteUserSite.class))).thenReturn(mockResponse);
//                 // ResponseUtilのスタティックメソッドをモック化し、HTTPステータスを返却
//                 mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
//                                 .thenReturn(HttpStatus.OK);

//                 // === テスト対象メソッドの実行 ===
//                 ResponseEntity<DeleteUserSiteResponse> response = 
//                         participatingUserController.deleteUserSite(request);

//                 // === 検証 ===
//                 // レスポンスが正常に返却されることを確認
//                 assertThat(response).isNotNull();
//                 assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//                 // レスポンスボディの内容を検証
//                 assertThat(response.getBody()).isNotNull();
//                 assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.OK);
//                 assertThat(response.getBody().getMessage()).isEqualTo("ユーザーと拠点の紐付けを削除しました");

//                 // サービスメソッドが1回呼ばれたことを確認
//                 verify(userService, times(1)).removeUserSiteConnection(any(DeleteUserSite.class));
//         }
//     }

//     @Test
//     @DisplayName("ユーザ登録が正常にできること")
//     void testCreateUser_Success() {
//         try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
//                 // === 認証情報のモック設定 ===
//                 when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);

//                 // === テストデータの準備 ===
//                 // 新規ユーザ作成のリクエストデータを設定
//                 // Arrange
//                 CreateUserRequest request = new CreateUserRequest();
//                         request.setUsername("newuser");
//                         request.setFirstName("次郎");
//                         request.setFirstNameKana("ジロウ");
//                         request.setLastName("佐藤");
//                         request.setLastNameKana("サトウ");
//                         request.setEmail("sato@example.com");
//                         request.setPhoneNumber("090-1234-5678");
//                         request.setUserPosition("一般社員");
//                         request.setPassword("password123");
//                         request.setSiteId(1);

//                 // 作成されたユーザ情報のレスポンスデータを準備
//                 UserInfoResponse userInfo = UserInfoResponse.builder()
//                         .id("10")
//                         .username("newuser")
//                         .firstName("次郎")
//                         .firstNameKana("ジロウ")
//                         .lastName("佐藤")
//                         .lastNameKana("サトウ")
//                         .email("sato@example.com")
//                         .phoneNumber("090-1234-5678")
//                         .role("user")
//                         .build();

//                 // 紐付けられた拠点情報のレスポンスデータを準備
//                 SiteResponse siteInfo = SiteResponse.builder()
//                         .siteName("東京本社")
//                         .build();

//                 // サービス層から返却される期待レスポンスを作成
//                 CreateUserResponse mockResponse =  new CreateUserResponse();
//                         mockResponse.setStatusCode(ResponseConstants.OK); //statusCode
//                         mockResponse.setMessage("ユーザーが正常に作成されました。");
//                         mockResponse.setUser(userInfo);
//                         mockResponse.setSite(siteInfo);

//                 // === モックの振る舞いを定義 ===
//                 // userServiceのcreateUserメソッドに対してモックレスポンスを返却
//                 when(userService.createUser(TEST_COMPANY_ID, TEST_USERNAME, request))
//                         .thenReturn(mockResponse);
//                 // ResponseUtilのスタティックメソッドをモック化
//                 mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
//                                 .thenReturn(HttpStatus.OK);

//                 // === テスト対象メソッドの実行 ===
//                 // Act
//                 ResponseEntity<CreateUserResponse> response = 
//                         participatingUserController.createUser(authentication, request);

//                 // === 検証 ===
//                 // レスポンスの基本情報を検証
//                 // Assert
//                 assertThat(response).isNotNull();
//                 assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//                 assertThat(response.getBody()).isNotNull();
//                 assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.OK);
//                 assertThat(response.getBody().getMessage()).isEqualTo("ユーザーが正常に作成されました。");

//                 // 作成されたユーザ情報を検証
//                 assertThat(response.getBody().getUser()).isNotNull();
//                 assertThat(response.getBody().getUser().getUsername()).isEqualTo("newuser");
//                 assertThat(response.getBody().getUser().getEmail()).isEqualTo("sato@example.com");

//                 // 紐付けられた拠点情報を検証
//                 assertThat(response.getBody().getSite()).isNotNull();
//                 assertThat(response.getBody().getSite().getSiteName()).isEqualTo("東京本社");

//                 // 各メソッドが適切に呼ばれたことを確認
//                 verify(jwtUtil, times(1)).extractCompanyId(eq(TEST_TOKEN));
//                 verify(jwtUtil, times(1)).extractUsername(eq(TEST_TOKEN));
//                 verify(userService, times(1)).createUser(
//                         eq(TEST_COMPANY_ID), 
//                         eq(TEST_USERNAME), 
//                         any(CreateUserRequest.class)
//                 );
//         }
//     }

//     @Test
//     @DisplayName("ユーザ更新が正常に行われること")
//     void testUpdateUser_Success() {
//         try (MockedStatic<ResponseUtil> mockedResponseUtil = mockStatic(ResponseUtil.class)) {
//                 // === 認証情報のモック設定 ===
//                 when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);

//                 // === テストデータの準備 ===
//                 // ユーザ更新のリクエストデータを設定
//                 // Arrange
//                 UpdateUserRequest request =  new UpdateUserRequest();
//                         request.setUserId(5);
//                         request.setFirstName("三郎");
//                         request.setFirstNameKana("サブロウ");
//                         request.setLastName("鈴木");
//                         request.setLastNameKana("スズキ");
//                         request.setEmail("suzuki_updated@example.com");
//                         request.setPhoneNumber("090-9876-5432");
//                         request.setUserPosition("課長");
//                         request.setSiteId(2);

//                 // 更新後のユーザ情報のレスポンスデータを準備
//                 UserInfoResponse updatedUserInfo = UserInfoResponse.builder()
//                         .id("5")
//                         .firstName("三郎")
//                         .firstNameKana("サブロウ")
//                         .lastName("鈴木")
//                         .lastNameKana("スズキ")
//                         .email("suzuki_updated@example.com")
//                         .phoneNumber("090-9876-5432")
//                         .role("user")
//                         .build();

//                 // 更新後の拠点情報リストを準備
//                 List<SiteResponse> updatedSites = new ArrayList<>();
//                 SiteResponse siteInfo = SiteResponse.builder()
//                         .siteName("名古屋支社")
//                         .build();
//                 updatedSites.add(siteInfo);

                
//                 // サービス層から返却される期待レスポンスを作成
//                 UpdateUserResponse mockResponse =  new UpdateUserResponse();
//                         mockResponse.setStatusCode(ResponseConstants.OK);
//                         mockResponse.setMessage("ユーザー情報が正常に更新されました。");
//                         mockResponse.setUser(updatedUserInfo);
//                         mockResponse.setUpdatedSites(updatedSites);

//                 // === モックの振る舞いを定義 ===
//                 // userServiceのupdateUserメソッドに対してモックレスポンスを返却
//                 when(userService.updateUser(TEST_COMPANY_ID, TEST_USERNAME, request))
//                         .thenReturn(mockResponse);
//                 // ResponseUtilのスタティックメソッドをモック化
//                 mockedResponseUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK))
//                                 .thenReturn(HttpStatus.OK);

//                 // === テスト対象メソッドの実行 ===
//                 // Act
//                 ResponseEntity<UpdateUserResponse> response = 
//                         participatingUserController.updateUser(authentication, request);

//                 // === 検証 ===
//                 // レスポンスの基本情報を検証
//                 // Assert
//                 assertThat(response).isNotNull();
//                 assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//                 assertThat(response.getBody()).isNotNull();
//                 assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.OK);
//                 assertThat(response.getBody().getMessage()).isEqualTo("ユーザー情報が正常に更新されました。");

//                 // 更新されたユーザ情報を検証
//                 assertThat(response.getBody().getUser()).isNotNull();
//                 assertThat(response.getBody().getUser().getEmail()).isEqualTo("suzuki_updated@example.com");
//                 assertThat(response.getBody().getUser().getFirstName()).isEqualTo("三郎");

//                 // 更新された拠点情報を検証
//                 assertThat(response.getBody().getUpdatedSites()).hasSize(1);
//                 assertThat(response.getBody().getUpdatedSites().get(0).getSiteName()).isEqualTo("名古屋支社");

//                 // 各メソッドが適切に呼ばれたことを確認
//                 verify(jwtUtil, times(1)).extractCompanyId(eq(TEST_TOKEN));
//                 verify(jwtUtil, times(1)).extractUsername(eq(TEST_TOKEN));
//                 verify(userService, times(1)).updateUser(
//                         eq(TEST_COMPANY_ID), 
//                         eq(TEST_USERNAME), 
//                         any(UpdateUserRequest.class));
//         }
//         }
// }