package com.kct.jcredit.core_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.UpdatePartiCompanyRequest;
import com.kct.jcredit.core_backend.dto.response.UpdatePartiCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.details.CompanyInfoResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.service.impl.PartiCompanyServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PartiCompanyServiceTest {

    @Mock
    private ParticipatingCompanyRepository participatingCompanyRepository;

    @InjectMocks
    private PartiCompanyServiceImpl partiCompanyService;

    private ParticipatingCompany testCompany;
    private UpdatePartiCompanyRequest updateRequest;
    private static final String TEST_COMPANY_ID = "COMP001";

    @BeforeEach
    void setUp() {
        // テストデータの準備
        testCompany = new ParticipatingCompany();
        testCompany.setId(1);
        testCompany.setCompanyId(TEST_COMPANY_ID);
        testCompany.setNameJa("テスト株式会社");
        testCompany.setNameFurigana("テストカブシキガイシャ");
        testCompany.setNameEn("Test Corporation");
        testCompany.setEmail("test@example.com");
        testCompany.setPhoneNumber("03-1234-5678");
        testCompany.setFax("03-1234-5679");
        testCompany.setCorpNumber("1234567890123");
        testCompany.setPostalCode("1000001");
        testCompany.setPrefecture("東京都");
        testCompany.setMunicipality("渋谷区");
        testCompany.setBlocknum("1-1-1");
        testCompany.setBuildingName("テストビル");
        updateRequest = new UpdatePartiCompanyRequest();
    }

    @Test
    @DisplayName("正常系: 全フィールド更新")
    void updateCompany_Success_AllFields() {
        // Given
        updateRequest.setPhoneNumber("03-9999-9999");
        updateRequest.setFax("03-8888-8888");
        updateRequest.setNameJa("更新株式会社");
        updateRequest.setNameFurigana("コウシンカブシキガイシャ");
        updateRequest.setNameEn("Updated Corporation");
        updateRequest.setCorpNumber("9876543210123");
        updateRequest.setPostalCode("1500001");
        updateRequest.setPrefecture("東京都");
        updateRequest.setMunicipality("渋谷区");
        updateRequest.setBlocknum("2-2-2");
        updateRequest.setBuildingName("更新ビル");

        when(participatingCompanyRepository.findByCompanyIdForUpdate(TEST_COMPANY_ID))
                .thenReturn(Optional.of(testCompany));
        when(participatingCompanyRepository.save(any(ParticipatingCompany.class)))
                .thenReturn(testCompany);

        // When
        UpdatePartiCompanyResponse response = partiCompanyService.updateCompany(TEST_COMPANY_ID, updateRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getCompany()).isNotNull();
        assertThat(response.getCompany().getPhoneNumber()).isEqualTo("03-9999-9999");
        assertThat(response.getCompany().getFax()).isEqualTo("03-8888-8888");
        assertThat(response.getCompany().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getCompany().getNameJA()).isEqualTo("更新株式会社");
        assertThat(response.getCompany().getNameFurigana()).isEqualTo("コウシンカブシキガイシャ");
        assertThat(response.getCompany().getNameEN()).isEqualTo("Updated Corporation");
        assertThat(response.getCompany().getCorpNumber()).isEqualTo("9876543210123");
        assertThat(response.getCompany().getPostalCode()).isEqualTo("1500001");
        assertThat(response.getCompany().getAddress())
                .isEqualTo("東京都 渋谷区 2-2-2 更新ビル");

        verify(participatingCompanyRepository, times(1)).findByCompanyIdForUpdate(TEST_COMPANY_ID);
        verify(participatingCompanyRepository, times(1)).save(testCompany);
    }

    @Test
    @DisplayName("正常系: 電話番号のみ更新")
    void updateCompany_Success_PhoneNumberOnly() {
        // Given
        updateRequest.setPhoneNumber("03-9999-9999");

        when(participatingCompanyRepository.findByCompanyIdForUpdate(TEST_COMPANY_ID))
                .thenReturn(Optional.of(testCompany));
        when(participatingCompanyRepository.save(any(ParticipatingCompany.class)))
                .thenReturn(testCompany);

        // When
        UpdatePartiCompanyResponse response = partiCompanyService.updateCompany(TEST_COMPANY_ID, updateRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getCompany().getPhoneNumber()).isEqualTo("03-9999-9999");
        assertThat(response.getCompany().getFax()).isEqualTo("03-1234-5679"); // 変更なし
        assertThat(response.getCompany().getEmail()).isEqualTo("test@example.com"); // 変更なし

        verify(participatingCompanyRepository, times(1)).save(testCompany);
    }

    @Test
    @DisplayName("正常系: FAXのみ更新")
    void updateCompany_Success_FaxOnly() {
        // Given
        updateRequest.setFax("03-7777-7777");

        when(participatingCompanyRepository.findByCompanyIdForUpdate(TEST_COMPANY_ID))
                .thenReturn(Optional.of(testCompany));
        when(participatingCompanyRepository.save(any(ParticipatingCompany.class)))
                .thenReturn(testCompany);

        // When
        UpdatePartiCompanyResponse response = partiCompanyService.updateCompany(TEST_COMPANY_ID, updateRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getCompany().getFax()).isEqualTo("03-7777-7777");
        assertThat(response.getCompany().getPhoneNumber()).isEqualTo("03-1234-5678"); // 変更なし
        assertThat(response.getCompany().getEmail()).isEqualTo("test@example.com"); // 変更なし

        verify(participatingCompanyRepository, times(1)).save(testCompany);
    }

    @Test
    @DisplayName("正常系: メールアドレスは更新対象外")
    void updateCompany_EmailIsNotUpdated() {
        // Given
        updateRequest.setPhoneNumber("03-9999-9999");

        when(participatingCompanyRepository.findByCompanyIdForUpdate(TEST_COMPANY_ID))
                .thenReturn(Optional.of(testCompany));
        when(participatingCompanyRepository.save(any(ParticipatingCompany.class)))
                .thenReturn(testCompany);

        // When
        UpdatePartiCompanyResponse response = partiCompanyService.updateCompany(TEST_COMPANY_ID, updateRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getCompany().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getCompany().getPhoneNumber()).isEqualTo("03-9999-9999");
        assertThat(response.getCompany().getFax()).isEqualTo("03-1234-5679"); // 変更なし

        verify(participatingCompanyRepository, times(1)).save(testCompany);
    }

    @Test
    @DisplayName("正常系: 更新項目なし")
    void updateCompany_Success_NoUpdates() {
        // Given - すべてのフィールドがnull

        when(participatingCompanyRepository.findByCompanyIdForUpdate(TEST_COMPANY_ID))
                .thenReturn(Optional.of(testCompany));
        when(participatingCompanyRepository.save(any(ParticipatingCompany.class)))
                .thenReturn(testCompany);

        // When
        UpdatePartiCompanyResponse response = partiCompanyService.updateCompany(TEST_COMPANY_ID, updateRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getCompany().getPhoneNumber()).isEqualTo("03-1234-5678");
        assertThat(response.getCompany().getFax()).isEqualTo("03-1234-5679");
        assertThat(response.getCompany().getEmail()).isEqualTo("test@example.com");

        verify(participatingCompanyRepository, times(1)).save(testCompany);
    }

    
    @Test
    @DisplayName("異常系: 企業が見つからない")
    void updateCompany_CompanyNotFound() {
        // Given
        updateRequest.setPhoneNumber("03-9999-9999");

        when(participatingCompanyRepository.findByCompanyIdForUpdate(TEST_COMPANY_ID))
                .thenReturn(Optional.empty());

        // When
        UpdatePartiCompanyResponse response = partiCompanyService.updateCompany(TEST_COMPANY_ID, updateRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_COMPANY_NOT_FOUND);
        assertThat(response.getCompany()).isNull();

        verify(participatingCompanyRepository, times(1)).findByCompanyIdForUpdate(TEST_COMPANY_ID);
        verify(participatingCompanyRepository, never()).save(any());
    }

    @Test
    @DisplayName("異常系: DB保存時に例外発生")
    void updateCompany_DatabaseException() {
        // Given
        updateRequest.setPhoneNumber("03-9999-9999");

        when(participatingCompanyRepository.findByCompanyIdForUpdate(TEST_COMPANY_ID))
                .thenReturn(Optional.of(testCompany));
        when(participatingCompanyRepository.save(any(ParticipatingCompany.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When
        UpdatePartiCompanyResponse response = partiCompanyService.updateCompany(TEST_COMPANY_ID, updateRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
        assertThat(response.getCompany()).isNull();

        verify(participatingCompanyRepository, times(1)).findByCompanyIdForUpdate(TEST_COMPANY_ID);
        verify(participatingCompanyRepository, times(1)).save(testCompany);
    }

    @Test
    @DisplayName("異常系: 法人番号が重複している")
    void updateCompany_DuplicateCorpNumber() {
        ParticipatingCompany anotherCompany = new ParticipatingCompany();
        anotherCompany.setId(2);
        anotherCompany.setCorpNumber("9876543210123");
        updateRequest.setCorpNumber("9876543210123");

        when(participatingCompanyRepository.findByCompanyIdForUpdate(TEST_COMPANY_ID))
                .thenReturn(Optional.of(testCompany));
        when(participatingCompanyRepository.findByCorpNumber("9876543210123"))
                .thenReturn(Optional.of(anotherCompany));

        UpdatePartiCompanyResponse response = partiCompanyService.updateCompany(TEST_COMPANY_ID, updateRequest);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.CONFLICT);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_CORP_NUMBER_ALREADY_USED);
        verify(participatingCompanyRepository, never()).save(any());
    }

    @Test
    @DisplayName("正常系: 住所フィールドがnullの場合の住所文字列生成")
    void updateCompany_Success_WithNullAddressFields() {
        // Given
        testCompany.setPrefecture(null);
        testCompany.setMunicipality(null);
        testCompany.setBlocknum(null);
        testCompany.setBuildingName(null);

        updateRequest.setPhoneNumber("03-9999-9999");

        when(participatingCompanyRepository.findByCompanyIdForUpdate(TEST_COMPANY_ID))
                .thenReturn(Optional.of(testCompany));
        when(participatingCompanyRepository.save(any(ParticipatingCompany.class)))
                .thenReturn(testCompany);

        // When
        UpdatePartiCompanyResponse response = partiCompanyService.updateCompany(TEST_COMPANY_ID, updateRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getCompany().getAddress()).isEqualTo("null null null null");
    }

    @Test
    @DisplayName("正常系: isSuspendedがtrueの企業の更新")
    void updateCompany_Success_SuspendedCompany() {
        // Given
        testCompany.setCompanyStatus(CompanyStatus.SUSPENDED);
        updateRequest.setPhoneNumber("03-9999-9999");

        when(participatingCompanyRepository.findByCompanyIdForUpdate(TEST_COMPANY_ID))
                .thenReturn(Optional.of(testCompany));
        when(participatingCompanyRepository.save(any(ParticipatingCompany.class)))
                .thenReturn(testCompany);

        // When
        UpdatePartiCompanyResponse response = partiCompanyService.updateCompany(TEST_COMPANY_ID, updateRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getCompany().getIsSuspended()).isTrue();
    }

    @Test
    @DisplayName("正常系: CompanyInfoResponseの全フィールド検証")
    void updateCompany_Success_VerifyAllResponseFields() {
        // Given
        updateRequest.setPhoneNumber("03-9999-9999");
        updateRequest.setFax("03-8888-8888");
        updateRequest.setPhoneNumber("03-9999-9999");

        when(participatingCompanyRepository.findByCompanyIdForUpdate(TEST_COMPANY_ID))
                .thenReturn(Optional.of(testCompany));
        when(participatingCompanyRepository.save(any(ParticipatingCompany.class)))
                .thenReturn(testCompany);

        // When
        UpdatePartiCompanyResponse response = partiCompanyService.updateCompany(TEST_COMPANY_ID, updateRequest);

        // Then
        CompanyInfoResponse companyInfo = response.getCompany();
        assertThat(companyInfo).isNotNull();
        assertThat(companyInfo.getId()).isEqualTo("1");
        assertThat(companyInfo.getNameJA()).isEqualTo("テスト株式会社");
        assertThat(companyInfo.getNameFurigana()).isEqualTo("テストカブシキガイシャ");
        assertThat(companyInfo.getNameEN()).isEqualTo("Test Corporation");
        assertThat(companyInfo.getCorpNumber()).isEqualTo("1234567890123");
        assertThat(companyInfo.getIsSuspended()).isFalse();
    }




    @Test
    @DisplayName("異常系: 企業が見つからない")
    void updateCompany_CompanyNotFound() {
        // Given
        updateRequest.setPhoneNumber("03-9999-9999");

        when(participatingCompanyRepository.findByCompanyId(TEST_COMPANY_ID))
                .thenReturn(Optional.empty());

        // When
        UpdatePartiCompanyResponse response = partiCompanyService.updateCompany(TEST_COMPANY_ID, updateRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
        assertThat(response.getCompany()).isNull();

        verify(participatingCompanyRepository, times(1)).findByCompanyId(TEST_COMPANY_ID);
        verify(participatingCompanyRepository, never()).save(any());
    }

    @Test
    @DisplayName("異常系: DB保存時に例外発生")
    void updateCompany_DatabaseException() {
        // Given
        updateRequest.setPhoneNumber("03-9999-9999");

        when(participatingCompanyRepository.findByCompanyId(TEST_COMPANY_ID))
                .thenReturn(Optional.of(testCompany));
        when(participatingCompanyRepository.save(any(ParticipatingCompany.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When
        UpdatePartiCompanyResponse response = partiCompanyService.updateCompany(TEST_COMPANY_ID, updateRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
        assertThat(response.getCompany()).isNull();

        verify(participatingCompanyRepository, times(1)).findByCompanyId(TEST_COMPANY_ID);
        verify(participatingCompanyRepository, times(1)).save(testCompany);
    }
}