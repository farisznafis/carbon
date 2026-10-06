package com.kct.jcredit.core_backend.controller.PartiCompanyControllerTest;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.UpdatePartiCompanyRequest;
import com.kct.jcredit.core_backend.dto.response.UpdatePartiCompanyResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.service.impl.PartiCompanyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PartiCompanyServiceImpl#updateCompany tests.
 */
class PartiCompanyServiceTest {
    private static final String COMPANY_ID = "PC12345678";
    private static final int COMPANY_DATABASE_ID = 5678;

    private ParticipatingCompanyRepository participatingCompanyRepository;
    private PartiCompanyServiceImpl service;

    @BeforeEach
    void setUp() {
        participatingCompanyRepository = mock(ParticipatingCompanyRepository.class);
        service = new PartiCompanyServiceImpl(participatingCompanyRepository);
    }

    private ParticipatingCompany company(int id, String corpNumber) {
        ParticipatingCompany company = new ParticipatingCompany();
        company.setId(id);
        company.setCompanyId(COMPANY_ID);
        company.setNameJa("旧法人名");
        company.setNameFurigana("キュウホウジンメイ");
        company.setNameEn("Old Corporation");
        company.setEmail("sample@company.com");
        company.setPhoneNumber("0311111111");
        company.setFax("0311111112");
        company.setCorpNumber(corpNumber);
        company.setPostalCode("1000001");
        company.setPrefecture("東京都");
        company.setMunicipality("千代田区");
        company.setBlocknum("旧番地");
        company.setBuildingName("旧ビル");
        return company;
    }

    private UpdatePartiCompanyRequest validRequest() {
        UpdatePartiCompanyRequest request = new UpdatePartiCompanyRequest();
        request.setNameJa("株式会社サンプル");
        request.setNameFurigana("カブシキガイシャサンプル");
        request.setNameEn("Sample Corporation");
        request.setPhoneNumber("0312345678");
        request.setCorpNumber("1234567890123");
        request.setPostalCode("100-0001");
        request.setPrefecture("東京都");
        request.setMunicipality("千代田区");
        request.setBlocknum("千代田1-1");
        request.setBuildingName("サンプルビル10階");
        request.setFax("0312345670");
        return request;
    }

    @Nested
    @DisplayName("PATCH /api/company のテスト")
    class UpdateCompanyTest {

        @Test
        @DisplayName("UT-PCS-001: companyIdに対応する会社が存在しない場合、NOT_FOUNDを返す")
        void updateCompany_companyNotFound_returnsNotFound() {
            UpdatePartiCompanyRequest request = validRequest();
            when(participatingCompanyRepository.findByCompanyIdForUpdate("PC-UNKNOWN"))
                    .thenReturn(Optional.empty());

            UpdatePartiCompanyResponse response =
                    service.updateCompany("PC-UNKNOWN", request);

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_COMPANY_NOT_FOUND);
            assertThat(response.getCompany()).isNull();
            verify(participatingCompanyRepository).findByCompanyIdForUpdate("PC-UNKNOWN");
            verify(participatingCompanyRepository, never()).findByCorpNumber(any());
            verify(participatingCompanyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UT-PCS-002: 必須フィールドが空文字の場合、VALIDATION_ERRORを返し各フィールドのエラーメッセージが設定される")
        void updateCompany_requiredFieldsBlank_returnsValidationError() {
            UpdatePartiCompanyRequest request = validRequest();
            request.setNameJa("");
            request.setNameFurigana("");
            request.setPostalCode("");
            when(participatingCompanyRepository.findByCompanyIdForUpdate(COMPANY_ID))
                    .thenReturn(Optional.of(company(COMPANY_DATABASE_ID, "1234567890123")));

            UpdatePartiCompanyResponse response = service.updateCompany(COMPANY_ID, request);

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.VALIDATION_ERROR);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_VALIDATION_ERROR);
            assertThat(response.getErrors()).containsExactlyInAnyOrderEntriesOf(
                    java.util.Map.of(
                            "nameJa", "法人名を入力してください。",
                            "nameFurigana", "法人名カナを入力してください。",
                            "postalCode", "郵便番号を入力してください。"));
            verify(participatingCompanyRepository, never()).findByCorpNumber(any());
            verify(participatingCompanyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UT-PCS-003: 入力された法人番号が自分以外の会社で既に使用されている場合、CONFLICTを返す")
        void updateCompany_corpNumberUsedByAnotherCompany_returnsConflict() {
            UpdatePartiCompanyRequest request = validRequest();
            when(participatingCompanyRepository.findByCompanyIdForUpdate(COMPANY_ID))
                    .thenReturn(Optional.of(company(COMPANY_DATABASE_ID, "1234567890123")));
            when(participatingCompanyRepository.findByCorpNumber(request.getCorpNumber()))
                    .thenReturn(Optional.of(company(9999, request.getCorpNumber())));

            UpdatePartiCompanyResponse response = service.updateCompany(COMPANY_ID, request);

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.CONFLICT);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_CORP_NUMBER_ALREADY_USED);
            assertThat(response.getCompany()).isNull();
            verify(participatingCompanyRepository).findByCorpNumber(request.getCorpNumber());
            verify(participatingCompanyRepository, never()).save(any());
        }

        @Test
        @DisplayName("UT-PCS-004: 法人番号が自分自身の既存値と一致する場合、各フィールドが更新されOKを返す")
        void updateCompany_corpNumberBelongsToSelf_updatesFieldsAndReturnsOk() {
            UpdatePartiCompanyRequest request = validRequest();
            ParticipatingCompany existing = company(COMPANY_DATABASE_ID, request.getCorpNumber());
            when(participatingCompanyRepository.findByCompanyIdForUpdate(COMPANY_ID))
                    .thenReturn(Optional.of(existing));
            when(participatingCompanyRepository.findByCorpNumber(request.getCorpNumber()))
                    .thenReturn(Optional.of(existing));
            when(participatingCompanyRepository.save(any(ParticipatingCompany.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UpdatePartiCompanyResponse response = service.updateCompany(COMPANY_ID, request);

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getCompany()).isNotNull();
            assertThat(response.getCompany().getNameJA()).isEqualTo("株式会社サンプル");
            assertThat(response.getCompany().getNameFurigana()).isEqualTo("カブシキガイシャサンプル");
            assertThat(response.getCompany().getNameEN()).isEqualTo("Sample Corporation");
            assertThat(response.getCompany().getPhoneNumber()).isEqualTo("0312345678");
            assertThat(response.getCompany().getFax()).isEqualTo("0312345670");
            assertThat(response.getCompany().getCorpNumber()).isEqualTo(request.getCorpNumber());
            assertThat(response.getCompany().getPostalCode()).isEqualTo("100-0001");
            assertThat(response.getCompany().getPrefecture()).isEqualTo("東京都");
            assertThat(response.getCompany().getMunicipality()).isEqualTo("千代田区");
            assertThat(response.getCompany().getBlocknum()).isEqualTo("千代田1-1");
            assertThat(response.getCompany().getBuildingName()).isEqualTo("サンプルビル10階");
            assertThat(response.getCompany().getAddress())
                    .isEqualTo("東京都 千代田区 千代田1-1 サンプルビル10階");
            ArgumentCaptor<ParticipatingCompany> companyCaptor =
                    ArgumentCaptor.forClass(ParticipatingCompany.class);
            verify(participatingCompanyRepository).save(companyCaptor.capture());
            assertThat(companyCaptor.getValue()).isSameAs(existing);
            assertThat(existing.getNameJa()).isEqualTo("株式会社サンプル");
            assertThat(existing.getCorpNumber()).isEqualTo(request.getCorpNumber());
        }

        @Test
        @DisplayName("UT-PCS-005: save実行時に例外がthrowされた場合、INTERNAL_SERVER_ERRORを返す")
        void updateCompany_saveThrowsException_returnsInternalServerError() {
            UpdatePartiCompanyRequest request = validRequest();
            when(participatingCompanyRepository.findByCompanyIdForUpdate(COMPANY_ID))
                    .thenReturn(Optional.of(company(COMPANY_DATABASE_ID, request.getCorpNumber())));
            when(participatingCompanyRepository.findByCorpNumber(request.getCorpNumber()))
                    .thenReturn(Optional.empty());
            when(participatingCompanyRepository.save(any(ParticipatingCompany.class)))
                    .thenThrow(new RuntimeException("db error"));

            UpdatePartiCompanyResponse response = service.updateCompany(COMPANY_ID, request);

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
            assertThat(response.getCompany()).isNull();
            verify(participatingCompanyRepository).save(any(ParticipatingCompany.class));
        }
    }
}
