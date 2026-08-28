package com.kct.jcredit.core_backend.controller;

// AssertJ
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
// Spring
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.ApproveDeletionRequest;
import com.kct.jcredit.core_backend.dto.request.CreateDeletionRequest;
import com.kct.jcredit.core_backend.dto.request.EmailOtpStartRequest;
import com.kct.jcredit.core_backend.dto.request.OtpResendRequest;
import com.kct.jcredit.core_backend.dto.request.OtpVerifyRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyInfoValidateRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyMethodologyValidateRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyRegisterRequest;
import com.kct.jcredit.core_backend.dto.request.RejectDeletionRequest;
import com.kct.jcredit.core_backend.dto.request.UpdatePartiCompanyRequest;
import com.kct.jcredit.core_backend.dto.response.DeletionRequestResponse;
import com.kct.jcredit.core_backend.dto.response.OtpChallengeResponse;
import com.kct.jcredit.core_backend.dto.response.UpdatePartiCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.CompanyInfoResponse;
import com.kct.jcredit.core_backend.dto.response.details.PartiCompanyResponseDetails;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.service.CompanyDeletionService;
import com.kct.jcredit.core_backend.service.CompanySecurityFlowService;
import com.kct.jcredit.core_backend.service.PartiCompanyRegisterService;
import com.kct.jcredit.core_backend.service.PartiCompanyService;
import com.kct.jcredit.core_backend.util.JwtUtil;
// 他の必要なインポート...
@ExtendWith(MockitoExtension.class)
@DisplayName("PartiCompanyController 単体テスト")
public class PartiCompanyControllerTest {

    // テスト用定数の定義
    private static final String TEST_REASON = "契約違反のため";
    private static final String companyId = "COMP00000001";

    @Mock
    private PartiCompanyService companyService;
    @Mock
    private PartiCompanyRegisterService registerService;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private CompanyDeletionService companyDeletionService;
    @Mock
    private CompanySecurityFlowService companySecurityFlowService;

    @Mock
    private ParticipatingCompanyRepository companyRepository;
    @Mock
    private CompanyInfoResponse companyInfoResponse;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private PartiCompanyController partiCompanyController;

    private UpdatePartiCompanyRequest req;
    private CompanyInfoResponse companyDetails;
    private UpdatePartiCompanyResponse res;

    @BeforeEach
    void setUp() {

        //リクエスト作成
        req = new UpdatePartiCompanyRequest();
        req.setNameJa("株式会社サンプル");
        req.setNameFurigana("カブシキガイシャサンプル");
        req.setNameEn("Sumple Corporation");
        req.setPhoneNumber("070-1234-5678");
        req.setFax("03-1234-5679");



        // ParticipatingCompany（Entity）を作成
        companyDetails = CompanyInfoResponse.builder()
        .id("1")
        .companyId(companyId)
        .nameJA("株式会社サンプル")
        .nameFurigana("カブシキガイシャサンプル")
        .nameEN("Sumple Corporation")
        .email("corporate@company.com") //←リクエストの値に更新される
        .phoneNumber("070-1234-5678") //←リクエストの値に更新される
        .fax("03-1234-5679") //←リクエストの値に更新される
        .corpNumber("123467890123")
        .address("サンプルビル 3F")
        .isSuspended(true)
        .build();

        res = new UpdatePartiCompanyResponse();
        res.setCompany(companyDetails);
        res.setStatusCode(ResponseConstants.OK);

        

    }

    //有効なリクエストで企業情報の更新が成功する（statusCode=200）
    @Test
    @DisplayName("有効なリクエストで企業情報の更新が成功する（statusCode=200）")
    void updateCompany_Success() {
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(auth.getCredentials()).thenReturn("dummy.jwt.token");
        
        when(auth.getCredentials()).thenReturn("dummy.jwt.token");

        when(jwtUtil.extractCompanyId("dummy.jwt.token")).thenReturn(companyId);

        when(companyService.updateCompany(companyId, req)).thenReturn(res);

        //テスト実行
        ResponseEntity<UpdatePartiCompanyResponse> response = partiCompanyController.updateCompany(auth, req);

        UpdatePartiCompanyResponse getBody = response.getBody();
        assertNotNull(getBody);
        assertEquals(companyId, getBody.getCompany().getCompanyId());
        assertEquals(ResponseConstants.OK, getBody.getStatusCode());


        CompanyInfoResponse company = getBody.getCompany();
        assertEquals("株式会社サンプル", company.getNameJA());
        assertEquals("カブシキガイシャサンプル", company.getNameFurigana());
        assertEquals("Sumple Corporation", company.getNameEN());
        assertEquals(req.getPhoneNumber(), company.getPhoneNumber());
        assertEquals(req.getFax(), company.getFax());
        assertEquals("123467890123", company.getCorpNumber());
        assertEquals("サンプルビル 3F", company.getAddress());
        assertEquals(true, company.getIsSuspended());  

        verify(companyService, times(1))
            .updateCompany(eq(companyId), eq(req));
    }

    //レスポンスのstatusCodeに応じた適切なHTTPステータスが返る(NOT_FOUND)
    @Test
    @DisplayName("レスポンスのstatusCodeに応じた適切なHTTPステータスが返る")
    void updateCompany_NOT_FOUND() {
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        
        when(auth.getCredentials()).thenReturn("dummy.jwt.token");

        when(jwtUtil.extractCompanyId("dummy.jwt.token")).thenReturn(companyId);

        UpdatePartiCompanyResponse res = new UpdatePartiCompanyResponse();
        res.setCompany(companyDetails);
        res.setStatusCode(ResponseConstants.NOT_FOUND);
        res.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);

        when(companyService.updateCompany("COMP00000001", req))
            .thenReturn(res);

        //テスト実行
        ResponseEntity<UpdatePartiCompanyResponse> response = partiCompanyController.updateCompany(auth, req);

        UpdatePartiCompanyResponse getBody = response.getBody();
        assertNotNull(getBody);
        assertEquals(companyId, getBody.getCompany().getCompanyId());
        assertEquals("NOT_FOUND", getBody.getStatusCode());
        assertEquals("該当するサイトが見つかりませんでした。", res.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(res.getMessage(), getBody.getMessage());
    }

    //レスポンスのstatusCodeに応じた適切なHTTPステータスが返る(INTERNAL_SERVER_ERROR)
    @Test
    @DisplayName("レスポンスのstatusCodeに応じた適切なHTTPステータスが返る(INTERNAL_SERVER_ERROR)")
    void updateCompany_INTERNAL_SERVER_ERROR() {
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);

        when(auth.getCredentials()).thenReturn("dummy.jwt.token");

        when(jwtUtil.extractCompanyId("dummy.jwt.token")).thenReturn(companyId);

        UpdatePartiCompanyResponse res = new UpdatePartiCompanyResponse();
        res.setCompany(companyDetails);
        res.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
        res.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);

        when(companyService.updateCompany("COMP00000001", req))
            .thenReturn(res);

        //テスト実行
        ResponseEntity<UpdatePartiCompanyResponse> response = partiCompanyController.updateCompany(auth, req);

        UpdatePartiCompanyResponse getBody = response.getBody();
        assertNotNull(getBody);
        assertEquals(companyId, getBody.getCompany().getCompanyId());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(res.getMessage(), getBody.getMessage());
    }

// ========================================
// singnup のテストケース
// ========================================
@Test
@DisplayName("正しいリクエストを送ったとき HTTP 201 が返ること")
void singnup_Success() {
    // === Arrange（準備フェーズ）===

    // リクエストの作成
    PartiCompanyRegisterRequest request = new PartiCompanyRegisterRequest();
    request.setEmailVerificationChallengeId(UUID.randomUUID());
    request.setCorporateName("株式会社サンプル");
    request.setCorporateNameKana("カブシキガイシャサンプル");
    request.setCompanyNameEnglish("Sumple Corporation");

    // モックのレスポンスを作成
    PartiCompanyResponseDetails MockResponse =  new PartiCompanyResponseDetails("CREATED","登録申請が完了しました。","COMP00000001","PENDING");

    // === モックの振る舞いを定義 ===
    when(registerService.register(request))
            .thenReturn(MockResponse);

    // === Act（実行フェーズ）===
    
    ResponseEntity<PartiCompanyResponseDetails> response = 
            partiCompanyController.signup(request);

    // === Assert（検証フェーズ）===
    
    // レスポンスがnullでないことを確認
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    PartiCompanyResponseDetails body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getStatusCode()).isEqualTo("CREATED");
    assertThat(body.getMessage()).isEqualTo("登録申請が完了しました。");
    assertThat(body.getRegistrationId()).isEqualTo("COMP00000001");
    assertThat(body.getStatus()).isEqualTo("PENDING");

    verify(registerService, times(1)).register(
        eq(request));
}

// ========================================
// startSignupEmailVerification のテストケース
// ========================================
@Test
@DisplayName("正しいリクエストを送ったとき HTTP 200 が返ること")
void startSignupEmailVerification_Success() {
    // === Arrange（準備フェーズ）===
    
    String email = "yamada_taro@test.com";

    // リクエストの作成
    EmailOtpStartRequest request = new EmailOtpStartRequest();
    request.setEmail(email);

    // モックのレスポンスを作成
    OtpChallengeResponse MockResponse =  new OtpChallengeResponse();
    MockResponse.setStatusCode(ResponseConstants.OK);
    MockResponse.setMessage(ResponseConstants.OK);
    MockResponse.setChallengeId(UUID.randomUUID());
    MockResponse.setMaskedEmail(email);
    MockResponse.setExpiresInSeconds(300L);

    // === モックの振る舞いを定義 ===
    when(companySecurityFlowService.startSignupEmailVerification(
            eq(request.getEmail())))
            .thenReturn(MockResponse);

    // === Act（実行フェーズ）===
    
    ResponseEntity<OtpChallengeResponse> response = 
            partiCompanyController.startSignupEmailVerification(request);

    // === Assert（検証フェーズ）===
    
    // レスポンスがnullでないことを確認
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    OtpChallengeResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.OK);
    assertThat(body.getMessage()).isEqualTo(ResponseConstants.OK);
    assertThat(body.getChallengeId()).isNotNull();
    assertThat(body.getMaskedEmail()).isEqualTo(email);
    assertThat(body.getExpiresInSeconds()).isEqualTo(300L);

    verify(companySecurityFlowService, times(1)).startSignupEmailVerification(
        eq(email));

}


@Test
@DisplayName("レスポンスのstatusCodeに応じた適切なHTTPステータスが返る(BAD_REQUEST)")
void startSignupEmailVerification_BAD_REQUEST() {
    // === Arrange（準備フェーズ）===
    
    String email = "yamada_taro@test.com";

    // リクエストの作成
    EmailOtpStartRequest request = new EmailOtpStartRequest();
    request.setEmail(email);

    // モックのレスポンスを作成
    OtpChallengeResponse MockResponse =  new OtpChallengeResponse();
    MockResponse.setStatusCode(ResponseConstants.BAD_REQUEST);
    MockResponse.setMessage(ResponseConstants.MSG_BAD_REQUEST);

    // === モックの振る舞いを定義 ===
    when(companySecurityFlowService.startSignupEmailVerification(
            eq(request.getEmail())))
            .thenReturn(MockResponse);

    // === Act（実行フェーズ）===
    
    ResponseEntity<OtpChallengeResponse> response = 
            partiCompanyController.startSignupEmailVerification(request);

    // === Assert（検証フェーズ）===
    
    // レスポンスがnullでないことを確認
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    OtpChallengeResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
    assertThat(body.getMessage()).isEqualTo(ResponseConstants.MSG_BAD_REQUEST);

}

    
// ========================================
// resendSignupEmailVerificationのテストケース
// ========================================
@Test
@DisplayName("正しいリクエストを送ったとき HTTP 200 が返ること")
void resendSignupEmailVerification_Success() {
    // === Arrange（準備フェーズ）===
    
    String email = "yamada_taro@test.com";

    // リクエストの作成
    OtpResendRequest request = new OtpResendRequest();
    request.setChallengeId(UUID.randomUUID());

    // モックのレスポンスを作成
    OtpChallengeResponse MockResponse =  new OtpChallengeResponse();
    MockResponse.setStatusCode(ResponseConstants.OK);
    MockResponse.setMessage(ResponseConstants.OK);
    MockResponse.setChallengeId(UUID.randomUUID());
    MockResponse.setMaskedEmail(email);
    MockResponse.setExpiresInSeconds(300L);

    // === モックの振る舞いを定義 ===
    when(companySecurityFlowService.resendSignupEmailVerification(
            eq(request.getChallengeId())))
            .thenReturn(MockResponse);

    // === Act（実行フェーズ）===
    
    ResponseEntity<OtpChallengeResponse> response = 
            partiCompanyController.resendSignupEmailVerification(request);

    // === Assert（検証フェーズ）===
    
    // レスポンスがnullでないことを確認
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    OtpChallengeResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.OK);
    assertThat(body.getMessage()).isEqualTo(ResponseConstants.OK);
    assertThat(body.getChallengeId()).isNotNull();
    assertThat(body.getMaskedEmail()).isEqualTo(email);
    assertThat(body.getExpiresInSeconds()).isEqualTo(300L);

    verify(companySecurityFlowService, times(1)).resendSignupEmailVerification(
        eq(request.getChallengeId()));

    System.out.println(request);
    System.out.println(response);
    System.out.println(body);

}

@Test
@DisplayName("レスポンスのstatusCodeに応じた適切なHTTPステータスが返る(BAD_REQUEST)")
void resendSignupEmailVerification_BAD_REQUEST() {
    // === Arrange（準備フェーズ）===
    // リクエストの作成
    OtpResendRequest request = new OtpResendRequest();
    request.setChallengeId(UUID.randomUUID());

    // モックのレスポンスを作成
    OtpChallengeResponse MockResponse =  new OtpChallengeResponse();
    MockResponse.setStatusCode(ResponseConstants.BAD_REQUEST);
    MockResponse.setMessage(ResponseConstants.MSG_BAD_REQUEST);
    MockResponse.setChallengeId(UUID.randomUUID());

    // === モックの振る舞いを定義 ===
    when(companySecurityFlowService.resendSignupEmailVerification(
            eq(request.getChallengeId())))
            .thenReturn(MockResponse);

    // === Act（実行フェーズ）===
    
    ResponseEntity<OtpChallengeResponse> response = 
            partiCompanyController.resendSignupEmailVerification(request);

    // === Assert（検証フェーズ）===
    
    // レスポンスがnullでないことを確認
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    OtpChallengeResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
    assertThat(body.getMessage()).isEqualTo(ResponseConstants.MSG_BAD_REQUEST);
}


// ========================================
// verifySignupEmailのテストケース
// ========================================
@Test
@DisplayName("正しいリクエストを送ったとき HTTP 200 が返ること")
void verifySignupEmail_Success() {
    // === Arrange（準備フェーズ）===
    
    String code = "123456";

    // リクエストの作成
    OtpVerifyRequest request = new OtpVerifyRequest();
    request.setChallengeId(UUID.randomUUID());
    request.setCode(code);

    // モックのレスポンスを作成
    BaseResponse MockResponse =  new BaseResponse();
    MockResponse.setStatusCode(ResponseConstants.OK);
    MockResponse.setMessage(ResponseConstants.OK);

    // === モックの振る舞いを定義 ===
    when(companySecurityFlowService.verifySignupEmail(
            eq(request.getChallengeId()),
            eq(request.getCode())))
            .thenReturn(MockResponse);

    // === Act（実行フェーズ）===
    
    ResponseEntity<BaseResponse> response = 
            partiCompanyController.verifySignupEmail(request);

    // === Assert（検証フェーズ）===
    
    // レスポンスがnullでないことを確認
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    BaseResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.OK);
    assertThat(body.getMessage()).isEqualTo(ResponseConstants.OK);

    verify(companySecurityFlowService, times(1)).verifySignupEmail(
        eq(request.getChallengeId()),
        eq(request.getCode()));

    System.out.println(request);
    System.out.println(response);
    System.out.println(body);
}

@Test
@DisplayName("レスポンスのstatusCodeに応じた適切なHTTPステータスが返る(BAD_REQUEST)")
void verifySignupEmail_BAD_REQUEST() {
    // === Arrange（準備フェーズ）===
    
    String code = "123456";

    // リクエストの作成
    OtpVerifyRequest request = new OtpVerifyRequest();
    request.setChallengeId(UUID.randomUUID());
    request.setCode(code);

    // モックのレスポンスを作成
    BaseResponse MockResponse =  new BaseResponse();
    MockResponse.setStatusCode(ResponseConstants.BAD_REQUEST);
    MockResponse.setMessage(ResponseConstants.MSG_BAD_REQUEST);

    // === モックの振る舞いを定義 ===
    when(companySecurityFlowService.verifySignupEmail(
            eq(request.getChallengeId()),
            eq(request.getCode())))
            .thenReturn(MockResponse);

    // === Act（実行フェーズ）===
    
    ResponseEntity<BaseResponse> response = 
            partiCompanyController.verifySignupEmail(request);

    // === Assert（検証フェーズ）===
    
    // レスポンスがnullでないことを確認
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    BaseResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
    assertThat(body.getMessage()).isEqualTo(ResponseConstants.MSG_BAD_REQUEST);
}

// ========================================
//  getRejectedSignupRevisionのテストケース
// ========================================
@Test
@DisplayName("正しいリクエストを送ったとき HTTP 200 が返ること")
void  getRejectedSignupRevision_Success() {
    // === Arrange（準備フェーズ）===
    
    String actionSession = "test-session-token";

    // モックのレスポンスを作成
    PartiCompanyRegisterRequest mockResponse =  new PartiCompanyRegisterRequest();

    // === モックの振る舞いを定義 ===
    when(companySecurityFlowService.getRevisionDraft(actionSession))
            .thenReturn(mockResponse);

    // === Act（実行フェーズ）===
    
    ResponseEntity<PartiCompanyRegisterRequest> response = 
            partiCompanyController.getRejectedSignupRevision(actionSession);

    // === Assert（検証フェーズ）===
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(mockResponse);

    verify(companySecurityFlowService).getRevisionDraft(actionSession);
}


// ========================================
// reviseRejectedSignupのテストケース
// ========================================
@Test
@DisplayName("正しいリクエストを送ったとき HTTP 200 が返ること")
void reviseRejectedSignup_Success() {
    // === Arrange（準備フェーズ）===
    
    String actionSession = "test-session-token";
    String number = "1234567890123";
    String company = "株式会社サンプル";

    // リクエストの作成
    PartiCompanyRegisterRequest request = new PartiCompanyRegisterRequest();
    request.setCorporateNumber(number);
    request.setCorporateName(company);

    // モックのレスポンスを作成
    BaseResponse mockResponse =  new BaseResponse();
    mockResponse.setStatusCode(ResponseConstants.OK);
    mockResponse.setMessage(ResponseConstants.OK);

    // === モックの振る舞いを定義 ===
    when(companySecurityFlowService.reviseRejectedSignup(actionSession,request))
            .thenReturn(mockResponse);

    // === Act（実行フェーズ）===
    
    ResponseEntity<BaseResponse> response = 
            partiCompanyController.reviseRejectedSignup(actionSession,request);

    // === Assert（検証フェーズ）===
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(mockResponse);

    verify(companySecurityFlowService).reviseRejectedSignup(actionSession,request);
}



// ========================================
// validateSignupCompanyInfoのテストケース
// ========================================
@Test
@DisplayName("正しいリクエストを送ったとき HTTP 200 が返ること")
void validateSignupCompanyInfo_Success() {
    // === Arrange（準備フェーズ）===
    
    String actionSession = "test-session-token";
    String number = "1234567890123";
    String company = "株式会社サンプル";

    // リクエストの作成
    PartiCompanyInfoValidateRequest request = new PartiCompanyInfoValidateRequest();
    request.setCorporateNumber(number);
    request.setCorporateName(company);

    // モックのレスポンスを作成
    BaseResponse mockResponse =  new BaseResponse();
    mockResponse.setStatusCode(ResponseConstants.OK);
    mockResponse.setMessage(ResponseConstants.MSG_VALIDATION_SUCCESS);
    mockResponse.setTimestamp(LocalDateTime.now());

    // === モックの振る舞いを定義 ===
    doNothing().when(registerService).validateCompanyInfo(request);

    // === Act（実行フェーズ）===
    
    ResponseEntity<BaseResponse> response = 
            partiCompanyController.validateSignupCompanyInfo(request);

    // === Assert（検証フェーズ）===
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getStatusCode()).isEqualTo(mockResponse.getStatusCode());
    assertThat(response.getBody().getMessage()).isEqualTo(mockResponse.getMessage());

    verify(registerService).validateCompanyInfo(request);
}

// ========================================
// validateSignupCompanyInfoのテストケース
// ========================================
@Test
@DisplayName("正しいリクエストを送ったとき HTTP 200 が返ること")
void validateSignupMethodology_Success() {
    // === Arrange（準備フェーズ）===
    PartiCompanyMethodologyValidateRequest request = new PartiCompanyMethodologyValidateRequest();
    
    // リクエストの作成

    // Act
    ResponseEntity<BaseResponse> response =
            partiCompanyController.validateSignupMethodology(request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.OK);
    assertThat(response.getBody().getMessage()).isEqualTo(ResponseConstants.MSG_VALIDATION_SUCCESS);
}

// ========================================
// createDeletionRequest のテストケース
// ========================================
@Test
@DisplayName("削除申請: 正常に削除申請を作成できること")
void testCreateDeletionRequest_Success() {
    // === Arrange（準備フェーズ）===
    
    // リクエストの作成
    CreateDeletionRequest request = new CreateDeletionRequest();
    request.setReason(TEST_REASON);

    // モックのレスポンスを作成
    DeletionRequestResponse mockResponse = DeletionRequestResponse.builder()
            .statusCode(ResponseConstants.OK)
            .message(ResponseConstants.MSG_DELETION_REQUEST_SUBMITTED)
            .requestId(1)
            .companyId(companyId)
            .companyName("テスト企業株式会社")
            .requestedBy("master001")
            .requestedAt(LocalDateTime.now())
            .reason(TEST_REASON)
            .status("PENDING")
            .build();

    // === モックの振る舞いを定義 ===

    String mockJwtToken = "mock.jwt.token";
    when(authentication.getCredentials()).thenReturn(mockJwtToken);
    
    // jwtUtilのモック設定: JWTトークンからユーザー名を抽出
    when(jwtUtil.extractUsername(any())).thenReturn("master001");

    // サービスのモック設定: 削除申請を作成
    when(companyDeletionService.createDeletionRequest(
            eq(companyId), 
            eq("master001"), 
            eq(TEST_REASON)))
            .thenReturn(mockResponse);

    // === Act（実行フェーズ）===
    
    ResponseEntity<DeletionRequestResponse> response = 
            partiCompanyController.createDeletionRequest(authentication, companyId, request);

    // === Assert（検証フェーズ）===
    
    // レスポンスがnullでないことを確認
    assertThat(response).isNotNull();

    // HTTPステータスが200 OKであることを確認
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // レスポンスボディを取得
    DeletionRequestResponse body = response.getBody();
    assertThat(body).isNotNull();

    // レスポンスボディのステータスコードがOKであることを確認
    assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.OK);

    // レスポンスボディのメッセージを確認
    assertThat(body.getMessage()).isEqualTo(ResponseConstants.MSG_DELETION_REQUEST_SUBMITTED);

    // リクエストIDが正しいことを確認
    assertThat(body.getRequestId()).isEqualTo(1);

    // 企業IDが正しいことを確認
    assertThat(body.getCompanyId()).isEqualTo(companyId);

    // 企業名が正しいことを確認
    assertThat(body.getCompanyName()).isEqualTo("テスト企業株式会社");

    // 申請者が正しいことを確認
    assertThat(body.getRequestedBy()).isEqualTo("master001");

    // 理由が正しいことを確認
    assertThat(body.getReason()).isEqualTo(TEST_REASON);

    // ステータスが正しいことを確認（保留中）
    assertThat(body.getStatus()).isEqualTo("PENDING");

    // 申請日時がnullでないことを確認
    assertThat(body.getRequestedAt()).isNotNull();

    // === Verify（呼び出し検証フェーズ）===
    
    // jwtUtil.extractUsernameが1回だけ呼ばれたことを確認
    verify(jwtUtil, times(1)).extractUsername(any());

    // companyDeletionService.createDeletionRequestが1回だけ呼ばれたことを確認
    verify(companyDeletionService, times(1)).createDeletionRequest(
            eq(companyId), 
            eq("master001"), 
            eq(TEST_REASON));
}

@Test
@DisplayName("削除申請: 企業が見つからない場合")
void testCreateDeletionRequest_CompanyNotFound() {
    // === Arrange（準備フェーズ）===
    
    // リクエストの作成
    CreateDeletionRequest request = new CreateDeletionRequest();
    request.setReason(TEST_REASON);

    // モックのレスポンスを作成（企業が見つからない）
    DeletionRequestResponse mockResponse = DeletionRequestResponse.builder()
            .statusCode(ResponseConstants.NOT_FOUND)
            .message(ResponseConstants.MSG_NOT_FOUND)
            .build();

    // === モックの振る舞いを定義 ===

    String mockJwtToken = "mock.jwt.token";
    when(authentication.getCredentials()).thenReturn(mockJwtToken);
    
    // jwtUtilのモック設定: JWTトークンからユーザー名を抽出
    when(jwtUtil.extractUsername(any())).thenReturn("master001");

    // サービスのモック設定: 企業が見つからないレスポンスを返す
    when(companyDeletionService.createDeletionRequest(
            eq(companyId), 
            eq("master001"), 
            eq(TEST_REASON)))
            .thenReturn(mockResponse);

    // === Act（実行フェーズ）===
    
    ResponseEntity<DeletionRequestResponse> response = 
            partiCompanyController.createDeletionRequest(authentication, companyId, request);

    // === Assert（検証フェーズ）===
    
    // レスポンスがnullでないことを確認
    assertThat(response).isNotNull();

    // HTTPステータスが404 NOT_FOUNDであることを確認
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    // レスポンスボディを取得
    DeletionRequestResponse body = response.getBody();
    assertThat(body).isNotNull();

    // レスポンスボディのステータスコードがNOT_FOUNDであることを確認
    assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);

    // レスポンスボディのメッセージを確認
    assertThat(body.getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);

    // === Verify（呼び出し検証フェーズ）===
    
    // jwtUtil.extractUsernameが1回だけ呼ばれたことを確認
    verify(jwtUtil, times(1)).extractUsername(any());

    // companyDeletionService.createDeletionRequestが1回だけ呼ばれたことを確認
    verify(companyDeletionService, times(1)).createDeletionRequest(
            eq(companyId), 
            eq("master001"), 
            eq(TEST_REASON));
}
    @Test
    @DisplayName("削除申請: 企業が既に削除されている場合")
    void testCreateDeletionRequest_CompanyAlreadyDeleted() {
        // === Arrange（準備フェーズ）===
        
        // リクエストの作成
        CreateDeletionRequest request = new CreateDeletionRequest();
        request.setReason(TEST_REASON);

        // モックのレスポンスを作成（企業が既に削除されている）
        DeletionRequestResponse mockResponse = DeletionRequestResponse.builder()
                .statusCode(ResponseConstants.BAD_REQUEST)
                .message(ResponseConstants.MSG_COMPANY_ALREADY_DELETED)
                .companyId(companyId)
                .companyName("テスト企業株式会社")
                .build();

        // === モックの振る舞いを定義 ===
        
        String mockJwtToken = "mock.jwt.token";
        when(authentication.getCredentials()).thenReturn(mockJwtToken);

        // jwtUtilのモック設定: JWTトークンからユーザー名を抽出
        when(jwtUtil.extractUsername(any())).thenReturn("master001");

        // サービスのモック設定: 企業が既に削除されているエラーレスポンスを返す
        when(companyDeletionService.createDeletionRequest(
                eq(companyId), 
                eq("master001"), 
                eq(TEST_REASON)))
                .thenReturn(mockResponse);

        // === Act（実行フェーズ）===
        
        ResponseEntity<DeletionRequestResponse> response = 
                partiCompanyController.createDeletionRequest(authentication, companyId, request);

        // === Assert（検証フェーズ）===
        
        // レスポンスがnullでないことを確認
        assertThat(response).isNotNull();

        // HTTPステータスが400 BAD_REQUESTであることを確認
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // レスポンスボディを取得
        DeletionRequestResponse body = response.getBody();
        assertThat(body).isNotNull();

        // レスポンスボディのステータスコードがBAD_REQUESTであることを確認
        assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);

        // レスポンスボディのメッセージを確認（企業が既に削除されている）
        assertThat(body.getMessage()).isEqualTo(ResponseConstants.MSG_COMPANY_ALREADY_DELETED);

        // 企業IDが正しいことを確認
        assertThat(body.getCompanyId()).isEqualTo(companyId);

        // 企業名が正しいことを確認
        assertThat(body.getCompanyName()).isEqualTo("テスト企業株式会社");

        // === Verify（呼び出し検証フェーズ）===
        
        // jwtUtil.extractUsernameが1回だけ呼ばれたことを確認
        verify(jwtUtil, times(1)).extractUsername(any());

        // companyDeletionService.createDeletionRequestが1回だけ呼ばれたことを確認
        verify(companyDeletionService, times(1)).createDeletionRequest(
                eq(companyId), 
                eq("master001"), 
                eq(TEST_REASON));
    }

    @Test
    @DisplayName("削除申請: 既に削除申請が存在する場合")
    void testCreateDeletionRequest_AlreadyRequested() {
        // === Arrange（準備フェーズ）===
        
        // リクエストの作成
        CreateDeletionRequest request = new CreateDeletionRequest();
        request.setReason(TEST_REASON);

        // モックのレスポンスを作成（既に削除申請が存在する）
        DeletionRequestResponse mockResponse = DeletionRequestResponse.builder()
                .statusCode(ResponseConstants.BAD_REQUEST)
                .message(ResponseConstants.MSG_DELETION_ALREADY_REQUESTED)
                .companyId(companyId)
                .companyName("テスト企業株式会社")
                .build();

        // === モックの振る舞いを定義 ===

        String mockJwtToken = "mock.jwt.token";
        when(authentication.getCredentials()).thenReturn(mockJwtToken);
        
        // jwtUtilのモック設定: JWTトークンからユーザー名を抽出
        when(jwtUtil.extractUsername(any())).thenReturn("master001");

        // サービスのモック設定: 既に削除申請が存在するエラーレスポンスを返す
        when(companyDeletionService.createDeletionRequest(
                eq(companyId), 
                eq("master001"), 
                eq(TEST_REASON)))
                .thenReturn(mockResponse);

        // === Act（実行フェーズ）===
        
        ResponseEntity<DeletionRequestResponse> response = 
                partiCompanyController.createDeletionRequest(authentication, companyId, request);

        // === Assert（検証フェーズ）===
        
        // レスポンスがnullでないことを確認
        assertThat(response).isNotNull();

        // HTTPステータスが400 BAD_REQUESTであることを確認
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // レスポンスボディを取得
        DeletionRequestResponse body = response.getBody();
        assertThat(body).isNotNull();

        // レスポンスボディのステータスコードがBAD_REQUESTであることを確認
        assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);

        // レスポンスボディのメッセージを確認（既に削除申請が存在する）
        assertThat(body.getMessage()).isEqualTo(ResponseConstants.MSG_DELETION_ALREADY_REQUESTED);

        // 企業IDが正しいことを確認
        assertThat(body.getCompanyId()).isEqualTo(companyId);

        // 企業名が正しいことを確認
        assertThat(body.getCompanyName()).isEqualTo("テスト企業株式会社");

        // === Verify（呼び出し検証フェーズ）===
        
        // jwtUtil.extractUsernameが1回だけ呼ばれたことを確認
        verify(jwtUtil, times(1)).extractUsername(any());

        // companyDeletionService.createDeletionRequestが1回だけ呼ばれたことを確認
        verify(companyDeletionService, times(1)).createDeletionRequest(
                eq(companyId), 
                eq("master001"), 
                eq(TEST_REASON));
    }

// ========================================
// approveDeletionRequest のテストケース
// ========================================
@Test
@DisplayName("削除承認: 正常に削除申請を承認できること")
void testApproveDeletionRequest_Success() {
    // === Arrange（準備フェーズ）===
    
    // リクエストの作成
    ApproveDeletionRequest request = new ApproveDeletionRequest();

    // モックのレスポンスを作成
    DeletionRequestResponse mockResponse = DeletionRequestResponse.builder()
            .statusCode(ResponseConstants.OK)
            .message(ResponseConstants.MSG_DELETION_REQUEST_APPROVED)
            .requestId(1)
            .companyId(companyId)
            .companyName("テスト企業株式会社")
            .requestedBy("master001")
            .requestedAt(LocalDateTime.now().minusDays(1))
            .reason(TEST_REASON)
            .status("APPROVED")
            .decidedBy("operator001")
            .decidedAt(LocalDateTime.now())
            .build();

    // === モックの振る舞いを定義 ===

    String mockJwtToken = "mock.jwt.token";
    when(authentication.getCredentials()).thenReturn(mockJwtToken);
    
    // jwtUtilのモック設定: JWTトークンから承認者のユーザー名を抽出
    when(jwtUtil.extractUsername(any())).thenReturn("operator001");

    // サービスのモック設定: 削除申請を承認
    when(companyDeletionService.approveDeletionRequest(
            eq(companyId), 
            eq("operator001")))
            .thenReturn(mockResponse);

    // === Act（実行フェーズ）===
    
    ResponseEntity<DeletionRequestResponse> response = 
            partiCompanyController.approveDeletionRequest(authentication, companyId, request);

    // === Assert（検証フェーズ）===
    
    // レスポンスがnullでないことを確認
    assertThat(response).isNotNull();

    // HTTPステータスが200 OKであることを確認
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // レスポンスボディを取得
    DeletionRequestResponse body = response.getBody();
    assertThat(body).isNotNull();

    // レスポンスボディのステータスコードがOKであることを確認
    assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.OK);

    // レスポンスボディのメッセージを確認
    assertThat(body.getMessage()).isEqualTo(ResponseConstants.MSG_DELETION_REQUEST_APPROVED);

    // リクエストIDが正しいことを確認
    assertThat(body.getRequestId()).isEqualTo(1);

    // 企業IDが正しいことを確認
    assertThat(body.getCompanyId()).isEqualTo(companyId);

    // 企業名が正しいことを確認
    assertThat(body.getCompanyName()).isEqualTo("テスト企業株式会社");

    // 申請者が正しいことを確認
    assertThat(body.getRequestedBy()).isEqualTo("master001");

    // 申請日時がnullでないことを確認
    assertThat(body.getRequestedAt()).isNotNull();

    // 削除理由が正しいことを確認
    assertThat(body.getReason()).isEqualTo(TEST_REASON);

    // ステータスが正しいことを確認（承認済み）
    assertThat(body.getStatus()).isEqualTo("APPROVED");

    // 承認者が正しいことを確認
    assertThat(body.getDecidedBy()).isEqualTo("operator001");

    // 承認日時がnullでないことを確認
    assertThat(body.getDecidedAt()).isNotNull();

    // === Verify（呼び出し検証フェーズ）===
    
    // jwtUtil.extractUsernameが1回だけ呼ばれたことを確認
    verify(jwtUtil, times(1)).extractUsername(any());

    // companyDeletionService.approveDeletionRequestが1回だけ呼ばれたことを確認
    verify(companyDeletionService, times(1)).approveDeletionRequest(
            eq(companyId), 
            eq("operator001"));
}
    @Test
    @DisplayName("削除承認: 削除申請が見つからない場合")
    void testApproveDeletionRequest_RequestNotFound() {
        // === Arrange（準備フェーズ）===
        
        // リクエストの作成
        ApproveDeletionRequest request = new ApproveDeletionRequest();

        // モックのレスポンスを作成（削除申請が見つからない）
        DeletionRequestResponse mockResponse = DeletionRequestResponse.builder()
                .statusCode(ResponseConstants.NOT_FOUND)
                .message(ResponseConstants.MSG_DELETION_REQUEST_NOT_FOUND)
                .build();

        // === モックの振る舞いを定義 ===

        String mockJwtToken = "mock.jwt.token";
        when(authentication.getCredentials()).thenReturn(mockJwtToken);
        
        // jwtUtilのモック設定: JWTトークンから承認者のユーザー名を抽出
        when(jwtUtil.extractUsername(any())).thenReturn("operator001");

        // サービスのモック設定: 削除申請が見つからないエラーレスポンスを返す
        when(companyDeletionService.approveDeletionRequest(
                eq(companyId), 
                eq("operator001")))
                .thenReturn(mockResponse);

        // === Act（実行フェーズ）===
        
        ResponseEntity<DeletionRequestResponse> response = 
                partiCompanyController.approveDeletionRequest(authentication, companyId, request);

        // === Assert（検証フェーズ）===
        
        // レスポンスがnullでないことを確認
        assertThat(response).isNotNull();

        // HTTPステータスが404 NOT_FOUNDであることを確認
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // レスポンスボディを取得
        DeletionRequestResponse body = response.getBody();
        assertThat(body).isNotNull();

        // レスポンスボディのステータスコードがNOT_FOUNDであることを確認
        assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);

        // レスポンスボディのメッセージを確認（削除申請が見つからない）
        assertThat(body.getMessage()).isEqualTo(ResponseConstants.MSG_DELETION_REQUEST_NOT_FOUND);

        // === Verify（呼び出し検証フェーズ）===
        
        // jwtUtil.extractUsernameが1回だけ呼ばれたことを確認
        verify(jwtUtil, times(1)).extractUsername(any());

        // companyDeletionService.approveDeletionRequestが1回だけ呼ばれたことを確認
        verify(companyDeletionService, times(1)).approveDeletionRequest(
                eq(companyId), 
                eq("operator001"));
    }

// ========================================
// rejectDeletionRequest のテストケース
// ========================================
    @Test
    @DisplayName("削除却下: 正常に削除申請を却下できること")
    void testRejectDeletionRequest_Success() {
        // === Arrange（準備フェーズ）===
        
        String rejectionReason = "要件が不十分";
        
        // リクエストの作成
        RejectDeletionRequest request = new RejectDeletionRequest();
        request.setRejectionReason(rejectionReason);

        // モックのレスポンスを作成
        DeletionRequestResponse mockResponse = DeletionRequestResponse.builder()
                .statusCode(ResponseConstants.OK)
                .message(ResponseConstants.MSG_DELETION_REQUEST_REJECTED)
                .requestId(1)
                .companyId(companyId)
                .companyName("テスト企業株式会社")
                .requestedBy("master001")
                .requestedAt(LocalDateTime.now().minusDays(1))
                .reason(TEST_REASON)
                .status("REJECTED")
                .decidedBy("operator001")
                .decidedAt(LocalDateTime.now())
                .rejectionReason(rejectionReason)
                .build();

        // === モックの振る舞いを定義 ===

        String mockJwtToken = "mock.jwt.token";
        when(authentication.getCredentials()).thenReturn(mockJwtToken);
        
        // jwtUtilのモック設定: JWTトークンから却下者のユーザー名を抽出
        when(jwtUtil.extractUsername(any())).thenReturn("operator001");

        // サービスのモック設定: 削除申請を却下
        when(companyDeletionService.rejectDeletionRequest(
                eq(companyId), 
                eq("operator001"), 
                eq(rejectionReason)))
                .thenReturn(mockResponse);

        // === Act（実行フェーズ）===
        
        ResponseEntity<DeletionRequestResponse> response = 
                partiCompanyController.rejectDeletionRequest(authentication, companyId, request);

        // === Assert（検証フェーズ）===
        
        // レスポンスがnullでないことを確認
        assertThat(response).isNotNull();

        // HTTPステータスが200 OKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // レスポンスボディを取得
        DeletionRequestResponse body = response.getBody();
        assertThat(body).isNotNull();

        // レスポンスボディのステータスコードがOKであることを確認
        assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.OK);

        // レスポンスボディのメッセージを確認
        assertThat(body.getMessage()).isEqualTo(ResponseConstants.MSG_DELETION_REQUEST_REJECTED);

        // リクエストIDが正しいことを確認
        assertThat(body.getRequestId()).isEqualTo(1);

        // 企業IDが正しいことを確認
        assertThat(body.getCompanyId()).isEqualTo(companyId);

        // 企業名が正しいことを確認
        assertThat(body.getCompanyName()).isEqualTo("テスト企業株式会社");

        // 申請者が正しいことを確認
        assertThat(body.getRequestedBy()).isEqualTo("master001");

        // 申請日時がnullでないことを確認
        assertThat(body.getRequestedAt()).isNotNull();

        // 申請理由が正しいことを確認
        assertThat(body.getReason()).isEqualTo(TEST_REASON);

        // ステータスが正しいことを確認（却下済み）
        assertThat(body.getStatus()).isEqualTo("REJECTED");

        // 却下者が正しいことを確認
        assertThat(body.getDecidedBy()).isEqualTo("operator001");

        // 却下日時がnullでないことを確認
        assertThat(body.getDecidedAt()).isNotNull();

        // 却下理由が正しいことを確認
        assertThat(body.getRejectionReason()).isEqualTo(rejectionReason);

        // === Verify（呼び出し検証フェーズ）===
        
        // jwtUtil.extractUsernameが1回だけ呼ばれたことを確認
        verify(jwtUtil, times(1)).extractUsername(any());

        // companyDeletionService.rejectDeletionRequestが1回だけ呼ばれたことを確認
        verify(companyDeletionService, times(1)).rejectDeletionRequest(
                eq(companyId), 
                eq("operator001"), 
                eq(rejectionReason));
    }

    @Test
    @DisplayName("削除却下: 削除申請が見つからない場合")
    void testRejectDeletionRequest_RequestNotFound() {
        // === Arrange（準備フェーズ）===
        
        String rejectionReason = "要件が不十分";
        
        // リクエストの作成
        RejectDeletionRequest request = new RejectDeletionRequest();
        request.setRejectionReason(rejectionReason);

        // モックのレスポンスを作成（削除申請が見つからない）
        DeletionRequestResponse mockResponse = DeletionRequestResponse.builder()
                .statusCode(ResponseConstants.NOT_FOUND)
                .message(ResponseConstants.MSG_DELETION_REQUEST_NOT_FOUND)
                .build();

        // === モックの振る舞いを定義 ===

        String mockJwtToken = "mock.jwt.token";
        when(authentication.getCredentials()).thenReturn(mockJwtToken);
        
        // jwtUtilのモック設定: JWTトークンから却下者のユーザー名を抽出
        when(jwtUtil.extractUsername(any())).thenReturn("operator001");

        // サービスのモック設定: 削除申請が見つからないエラーレスポンスを返す
        when(companyDeletionService.rejectDeletionRequest(
                eq(companyId), 
                eq("operator001"), 
                eq(rejectionReason)))
                .thenReturn(mockResponse);

        // === Act（実行フェーズ）===
        
        ResponseEntity<DeletionRequestResponse> response = 
                partiCompanyController.rejectDeletionRequest(authentication, companyId, request);

        // === Assert（検証フェーズ）===
        
        // レスポンスがnullでないことを確認
        assertThat(response).isNotNull();

        // HTTPステータスが404 NOT_FOUNDであることを確認
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // レスポンスボディを取得
        DeletionRequestResponse body = response.getBody();
        assertThat(body).isNotNull();

        // レスポンスボディのステータスコードがNOT_FOUNDであることを確認
        assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);

        // レスポンスボディのメッセージを確認（削除申請が見つからない）
        assertThat(body.getMessage()).isEqualTo(ResponseConstants.MSG_DELETION_REQUEST_NOT_FOUND);

        // === Verify（呼び出し検証フェーズ）===
        
        // jwtUtil.extractUsernameが1回だけ呼ばれたことを確認
        verify(jwtUtil, times(1)).extractUsername(any());

        // companyDeletionService.rejectDeletionRequestが1回だけ呼ばれたことを確認
        verify(companyDeletionService, times(1)).rejectDeletionRequest(
                eq(companyId), 
                eq("operator001"), 
                eq(rejectionReason));
    }
}


