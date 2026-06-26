package com.kct.jcredit.core_backend.controller;

// AssertJ
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

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
import com.kct.jcredit.core_backend.dto.request.ReactivationRequest;
import com.kct.jcredit.core_backend.dto.request.RejectDeletionRequest;
import com.kct.jcredit.core_backend.dto.request.SuspendCompanyRequest;
import com.kct.jcredit.core_backend.dto.request.UpdatePartiCompanyRequest;
import com.kct.jcredit.core_backend.dto.response.DeletionRequestResponse;
import com.kct.jcredit.core_backend.dto.response.ReactivationResponse;
import com.kct.jcredit.core_backend.dto.response.SuspendCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.UpdatePartiCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.details.CompanyInfoResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.enums.SubscriptionStatus;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.service.CompanyDeletionService;
import com.kct.jcredit.core_backend.service.PartiCompanyRegisterService;
import com.kct.jcredit.core_backend.service.PartiCompanyService;
import com.kct.jcredit.core_backend.service.PaymentGatewayService;
import com.kct.jcredit.core_backend.service.SubscriptionService;
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
    private ParticipatingCompanyRepository companyRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private PaymentGatewayService paymentGatewayService;

    @Mock
    private PartiCompanyRegisterService registerService;

    @Mock
    private CompanyInfoResponse companyInfoResponse;


    @Mock
    private CompanyDeletionService companyDeletionService;

    @Mock
    private JwtUtil jwtUtil;

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
        req.setCompanyId(companyId);
        req.setPhoneNumber("070-1234-5678");
        req.setFax("03-1234-5679");
        req.setEmail("corporate@company.com");


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

        when(companyService.updateCompany(companyId, req))
            .thenReturn(res);

        //テスト実行
        ResponseEntity<UpdatePartiCompanyResponse> response = partiCompanyController.updateCompany(req);

        UpdatePartiCompanyResponse getBody = response.getBody();
        assertNotNull(getBody);
        assertEquals(req.getCompanyId(), getBody.getCompany().getCompanyId());
        assertEquals(ResponseConstants.OK, getBody.getStatusCode());


        CompanyInfoResponse company = getBody.getCompany();
        assertEquals(req.getCompanyId(), company.getCompanyId());
        assertEquals("株式会社サンプル", company.getNameJA());
        assertEquals("カブシキガイシャサンプル", company.getNameFurigana());
        assertEquals("Sumple Corporation", company.getNameEN());
        assertEquals(req.getEmail(), company.getEmail());
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

        
        UpdatePartiCompanyResponse res = new UpdatePartiCompanyResponse();
        res.setCompany(companyDetails);
        res.setStatusCode(ResponseConstants.NOT_FOUND);
        res.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);

        when(companyService.updateCompany("COMP00000001", req))
            .thenReturn(res);

        //テスト実行
        ResponseEntity<UpdatePartiCompanyResponse> response = partiCompanyController.updateCompany(req);

        UpdatePartiCompanyResponse getBody = response.getBody();
        assertNotNull(getBody);
        assertEquals(req.getCompanyId(), getBody.getCompany().getCompanyId());
        assertEquals("NOT_FOUND", getBody.getStatusCode());
        assertEquals("該当するサイトが見つかりませんでした。", res.getMessage());
    }

    //レスポンスのstatusCodeに応じた適切なHTTPステータスが返る(INTERNAL_SERVER_ERROR)
    @Test
    @DisplayName("レスポンスのstatusCodeに応じた適切なHTTPステータスが返る(INTERNAL_SERVER_ERROR)")
    void updateCompany_INTERNAL_SERVER_ERROR() {
        
        UpdatePartiCompanyResponse res = new UpdatePartiCompanyResponse();
        res.setCompany(companyDetails);
        res.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
        res.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);

        when(companyService.updateCompany("COMP00000001", req))
            .thenReturn(res);

        //テスト実行
        ResponseEntity<UpdatePartiCompanyResponse> response = partiCompanyController.updateCompany(req);

        UpdatePartiCompanyResponse getBody = response.getBody();
        assertNotNull(getBody);
        assertEquals(req.getCompanyId(), getBody.getCompany().getCompanyId());
        assertEquals("INTERNAL_SERVER_ERROR", getBody.getStatusCode());
        assertEquals("サイト情報の取得中にエラーが発生しました。", res.getMessage());
    }


    @Test
    @DisplayName("企業停止が正常に実行されること")
    void testSuspendCompany_Success() {
        // === Arrange（準備フェーズ）===
        
        // リクエストオブジェクトを作成
        SuspendCompanyRequest request = new SuspendCompanyRequest();
        request.setCompanyId(companyId);
        request.setReason(TEST_REASON);

        // テスト用の企業エンティティを作成
        ParticipatingCompany mockCompany = new ParticipatingCompany();
        mockCompany.setCompanyId(companyId);
        mockCompany.setNameJa("テスト株式会社");
        mockCompany.setSubscriptionStatus(SubscriptionStatus.ACTIVE); // アクティブ状態に設定

        // === モックの振る舞いを定義 ===
        
        // companyRepository.findByCompanyIdが呼ばれたらOptional.of(mockCompany)を返す
        when(companyRepository.findByCompanyId(companyId))
                .thenReturn(Optional.of(mockCompany));

        // subscriptionService.suspendCompanyManuallyは戻り値なし（voidメソッド）なので特に設定不要

        // === Act（実行フェーズ）===
        
        // コントローラーのsuspendCompanyメソッドを実行
        ResponseEntity<SuspendCompanyResponse> response = 
                partiCompanyController.suspendCompany(request);

        // === Assert（検証フェーズ）===
        
        // レスポンスがnullでないことを確認
        assertThat(response).isNotNull();

        // HTTPステータスが200 OKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // レスポンスボディを取得
        SuspendCompanyResponse body = response.getBody();
        assertThat(body).isNotNull();

        // レスポンスボディのステータスコードがOKであることを確認
        assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.OK);

        // メッセージの確認
        assertThat(body.getMessage()).isEqualTo(ResponseConstants.MSG_SUSPEND_SUCCESS);

        // 企業IDと企業名の確認
        assertThat(body.getCompanyId()).isEqualTo(companyId);
        assertThat(body.getCompanyName()).isEqualTo("テスト株式会社");

        // === Verify（呼び出し検証フェーズ）===
        
        // companyRepository.findByCompanyIdが1回だけ呼ばれたことを確認
        verify(companyRepository, times(1)).findByCompanyId(eq(companyId));

        // subscriptionService.suspendCompanyManuallyが1回だけ呼ばれたことを確認
        verify(subscriptionService, times(1)).suspendCompanyManually(
                eq(companyId), 
                eq(TEST_REASON)
        );
    }

    @Test
    @DisplayName("停止理由がnullの場合でも正常に処理されること")
    void testSuspendCompany_WithNullReason() {
        // === Arrange（準備フェーズ）===
        
        // リクエストオブジェクトを作成（理由なし）
        SuspendCompanyRequest request = new SuspendCompanyRequest();
        request.setCompanyId(companyId);
        request.setReason(null);

        // テスト用の企業エンティティを作成
        ParticipatingCompany mockCompany = new ParticipatingCompany();
        mockCompany.setCompanyId(companyId);
        mockCompany.setNameJa("テスト株式会社");
        mockCompany.setSubscriptionStatus(SubscriptionStatus.ACTIVE); // アクティブ状態に設定

        // === モックの振る舞いを定義 ===
        
        // companyRepository.findByCompanyIdが呼ばれたらOptional.of(mockCompany)を返す
        when(companyRepository.findByCompanyId(companyId))
                .thenReturn(Optional.of(mockCompany));

        // === Act（実行フェーズ）===
        
        // コントローラーのsuspendCompanyメソッドを実行
        ResponseEntity<SuspendCompanyResponse> response = 
                partiCompanyController.suspendCompany(request);

        // === Assert（検証フェーズ）===
        
        // レスポンスがnullでないことを確認
        assertThat(response).isNotNull();

        // HTTPステータスが200 OKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // レスポンスボディを取得
        SuspendCompanyResponse body = response.getBody();
        assertThat(body).isNotNull();

        // レスポンスボディのステータスコードがOKであることを確認
        assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.OK);

        // メッセージの確認
        assertThat(body.getMessage()).isEqualTo(ResponseConstants.MSG_SUSPEND_SUCCESS);

        // 企業IDと企業名の確認
        assertThat(body.getCompanyId()).isEqualTo(companyId);
        assertThat(body.getCompanyName()).isEqualTo("テスト株式会社");

        // === Verify（呼び出し検証フェーズ）===
        
        // companyRepository.findByCompanyIdが1回だけ呼ばれたことを確認
        verify(companyRepository, times(1)).findByCompanyId(eq(companyId));

        // subscriptionService.suspendCompanyManuallyが理由nullで呼ばれることを確認
        verify(subscriptionService, times(1)).suspendCompanyManually(
                eq(companyId), 
                isNull() // eq(null)よりもisNull()の方が明示的
        );
    }

@Test
@DisplayName("企業再開: 正常に再開できること")
void testReactivateCompany_Success() {
    // === Arrange（準備フェーズ）===
    
    // リクエストの作成
    ReactivationRequest request = new ReactivationRequest();
    request.setCompanyId(companyId);
    request.setPaymentMethod("CREDIT_CARD");

    // モックの企業エンティティを作成（停止中の企業）
    ParticipatingCompany company = ParticipatingCompany.builder()
            .companyId(companyId)
            .nameJa("テスト企業株式会社")
            .subscriptionStatus(SubscriptionStatus.SUSPENDED) // 停止中
            .paymentMethod("CREDIT_CARD")
            .build();

    // 決済成功のモック結果を作成
    PaymentGatewayService.PaymentResult paymentResult = 
            new PaymentGatewayService.PaymentResult(true, "TXN-12345678", null);

    // === モックの振る舞いを定義 ===
    
    // リポジトリのモック設定: 企業が見つかる
    when(companyRepository.findByCompanyId(companyId))
            .thenReturn(Optional.of(company));

    // 決済ゲートウェイのモック設定: 決済が成功する
    when(paymentGatewayService.processPayment(
            eq(companyId), 
            eq("CREDIT_CARD"), 
            any(BigDecimal.class)))
            .thenReturn(paymentResult);

    // === Act（実行フェーズ）===
    
    ResponseEntity<ReactivationResponse> response = 
            partiCompanyController.reactivateCompany(request);

    // === Assert（検証フェーズ）===
    
    // レスポンスがnullでないことを確認
    assertThat(response).isNotNull();

    // HTTPステータスが200 OKであることを確認
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // レスポンスボディを取得
    ReactivationResponse body = response.getBody();
    assertThat(body).isNotNull();

    // レスポンスボディのステータスコードがOKであることを確認
    assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.OK);

    // レスポンスボディのメッセージを確認
    assertThat(body.getMessage()).isEqualTo(ResponseConstants.MSG_REACTIVATION_SUCCESS);

    // 企業IDが正しいことを確認
    assertThat(body.getCompanyId()).isEqualTo(companyId);

    // 企業名が正しいことを確認
    assertThat(body.getCompanyName()).isEqualTo("テスト企業株式会社");

    // アクティブ状態がtrueであることを確認
    assertThat(body.getIsActive()).isTrue();

    // 停止状態がfalseであることを確認
    assertThat(body.getIsSuspended()).isFalse();

    // === Verify（呼び出し検証フェーズ）===
    
    // companyRepository.findByCompanyIdが1回だけ呼ばれたことを確認
    verify(companyRepository, times(1)).findByCompanyId(eq(companyId));

    // paymentGatewayService.processPaymentが1回だけ呼ばれたことを確認
    verify(paymentGatewayService, times(1)).processPayment(
            eq(companyId), 
            eq("CREDIT_CARD"), 
            any(BigDecimal.class));

    // subscriptionService.reactivateCompanyWithPaymentが1回だけ呼ばれたことを確認
    verify(subscriptionService, times(1)).reactivateCompanyWithPayment(
            eq(company), 
            eq("CREDIT_CARD"), 
            eq(paymentResult));
}

@Test
@DisplayName("企業再開: 企業が見つからない場合")
void testReactivateCompany_CompanyNotFound() {
    // === Arrange（準備フェーズ）===
    
    // リクエストの作成
    ReactivationRequest request = new ReactivationRequest();
    request.setCompanyId(companyId);
    request.setPaymentMethod("CREDIT_CARD");

    // リポジトリのモック設定: 企業が見つからない
    when(companyRepository.findByCompanyId(companyId))
            .thenReturn(Optional.empty());

    // === Act（実行フェーズ）===
    ResponseEntity<ReactivationResponse> response = 
            partiCompanyController.reactivateCompany(request);

    // === Assert（検証フェーズ）===
    
    // レスポンスがnullでないことを確認
    assertThat(response).isNotNull();

    // HTTPステータスが400 BAD_REQUESTであることを確認
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    // レスポンスボディがnullでないことを確認
    assertThat(response.getBody()).isNotNull();

    // レスポンスボディのステータスコードがNOT_FOUNDであることを確認
    assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);

    // レスポンスボディのメッセージを確認
    assertThat(response.getBody().getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);

    // === Verify（呼び出し検証フェーズ）===
    
    // companyRepository.findByCompanyIdが1回だけ呼ばれたことを確認
    verify(companyRepository, times(1)).findByCompanyId(eq(companyId));

    // paymentGatewayServiceが呼ばれていないことを確認
    verify(paymentGatewayService, never()).processPayment(any(), any(), any());

    // subscriptionServiceが呼ばれていないことを確認
    verify(subscriptionService, never()).reactivateCompanyWithPayment(any(), any(), any());
}

@Test
@DisplayName("企業再開: 企業が停止状態でない場合")
void testReactivateCompany_CompanyNotSuspended() {
    // === Arrange（準備フェーズ）===

    System.out.println("Test companyId: " + companyId);
    System.out.println("Request companyId: " + req.getCompanyId());
    
    // リクエストの作成
    ReactivationRequest request = new ReactivationRequest();
    request.setCompanyId(companyId);
    request.setPaymentMethod("CREDIT_CARD");

    // モックの企業エンティティを作成（アクティブな企業）
    ParticipatingCompany company = ParticipatingCompany.builder()
            .companyId(companyId)
            .nameJa("テスト企業株式会社")
            .subscriptionStatus(SubscriptionStatus.ACTIVE)
            .paymentMethod("CREDIT_CARD")
            .build();

    // リポジトリのモック設定: 企業が見つかる
    when(companyRepository.findByCompanyId(companyId))
            .thenReturn(Optional.of(company));

    // === Act（実行フェーズ）===
    ResponseEntity<ReactivationResponse> response = 
            partiCompanyController.reactivateCompany(request);

    // === Assert（検証フェーズ）===
    
    // レスポンスがnullでないことを確認
    assertThat(response).isNotNull();

    // HTTPステータスが400 BAD_REQUESTであることを確認
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    // レスポンスボディがnullでないことを確認
    assertThat(response.getBody()).isNotNull();

    // レスポンスボディのステータスコードがBAD_REQUESTであることを確認
    assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);

    // レスポンスボディのメッセージを確認
    assertThat(response.getBody().getMessage()).isEqualTo(ResponseConstants.MSG_COMPANY_NOT_SUSPENDED);

    // === Verify（呼び出し検証フェーズ）===
    
    // companyRepository.findByCompanyIdが1回だけ呼ばれたことを確認
    verify(companyRepository, times(1)).findByCompanyId(eq(companyId));

    // paymentGatewayServiceが呼ばれていないことを確認
    verify(paymentGatewayService, never()).processPayment(any(), any(), any());

    // subscriptionServiceが呼ばれていないことを確認
    verify(subscriptionService, never()).reactivateCompanyWithPayment(any(), any(), any());

    System.out.println("Status Code: " + response.getStatusCode());
    System.out.println("Response Body: " + response.getBody());
}

@Test
@DisplayName("企業再開: 決済が失敗した場合")
void testReactivateCompany_PaymentFailed() {
    // === Arrange（準備フェーズ）===
    
    // リクエストの作成
    ReactivationRequest request = new ReactivationRequest();
    request.setCompanyId(companyId);
    request.setPaymentMethod("CREDIT_CARD");

    // モックの企業エンティティを作成（停止中の企業）
    ParticipatingCompany company = ParticipatingCompany.builder()
            .companyId(companyId)
            .nameJa("テスト企業株式会社")
            .subscriptionStatus(SubscriptionStatus.SUSPENDED)
            .paymentMethod("CREDIT_CARD")
            .build();

    // 決済失敗のモック結果を作成
    PaymentGatewayService.PaymentResult paymentResult = 
            new PaymentGatewayService.PaymentResult(false, null, "Payment declined by gateway");

    // リポジトリのモック設定: 企業が見つかる
    when(companyRepository.findByCompanyId(companyId))
            .thenReturn(Optional.of(company));

    // 決済ゲートウェイのモック設定: 決済が失敗する
    when(paymentGatewayService.processPayment(
            eq(companyId), 
            eq("CREDIT_CARD"), 
            any(BigDecimal.class)))
            .thenReturn(paymentResult);

    // === Act（実行フェーズ）===
    ResponseEntity<ReactivationResponse> response = 
            partiCompanyController.reactivateCompany(request);

    // === Assert（検証フェーズ）===
    
    // レスポンスがnullでないことを確認
    assertThat(response).isNotNull();

    // HTTPステータスが402 PAYMENT_REQUIREDであることを確認
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);

    // レスポンスボディがnullでないことを確認
    assertThat(response.getBody()).isNotNull();

    // レスポンスボディのステータスコードがBAD_REQUESTであることを確認
    assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);

    // レスポンスボディのメッセージを確認
    assertThat(response.getBody().getMessage())
            .isEqualTo(ResponseConstants.MSG_PAYMENT_FAILED + "Payment declined by gateway");

    // 企業IDが正しいことを確認
    assertThat(response.getBody().getCompanyId()).isEqualTo(companyId);

    // 企業名が正しいことを確認
    assertThat(response.getBody().getCompanyName()).isEqualTo("テスト企業株式会社");

    // アクティブ状態がfalseであることを確認
    assertThat(response.getBody().getIsActive()).isFalse();

    // 停止状態がtrueであることを確認
    assertThat(response.getBody().getIsSuspended()).isTrue();

    // === Verify（呼び出し検証フェーズ）===
    
    // companyRepository.findByCompanyIdが1回だけ呼ばれたことを確認
    verify(companyRepository, times(1)).findByCompanyId(eq(companyId));

    // paymentGatewayService.processPaymentが1回だけ呼ばれたことを確認
    verify(paymentGatewayService, times(1)).processPayment(
            eq(companyId), 
            eq("CREDIT_CARD"), 
            any(BigDecimal.class));

    // subscriptionServiceが呼ばれていないことを確認
    verify(subscriptionService, never()).reactivateCompanyWithPayment(any(), any(), any());
}
    @Test
    @DisplayName("企業再開: 決済方法がnullの場合、企業の決済方法を使用する")
    void testReactivateCompany_PaymentMethodFromCompany() {
        // === Arrange（準備フェーズ）===
        
        // リクエストの作成（決済方法なし）
        ReactivationRequest request = new ReactivationRequest();
        request.setCompanyId(companyId);
        request.setPaymentMethod(null); // 決済方法を指定しない

        // モックの企業エンティティを作成（停止中の企業、既存の決済方法あり）
        ParticipatingCompany company = ParticipatingCompany.builder()
                .companyId(companyId)
                .nameJa("テスト企業株式会社")
                .subscriptionStatus(SubscriptionStatus.SUSPENDED) // 停止中
                .paymentMethod("BANK_TRANSFER") // 企業に登録済みの決済方法
                .build();

        // 決済成功のモック結果を作成
        PaymentGatewayService.PaymentResult paymentResult = 
                new PaymentGatewayService.PaymentResult(true, "TXN-87654321", null);

        // === モックの振る舞いを定義 ===
        
        // リポジトリのモック設定: 企業が見つかる
        when(companyRepository.findByCompanyId(companyId))
                .thenReturn(Optional.of(company));

        // 決済ゲートウェイのモック設定: 企業の決済方法で決済が成功する
        when(paymentGatewayService.processPayment(
                eq(companyId), 
                eq("BANK_TRANSFER"), // 企業の決済方法が使用される
                any(BigDecimal.class)))
                .thenReturn(paymentResult);

        // === Act（実行フェーズ）===
        
        ResponseEntity<ReactivationResponse> response = 
                partiCompanyController.reactivateCompany(request);

        // === デバッグ出力 ===
        System.out.println("===== デバッグ情報 =====");
        System.out.println("レスポンスコード: " + response.getStatusCode());
        System.out.println("レスポンスボディ: " + response.getBody());
        if (response.getBody() != null) {
                System.out.println("ステータスコード: " + response.getBody().getStatusCode());
                System.out.println("メッセージ: " + response.getBody().getMessage());
        }
        System.out.println("=======================");

        // === Assert（検証フェーズ）===
        
        // レスポンスがnullでないことを確認
        assertThat(response).isNotNull();

        // HTTPステータスが200 OKであることを確認
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // レスポンスボディを取得
        ReactivationResponse body = response.getBody();
        assertThat(body).isNotNull();

        // レスポンスボディのステータスコードがOKであることを確認
        assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.OK);

        // レスポンスボディのメッセージを確認
        assertThat(body.getMessage()).isEqualTo(ResponseConstants.MSG_REACTIVATION_SUCCESS);

        // 企業IDと企業名が正しいことを確認
        assertThat(body.getCompanyId()).isEqualTo(companyId);
        assertThat(body.getCompanyName()).isEqualTo("テスト企業株式会社");

        // === Verify（呼び出し検証フェーズ）===
        
        // companyRepository.findByCompanyIdが1回だけ呼ばれたことを確認
        verify(companyRepository, times(1)).findByCompanyId(eq(companyId));

        // paymentGatewayService.processPaymentが企業の決済方法（BANK_TRANSFER）で呼ばれたことを確認
        verify(paymentGatewayService, times(1)).processPayment(
                eq(companyId), 
                eq("BANK_TRANSFER"), // 企業に登録されている決済方法が使われる
                any(BigDecimal.class));

        // subscriptionService.reactivateCompanyWithPaymentが1回だけ呼ばれたことを確認
        verify(subscriptionService, times(1)).reactivateCompanyWithPayment(
                eq(company), 
                eq("BANK_TRANSFER"), // 企業の決済方法が渡される
                eq(paymentResult));
    }
@Test
@DisplayName("企業再開: 決済方法と企業の決済方法が両方nullの場合、デフォルトでCREDIT_CARDを使用する")
void testReactivateCompany_PaymentMethodDefault() {
    // === Arrange（準備フェーズ）===
    
    // リクエストの作成（決済方法なし）
    ReactivationRequest request = new ReactivationRequest();
    request.setCompanyId(companyId);
    request.setPaymentMethod(null); // リクエストに決済方法なし

    // モックの企業エンティティを作成（停止中の企業、決済方法なし）
    ParticipatingCompany company = ParticipatingCompany.builder()
            .companyId(companyId)
            .nameJa("テスト企業株式会社")
            .subscriptionStatus(SubscriptionStatus.SUSPENDED) // 停止中
            .paymentMethod(null) // 企業にも決済方法が登録されていない
            .build();

    // 決済成功のモック結果を作成
    PaymentGatewayService.PaymentResult paymentResult = 
            new PaymentGatewayService.PaymentResult(true, "TXN-11223344", null);

    // === モックの振る舞いを定義 ===
    
    // リポジトリのモック設定: 企業が見つかる
    when(companyRepository.findByCompanyId(companyId))
            .thenReturn(Optional.of(company));

    // 決済ゲートウェイのモック設定: デフォルトの決済方法（CREDIT_CARD）で決済が成功する
    when(paymentGatewayService.processPayment(
            eq(companyId), 
            eq("CREDIT_CARD"), // デフォルトの決済方法
            any(BigDecimal.class)))
            .thenReturn(paymentResult);

    // === Act（実行フェーズ）===
    
    ResponseEntity<ReactivationResponse> response = 
            partiCompanyController.reactivateCompany(request);

    // === Assert（検証フェーズ）===
    
    // レスポンスがnullでないことを確認
    assertThat(response).isNotNull();

    // HTTPステータスが200 OKであることを確認
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    // レスポンスボディを取得
    ReactivationResponse body = response.getBody();
    assertThat(body).isNotNull();

    // レスポンスボディのステータスコードがOKであることを確認
    assertThat(body.getStatusCode()).isEqualTo(ResponseConstants.OK);

    // レスポンスボディのメッセージを確認
    assertThat(body.getMessage()).isEqualTo(ResponseConstants.MSG_REACTIVATION_SUCCESS);

    // 企業IDと企業名が正しいことを確認
    assertThat(body.getCompanyId()).isEqualTo(companyId);
    assertThat(body.getCompanyName()).isEqualTo("テスト企業株式会社");

    // === Verify（呼び出し検証フェーズ）===
    
    // companyRepository.findByCompanyIdが1回だけ呼ばれたことを確認
    verify(companyRepository, times(1)).findByCompanyId(eq(companyId));

    // paymentGatewayService.processPaymentがデフォルトの決済方法（CREDIT_CARD）で呼ばれたことを確認
    verify(paymentGatewayService, times(1)).processPayment(
            eq(companyId), 
            eq("CREDIT_CARD"), // デフォルト値が使用される
            any(BigDecimal.class));

    // subscriptionService.reactivateCompanyWithPaymentが1回だけ呼ばれたことを確認
    verify(subscriptionService, times(1)).reactivateCompanyWithPayment(
            eq(company), 
            eq("CREDIT_CARD"), // デフォルト値が渡される
            eq(paymentResult));
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


