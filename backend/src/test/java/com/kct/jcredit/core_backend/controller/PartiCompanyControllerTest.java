package com.kct.jcredit.core_backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.SuspendCompanyRequest;
import com.kct.jcredit.core_backend.dto.request.UpdatePartiCompanyRequest;
import com.kct.jcredit.core_backend.dto.response.UpdatePartiCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.details.CompanyInfoResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.service.PartiCompanyRegisterService;
import com.kct.jcredit.core_backend.service.PartiCompanyService;
import com.kct.jcredit.core_backend.service.PaymentGatewayService;
import com.kct.jcredit.core_backend.service.SubscriptionService;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartiCompanyController 単体テスト")
public class PartiCompanyControllerTest {
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
    private ParticipatingCompanyRepository participatingCompanyRepository;

    @InjectMocks
    private PartiCompanyController partiCompanyController;

    @BeforeEach
    void setUp() {

        String companyId = "COMP00000001";

        //リクエスト作成
        UpdatePartiCompanyRequest req = new UpdatePartiCompanyRequest();
        req.setCompanyId(companyId);
        req.setPhoneNumber("070-1234-5678");
        req.setFax("03-1234-5679");
        req.setEmail("corporate@company.com");


        // ParticipatingCompany（Entity）を作成
        CompanyInfoResponse companyDetails = CompanyInfoResponse.builder()
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
        .isSuspended(false)
        .build();

    }

    //有効なリクエストで企業情報の更新が成功する（statusCode=200）
    @Test
    @DisplayName("有効なリクエストで企業情報の更新が成功する（statusCode=200）")
    void updateCompany_Success() {

        
        UpdatePartiCompanyResponse res = new UpdatePartiCompanyResponse();
        res.setCompany(companyDetails);
        res.setStatusCode(ResponseConstants.OK);

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
        assertEquals(false, company.getIsSuspended());  

        verify(companyService, times(1))
            .updateCompany(eq(companyId), eq(req));
    }

    //レスポンスのstatusCodeに応じた適切なHTTPステータスが返る(NOT_FOUND)
    @Test
    @DisplayName("レスポンスのstatusCodeに応じた適切なHTTPステータスが返る")
    void updateCompany_NOT_FOUND() {
        //リクエスト作成
        UpdatePartiCompanyRequest req = new UpdatePartiCompanyRequest();
        req.setCompanyId("COMP00000001");
        req.setPhoneNumber("070-1234-5678");
        req.setFax("03-1234-5679");
        req.setEmail("corporate@company.com");


        // ParticipatingCompany（Entity）を作成
        CompanyInfoResponse companyDetails = CompanyInfoResponse.builder()
        .id("1")
        .companyId("COMP00000001")
        .nameJA("株式会社サンプル")
        .nameFurigana("カブシキガイシャサンプル")
        .nameEN("Sumple Corporation")
        .email("corporate@company.com") //←リクエストの値に更新される
        .phoneNumber("070-1234-5678") //←リクエストの値に更新される
        .fax("03-1234-5679") //←リクエストの値に更新される
        .corpNumber("123467890123")
        .address("サンプルビル 3F")
        .isSuspended(false)
        .build();

        
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
        //リクエスト作成
        UpdatePartiCompanyRequest req = new UpdatePartiCompanyRequest();
        req.setCompanyId("COMP00000001");
        req.setPhoneNumber("070-1234-5678");
        req.setFax("03-1234-5679");
        req.setEmail("corporate@company.com");


        // ParticipatingCompany（Entity）を作成
        CompanyInfoResponse companyDetails = CompanyInfoResponse.builder()
        .id("1")
        .companyId("COMP00000001")
        .nameJA("株式会社サンプル")
        .nameFurigana("カブシキガイシャサンプル")
        .nameEN("Sumple Corporation")
        .email("corporate@company.com") //←リクエストの値に更新される
        .phoneNumber("070-1234-5678") //←リクエストの値に更新される
        .fax("03-1234-5679") //←リクエストの値に更新される
        .corpNumber("123467890123")
        .address("サンプルビル 3F")
        .isSuspended(false)
        .build();

        
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

    //==========================================================

    //有効なリクエストで企業情報の更新が成功する（statusCode=200）
    @Test
    @DisplayName("有効なリクエストで企業情報の更新が成功する（statusCode=200）")
    void signup_Success() {

        String companyId = "COMP00000001";

        //リクエスト作成
        SuspendCompanyRequest req = new SuspendCompanyRequest();
        req.setCompanyId(companyId);
        req.setReason("停止理由");

        // ParticipatingCompany（Entity）を作成
        CompanyInfoResponse companyDetails = CompanyInfoResponse.builder()
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
            .isSuspended(false)
            .build();

        
        UpdatePartiCompanyResponse res = new UpdatePartiCompanyResponse();
        res.setCompany(companyDetails);
        res.setStatusCode(ResponseConstants.OK);

        Optional<ParticipatingCompany> company = companyRepository.findByCompanyId(companyId);

        when(companyRepository.findByCompanyId(companyId))
            .thenReturn(Optional.of(company));

        when(subscriptionService.suspendCompanyManually(companyId, req.getReason()))
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
        assertEquals(false, company.getIsSuspended());  

        verify(companyService, times(1))
            .updateCompany(eq(companyId), eq(req));
    }

}
