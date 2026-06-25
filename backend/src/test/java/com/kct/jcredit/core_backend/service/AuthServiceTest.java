package com.kct.jcredit.core_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatorCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.impl.AuthServiceImpl;
import com.kct.jcredit.core_backend.util.JwtUtil;

@ExtendWith(MockitoExtension.class)
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
        operatingUser.setCompany(dummyCompany);
}

    //正しい企業ID/正しいメールアドレス/パスワードでログイン成功
    @Test
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
        when(jwtUtil.generateToken(1, "alice", "123", "USER")).thenReturn("jwt-token-123");

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

        //正しい企業ID/正しいユーザーネーム/パスワードでログイン成功
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
        when(jwtUtil.generateToken(1, "alice", 123, "USER")).thenReturn("jwt-token-123");

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
    void authService_userLogin_email_password_false() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setCompanyId("123");
        req.setIdentifier("alice@example.com"); // username でも email でもOK
        req.setPassword("raw-pass");

        // 企業IDが存在
        ParticipatingCompany existingCompany = new ParticipatingCompany();
        existingCompany.setId(123);
        existingCompany.setCompanyId("123");
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
    void authService_userLogin_email_email_false() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setCompanyId("123");
        req.setIdentifier("alice@example.co"); // username でも email でもOK
        req.setPassword("raw-pass");

        // 企業IDが存在
        ParticipatingCompany existingCompany = new ParticipatingCompany();
        existingCompany.setId(123);
        existingCompany.setCompanyId("123");
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
    void authService_userLogin_account_lock() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setCompanyId(123);
        req.setIdentifier("alice@example.com"); // username でも email でもOK
        req.setPassword("raw-pass");

        // 企業IDが存在
        when(participatingCompanyRepository.findByCompanyId(123))
                .thenReturn(Optional.of(new ParticipatingCompany()));
        
        
        // ユーザーの用意（ロックなし・有効）
        ParticipatingUser user = new ParticipatingUser();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setCompanyId(123);
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
    void authService_userLogin_afterLock12Hours_success() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setCompanyId(123);
        req.setIdentifier("alice@example.com"); // username でも email でもOK
        req.setPassword("raw-pass");

        // 企業IDが存在
        when(participatingCompanyRepository.findByCompanyId(123))
                .thenReturn(Optional.of(new ParticipatingCompany()));
        
        
        // ユーザーの用意（ロックなし・有効）
        ParticipatingUser user = new ParticipatingUser();
        user.setId(1);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setCompanyId(123);
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
        when(jwtUtil.generateToken(1, "alice", 123, "USER")).thenReturn("jwt-token-123");

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
    void authService_userLogin_accountLocked_failure() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setCompanyId(123);
        req.setIdentifier("alice@example.com"); // username でも email でもOK
        req.setPassword("raw-pass");

        // 企業IDが存在
        when(participatingCompanyRepository.findByCompanyId(123))
                .thenReturn(Optional.of(new ParticipatingCompany()));
        
        
        // ユーザーの用意（ロックあり・有効）
        ParticipatingUser user = new ParticipatingUser();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setCompanyId(123);
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

    //正しいユーザーネーム/パスワードでログイン成功
    @Test
    void authService_adminLogin_username_succes() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setIdentifier("alice"); // username でも email でもOK
        req.setPassword("raw-pass");
        
        // ユーザーの用意（ロックなし・有効）
        OperatingUser operator = new OperatingUser();
        operator.setId(1);
        operator.setUsername("alice");
        operator.setEmail("alice@example.com");
        operator.setCompanyId(123);
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
        when(jwtUtil.generateToken(1, "alice", 123, "USER")).thenReturn("jwt-token-123");

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
    void authService_adminLogin_email_succes() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setIdentifier("alice@example.com"); // username でも email でもOK
        req.setPassword("raw-pass");
        
        // ユーザーの用意（ロックなし・有効）
        OperatingUser operator = new OperatingUser();
        operator.setId(1);
        operator.setUsername("alice");
        operator.setEmail("alice@example.com");
        operator.setCompanyId(123);
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
        when(jwtUtil.generateToken(1,   "alice", 123, "USER")).thenReturn("jwt-token-123");

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
    void authService_adminLogin_username_password_false() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setIdentifier("alice"); // username でも email でもOK
        req.setPassword("raw-pass");
        
        // ユーザーの用意（ロックなし・有効）
        OperatingUser operator = new OperatingUser();
        operator.setId(1);
        operator.setUsername("alice");
        operator.setEmail("alice@example.com");
        operator.setCompanyId(123);
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
    void authService_adminLogin_4times_not_account_lock() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setIdentifier("alice"); // username でも email でもOK
        req.setPassword("raw-pass");
        
        // ユーザーの用意（ロックなし・有効）
        OperatingUser operator = new OperatingUser();
        operator.setUsername("alice");
        operator.setEmail("alice@example.com");
        operator.setCompanyId(123);
        operator.setRole("USER");
        operator.setFirstName("Alice");
        operator.setLastName("Smith");
        operator.setPassword("$2a$...hash..."); // ダミー
        operator.setAccountLockedUntil(null);
        operator.setLoginAttempts(4); // 0にリセットされることを確認
        operator.setIsActive(true);

        //ユーザ検索しユーザを返す
        when(operatingUserRepository.findByEmailOrUsername("alice", "alice"))
                .thenReturn(Optional.of(operator));
        

        // パスワード一致
        when(passwordEncoder.matches("raw-pass", "$2a$...hash...")).thenReturn(true);
        

        // トークン発行
        when(jwtUtil.generateToken(1, "alice", 123, "USER")).thenReturn("jwt-token-123");

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
        System.out.println("authService_adminLogin_4times_not_account_lock() succes");
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

    //5回ログイン失敗したらアカウントロックされる
    @Test
    void authService_adminLogin_5times_account_lock() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setIdentifier("alice@example.com"); // username でも email でもOK
        req.setPassword("raw-pass");
        
        // ユーザーの用意（ロックなし・有効）
        OperatingUser operator = new OperatingUser();
        operator.setUsername("alice");
        operator.setEmail("alice@example.com");
        operator.setCompanyId(123);
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
    void authService_adminLogin_afterLock12Hours_succes() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setIdentifier("alice"); // username でも email でもOK
        req.setPassword("raw-pass");
        
        // ユーザーの用意（ロックなし・有効）
        OperatingUser operator = new OperatingUser();
        operator.setId(1);
        operator.setUsername("alice");
        operator.setEmail("alice@example.com");
        operator.setCompanyId(123);
        operator.setRole("USER");
        operator.setFirstName("Alice");
        operator.setLastName("Smith");
        operator.setPassword("$2a$...hash..."); // ダミー
        operator.setAccountLockedUntil(LocalDateTime.now().minusMinutes(1));
        operator.setLoginAttempts(5); // 成功時に0へリセットされることを検証
        operator.setIsActive(true);

        //ユーザ検索しユーザを返す
        when(operatingUserRepository.findByEmailOrUsername("alice", "alice"))
                .thenReturn(Optional.of(operator));
        

        // パスワード一致
        when(passwordEncoder.matches("raw-pass", "$2a$...hash...")).thenReturn(true);
        

        // トークン発行
        when(jwtUtil.generateToken(1, "alice", 123, "USER")).thenReturn("jwt-token-123");

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
        verify(operatingUserRepository, atLeastOnce()).save(userCaptor.capture());
        OperatingUser saved = userCaptor.getValue();
        assertEquals(0, saved.getLoginAttempts());
        //アカウントロック期限時間がnullになっていることを確認
        assertEquals(null, saved.getAccountLockedUntil());

        //結果をlogにだす
        System.out.println("authService_adminLogin_afterLock12Hours_succes() succes");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("response: " + res.getStatusCode());
        System.out.println("token" + res.getToken());
        System.out.println("admin: " + res.getAdmin());
        System.out.println("name: " + res.getAdmin().getUsername());
        System.out.println("lastName firstName:" + res.getAdmin().getName());
        System.out.println("user/master: " + res.getAdmin().getRole());
        System.out.println("Email: " + res.getAdmin().getEmail());
        System.out.println("loginAttemptsNum: " + saved.getLoginAttempts());
        //アカウントロック期限時間がnullになっていることコンソールに出して確認
        System.out.println("accountLockedUntil: " + saved.getAccountLockedUntil());
    }

    //ロックされている状態でログインできないこと
    @Test
    void authService_adminLogin_accountLocked_failure() throws Exception {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setIdentifier("alice"); // username でも email でもOK
        req.setPassword("raw-pass");
        
        // ユーザーの用意（ロックあり・有効）
        OperatingUser operator = new OperatingUser();
        operator.setUsername("alice");
        operator.setEmail("alice@example.com");
        operator.setCompanyId(123);
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

    //一般ユーザが自分自身のパスワード更新に成功
    @Test
    void authService_changePassword_GeneralUserSelfChange_success() {
        // Arrange
        String encodedNewPassword = "$2a$...newhash..."; // モックが返す新しいパスワードのハッシュ
        String testToken = "test-jwt-token";
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(1); // トークンの有効期限（ダミー）

        // PasswordChangeRequestの準備
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(null); // 自分自身のパスワード変更
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        String username = "user1"; // ログイン中のユーザー名

        // Mockの振る舞いを設定
        // 1. changePasswordメソッド内でユーザーを特定
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(participatingUser)); // setUpにてsetしたUserを返している
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.empty()); // 管理者ユーザーが存在しないことを返す

        // 2. updateParticipatingUserPasswordメソッド内のパスワード検証
        when(passwordEncoder.matches(request.getCurrentPassword(), participatingUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(request.getNewPassword(), participatingUser.getPassword())).thenReturn(false); // 新旧パスワードが同じでない

        // 3. パスワードのエンコード
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn(encodedNewPassword);

        // 4. userRepository.saveのモック
        // saveが呼ばれたら、引数で渡されたユーザーをそのまま返すように設定
        when(userRepository.save(any(ParticipatingUser.class))).thenAnswer(invocation -> invocation.getArgument(0));


        // Act
        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        // Assert
        // 結果の検証
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertEquals("パスワードが正常に変更されました。", response.getMessage());

        // userRepository.saveが呼ばれたこと、かつその際にパスワードが更新されたことを検証
        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        ParticipatingUser savedUser = userCaptor.getValue();
        assertEquals(encodedNewPassword, savedUser.getPassword()); // 保存されたユーザーのパスワードが新しいものになっていることを確認

        // 各メソッドが呼ばれた回数を検証
        verify(userRepository, times(1)).findByUsername(username);
        verify(operatingUserRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).matches("old-password", "$2a$...oldhash..."); // 現在のパスワード検証
        verify(passwordEncoder, times(1)).matches("new-password", "$2a$...oldhash..."); // 新旧パスワード同一チェック
        verify(passwordEncoder, times(1)).encode("new-password"); // 新しいパスワードのエンコード
        
        verify(blacklistService, times(1)).recordPasswordChange(username);

        // コンソール出力
        System.out.println("authService_changePassword_participatingUserSelfChange_success() succes");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("Response Status Code: " + response.getStatusCode());
        System.out.println("Response Message: " + response.getMessage());
        System.out.println("Saved User New Password Hash: " + savedUser.getPassword());
    }

    // 1. 管理ユーザが自分自身のパスワード更新に成功
    @Test
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
    void authService_changePassword_MasterChangesAnotherMasterSameCompany_success() {
        // Arrange
        String encodedNewPassword = "$2a$...newhash...";
        String testToken = "test-jwt-token";

        //★
        ParticipatingUser masterUser = new ParticipatingUser();
        masterUser.setId(1);
        masterUser.setUsername("master1");
        masterUser.setPassword("$2a$...masterhash...");
        masterUser.setRole("master");
        masterUser.setCompanyId(123);
        masterUser.setIsActive(true);

        ParticipatingUser targetMasterUser = new ParticipatingUser();
        targetMasterUser.setId(2);
        targetMasterUser.setUsername("master2");
        targetMasterUser.setPassword("$2a$...oldhash...");
        targetMasterUser.setRole("master");
        targetMasterUser.setCompanyId(123);
        targetMasterUser.setIsActive(true);
        //★

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(2);//★
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        String username = "master1";

        // Mockの設定
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.findById(2)).thenReturn(Optional.of(targetMasterUser));//★
        
        when(passwordEncoder.matches(eq("old-password"), anyString())).thenReturn(true);
        when(passwordEncoder.matches(eq("new-password"), anyString())).thenReturn(false);

        //when(passwordEncoder.matches(request.getCurrentPassword(), targetMasterUser.getPassword())).thenReturn(true);
        //when(passwordEncoder.matches(request.getNewPassword(), targetMasterUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn(encodedNewPassword);
        when(userRepository.save(any(ParticipatingUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        //トークンブラックリスト化はしていない

        // Act
        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        // Assert
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertEquals("パスワードが正常に変更されました。", response.getMessage());

        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        ParticipatingUser savedUser = userCaptor.getValue();
        assertEquals(encodedNewPassword, savedUser.getPassword());

        // 各メソッドが呼ばれた回数を検証
        verify(userRepository, times(1)).findByUsername(username);
        verify(operatingUserRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).findById(2);//★
        verify(passwordEncoder, times(1)).matches("old-password", "$2a$...oldhash...");
        verify(passwordEncoder, times(1)).matches("new-password", "$2a$...oldhash...");
        verify(passwordEncoder, times(1)).encode("new-password");
        
        // トークンブラックリスト化されていないことを確認
        verify(jwtUtil, never()).extractExpirationAsLocalDateTime(anyString());//★
        verify(blacklistService, never()).blacklistToken(anyString(), any(LocalDateTime.class));//★

        // コンソール出力
        System.out.println("authService_changePassword_MasterChangesAnotherMasterSameCompany_success() success");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("Response Status Code: " + response.getStatusCode());
        System.out.println("Response Message: " + response.getMessage());
        System.out.println("Saved User New Password Hash: " + savedUser.getPassword());
    }

    // 3. マスターユーザが同一会社の一般ユーザのパスワード更新に成功
    @Test
    void authService_changePassword_MasterChangesGeneralUserSameCompany_success() {
        // Arrange
        String encodedNewPassword = "$2a$...newhash...";
        String testToken = "test-jwt-token";
        //トークンの有効期限なし

        //★
        ParticipatingUser masterUser = new ParticipatingUser();
        masterUser.setId(1);
        masterUser.setUsername("master1");
        masterUser.setPassword("$2a$...masterhash...");
        masterUser.setRole("master");
        masterUser.setCompanyId(123);
        masterUser.setIsActive(true);

        ParticipatingUser targetGeneralUser = new ParticipatingUser();
        targetGeneralUser.setId(3);
        targetGeneralUser.setUsername("user2");
        targetGeneralUser.setPassword("$2a$...oldhash...");
        targetGeneralUser.setRole("user");
        targetGeneralUser.setCompanyId(123);
        targetGeneralUser.setIsActive(true);
        //★

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(3);
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        String username = "master1";

        // Mockの設定
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.findById(3)).thenReturn(Optional.of(targetGeneralUser));//★
        
        when(passwordEncoder.matches(request.getCurrentPassword(), targetGeneralUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(request.getNewPassword(), targetGeneralUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn(encodedNewPassword);
        when(userRepository.save(any(ParticipatingUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        //トークンブラックリスト化のためのもっくなし

        // Act
        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        // Assert
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertEquals("パスワードが正常に変更されました。", response.getMessage());

        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        ParticipatingUser savedUser = userCaptor.getValue();
        assertEquals(encodedNewPassword, savedUser.getPassword());

        // 各メソッドが呼ばれた回数を検証
        verify(userRepository, times(1)).findByUsername(username);
        verify(operatingUserRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).findById(3);//★
        verify(passwordEncoder, times(1)).matches("old-password", "$2a$...oldhash...");
        verify(passwordEncoder, times(1)).matches("new-password", "$2a$...oldhash...");
        verify(passwordEncoder, times(1)).encode("new-password");
        
        // トークンブラックリスト化されていないことを確認
        verify(jwtUtil, never()).extractExpirationAsLocalDateTime(anyString());//★
        verify(blacklistService, never()).blacklistToken(anyString(), any(LocalDateTime.class));//★

        // コンソール出力
        System.out.println("authService_changePassword_MasterChangesGeneralUserSameCompany_success() success");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("Response Status Code: " + response.getStatusCode());
        System.out.println("Response Message: " + response.getMessage());
        System.out.println("Saved User New Password Hash: " + savedUser.getPassword());
    }

    // 4. 管理ユーザがマスターユーザのパスワード更新に成功
    @Test
    void authService_changePassword_OperatingUserChangesMasterUser_success() {
        // Arrange
        String encodedNewPassword = "$2a$...newhash...";
        String testToken = "test-jwt-token";
        //トークンの有効期限なし

        //★
        ParticipatingUser targetMasterUser = new ParticipatingUser();
        targetMasterUser.setId(2);
        targetMasterUser.setUsername("master1");
        targetMasterUser.setPassword("$2a$...oldhash...");
        targetMasterUser.setRole("master");
        targetMasterUser.setCompanyId(123);
        targetMasterUser.setIsActive(true);
        //★

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(2);//★
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        String username = "operatingUser1";

        // Mockの設定
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.of(operatingUser));
        when(userRepository.findById(2)).thenReturn(Optional.of(targetMasterUser));//★
        
        when(passwordEncoder.matches(request.getCurrentPassword(), targetMasterUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(request.getNewPassword(), targetMasterUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn(encodedNewPassword);
        when(userRepository.save(any(ParticipatingUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        //トークンブラックリスト化のためのモックなし

        // Act
        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        // Assert
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertEquals("パスワードが正常に変更されました。", response.getMessage());

        ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        ParticipatingUser savedUser = userCaptor.getValue();
        assertEquals(encodedNewPassword, savedUser.getPassword());

        // 各メソッドが呼ばれた回数を検証
        verify(userRepository, times(1)).findByUsername(username);
        verify(operatingUserRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).findById(2);//★
        verify(passwordEncoder, times(1)).matches("old-password", "$2a$...oldhash...");
        verify(passwordEncoder, times(1)).matches("new-password", "$2a$...oldhash...");
        verify(passwordEncoder, times(1)).encode("new-password");
        
        // トークンブラックリスト化されていないことを確認
        verify(jwtUtil, never()).extractExpirationAsLocalDateTime(anyString());//★
        verify(blacklistService, never()).blacklistToken(anyString(), any(LocalDateTime.class));//★

        // コンソール出力
        System.out.println("authService_changePassword_OperatingUserChangesMasterUser_success() success");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("Response Status Code: " + response.getStatusCode());
        System.out.println("Response Message: " + response.getMessage());
        System.out.println("Saved User New Password Hash: " + savedUser.getPassword());
    }

    // 5. ユーザーネームがどちらにも存在しない → NOT_FOUND
    @Test
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
    void authService_changePassword_GeneralUserChangesOtherUser_returnsForbidden() {
        // Arrange
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setUserId(2);
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        String username = "user1";
        String testToken = "test-jwt-token";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(participatingUser));
        when(operatingUserRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act
        PasswordChangeResponse response = authServiceImpl.changePassword(username, testToken, request);

        // Assert
        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_FORBIDDEN, response.getMessage());

        // 各メソッドが呼ばれた回数を検証
        verify(userRepository, times(1)).findByUsername(username);
        verify(operatingUserRepository, times(1)).findByUsername(username);

        // コンソール出力
        System.out.println("authService_changePassword_GeneralUserChangesOtherUser_returnsForbidden() success");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("Response Status Code: " + response.getStatusCode());
        System.out.println("Response Message: " + response.getMessage());
    }

    // 11. マスターユーザが別会社の他ユーザーパスワード変更要求 → FORBIDDEN
    @Test
    void authService_changePassword_MasterChangesDifferentCompanyUser_returnsForbidden() {
        // Arrange
        ParticipatingUser masterUser = new ParticipatingUser();
        masterUser.setId(1);
        masterUser.setUsername("master1");
        masterUser.setPassword("$2a$...masterhash...");
        masterUser.setRole("master");
        masterUser.setCompanyId(123);
        masterUser.setIsActive(true);

        ParticipatingUser targetUser = new ParticipatingUser();
        targetUser.setId(2);
        targetUser.setUsername("user2");
        targetUser.setPassword("$2a$...oldhash...");
        targetUser.setRole("user");
        targetUser.setCompanyId("COMP999");
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
        System.out.println("authService_changePassword_MasterChangesDifferentCompanyUser_returnsForbidden() success");
        System.out.println("Test execution time: " + OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        System.out.println("Response Status Code: " + response.getStatusCode());
        System.out.println("Response Message: " + response.getMessage());
    }

    // 12. マスターユーザが存在しない対象ユーザ指定 → NOT_FOUND
    @Test
    void authService_changePassword_MasterChangesNonexistentUser_returnsNotFound() {
        // Arrange
        ParticipatingUser masterUser = new ParticipatingUser();
        masterUser.setId(1);
        masterUser.setUsername("master1");
        masterUser.setPassword("$2a$...masterhash...");
        masterUser.setRole("master");
        masterUser.setCompanyId(123);
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
    void authService_changePassword_OperatingUserChangesNonMasterUser_returnsForbidden() {
        // Arrange
        ParticipatingUser generalUser = new ParticipatingUser();
        generalUser.setId(2);
        generalUser.setUsername("user2");
        generalUser.setPassword("$2a$...oldhash...");
        generalUser.setRole("user");
        generalUser.setCompanyId(123);
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
}