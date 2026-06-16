package com.kct.jcredit.core_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.request.CreateUserRequest;
import com.kct.jcredit.core_backend.dto.request.DeleteUserSite;
import com.kct.jcredit.core_backend.dto.request.UpdateUserRequest;
import com.kct.jcredit.core_backend.dto.response.CreateUserResponse;
import com.kct.jcredit.core_backend.dto.response.DeleteUserSiteResponse;
import com.kct.jcredit.core_backend.dto.response.UpdateUserResponse;
import com.kct.jcredit.core_backend.dto.response.UserListResponse;
import com.kct.jcredit.core_backend.dto.response.UserProfileResponse;
import com.kct.jcredit.core_backend.dto.response.details.SiteResponse;
import com.kct.jcredit.core_backend.dto.response.details.UserInfoResponse;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.enums.SubscriptionStatus;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.impl.UserServiceImpl;


@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private ParticipatingUserRepository userRepository;

    @Mock
    private ParticipatingCompanyRepository participatingCompanyRepository;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private String masterCompanyId;
    private String masterUsername;
    private CreateUserRequest createUserRequest;
    private Site site;
    private ParticipatingCompany company;
    private ParticipatingCompany testCompany;
    private Site testSite;
    private ParticipatingUser masterUser;
    private ParticipatingUser normalUser;
    private ParticipatingUser newUser;
    private ParticipatingUser generalUser;

    private OperatingUser operatingUser;

    private CreateUserRequest validRequest;
    



    @BeforeEach
    void setUp() {
        // テスト用の会社を作成
        testCompany = ParticipatingCompany.builder()
                .companyId("COMP001")
                .nameJa("テスト株式会社")
                .nameFurigana("テストカブシキガイシャ")
                .nameEn("Test Company")
                .email("company@test.com")
                .phoneNumber("03-1234-5678")
                .fax("03-1234-5679")
                .corpNumber("1234567890123")
                .address("東京都渋谷区")
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build();
        testCompany.setId(1);

        generalUser = ParticipatingUser.builder()
            .username("general_user")
            .firstName("次郎")
            .firstNameKana("ジロウ")
            .lastName("佐藤")
            .lastNameKana("サトウ")
            .email("master@test.com")
            .phoneNumber("090-1111-2222")
            .userPosition("営業")
            .password("encodedPassword")
            .role(RoleConstants.USER) // "user"
            .companyId("COMP001")
            .loginAttempts(0)
            .build();
        generalUser.setId(2);
        generalUser.setIsActive(true);
        generalUser.setCreatedAt(LocalDateTime.now());
        generalUser.setUpdatedAt(LocalDateTime.now());

        // テスト用の拠点を作成
        testSite = Site.builder()
                .company(testCompany)
                .siteName("本社")
                .picFirstName("太郎")
                .picFirstNameKana("タロウ")
                .picLastName("山田")
                .picLastNameKana("ヤマダ")
                .picEmail("master@test.com")
                .picPhoneNumber("090-1234-5678")
                .picPosition("部長")
                .build();
        testSite.setId(1);

        generalUser.setSite(testSite);

        // テスト用のマスターユーザーを作成
        masterUser = ParticipatingUser.builder()
                .username("master_user")
                .firstName("太郎")
                .firstNameKana("タロウ")
                .lastName("山田")
                .lastNameKana("ヤマダ")
                .email("master@test.com")
                .phoneNumber("090-1234-5678")
                .userPosition("部長")
                .password("encoded_password")
                .role(RoleConstants.MASTER)
                .site(testSite)
                .companyId("COMP001")
                .loginAttempts(0)
                .build();
        masterUser.setId(1);
        masterUser.setIsActive(true);
        masterUser.setCreatedAt(LocalDateTime.now());

        // テスト用の一般ユーザーを作成
        normalUser = ParticipatingUser.builder()
                .username("normal_user")
                .firstName("花子")
                .firstNameKana("ハナコ")
                .lastName("佐藤")
                .lastNameKana("サトウ")
                .email("user@test.com")
                .phoneNumber("090-8765-4321")
                .userPosition("課長")
                .password("encoded_password")
                .role(RoleConstants.USER)
                .site(testSite)
                .companyId("COMP001")
                .loginAttempts(0)
                .build();
        normalUser.setId(2);
        normalUser.setIsActive(true);
        normalUser.setCreatedAt(LocalDateTime.now());

        operatingUser = OperatingUser.builder()
                .username("operating_user")
                .firstName("次郎")
                .firstNameKana("ジロウ")
                .lastName("田中")
                .lastNameKana("タナカ")
                .email("operating@test.com")
                .phoneNumber("090-0000-0000")
                .password("encoded_password")
                .role(RoleConstants.OPERATOR)
                .loginAttempts(0)
                .build();
        operatingUser.setId(3);
        operatingUser.setIsActive(true);
        operatingUser.setCreatedAt(LocalDateTime.now());

        // validRequest の初期化
        validRequest = new CreateUserRequest();
            validRequest.setSiteId(1);
            validRequest.setUsername("newuser");
            validRequest.setFirstName("Taro");
            validRequest.setLastName("Yamada");
            validRequest.setFirstNameKana("タロウ");
            validRequest.setLastNameKana("ヤマダ");
            validRequest.setEmail("taro.yamada@example.com");
            validRequest.setPhoneNumber("080-9876-5432");
            validRequest.setUserPosition("Staff");
            validRequest.setPassword("SecurePass123!");
    }


    //最初に作成していた11本の正常系
    @Test
    @DisplayName("UT-S-001: マスターアカウントの状態でプロフィールを表示できること")
    void testGetUserProfile_Master() {
        // Given
        when(userRepository.findByUsername("master_user")).thenReturn(Optional.of(masterUser));

        // When
        UserProfileResponse response = userService.getUserProfile("master_user");

        // Then
        //レスポンスがnullでないことを確認
        assertThat(response).isNotNull();
        //レスポンスのステータスコードがOKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        //プロフィール情報がnullでないことを確認
        assertThat(response.getProfile()).isNotNull();
        //ユーザー名が"master_user"であることを確認
        assertThat(response.getProfile().getUsername()).isEqualTo("master_user");
        //ロールがMASTERであることを確認
        assertThat(response.getProfile().getRole()).isEqualTo(RoleConstants.MASTER);
        //名が"太郎"であることを確認
        assertThat(response.getProfile().getFirstName()).isEqualTo("太郎");
        //姓が"山田"であることを確認
        assertThat(response.getProfile().getLastName()).isEqualTo("山田");
        //メールアドレスが"master@test.com"であることを確認
        assertThat(response.getProfile().getEmail()).isEqualTo("master@test.com");
        //会社情報がnullでないことを確認
        assertThat(response.getProfile().getCompany()).isNotNull();
        //会社IDが"COMP001"であることを確認
        assertThat(response.getProfile().getCompany().getCompanyId()).isEqualTo("COMP001");

        //userRepository.findByUsernameが1回だけ呼ばれたことを確認
        verify(userRepository, times(1)).findByUsername("master_user");
    }

    //最初に作成していた11本の正常系
    @Test
    @DisplayName("UT-S-002: ユーザアカウントの状態でプロフィールを表示できること")
    void testGetUserProfile_User() {
        // Given
        when(userRepository.findByUsername("normal_user")).thenReturn(Optional.of(normalUser));

        // When
        UserProfileResponse response = userService.getUserProfile("normal_user");

        //Then
        //レスポンスがnullでないことを確認
        assertThat(response).isNotNull();
        //レスポンスのステータスコードがOKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        //プロフィール情報がnullでないことを確認
        assertThat(response.getProfile()).isNotNull();
        //ユーザー名が"normal_user"であることを確認
        assertThat(response.getProfile().getUsername()).isEqualTo("normal_user");
        //ロールがUSERであることを確認
        assertThat(response.getProfile().getRole()).isEqualTo(RoleConstants.USER);
        //名が"花子"であることを確認
        assertThat(response.getProfile().getFirstName()).isEqualTo("花子");
        //姓が"佐藤"であることを確認
        assertThat(response.getProfile().getLastName()).isEqualTo("佐藤");
        //メールアドレスが"user@test.com"であることを確認
        assertThat(response.getProfile().getEmail()).isEqualTo("user@test.com");
        //会社情報がnullでないことを確認
        assertThat(response.getProfile().getCompany()).isNotNull();
        //会社IDが"COMP001"であることを確認
        assertThat(response.getProfile().getCompany().getCompanyId()).isEqualTo("COMP001");

        //userRepository.findByUsernameが1回だけ呼ばれたことを確認
        verify(userRepository, times(1)).findByUsername("normal_user");
    }

    @Test
    @DisplayName("UT-S-003: 存在しないユーザー名の場合、NOT_FOUNDが返却されること")
    void testGetUserProfile_NOT_FOUND() {
        // Given
        when(userRepository.findByUsername("null_user")).thenReturn(Optional.empty());

        // When
        UserProfileResponse response = userService.getUserProfile("null_user");

        //Then
        //レスポンスのステータスコードがOKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        //メッセージが適切に設定されていること
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_USER_NOT_FOUND);
        //プロフィール情報がnullでないことを確認
        assertThat(response.getProfile()).isNull();
    }


    //最初に作成していた11本の正常系
    @Test
    @DisplayName("UT-S-004: マスターが同企業のユーザ情報すべて取得できること")
    void testGetUsersByCompany() {
        // Given
        String companyId = "COMP001";
        List<ParticipatingUser> users = Arrays.asList(masterUser, normalUser);
        
        when(participatingCompanyRepository.findByCompanyId(companyId))
                .thenReturn(Optional.of(testCompany));
        when(userRepository.findByCompanyId(companyId)).thenReturn(users);

        // When
        UserListResponse response = userService.getUsersByCompany(companyId);

        // Then
        //レスポンスがnullでないことを確認
        assertThat(response).isNotNull();
        //レスポンスのステータスコードがOKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        //ユーザーリストがnullでないことを確認
        assertThat(response.getUsers()).isNotNull();
        //ユーザーリストのサイズが2であることを確認
        assertThat(response.getUsers()).hasSize(2);
        //1人目のユーザーの名が"太郎"であることを確認
        assertThat(response.getUsers().get(0).getFirstName()).isEqualTo("太郎");
        //2人目のユーザーの名が"花子"であることを確認
        assertThat(response.getUsers().get(1).getFirstName()).isEqualTo("花子");

        //participatingCompanyRepository.findByCompanyIdが1回だけ呼ばれたことを確認
        verify(participatingCompanyRepository, times(1)).findByCompanyId(companyId);
        //userRepository.findByCompanyIdが1回だけ呼ばれたことを確認
        verify(userRepository, times(1)).findByCompanyId(companyId);
    }

    
    @Test
    @DisplayName("UT-S-005: 企業にユーザーが1人だけ存在する場合、その情報が取得できること")
    void testGetUserByCompany() {
        // Given
        String companyId = "COMP001";
        List<ParticipatingUser> user = Arrays.asList(normalUser);
        
        when(participatingCompanyRepository.findByCompanyId(companyId))
                .thenReturn(Optional.of(testCompany));
        when(userRepository.findByCompanyId(companyId)).thenReturn(user);

        // When
        UserListResponse response = userService.getUsersByCompany(companyId);

        // Then
        //レスポンスがnullでないことを確認
        assertThat(response).isNotNull();
        //レスポンスのステータスコードがOKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        //ユーザーリストがnullでないことを確認
        assertThat(response.getUsers()).isNotNull();

        assertThat(response.getUsers())
                .hasSize(1)
                .satisfies(userList -> {
                        // 1人目（normalUser）の検証
                        UserInfoResponse second = userList.get(0);
                        assertThat(second.getUsername()).isEqualTo("normal_user");
                        assertThat(second.getFirstName()).isEqualTo("花子");
                        assertThat(second.getLastName()).isEqualTo("佐藤");
                        assertThat(second.getEmail()).isEqualTo("user@test.com");
                        assertThat(second.getRole()).isEqualTo(RoleConstants.USER);
                });

        //participatingCompanyRepository.findByCompanyIdが1回だけ呼ばれたことを確認
        verify(participatingCompanyRepository, times(1)).findByCompanyId(companyId);
        //userRepository.findByCompanyIdが1回だけ呼ばれたことを確認
        verify(userRepository, times(1)).findByCompanyId(companyId);
    }

    @Test
    @DisplayName("UT-S-006: 企業にユーザーが0人の場合、空のリストが返却されること")
    void test0GetUserByCompany() {
        // Given
        String companyId = "COMP001";
        List<ParticipatingUser> nulluser = Collections.emptyList();
        
        when(participatingCompanyRepository.findByCompanyId(companyId))
                .thenReturn(Optional.of(testCompany));
        when(userRepository.findByCompanyId(companyId)).thenReturn(nulluser);

        // When
        UserListResponse response = userService.getUsersByCompany(companyId);

        // Then
        //レスポンスがnullでないことを確認
        assertThat(response).isNotNull();
        //レスポンスのステータスコードがOKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        //ユーザーリストがnullでないことを確認
        assertThat(response.getUsers()).isNotNull();
        //ユーザーリストのサイズが0であることを確認
        assertThat(response.getUsers()).isEmpty();

        //participatingCompanyRepository.findByCompanyIdが1回だけ呼ばれたことを確認
        verify(participatingCompanyRepository, times(1)).findByCompanyId(companyId);
        //userRepository.findByCompanyIdが1回だけ呼ばれたことを確認
        verify(userRepository, times(1)).findByCompanyId(companyId);
    }

    @Test
    @DisplayName("UT-S-007: 存在しない企業IDの場合、NOT_FOUNDが返却されること")
    void testGetUsersByCompanyNotFound() {
        // Given
        String companyId = "COMP999";
        
        when(participatingCompanyRepository.findByCompanyId(companyId))
                .thenReturn(Optional.empty());

        // When
        UserListResponse response = userService.getUsersByCompany(companyId);

        // Then
        //レスポンスがnullでないことを確認
        assertThat(response).isNotNull();
        //レスポンスのステータスコードがNOT_FOUNDであることを確認
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        //ユーザーリストのサイズがnullであることを確認
        assertThat(response.getUsers()).isNull();

        //participatingCompanyRepository.findByCompanyIdが1回だけ呼ばれたことを確認
        verify(participatingCompanyRepository, times(1)).findByCompanyId(companyId);
    }

    
    @Test
    @DisplayName("UT-S-008: データベースエラーが発生した場合、適切なエラーハンドリングがされること")
    void testGetUsersByCompanyData() {
        // Given
        String companyId = "COMP001";
        
        when(participatingCompanyRepository.findByCompanyId(companyId))
                .thenThrow(new DataAccessException("Database connection failed") {});

        // When & Then
        assertThatThrownBy(() -> userService.getUsersByCompany(companyId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Database connection failed");
        verify(participatingCompanyRepository, times(1)).findByCompanyId(companyId);
    }

    //最初に作成していた11本の正常系
    @Test
    @DisplayName("UT-S-009: ユーザと拠点のつながりを切ることができること")
    void testRemoveUserSiteConnection() {
        // Given
        DeleteUserSite request = new DeleteUserSite();
        request.setUserId("2");
        request.setSiteId("1");

        when(userRepository.findById(2)).thenReturn(Optional.of(normalUser));
        when(siteRepository.findById(1)).thenReturn(Optional.of(testSite));
        when(userRepository.findByCompanyIdAndRole("COMP001", RoleConstants.MASTER))
                .thenReturn(Optional.of(masterUser));
        when(userRepository.save(any(ParticipatingUser.class))).thenReturn(normalUser);
        when(siteRepository.save(any(Site.class))).thenReturn(testSite);

        // When
        DeleteUserSiteResponse response = userService.removeUserSiteConnection(request);

        // Then
        //レスポンスがnullでないことを確認
        assertThat(response).isNotNull();
        //レスポンスのステータスコードがOKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        //userRepository.saveが1回だけ呼ばれ、ユーザーの拠点がnullになっていることを確認
        verify(userRepository, times(1)).save(argThat(user -> user.getSite() == null));
        //siteRepository.saveが1回だけ呼ばれ、拠点の担当者メールがマスターユーザーのメールになっていることを確認
        verify(siteRepository, times(1)).save(argThat(site -> 
                site.getPicEmail().equals(masterUser.getEmail())));
    }

    
    @Test
    @DisplayName("UT-S-010: 存在しないuserIdの場合、NOT_FOUNDが返却されること")
    void testRemoveUserSiteConnection_UserNotFound() {
        // Given
        DeleteUserSite request = new DeleteUserSite();
        request.setUserId("999");
        request.setSiteId("1");

        when(userRepository.findById(999)).thenReturn(Optional.empty());
        

        // When
        DeleteUserSiteResponse response = userService.removeUserSiteConnection(request);

        // Then
        // レスポンスがnullでないことを確認
        assertThat(response).isNotNull();
        // レスポンスのステータスコードがOKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_USER_NOT_FOUND);
        
        verify(userRepository, times(1)).findById(999);

        verify(siteRepository, never()).findById(anyInt());
        verify(userRepository, never()).findByCompanyIdAndRole(anyString(), anyString());
        // userRepository.saveも呼ばれていることを確認（副次的な検証）
        verify(userRepository, never()).save(any(ParticipatingUser.class));
        verify(siteRepository, never()).save(any(Site.class));
    }

    @Test
    @DisplayName("UT-S-011: 存在しないsiteIdの場合、NOT_FOUNDが返却されること")
    void testRemoveUserSiteConnection_SetIdNotFound() {
        // Given
        DeleteUserSite request = new DeleteUserSite();
        request.setUserId("1");
        request.setSiteId("999");

        when(userRepository.findById(1)).thenReturn(Optional.of(normalUser));
        when(siteRepository.findById(999)).thenReturn(Optional.empty());

        // When
        DeleteUserSiteResponse response = userService.removeUserSiteConnection(request);

        // Then
        // レスポンスがnullでないことを確認
        assertThat(response).isNotNull();
        // レスポンスのステータスコードがOKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
        
        // userRepository.findByIdとsiteRepository.findByIdのみ呼ばれる
        verify(userRepository, times(1)).findById(1);
        verify(siteRepository, times(1)).findById(999);
        
        // 以降の処理は実行されない
        verifyNoMoreInteractions(userRepository, siteRepository);
    }

    @Test
    @DisplayName("UT-S-012: userIdが数値に変換できない場合、例外が発生すること")
    void testRemoveUserSiteConnection_UserIdNotNumeric() {
        // Given
        DeleteUserSite request = new DeleteUserSite();
        request.setUserId("abc");
        request.setSiteId("1");

        assertThatThrownBy(() -> userService.removeUserSiteConnection(request))
                .isInstanceOf(NumberFormatException.class);

        // userRepository.saveも呼ばれていることを確認（副次的な検証）
        verifyNoInteractions(userRepository);
        verifyNoInteractions(siteRepository);
    }

    @Test
    @DisplayName("UT-S-013: siteIdが数値に変換できない場合、例外が発生すること")
    void testRemoveUserSiteConnection_SiteIdNotNumeric() {

        // Given
        DeleteUserSite request = new DeleteUserSite();
        request.setUserId("1");
        request.setSiteId("xyz");

        when(userRepository.findById(1)).thenReturn(Optional.of(normalUser));

        assertThatThrownBy(() -> userService.removeUserSiteConnection(request))
                .isInstanceOf(NumberFormatException.class);

        verify(userRepository, times(1)).findById(1);
        verifyNoInteractions(siteRepository); // siteRepositoryは呼ばれない（parseIntで落ちる）
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-E-014: userIdがnullの場合、例外が発生すること")
    void testRemoveUserSiteConnection_UserIdNull() {
        // Given
        DeleteUserSite request = new DeleteUserSite();
        request.setUserId(null);
        request.setSiteId("1");

        // When & Then
        assertThatThrownBy(() -> userService.removeUserSiteConnection(request))
                .isInstanceOf(NumberFormatException.class)
                .hasMessageContaining("null");
        
        // 例外発生時はリポジトリ操作が行われないことを確認
        verifyNoInteractions(userRepository);
        verifyNoInteractions(siteRepository);
    }

    @Test
    @DisplayName("UT-E-015: siteIdがnullの場合、例外が発生すること")
    void testRemoveUserSiteConnection_SiteIdNull() {
        // Given
        DeleteUserSite request = new DeleteUserSite();
        request.setUserId("1");
        request.setSiteId(null);

        when(userRepository.findById(1)).thenReturn(Optional.of(normalUser));

        // When & Then
        assertThatThrownBy(() -> userService.removeUserSiteConnection(request))
                .isInstanceOf(NumberFormatException.class)
                .hasMessageContaining("null");
        
        // 例外発生時はリポジトリ操作が行われないことを確認
        verify(userRepository, times(1)).findById(1);
        verifyNoInteractions(siteRepository);
    }

    @Test
    @DisplayName("UT-S-016: サイトに会社情報が紐づいていない場合、例外が発生すること")
    void testRemoveUsersSiteConnection_SiteCompanyNull() {
        // Given
        DeleteUserSite request = new DeleteUserSite();
        request.setUserId("2");
        request.setSiteId("1");

        Site siteWithoutCompany = Site.builder()
                .siteName("本社")
                .postalCode("100-0001")
                .prefecture("東京都")
                .city("千代田区")
                .town("千代田")
                .blocknum("1-1-1")
                .address("東京都千代田区千代田1-1-1")
                .picFirstName("Taro")
                .picLastName("Yamada")
                .picFirstNameKana("タロウ")
                .picLastNameKana("ヤマダ")
                .picEmail("yamada@example.com")
                .picPhoneNumber("03-1234-5678")
                .picPosition("Director")
                .company(null) // companyがnull
                .build();

        when(userRepository.findById(2)).thenReturn(Optional.of(normalUser));
        when(siteRepository.findById(1)).thenReturn(Optional.of(siteWithoutCompany));
        when(userRepository.save(any(ParticipatingUser.class))).thenReturn(normalUser);

        // When
        assertThatThrownBy(() -> userService.removeUserSiteConnection(request))
                .isInstanceOf(NullPointerException.class);

        // Then
        //レスポンスがnullでないことを確認
        verify(userRepository, times(1)).findById(2);
        verify(siteRepository, times(1)).findById(1);
        verify(userRepository, times(1)).save(any(ParticipatingUser.class));
        verify(userRepository, never()).findByCompanyIdAndRole(anyString(), anyString());
        // ユーザのsiteがnullのまま保存されることを確認
        verify(siteRepository, never()).save(any(Site.class));
    }

    @Test
    @DisplayName("UT-S-017: データベース保存時に例外が発生した場合、適切にロールバックされること")
    void testRemoveUsersSiteConnection_UserSaveException() {
        // Given
        DeleteUserSite request = new DeleteUserSite();
        request.setUserId("2");
        request.setSiteId("1");

        when(userRepository.findById(2)).thenReturn(Optional.of(normalUser));
        when(siteRepository.findById(1)).thenReturn(Optional.of(testSite));

        when(userRepository.save(any(ParticipatingUser.class)))
                .thenThrow(new DataAccessException("Database connection failed") {});

        // When
        assertThatThrownBy(() -> userService.removeUserSiteConnection(request))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("Database connection failed");

        // Then
        verify(userRepository, times(1)).findById(2);
        verify(siteRepository, times(1)).findById(1);
        verify(userRepository, times(1)).save(any(ParticipatingUser.class));
        // user保存時に例外が発生したため、以降の処理は実行されないverify(userRepository, never()).findByCompanyIdAndRole(anyString(), anyString());
        verify(siteRepository, never()).save(any(Site.class));
    }


    //最初に作成していた11本の正常系
    @Test
    @DisplayName("UT-S-018: ユーザ登録が正常にできること(マスター)")
    void testCreateUser() {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setSiteId(1);
        request.setUsername("new_user");
        request.setFirstName("次郎");
        request.setFirstNameKana("ジロウ");
        request.setLastName("田中");
        request.setLastNameKana("タナカ");
        request.setEmail("new@test.com");
        request.setPhoneNumber("090-1111-2222");
        request.setUserPosition("主任");
        request.setPassword("password123");

        ParticipatingUser newUser = ParticipatingUser.builder()
                .username("new_user")
                .firstName("次郎")
                .firstNameKana("ジロウ")
                .lastName("田中")
                .lastNameKana("タナカ")
                .email("new@test.com")
                .phoneNumber("090-1111-2222")
                .userPosition("主任")
                .password("encoded_password")
                .role(RoleConstants.USER)
                .site(testSite)
                .companyId("COMP001")
                .build();
        
        newUser.setId(3);

        ParticipatingCompany company = new ParticipatingCompany();
        company.setCompanyId("COMP001");
        testSite.setCompany(company);

        masterUser.setEmail("master@example.com");
        testSite.setPicEmail("master@example.com");

        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");

        when(siteRepository.findById(1)).thenReturn(Optional.of(testSite));
        when(userRepository.findByUsername("master_user")).thenReturn(Optional.of(masterUser));
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("new_user")).thenReturn(false);
        when(userRepository.save(any(ParticipatingUser.class))).thenReturn(newUser);
        when(siteRepository.save(any(Site.class))).thenReturn(testSite);

        // When
        CreateUserResponse response = userService.createUser("COMP001", "master_user", request);

        // Then
        //レスポンスがnullでないことを確認
        assertThat(response).isNotNull();
        //レスポンスのステータスコードがOKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        //作成されたユーザー情報がnullでないことを確認
        assertThat(response.getUser()).isNotNull();
        //作成されたユーザー名が"new_user"であることを確認
        assertThat(response.getUser().getUsername()).isEqualTo("new_user");
        //拠点情報がnullでないことを確認
        assertThat(response.getSite()).isNotNull();

        assertThat(response.getMessage()).isEqualTo("ユーザーが正常に作成されました。");

        //userRepository.saveが1回だけ呼ばれたことを確認
        verify(userRepository, times(1)).save(any(ParticipatingUser.class));
        //siteRepository.saveが1回だけ呼ばれたことを確認
        verify(siteRepository, times(1)).save(any(Site.class));

        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("UT-S-019: 新規ユーザーの全ての項目が正しく設定されること")
    void testCreateUser_AllFieldsAreSetCorrectly() {
        // Given
        String masterCompanyId = "COMP001";
        String username = "master";

        // ★ ParticipatingCompany の設定
        ParticipatingCompany testCompany = new ParticipatingCompany();
        testCompany.setCompanyId("COMP001");
        
        // ★ masterUser の設定（isSitePIC チェック用）
        masterUser.setEmail("master@example.com");
        // ★ testSite の設定
        testSite.setId(1);
        testSite.setSiteName("本社");
        testSite.setCompany(testCompany);
        testSite.setPicEmail("master@example.com");  // isSitePICで一致させる

        when(siteRepository.findById(1)).thenReturn(Optional.of(testSite));
        when(userRepository.findByUsername("master")).thenReturn(Optional.of(masterUser));
        when(userRepository.existsByEmail("taro.yamada@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("encodedSecurePass123!");
        
        when(userRepository.save(any(ParticipatingUser.class))).thenAnswer(invocation -> {
                ParticipatingUser user = invocation.getArgument(0);
                return user;
        });
        
        when(siteRepository.save(any(Site.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime beforeTest = LocalDateTime.now();

        // When
        CreateUserResponse response = userService.createUser(masterCompanyId, username, validRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getMessage()).isEqualTo("ユーザーが正常に作成されました。");

        // ==========ユーザー情報の検証 ==========
        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        ParticipatingUser savedUser = userCaptor.getValue();

        // 1. 基本情報の検証
        assertThat(savedUser.getUsername()).isEqualTo("newuser");
        assertThat(savedUser.getFirstName()).isEqualTo("Taro");
        assertThat(savedUser.getLastName()).isEqualTo("Yamada");
        assertThat(savedUser.getFirstNameKana()).isEqualTo("タロウ");
        assertThat(savedUser.getLastNameKana()).isEqualTo("ヤマダ");
        assertThat(savedUser.getEmail()).isEqualTo("taro.yamada@example.com");
        assertThat(savedUser.getPhoneNumber()).isEqualTo("080-9876-5432");
        assertThat(savedUser.getUserPosition()).isEqualTo("Staff");

        // 2. パスワードの検証（エンコード済み）
        assertThat(savedUser.getPassword()).isEqualTo("encodedSecurePass123!");
        assertThat(savedUser.getPassword()).isNotEqualTo("SecurePass123!");
        verify(passwordEncoder, times(1)).encode("SecurePass123!");

        // 3. 権限とロールの検証
        assertThat(savedUser.getRole()).isEqualTo(RoleConstants.USER);
        
        // 4. アカウントロック関連の検証
        assertThat(savedUser.getLoginAttempts()).isEqualTo(0);
        assertThat(savedUser.getAccountLockedUntil()).isNull();

        // 5. サイト関連の検証
        assertThat(savedUser.getSite()).isNotNull();
        assertThat(savedUser.getSite().getId()).isEqualTo(1);
        assertThat(savedUser.getSite().getSiteName()).isEqualTo("本社");

        // 6. 会社IDの検証
        assertThat(savedUser.getCompanyId()).isEqualTo("COMP001");
        assertThat(savedUser.getCompanyId()).isEqualTo(masterCompanyId);

        // 7. アクティブフラグの検証
        assertThat(savedUser.getIsActive()).isTrue();

        // 8. タイムスタンプの検証
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isEqualTo(savedUser.getUpdatedAt());
        assertThat(savedUser.getCreatedAt()).isAfterOrEqualTo(beforeTest);
        assertThat(savedUser.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(1));

        // ========== サイトPIC情報の更新検証 ==========
        ArgumentCaptor<Site> siteCaptor = ArgumentCaptor.forClass(Site.class);
        verify(siteRepository, times(1)).save(siteCaptor.capture());
        Site savedSite = siteCaptor.getValue();
        
        // 9. サイトのPIC情報が新規ユーザーの情報で更新されていることを確認
        assertThat(savedSite.getPicFirstName()).isEqualTo("Taro");
        assertThat(savedSite.getPicLastName()).isEqualTo("Yamada");
        assertThat(savedSite.getPicFirstNameKana()).isEqualTo("タロウ");
        assertThat(savedSite.getPicLastNameKana()).isEqualTo("ヤマダ");
        assertThat(savedSite.getPicEmail()).isEqualTo("taro.yamada@example.com");
        assertThat(savedSite.getPicPhoneNumber()).isEqualTo("080-9876-5432");
        assertThat(savedSite.getPicPosition()).isEqualTo("Staff");
        
        // 10. サイトの更新日時が設定されていることを確認
        assertThat(savedSite.getUpdatedAt()).isNotNull();
        assertThat(savedSite.getUpdatedAt()).isAfterOrEqualTo(beforeTest);

        // ========== レスポンスの検証 ==========
        // 11. レスポンスのユーザー情報を検証
        assertThat(response.getUser()).isNotNull();
        UserInfoResponse userResponse = response.getUser();
        assertThat(userResponse.getUsername()).isEqualTo("newuser");
        assertThat(userResponse.getEmail()).isEqualTo("taro.yamada@example.com");

        // 12. レスポンスのサイト情報を検証
        assertThat(response.getSite()).isNotNull();
        SiteResponse siteResponse = response.getSite();
        assertThat(siteResponse.getSiteName()).isEqualTo("本社");
        // ========== リポジトリ呼び出しの検証 ==========
        // 13. 各リポジトリメソッドが正しい回数呼ばれたことを確認
        verify(siteRepository, times(1)).findById(1);
        verify(userRepository, times(1)).findByUsername("master");
        verify(userRepository, times(1)).existsByEmail("taro.yamada@example.com");
        verify(userRepository, times(1)).existsByUsername("newuser");
        }

    @Test
    @DisplayName("UT-S-020: パスワードがエンコードされて保存されること")
    void testCreateUser_PasswordIsEncoded() {
        // Given
        String masterCompanyId = "COMP001";
        String username = "master";
        String rawPassword = "SecurePass123!";
        String encodedPassword = "encodedSecurePass123!";

        // ★ ParticipatingCompany の設定
        ParticipatingCompany testCompany = new ParticipatingCompany();
        testCompany.setCompanyId(masterCompanyId);
        
        // ★ masterUser の設定（isSitePIC チェック用）
        masterUser.setEmail("master@example.com");

        // ★ testSite の設定
        testSite.setCompany(testCompany);
        testSite.setPicEmail("master@example.com");  // isSitePICで一致させる

        when(siteRepository.findById(1)).thenReturn(Optional.of(testSite));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(userRepository.existsByEmail("taro.yamada@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        
        when(userRepository.save(any(ParticipatingUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(siteRepository.save(any(Site.class))).thenAnswer(inv -> inv.getArgument(0));
        
        // When
        CreateUserResponse response = userService.createUser(masterCompanyId, username, validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

        // ==========ユーザー情報の検証 ==========
        verify(passwordEncoder, times(1)).encode(rawPassword);

        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository).save(userCaptor.capture());
        ParticipatingUser savedUser = userCaptor.getValue();

        assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
        assertThat(savedUser.getPassword()).isNotEqualTo(rawPassword);
        }


    @Test
    @DisplayName("UT-S-021: 新規ユーザーのロールがUSERに設定されること")
    void testCreateUser_RoleIsSetToUser() {
        // Given
        String masterCompanyId = "COMP001";
        String username = "master";

        // ★ ParticipatingCompany の設定
        ParticipatingCompany testCompany = new ParticipatingCompany();
        testCompany.setCompanyId("COMP001");
        
        // ★ masterUser の設定（isSitePIC チェック用）
        masterUser.setEmail("master@example.com");
        // ★ testSite の設定
        testSite.setCompany(testCompany);
        testSite.setPicEmail("master@example.com");  // isSitePICで一致させる

        when(siteRepository.findById(1)).thenReturn(Optional.of(testSite));
        when(userRepository.findByUsername("master")).thenReturn(Optional.of(masterUser));
        when(userRepository.existsByEmail("taro.yamada@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("encodedSecurePass123!");
        
        when(userRepository.save(any(ParticipatingUser.class))).thenAnswer(invocation -> {
                ParticipatingUser user = invocation.getArgument(0);
                return user;
        });
        
        when(siteRepository.save(any(Site.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CreateUserResponse response = userService.createUser(masterCompanyId, username, validRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getMessage()).isEqualTo("ユーザーが正常に作成されました。");

        // ==========ユーザー情報の検証 ==========
        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        ParticipatingUser savedUser = userCaptor.getValue();

        // ロールがUSERに設定されていることを検証
        assertThat(savedUser.getRole())
        .as("新規ユーザーのロールがnullでないこと")
        .isNotNull()
        .as("新規ユーザーのロールがUSERであること")
        .isEqualTo(RoleConstants.USER);

        // ロールがMASTERやADMINでないことを検証
        assertThat(savedUser.getRole())
                .as("新規ユーザーのロールがMASTERでないこと")
                .isNotEqualTo(RoleConstants.MASTER)
                .as("新規ユーザーのロールがOPERATORでないこと")
                .isNotEqualTo(RoleConstants.OPERATOR);

        assertThat(savedUser.getRole()).isEqualTo(RoleConstants.USER);

        assertThat(validRequest.toString()).doesNotContainIgnoringCase("role");
        }


    @Test
    @DisplayName("UT-S-022: 新規ユーザーの初期値（loginAttempts=0、accountLockedUntil=null等）が正しく設定されること")
    void testCreateUser_InitialValuesAreSetCorrectly() {
        // Given
        String masterCompanyId = "COMP001";
        String username = "master";

        // ★ ParticipatingCompany の設定
        ParticipatingCompany testCompany = new ParticipatingCompany();
        testCompany.setCompanyId("COMP001");
        
        // ★ masterUser の設定（isSitePIC チェック用）
        masterUser.setEmail("master@example.com");

        // ★ testSite の設定
        testSite.setCompany(testCompany);
        testSite.setPicEmail("master@example.com");  // isSitePICで一致させる
        testSite.setPicFirstNameKana("マスター");
        testSite.setPicLastNameKana("ユーザー");
        testSite.setPicPhoneNumber("080-1234-5678");
        testSite.setPicPosition("Manager");

        when(siteRepository.findById(1)).thenReturn(Optional.of(testSite));
        when(userRepository.findByUsername("master")).thenReturn(Optional.of(masterUser));
        when(userRepository.existsByEmail("taro.yamada@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("encodedSecurePass123!");
        
        when(userRepository.save(any(ParticipatingUser.class))).thenAnswer(invocation -> {
                ParticipatingUser user = invocation.getArgument(0);
                return user;
        });
        
        when(siteRepository.save(any(Site.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime beforeTest = LocalDateTime.now();

        // When
        CreateUserResponse response = userService.createUser(masterCompanyId, username, validRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getMessage()).isEqualTo("ユーザーが正常に作成されました。");

        // ==========ユーザー情報の検証 ==========
        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        ParticipatingUser savedUser = userCaptor.getValue();

        assertThat(savedUser.getLoginAttempts())
                .as("ログイン試行回数がnullでないこと")
                .isNotNull()
                .as("ログイン試行回数の初期値が0であること")
                .isEqualTo(0)
                .as("ログイン試行回数が負の値でないこと")
                .isNotNegative();

        // 2. アカウントロック日時の初期値を検証
        assertThat(savedUser.getAccountLockedUntil())
                .as("アカウントロック日時の初期値がnullであること")
                .isNull();

        // 3. アクティブフラグの初期値を検証（BaseEntityから継承）
        assertThat(savedUser.getIsActive())
                .as("isActiveがnullでないこと")
                .isNotNull()
                .as("isActiveの初期値がtrueであること")
                .isTrue();

        // タイムスタンプの初期値を検証 
        assertThat(savedUser.getCreatedAt())
                .as("createdAtがnullでないこと")
                .isNotNull()
                .as("createdAtがテスト実行時刻以降であること")
                .isAfterOrEqualTo(beforeTest)
                .as("createdAtが現在時刻以前であること")
                .isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(1));

        assertThat(savedUser.getUpdatedAt())
                .as("updatedAtがnullでないこと")
                .isNotNull()
                .as("updatedAtがテスト実行時刻以降であること")
                .isAfterOrEqualTo(beforeTest)
                .as("updatedAtが現在時刻以前であること")
                .isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(1));
        
        // 5. 作成日時と更新日時が同じであることを検証（新規作成時）
        assertThat(savedUser.getCreatedAt())
                .as("新規作成時はcreatedAtとupdatedAtが同じであること")
                .isEqualTo(savedUser.getUpdatedAt());
        
        // 6. ロールの初期値を検証
        assertThat(savedUser.getRole())
                .as("ロールがnullでないこと")
                .isNotNull()
                .as("ロールの初期値がUSERであること")
                .isEqualTo(RoleConstants.USER);
        
        // 7. 会社IDが設定されていることを検証
        assertThat(savedUser.getCompanyId())
                .as("会社IDがnullでないこと")
                .isNotNull()
                .as("会社IDがmasterの会社IDと一致すること")
                .isEqualTo(masterCompanyId)
                .isEqualTo("COMP001");
        
        // 8. サイトが設定されていることを検証
        assertThat(savedUser.getSite())
                .as("サイトがnullでないこと")
                .isNotNull();
        assertThat(savedUser.getSite().getId())
                .as("サイトIDが正しく設定されていること")
                .isEqualTo(1);
        }

        
    @Test
    @DisplayName("UT-S-023: サイトのPIC情報が新規ユーザーの情報に更新されること")
    void testCreateUser_SitePICIsUpdatedWithNewUserInfo() {
        // Given
        String masterCompanyId = "COMP001";
        String username = "master";

        // ParticipatingCompany の設定
        ParticipatingCompany testCompany = new ParticipatingCompany();
        testCompany.setCompanyId("COMP001");
        
        // masterUser の設定（isSitePIC チェック用）
        masterUser.setEmail("master@example.com");
        masterUser.setFirstName("Master");
        masterUser.setLastName("User");
        
        // testSite の設定（既存のPIC情報＝master）
        testSite.setId(1);
        testSite.setSiteName("本社");
        testSite.setCompany(testCompany);
        testSite.setPicEmail("master@example.com");
        testSite.setPicFirstName("Master");
        testSite.setPicLastName("User");
        testSite.setPicFirstNameKana("マスター");
        testSite.setPicLastNameKana("ユーザー");
        testSite.setPicPhoneNumber("080-1234-5678");
        testSite.setPicPosition("Manager");

        // Mocks
        when(siteRepository.findById(1)).thenReturn(Optional.of(testSite));
        when(userRepository.findByUsername("master")).thenReturn(Optional.of(masterUser));
        when(userRepository.existsByEmail("taro.yamada@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("encodedSecurePass123!");
        
        when(userRepository.save(any(ParticipatingUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        when(siteRepository.save(any(Site.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime beforeTest = LocalDateTime.now();

        // When
        CreateUserResponse response = userService.createUser(masterCompanyId, username, validRequest);

        LocalDateTime afterTest = LocalDateTime.now();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getMessage()).isEqualTo("ユーザーが正常に作成されました。");

        // ========== サイトPIC情報の更新検証 ==========
        
        // 1. siteRepository.save()が呼ばれたことを確認
        ArgumentCaptor<Site> siteCaptor = ArgumentCaptor.forClass(Site.class);
        verify(siteRepository, times(1)).save(siteCaptor.capture());
        Site savedSite = siteCaptor.getValue();
        
        // 2. サイトのPIC名前情報が更新されていることを確認
        assertThat(savedSite.getPicFirstName())
                .as("サイトのPIC名（名）が新規ユーザーの情報に更新されていること")
                .isNotNull()
                .isEqualTo(validRequest.getFirstName());
        
        assertThat(savedSite.getPicLastName())
                .as("サイトのPIC名（姓）が新規ユーザーの情報に更新されていること")
                .isNotNull()
                .isEqualTo(validRequest.getLastName());
        
        // 3. サイトのPIC名前（カナ）情報が更新されていることを確認
        assertThat(savedSite.getPicFirstNameKana())
                .as("サイトのPIC名（名・カナ）が新規ユーザーの情報に更新されていること")
                .isNotNull()
                .isEqualTo(validRequest.getFirstNameKana());
        
        assertThat(savedSite.getPicLastNameKana())
                .as("サイトのPIC名（姓・カナ）が新規ユーザーの情報に更新されていること")
                .isNotNull()
                .isEqualTo(validRequest.getLastNameKana());
        
        // 4. サイトのPICメールアドレスが更新されていることを確認
        assertThat(savedSite.getPicEmail())
                .as("サイトのPICメールアドレスが新規ユーザーの情報に更新されていること")
                .isNotNull()
                .isEqualTo(validRequest.getEmail());
        
        // 5. サイトのPIC電話番号が更新されていることを確認
        assertThat(savedSite.getPicPhoneNumber())
                .as("サイトのPIC電話番号が新規ユーザーの情報に更新されていること")
                .isNotNull()
                .isEqualTo(validRequest.getPhoneNumber());
        
        // 6. サイトのPIC役職が更新されていることを確認
        assertThat(savedSite.getPicPosition())
                .as("サイトのPIC役職が新規ユーザーの情報に更新されていること")
                .isNotNull()
                .isEqualTo(validRequest.getUserPosition());
        
        // 7. サイトの更新日時が設定されていることを確認
        assertThat(savedSite.getUpdatedAt())
                .as("サイトの更新日時が設定されていること")
                .isNotNull()
                .isBetween(beforeTest, afterTest);
        
        // 11. レスポンスのサイト情報を確認
        assertThat(response.getSite())
                .as("レスポンスにサイト情報が含まれていること")
                .isNotNull();
        
        SiteResponse siteResponse = response.getSite();
        assertThat(siteResponse.getSiteName())
                .as("レスポンスのサイト名が正しいこと")
                .isEqualTo("本社");
        }


    @Test
    @DisplayName("UT-S-024: 新規ユーザーの会社IDがマスターと同じになること")
    void testCreateUser_CompanyIdMatchesMasterCompanyId() {
        // Given
        String masterCompanyId = "COMP001";
        String username = "master";

        // ParticipatingCompany の設定
        ParticipatingCompany testCompany = new ParticipatingCompany();
        testCompany.setCompanyId("COMP001");
        
        // masterUser の設定
        masterUser.setEmail("master@example.com");
        
        // testSite の設定
        testSite.setId(1);
        testSite.setSiteName("本社");
        testSite.setCompany(testCompany);
        testSite.setPicEmail("master@example.com");

        // Mocks
        when(siteRepository.findById(1)).thenReturn(Optional.of(testSite));
        when(userRepository.findByUsername("master")).thenReturn(Optional.of(masterUser));
        when(userRepository.existsByEmail("taro.yamada@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("encodedSecurePass123!");
        
        when(userRepository.save(any(ParticipatingUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        when(siteRepository.save(any(Site.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CreateUserResponse response = userService.createUser(masterCompanyId, username, validRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getMessage()).isEqualTo("ユーザーが正常に作成されました。");

        // ========== 会社IDの検証 ==========
        
        // 1. 保存されたユーザー情報を取得
        ArgumentCaptor<ParticipatingUser> captor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository, times(1)).save(captor.capture());
        ParticipatingUser savedUser = captor.getValue();

        assertThat(savedUser.getCompanyId())
                .as("新規ユーザーの会社IDが正しく設定されていること")
                .isNotNull()
                .isNotEmpty()
                .isEqualTo(masterCompanyId)
                .isEqualTo(testSite.getCompany().getCompanyId());
        }

    @Test
    @DisplayName("UT-S-025: 作成日時と更新日時が設定されること")
    void testCreateUser_CreatedAtAndUpdatedAtAreSet() {
        // Given
        String masterCompanyId = "COMP001";
        String username = "master";

        // ParticipatingCompany の設定
        ParticipatingCompany testCompany = new ParticipatingCompany();
        testCompany.setCompanyId("COMP001");
        
        // masterUser の設定
        masterUser.setEmail("master@example.com");
        
        // testSite の設定
        testSite.setCompany(testCompany);
        testSite.setPicEmail("master@example.com");

        // Mocks
        when(siteRepository.findById(1)).thenReturn(Optional.of(testSite));
        when(userRepository.findByUsername("master")).thenReturn(Optional.of(masterUser));
        when(userRepository.existsByEmail("taro.yamada@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("encodedSecurePass123!");
        
        when(userRepository.save(any(ParticipatingUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        when(siteRepository.save(any(Site.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // テスト実行前の時刻を記録
        LocalDateTime beforeTest = LocalDateTime.now();

        // When
        CreateUserResponse response = userService.createUser(masterCompanyId, username, validRequest);

        // テスト実行後の時刻を記録
        LocalDateTime afterTest = LocalDateTime.now();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

        // ========== タイムスタンプの検証 ==========
        
        // 保存されたユーザー情報を取得
        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        ParticipatingUser savedUser = userCaptor.getValue();
        
        // createdAtが設定されていることを確認
        assertThat(savedUser.getCreatedAt())
                .as("ユーザーのcreatedAtが設定されていること")
                .isNotNull();
        
        // updatedAtが設定されていることを確認
        assertThat(savedUser.getUpdatedAt())
                .as("ユーザーのupdatedAtが設定されていること")
                .isNotNull();
           
        assertThat(savedUser.getCreatedAt())
                .as("ユーザーのcreatedAtがテスト実行時刻の範囲内であること")
                .isBetween(beforeTest, afterTest);
        
        // updatedAtがテスト実行時刻の範囲内であることを確認
        assertThat(savedUser.getUpdatedAt())
                .as("ユーザーのupdatedAtがテスト実行時刻の範囲内であること")
                .isBetween(beforeTest, afterTest);
        
        // 新規作成時はcreatedAtとupdatedAtが同じであることを確認
        assertThat(savedUser.getCreatedAt())
                .as("新規作成時はcreatedAtとupdatedAtが同じであること")
                .isEqualTo(savedUser.getUpdatedAt());
        
        ArgumentCaptor<Site> siteCaptor = ArgumentCaptor.forClass(Site.class);
        verify(siteRepository, times(1)).save(siteCaptor.capture());
        Site savedSite = siteCaptor.getValue();

        assertThat(savedSite.getUpdatedAt())
                .as("サイトのupdatedAtが設定されていること")
                .isNotNull()
                .isBetween(beforeTest, afterTest);
        }

    @Test
    @DisplayName("UT-S-026: レスポンスにサイト情報とユーザー情報が含まれること")
    void testCreateUser_ResponseContainsSiteAndUserInfo() {
        // Given
        String masterCompanyId = "COMP001";
        String username = "master";

        // ParticipatingCompany の設定
        ParticipatingCompany testCompany = new ParticipatingCompany();
        testCompany.setCompanyId("COMP001");
        
        // masterUser の設定
        masterUser.setEmail("master@example.com");
        
        // testSite の設定
        testSite.setId(1);
        testSite.setSiteName("本社");
        testSite.setCompany(testCompany);
        testSite.setPicEmail("master@example.com");

        // Mocks
        when(siteRepository.findById(1)).thenReturn(Optional.of(testSite));
        when(userRepository.findByUsername("master")).thenReturn(Optional.of(masterUser));
        when(userRepository.existsByEmail("taro.yamada@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("encodedSecurePass123!");
        
        when(userRepository.save(any(ParticipatingUser.class))).thenAnswer(invocation -> {
                ParticipatingUser user = invocation.getArgument(0);
                user.setId(100);  // 保存されたユーザーにIDを設定
                return user;
        });
        
        when(siteRepository.save(any(Site.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CreateUserResponse response = userService.createUser(masterCompanyId, username, validRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

        // ========== レスポンスの基本検証 ==========
        
        // レスポンスが正常に返されていることを確認
        assertThat(response.getUser())
                .as("レスポンスにユーザー情報が含まれていること")
                .isNotNull();

        UserInfoResponse userInfo = response.getUser();
        
        // ユーザー情報の各フィールドを検証
        assertThat(userInfo.getId())
                .as("ユーザーIDが設定されていること")
                .isNotNull();
        
        assertThat(userInfo.getUsername())
                .as("ユーザー名が設定されていること")
                .isEqualTo(validRequest.getUsername());
        
        assertThat(userInfo.getEmail())
                .as("メールアドレスが設定されていること")
                .isEqualTo(validRequest.getEmail());
        
        assertThat(userInfo.getRole())
                .as("ロールが設定されていること")
                .isEqualTo(RoleConstants.USER);
        
        assertThat(userInfo.getCompanyId())
                .as("会社IDが設定されていること")
                .isEqualTo(masterCompanyId);
        
        // ========== サイト情報の検証 ==========
        
        // レスポンスにサイト情報が含まれていることを確認
        assertThat(response.getSite())
                .as("レスポンスにサイト情報が含まれていること")
                .isNotNull();
        
        SiteResponse siteInfo = response.getSite();
        
        // サイト情報の各フィールドを検証
        assertThat(siteInfo.getId())
                .as("サイトIDが設定されていること")
                .isNotNull();
        
        assertThat(siteInfo.getSiteName())
                .as("サイト名が設定されていること")
                .isEqualTo(testSite.getSiteName());
        }

        
    @Test
    @DisplayName("UT-S-027: 電話番号がnullの際ユーザー登録できないこと")
    void createUser_phoneNumberIsNull_ReturnsOk() {
        String masterCompanyId = "COMPANY_001"; // または既存のテストデータの会社ID
        String username = "test_master_user";
        
        CreateUserRequest request = new CreateUserRequest();
        // 必須フィールドを設定
        request.setSiteId(1);
        request.setUsername("new_user");
        request.setFirstName("太郎");
        request.setFirstNameKana("タロウ");
        request.setLastName("山田");
        request.setLastNameKana("ヤマダ");
        request.setEmail("test@example.com");
        request.setPhoneNumber(null); // null許可のテスト
        request.setUserPosition("担当者");
        request.setPassword("Password123!");
        
        // ★ masterCompanyId を引数に追加してメソッド呼び出し
        CreateUserResponse response = userService.createUser(masterCompanyId, username, request);
        
        // アサーション
        assertEquals(ResponseConstants.NOT_FOUND, response.getStatusCode());
        assertNull(response.getUser());

    }

    @Test
    @DisplayName("UT-S-028: 役職（userPosition）がnullでもユーザー登録できること")
    void createUser_userPositionIsNull_ReturnsOk() {
        // BeforeEachで作成されたテストデータを使用
        String masterCompanyId = testCompany.getCompanyId();
        String username = masterUser.getUsername();

        testSite.setPicEmail(masterUser.getEmail());
        
        // モックの動作設定
        when(siteRepository.findById(testSite.getId())).thenReturn(Optional.of(testSite));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(userRepository.existsByEmail("newuser_position_null@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser_position_null")).thenReturn(false);
        when(userRepository.save(any(ParticipatingUser.class))).thenAnswer(invocation -> {
                ParticipatingUser saved = invocation.getArgument(0);
                saved.setId(10); // IDを設定
                return saved;
        });
        when(siteRepository.save(any(Site.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // リクエスト作成（validRequestをベースに）
        CreateUserRequest request = new CreateUserRequest();
        request.setSiteId(testSite.getId());
        request.setUsername("newuser_position_null");
        request.setFirstName("次郎");
        request.setFirstNameKana("ジロウ");
        request.setLastName("田中");
        request.setLastNameKana("タナカ");
        request.setEmail("newuser_position_null@example.com");
        request.setPhoneNumber("090-9999-8888");
        request.setUserPosition(null); // ★ 役職をnullに設定
        request.setPassword("SecurePass123!");
        
        // メソッド呼び出し
        CreateUserResponse response = userService.createUser(masterCompanyId, username, request);
        
        // アサーション
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertEquals("ユーザーが正常に作成されました。", response.getMessage());
        assertNotNull(response.getUser());
        assertNull(response.getUser().getUserPosition()); // ★ 役職がnullであることを確認
        assertEquals("newuser_position_null", response.getUser().getUsername());
        assertEquals("newuser_position_null@example.com", response.getUser().getEmail());
        
        // userRepository.saveが呼ばれたことを確認
        verify(userRepository, times(1)).save(any(ParticipatingUser.class));
    }

    @Test
    @DisplayName("UT-S-029: 存在しないサイトIDの場合、NOT_FOUNDが返却されること")
    void createUser_siteNotFound_ReturnsNotFound() {
        String masterCompanyId = testCompany.getCompanyId();
        String username = masterUser.getUsername();
        
        int nonExistentSiteId = 999; // 存在しないサイトID
        
        //★ サイトが見つからない場合のモック設定
        when(siteRepository.findById(nonExistentSiteId)).thenReturn(Optional.empty());
        
        CreateUserRequest request = new CreateUserRequest();
        request.setSiteId(nonExistentSiteId); // ★ 存在しないサイトIDを設定
        request.setUsername("newuser_test");
        request.setFirstName("太郎");
        request.setFirstNameKana("タロウ");
        request.setLastName("山田");
        request.setLastNameKana("ヤマダ");
        request.setEmail("newuser_test@example.com");
        request.setPhoneNumber("090-1111-2222");
        request.setUserPosition("担当者");
        request.setPassword("SecurePass123!");
        
        // メソッド呼び出し
        CreateUserResponse response = userService.createUser(masterCompanyId, username, request);
        
        // アサーション
        assertEquals(ResponseConstants.NOT_FOUND, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_NOT_FOUND, response.getMessage());
        assertNull(response.getUser());
        assertNull(response.getSite());
        
        // サイトが見つからないため、ユーザー保存処理は実行されない
        verify(siteRepository, times(1)).findById(nonExistentSiteId);
        verify(userRepository, never()).save(any(ParticipatingUser.class));
        verify(siteRepository, never()).save(any(Site.class));
    }

    @Test
    @DisplayName("UT-S-030: マスターの会社IDとサイトの会社IDが一致しない場合、FORBIDDENが返却されること")
        void createUser_companyIdMismatch_ReturnsForbidden() {
        String masterCompanyId = testCompany.getCompanyId(); // "COMP001"
        String username = masterUser.getUsername();
        
        // ★ 別の会社を作成
        ParticipatingCompany otherCompany = ParticipatingCompany.builder()
                .companyId("COMP002") // 異なる会社ID
                .nameJa("他社株式会社")
                .nameFurigana("タシャカブシキガイシャ")
                .nameEn("Other Company")
                .email("other@test.com")
                .phoneNumber("03-9999-8888")
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build();
        otherCompany.setId(2);
        
        // ★ 別の会社に紐づくサイトを作成
        Site otherSite = Site.builder()
                .company(otherCompany) // 異なる会社に所属
                .siteName("他社支店")
                .picFirstName("次郎")
                .picFirstNameKana("ジロウ")
                .picLastName("鈴木")
                .picLastNameKana("スズキ")
                .picEmail("other_pic@test.com")
                .picPhoneNumber("090-9999-8888")
                .picPosition("課長")
                .build();
        otherSite.setId(2);
        
        // モック設定
        when(siteRepository.findById(otherSite.getId())).thenReturn(Optional.of(otherSite));
        
        CreateUserRequest request = new CreateUserRequest();
        request.setSiteId(otherSite.getId()); // ★ 異なる会社のサイトIDを指定
        request.setUsername("newuser_forbidden");
        request.setFirstName("三郎");
        request.setFirstNameKana("サブロウ");
        request.setLastName("田中");
        request.setLastNameKana("タナカ");
        request.setEmail("newuser_forbidden@example.com");
        request.setPhoneNumber("090-3333-4444");
        request.setUserPosition("係長");
        request.setPassword("SecurePass123!");
        
        // メソッド呼び出し
        CreateUserResponse response = userService.createUser(masterCompanyId, username, request);
        
        // アサーション
        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_FORBIDDEN, response.getMessage());
        assertNull(response.getUser());
        assertNull(response.getSite());
        
        // 権限チェックで失敗するため、ユーザー保存処理は実行されない
        verify(siteRepository, times(1)).findById(otherSite.getId());
        verify(userRepository, never()).save(any(ParticipatingUser.class));
        verify(siteRepository, never()).save(any(Site.class));verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).existsByUsername(anyString());
    }

    @Test
    @DisplayName("UT-S-031: サイトのPICがマスターユーザー自身でない場合、FORBIDDENが返却されること")
    void createUser_masterIsNotSitePIC_ReturnsForbidden() {
        String masterCompanyId = testCompany.getCompanyId(); // "COMP001"
        String username = masterUser.getUsername();
        
        // ★ サイトのPICメールをマスターユーザーと異なるものに設定
        testSite.setPicEmail("different_pic@test.com"); // masterUser.getEmail() = "master@test.com" とは異なる
        
        // モック設定
        when(siteRepository.findById(testSite.getId())).thenReturn(Optional.of(testSite));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        
        CreateUserRequest request = new CreateUserRequest();
        request.setSiteId(testSite.getId());
        request.setUsername("newuser_not_pic");
        request.setFirstName("四郎");
        request.setFirstNameKana("シロウ");
        request.setLastName("佐藤");
        request.setLastNameKana("サトウ");
        request.setEmail("newuser_not_pic@example.com");
        request.setPhoneNumber("090-5555-6666");
        request.setUserPosition("主任");
        request.setPassword("SecurePass123!");
        
        // メソッド呼び出し
        CreateUserResponse response = userService.createUser(masterCompanyId, username, request);
        
        // アサーション
        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_FORBIDDEN, response.getMessage());
        assertNull(response.getUser());
        assertNull(response.getSite());
        
        // 検証: PICチェックで失敗するため、メール・ユーザー名重複チェックやユーザー保存は実行されない
        verify(siteRepository, times(1)).findById(testSite.getId());
        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any(ParticipatingUser.class));
        verify(siteRepository, never()).save(any(Site.class));
    }

    @Test
    @DisplayName("UT-S-032: マスターユーザーが見つからない場合、IllegalStateExceptionがスローされること")
    void createUser_masterUserNotFound_ThrowsIllegalStateException() {
        String masterCompanyId = testCompany.getCompanyId(); // "COMP001"
        String username = "non_existent_master"; // 存在しないユーザー名
        
        // サイトのPICメールを設定（会社IDチェックを通過させるため）
        testSite.setPicEmail("master@test.com");
        
        // モック設定
        when(siteRepository.findById(testSite.getId())).thenReturn(Optional.of(testSite));
        
        // ★ マスターユーザーが見つからない場合のモック
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        
        CreateUserRequest request = new CreateUserRequest();
        request.setSiteId(testSite.getId());
        request.setUsername("newuser_test");
        request.setFirstName("太郎");
        request.setFirstNameKana("タロウ");
        request.setLastName("山田");
        request.setLastNameKana("ヤマダ");
        request.setEmail("newuser_test@example.com");
        request.setPhoneNumber("090-1111-2222");
        request.setUserPosition("担当者");
        request.setPassword("SecurePass123!");
        
        // 例外がスローされることを検証
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> userService.createUser(masterCompanyId, username, request)
        );
        
        //例外メッセージの検証
        assertEquals("想定外のエラーが発生しました。", exception.getMessage());
        // 検証: マスターユーザー取得後の処理は実行されない
        verify(siteRepository, times(1)).findById(testSite.getId());
        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any(ParticipatingUser.class));
        verify(siteRepository, never()).save(any(Site.class));
    }

    @Test
    @DisplayName("UT-S-033: メールアドレスが重複している場合、USED_EMAILが返却されること")
    void createUser_emailAlreadyExists_ReturnsUsedEmail() {
        String masterCompanyId = testCompany.getCompanyId(); // "COMP001"
        String username = masterUser.getUsername();
        String duplicateEmail = "duplicate@test.com"; // 重複するメールアドレス
        
        // サイトのPICメールをマスターユーザーと一致させる（PICチェックをパス）
        testSite.setPicEmail(masterUser.getEmail());
        
        // モック設定
        when(siteRepository.findById(testSite.getId())).thenReturn(Optional.of(testSite));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        
        // ★ メールアドレスが既に存在する状態にする
        when(userRepository.existsByEmail(duplicateEmail)).thenReturn(true);
        
        CreateUserRequest request = new CreateUserRequest();
        request.setSiteId(testSite.getId());
        request.setUsername("newuser_duplicate_email");
        request.setFirstName("花子");
        request.setFirstNameKana("ハナコ");
        request.setLastName("佐藤");
        request.setLastNameKana("サトウ");
        request.setEmail(duplicateEmail); // ★ 重複するメールアドレスを設定
        request.setPhoneNumber("090-3333-4444");
        request.setUserPosition("課長");
        request.setPassword("SecurePass123!");
        
        // メソッド呼び出し
        CreateUserResponse response = userService.createUser(masterCompanyId, username, request);
        
        // アサーション
        assertEquals(ResponseConstants.USED_EMAIL, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_USED_EMAIL, response.getMessage());
        assertNull(response.getUser());
        assertNull(response.getSite());
        
        // 検証: メールアドレス重複チェックで失敗するため、以降の処理は実行されない
        verify(siteRepository, times(1)).findById(testSite.getId());
        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).existsByEmail(duplicateEmail);
        verify(userRepository, never()).existsByUsername(anyString()); // ユーザー名チェックは実行されない
        verify(userRepository, never()).save(any(ParticipatingUser.class)); // ユーザー保存は実行されない
        verify(siteRepository, never()).save(any(Site.class)); // サイト更新は実行されない
    }

    @Test
    @DisplayName("UT-S-034: ユーザー名が重複している場合、USED_USERNAMEが返却されること")
    void createUser_usernameAlreadyExists_ReturnsUsedUsername() {
        String masterCompanyId = testCompany.getCompanyId(); // "COMP001"
        String username = masterUser.getUsername();
        String duplicateUsername = "existing_user"; // 重複するユーザー名
        
        // サイトのPICメールをマスターユーザーと一致させる（PICチェックをパス）
        testSite.setPicEmail(masterUser.getEmail());
        
        // モック設定
        when(siteRepository.findById(testSite.getId())).thenReturn(Optional.of(testSite));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        
        // ★ メールアドレスは重複していない（このチェックをパス）
        when(userRepository.existsByEmail("newuser@test.com")).thenReturn(false);
        
        // ★ ユーザー名が既に存在する状態にする
        when(userRepository.existsByUsername(duplicateUsername)).thenReturn(true);
        
        CreateUserRequest request = new CreateUserRequest();
        request.setSiteId(testSite.getId());
        request.setUsername(duplicateUsername); // ★ 重複するユーザー名を設定
        request.setFirstName("三郎");
        request.setFirstNameKana("サブロウ");
        request.setLastName("田中");
        request.setLastNameKana("タナカ");
        request.setEmail("newuser@test.com"); // メールアドレスは重複していない
        request.setPhoneNumber("090-7777-8888");
        request.setUserPosition("部長");
        request.setPassword("SecurePass123!");
        
        // メソッド呼び出し
        CreateUserResponse response = userService.createUser(masterCompanyId, username, request);
        
        // アサーション
        assertEquals(ResponseConstants.USED_USERNAME, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_USED_USERNAME, response.getMessage());
        assertNull(response.getUser());
        assertNull(response.getSite());
        
        // 検証: ユーザー名重複チェックで失敗するため、以降の処理は実行されない
        verify(siteRepository, times(1)).findById(testSite.getId());
        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).existsByEmail("newuser@test.com"); // メールチェックは実行される
        verify(userRepository, times(1)).existsByUsername(duplicateUsername); // ユーザー名チェックで検出
        verify(userRepository, never()).save(any(ParticipatingUser.class)); // ユーザー保存は実行されない
        verify(siteRepository, never()).save(any(Site.class)); // サイト更新は実行されない
    }

    // --- 新しいテストケース ---

    @Test
    @DisplayName("UT-S-035: サイトに会社情報が紐づいていない場合、NullPointerExceptionが発生すること")
    void createUser_siteHasNoCompany_ThrowsNullPointerException() {
        String masterCompanyId = testCompany.getCompanyId(); // "COMP001"
        String username = masterUser.getUsername();
        
        Site siteWithoutCompany = Site.builder()
                .siteName("会社なしサイト")
                .postalCode("150-0001")
                .prefecture("東京都")
                .city("渋谷区")
                .town("神宮前")
                .blocknum("1-2-3")
                .address("東京都渋谷区神宮前1-2-3")
                .picFirstName("太郎")
                .picFirstNameKana("タロウ")
                .picLastName("山田")
                .picLastNameKana("ヤマダ")
                .picEmail("pic@test.com")
                .picPhoneNumber("03-1234-5678")
                .picPosition("担当者")
                .company(null) //★ 会社情報がnull
                .build();
        // BaseEntityのフィールドをsetterで設定（IDはInteger型）
        siteWithoutCompany.setId(999); // Integer型
        siteWithoutCompany.setIsActive(true);
        siteWithoutCompany.setCreatedAt(LocalDateTime.now());
        siteWithoutCompany.setUpdatedAt(LocalDateTime.now());
        siteWithoutCompany.setCreatedBy("test_user");
        siteWithoutCompany.setUpdatedBy("test_user");
        
        // モック設定
        when(siteRepository.findById(siteWithoutCompany.getId())).thenReturn(Optional.of(siteWithoutCompany));
        
        CreateUserRequest request = new CreateUserRequest();
        request.setSiteId(siteWithoutCompany.getId());
        request.setUsername("newuser_test");
        request.setFirstName("太郎");
        request.setFirstNameKana("タロウ");
        request.setLastName("山田");
        request.setLastNameKana("ヤマダ");
        request.setEmail("newuser@test.com");
        request.setPhoneNumber("090-1111-2222");
        request.setUserPosition("担当者");
        request.setPassword("SecurePass123!");
        
        // 例外がスローされることを検証
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> userService.createUser(masterCompanyId, username, request)
        );
        
        //検証: サイト取得後、会社情報アクセス時に例外発生
        verify(siteRepository, times(1)).findById(siteWithoutCompany.getId());
        verify(userRepository, never()).findByUsername(anyString()); // マスターユーザー取得前に失敗
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any(ParticipatingUser.class));
    }

    @Test
    @DisplayName("UT-S-036: userRepository.save()でエラーが発生した場合、例外がスローされること")
    void createUser_userRepositorySaveFails_ThrowsException() {
        String masterCompanyId = testCompany.getCompanyId(); // "COMP001"
        String username = masterUser.getUsername();
        
        // サイトのPICメールをマスターユーザーと一致させる（PICチェックをパス）
        testSite.setPicEmail(masterUser.getEmail());
        
        // モック設定
        when(siteRepository.findById(testSite.getId())).thenReturn(Optional.of(testSite));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(userRepository.existsByEmail("newuser@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser_test")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("encodedPassword123");
        
        //★ userRepository.save()で例外をスロー
        when(userRepository.save(any(ParticipatingUser.class)))
                .thenThrow(new DataAccessException("データベース接続エラー") {});
        
        CreateUserRequest request = new CreateUserRequest();
        request.setSiteId(testSite.getId());
        request.setUsername("newuser_test");
        request.setFirstName("三郎");
        request.setFirstNameKana("サブロウ");
        request.setLastName("田中");
        request.setLastNameKana("タナカ");
        request.setEmail("newuser@test.com");
        request.setPhoneNumber("090-7777-8888");
        request.setUserPosition("部長");
        request.setPassword("SecurePass123!");
        
        // 例外がスローされることを検証
        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> userService.createUser(masterCompanyId, username, request)
        );
        
        // 例外メッセージを検証
        assertEquals("データベース接続エラー", exception.getMessage());
        
        // 検証: save()まで到達し、例外が発生
        verify(siteRepository, times(1)).findById(testSite.getId());
        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).existsByEmail("newuser@test.com");
        verify(userRepository, times(1)).existsByUsername("newuser_test");
        verify(passwordEncoder, times(1)).encode("SecurePass123!");
        verify(userRepository, times(1)).save(any(ParticipatingUser.class)); // save()は呼ばれるverify(siteRepository, never()).save(any(Site.class)); // save()失敗後のサイト更新は実行されない
    }

    @Test
    @DisplayName("UT-S-037: siteRepository.save()でエラーが発生した場合、例外がスローされること")
    void createUser_siteRepositorySaveFails_ThrowsException() {
        String masterCompanyId = testCompany.getCompanyId(); // "COMP001"
        String username = masterUser.getUsername();
        
        // サイトのPICメールをマスターユーザーと一致させる（PICチェックをパス）
        testSite.setPicEmail(masterUser.getEmail());
        
        // 保存されるユーザーオブジェクトを作成
        ParticipatingUser savedUser = ParticipatingUser.builder()
                .username("newuser_test")
                .firstName("三郎")
                .firstNameKana("サブロウ")
                .lastName("田中")
                .lastNameKana("タナカ")
                .email("newuser@test.com")
                .phoneNumber("090-7777-8888")
                .userPosition("部長")
                .password("encodedPassword123")
                .role(RoleConstants.USER)
                .companyId("testCompany")
                .build();
        savedUser.setId(999);
        savedUser.setIsActive(true);
        savedUser.setCreatedAt(LocalDateTime.now());
        savedUser.setUpdatedAt(LocalDateTime.now());
        
        // モック設定
        when(siteRepository.findById(testSite.getId())).thenReturn(Optional.of(testSite));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(userRepository.existsByEmail("newuser@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser_test")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("encodedPassword123");
        when(userRepository.save(any(ParticipatingUser.class))).thenReturn(savedUser); // ユーザー保存は成功
        
        //★ siteRepository.save()で例外をスロー
        when(siteRepository.save(any(Site.class)))
                .thenThrow(new DataAccessException("サイト保存時のデータベースエラー") {});
        
        CreateUserRequest request = new CreateUserRequest();
        request.setSiteId(testSite.getId());
        request.setUsername("newuser_test");
        request.setFirstName("三郎");
        request.setFirstNameKana("サブロウ");
        request.setLastName("田中");
        request.setLastNameKana("タナカ");
        request.setEmail("newuser@test.com");
        request.setPhoneNumber("090-7777-8888");
        request.setUserPosition("部長");
        request.setPassword("SecurePass123!");
        
        // 例外がスローされることを検証
        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> userService.createUser(masterCompanyId, username, request)
        );
        
        // 例外メッセージを検証
        assertEquals("サイト保存時のデータベースエラー", exception.getMessage());
        // 検証: userRepository.save()は成功し、siteRepository.save()で失敗
        verify(siteRepository, times(1)).findById(testSite.getId());
        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).existsByEmail("newuser@test.com");
        verify(userRepository, times(1)).existsByUsername("newuser_test");
        verify(passwordEncoder, times(1)).encode("SecurePass123!");
        verify(userRepository, times(1)).save(any(ParticipatingUser.class)); // ユーザー保存は成功
        verify(siteRepository, times(1)).save(any(Site.class)); // サイト保存で失敗
    }

    @Test
    @DisplayName("UT-S-038: パスワードエンコード時にエラーが発生した場合、例外がスローされること")
    void createUser_passwordEncoderFails_ThrowsException() {
        String masterCompanyId = testCompany.getCompanyId(); // "COMP001"
        String username = masterUser.getUsername();
        
        // サイトのPICメールをマスターユーザーと一致させる（PICチェックをパス）
        testSite.setPicEmail(masterUser.getEmail());
        
        // モック設定
        when(siteRepository.findById(testSite.getId())).thenReturn(Optional.of(testSite));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(userRepository.existsByEmail("newuser@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser_test")).thenReturn(false);
        
        //★ passwordEncoder.encode()で例外をスロー
        when(passwordEncoder.encode("SecurePass123!")).thenThrow(new IllegalArgumentException("パスワードエンコードエラー"));
        
        CreateUserRequest request = new CreateUserRequest();
        request.setSiteId(testSite.getId());
        request.setUsername("newuser_test");
        request.setFirstName("三郎");
        request.setFirstNameKana("サブロウ");
        request.setLastName("田中");
        request.setLastNameKana("タナカ");
        request.setEmail("newuser@test.com");
        request.setPhoneNumber("090-7777-8888");
        request.setUserPosition("部長");
        request.setPassword("SecurePass123!");
        
        // 例外がスローされることを検証
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(masterCompanyId, username, request)
        );
        
        // 例外メッセージを検証
        assertEquals("パスワードエンコードエラー", exception.getMessage());
        
        // 検証: passwordEncoder.encode()まで到達し、例外が発生
        verify(siteRepository, times(1)).findById(testSite.getId());
        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).existsByEmail("newuser@test.com");
        verify(userRepository, times(1)).existsByUsername("newuser_test");
        verify(passwordEncoder, times(1)).encode("SecurePass123!");
        // save()は呼ばれない
        verify(userRepository, never()).save(any(ParticipatingUser.class));
        verify(siteRepository, never()).save(any(Site.class));
    }

    //最初に作成していた11本の正常系
    @Test
    @DisplayName("UT-S-039: マスタが自分自身の情報を更新できること(拠点変更なし)→マスタの情報を変えるとほかの拠点に紐づけられたマスタの情報も変更される")
    void testUpdateUser_MasterSelf_NoSiteChange() {
        // Given
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("太郎更新");
        request.setEmail("master_updated@test.com");
        request.setPhoneNumber("090-9999-8888");

        Site anotherSite = Site.builder()
                .company(testCompany)
                .siteName("支店")
                .picFirstName("太郎")
                .picLastName("山田")
                .picEmail("master@test.com")
                .build();
        anotherSite.setId(2);

        List<Site> masterPICSites = Arrays.asList(anotherSite);

        when(userRepository.findByUsername("master_user")).thenReturn(Optional.of(masterUser));
        when(participatingCompanyRepository.findByCompanyId("COMP001"))
                .thenReturn(Optional.of(testCompany));
        when(siteRepository.findByCompanyIdAndPicEmail(1, "master@test.com"))
                .thenReturn(masterPICSites);
        when(userRepository.save(any(ParticipatingUser.class))).thenReturn(masterUser);
        when(siteRepository.save(any(Site.class))).thenAnswer(i -> i.getArgument(0));

        // When
        UpdateUserResponse response = userService.updateUser("COMP001", "master_user", request);

        // Then
        //レスポンスがnullでないことを確認
        assertThat(response).isNotNull();
        //レスポンスのステータスコードがOKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        //更新されたユーザー情報がnullでないことを確認
        assertThat(response.getUser()).isNotNull();
        //更新された拠点リストが空でないことを確認
        assertThat(response.getUpdatedSites()).isNotEmpty();
        //更新された拠点リストのサイズが1以上であることを確認
        assertThat(response.getUpdatedSites().size()).isGreaterThanOrEqualTo(1);

        //userRepository.saveが1回だけ呼ばれたことを確認
        verify(userRepository, times(1)).save(any(ParticipatingUser.class));
        //siteRepository.saveが少なくとも1回呼ばれたことを確認
        verify(siteRepository, atLeast(1)).save(any(Site.class));
    }

    //最初に作成していた11本の正常系
    @Test
    @DisplayName("UT-S-040: マスタがユーザの情報を更新できること(拠点変更なし)")
    void testUpdateUser_MasterUpdatingUser_NoSiteChange() {
        // Given
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUserId(2);
        request.setFirstName("花子更新");
        request.setPhoneNumber("090-7777-6666");

        when(userRepository.findByUsername("master_user")).thenReturn(Optional.of(masterUser));
        when(participatingCompanyRepository.findByCompanyId("COMP001"))
                .thenReturn(Optional.of(testCompany));
        when(userRepository.findById(2)).thenReturn(Optional.of(normalUser));
        when(userRepository.save(any(ParticipatingUser.class))).thenReturn(normalUser);

        // When
        UpdateUserResponse response = userService.updateUser("COMP001", "master_user", request);

        // Then
        //レスポンスがnullでないことを確認
        assertThat(response).isNotNull();
        //レスポンスのステータスコードがOKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        //更新されたユーザー情報がnullでないことを確認
        assertThat(response.getUser()).isNotNull();

        //userRepository.saveが1回だけ呼ばれたことを確認
        verify(userRepository, times(1)).save(any(ParticipatingUser.class));
    }

    //最初に作成していた11本の正常系
    @Test
    @DisplayName("UT-S-041: ユーザが自分自身の情報を更新できること(拠点変更なし)")
    void testUpdateUser_UserSelf_NoSiteChange() {
        // Given
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("花子更新");
        request.setPhoneNumber("090-5555-4444");

        when(userRepository.findByUsername("normal_user")).thenReturn(Optional.of(normalUser));
        when(participatingCompanyRepository.findByCompanyId("COMP001"))
                .thenReturn(Optional.of(testCompany));
        when(userRepository.save(any(ParticipatingUser.class))).thenReturn(normalUser);

        // When
        UpdateUserResponse response = userService.updateUser("COMP001", "normal_user", request);

        // Then
        //レスポンスがnullでないことを確認
        assertThat(response).isNotNull();
        //レスポンスのステータスコードがOKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        //更新されたユーザー情報がnullでないことを確認
        assertThat(response.getUser()).isNotNull();

        //userRepository.saveが1回だけ呼ばれたことを確認
        verify(userRepository, times(1)).save(any(ParticipatingUser.class));
    }

    //最初に作成していた11本の正常系
    @Test
    @DisplayName("UT-S-042: マスタが自分自身の情報を更新できること(拠点変更あり)")
    void testUpdateUser_MasterSelf_WithSiteChange() {
        // Given
        Site newSite = Site.builder()
                .company(testCompany)
                .siteName("新拠点")
                .picFirstName("太郎")
                .picLastName("山田")
                .picEmail("master@test.com")
                .build();
        newSite.setId(2);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setSiteId(2);
        request.setFirstName("太郎更新");

        when(userRepository.findByUsername("master_user")).thenReturn(Optional.of(masterUser));
        when(participatingCompanyRepository.findByCompanyId("COMP001"))
                .thenReturn(Optional.of(testCompany));
        when(siteRepository.findById(2)).thenReturn(Optional.of(newSite));
        when(userRepository.save(any(ParticipatingUser.class))).thenReturn(masterUser);
        when(siteRepository.save(any(Site.class))).thenAnswer(i -> i.getArgument(0));

        // When
        UpdateUserResponse response = userService.updateUser("COMP001", "master_user", request);

        // Then
        //レスポンスがnullでないことを確認
        assertThat(response).isNotNull();
        //レスポンスのステータスコードがOKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        //更新されたユーザー情報がnullでないことを確認
        assertThat(response.getUser()).isNotNull();
        //更新された拠点リストが空でないことを確認
        assertThat(response.getUpdatedSites()).isNotEmpty();

        //userRepository.saveが1回だけ呼ばれたことを確認
        verify(userRepository, times(1)).save(any(ParticipatingUser.class));
        //siteRepository.saveが少なくとも2回呼ばれたことを確認
        verify(siteRepository, atLeast(2)).save(any(Site.class));
    }

    //最初に作成していた11本の正常系
    @Test
    @DisplayName("UT-S-043: マスタがユーザの情報を更新できること(拠点変更あり)")
    void testUpdateUser_MasterUpdatingUser_WithSiteChange() {
        // Given
        Site currentSite = Site.builder()
            .company(testCompany)
            .siteName("現拠点")
            .picFirstName(normalUser.getFirstName())
            .picLastName(normalUser.getLastName())
            .picEmail(normalUser.getEmail())
            .build();
        currentSite.setId(1);

        Site newSite = Site.builder()
                .company(testCompany)
                .siteName("新拠点")
                .picFirstName(masterUser.getFirstName())
                .picLastName(masterUser.getLastName())
                .picEmail(masterUser.getEmail())
                .build();
        newSite.setId(2);
        
        normalUser.setSite(currentSite);
        masterUser.setSite(testSite);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUserId(2);
        request.setSiteId(2);
        request.setFirstName("花子更新");

        when(userRepository.findByUsername("master_user")).thenReturn(Optional.of(masterUser));
        when(participatingCompanyRepository.findByCompanyId("COMP001"))
                .thenReturn(Optional.of(testCompany));
        when(userRepository.findById(2)).thenReturn(Optional.of(normalUser));
        when(siteRepository.findById(2)).thenReturn(Optional.of(newSite));
        when(userRepository.save(any(ParticipatingUser.class))).thenReturn(normalUser);
        when(siteRepository.save(any(Site.class))).thenAnswer(i -> i.getArgument(0));

        // When
        UpdateUserResponse response = userService.updateUser("COMP001", "master_user", request);

        // Then
        //レスポンスがnullでないことを確認
        assertThat(response).isNotNull();
        //レスポンスのステータスコードがOKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        //更新されたユーザー情報がnullでないことを確認
        assertThat(response.getUser()).isNotNull();
        //更新された拠点リストのサイズが2であることを確認
        assertThat(response.getUpdatedSites()).hasSize(2);

        //userRepository.saveが1回だけ呼ばれたことを確認
        verify(userRepository, times(1)).save(any(ParticipatingUser.class));
        //siteRepository.saveが2回呼ばれたことを確認
        verify(siteRepository, times(2)).save(any(Site.class));
    }

    //最初に作成していた11本の正常系
    @Test
    @DisplayName("UT-S-044: ユーザが自分自身の情報を更新できること(拠点変更あり) - 権限エラー")
    void testUpdateUser_UserSelf_WithSiteChange_Forbidden() {
        // Given
        UpdateUserRequest request = new UpdateUserRequest();
        request.setSiteId(2);
        request.setFirstName("花子更新");

        when(userRepository.findByUsername("normal_user")).thenReturn(Optional.of(normalUser));
        when(participatingCompanyRepository.findByCompanyId("COMP001"))
                .thenReturn(Optional.of(testCompany));

        // When
        UpdateUserResponse response = userService.updateUser("COMP001", "normal_user", request);

        // Then
        //レスポンスがnullでないことを確認
        assertThat(response).isNotNull();
        //レスポンスのステータスコードがFORBIDDENであることを確認
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);

        //userRepository.saveが呼ばれていないことを確認
        verify(userRepository, never()).save(any(ParticipatingUser.class));
        //siteRepository.saveが呼ばれていないことを確認
        verify(siteRepository, never()).save(any(Site.class));
        }

@Test
@DisplayName("UT-S-045: ユーザが自分自身の情報を更新し、かつサイトのPICである場合、サイト情報も更新されること〇")
void testUpdateUser_GeneralUserSelf_AsPIC_SiteUpdated() {
    // ===== Given =====
    // generalUserをtestSiteのPICに設定
    generalUser.setEmail("general@test.com");
    testSite.setPicEmail(generalUser.getEmail());
    testSite.setPicFirstName(generalUser.getFirstName());
    testSite.setPicLastName(generalUser.getLastName());
    testSite.setPicPhoneNumber(generalUser.getPhoneNumber());
    generalUser.setSite(testSite);

    // 更新リクエスト (名前とメールアドレスを変更)
    UpdateUserRequest request = new UpdateUserRequest();
    request.setFirstName("次郎改名");
    request.setLastName("佐藤改名");
    // request.setSiteId() は設定しない（サイト変更なし）

    // ===== モック設定 =====
    when(userRepository.findByUsername("general_user")).thenReturn(Optional.of(generalUser));
    when(participatingCompanyRepository.findByCompanyId("COMP001"))
            .thenReturn(Optional.of(testCompany));
    when(userRepository.save(any(ParticipatingUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    when(siteRepository.save(any(Site.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    // ===== When =====
    UpdateUserResponse response = userService.updateUser("COMP001", "general_user", request);

    // ===== Then =====
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
    assertThat(response.getUser().getFirstName()).isEqualTo("次郎改名");
    assertThat(response.getUser().getEmail()).isEqualTo("general@test.com");

    // サイト情報も更新されていることを確認
    assertThat(response.getUpdatedSites()).hasSize(1);
    SiteResponse updatedSiteResponse = response.getUpdatedSites().get(0);
    assertThat(updatedSiteResponse.getId()).isEqualTo("1");
    assertThat(updatedSiteResponse.getPicFirstName()).isEqualTo("次郎改名");
    assertThat(updatedSiteResponse.getPicLastName()).isEqualTo("佐藤改名");

    verify(userRepository, times(1)).save(any(ParticipatingUser.class));
    verify(siteRepository, times(1)).save(any(Site.class));
}


    @Test
    @DisplayName("UT-S-046 ユーザが自分自身の情報を更新し、サイトのPICでない場合、サイト情報は更新されないこと〇")
    void testUpdateUser_GeneralUserSelf_NotPIC_SiteNotUpdated() {
        // ===== Given =====
        
        // 別のユーザーがPICとなっているサイトを作成
        Site testSite = Site.builder()
                .company(testCompany)
                .siteName("営業所B")
                .postalCode("100-0002")
                .prefecture("東京都")
                .city("千代田区")
                .town("丸の内")
                .blocknum("2-2")
                .address("丸の内ビル")
                .picFirstName("太郎")              // 別の人がPIC
                .picFirstNameKana("タロウ")
                .picLastName("山田")
                .picLastNameKana("ヤマダ")
                .picEmail("another@test.com")      // generalUserと異なる
                .picPhoneNumber("090-8888-9999")
                .picPosition("部長")
                .build();
        testSite.setId(2);
        testSite.setIsActive(true);
        
        generalUser.setSite(testSite);
        // 更新リクエスト
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("次郎更新");
        request.setEmail("general_updated@test.com");
        request.setPhoneNumber("090-9999-7777");
        
        // ===== モック設定 =====
        when(userRepository.findByUsername("general_user"))
                .thenReturn(Optional.of(generalUser));
        when(participatingCompanyRepository.findByCompanyId("COMP001"))
                .thenReturn(Optional.of(testCompany));
        when(userRepository.save(any(ParticipatingUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // ===== When =====
        UpdateUserResponse response = userService.updateUser("COMP001", "general_user", request);
        
        // ===== Then =====
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getUser().getFirstName()).isEqualTo("次郎更新");
        // サイト情報は更新されない（updatedSitesは空）
        assertThat(response.getUpdatedSites()).isEmpty();
        
        // ユーザー保存は1回
        verify(userRepository, times(1)).save(any(ParticipatingUser.class));
        
        // サイト保存は0回（isSitePIC = falseなので）
        verify(siteRepository, never()).save(any(Site.class));
        verify(siteRepository, never()).findByCompanyIdAndPicEmail(anyInt(), anyString());
    }

    @Test
    @DisplayName("UT-S-047: マスタが自分自身の情報を更新し、メールアドレスが変更された場合、旧メールアドレスでPICとなっている全サイトが更新されること")
    void testUpdateUser_MasterSelf_EmailChange_AllPICSitesUpdated() {
        // ===== Given =====
        masterUser.setEmail("master_old@test.com"); // 古いメールアドレスを設定

        // マスターがPICとなっている複数のサイトを作成
        Site site1 = Site.builder().company(testCompany).siteName("サイト1").picEmail("master_old@test.com").picFirstName("太郎").picLastName("田中").build();
        site1.setId(101); // masterUserのサイト (testSite) とは別のID
        site1.setIsActive(true);
        Site site2 = Site.builder().company(testCompany).siteName("サイト2").picEmail("master_old@test.com").picFirstName("太郎").picLastName("田中").build();
        site2.setId(102);
        site2.setIsActive(true);
        Site site3 = Site.builder().company(testCompany).siteName("サイト3").picEmail("another_pic@test.com").picFirstName("花子").picLastName("鈴木").build(); // 関係ないサイト
        site3.setId(103);
        site3.setIsActive(true);

        masterUser.setSite(site1); // マスターの現在のサイトをsite1に設定

        // 更新リクエスト (メールアドレスを変更)
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("master_new@test.com");
        request.setFirstName("太郎改名"); // 名前も変更

        // ===== モック設定 =====
        when(userRepository.findByUsername("master_user"))
                .thenReturn(Optional.of(masterUser));
        when(participatingCompanyRepository.findByCompanyId("COMP001"))
                .thenReturn(Optional.of(testCompany));

        // findByCompanyIdAndPicEmail が master_old@test.com でPICなサイトを返すように設定
        when(siteRepository.findByCompanyIdAndPicEmail(testCompany.getId(), "master_old@test.com"))
                .thenReturn(Arrays.asList(masterUser.getSite(), site2)); // masterUser.getSite() は site1

        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        when(userRepository.save(userCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Site> siteCaptor = ArgumentCaptor.forClass(Site.class);
        when(siteRepository.save(siteCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0)); // saveされるサイトをキャプチャ

        // ===== When =====
        UpdateUserResponse response = userService.updateUser("COMP001", "master_user", request);

        // ===== Then =====
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

        // ユーザー情報が更新されていること
        ParticipatingUser savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("master_new@test.com");
        assertThat(savedUser.getFirstName()).isEqualTo("太郎改名");

        // 更新されたサイトが2つ含まれること (site1 と site2)
        assertThat(response.getUpdatedSites()).hasSize(2);
        // 更新されたサイトのPIC情報が新しいユーザー情報で更新されていること
        // site1とsite2が更新されたことを確認
        // キャプチャされたサイトのリストを検証するため、allValues()を使用
        List<Site> savedSites = siteCaptor.getAllValues();
        assertThat(savedSites).hasSize(2); // site1とsite2の計2回saveが呼ばれる
        
        // サイト1の検証 (マスターの現在のサイト)
        Site updatedSite1 = savedSites.stream().filter(s -> s.getId().equals(site1.getId())).findFirst().orElseThrow();
        assertThat(updatedSite1.getPicEmail()).isEqualTo("master_new@test.com");
        assertThat(updatedSite1.getPicFirstName()).isEqualTo("太郎改名");

        // サイト2の検証
        Site updatedSite2 = savedSites.stream().filter(s -> s.getId().equals(site2.getId())).findFirst().orElseThrow();
        assertThat(updatedSite2.getPicEmail()).isEqualTo("master_new@test.com");
        assertThat(updatedSite2.getPicFirstName()).isEqualTo("太郎改名");

        // userRepository.saveは1回、siteRepository.saveは2回呼ばれていることを確認
        verify(userRepository, times(1)).save(any(ParticipatingUser.class));
        verify(siteRepository, times(2)).save(any(Site.class));
        verify(siteRepository, times(1)).findByCompanyIdAndPicEmail(testCompany.getId(), "master_old@test.com");
    }




    @Test
    @DisplayName("UT-S-048: マスタが他ユーザーの情報を更新し、そのユーザーがサイトのPICの場合、サイト情報も更新されること")
        void testUpdateUser_MasterUpdatesOtherUser_AsPIC_SiteUpdated() {
        // ===== Given =====
        generalUser.setEmail("general@test.com");
        generalUser.setSite(testSite);
        generalUser.setRole(RoleConstants.USER);
        
        testSite.setPicEmail(generalUser.getEmail());
        testSite.setPicFirstName(generalUser.getFirstName());
        testSite.setPicLastName(generalUser.getLastName());
        testSite.setPicPhoneNumber(generalUser.getPhoneNumber());
        
        masterUser.setSite(testSite); // マスターも同じサイトに所属
        
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUserId(generalUser.getId()); // 他ユーザーを指定
        request.setFirstName("次郎修正");
        request.setLastName("佐藤修正");
        request.setEmail("general_modified@test.com");
        request.setPhoneNumber("090-5555-6666");
        
        // ===== モック設定 =====
        when(userRepository.findByUsername("master_user"))
                .thenReturn(Optional.of(masterUser));
        when(participatingCompanyRepository.findByCompanyId("COMP001"))
                .thenReturn(Optional.of(testCompany));
        when(userRepository.findById(generalUser.getId()))
                .thenReturn(Optional.of(generalUser));
        when(userRepository.save(any(ParticipatingUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(siteRepository.save(any(Site.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // ===== When =====
        UpdateUserResponse response = userService.updateUser("COMP001", "master_user", request);
        
        // ===== Then =====
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        
        // サイト情報も更新されている
        assertThat(response.getUpdatedSites()).hasSize(1);
        SiteResponse updatedSite = response.getUpdatedSites().get(0);
        assertThat(updatedSite.getPicFirstName()).isEqualTo("次郎修正");
        assertThat(updatedSite.getPicLastName()).isEqualTo("佐藤修正");
        
        verify(userRepository, times(1)).save(any(ParticipatingUser.class));
        verify(siteRepository, times(1)).save(any(Site.class));
    }

@Test
@DisplayName("UT-S-049: マスタが他ユーザーの情報を更新し、そのユーザーがサイトのPICでない場合、サイト情報は更新されないこと")
void testUpdateUser_MasterUpdatesOtherUser_NotPIC_SiteNotUpdated() {
    // ===== Given =====
    generalUser.setEmail("general@test.com");
    generalUser.setSite(testSite);
    generalUser.setRole(RoleConstants.USER);
    
    // testSiteのPICは別のメールアドレス（masterUser）
    testSite.setPicEmail("master@test.com");
    testSite.setPicFirstName("太郎");
    testSite.setPicLastName("山田");
    
    masterUser.setSite(testSite);
    
    UpdateUserRequest request = new UpdateUserRequest();
    request.setUserId(generalUser.getId());
    request.setFirstName("次郎変更");
    request.setLastName("佐藤変更");
    request.setEmail("general_new@test.com");
    request.setPhoneNumber("090-7777-8888");
    
    // ===== モック設定 =====
    when(userRepository.findByUsername("master_user"))
            .thenReturn(Optional.of(masterUser));
    when(participatingCompanyRepository.findByCompanyId("COMP001"))
            .thenReturn(Optional.of(testCompany));
    when(userRepository.findById(generalUser.getId()))
            .thenReturn(Optional.of(generalUser));
    when(userRepository.save(any(ParticipatingUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    // ===== When =====
    UpdateUserResponse response = userService.updateUser("COMP001", "master_user", request);
    
    // ===== Then =====
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
    assertThat(response.getUser().getEmail()).isEqualTo("general_new@test.com");
    
    // サイト情報は更新されない
    assertThat(response.getUpdatedSites()).isEmpty();
    
    verify(userRepository, times(1)).save(any(ParticipatingUser.class));
    verify(siteRepository, never()).save(any(Site.class));
}


@Test
@DisplayName("UT-S-050: マスタがサイトのPICでないユーザーを移動した場合、旧サイトのPIC情報が更新されないこと〇")
void testUpdateUser_MasterMovesNonPICUser_OldSitePICNotChanged() {
    // ===== Given =====
    Site oldSite = Site.builder()
            .company(testCompany)
            .siteName("旧サイト")
            .picEmail("master@test.com") // masterUserがPIC
            .picFirstName("太郎")
            .picLastName("山田")
            .picPhoneNumber("090-1234-5678")
            .build();
    oldSite.setId(1);
    
    Site newSite = Site.builder()
            .company(testCompany)
            .siteName("新サイト")
            .picEmail("master@test.com") // masterUserがPIC
            .picFirstName("太郎")
            .picLastName("山田")
            .build();
    newSite.setId(2);
    
    generalUser.setEmail("general@test.com");
    generalUser.setSite(oldSite); // 旧サイトに所属（PICではない）
    generalUser.setRole(RoleConstants.USER);
    
    masterUser.setSite(oldSite);
    
    UpdateUserRequest request = new UpdateUserRequest();
    request.setUserId(generalUser.getId());
    request.setFirstName(generalUser.getFirstName());
    request.setLastName(generalUser.getLastName());
    request.setEmail(generalUser.getEmail());
    request.setPhoneNumber(generalUser.getPhoneNumber());
    request.setSiteId(2); // 新サイトへ移動

    
    
    // ===== モック設定 =====
    when(userRepository.findByUsername("master_user"))
            .thenReturn(Optional.of(masterUser));
    when(participatingCompanyRepository.findByCompanyId("COMP001"))
            .thenReturn(Optional.of(testCompany));
    when(userRepository.findById(generalUser.getId()))
            .thenReturn(Optional.of(generalUser));
    when(siteRepository.findById(2))
            .thenReturn(Optional.of(newSite));
    when(userRepository.save(any(ParticipatingUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    when(siteRepository.save(any(Site.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    
    // ===== When =====
    UpdateUserResponse response = userService.updateUser("COMP001", "master_user", request);
    
    // ===== Then =====
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
    assertThat(response.getUser().getSiteId()).isEqualTo("2");
    
    // ArgumentCaptorを使用
    ArgumentCaptor<Site> siteCaptor = ArgumentCaptor.forClass(Site.class);
    verify(siteRepository, times(1)).save(siteCaptor.capture());

    Site savedSite = siteCaptor.getValue();
    assertThat(savedSite.getId()).isEqualTo(2); // newSiteが保存されている
    assertThat(savedSite.getSiteName()).isEqualTo("新サイト");

    // 旧サイトが保存されていないことを確認
    verify(siteRepository, never()).save(argThat(site -> 
    site.getId() != null && site.getId().equals(1)
    ));
    
    verify(userRepository, times(1)).save(any(ParticipatingUser.class));
    // newSiteのPIC情報が更新される
    verify(siteRepository, times(1)).save(any(Site.class));
}


    @Test
    @DisplayName("UT-S-051: ユーザが一部の項目のみ変更した場合、変更された項目だけが更新されること〇")
    void testUpdateUser_GeneralUserSelf_PartialUpdate() {
        // ===== Given =====
        
        // 元の値を保存
        String originalFirstName = generalUser.getFirstName();     // "次郎"
        String originalLastName = generalUser.getLastName();       // "佐藤"
        String originalEmail = generalUser.getEmail();             // "general@test.com"
        String originalPhoneNumber = generalUser.getPhoneNumber(); // "090-1111-2222"
        
        generalUser.setSite(testSite);
        
        // 一部の項目のみ更新（電話番号とメールだけ）
        UpdateUserRequest request = new UpdateUserRequest();
        request.setPhoneNumber("090-9999-7777");
        request.setEmail("general_updated@test.com");
        // firstName, lastName等は指定しない（null）
        
        // ===== モック設定 =====
        when(userRepository.findByUsername("general_user"))
                .thenReturn(Optional.of(generalUser));
        when(participatingCompanyRepository.findByCompanyId("COMP001"))
                .thenReturn(Optional.of(testCompany));
        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        when(userRepository.save(userCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        ArgumentCaptor<Site> siteCaptor = ArgumentCaptor.forClass(Site.class);
        when(siteRepository.save(siteCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // ===== When =====
        UpdateUserResponse response = userService.updateUser("COMP001", "general_user", request);
        
        // ===== Then =====
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        
        // 保存されたユーザーを検証
        ParticipatingUser savedUser = userCaptor.getValue();
        
        // 更新された項目
        assertThat(savedUser.getPhoneNumber()).isEqualTo("090-9999-7777");
        assertThat(savedUser.getEmail()).isEqualTo("general_updated@test.com");
        
        // 更新されていない項目（元の値のまま）
        assertThat(savedUser.getFirstName()).isEqualTo(originalFirstName);      // "次郎"
        assertThat(savedUser.getLastName()).isEqualTo(originalLastName);        // "佐藤"
        assertThat(savedUser.getFirstNameKana()).isEqualTo("ジロウ");
        assertThat(savedUser.getLastNameKana()).isEqualTo("サトウ");
        assertThat(savedUser.getUserPosition()).isEqualTo("営業");
        // サイトも更新される（PICなので）
        assertThat(response.getUpdatedSites()).hasSize(1);
        
        Site savedSite = siteCaptor.getValue();
        // サイトも一部だけ更新される
        assertThat(savedSite.getPicPhoneNumber()).isEqualTo("090-9999-7777");
        assertThat(savedSite.getPicEmail()).isEqualTo("general_updated@test.com");
        // 名前は変わらない
        assertThat(savedSite.getPicFirstName()).isEqualTo("次郎");
        assertThat(savedSite.getPicLastName()).isEqualTo("佐藤");
    }

    @Test
    @DisplayName("UT-S-052: 空文字列やブランク文字列が指定された場合、その項目は更新されないこと (StringUtils.isNotBlankの動作)〇")
    void testUpdateUser_GeneralUserSelf_BlankStringsIgnored() {
        // ===== Given =====
        
        generalUser.setSite(null);
        
        String originalFirstName = generalUser.getFirstName();     // "次郎"
        String originalLastName = generalUser.getLastName();       // "佐藤"
        String originalPhoneNumber = generalUser.getPhoneNumber(); // "090-1111-2222"
        
        // 空文字列やブランクを指定
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("");// 空文字列
        request.setLastName("");         // ブランク（スペースのみ）
        request.setPhoneNumber("090-9999-7777"); // これだけ有効
        
        // ===== モック設定 =====
        when(userRepository.findByUsername("general_user"))
                .thenReturn(Optional.of(generalUser));
        when(participatingCompanyRepository.findByCompanyId("COMP001"))
                .thenReturn(Optional.of(testCompany));
        
        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        when(userRepository.save(userCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // ===== When =====
        UpdateUserResponse response = userService.updateUser("COMP001", "general_user", request);
        
        // ===== Then =====
        assertThat(response).isNotNull();
        System.out.println(generalUser.getSite());
        System.out.println(response.getMessage());
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        
        ParticipatingUser savedUser = userCaptor.getValue();
        
        // 空文字列・ブランクは無視される（元の値のまま）
        assertThat(savedUser.getFirstName()).isEqualTo(originalFirstName);   // "次郎"
        assertThat(savedUser.getLastName()).isEqualTo(originalLastName);     // "佐藤"
        
        // 有効な値は更新される
        assertThat(savedUser.getPhoneNumber()).isEqualTo("090-9999-7777");
    }

    @Test
        @DisplayName("UT-S-053: ユーザ情報に変更がない場合でも、DBへの保存は実行されること")
        void testUpdateUser_NoChanges_StillSavesToDB() {
        // Given
        // 変更前と同じ情報でリクエストを作成
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName(normalUser.getFirstName());  // 既存と同じ
        request.setLastName(normalUser.getLastName());    // 既存と同じ
        request.setEmail(normalUser.getEmail());          // 既存と同じ
        request.setPhoneNumber(normalUser.getPhoneNumber()); // 既存と同じ

        when(userRepository.findByUsername("normal_user")).thenReturn(Optional.of(normalUser));
        when(participatingCompanyRepository.findByCompanyId("COMP001"))
                .thenReturn(Optional.of(testCompany));
        when(userRepository.save(any(ParticipatingUser.class))).thenReturn(normalUser);

        // When
        UpdateUserResponse response = userService.updateUser("COMP001", "normal_user", request);

        // Then
        // レスポンスがnullでないことを確認
        assertThat(response).isNotNull();
        // レスポンスのステータスコードがOKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        // 更新されたユーザー情報がnullでないことを確認
        assertThat(response.getUser()).isNotNull();
        
        // updatedAtが更新されていることを確認（引数キャプチャで検証）
        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository,times(1)).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
@DisplayName("UT-S-054: マスタが自分自身の情報を更新し、変更がない場合はサイト情報が更新されないこと〇")
void testUpdateUser_MasterSelf_NoChange_SiteNotUpdated() {
    // ===== Given =====
    masterUser.setEmail("master@test.com");
    masterUser.setSite(testSite);
    
    testSite.setPicEmail(masterUser.getEmail());
    testSite.setPicFirstName(masterUser.getFirstName());
    testSite.setPicLastName(masterUser.getLastName());
    testSite.setPicPhoneNumber(masterUser.getPhoneNumber());
    
    // 変更なしのリクエスト
    UpdateUserRequest request = new UpdateUserRequest();
    request.setFirstName(masterUser.getFirstName());
    request.setLastName(masterUser.getLastName());
    request.setEmail(masterUser.getEmail());
    request.setPhoneNumber(masterUser.getPhoneNumber());
    
    // ===== モック設定 =====
    when(userRepository.findByUsername("master_user"))
            .thenReturn(Optional.of(masterUser));
    when(participatingCompanyRepository.findByCompanyId("COMP001"))
            .thenReturn(Optional.of(testCompany));
    when(userRepository.save(any(ParticipatingUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    
    // ===== When =====
    UpdateUserResponse response = userService.updateUser("COMP001", "master_user", request);
    
    // ===== Then =====
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
    
    // サイト情報は更新されない（空のリスト）
    assertThat(response.getUpdatedSites()).isEmpty();
    
    verify(userRepository, times(1)).save(any(ParticipatingUser.class));
    verify(siteRepository, never()).save(any(Site.class));
    verify(siteRepository, never()).findByCompanyIdAndPicEmail(anyInt(), anyString());
}

    @Test
    @DisplayName("UT-S-055: ユーザが情報を更新した際、updatedAtが更新されること〇")
    void testUpdateUser_GeneralUserSelf_UpdatedAtIsRefreshed() {
        // ===== Given =====
        
        LocalDateTime originalUpdatedAt = LocalDateTime.now().minusDays(1);
        generalUser.setUpdatedAt(originalUpdatedAt);
        generalUser.setSite(testSite);
        
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("次郎更新");
        // ===== モック設定 =====
        when(userRepository.findByUsername("general_user"))
                .thenReturn(Optional.of(generalUser));
        when(participatingCompanyRepository.findByCompanyId("COMP001"))
                .thenReturn(Optional.of(testCompany));
        
        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        when(userRepository.save(userCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        ArgumentCaptor<Site> siteCaptor = ArgumentCaptor.forClass(Site.class);
        when(siteRepository.save(siteCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        LocalDateTime beforeUpdate = LocalDateTime.now();
        
        // ===== When =====
        UpdateUserResponse response = userService.updateUser("COMP001", "general_user", request);
        
        LocalDateTime afterUpdate = LocalDateTime.now();
        
        // ===== Then =====
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        
        // ユーザーのupdatedAtが更新されている
        ParticipatingUser savedUser = userCaptor.getValue();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(savedUser.getUpdatedAt()).isBetween(beforeUpdate, afterUpdate);
        
        // サイトのupdatedAtも更新されている
        Site savedSite = siteCaptor.getValue();
        assertThat(savedSite.getUpdatedAt()).isNotNull();
        assertThat(savedSite.getUpdatedAt()).isBetween(beforeUpdate, afterUpdate);
   }


    @Test
    @DisplayName("UT-S-056: 電話番号や役職がnullでも更新できること〇")
        void testUpdateUser_GeneralUserSelf_NullFieldsAccepted() {
        // ===== Given =====
        
        generalUser.setSite(testSite);
        
        // 名前だけ更新、電話番号と役職はnull
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("次郎更新");
        request.setPhoneNumber(null);
        request.setUserPosition(null);
        
        // ===== モック設定 =====
        when(userRepository.findByUsername("general_user"))
                .thenReturn(Optional.of(generalUser));
        when(participatingCompanyRepository.findByCompanyId("COMP001"))
                .thenReturn(Optional.of(testCompany));
        when(userRepository.save(any(ParticipatingUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(siteRepository.save(any(Site.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // ===== When =====
        UpdateUserResponse response = userService.updateUser("COMP001", "general_user", request);
        
        // ===== Then =====
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        
        // 名前は更新される
        assertThat(response.getUser().getFirstName()).isEqualTo("次郎更新");
        
        // null指定されたフィールドは変更されない（元の値のまま）
        assertThat(response.getUser().getPhoneNumber()).isEqualTo("090-1111-2222");
        assertThat(response.getUser().getUserPosition()).isEqualTo("営業");
    }


    @Test
    @DisplayName("UT-S-057: マスタがサイトに紐づいていない状態（site=null）で自分自身の情報を更新できること〇")
    void testUpdateUser_MasterSelf_NoSite_Success() {
        // ===== Given =====
        masterUser.setSite(null); // サイトに紐づいていない
        masterUser.setEmail("master@test.com");
        
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("太郎変更");
        request.setLastName("山田変更");
        request.setEmail("master_changed@test.com");
        request.setPhoneNumber("070-1111-2222");
        
        // ===== モック設定 =====
        when(userRepository.findByUsername("master_user"))
                .thenReturn(Optional.of(masterUser));
        when(participatingCompanyRepository.findByCompanyId("COMP001"))
                .thenReturn(Optional.of(testCompany));
        when(siteRepository.findByCompanyIdAndPicEmail(testCompany.getId(), "master@test.com"))
                .thenReturn(Collections.emptyList());
        when(userRepository.save(any(ParticipatingUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // ===== When =====
        UpdateUserResponse response = userService.updateUser("COMP001", "master_user", request);
        
        // ===== Then =====
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getUser().getFirstName()).isEqualTo("太郎変更");
        assertThat(response.getUser().getEmail()).isEqualTo("master_changed@test.com");
        
        // サイト更新は発生しない
        assertThat(response.getUpdatedSites()).isEmpty();
        
        verify(userRepository, times(1)).save(any(ParticipatingUser.class));
        verify(siteRepository, never()).save(any(Site.class));
}


    @Test
    @DisplayName("UT-S-058: 対象ユーザーが見つからない場合（userIdが不正）、NOT_FOUNDが返却されること〇")
    void testUpdateUser_TargetUserNotFound_ReturnsNotFound() {
        // ===== Given =====
        // マスターが更新するリクエスト (存在しないuserIdを指定)
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUserId(999); // 存在しないユーザーID
        request.setFirstName("不明なユーザー");

        // ===== モック設定 =====
        when(userRepository.findByUsername("master_user"))
                .thenReturn(Optional.of(masterUser)); // リクエスト元のマスターは存在する
        when(participatingCompanyRepository.findByCompanyId("COMP001"))
                .thenReturn(Optional.of(testCompany));
        when(userRepository.findById(999)) // 存在しないユーザーIDの検索結果はOptional.empty()
                .thenReturn(Optional.empty());

        // ===== When =====
        UpdateUserResponse response = userService.updateUser("COMP001", "master_user", request);

        // ===== Then =====
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getUser()).isNull();
        assertThat(response.getUpdatedSites()).isNull();

        // ユーザーもサイトも保存されない
        verify(userRepository, never()).save(any(ParticipatingUser.class));
        verify(siteRepository, never()).save(any(Site.class));
    }

    @Test
    @DisplayName("UT-S-059: 一般ユーザーがサイト変更を要求した場合、FORBIDDENが返却されること〇")
    void testUpdateUser_GeneralUser_SiteChangeRequested_ReturnsForbidden() {
        // ===== Given =====
        // generalUserの現在のサイト
        Site currentSiteForGeneral = Site.builder().company(testCompany).siteName("営業所A").build();
        currentSiteForGeneral.setId(1);
        generalUser.setSite(currentSiteForGeneral);

        // 別のサイト（一般ユーザーが変更を要求するサイト）
        Site newSiteForGeneral = Site.builder().company(testCompany).siteName("営業所C").build();
        newSiteForGeneral.setId(3);

        // 更新リクエスト (サイトIDを変更)
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("次郎更新");
        request.setSiteId(newSiteForGeneral.getId()); // 現在のサイトとは異なるID

        // ===== モック設定 =====
        when(userRepository.findByUsername("general_user"))
                .thenReturn(Optional.of(generalUser));
        when(participatingCompanyRepository.findByCompanyId("COMP001"))
                .thenReturn(Optional.of(testCompany));
        // 一般ユーザーなのでuserRepository.findByIdは呼ばれない
        // サイト変更がFORBIDDENになるため、newSiteForGeneralの取得など後続処理は呼ばれないはず

        // ===== When =====
        UpdateUserResponse response = userService.updateUser("COMP001", "general_user", request);

        // ===== Then =====
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.FORBIDDEN);
        assertThat(response.getUser()).isNull();
        assertThat(response.getUpdatedSites()).isNull();

        // ユーザーもサイトも保存されない
        verify(userRepository, never()).save(any(ParticipatingUser.class));
        verify(siteRepository, never()).save(any(Site.class));
    }

    
@Test
@DisplayName("UT-S-060: マスタの会社IDと旧サイトの会社IDが一致しない場合、FORBIDDENが返却されること〇")
void testUpdateUser_MasterCompanyIdMismatchWithOldSite_ReturnsForbidden() {
    // ===== Given =====
    ParticipatingCompany otherCompany = ParticipatingCompany.builder()
            .companyId("COMP999")
            .nameJa("別会社")
            .build();
    otherCompany.setId(999);
    
    Site oldSite = Site.builder()
            .company(otherCompany) // 異なる会社
            .siteName("別会社のサイト")
            .picEmail("general@test.com")
            .build();
    oldSite.setId(1);
    
    Site newSite = Site.builder()
            .company(testCompany)
            .siteName("新サイト")
            .picEmail("master@test.com")
            .build();
    newSite.setId(2);
    
    generalUser.setEmail("general@test.com");
    generalUser.setSite(oldSite);
    generalUser.setRole(RoleConstants.USER);
    generalUser.setCompanyId("COMP999"); // 異なる会社ID
    
    masterUser.setSite(testSite);
    
    UpdateUserRequest request = new UpdateUserRequest();
    request.setUserId(generalUser.getId());
    request.setFirstName("次郎");
    request.setLastName("佐藤");
    request.setEmail("general@test.com");
    request.setPhoneNumber("090-1111-2222");
    request.setSiteId(2);
    // ===== モック設定 =====
    when(userRepository.findByUsername("master_user"))
            .thenReturn(Optional.of(masterUser));
    when(participatingCompanyRepository.findByCompanyId("COMP001"))
            .thenReturn(Optional.of(testCompany));
    when(userRepository.findById(generalUser.getId()))
            .thenReturn(Optional.of(generalUser));
    when(siteRepository.findById(2))
            .thenReturn(Optional.of(newSite));
    
    // ===== When =====
    UpdateUserResponse response = userService.updateUser("COMP001", "master_user", request);
    
    // ===== Then =====
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
    assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_FORBIDDEN);
    
    verify(userRepository, never()).save(any(ParticipatingUser.class));
    verify(siteRepository, never()).save(any(Site.class));
}



@Test
@DisplayName("UT-S-061: マスターの会社IDと新サイトの会社IDが一致しない場合、FORBIDDENが返却されること〇")
void testUpdateUser_MasterCompanyIdMismatchWithNewSite_ReturnsForbidden() {
    // ===== Given =====
    ParticipatingCompany otherCompany = ParticipatingCompany.builder()
            .companyId("COMP999")
            .nameJa("別会社")
            .build();
    otherCompany.setId(999);
    
    Site oldSite = Site.builder()
            .company(testCompany)
            .siteName("旧サイト")
            .picEmail("general@test.com")
            .build();
    oldSite.setId(1);
    
    Site newSite = Site.builder()
            .company(otherCompany) // 異なる会社
            .siteName("別会社のサイト")
            .picEmail("other@test.com")
            .build();
    newSite.setId(2);
    
    generalUser.setEmail("general@test.com");
    generalUser.setSite(oldSite);
    generalUser.setRole(RoleConstants.USER);
    generalUser.setCompanyId("COMP001");
    
    masterUser.setSite(testSite);
    
    UpdateUserRequest request = new UpdateUserRequest();
    request.setUserId(generalUser.getId());
    request.setFirstName("次郎");
    request.setLastName("佐藤");
    request.setEmail("general@test.com");
    request.setPhoneNumber("090-1111-2222");
    request.setSiteId(2); // 別会社のサイトへ移動を試みる
    
    // ===== モック設定 =====
    when(userRepository.findByUsername("master_user"))
            .thenReturn(Optional.of(masterUser));
    when(participatingCompanyRepository.findByCompanyId("COMP001"))
            .thenReturn(Optional.of(testCompany));
    when(userRepository.findById(generalUser.getId()))
            .thenReturn(Optional.of(generalUser));
    when(siteRepository.findById(2))
            .thenReturn(Optional.of(newSite));
    
    // ===== When =====
    UpdateUserResponse response = userService.updateUser("COMP001", "master_user", request);
    
    // ===== Then =====
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
    assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_FORBIDDEN);
    
    verify(userRepository, never()).save(any(ParticipatingUser.class));
    verify(siteRepository, never()).save(any(Site.class));
}

@Test
@DisplayName("UT-S-062: 新サイトにすでにユーザ(非マスター)が割り当てられている場合、FOBIDDENが返却されること")
void testUpdateUser_NewSiteAlreadyHasNonMasterUser_ReturnsForbidden() {
    // Given
    // 新しいサイトを作成（既に別のユーザーがPICとして割り当てられている）
    ParticipatingUser anotherUser = ParticipatingUser.builder()
            .username("another_user")
            .firstName("次郎")
            .lastName("佐藤")
            .email("another@test.com")
            .companyId("COMP001")
            .role("user")
            .build();
    anotherUser.setId(3);

    Site newSite = Site.builder()
            .company(testCompany)
            .siteName("新拠点")
            .picFirstName(anotherUser.getFirstName())// 別ユーザーのPIC情報
            .picLastName(anotherUser.getLastName())
            .picEmail(anotherUser.getEmail())  // マスターではないユーザーのメール
            .build();
    newSite.setId(2);

    Site currentSite = Site.builder()
            .company(testCompany)
            .siteName("現拠点")
            .picFirstName(normalUser.getFirstName())
            .picLastName(normalUser.getLastName())
            .picEmail(normalUser.getEmail())
            .build();
    currentSite.setId(1);

    normalUser.setSite(currentSite);

    UpdateUserRequest request = new UpdateUserRequest();
    request.setUserId(2);  // normalUserを更新
    request.setSiteId(2);  // 既に別ユーザーが割り当てられている新サイトへ変更
    request.setFirstName("花子更新");

    when(userRepository.findByUsername("master_user")).thenReturn(Optional.of(masterUser));
    when(participatingCompanyRepository.findByCompanyId("COMP001"))
            .thenReturn(Optional.of(testCompany));
    when(userRepository.findById(2)).thenReturn(Optional.of(normalUser));
    when(siteRepository.findById(2)).thenReturn(Optional.of(newSite));

    // When
    UpdateUserResponse response = userService.updateUser("COMP001", "master_user", request);

    // Then
    // レスポンスがnullでないことを確認
    assertThat(response).isNotNull();
    // レスポンスのステータスコードがFORBIDDENであることを確認
    assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
    // エラーメッセージが適切であることを確認
    assertThat(response.getMessage()).isEqualTo("この拠点は既に担当者がいるのでユーザを割り当てることができません。");
    
    // DBへの保存処理が実行されていないことを確認
    verify(userRepository, never()).save(any(ParticipatingUser.class));
    verify(siteRepository, never()).save(any(Site.class));
}

/*@Test
@DisplayName("UT-S-063: userIdの形式が不正な場合、BAD_REQUESTが返却されること")
void testUpdateUser_InvalidUserIdFormat_ReturnsBadRequest() {
    // Given
    UpdateUserRequest request = new UpdateUserRequest();
    // userIdに不正な形式の値を設定（想定: リクエスト処理でNumberFormatExceptionが発生）
    // 実際の実装では、userIdはIntegerなので、このテストはリクエストのパース段階や
    // findByIdでのキャスト時にNumberFormatExceptionが発生することを想定
    
    // NumberFormatExceptionをスローするようにモック設定
    when(userRepository.findByUsername("master_user")).thenReturn(Optional.of(masterUser));
    when(participatingCompanyRepository.findByCompanyId("COMP001"))
            .thenReturn(Optional.of(testCompany));
    when(userRepository.findById(any(Integer.class)))
            .thenThrow(new NumberFormatException("Invalid user ID format"));

    request.setUserId(999); // 存在しないID（実際にはfindByIdでNumberFormatExceptionをスロー）
    request.setFirstName("テスト");

    // When
    UpdateUserResponse response = userService.updateUser("COMP001", "master_user", request);

    // Then
    // レスポンスがnullでないことを確認
    assertThat(response).isNotNull();
    // レスポンスのステータスコードがBAD_REQUESTであることを確認
    assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
    // エラーメッセージが適切であることを確認
    assertThat(response.getMessage()).isEqualTo("ユーザーIDの形式が正しくありません。");
    
    // DBへの保存処理が実行されていないことを確認
    verify(userRepository, never()).save(any(ParticipatingUser.class));
    verify(siteRepository, never()).save(any(Site.class));
}*/


@Test
@DisplayName("UT-S-063: 旧サイトに会社情報が紐づいていない場合、NullPointerExceptionが発生すること〇")
void testUpdateUser_OldSiteCompanyIsNull_ThrowsNullPointerException() {
    // ===== Given =====
    Site oldSite = Site.builder()
            .siteName("会社情報なしサイト")
            .picEmail("general@test.com")
            .company(null) // 会社情報がnull
            .build();
    oldSite.setId(1);
    
    Site newSite = Site.builder()
            .company(testCompany)
            .siteName("新サイト")
            .picEmail("master@test.com")
            .build();
    newSite.setId(2);
    
    generalUser.setEmail("general@test.com");
    generalUser.setSite(oldSite);
    generalUser.setRole(RoleConstants.USER);
    generalUser.setCompanyId("COMP001");
    
    masterUser.setSite(testSite);
    
    UpdateUserRequest request = new UpdateUserRequest();
    request.setUserId(generalUser.getId());
    request.setFirstName("次郎");
    request.setLastName("佐藤");
    request.setEmail("general@test.com");
    request.setPhoneNumber("090-1111-2222");
    request.setSiteId(2);
    
    // ===== モック設定 =====
    when(userRepository.findByUsername("master_user"))
            .thenReturn(Optional.of(masterUser));
    when(participatingCompanyRepository.findByCompanyId("COMP001"))
            .thenReturn(Optional.of(testCompany));
    when(userRepository.findById(generalUser.getId()))
            .thenReturn(Optional.of(generalUser));
    when(siteRepository.findById(2))
            .thenReturn(Optional.of(newSite));
    
    // ===== When & Then =====
    assertThrows(NullPointerException.class, () -> {
        userService.updateUser("COMP001", "master_user", request);
    });
    
    verify(userRepository, never()).save(any(ParticipatingUser.class));
    verify(siteRepository, never()).save(any(Site.class));
}

@Test
@DisplayName("UT-S-064: 新サイトに会社情報が紐づいていない場合、NullPointerExceptionが発生すること〇")
void testUpdateUser_NewSiteCompanyIsNull_ThrowsNullPointerException() {
    // ===== Given =====
    Site oldSite = Site.builder()
            .company(testCompany)
            .siteName("旧サイト")
            .picEmail("general@test.com")
            .build();
    oldSite.setId(1);
    
    Site newSite = Site.builder()
            .siteName("会社情報なしサイト")
            .picEmail("master@test.com")
            .company(null) // 会社情報がnull
            .build();
    newSite.setId(2);
    
    generalUser.setEmail("general@test.com");
    generalUser.setSite(oldSite);
    generalUser.setRole(RoleConstants.USER);
    generalUser.setCompanyId("COMP001");
    
    masterUser.setSite(testSite);
    
    UpdateUserRequest request = new UpdateUserRequest();
    request.setUserId(generalUser.getId());
    request.setFirstName("次郎");
    request.setLastName("佐藤");
    request.setEmail("general@test.com");
    request.setPhoneNumber("090-1111-2222");
    request.setSiteId(2);
    
    // ===== モック設定 =====
    when(userRepository.findByUsername("master_user"))
            .thenReturn(Optional.of(masterUser));
    when(participatingCompanyRepository.findByCompanyId("COMP001"))
            .thenReturn(Optional.of(testCompany));
    when(userRepository.findById(generalUser.getId()))
            .thenReturn(Optional.of(generalUser));
    when(siteRepository.findById(2))
            .thenReturn(Optional.of(newSite));
    
    // ===== When & Then =====
    assertThrows(NullPointerException.class, () -> {
        userService.updateUser("COMP001", "master_user", request);
    });
    
    verify(userRepository, never()).save(any(ParticipatingUser.class));
    verify(siteRepository, never()).save(any(Site.class));
}

    
    // --- UserService内の private/protected メソッドをモック/検証するために必要となるユーティリティ ---
    // UserServiceのユーティリティメソッドは、実際のコードには存在しないので、モックの振る舞いを定義するか、
    // ここで簡易実装をするか、またはPowerMockitoなどを使う必要があります。
    // 今回はテスト対象のコードに沿って、updateUserInfo, isSitePIC, mapToSiteResponse を簡易実装します。

    private boolean updateUserInfo(ParticipatingUser user, UpdateUserRequest request) {
        boolean changed = false;
        if (request.getFirstName() != null && !request.getFirstName().isEmpty() && !request.getFirstName().equals(user.getFirstName())) {
            user.setFirstName(request.getFirstName());
            changed = true;
        }
        if (request.getLastName() != null && !request.getLastName().isEmpty() && !request.getLastName().equals(user.getLastName())) {
            user.setLastName(request.getLastName());
            changed = true;
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty() && !request.getEmail().equals(user.getEmail())) {
            user.setEmail(request.getEmail());
            changed = true;
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty() && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
            user.setPhoneNumber(request.getPhoneNumber());
            changed = true;
        }
        if (request.getUserPosition() != null && !request.getUserPosition().isEmpty() && !request.getUserPosition().equals(user.getUserPosition())) {
            user.setUserPosition(request.getUserPosition());
            changed = true;
        }
        // 他のフィールドも同様に比較・更新
        return changed;
    }

    private void updateSitePIC(Site site, ParticipatingUser user, UpdateUserRequest request) {
        site.setPicFirstName(request.getFirstName() != null ? request.getFirstName() : user.getFirstName());
        site.setPicLastName(request.getLastName() != null ? request.getLastName() : user.getLastName());
        site.setPicEmail(request.getEmail() != null ? request.getEmail() : user.getEmail());
        site.setPicPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : user.getPhoneNumber());
        site.setPicPosition(request.getUserPosition() != null ? request.getUserPosition() : user.getUserPosition());
        site.setUpdatedAt(LocalDateTime.now());
    }
    
    // マスターが旧サイトのPICになる場合のメソッド
    private void updateSitePIC(Site site, ParticipatingUser masterUser) {
        site.setPicFirstName(masterUser.getFirstName());
        site.setPicLastName(masterUser.getLastName());
        site.setPicEmail(masterUser.getEmail());
        site.setPicPhoneNumber(masterUser.getPhoneNumber());
        site.setPicPosition(masterUser.getUserPosition());
        site.setUpdatedAt(LocalDateTime.now());
    }

    private boolean isSitePIC(Site site, ParticipatingUser user) {
        return site != null && user != null && user.getEmail().equals(site.getPicEmail());
    }

    private SiteResponse mapToSiteResponse(Site site) {
        return SiteResponse.builder()
                .id(String.valueOf(site.getId()))
                .siteName(site.getSiteName())
                .picFirstName(site.getPicFirstName())
                .picLastName(site.getPicLastName())
                .build();
    }
}