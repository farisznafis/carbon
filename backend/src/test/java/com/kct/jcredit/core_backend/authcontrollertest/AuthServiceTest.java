package com.kct.jcredit.core_backend.authcontrollertest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.util.List;
// ログ用
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.AdminLoginRequest;
import com.kct.jcredit.core_backend.dto.request.LoginRequest;
import com.kct.jcredit.core_backend.dto.request.PasswordChangeRequest;
import com.kct.jcredit.core_backend.dto.response.AdminLoginResponse;
import com.kct.jcredit.core_backend.dto.response.LoginResponse;
import com.kct.jcredit.core_backend.dto.response.PasswordChangeResponse;
import com.kct.jcredit.core_backend.dto.response.UserProfileResponse;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatorCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.TokenBlacklistService;
import com.kct.jcredit.core_backend.service.UserService;
import com.kct.jcredit.core_backend.service.UsernameAvailabilityService;
import com.kct.jcredit.core_backend.service.impl.AuthServiceImpl;
import com.kct.jcredit.core_backend.service.impl.UserServiceImpl;
import com.kct.jcredit.core_backend.util.JwtUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AuthServiceTest {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceTest.class);

    @Mock
    ParticipatingCompanyRepository participatingCompanyRepository;
    @Mock 
    OperatingUserRepository operatingUserRepository;
    @Mock 
    OperatorCompanyRepository operatingCompanyRepository;
    @Mock
    ParticipatingUserRepository userRepository;
    @Mock 
    SiteRepository siteRepository;
    @Mock
    BCryptPasswordEncoder passwordEncoder;
    @Mock 
    TokenBlacklistService blacklistService;
    @Mock
    JwtUtil jwtUtil;
    @InjectMocks
    AuthServiceImpl authServiceImpl; 
    @Mock
    UserService userServiceImpl;
    @Mock   
    UsernameAvailabilityService usernameAvailabilityService;


    private ParticipatingUser participatingUser;
    // private ParticipatingCompany operatingCompany;
    private OperatingUser operatingUser;
    private OperatorCompany operatorCompany;

    @BeforeEach
    void setUp() {
        
        // テスト用会社の初期化
        ParticipatingCompany dummyCompany = new ParticipatingCompany();
        dummyCompany.setId(123);
        dummyCompany.setCompanyId("123");
        
        // テスト用ユーザーの初期化
        participatingUser = new ParticipatingUser();
        participatingUser.setId(1);
        participatingUser.setUsername("user1");
        participatingUser.setEmail("user1@example.com");
        participatingUser.setPassword("$2a$...oldhash..."); // モックでマッチング
        participatingUser.setRole("user");
        participatingUser.setFirstName("First");
        participatingUser.setLastName("Last");
        participatingUser.setIsActive(true);
        participatingUser.setCompany(dummyCompany);
        
        // テスト用OperatingUserの初期化
        operatingUser = new OperatingUser();
        operatingUser.setId(1);
        operatingUser.setUsername("operatingUser1");
        operatingUser.setEmail("operating1@example.com");
        operatingUser.setPassword("$2a$...oldhash...");
        operatingUser.setRole("admin");
        operatingUser.setFirstName("Operating");
        operatingUser.setLastName("User");
        operatingUser.setIsActive(true);
        operatorCompany = new OperatorCompany();
        operatorCompany.setId(123);
        operatorCompany.setCompanyId("123");
        operatorCompany.setIsActive(true);
        operatingUser.setCompany(operatorCompany);

        userServiceImpl = new UserServiceImpl(
        userRepository,
        participatingCompanyRepository,
        siteRepository,
        passwordEncoder,
        blacklistService,
        usernameAvailabilityService
    );
}

    //正しい企業ID/正しいメールアドレス/パスワードでログイン成功
    @Test
    @DisplayName("UT-AS-101: 参加企業ユーザーログイン成功")
    void authService_userLogin_email_succes(TestInfo testInfo) throws Exception {
        log.info("=== START {} ===", testInfo.getDisplayName());

        LoginRequest req = new LoginRequest();
        req.setCompanyId("123");
        req.setIdentifier("alice@example.com"); 
        req.setPassword("raw-pass");

        // 企業IDが存在
        ParticipatingCompany existingCompany = new ParticipatingCompany();
        existingCompany.setId(123);
        existingCompany.setCompanyId("123");
        existingCompany.setCompanyStatus(CompanyStatus.ACTIVE);
        when(participatingCompanyRepository.findByCompanyId("123"))
                .thenReturn(Optional.of(existingCompany));

        // ユーザーの用意（ロックなし・有効）
        ParticipatingUser user = new ParticipatingUser();
        user.setId(1);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        ParticipatingCompany company = new ParticipatingCompany();
        company.setId(123);
        company.setCompanyId("123");
        user.setCompany(company); 
        user.setRole("USER");
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setPassword("$2a$...hash..."); // ダミー
        user.setAccountLockedUntil(null);
        user.setLoginAttempts(2); // 成功時に0へリセットされることを検証
        user.setIsActive(true);

        when(userRepository.findByUsernameOrEmailAndCompanyId("alice@example.com", "alice@example.com", 123))
                .thenReturn(Optional.of(user));
        

        // パスワード一致
        when(passwordEncoder.matches("raw-pass", "$2a$...hash...")).thenReturn(true);
        

        // トークン発行
        when(jwtUtil.generateToken(1, "alice", "123", "USER",List.of())).thenReturn("jwt-token-123");

        // Act
        LoginResponse res = authServiceImpl.userLogin(req);


        // Assert
        assertEquals(ResponseConstants.OK, res.getStatusCode());
        assertEquals("jwt-token-123", res.getToken());
        // assertNotNull(res.getUser());
        // assertEquals("alice", res.getUser().getUsername());
        // assertEquals("Smith Alice", res.getUser().getName()); 
        // assertEquals("USER", res.getUser().getRole());
        // assertEquals("alice@example.com", res.getUser().getEmail());
        // assertEquals(123, res.getUser().getCompanyId());
        
        
        // ユーザーの試行回数が0にリセットされ保存されていること
        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        ParticipatingUser saved = userCaptor.getValue();
        assertEquals(0, saved.getLoginAttempts());

        //結果をlogにだす
        System.out.println("authService_userLogin_email_succes() succes");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("response: " + res.getStatusCode());
        System.out.println("token: " + res.getToken());
        // System.out.println("user: " + res.getUser());
        // System.out.println("name: " + res.getUser().getUsername());
        // System.out.println("lastName firstName: " + res.getUser().getName());
        // System.out.println("user/master: " + res.getUser().getRole());
        // System.out.println("Email: " + res.getUser().getEmail());
        // System.out.println("companyID: " + res.getUser().getCompanyId());
        System.out.println("loginAttemptsNum: " + saved.getLoginAttempts());
    }

    @DisplayName("UT-AS-102: 参加企業ユーザーログイン成功（ユーザー名指定）")
    @Test
    void authService_userLogin_username_username_succes() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setCompanyId("123");
        req.setIdentifier("alice"); // username でも email でもOK
        req.setPassword("raw-pass");

        // 企業IDが存在
        ParticipatingCompany existingCompany = new ParticipatingCompany();
        existingCompany.setId(123);
        existingCompany.setCompanyId("123");
        existingCompany.setCompanyStatus(CompanyStatus.ACTIVE);
        when(participatingCompanyRepository.findByCompanyId("123"))
                .thenReturn(Optional.of(existingCompany));
        
        
        // ユーザーの用意（ロックなし・有効）
        ParticipatingUser user = new ParticipatingUser();
        user.setId(1);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        ParticipatingCompany company = new ParticipatingCompany();
        company.setId(123);
        company.setCompanyId("123");
        user.setCompany(company);
        user.setRole("USER");
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setPassword("$2a$...hash..."); // ダミー
        user.setAccountLockedUntil(null);
        user.setLoginAttempts(2); // 成功時に0へリセットされることを検証
        user.setIsActive(true);

        when(userRepository.findByUsernameOrEmailAndCompanyId("alice", "alice", 123))
                .thenReturn(Optional.of(user));
        

        // パスワード一致
        when(passwordEncoder.matches("raw-pass", "$2a$...hash...")).thenReturn(true);
        

        // トークン発行
        when(jwtUtil.generateToken(1, "alice", "123", "USER", List.of())).thenReturn("jwt-token-123");

        // Act
        LoginResponse res = authServiceImpl.userLogin(req);

        // Assert
        assertEquals(ResponseConstants.OK, res.getStatusCode());
        assertEquals("jwt-token-123", res.getToken());
        // assertNotNull(res.getUser());
        // assertEquals("alice", res.getUser().getUsername());
        // assertEquals("Smith Alice", res.getUser().getName()); // 実装：last + space + first
        // assertEquals("USER", res.getUser().getRole());
        // assertEquals("alice@example.com", res.getUser().getEmail());
        // assertEquals(123, res.getUser().getCompanyId());
        
        
        // ユーザーの試行回数が0にリセットされ保存されていること
        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        ParticipatingUser saved = userCaptor.getValue();
        assertEquals(0, saved.getLoginAttempts());

        //結果をlogにだす
        System.out.println("authService_userLogin_username_username_succes() succes");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("response: " + res.getStatusCode());
        System.out.println("token: " + res.getToken());
        // System.out.println("user: " + res.getUser());
        // System.out.println("name: " + res.getUser().getUsername());
        // System.out.println("lastName firstName: " + res.getUser().getName());
        // System.out.println("user/master: " + res.getUser().getRole());
        // System.out.println("Email: " + res.getUser().getEmail());
        // System.out.println("companyID: " + res.getUser().getCompanyId());
        System.out.println("loginAttemptsNum: " + saved.getLoginAttempts());
    }


    //正しい企業ID/正しいメールアドレス/誤ったパスワードでログイン失敗→INVALID_FORMAT
    @Test
    @DisplayName("UT-AS-103: 参加企業ユーザーログイン失敗 - メールパスワード不一致")
    void authService_userLogin_email_password_false() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setCompanyId("123");
        req.setIdentifier("alice@example.com"); // username でも email でもOK
        req.setPassword("raw-pass");

        // 企業IDが存在
        ParticipatingCompany existingCompany = new ParticipatingCompany();
        existingCompany.setId(123);
        existingCompany.setCompanyId("123");
        existingCompany.setCompanyStatus(CompanyStatus.ACTIVE);
        when(participatingCompanyRepository.findByCompanyId("123"))
                .thenReturn(Optional.of(existingCompany));
        
        
        // ユーザーの用意（ロックなし・有効）
        ParticipatingUser user = new ParticipatingUser();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        ParticipatingCompany company = new ParticipatingCompany();
        company.setId(123);
        company.setCompanyId("123");
        user.setCompany(company);
        user.setRole("USER");
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setPassword("$2a$...hash..."); // ダミー
        user.setAccountLockedUntil(null);
        user.setLoginAttempts(2); // 成功時に0へリセットされることを検証
        user.setIsActive(true);

        when(userRepository.findByUsernameOrEmailAndCompanyId("alice@example.com", "alice@example.com", 123))
                .thenReturn(Optional.of(user));
        

        // パスワード一致
        when(passwordEncoder.matches("raw-pass", "$2a$...hash...")).thenReturn(false);
        
        // Act
        LoginResponse res = authServiceImpl.userLogin(req);

        // Assert
        assertEquals(ResponseConstants.INVALID_CREDENTIALS, res.getStatusCode());
        assertEquals(null, res.getToken());
        // assertEquals(null, res.getUser());
        
        // ユーザーの試行回数が0にリセットされ保存されていること
        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        ParticipatingUser saved = userCaptor.getValue();
        assertEquals(3, saved.getLoginAttempts());

        //結果をlogにだす
        System.out.println("authService_userLogin_email_password_false() succes");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("response: " + res.getStatusCode());
        System.out.println("token: " + res.getToken());
        // System.out.println("user: " + res.getUser());
        System.out.println("loginAttemptsNum: " + saved.getLoginAttempts());
    }

    //正しい企業ID/誤ったメールアドレス/正しいパスワードでログイン失敗→INVALID_FORMAT
    @Test
@DisplayName("UT-AS-104: 参加企業ユーザーログイン失敗 - メールアドレス不一致")
    void authService_userLogin_email_email_false() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setCompanyId("123");
        req.setIdentifier("alice@example.co"); // username でも email でもOK
        req.setPassword("raw-pass");

        // 企業IDが存在
        ParticipatingCompany existingCompany = new ParticipatingCompany();
        existingCompany.setId(123);
        existingCompany.setCompanyId("123");
        existingCompany.setCompanyStatus(CompanyStatus.ACTIVE);
        when(participatingCompanyRepository.findByCompanyId("123"))
                .thenReturn(Optional.of(existingCompany));
        
        when(userRepository.findByUsernameOrEmailAndCompanyId("alice@example.co", "alice@example.co", 123))
                .thenReturn(Optional.empty());
                
        // Act
        LoginResponse res = authServiceImpl.userLogin(req);

        //帰ってくる値はINVALID_CREDENTIALS
        // Assert
        assertEquals(ResponseConstants.INVALID_CREDENTIALS, res.getStatusCode());
        assertEquals(null, res.getToken());
        // assertEquals(null, res.getUser());
        
        
        // ユーザーが見つからないので save() は呼ばれない
        verify(userRepository, never()).save(any());

        //結果をlogにだす
        System.out.println("authService_userLogin_email_email_false() succes");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("response: " + res.getStatusCode());
        System.out.println("token: " + res.getToken());
        // System.out.println("user: " + res.getUser());

    }

    //誤った企業ID/正しいメールアドレス/正しいパスワードでログイン失敗→INVALID_FORMAT
    @Test
    @DisplayName("UT-AS-105: 参加企業ユーザーログイン失敗 - 企業ID不一致")
    void authService_userLogin_email_companyId_false() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setCompanyId("COMP12");
        req.setIdentifier("alice@example.com"); // username でも email でもOK
        req.setPassword("raw-pass");

        // 企業IDが見つからない
        when(participatingCompanyRepository.findByCompanyId("COMP12"))
                .thenReturn(Optional.empty());
                
        // Act
        LoginResponse res = authServiceImpl.userLogin(req);

        //帰ってくる値はINVALID_CREDENTIALS
        // Assert
        assertEquals(ResponseConstants.INVALID_CREDENTIALS, res.getStatusCode());
        assertEquals(null, res.getToken());
        // assertEquals(null, res.getUser());
        
        
        // ユーザーが見つからないので save() は呼ばれない
        verify(userRepository, never()).save(any());

        //結果をlogにだす
        System.out.println("authService_userLogin_email_companyId_false() succes");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("response: " + res.getStatusCode());
        System.out.println("token: " + res.getToken());
        // System.out.println("user: " + res.getUser());

    }

    //5回ログイン失敗したらアカウントロックされる
        @Test
    @DisplayName("UT-AS-106: 5回ログイン失敗したら参加企業ユーザーログイン失敗 - アカウントロック")
    void authService_userLogin_account_lock() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setCompanyId("123");
        req.setIdentifier("alice@example.com"); // username でも email でもOK
        req.setPassword("raw-pass");

        // 企業IDが存在
        ParticipatingCompany existingCompany = new ParticipatingCompany();
        existingCompany.setId(123);
        existingCompany.setCompanyId("123");
        existingCompany.setCompanyStatus(CompanyStatus.ACTIVE);
        when(participatingCompanyRepository.findByCompanyId("123"))
                .thenReturn(Optional.of(existingCompany));
        
        
        // ユーザーの用意（ロックなし・有効）
        ParticipatingUser user = new ParticipatingUser();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        ParticipatingCompany company = new ParticipatingCompany();
        company.setId(123);
        company.setCompanyId("123");
        user.setCompany(company);
        user.setRole("USER");
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setPassword("$2a$...hash..."); // ダミー
        user.setAccountLockedUntil(null);
        //アップデート後
        //実際はloginAttempts:5で検証する
        user.setLoginAttempts(5); // 成功時に0へリセットされることを検証
        user.setIsActive(true);

        //ユーザ検索しユーザを返す
        when(userRepository.findByUsernameOrEmailAndCompanyId("alice@example.com", "alice@example.com", 123))
                .thenReturn(Optional.of(user));

        // パスワード一致
        when(passwordEncoder.matches("raw-pass", "$2a$...hash...")).thenReturn(false);

        // 実行直前の時刻
        LocalDateTime before = LocalDateTime.now();

        // Act
        LoginResponse res = authServiceImpl.userLogin(req);

         // 実行直後の時刻
        LocalDateTime after = LocalDateTime.now();
        // Assert
        assertEquals(ResponseConstants.ACCOUNT_LOCKED, res.getStatusCode());
        assertEquals(null, res.getToken());
        //assertEquals(null, res.getUser());

        
        
        // ユーザーの試行回数が0にリセットされ保存されていること
        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        ParticipatingUser saved = userCaptor.getValue();
        //アップデート後
        //実際はexpected:4ではなく5で検証する
        assertEquals(5, saved.getLoginAttempts());

        LocalDateTime lockedUntil = saved.getAccountLockedUntil();

        
        assertTrue(lockedUntil.isAfter(before.plusHours(12).minusSeconds(1)));
        assertTrue(lockedUntil.isBefore(after.plusHours(12).plusSeconds(1)));

        //結果をlogにだす
        System.out.println("authService_userLogin_account_lock() succes");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("response: " + res.getStatusCode());
        System.out.println("token: " + res.getToken());
        //System.out.println("user: " + res.getUser());
        System.out.println("loginAttemptsNum: " + saved.getLoginAttempts());
        System.out.println("lokedUntil: " + lockedUntil);
    }

        //アカウントロックから12時間経ったらアカウントが解除されたのち、
        //ログイン可能になること
    @Test
    @DisplayName("UT-AS-107: 5回ログイン失敗後、12時間経過すると参加企業ユーザーログイン成功")
    void authService_userLogin_afterLock12Hours_success() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setCompanyId("123");
        req.setIdentifier("alice@example.com"); // username でも email でもOK
        req.setPassword("raw-pass");

        // 企業IDが存在
        ParticipatingCompany existingCompany = new ParticipatingCompany();
        existingCompany.setId(123);
        existingCompany.setCompanyId("123");
        existingCompany.setCompanyStatus(CompanyStatus.ACTIVE);
        when(participatingCompanyRepository.findByCompanyId("123"))
                .thenReturn(Optional.of(existingCompany));
        
        
        // ユーザーの用意（ロックなし・有効）
        ParticipatingUser user = new ParticipatingUser();
        user.setId(1);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        ParticipatingCompany company = new ParticipatingCompany();
        company.setId(123);
        company.setCompanyId("123");
        user.setCompany(company);
        user.setRole("USER");
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setPassword("$2a$...hash..."); // ダミー
        user.setAccountLockedUntil(LocalDateTime.now().minusMinutes(1));
        user.setLoginAttempts(5); // 成功時に0へリセットされることを検証
        user.setIsActive(true);

        when(userRepository.findByUsernameOrEmailAndCompanyId("alice@example.com", "alice@example.com", 123))
                .thenReturn(Optional.of(user));
        

        // パスワード一致
        when(passwordEncoder.matches("raw-pass", "$2a$...hash...")).thenReturn(true);
        

        // トークン発行
        when(jwtUtil.generateToken(1, "alice", "123", "USER", List.of())).thenReturn("jwt-token-123");

        // Act
        LoginResponse res = authServiceImpl.userLogin(req);

        // Assert
        assertEquals(ResponseConstants.OK, res.getStatusCode());
        assertEquals("jwt-token-123", res.getToken());
        // assertNotNull(res.getUser());
        // assertEquals("alice", res.getUser().getUsername());
        // assertEquals("Smith Alice", res.getUser().getName()); // 実装：last + space + first
        // assertEquals("USER", res.getUser().getRole());
        // assertEquals("alice@example.com", res.getUser().getEmail());
        // assertEquals(123, res.getUser().getCompanyId());
        
        
        // ユーザーの試行回数が0にリセットされ保存されていること
        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        ParticipatingUser saved = userCaptor.getValue();
        assertEquals(0, saved.getLoginAttempts());
        //アカウントロック期限時間がnullになっていることを確認
        assertEquals(null, saved.getAccountLockedUntil());

        //結果をlogにだす
        System.out.println("authService_userLogin_afterLock12Hours_success() succes");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("response: " + res.getStatusCode());
        System.out.println("token: " + res.getToken());
        // System.out.println("user: " + res.getUser());
        // System.out.println("name: " + res.getUser().getUsername());
        // System.out.println("lastName firstName: " + res.getUser().getName());
        // System.out.println("user/master: " + res.getUser().getRole());
        // System.out.println("Email: " + res.getUser().getEmail());
        // System.out.println("companyID: " + res.getUser().getCompanyId());
        System.out.println("loginAttemptsNum: " + saved.getLoginAttempts());
        //アカウントロック期限時間がnullになっていることコンソールに出して確認
        System.out.println("accountLockedUntil: " + saved.getAccountLockedUntil());
    }

    //ロックされている状態でログインできないこと
        @Test
        @DisplayName("UT-AS-108: ロックされている状態でログインできないこと")
        void authService_userLogin_accountLocked_failure() throws Exception {
                LoginRequest req = new LoginRequest();
                req.setCompanyId("123");
                req.setIdentifier("alice@example.com"); // username でも email でもOK
                req.setPassword("raw-pass");

                // 企業IDが存在
                ParticipatingCompany existingCompany = new ParticipatingCompany();
                existingCompany.setId(123);
                existingCompany.setCompanyId("123");
                existingCompany.setCompanyStatus(CompanyStatus.ACTIVE);
                when(participatingCompanyRepository.findByCompanyId("123"))
                        .thenReturn(Optional.of(existingCompany));
                
                
                // ユーザーの用意（ロックあり・有効）
                ParticipatingUser user = new ParticipatingUser();
                user.setUsername("alice");
                user.setEmail("alice@example.com");
                ParticipatingCompany company = new ParticipatingCompany();
                company.setId(123);
                company.setCompanyId("123");
                user.setCompany(company);
                user.setRole("USER");
                user.setFirstName("Alice");
                user.setLastName("Smith");
                user.setPassword("$2a$...hash..."); // ダミー
                user.setAccountLockedUntil(LocalDateTime.now().plusHours(10));
                user.setLoginAttempts(5); // 成功時に0へリセットされることを検証
                user.setIsActive(true);

                //ユーザ検索しユーザを返す
                when(userRepository.findByUsernameOrEmailAndCompanyId("alice@example.com", "alice@example.com", 123))
                        .thenReturn(Optional.of(user));
                
                // Act
                LoginResponse res = authServiceImpl.userLogin(req);

                // Assert
                assertEquals(ResponseConstants.ACCOUNT_LOCKED, res.getStatusCode());
                assertEquals(null, res.getToken());
                //assertEquals(null, res.getUser());

                verify(userRepository, never()).save(any());

                //結果をlogにだす
                System.out.println("authService_userLogin_accountLocked_failure() succes");
                System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
                System.out.println("response: " + res.getStatusCode());
                System.out.println("token: " + res.getToken());
                //System.out.println("user: " + res.getUser());

                
        }

        @Test
        @DisplayName("UT-AS-109: ロック中は正しいパスワードでもパスワード照合が行われないこと（ガード確認）")
        void authService_userLogin_locked_passwordMatchNeverCalled() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setCompanyId("123");
        req.setIdentifier("user1");
        req.setPassword("raw-pass"); // 正しいパスワードでも弾かれることを確認

        ParticipatingCompany existingCompany = new ParticipatingCompany();
        existingCompany.setId(123);
        existingCompany.setCompanyId("123");
        existingCompany.setCompanyStatus(CompanyStatus.ACTIVE);
        when(participatingCompanyRepository.findByCompanyId("123"))
                .thenReturn(Optional.of(existingCompany));

        participatingUser.setLoginAttempts(5);
        participatingUser.setAccountLockedUntil(LocalDateTime.now().plusHours(2));
        when(userRepository.findByUsernameOrEmailAndCompanyId("user1", "user1", 123))
                .thenReturn(Optional.of(participatingUser));

        // Act
        LoginResponse res = authServiceImpl.userLogin(req);

        // Assert
        assertEquals(ResponseConstants.ACCOUNT_LOCKED, res.getStatusCode());
        assertNull(res.getToken());

        // ロック中はパスワード照合自体が実行されないこと
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtUtil, never()).generateToken(any(), any(), any(), any(), any());

        System.out.println("authService_userLogin_locked_passwordMatchNeverCalled() success");
        System.out.println("response: " + res.getStatusCode());
        }

        @Test
        @DisplayName("UT-AS-110: ロック解除後の再ログイン成功時、失敗回数カウンタが0にリセットされること")
        void authService_userLogin_afterUnlock_attemptsResetToZero() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setCompanyId("123");
        req.setIdentifier("user1");
        req.setPassword("raw-pass");

        ParticipatingCompany existingCompany = new ParticipatingCompany();
        existingCompany.setId(123);
        existingCompany.setCompanyId("123");
        existingCompany.setCompanyStatus(CompanyStatus.ACTIVE);
        when(participatingCompanyRepository.findByCompanyId("123"))
                .thenReturn(Optional.of(existingCompany));

        // ロック期限が過去（解除済み）、かつ失敗回数が残っている状態
        participatingUser.setLoginAttempts(5);
        participatingUser.setAccountLockedUntil(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByUsernameOrEmailAndCompanyId("user1", "user1", 123))
                .thenReturn(Optional.of(participatingUser));

        when(passwordEncoder.matches("raw-pass", participatingUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(participatingUser.getId(), "user1", "123", "user", List.of()))
                .thenReturn("jwt-token-reset");
        when(userRepository.save(any(ParticipatingUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LoginResponse res = authServiceImpl.userLogin(req);

        // Assert
        assertEquals(ResponseConstants.OK, res.getStatusCode());
        assertEquals("jwt-token-reset", res.getToken());

        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        ParticipatingUser saved = userCaptor.getValue();
        assertEquals(0, saved.getLoginAttempts());
        assertNull(saved.getAccountLockedUntil());

        System.out.println("authService_userLogin_afterUnlock_attemptsResetToZero() success");
        System.out.println("loginAttemptsNum: " + saved.getLoginAttempts());
        }

        @Test
        @DisplayName("UT-AS-111: ログイン成功時、JWTトークンが正しい引数で生成されること")
        void authService_userLogin_jwtGeneratedWithCorrectArgs() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setCompanyId("123");
        req.setIdentifier("user1");
        req.setPassword("raw-pass");

        ParticipatingCompany existingCompany = new ParticipatingCompany();
        existingCompany.setId(123);
        existingCompany.setCompanyId("123");
        existingCompany.setCompanyStatus(CompanyStatus.ACTIVE);
        when(participatingCompanyRepository.findByCompanyId("123"))
                .thenReturn(Optional.of(existingCompany));

        when(userRepository.findByUsernameOrEmailAndCompanyId("user1", "user1", 123))
                .thenReturn(Optional.of(participatingUser));
        when(passwordEncoder.matches("raw-pass", participatingUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(participatingUser.getId(), "user1", "123", "user", List.of()))
                .thenReturn("jwt-token-verify-args");
        when(userRepository.save(any(ParticipatingUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LoginResponse res = authServiceImpl.userLogin(req);

        // Assert
        assertEquals(ResponseConstants.OK, res.getStatusCode());

        // JWT生成が正しい引数（userId, username, companyId, role, authorities）で1回呼ばれたこと
        verify(jwtUtil, times(1)).generateToken(
                participatingUser.getId(), "user1", "123", "user", List.of());

        System.out.println("authService_userLogin_jwtGeneratedWithCorrectArgs() success");
        System.out.println("token: " + res.getToken());
        }

        @Test
        @DisplayName("UT-AS-112: 参加企業が無効化（非アクティブ）状態の場合、ログイン失敗すること")
        void authService_userLogin_companyInactive_loginFails() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setCompanyId("123");
        req.setIdentifier("user1");
        req.setPassword("raw-pass");

        // 企業が非アクティブ状態
        ParticipatingCompany inactiveCompany = new ParticipatingCompany();
        inactiveCompany.setId(123);
        inactiveCompany.setCompanyId("123");
        inactiveCompany.setCompanyStatus(CompanyStatus.REJECTED);
        when(participatingCompanyRepository.findByCompanyId("123"))
                .thenReturn(Optional.of(inactiveCompany));

        // Act
        LoginResponse res = authServiceImpl.userLogin(req);

        // Assert
        assertEquals(ResponseConstants.FORBIDDEN, res.getStatusCode());
        assertNull(res.getToken());

        // 企業が無効な場合、ユーザー検索自体が行われないこと
        verify(userRepository, never()).findByUsernameOrEmailAndCompanyId(any(), any(), any());

        System.out.println("authService_userLogin_companyInactive_loginFails() success");
        System.out.println("response: " + res.getStatusCode());
        }

    //正しいユーザーネーム/パスワードでログイン成功
    @Test
    @DisplayName("UT-AS-201: 管理者正しいユーザーネーム/パスワードでログイン成功")
    void authService_adminLogin_username_succes() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setIdentifier("alice"); // username でも email でもOK
        req.setPassword("raw-pass");
        
        // ユーザーの用意（ロックなし・有効）
        OperatingUser operator = new OperatingUser();
        operator.setId(1);
        operator.setUsername("alice");
        operator.setEmail("alice@example.com");
        OperatorCompany company = new OperatorCompany();
        company.setId(123);
        company.setCompanyId("123");
        company.setIsActive(true);
        operator.setCompany(company);
        operator.setRole("USER");
        operator.setFirstName("Alice");
        operator.setLastName("Smith");
        operator.setPassword("$2a$...hash..."); // ダミー
        operator.setAccountLockedUntil(null);
        operator.setLoginAttempts(2); // 成功時に0へリセットされることを検証
        operator.setIsActive(true);

        //ユーザ検索しユーザを返す
        when(operatingUserRepository.findByEmailOrUsername("alice", "alice"))
                .thenReturn(Optional.of(operator));
        

        // パスワード一致
        when(passwordEncoder.matches("raw-pass", "$2a$...hash...")).thenReturn(true);
        

        // トークン発行
        when(jwtUtil.generateToken(1, "alice", "123", "USER", List.of())).thenReturn("jwt-token-123");

        // Act
        AdminLoginResponse res = authServiceImpl.operatorLogin(req);

        // Assert
        assertEquals(ResponseConstants.OK, res.getStatusCode());
        assertEquals("jwt-token-123", res.getToken());
        assertNotNull(res.getAdmin());
        assertEquals("alice", res.getAdmin().getUsername());
        assertEquals("Smith Alice", res.getAdmin().getName()); // 実装：last + space + first
        assertEquals("USER", res.getAdmin().getRole());
        assertEquals("alice@example.com", res.getAdmin().getEmail());
        
        
        // ユーザーの試行回数が0にリセットされ保存されていること
        ArgumentCaptor<OperatingUser> userCaptor = ArgumentCaptor.forClass(OperatingUser.class);
        verify(operatingUserRepository).save(userCaptor.capture());
        OperatingUser saved = userCaptor.getValue();
        assertEquals(0, saved.getLoginAttempts());

        //結果をlogにだす
        System.out.println("authService_adminLogin_username_succes() succes");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("response: " + res.getStatusCode());
        System.out.println("token: " + res.getToken());
        System.out.println("admin: " + res.getAdmin());
        System.out.println("name: " + res.getAdmin().getUsername());
        System.out.println("lastName firstName: " + res.getAdmin().getName());
        System.out.println("user/master: " + res.getAdmin().getRole());
        System.out.println("Email: " + res.getAdmin().getEmail());
        System.out.println("loginAttemptsNum: " + saved.getLoginAttempts());
    }

    //正しいメールアドレス/パスワードでログイン成功
    @Test
        @DisplayName("UT-AS-202: 管理者正しいメールアドレス/パスワードでログイン成功")
    void authService_adminLogin_email_succes() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setIdentifier("alice@example.com"); // username でも email でもOK
        req.setPassword("raw-pass");
        
        // ユーザーの用意（ロックなし・有効）
        OperatingUser operator = new OperatingUser();
        operator.setId(1);
        operator.setUsername("alice");
        operator.setEmail("alice@example.com");
        OperatorCompany company = new OperatorCompany();
        company.setId(123);
        company.setCompanyId("123");
        company.setIsActive(true);
        operator.setCompany(company);
        operator.setRole("USER");
        operator.setFirstName("Alice");
        operator.setLastName("Smith");
        operator.setPassword("$2a$...hash..."); // ダミー
        operator.setAccountLockedUntil(null);
        operator.setLoginAttempts(2); // 成功時に0へリセットされることを検証
        operator.setIsActive(true);

        //ユーザ検索しユーザを返す
        when(operatingUserRepository.findByEmailOrUsername("alice@example.com", "alice@example.com"))
                .thenReturn(Optional.of(operator));
        

        // パスワード一致
        when(passwordEncoder.matches("raw-pass", "$2a$...hash...")).thenReturn(true);
        

        // トークン発行
        when(jwtUtil.generateToken(1,   "alice", "123", "USER", List.of())).thenReturn("jwt-token-123");

        // Act
        AdminLoginResponse res = authServiceImpl.operatorLogin(req);

        // Assert
        assertEquals(ResponseConstants.OK, res.getStatusCode());
        assertEquals("jwt-token-123", res.getToken());
        assertNotNull(res.getAdmin());
        assertEquals("alice", res.getAdmin().getUsername());
        assertEquals("Smith Alice", res.getAdmin().getName()); // 実装：last + space + first
        assertEquals("USER", res.getAdmin().getRole());
        assertEquals("alice@example.com", res.getAdmin().getEmail());
        
        
        // ユーザーの試行回数が0にリセットされ保存されていること
        ArgumentCaptor<OperatingUser> userCaptor = ArgumentCaptor.forClass(OperatingUser.class);
        verify(operatingUserRepository).save(userCaptor.capture());
        OperatingUser saved = userCaptor.getValue();
        assertEquals(0, saved.getLoginAttempts());

        //結果をlogにだす
        System.out.println("authService_adminLogin_email_succes() succes");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("response: " + res.getStatusCode());
        System.out.println("token: " + res.getToken());
        System.out.println("admin: " + res.getAdmin());
        System.out.println("name: " + res.getAdmin().getUsername());
        System.out.println("lastName firstName: " + res.getAdmin().getName());
        System.out.println("user/master: " + res.getAdmin().getRole());
        System.out.println("Email: " + res.getAdmin().getEmail());
        System.out.println("loginAttemptsNum: " + saved.getLoginAttempts());
    }

    //正しいユーザーネーム/誤ったパスワードでログイン失敗
    @Test
    @DisplayName("UT-AS-203: 管理者誤ったユーザーネーム/パスワードでログイン失敗")
    void authService_adminLogin_username_password_false() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setIdentifier("alice"); // username でも email でもOK
        req.setPassword("raw-pass");
        
        // ユーザーの用意（ロックなし・有効）
        OperatingUser operator = new OperatingUser();
        operator.setId(1);
        operator.setUsername("alice");
        operator.setEmail("alice@example.com");
        OperatorCompany company = new OperatorCompany();
        company.setId(123);
        company.setCompanyId("123");
        company.setIsActive(true);
        operator.setCompany(company);
        operator.setRole("USER");
        operator.setFirstName("Alice");
        operator.setLastName("Smith");
        operator.setPassword("$2a$...hash..."); // ダミー
        operator.setAccountLockedUntil(null);
        operator.setLoginAttempts(2);
        operator.setIsActive(true);

        //ユーザ検索しユーザを返す
        when(operatingUserRepository.findByEmailOrUsername("alice", "alice"))
                .thenReturn(Optional.of(operator));
        

        // パスワード一致
        when(passwordEncoder.matches("raw-pass", "$2a$...hash...")).thenReturn(false);


        // Act
        AdminLoginResponse res = authServiceImpl.operatorLogin(req);

        // Assert
        assertEquals(ResponseConstants.INVALID_CREDENTIALS, res.getStatusCode());
        assertEquals(null, res.getToken());
        assertEquals(null, res.getAdmin());
        
        
        // ユーザーの試行回数を3に増やして保存されていること
        ArgumentCaptor<OperatingUser> userCaptor = ArgumentCaptor.forClass(OperatingUser.class);
        verify(operatingUserRepository).save(userCaptor.capture());
        OperatingUser saved = userCaptor.getValue();
        assertEquals(3, saved.getLoginAttempts());

        //結果をlogにだす
        System.out.println("authService_adminLogin_username_password_false() succes");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("response: " + res.getStatusCode());
        System.out.println("token: " + res.getToken());
        System.out.println("admin: " + res.getAdmin());
        System.out.println("loginAttemptsNum: " + saved.getLoginAttempts());
    }

        //誤ったユーザーネーム/正しいパスワードでログイン成功
    @Test
        @DisplayName("UT-AS-204: 管理者誤ったユーザーネーム/正しいパスワードでログイン失敗")
    void authService_adminLogin_username_username_false() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setIdentifier("alice"); // username でも email でもOK
        req.setPassword("raw-pass");
        

        //ユーザ検索しユーザを返す
        when(operatingUserRepository.findByEmailOrUsername("alice", "alice"))
                .thenReturn(Optional.empty());
        

        // Act
        AdminLoginResponse res = authServiceImpl.operatorLogin(req);

        // Assert
        assertEquals(ResponseConstants.INVALID_CREDENTIALS, res.getStatusCode());
        assertEquals(null, res.getToken());
        assertEquals(null, res.getAdmin());

        //結果をlogにだす
        System.out.println("authService_adminLogin_username_username_false() succes");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("response: " + res.getStatusCode());
        System.out.println("token: " + res.getToken());
        System.out.println("admin: " + res.getAdmin());
        
    }

    //4回ログイン失敗しても、
   // アカウントロックされずに、ログインができることを確認
    @Test
    @DisplayName("UT-AS-205: 管理者4回ログイン失敗してもアカウントロックされないこと")
    void authService_adminLogin_4times_not_account_lock() throws Exception {

        // Arrange
        AdminLoginRequest req = new AdminLoginRequest();
        req.setIdentifier("alice");
        req.setPassword("raw-pass");

        OperatorCompany company = new OperatorCompany();
        company.setId(123);
        company.setCompanyId("123");
        company.setIsActive(true);

        OperatingUser operator = new OperatingUser();
        operator.setId(1);
        operator.setUsername("alice");
        operator.setEmail("alice@example.com");
        operator.setCompany(company);
        operator.setRole("USER");
        operator.setFirstName("Alice");
        operator.setLastName("Smith");
        operator.setPassword("$2a$...hash...");
        operator.setAccountLockedUntil(null);
        operator.setLoginAttempts(4);
        operator.setIsActive(true);

        when(
            operatingUserRepository.findByEmailOrUsername(
                "alice",
                "alice"
            )
        ).thenReturn(Optional.of(operator));

        when(
            passwordEncoder.matches(
                "raw-pass",
                "$2a$...hash..."
            )
        ).thenReturn(true);

        when(
            jwtUtil.generateToken(
                1,
                "alice",
                "123",
                "USER",
                List.of()
            )
        ).thenReturn("jwt-token-123");

        // Act
        AdminLoginResponse res = authServiceImpl.operatorLogin(req);

        // Assert
        assertEquals(ResponseConstants.OK, res.getStatusCode());
        assertEquals("jwt-token-123", res.getToken());

        assertNotNull(res.getAdmin());
        assertEquals("alice", res.getAdmin().getUsername());
        assertEquals("Smith Alice", res.getAdmin().getName());
        assertEquals("USER", res.getAdmin().getRole());
        assertEquals("alice@example.com", res.getAdmin().getEmail());

        ArgumentCaptor<OperatingUser> userCaptor =
                ArgumentCaptor.forClass(OperatingUser.class);

        verify(operatingUserRepository).save(userCaptor.capture());

        OperatingUser saved = userCaptor.getValue();

        assertEquals(0, saved.getLoginAttempts());
    }

    //5回ログイン失敗したらアカウントロックされる
    @Test
        @DisplayName("UT-AS-206: 管理者5回ログイン失敗したらアカウントロックされること")
    void authService_adminLogin_5times_account_lock() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setIdentifier("alice@example.com"); // username でも email でもOK
        req.setPassword("raw-pass");
        
        // ユーザーの用意（ロックなし・有効）
        OperatingUser operator = new OperatingUser();
        operator.setUsername("alice");
        operator.setEmail("alice@example.com");
        OperatorCompany company = new OperatorCompany();
        company.setId(123);
        company.setCompanyId("123");
        company.setIsActive(true);
        operator.setCompany(company);
        operator.setRole("USER");
        operator.setFirstName("Alice");
        operator.setLastName("Smith");
        operator.setPassword("$2a$...hash..."); // ダミー
        operator.setAccountLockedUntil(null);
        operator.setLoginAttempts(5); //5でロックされることを確認
        operator.setIsActive(true);

        //ユーザ検索しユーザを返す
        when(operatingUserRepository.findByEmailOrUsername("alice@example.com", "alice@example.com"))
                .thenReturn(Optional.of(operator));
        

        // パスワード一致
        when(passwordEncoder.matches("raw-pass", "$2a$...hash...")).thenReturn(false);
        

        // 実行直前の時刻
        LocalDateTime before = LocalDateTime.now();

        // Act
        AdminLoginResponse res = authServiceImpl.operatorLogin(req);

        // 実行直後の時刻
        LocalDateTime after = LocalDateTime.now();

        // Assert
        assertEquals(ResponseConstants.ACCOUNT_LOCKED, res.getStatusCode());
        assertEquals(null, res.getToken());
        assertEquals(null, res.getAdmin());
        
        
        // ユーザーの試行回数を4に増やして保存されていること
        ArgumentCaptor<OperatingUser> userCaptor = ArgumentCaptor.forClass(OperatingUser.class);
        verify(operatingUserRepository).save(userCaptor.capture());
        OperatingUser saved = userCaptor.getValue();
        assertEquals(5, saved.getLoginAttempts());

        LocalDateTime lockedUntil = saved.getAccountLockedUntil();

        
        assertTrue(lockedUntil.isAfter(before.plusHours(12).minusSeconds(1)));
        assertTrue(lockedUntil.isBefore(after.plusHours(12).plusSeconds(1)));


        //結果をlogにだす
        System.out.println("authService_adminLogin_5times_account_lock() succes");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("response: " + res.getStatusCode());
        System.out.println("token: " + res.getToken());
        System.out.println("admin: " + res.getAdmin());
        System.out.println("loginAttemptsNum: " + saved.getLoginAttempts());
        System.out.println("lokedUntil: " + lockedUntil);
    }

        //アカウントロックから12時間経ったらアカウントが解除されたのち、
        //ログイン可能になること
    @Test
        @DisplayName("UT-AS-207: 管理者5回ログイン失敗後、12時間経過するとログイン成功")
    void authService_adminLogin_afterLock12Hours_succes() throws Exception {

        AdminLoginRequest req = new AdminLoginRequest();
        req.setIdentifier("alice");
        req.setPassword("raw-pass");

        OperatorCompany company = new OperatorCompany();
        company.setId(123);
        company.setCompanyId("123");
        company.setIsActive(true);

        OperatingUser operator = new OperatingUser();
        operator.setId(1);
        operator.setUsername("alice");
        operator.setEmail("alice@example.com");
        operator.setCompany(company);
        operator.setRole("USER");
        operator.setFirstName("Alice");
        operator.setLastName("Smith");
        operator.setPassword("$2a$...hash...");
        operator.setAccountLockedUntil(LocalDateTime.now().minusMinutes(1));
        operator.setLoginAttempts(5);
        operator.setIsActive(true);

        when(
            operatingUserRepository.findByEmailOrUsername(
                "alice",
                "alice"
            )
        ).thenReturn(Optional.of(operator));

        when(
            passwordEncoder.matches(
                "raw-pass",
                "$2a$...hash..."
            )
        ).thenReturn(true);

        when(
            jwtUtil.generateToken(
                1,
                "alice",
                "123",
                "USER",
                List.of()
            )
        ).thenReturn("jwt-token-123");

        // Act
        AdminLoginResponse res = authServiceImpl.operatorLogin(req);

        // Assert
        assertEquals(ResponseConstants.OK, res.getStatusCode());
        assertEquals("jwt-token-123", res.getToken());

        assertNotNull(res.getAdmin());
        assertEquals("alice", res.getAdmin().getUsername());
        assertEquals("Smith Alice", res.getAdmin().getName());
        assertEquals("USER", res.getAdmin().getRole());
        assertEquals("alice@example.com", res.getAdmin().getEmail());

        ArgumentCaptor<OperatingUser> userCaptor =
                ArgumentCaptor.forClass(OperatingUser.class);

        verify(operatingUserRepository, atLeastOnce())
                .save(userCaptor.capture());

        OperatingUser saved = userCaptor.getValue();

        assertEquals(0, saved.getLoginAttempts());
        assertNull(saved.getAccountLockedUntil());
    }

    //ロックされている状態でログインできないこと
    @Test
        @DisplayName("UT-AS-208: 管理者ロックされている状態でログインできないこと")
    void authService_adminLogin_accountLocked_failure() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setIdentifier("alice"); // username でも email でもOK
        req.setPassword("raw-pass");
        
        // ユーザーの用意（ロックあり・有効）
        OperatingUser operator = new OperatingUser();
        operator.setUsername("alice");
        operator.setEmail("alice@example.com");
        OperatorCompany company = new OperatorCompany();
        company.setId(123);
        company.setCompanyId("123");
        company.setIsActive(true);
        operator.setCompany(company);
        operator.setRole("USER");
        operator.setFirstName("Alice");
        operator.setLastName("Smith");
        operator.setPassword("$2a$...hash..."); // ダミー
        operator.setAccountLockedUntil(LocalDateTime.now().plusMinutes(10));
        operator.setLoginAttempts(5);
        operator.setIsActive(true);

        //ユーザ検索しユーザを返す
        when(operatingUserRepository.findByEmailOrUsername("alice", "alice"))
                .thenReturn(Optional.of(operator));
        
        // Act
        AdminLoginResponse res = authServiceImpl.operatorLogin(req);

        // Assert
        assertEquals(ResponseConstants.ACCOUNT_LOCKED, res.getStatusCode());
        assertEquals(null, res.getToken());
        assertEquals(null, res.getAdmin());

        //結果をlogにだす
        System.out.println("authService_adminLogin_accountLocked_failure() succes");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("response: " + res.getStatusCode());
        System.out.println("token: " + res.getToken());
        System.out.println("admin: " + res.getAdmin());
    }

    @Test
        @DisplayName("UT-AS-209: 管理者ロック中は正しいパスワードでもパスワード照合が行われないこと（ガード確認）")
void authService_adminLogin_locked_passwordMatchNeverCalled() throws Exception {
    AdminLoginRequest req = new AdminLoginRequest();
    req.setIdentifier("operatingUser1");
    req.setPassword("raw-pass");

    operatingUser.setLoginAttempts(5);
    operatingUser.setAccountLockedUntil(LocalDateTime.now().plusHours(2));
    when(operatingUserRepository.findByEmailOrUsername("operatingUser1", "operatingUser1"))
            .thenReturn(Optional.of(operatingUser));
    // Act
    AdminLoginResponse res = authServiceImpl.operatorLogin(req);

    // Assert
    assertEquals(ResponseConstants.ACCOUNT_LOCKED, res.getStatusCode());
    assertNull(res.getToken());

    verify(passwordEncoder, never()).matches(any(), any());
    verify(jwtUtil, never()).generateToken(any(), any(), any(), any(), any());

    System.out.println("authService_adminLogin_locked_passwordMatchNeverCalled() success");
    System.out.println("response: " + res.getStatusCode());
}

        @Test
        @DisplayName("UT-AS-210: 管理者ロック解除後の再ログイン成功時、失敗回数カウンタが0にリセットされること")
        void authService_adminLogin_afterUnlock_attemptsResetToZero() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setIdentifier("operatingUser1");
        req.setPassword("raw-pass");

        operatingUser.setLoginAttempts(5);
        operatingUser.setAccountLockedUntil(LocalDateTime.now().minusMinutes(1));
        when(operatingUserRepository.findByEmailOrUsername("operatingUser1", "operatingUser1"))
                .thenReturn(Optional.of(operatingUser));
        when(passwordEncoder.matches("raw-pass", operatingUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(operatingUser.getId(), "operatingUser1", "123", "admin", List.of()))
                .thenReturn("jwt-token-admin-reset");
        when(operatingUserRepository.save(any(OperatingUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AdminLoginResponse res = authServiceImpl.operatorLogin(req);

        // Assert
        assertEquals(ResponseConstants.OK, res.getStatusCode());
        assertEquals("jwt-token-admin-reset", res.getToken());

        ArgumentCaptor<OperatingUser> userCaptor = ArgumentCaptor.forClass(OperatingUser.class);
        verify(operatingUserRepository, atLeastOnce()).save(userCaptor.capture());
        OperatingUser saved = userCaptor.getValue();
        assertEquals(0, saved.getLoginAttempts());
        assertNull(saved.getAccountLockedUntil());

        System.out.println("authService_adminLogin_afterUnlock_attemptsResetToZero() success");
        System.out.println("loginAttemptsNum: " + saved.getLoginAttempts());
        }

        @Test
        @DisplayName("UT-AS-211: 管理者ログイン成功時、JWTトークンが正しい引数で生成されること")
        void authService_adminLogin_jwtGeneratedWithCorrectArgs() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setIdentifier("operatingUser1");
        req.setPassword("raw-pass");

        when(operatingUserRepository.findByEmailOrUsername("operatingUser1", "operatingUser1"))
                .thenReturn(Optional.of(operatingUser));
        when(passwordEncoder.matches("raw-pass", operatingUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(operatingUser.getId(), "operatingUser1", "123", "admin", List.of()))
                .thenReturn("jwt-token-admin-verify");
        when(operatingUserRepository.save(any(OperatingUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AdminLoginResponse res = authServiceImpl.operatorLogin(req);

        // Assert
        assertEquals(ResponseConstants.OK, res.getStatusCode());

        verify(jwtUtil, times(1)).generateToken(
                operatingUser.getId(), "operatingUser1", "123", "admin", List.of());

        System.out.println("authService_adminLogin_jwtGeneratedWithCorrectArgs() success");
        System.out.println("token: " + res.getToken());
        }

        @Test
        @DisplayName("UT-AS-212: 運営会社が無効化（非アクティブ）状態の場合、ログイン失敗すること")
        void authService_adminLogin_companyInactive_loginFails() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setIdentifier("operatingUser1");
        req.setPassword("raw-pass");

        // 運営会社を非アクティブに設定
        operatorCompany.setIsActive(false);
        operatingUser.setCompany(operatorCompany);

        when(operatingUserRepository.findByEmailOrUsername("operatingUser1", "operatingUser1"))
                .thenReturn(Optional.of(operatingUser));

        // Act
        AdminLoginResponse res = authServiceImpl.operatorLogin(req);

        // Assert
        assertEquals(ResponseConstants.FORBIDDEN, res.getStatusCode());
        assertNull(res.getToken());

        verify(passwordEncoder, never()).matches(any(), any());

        System.out.println("authService_adminLogin_companyInactive_loginFails() success");
        System.out.println("response: " + res.getStatusCode());
        }

    //一般ユーザが自分自身のパスワード更新に成功
    @Test
        @DisplayName("UT-AS-301: 一般ユーザが自分自身のパスワード更新に成功")
        void authService_changePassword_GeneralUserSelfChange_success() {
        String encodedNewPassword = "$2a$...newhash...";
        String testToken = "test-jwt-token";

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(null);
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        String username = "user1";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(participatingUser));
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.empty()); // FIX: findByUsername, bukan findByEmailOrUsername

        when(passwordEncoder.matches(request.getCurrentPassword(), participatingUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(request.getNewPassword(), participatingUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn(encodedNewPassword);
        when(userRepository.save(any(ParticipatingUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertEquals("パスワードが正常に変更されました。", response.getMessage());

        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertEquals(encodedNewPassword, userCaptor.getValue().getPassword());

        verify(userRepository, times(1)).findByUsername(username);
        verify(operatingUserRepository, times(1)).findByUsername(username); // FIX: cocok dengan stub di atas
        verify(passwordEncoder, times(1)).matches("old-password", "$2a$...oldhash...");
        verify(passwordEncoder, times(1)).matches("new-password", "$2a$...oldhash...");
        verify(passwordEncoder, times(1)).encode("new-password");
        verify(blacklistService, times(1)).recordPasswordChange(username);
        }

    // 1. 管理ユーザが自分自身のパスワード更新に成功
    @Test
        @DisplayName("UT-AS-302: 管理ユーザが自分自身のパスワード更新に成功")
    void authService_changePassword_OperatingUserSelfChange_success() {
        // Arrange
        String encodedNewPassword = "$2a$...newhash...";
        String testToken = "test-jwt-token";
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(1);

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(null);
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        String username = "operatingUser1";

        // Mockの設定
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());//★
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.of(operatingUser));//★
        
        when(passwordEncoder.matches(request.getCurrentPassword(), operatingUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(request.getNewPassword(), operatingUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn(encodedNewPassword);
        when(operatingUserRepository.save(any(OperatingUser.class))).thenAnswer(invocation -> invocation.getArgument(0));


        // Act
        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        // Assert
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertEquals("パスワードが正常に変更されました。", response.getMessage());

        ArgumentCaptor<OperatingUser> userCaptor = ArgumentCaptor.forClass(OperatingUser.class);
        verify(operatingUserRepository, times(1)).save(userCaptor.capture());
        OperatingUser savedUser = userCaptor.getValue();
        assertEquals(encodedNewPassword, savedUser.getPassword());

        // 各メソッドが呼ばれた回数を検証
        verify(userRepository, times(1)).findByUsername(username);
        verify(operatingUserRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).matches("old-password", "$2a$...oldhash...");
        verify(passwordEncoder, times(1)).matches("new-password", "$2a$...oldhash...");
        verify(passwordEncoder, times(1)).encode("new-password");
        
        // コンソール出力
        System.out.println("authService_changePassword_OperatingUserSelfChange_success() success");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("Response Status Code: " + response.getStatusCode());
        System.out.println("Response Message: " + response.getMessage());
        System.out.println("Saved User New Password Hash: " + savedUser.getPassword());
    }

    // 2. マスターユーザが同一会社のマスターユーザのパスワード更新に成功
    @Test
        @DisplayName("UT-AS-303: マスターユーザーが同一会社の別マスターユーザーのパスワード更新に成功")
        void authService_changePassword_MasterChangesAnotherMasterSameCompany_success() {
        String encodedNewPassword = "$2a$...newhash...";
        String testToken = "test-jwt-token";

        ParticipatingCompany company = new ParticipatingCompany();
        company.setId(123);
        company.setCompanyId("123");

        ParticipatingUser masterUser = new ParticipatingUser();
        masterUser.setId(1);
        masterUser.setUsername("master1");
        masterUser.setPassword("$2a$...masterhash...");
        masterUser.setRole("master");
        masterUser.setCompany(company);
        masterUser.setIsActive(true);

        ParticipatingUser targetMasterUser = new ParticipatingUser();
        targetMasterUser.setId(2);
        targetMasterUser.setUsername("master2");
        targetMasterUser.setPassword("$2a$...oldhash...");
        targetMasterUser.setRole("master");
        targetMasterUser.setCompany(company);
        targetMasterUser.setIsActive(true);

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(2);
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        String username = "master1";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.findById(2)).thenReturn(Optional.of(targetMasterUser));
        when(passwordEncoder.matches("old-password", "$2a$...oldhash...")).thenReturn(true);
        when(passwordEncoder.matches("new-password", "$2a$...oldhash...")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn(encodedNewPassword);
        when(userRepository.save(any(ParticipatingUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        assertEquals(ResponseConstants.OK, response.getStatusCode());

        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(encodedNewPassword, userCaptor.getValue().getPassword());

        verify(jwtUtil, never()).extractExpirationAsLocalDateTime(anyString());
        verify(blacklistService, never()).blacklistToken(anyString(), any(LocalDateTime.class));

        verify(blacklistService, times(1)).recordPasswordChange("master2");
        }

    // 3. マスターユーザが同一会社の一般ユーザのパスワード更新に成功
        @Test
        @DisplayName("UT-AS-304: マスターユーザーが同一会社の一般ユーザーのパスワード更新に成功")
        void authService_changePassword_MasterChangesGeneralUserSameCompany_success() {

        String encodedNewPassword = "$2a$...newhash...";
        String testToken = "test-jwt-token";

        ParticipatingCompany company = new ParticipatingCompany();
        company.setId(123);
        company.setCompanyId("123");

        ParticipatingUser masterUser = new ParticipatingUser();
        masterUser.setId(1);
        masterUser.setUsername("master1");
        masterUser.setPassword("$2a$...masterhash...");
        masterUser.setRole("master");
        masterUser.setCompany(company);
        masterUser.setIsActive(true);

        ParticipatingUser targetGeneralUser = new ParticipatingUser();
        targetGeneralUser.setId(3);
        targetGeneralUser.setUsername("user2");
        targetGeneralUser.setPassword("$2a$...oldhash...");
        targetGeneralUser.setRole("user");
        targetGeneralUser.setCompany(company);
        targetGeneralUser.setIsActive(true);

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(3);
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        String username = "master1";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.findById(3)).thenReturn(Optional.of(targetGeneralUser));
        when(passwordEncoder.matches("old-password", "$2a$...oldhash...")).thenReturn(true);
        when(passwordEncoder.matches("new-password", "$2a$...oldhash...")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn(encodedNewPassword);
        when(userRepository.save(any(ParticipatingUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        assertEquals(ResponseConstants.OK, response.getStatusCode());

        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(encodedNewPassword, userCaptor.getValue().getPassword());

        verify(jwtUtil, never()).extractExpirationAsLocalDateTime(anyString());
        verify(blacklistService, never()).blacklistToken(anyString(), any(LocalDateTime.class));

        // FIX: token milik TARGET (user2) yang harus di-invalidasi, bukan aktor (master1)
        verify(blacklistService, times(1)).recordPasswordChange("user2");
        }

    // 4. 管理ユーザがマスターユーザのパスワード更新に成功
    @Test
        @DisplayName("UT-AS-305: 管理ユーザーがマスターユーザーのパスワード更新に成功")
        void authService_changePassword_OperatingUserChangesMasterUser_success() {
        String encodedNewPassword = "$2a$...newhash...";
        String testToken = "test-jwt-token";

        ParticipatingUser targetMasterUser = new ParticipatingUser();
        targetMasterUser.setId(2);
        targetMasterUser.setUsername("master1");
        targetMasterUser.setPassword("$2a$...oldhash...");
        targetMasterUser.setRole("master");
        ParticipatingCompany targetMasterCompany = new ParticipatingCompany();
        targetMasterCompany.setId(123);
        targetMasterCompany.setCompanyId("123");
        targetMasterUser.setCompany(targetMasterCompany);
        targetMasterUser.setIsActive(true);

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(2);
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        String username = "operatingUser1";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.of(operatingUser));
        when(userRepository.findById(2)).thenReturn(Optional.of(targetMasterUser));

        when(passwordEncoder.matches(request.getCurrentPassword(), targetMasterUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(request.getNewPassword(), targetMasterUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn(encodedNewPassword);
        when(userRepository.save(any(ParticipatingUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertEquals("パスワードが正常に変更されました。", response.getMessage());

        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertEquals(encodedNewPassword, userCaptor.getValue().getPassword());

        verify(userRepository, times(1)).findByUsername(username);
        verify(operatingUserRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).findById(2);
        verify(passwordEncoder, times(1)).matches("old-password", "$2a$...oldhash...");
        verify(passwordEncoder, times(1)).matches("new-password", "$2a$...oldhash...");
        verify(passwordEncoder, times(1)).encode("new-password");

        verify(jwtUtil, never()).extractExpirationAsLocalDateTime(anyString());
        verify(blacklistService, never()).blacklistToken(anyString(), any(LocalDateTime.class));

        verify(blacklistService, times(1)).recordPasswordChange("master1");
        }

    // 5. ユーザーネームがどちらにも存在しない → NOT_FOUND
    @Test
    @DisplayName("UT-AS-306: ユーザーネームがどちらにも存在しない場合、NOT_FOUNDを返す")
    void authService_changePassword_UsernameNotFound_returnsNotFound() {
        // Arrange
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        String username = "nonexistentUser";
        String testToken = "test-jwt-token";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act
        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        // Assert
        assertEquals(ResponseConstants.NOT_FOUND, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_NOT_FOUND, response.getMessage());

        // 各メソッドが呼ばれた回数を検証
        verify(userRepository, times(1)).findByUsername(username);
        verify(operatingUserRepository, times(1)).findByUsername(username);

        // コンソール出力
        System.out.println("authService_changePassword_UsernameNotFound_returnsNotFound() success");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("Response Status Code: " + response.getStatusCode());
        System.out.println("Response Message: " + response.getMessage());
    }

    // 6. 現在のパスワード不一致（Participating self）→ INVALID_CURRENT_PASSWORD
    @Test
    @DisplayName("UT-AS-307: 現在のパスワード不一致（Participating self）→ INVALID_CURRENT_PASSWORD")
    void authService_changePassword_ParticipatingSelfWrongCurrentPassword_returnsInvalidCurrentPassword() {
        // Arrange
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(null);
        request.setCurrentPassword("wrong-password");
        request.setNewPassword("new-password");

        String username = "user1";
        String testToken = "test-jwt-token";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(participatingUser));
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.matches(request.getCurrentPassword(), participatingUser.getPassword())).thenReturn(false);

        // Act
        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        // Assert
        assertEquals(ResponseConstants.INVALID_CURRENT_PASSWORD, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_INVALID_CURRENT_PASSWORD, response.getMessage());

        // 各メソッドが呼ばれた回数を検証
        verify(userRepository, times(1)).findByUsername(username);
        verify(operatingUserRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).matches("wrong-password", "$2a$...oldhash...");

        // コンソール出力
        System.out.println("authService_changePassword_ParticipatingSelfWrongCurrentPassword_returnsInvalidCurrentPassword() success");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("Response Status Code: " + response.getStatusCode());
        System.out.println("Response Message: " + response.getMessage());
    }

    // 7. 新しいパスワードが旧パスワードと同一（Participating self）→ SAME_PASSWORD
    @Test
        @DisplayName("UT-AS-308: 新しいパスワードが旧パスワードと同一（Participating self）→ SAME_PASSWORD")
    void authService_changePassword_ParticipatingSelfSamePassword_returnsSamePassword() {
        // Arrange
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(null);
        request.setCurrentPassword("old-password");
        request.setNewPassword("old-password");

        String username = "user1";
        String testToken = "test-jwt-token";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(participatingUser));
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.matches(request.getCurrentPassword(), participatingUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(request.getNewPassword(), participatingUser.getPassword())).thenReturn(true);

        // Act
        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        // Assert
        assertEquals(ResponseConstants.SAME_PASSWORD, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_SAME_PASSWORD, response.getMessage());

        // 各メソッドが呼ばれた回数を検証
        verify(userRepository, times(1)).findByUsername(username);
        verify(operatingUserRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(2)).matches("old-password", "$2a$...oldhash...");

        // コンソール出力
        System.out.println("authService_changePassword_ParticipatingSelfSamePassword_returnsSamePassword() success");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("Response Status Code: " + response.getStatusCode());
        System.out.println("Response Message: " + response.getMessage());
    }

    // 8. 現在のパスワード不一致（Operating self）→ INVALID_CURRENT_PASSWORD
    @Test
        @DisplayName("UT-AS-309: 現在のパスワード不一致（Operating self）→ INVALID_CURRENT_PASSWORD")
    void authService_changePassword_OperatingSelfWrongCurrentPassword_returnsInvalidCurrentPassword() {
        // Arrange
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(null);
        request.setCurrentPassword("wrong-password");
        request.setNewPassword("new-password");

        String username = "operatingUser1";
        String testToken = "test-jwt-token";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.of(operatingUser));
        when(passwordEncoder.matches(request.getCurrentPassword(), operatingUser.getPassword())).thenReturn(false);

        // Act
        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        // Assert
        assertEquals(ResponseConstants.INVALID_CURRENT_PASSWORD, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_INVALID_CURRENT_PASSWORD, response.getMessage());

        // 各メソッドが呼ばれた回数を検証
        verify(userRepository, times(1)).findByUsername(username);
        verify(operatingUserRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).matches("wrong-password", "$2a$...oldhash...");

        // コンソール出力
        System.out.println("authService_changePassword_OperatingSelfWrongCurrentPassword_returnsInvalidCurrentPassword() success");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("Response Status Code: " + response.getStatusCode());
        System.out.println("Response Message: " + response.getMessage());
    }

    // 9. 新しいパスワードが旧パスワードと同一（Operating self）→ SAME_PASSWORD
    @Test
        @DisplayName("UT-AS-310: 新しいパスワードが旧パスワードと同一（Operating self）→ SAME_PASSWORD")
    void authService_changePassword_OperatingSelfSamePassword_returnsSamePassword() {
        // Arrange
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(null);
        request.setCurrentPassword("old-password");
        request.setNewPassword("old-password");

        String username = "operatingUser1";
        String testToken = "test-jwt-token";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.of(operatingUser));
        when(passwordEncoder.matches(request.getCurrentPassword(), operatingUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(request.getNewPassword(), operatingUser.getPassword())).thenReturn(true);

        // Act
        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        // Assert
        assertEquals(ResponseConstants.SAME_PASSWORD, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_SAME_PASSWORD, response.getMessage());

        // 各メソッドが呼ばれた回数を検証
        verify(userRepository, times(1)).findByUsername(username);
        verify(operatingUserRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(2)).matches("old-password", "$2a$...oldhash...");

        // コンソール出力
        System.out.println("authService_changePassword_OperatingSelfSamePassword_returnsSamePassword() success");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("Response Status Code: " + response.getStatusCode());
        System.out.println("Response Message: " + response.getMessage());
    }

    // 10. 一般ユーザが他人のパスワード更新要求 → FORBIDDEN
    @Test
        @DisplayName("UT-AS-311: 一般ユーザが他人のパスワード更新要求 → FORBIDDEN")
        void authService_changePassword_GeneralUserChangesOtherUser_returnsForbidden() {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(2);
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        String username = "user1";
        String testToken = "test-jwt-token";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(participatingUser));
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.empty());

        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_FORBIDDEN, response.getMessage());

        verify(userRepository, times(1)).findByUsername(username);
        verify(operatingUserRepository, times(1)).findByUsername(username);

        // FIX: buktikan request benar-benar diblok sebelum diproses lebih lanjut
        verify(passwordEncoder, never()).matches(any(), any());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("UT-AS-312: マスターユーザが他社のユーザのパスワード更新要求 → FORBIDDEN")
        void authService_changePassword_MasterChangesDifferentCompanyUser_returnsForbidden() {
        ParticipatingCompany masterCompany = new ParticipatingCompany();
        masterCompany.setId(123);
        masterCompany.setCompanyId("123");

        ParticipatingCompany targetCompany = new ParticipatingCompany();
        targetCompany.setId(456);
        targetCompany.setCompanyId("456");

        ParticipatingUser masterUser = new ParticipatingUser();
        masterUser.setId(1);
        masterUser.setUsername("master1");
        masterUser.setPassword("$2a$...masterhash...");
        masterUser.setRole("master");
        masterUser.setCompany(masterCompany);
        masterUser.setIsActive(true);

        ParticipatingUser targetUser = new ParticipatingUser();
        targetUser.setId(2);
        targetUser.setUsername("user2");
        targetUser.setPassword("$2a$...oldhash...");
        targetUser.setRole("user");
        targetUser.setCompany(targetCompany);
        targetUser.setIsActive(true);

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(2);
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        String username = "master1";
        String testToken = "test-jwt-token";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.findById(2)).thenReturn(Optional.of(targetUser));

        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_FORBIDDEN, response.getMessage());

        verify(userRepository, times(1)).findByUsername(username);
        verify(operatingUserRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).findById(2);
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());

        // FIX: buktikan guard company-boundary jalan SEBELUM password check, bukan cuma "encode tidak jalan"
        verify(passwordEncoder, never()).matches(any(), any());

        verify(jwtUtil, never()).extractExpirationAsLocalDateTime(anyString());
        verify(blacklistService, never()).blacklistToken(anyString(), any(LocalDateTime.class));
        }

    // 12. マスターユーザが存在しない対象ユーザ指定 → NOT_FOUND
    @Test
    @DisplayName("UT-AS-313: マスターユーザが存在しない対象ユーザ指定 → NOT_FOUND")
    void authService_changePassword_MasterChangesNonexistentUser_returnsNotFound() {
        // Arrange
        ParticipatingUser masterUser = new ParticipatingUser();
        masterUser.setId(1);
        masterUser.setUsername("master1");
        masterUser.setPassword("$2a$...masterhash...");
        masterUser.setRole("master");
                ParticipatingCompany targetMasterUserId = new ParticipatingCompany();
        targetMasterUserId.setId(123);
        targetMasterUserId.setCompanyId("123");
        masterUser.setIsActive(true);

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(999);
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        String username = "master1";
        String testToken = "test-jwt-token";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // Act
        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        // Assert
        assertEquals(ResponseConstants.NOT_FOUND, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_NOT_FOUND, response.getMessage());

        // 各メソッドが呼ばれた回数を検証
        verify(userRepository, times(1)).findByUsername(username);
        verify(operatingUserRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).findById(999);

        // コンソール出力
        System.out.println("authService_changePassword_MasterChangesNonexistentUser_returnsNotFound() success");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("Response Status Code: " + response.getStatusCode());
        System.out.println("Response Message: " + response.getMessage());
    }

    // 13. 管理ユーザが対象ユーザ指定するが存在しない → NOT_FOUND
    @Test
        @DisplayName("UT-AS-314: 管理ユーザが対象ユーザ指定するが存在しない → NOT_FOUND")
    void authService_changePassword_OperatingUserChangesNonexistentUser_returnsNotFound() {
        // Arrange
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(999);
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        String username = "operatingUser1";
        String testToken = "test-jwt-token";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.of(operatingUser));
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // Act
        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        // Assert
        assertEquals(ResponseConstants.NOT_FOUND, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_NOT_FOUND, response.getMessage());

        // 各メソッドが呼ばれた回数を検証
        verify(userRepository, times(1)).findByUsername(username);
        verify(operatingUserRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).findById(999);

        // コンソール出力
        System.out.println("authService_changePassword_OperatingUserChangesNonexistentUser_returnsNotFound() success");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("Response Status Code: " + response.getStatusCode());
        System.out.println("Response Message: " + response.getMessage());
    }

    // 14. 管理ユーザがマスター以外のユーザを対象にパスワード更新要求 → FORBIDDEN
    @Test
    @DisplayName("UT-AS-315: 管理ユーザがマスター以外のユーザを対象にパスワード更新要求 → FORBIDDEN")
    void authService_changePassword_OperatingUserChangesNonMasterUser_returnsForbidden() {
        // Arrange
        ParticipatingUser generalUser = new ParticipatingUser();
        generalUser.setId(2);
        generalUser.setUsername("user2");
        generalUser.setPassword("$2a$...oldhash...");
        generalUser.setRole("user");
        ParticipatingCompany targetGeneralUserId = new ParticipatingCompany();
        targetGeneralUserId.setId(123);
        targetGeneralUserId.setCompanyId("123");
        generalUser.setIsActive(true);

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(2);
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        String username = "operatingUser1";
        String testToken = "test-jwt-token";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.of(operatingUser));
        when(userRepository.findById(2)).thenReturn(Optional.of(generalUser));

        // Act
        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        // Assert
        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_FORBIDDEN, response.getMessage());

        // 各メソッドが呼ばれた回数を検証
        verify(userRepository, times(1)).findByUsername(username);
        verify(operatingUserRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).findById(2);

        // コンソール出力
        System.out.println("authService_changePassword_OperatingUserChangesNonMasterUser_returnsForbidden() success");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("Response Status Code: " + response.getStatusCode());
        System.out.println("Response Message: " + response.getMessage());
    }

    @Test
        @DisplayName("UT-AS-316: マスターユーザが自分自身のパスワード更新に成功")
        void authService_changePassword_MasterSelfChange_success() {
        String testToken = "test-jwt-token";
        String masterUsername = "masterUser1";
        String encodedNewPassword = "$2a$...newMasterSelfHash...";

        ParticipatingUser masterUser = new ParticipatingUser();
        masterUser.setId(2);
        masterUser.setUsername(masterUsername);
        masterUser.setRole("master");
        masterUser.setCompany(participatingUser.getCompany());
        masterUser.setPassword("$2a$...masterOldHash...");

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(null); // 自分自身
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        when(userRepository.findByUsername(masterUsername)).thenReturn(Optional.of(masterUser));
        when(operatingUserRepository.findByUsername(masterUsername)).thenReturn(Optional.empty());

        when(passwordEncoder.matches(request.getCurrentPassword(), masterUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(request.getNewPassword(), masterUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn(encodedNewPassword);
        when(userRepository.save(any(ParticipatingUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PasswordChangeResponse response = authServiceImpl.changePassword(masterUsername, testToken, request);

        // Assert
        assertEquals(ResponseConstants.OK, response.getStatusCode());

        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertEquals(encodedNewPassword, userCaptor.getValue().getPassword());

        verify(blacklistService, times(1)).recordPasswordChange(masterUsername);

        System.out.println("authService_changePassword_MasterSelfChange_success() success");
        System.out.println("Response Status Code: " + response.getStatusCode());
        }

        @Test
        @DisplayName("UT-AS-317: 一般ユーザが同一会社の他の一般ユーザのパスワード更新要求 → FORBIDDEN")
        void authService_changePassword_generalUpdatesOtherGeneralSameCompany_forbidden() {
        String testToken = "test-jwt-token";
        String username = "user1";

        ParticipatingUser otherGeneralUser = new ParticipatingUser();
        otherGeneralUser.setId(15);
        otherGeneralUser.setUsername("otherGeneralUser");
        otherGeneralUser.setRole("user");
        otherGeneralUser.setCompany(participatingUser.getCompany());

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(otherGeneralUser.getId()); // FIX: int, not String.valueOf(...)
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(participatingUser));
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.empty());

        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
        verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("UT-AS-318: 一般ユーザがマスターユーザのパスワード更新要求 → FORBIDDEN")
        void authService_changePassword_generalUpdatesMaster_forbidden() {
        String testToken = "test-jwt-token";
        String username = "user1";

        ParticipatingUser masterUser = new ParticipatingUser();
        masterUser.setId(2);
        masterUser.setUsername("masterUser1");
        masterUser.setRole("master");
        masterUser.setCompany(participatingUser.getCompany());

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(masterUser.getId()); // FIX: int, not String.valueOf(...)
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(participatingUser));
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.empty());

        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
        verify(userRepository, never()).save(any());
        }

       @Test
        @DisplayName("UT-AS-319: 管理ユーザが他社の参加企業ユーザのパスワード更新要求 → FORBIDDEN")
        void authService_changePassword_adminUpdatesOtherCompanyParticipatingUser_forbidden() {
        String testToken = "test-jwt-token";
        String adminUsername = "operatingUser1";

        ParticipatingCompany otherCompany = new ParticipatingCompany();
        otherCompany.setId(888);
        otherCompany.setCompanyId("888");

        ParticipatingUser otherCompanyMaster = new ParticipatingUser();
        otherCompanyMaster.setId(40);
        otherCompanyMaster.setUsername("otherCompanyMaster");
        otherCompanyMaster.setPassword("$2a$...targethash..."); 
        otherCompanyMaster.setRole("master");
        otherCompanyMaster.setCompany(otherCompany);
        otherCompanyMaster.setIsActive(true);

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(otherCompanyMaster.getId());
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        when(userRepository.findByUsername(adminUsername)).thenReturn(Optional.empty());
        when(operatingUserRepository.findByUsername(adminUsername)).thenReturn(Optional.of(operatingUser));
        when(userRepository.findById(otherCompanyMaster.getId())).thenReturn(Optional.of(otherCompanyMaster));
        // kalau guard company-boundary jalan duluan, matches() harusnya tidak pernah dipanggil sama sekali

        PasswordChangeResponse response = authServiceImpl.changePassword(adminUsername, testToken, request);

        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_FORBIDDEN, response.getMessage());

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());

        verify(passwordEncoder, never()).matches(any(), any());
        }
        @Test
        @DisplayName("UT-AS-320: パスワード更新成功時、パスワード変更がblacklistに記録されること")
        void authService_changePassword_success_recordsPasswordChangeInBlacklist() {
        String testToken = "test-jwt-token";
        String username = "user1";
        String encodedNewPassword = "$2a$...newHashRecord...";

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(null);
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(participatingUser));
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.matches(request.getCurrentPassword(), participatingUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(request.getNewPassword(), participatingUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn(encodedNewPassword);
        when(userRepository.save(any(ParticipatingUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        // Assert
        assertEquals(ResponseConstants.OK, response.getStatusCode());

        // blacklistServiceが正しいusernameで1回だけ呼ばれること（トークン無効化のトリガー確認）
        verify(blacklistService, times(1)).recordPasswordChange(username);
        verifyNoMoreInteractions(blacklistService);

        System.out.println("authService_changePassword_success_recordsPasswordChangeInBlacklist() success");
        System.out.println("Response Status Code: " + response.getStatusCode());
        }

        // ==== getUserProfile ====

        @Test
        @DisplayName("UT-AS-401: プロフィール取得成功 - 会社情報・拠点情報あり")
        void getUserProfile_userWithCompanyAndSite_success() {
        ParticipatingCompany company = new ParticipatingCompany();
        company.setCompanyId("123");
        company.setNameJa("株式会社テスト");
        company.setNameFurigana("カブシキガイシャテスト");
        company.setNameEn("Test Co., Ltd.");
        company.setEmail("company@example.com");
        company.setPhoneNumber("096-000-0000");
        company.setFax("096-000-0001");
        company.setCorpNumber("1234567890123");
        company.setPrefecture("熊本県");
        company.setMunicipality("熊本市");
        company.setBlocknum("1-1-1");
        company.setBuildingName("テストビル");
        company.setCompanyStatus(CompanyStatus.SUSPENDED);

        Site site = new Site();
        site.setId(10);
        site.setSiteName("熊本拠点");

        ParticipatingUser user = new ParticipatingUser();
        user.setId(1);
        user.setUsername("alice");
        user.setFirstName("Alice");
        user.setFirstNameKana("アリス");
        user.setLastName("Smith");
        user.setLastNameKana("スミス");
        user.setEmail("alice@example.com");
        user.setPhoneNumber("090-1111-2222");
        user.setUserPosition("Manager");
        user.setRole("user");
        user.setSite(site);
        user.setCompany(company);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserProfileResponse response = userServiceImpl.getUserProfile("alice");

        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertNotNull(response.getProfile());
        assertEquals("alice", response.getProfile().getUsername());
        assertEquals("Smith Alice", response.getProfile().getName());
        assertEquals("10", response.getProfile().getSiteId());
        assertEquals("熊本拠点", response.getProfile().getSiteName());
        assertEquals("123", response.getProfile().getCompanyId());
        assertNotNull(response.getProfile().getCompany());
        assertEquals("株式会社テスト", response.getProfile().getCompany().getNameJA());
        }

        @Test
        @DisplayName("UT-AS-402: プロフィール取得成功 - 拠点未割り当て（site=null）")
        void getUserProfile_userWithoutSite_success() {
        ParticipatingCompany company = new ParticipatingCompany();
        company.setCompanyId("123");
        company.setNameJa("株式会社テスト");

        ParticipatingUser user = new ParticipatingUser();
        user.setId(2);
        user.setUsername("bob");
        user.setFirstName("Bob");
        user.setLastName("Jones");
        user.setRole("user");
        user.setSite(null); // 拠点未割り当て
        user.setCompany(company);

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));

        UserProfileResponse response = userServiceImpl.getUserProfile("bob");

        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertNotNull(response.getProfile());
        assertNull(response.getProfile().getSiteId());
        assertNull(response.getProfile().getSiteName());
        assertEquals("123", response.getProfile().getCompanyId());
        }

        @Test
        @DisplayName("UT-AS-403: 会社情報がnullのユーザーでもプロフィール取得できること（company=nullをOKとして返す）")
        void getUserProfile_userWithNullCompany_returnsOkWithNullCompany() {
        // FIX/Pattern B: 本来 company が null でも正常にプロフィールが返るべき、という「あるべき挙動」をassertする。
        // 現在の実装は company の null チェックが companyInfo の組み立てにしか効いておらず、
        // profile 組み立て時に user.getCompany().getCompanyId() を直接呼んでいるため、
        // company が null の場合ここで NoSuchMethodError ではなく NullPointerException が発生し、
        // catch(Exception) に飲み込まれて INTERNAL_SERVER_ERROR が返ってしまう可能性が高い。
        // このテストは現状の実装に対しては失敗(レッド)することが想定される — それが正しい。
        ParticipatingUser user = new ParticipatingUser();
        user.setId(3);
        user.setUsername("carol");
        user.setFirstName("Carol");
        user.setLastName("White");
        user.setRole("user");
        user.setSite(null);
        user.setCompany(null); // 会社情報なし

        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(user));

        UserProfileResponse response = userServiceImpl.getUserProfile("carol");

        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertNotNull(response.getProfile());
        assertNull(response.getProfile().getCompany());
        assertNull(response.getProfile().getCompanyId());
        }

        @Test
        @DisplayName("UT-AS-404: 該当ユーザーが存在しない場合NOT_FOUND")
        void getUserProfile_userNotFound_returnsNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        UserProfileResponse response = userServiceImpl.getUserProfile("nonexistent");

        assertEquals(ResponseConstants.NOT_FOUND, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_USER_NOT_FOUND, response.getMessage());
        assertNull(response.getProfile());
        }

        @Test
        @DisplayName("UT-AS-405: リポジトリで予期しない例外が発生した場合INTERNAL_SERVER_ERROR")
        void getUserProfile_repositoryThrows_returnsInternalServerError() {
        when(userRepository.findByUsername("dave")).thenThrow(new RuntimeException("DB connection error"));

        UserProfileResponse response = userServiceImpl.getUserProfile("dave");

        assertEquals(ResponseConstants.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_INTERNAL_ERROR, response.getMessage());
        assertNull(response.getProfile());
        }

        @Test
        @DisplayName("UT-AS-406: 姓または名がnullの場合、name項目に文字列\"null\"が混入しないこと")
        void getUserProfile_userWithNullFirstName_nameDoesNotContainLiteralNull() {
        // FIX/Pattern B: user.getLastName() + " " + user.getFirstName() は firstName が null のとき
        // Javaの文字列結合仕様上 "Smith null" のような文字列になってしまう。
        // UserServiceImpl.mapToUserInfoResponse() では StringUtils.joinWith + defaultString で
        // 同じ問題を回避しているため、getUserProfile() 側の実装も揃えるべき。
        // このテストは現状の実装に対しては失敗(レッド)することが想定される。
        ParticipatingCompany company = new ParticipatingCompany();
        company.setCompanyId("123");

        ParticipatingUser user = new ParticipatingUser();
        user.setId(4);
        user.setUsername("eve");
        user.setFirstName(null); // 名が未設定
        user.setLastName("Green");
        user.setRole("user");
        user.setCompany(company);

        when(userRepository.findByUsername("eve")).thenReturn(Optional.of(user));

        UserProfileResponse response = userServiceImpl.getUserProfile("eve");

        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertNotNull(response.getProfile());
        assertFalse(response.getProfile().getName().contains("null"),
                "name フィールドに文字列 \"null\" が混入してはいけない: " + response.getProfile().getName());
        assertEquals("Green", response.getProfile().getName());
        }
}
