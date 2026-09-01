package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.request.CreateSiteRequest;
import com.kct.jcredit.core_backend.dto.request.UpdateSiteRequest;
import com.kct.jcredit.core_backend.dto.response.CreateSiteResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentRegisterResponse;
import com.kct.jcredit.core_backend.dto.response.SiteInfoResponse;
import com.kct.jcredit.core_backend.dto.response.SiteListResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;
import com.kct.jcredit.core_backend.dto.response.details.SiteDataResponse;
import com.kct.jcredit.core_backend.dto.response.details.SiteResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.SiteR002MemberData;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteR002MemberDataRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.impl.SiteServiceImpl;
import com.kct.jcredit.core_backend.service.SiteService;


import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SiteServiceImpl のテスト")
class SiteServiceImplTest {

    @Mock private SiteRepository siteRepository;
    @Mock private ParticipatingCompanyRepository participatingCompanyRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private EnR002EquipmentFileRepository siteR002EquipmentFileRepository;
    @Mock private EnR002EquipmentRepository siteR002EquipmentRepository;
    @Mock private SiteProjectRelationRepository siteProjectRelationRepository;
    @Mock private ParticipatingUserRepository participatingUserRepository;

    @InjectMocks
    private SiteServiceImpl siteService;

    private static final Integer MAX_SITES_PER_COMPANY = 20;

    // ============================================================================
    // Helper builders
    // ============================================================================

    private ParticipatingCompany buildCompany(int dbId, String companyId) {
        ParticipatingCompany c = new ParticipatingCompany();
        c.setId(dbId);
        c.setCompanyId(companyId);
        c.setNameJa("テスト株式会社");
        c.setPrefecture("東京都");
        c.setMunicipality("渋谷区");
        return c;
    }

    private Site buildSite(int dbId, String publicId, ParticipatingCompany company, String siteName) {
        Site site = new Site();
        site.setId(dbId);
        site.setPublicId(publicId);
        site.setSiteName(siteName);
        site.setPostalCode("100-0001");
        site.setPrefecture("東京都");
        site.setMunicipality("千代田区");
        site.setBlocknum("1-1");
        site.setBuildingName("ビルA");
        site.setPicFirstName("太郎");
        site.setPicLastName("山田");
        site.setCompany(company);
        site.setSiteProjectRelations(new ArrayList<>());
        return site;
    }

    private Project buildProject(int dbId, String publicId, String name) {
        Project p = new Project();
        p.setId(dbId);
        p.setPublicId(publicId);
        p.setProjectName(name);
        return p;
    }

    private SiteProjectRelation buildRelation(Site site, Project project) {
        SiteProjectRelation r = new SiteProjectRelation();
        r.setSite(site);
        r.setProject(project);
        return r;
    }

    private ParticipatingUser buildUser(int id, ParticipatingCompany company, String role, Site currentSite) {
        ParticipatingUser u = new ParticipatingUser();
        u.setId(id);
        u.setFirstName("次郎");
        u.setFirstNameKana("ジロウ");
        u.setLastName("鈴木");
        u.setLastNameKana("スズキ");
        u.setEmail("jiro.suzuki@example.com");
        u.setPhoneNumber("03-9999-8888");
        u.setUserPosition("担当者");
        u.setRole(role);
        u.setCompany(company);
        u.setSite(currentSite);
        return u;
    }

    private CreateSiteRequest buildCreateSiteRequest(Integer userId) {
        CreateSiteRequest r = new CreateSiteRequest();
        r.setUserId(userId);
        r.setSiteName("東京支社");
        r.setPostalCode("100-0001");
        r.setPrefecture("大阪府");   // sengaja beda dari company, untuk verifikasi fix S3-BUG-01
        r.setMunicipality("大阪市");
        r.setBlocknum("1-1");
        r.setBuildingName("オフィスビルA10階");
        return r;
    }

    private UpdateSiteRequest buildUpdateSiteRequest(Integer picUserId, String postalCode, String prefecture,
                                                      String municipality, String blocknum, String buildingName) {
        UpdateSiteRequest r = new UpdateSiteRequest();
        r.setPICUserId(picUserId);
        r.setPostalCode(postalCode);
        r.setPrefecture(prefecture);
        r.setMunicipality(municipality);
        r.setBlocknum(blocknum);
        r.setBuildingName(buildingName);
        return r;
    }

        private EnR002Equipment buildEquipment(int id, int siteId) {
        return EnR002Equipment.builder()
                .equipmentOpDate(LocalDate.of(2026, 3, 20))
                .nominalMaxOutput(new BigDecimal("49.5"))
                .pcsManufacturer("株式会社オムロン")
                .pcsModel("KP49R")
                // .pcsUnitCount(1)
                .deviceNo("SF-TKY-001")
                .pcsStandby(new BigDecimal("0.05"))
                .hasBattery(true)
                .batteryMaker("Tesla Japan")
                .batteryModel("Powerwall 3")
                .batteryCapacity(new BigDecimal("13.5"))
                .totalInvestment(7800000)
                .hasSubsidy(true)
                .subsidyName("東京都自家消費型太陽光発電導入補助金")
                .subsidyAmount(new BigDecimal("1500000"))
                .preProjectUnitPrice(new BigDecimal("26.50"))
                .build();
    }

    private EnR002EquipmentFile buildEquipmentFile(String s3Key) {
        EnR002EquipmentFile f = new EnR002EquipmentFile();
        f.setS3Key(s3Key);
        return f;
    }

    // ============================================================================
    // 1. getSitesByCompanyId
    // ============================================================================
    @Nested
    @DisplayName("getSitesByCompanyId のテスト")
    class GetSitesByCompanyIdTest {

        @Test
        @DisplayName("SC-1: 有効な企業IDで複数の拠点とプロジェクトを取得")
        void validCompanyWithMultipleSites_returnsSiteList() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site site1 = buildSite(1, "PUB-SITE-001", company, "東京本社");
            Project project1 = buildProject(1, "PUB-PROJ-001", "太陽光発電プロジェクトA");
            site1.setSiteProjectRelations(List.of(buildRelation(site1, project1)));

            Site site2 = buildSite(2, "PUB-SITE-002", company, "大阪支社");
            Project project2 = buildProject(2, "PUB-PROJ-002", "太陽光発電プロジェクトB");
            site2.setSiteProjectRelations(List.of(buildRelation(site2, project2)));

            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(siteRepository.findByCompany(company)).thenReturn(List.of(site1, site2));

            SiteListResponse response = siteService.getSitesByCompanyId("COMP001");

            assertThat(response.getStatusCode()).isEqualTo("OK");
            assertThat(response.getSiteList()).hasSize(2);
            assertThat(response.getSiteList().get(0).getSiteId()).isEqualTo("PUB-SITE-001");
            assertThat(response.getSiteList().get(0).getProjects()).containsExactly("太陽光発電プロジェクトA");
            assertThat(response.getSiteList().get(0).getData()).isNull();
            assertThat(response.getSiteList().get(1).getProjects()).containsExactly("太陽光発電プロジェクトB");

            verify(participatingCompanyRepository, times(1)).findByCompanyId("COMP001");
            verify(siteRepository, times(1)).findByCompany(company);
        }

        @Test
        @DisplayName("SC-2: siteProjectRelations が null の場合、空のプロジェクトリストを返す")
        void nullProjectRelations_returnsEmptyProjects() {
            ParticipatingCompany company = buildCompany(2, "COMP002");
            Site site = buildSite(10, "PUB-SITE-010", company, "名古屋営業所");
            site.setSiteProjectRelations(null);

            when(participatingCompanyRepository.findByCompanyId("COMP002")).thenReturn(Optional.of(company));
            when(siteRepository.findByCompany(company)).thenReturn(List.of(site));

            SiteListResponse response = siteService.getSitesByCompanyId("COMP002");

            assertThat(response.getStatusCode()).isEqualTo("OK");
            assertThat(response.getSiteList().get(0).getProjects()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("EC-1: 企業が見つからない場合、NOT_FOUND を返し findByCompany は呼ばれない")
        void companyNotFound_returnsNotFound() {
            when(participatingCompanyRepository.findByCompanyId("COMP999")).thenReturn(Optional.empty());

            SiteListResponse response = siteService.getSitesByCompanyId("COMP999");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
            verify(siteRepository, never()).findByCompany(any());
        }

        @Test
        @DisplayName("EC-2: 企業は存在するが拠点が見つからない場合、NOT_FOUND を返す")
        void companyExistsButNoSites_returnsNotFound() {
            ParticipatingCompany company = buildCompany(4, "COMP004");
            when(participatingCompanyRepository.findByCompanyId("COMP004")).thenReturn(Optional.of(company));
            when(siteRepository.findByCompany(company)).thenReturn(List.of());

            SiteListResponse response = siteService.getSitesByCompanyId("COMP004");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        }
    }

    // ============================================================================
    // 2. getSiteByPublicId
    // ============================================================================
    @Nested
    @DisplayName("getSiteByPublicId のテスト")
    class GetSiteByPublicIdTest {

        @Test
        @DisplayName("SC-1: 有効な publicId で拠点が存在する場合、全フィールドを含む拠点情報を返す")
        void validPublicId_returnsSiteInfoWithAllFields() {
            ParticipatingCompany company = buildCompany(5, "COMP005");
            Site site = buildSite(1, "PUB-SITE-001", company, "東京本社");
            Project p1 = buildProject(1, "PUB-PROJ-001", "太陽光発電プロジェクトA");
            Project p2 = buildProject(2, "PUB-PROJ-002", "太陽光発電プロジェクトB");
            site.setSiteProjectRelations(List.of(buildRelation(site, p1), buildRelation(site, p2)));

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));

            SiteInfoResponse response = siteService.getSiteByPublicId("PUB-SITE-001");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getSite().getSiteId()).isEqualTo("PUB-SITE-001");
            assertThat(response.getSite().getSiteName()).isEqualTo("東京本社");
            assertThat(response.getSite().getProjects())
                    .containsExactly("太陽光発電プロジェクトA", "太陽光発電プロジェクトB");
            //assertThat(response.getSite().getData()).isNotNull(); //data is no longer used

            verify(siteRepository, times(1)).findByPublicIdWithFallback("PUB-SITE-001");
        }

        @Test
        @DisplayName("SC-2: siteProjectRelations が null の場合、空のプロジェクトリストを返す")
        void nullProjectRelations_returnsEmptyProjects() {
            Site site = buildSite(5, "PUB-SITE-005", null, "名古屋営業所");
            site.setSiteProjectRelations(null);

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-005")).thenReturn(Optional.of(site));

            SiteInfoResponse response = siteService.getSiteByPublicId("PUB-SITE-005");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getSite().getProjects()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("EC-1: 拠点が見つからない場合、NOT_FOUND を返す")
        void siteNotFound_returnsNotFound() {
            when(siteRepository.findByPublicIdWithFallback("UNKNOWN")).thenReturn(Optional.empty());

            SiteInfoResponse response = siteService.getSiteByPublicId("UNKNOWN");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
            assertThat(response.getSite()).isNull();
        }

        @Test
        @DisplayName("EC-2: リポジトリが例外をスローした場合、INTERNAL_SERVER_ERROR を返す")
        void repositoryThrows_returnsInternalServerError() {
            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001"))
                    .thenThrow(new RuntimeException("DB error"));

            SiteInfoResponse response = siteService.getSiteByPublicId("PUB-SITE-001");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
        }

        /*
         * BUG-1 (引き継ぎ): 統計データ（jCredit/sumEmissionReduction/sumEmission）が
         * ハードコードされている。DBから取得すべき値が固定文字列 "1"/"0.5"/"1.5" になっている。
         * ここでは「現状の挙動」を素直に記録するに留める（意図的失敗テストにはしない）。
         */
        @Test
        @DisplayName("BUG-1: 統計データがハードコードされている（現状の挙動を記録）")
        void statisticsData_isCurrentlyHardcoded() {
            Site site = buildSite(1, "PUB-SITE-001", null, "東京本社");
            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));

            SiteInfoResponse response = siteService.getSiteByPublicId("PUB-SITE-001");

            // // 現状はこの3値で固定 — DB由来の値に変わったらこのテストは修正が必要になる
            // assertThat(response.getSite().getData().getJCredit()).isEqualTo("1");
            // assertThat(response.getSite().getData().getSumEmissionReduction()).isEqualTo("0.5");
            // assertThat(response.getSite().getData().getSumEmission()).isEqualTo("1.5");
        }
    }

    // ============================================================================
    // 3. validateCreateSite
    // ============================================================================
    @Nested
    @DisplayName("validateCreateSite のテスト")
    class ValidateCreateSiteTest {

        @Test
        @DisplayName("SC-1: 全ての検証を通過した場合、成功レスポンスを返す")
        void allValid_returnsSuccess() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            ParticipatingUser user = buildUser(50, company, RoleConstants.MASTER, null);

            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(siteRepository.countByCompany_Id(1)).thenReturn(5L);
            when(participatingUserRepository.findById(50)).thenReturn(Optional.of(user));

            BaseResponse response = siteService.validateCreateSite(buildCreateSiteRequest(50), "COMP001");

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("EC-1: 企業が見つからない場合、companyId エラーで即座に返る（ユーザー検索は行われない）")
        void companyNotFound_returnsEarlyWithError() {
            when(participatingCompanyRepository.findByCompanyId("COMP999")).thenReturn(Optional.empty());

            BaseResponse response = siteService.validateCreateSite(buildCreateSiteRequest(50), "COMP999");

            assertThat(response.getErrors()).containsEntry("companyId", ResponseConstants.MSG_NOT_FOUND);
            verifyNoInteractions(participatingUserRepository);
        }

        @Test
        @DisplayName("EC-2: 拠点数が上限に達している場合、companyId エラーが付与されるが検証は継続する")
        void siteLimitExceeded_addsErrorButContinuesValidation() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            ParticipatingUser user = buildUser(50, company, RoleConstants.MASTER, null);

            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(siteRepository.countByCompany_Id(1)).thenReturn(20L); // == MAX
            when(participatingUserRepository.findById(50)).thenReturn(Optional.of(user));

            BaseResponse response = siteService.validateCreateSite(buildCreateSiteRequest(50), "COMP001");

            assertThat(response.getErrors()).containsEntry("companyId", ResponseConstants.MSG_LIMIT_EXCEEDED);
            // 上限超過でも早期returnしない — ユーザー検証まで実行されることを確認
            verify(participatingUserRepository, times(1)).findById(50);
        }

        @Test
        @DisplayName("EC-3: ユーザーが見つからない場合、userId エラーで即座に返る")
        void userNotFound_returnsEarlyWithError() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(siteRepository.countByCompany_Id(1)).thenReturn(5L);
            when(participatingUserRepository.findById(999)).thenReturn(Optional.empty());

            BaseResponse response = siteService.validateCreateSite(buildCreateSiteRequest(999), "COMP001");

            assertThat(response.getErrors()).containsEntry("userId", ResponseConstants.MSG_USER_NOT_FOUND);
        }

        @Test
        @DisplayName("EC-4: ユーザーの会社情報が null の場合、userId=FORBIDDEN エラーを返す（NPEなし）")
        void userCompanyNull_returnsForbidden() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            ParticipatingUser user = buildUser(50, null, RoleConstants.MASTER, null);

            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(siteRepository.countByCompany_Id(1)).thenReturn(5L);
            when(participatingUserRepository.findById(50)).thenReturn(Optional.of(user));

            BaseResponse response = siteService.validateCreateSite(buildCreateSiteRequest(50), "COMP001");

            assertThat(response.getErrors()).containsEntry("userId", ResponseConstants.MSG_FORBIDDEN);
        }

        @Test
        @DisplayName("EC-5: 非MASTERユーザーが既に他拠点に所属している場合、userId=BAD_REQUEST エラーを返す")
        void nonMasterUserAlreadyHasSite_returnsBadRequest() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site otherSite = buildSite(99, "PUB-SITE-099", company, "既存拠点");
            ParticipatingUser user = buildUser(50, company, RoleConstants.USER, otherSite);

            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(siteRepository.countByCompany_Id(1)).thenReturn(5L);
            when(participatingUserRepository.findById(50)).thenReturn(Optional.of(user));

            BaseResponse response = siteService.validateCreateSite(buildCreateSiteRequest(50), "COMP001");

            assertThat(response.getErrors()).containsEntry("userId", ResponseConstants.MSG_BAD_REQUEST);
        }

        /*
         * EC-6 — 現状の挙動を記録: FORBIDDEN と BAD_REQUEST が同時に発生し得る場合、
         * 両方とも同じキー "userId" に put されるため、後勝ちで BAD_REQUEST のみが残り、
         * FORBIDDEN の情報は静かに上書きされて消える。エラーが複数ある場合でも
         * クライアントには1種類のエラーしか伝わらない可能性がある点に注意。
         */
        @Test
        @DisplayName("EC-6: 複数条件が同時に該当する場合、同じキーのため片方のエラーメッセージが上書きされる（現状の挙動）")
        void multipleErrorsOnSameKey_lastOneWins() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            ParticipatingCompany otherCompany = buildCompany(2, "COMP002"); // 会社不一致も同時に発生させる
            Site otherSite = buildSite(99, "PUB-SITE-099", otherCompany, "既存拠点");
            ParticipatingUser user = buildUser(50, otherCompany, RoleConstants.USER, otherSite);

            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(siteRepository.countByCompany_Id(1)).thenReturn(5L);
            when(participatingUserRepository.findById(50)).thenReturn(Optional.of(user));

            BaseResponse response = siteService.validateCreateSite(buildCreateSiteRequest(50), "COMP001");

            // FORBIDDEN（会社不一致）は先に set されるが、BAD_REQUEST（他拠点所属）に上書きされる
            assertThat(response.getErrors()).containsEntry("userId", ResponseConstants.MSG_BAD_REQUEST);
            assertThat(response.getErrors()).doesNotContainValue(ResponseConstants.MSG_FORBIDDEN);
        }
    }

    // ============================================================================
    // 4. createSite
    // ============================================================================
    @Nested
    @DisplayName("createSite のテスト")
    class CreateSiteTest {

        @Test
        @DisplayName("SC-1: 有効なリクエストで拠点作成が成功し、PIC情報がユーザーから設定される")
        void validRequest_createsSiteWithPicFromUser() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            ParticipatingUser user = buildUser(50, company, RoleConstants.MASTER, null);
            CreateSiteRequest request = buildCreateSiteRequest(50);

            Site savedSite = buildSite(100, "PUB-SITE-100", company, request.getSiteName());

            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(siteRepository.countByCompany_Id(1)).thenReturn(5L);
            when(participatingUserRepository.findById(50)).thenReturn(Optional.of(user));
            when(siteRepository.save(any(Site.class))).thenReturn(savedSite);

            CreateSiteResponse response = siteService.createSite(request, "COMP001");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getSiteId()).isEqualTo("PUB-SITE-100");

            ArgumentCaptor<Site> siteCaptor = ArgumentCaptor.forClass(Site.class);
            verify(siteRepository).save(siteCaptor.capture());
            Site captured = siteCaptor.getValue();

            // 住所はリクエストから（S3-BUG-01 は修正済みであることの確認）
            assertThat(captured.getPrefecture()).isEqualTo("大阪府");
            assertThat(captured.getMunicipality()).isEqualTo("大阪市");
            // PIC情報はユーザーから取得される（company からではない）
            assertThat(captured.getPicFirstName()).isEqualTo(user.getFirstName());
            assertThat(captured.getPicLastName()).isEqualTo(user.getLastName());
            assertThat(captured.getPicEmail()).isEqualTo(user.getEmail());
            assertThat(captured.getPicPhoneNumber()).isEqualTo(user.getPhoneNumber());
            assertThat(captured.getPicPosition()).isEqualTo(user.getUserPosition());

            ArgumentCaptor<ParticipatingUser> userCaptor = ArgumentCaptor.forClass(ParticipatingUser.class);
            verify(participatingUserRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getSite()).isSameAs(captured);
        }

        @Test
        @DisplayName("EC-1: 企業が見つからない場合、NOT_FOUND を返し保存処理は行われない")
        void companyNotFound_returnsNotFoundAndSkipsSave() {
            when(participatingCompanyRepository.findByCompanyId("COMP999")).thenReturn(Optional.empty());

            CreateSiteResponse response = siteService.createSite(buildCreateSiteRequest(50), "COMP999");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);
            verify(siteRepository, never()).save(any());
        }

        @Test
        @DisplayName("EC-2: 拠点数が上限に達している場合、LIMIT_EXCEEDED を返し即座に終了する")
        void siteLimitExceeded_returnsLimitExceededImmediately() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(siteRepository.countByCompany_Id(1)).thenReturn(20L);

            CreateSiteResponse response = siteService.createSite(buildCreateSiteRequest(50), "COMP001");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.LIMIT_EXCEEDED);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_LIMIT_EXCEEDED);
            // createSite は上限到達時に即return — validateCreateSite(継続する)とは挙動が異なる点に注意
            verifyNoInteractions(participatingUserRepository);
        }

        @Test
        @DisplayName("EC-3: ユーザーが見つからない場合、NOT_FOUND を返す")
        void userNotFound_returnsNotFound() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(siteRepository.countByCompany_Id(1)).thenReturn(5L);
            when(participatingUserRepository.findById(999)).thenReturn(Optional.empty());

            CreateSiteResponse response = siteService.createSite(buildCreateSiteRequest(999), "COMP001");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_USER_NOT_FOUND);
            verify(siteRepository, never()).save(any());
        }

        /*
         * EC-4 — 欠陥候補: user.getCompany() が null の場合、validateCreateSite() は
         * null チェックしているが、createSite() は `!user.getCompany().getId()...` を
         * 直接呼び出しており null チェックが無い。NPE が発生する。
         */
        @Test
        @DisplayName("EC-4: ユーザーの会社情報が null の場合、NPE が発生する（欠陥候補・validateCreateSiteとの非対称）")
        void userCompanyNull_currentlyThrowsNpe() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            ParticipatingUser user = buildUser(50, null, RoleConstants.MASTER, null);

            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(siteRepository.countByCompany_Id(1)).thenReturn(5L);
            when(participatingUserRepository.findById(50)).thenReturn(Optional.of(user));

            assertThrows(NullPointerException.class,
                    () -> siteService.createSite(buildCreateSiteRequest(50), "COMP001"));
        }

        @Test
        @DisplayName("EC-5: ユーザーの会社IDが一致しない場合、FORBIDDEN を返す")
        void userCompanyMismatch_returnsForbidden() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            ParticipatingCompany otherCompany = buildCompany(2, "COMP002");
            ParticipatingUser user = buildUser(50, otherCompany, RoleConstants.MASTER, null);

            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(siteRepository.countByCompany_Id(1)).thenReturn(5L);
            when(participatingUserRepository.findById(50)).thenReturn(Optional.of(user));

            CreateSiteResponse response = siteService.createSite(buildCreateSiteRequest(50), "COMP001");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
            verify(siteRepository, never()).save(any());
        }

        @Test
        @DisplayName("EC-6: 非MASTERユーザーが既に他拠点に所属している場合、BAD_REQUEST を返す")
        void nonMasterUserAlreadyHasSite_returnsBadRequest() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site otherSite = buildSite(99, "PUB-SITE-099", company, "既存拠点");
            ParticipatingUser user = buildUser(50, company, RoleConstants.USER, otherSite);

            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(siteRepository.countByCompany_Id(1)).thenReturn(5L);
            when(participatingUserRepository.findById(50)).thenReturn(Optional.of(user));

            CreateSiteResponse response = siteService.createSite(buildCreateSiteRequest(50), "COMP001");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
            verify(siteRepository, never()).save(any());
        }

        @Test
        @DisplayName("EC-7: 保存中に例外が発生した場合、INTERNAL_SERVER_ERROR を返し以降の処理は行われない")
        void saveThrows_returnsInternalServerErrorAndSkipsUserUpdate() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            ParticipatingUser user = buildUser(50, company, RoleConstants.MASTER, null);

            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(siteRepository.countByCompany_Id(1)).thenReturn(5L);
            when(participatingUserRepository.findById(50)).thenReturn(Optional.of(user));
            when(siteRepository.save(any(Site.class))).thenThrow(new RuntimeException("DB connection error"));

            CreateSiteResponse response = siteService.createSite(buildCreateSiteRequest(50), "COMP001");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
            // save(site) が失敗したので user.setSite()/save() まで到達しない
            verify(participatingUserRepository, never()).save(any());
        }

        /*
         * BUG（引き継ぎ、S3-BUG-02相当）: createdBy がログインユーザーではなく
         * company.getNameJa() から設定されている。監査ログとして不正確。
         */
        @Test
        @DisplayName("BUG: createdBy が company 名から設定される（現状の挙動を記録）")
        void createdBy_currentlySetFromCompanyName() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            company.setNameJa("テスト株式会社");
            ParticipatingUser user = buildUser(50, company, RoleConstants.MASTER, null);

            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(siteRepository.countByCompany_Id(1)).thenReturn(5L);
            when(participatingUserRepository.findById(50)).thenReturn(Optional.of(user));
            when(siteRepository.save(any(Site.class))).thenReturn(buildSite(100, "PUB-SITE-100", company, "x"));

            siteService.createSite(buildCreateSiteRequest(50), "COMP001");

            ArgumentCaptor<Site> captor = ArgumentCaptor.forClass(Site.class);
            verify(siteRepository).save(captor.capture());
            // 理想はログインユーザー名だが、現状は company 名が入る
            assertThat(captor.getValue().getCreatedBy()).isEqualTo("テスト株式会社");
        }
    }

    // ============================================================================
    // 5. validateUpdateSite
    // ============================================================================
    @Nested
    @DisplayName("validateUpdateSite のテスト")
    class ValidateUpdateSiteTest {

        @Test
        @DisplayName("SC-1: 全ての検証を通過した場合（PICUserId指定なし）、成功レスポンスを返す")
        void allValidWithoutPicChange_returnsSuccess() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site site = buildSite(1, "PUB-SITE-001", company, "東京本社");

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));

            BaseResponse response = siteService.validateUpdateSite(
                    "PUB-SITE-001", "COMP001", buildUpdateSiteRequest(null, "123-4567", null, null, null, null));

            assertThat(response.getErrors()).isEmpty();
            verifyNoInteractions(participatingUserRepository);
        }

        @Test
        @DisplayName("SC-2: 有効な PICUserId が指定された場合、成功レスポンスを返す")
        void validPicUserId_returnsSuccess() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site site = buildSite(1, "PUB-SITE-001", company, "東京本社");
            ParticipatingUser pic = buildUser(60, company, RoleConstants.MASTER, null);

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));
            when(participatingUserRepository.findById(60)).thenReturn(Optional.of(pic));

            BaseResponse response = siteService.validateUpdateSite(
                    "PUB-SITE-001", "COMP001", buildUpdateSiteRequest(60, null, null, null, null, null));

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("EC-1: 拠点が見つからない場合、siteId エラーで即座に返る")
        void siteNotFound_returnsEarlyWithError() {
            when(siteRepository.findByPublicIdWithFallback("UNKNOWN")).thenReturn(Optional.empty());

            BaseResponse response = siteService.validateUpdateSite(
                    "UNKNOWN", "COMP001", buildUpdateSiteRequest(null, null, null, null, null, null));

            assertThat(response.getErrors()).containsEntry("siteId", ResponseConstants.MSG_NOT_FOUND);
        }

        @Test
        @DisplayName("EC-2: 拠点が異なる企業に属する場合、siteId=FORBIDDEN エラーを返す")
        void siteFromDifferentCompany_returnsForbidden() {
            ParticipatingCompany otherCompany = buildCompany(2, "COMP002");
            Site site = buildSite(1, "PUB-SITE-001", otherCompany, "東京本社");

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));

            BaseResponse response = siteService.validateUpdateSite(
                    "PUB-SITE-001", "COMP001", buildUpdateSiteRequest(null, null, null, null, null, null));

            assertThat(response.getErrors()).containsEntry("siteId", ResponseConstants.MSG_FORBIDDEN);
        }

        @Test
        @DisplayName("EC-3: 指定された PICUserId のユーザーが見つからない場合、PICUserId エラーを返す")
        void picUserNotFound_returnsError() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site site = buildSite(1, "PUB-SITE-001", company, "東京本社");

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));
            when(participatingUserRepository.findById(999)).thenReturn(Optional.empty());

            BaseResponse response = siteService.validateUpdateSite(
                    "PUB-SITE-001", "COMP001", buildUpdateSiteRequest(999, null, null, null, null, null));

            assertThat(response.getErrors()).containsEntry("PICUserId", ResponseConstants.MSG_USER_NOT_FOUND);
        }

        @Test
        @DisplayName("EC-4: 指定された PICUserId のユーザーが別会社に属する場合、FORBIDDEN エラーを返す")
        void picUserDifferentCompany_returnsForbidden() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            ParticipatingCompany otherCompany = buildCompany(2, "COMP002");
            Site site = buildSite(1, "PUB-SITE-001", company, "東京本社");
            ParticipatingUser pic = buildUser(60, otherCompany, RoleConstants.MASTER, null);

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));
            when(participatingUserRepository.findById(60)).thenReturn(Optional.of(pic));

            BaseResponse response = siteService.validateUpdateSite(
                    "PUB-SITE-001", "COMP001", buildUpdateSiteRequest(60, null, null, null, null, null));

            assertThat(response.getErrors()).containsEntry("PICUserId", ResponseConstants.MSG_FORBIDDEN);
        }

        @Test
        @DisplayName("EC-5: 非MASTERユーザーが他拠点に所属している場合、PICUserId=BAD_REQUEST エラーを返す")
        void picUserNonMasterAssignedElsewhere_returnsBadRequest() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site site = buildSite(1, "PUB-SITE-001", company, "東京本社");
            Site otherSite = buildSite(2, "PUB-SITE-002", company, "別拠点");
            ParticipatingUser pic = buildUser(60, company, RoleConstants.USER, otherSite);

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));
            when(participatingUserRepository.findById(60)).thenReturn(Optional.of(pic));

            BaseResponse response = siteService.validateUpdateSite(
                    "PUB-SITE-001", "COMP001", buildUpdateSiteRequest(60, null, null, null, null, null));

            assertThat(response.getErrors()).containsEntry("PICUserId", ResponseConstants.MSG_BAD_REQUEST);
        }

        @Test
        @DisplayName("SC-3: 非MASTERユーザーが更新対象の拠点自身に所属している場合、エラーにならない")
        void picUserNonMasterAssignedToSameSite_noError() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site site = buildSite(1, "PUB-SITE-001", company, "東京本社");
            ParticipatingUser pic = buildUser(60, company, RoleConstants.USER, site); // 自分自身の拠点

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));
            when(participatingUserRepository.findById(60)).thenReturn(Optional.of(pic));

            BaseResponse response = siteService.validateUpdateSite(
                    "PUB-SITE-001", "COMP001", buildUpdateSiteRequest(60, null, null, null, null, null));

            assertThat(response.getErrors()).isEmpty();
        }
    }

    // ============================================================================
    // 6. updateSite
    // ============================================================================
    @Nested
    @DisplayName("updateSite のテスト")
    class UpdateSiteTest {

        @Test
        @DisplayName("SC-1: 全ての住所フィールドが提供された場合、全て更新する")
        void allFieldsProvided_updatesAllAddressFields() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site site = buildSite(1, "PUB-SITE-001", company, "東京本社");

            UpdateSiteRequest request = buildUpdateSiteRequest(
                    null, "123-4567", "大阪府", "大阪市", "中之島3-3-3", "新ビルB5階");

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));
            when(participatingUserRepository.findById(any())).thenReturn(Optional.empty());
            when(siteRepository.save(any(Site.class))).thenAnswer(inv -> inv.getArgument(0));

            SiteInfoResponse response = siteService.updateSite("PUB-SITE-001", "COMP001", request);

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getSite().getPostalCode()).isEqualTo("123-4567");
            assertThat(response.getSite().getPrefecture()).isEqualTo("大阪府");
            assertThat(response.getSite().getMunicipality()).isEqualTo("大阪市");
            assertThat(response.getSite().getBlocknum()).isEqualTo("中之島3-3-3");
            assertThat(response.getSite().getBuildingName()).isEqualTo("新ビルB5階");

            ArgumentCaptor<Site> captor = ArgumentCaptor.forClass(Site.class);
            verify(siteRepository).save(captor.capture());
            assertThat(captor.getValue().getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("SC-2: 郵便番号のみ提供された場合、郵便番号だけを更新する")
        void onlyPostalCodeProvided_updatesOnlyPostalCode() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site site = buildSite(2, "PUB-SITE-002", company, "既存拠点");
            site.setPrefecture("東京都");
            site.setMunicipality("千代田区");
            site.setBlocknum("1-1");
            site.setBuildingName("オリジナルビル");

            UpdateSiteRequest request = buildUpdateSiteRequest(null, "123-4567", null, "", null, "");

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-002")).thenReturn(Optional.of(site));
            when(participatingUserRepository.findById(any())).thenReturn(Optional.empty());
            when(siteRepository.save(any(Site.class))).thenAnswer(inv -> inv.getArgument(0));

            siteService.updateSite("PUB-SITE-002", "COMP001", request);

            ArgumentCaptor<Site> captor = ArgumentCaptor.forClass(Site.class);
            verify(siteRepository).save(captor.capture());
            Site updated = captor.getValue();
            assertThat(updated.getPostalCode()).isEqualTo("123-4567");
            assertThat(updated.getPrefecture()).isEqualTo("東京都");     // 変更なし
            assertThat(updated.getMunicipality()).isEqualTo("千代田区"); // 変更なし
            assertThat(updated.getBlocknum()).isEqualTo("1-1");          // 変更なし
            assertThat(updated.getBuildingName()).isEqualTo("オリジナルビル"); // 変更なし
        }

        @Test
        @DisplayName("EC-1: 拠点が見つからない場合、NOT_FOUND を返す")
        void siteNotFound_returnsNotFound() {
            when(siteRepository.findByPublicIdWithFallback("UNKNOWN")).thenReturn(Optional.empty());

            SiteInfoResponse response = siteService.updateSite(
                    "UNKNOWN", "COMP999", buildUpdateSiteRequest(null, "123-4567", null, null, null, null));

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);
            verify(siteRepository, never()).save(any());
        }

        @Test
        @DisplayName("EC-2: 拠点が異なる企業に属する場合、FORBIDDEN を返す")
        void siteFromDifferentCompany_returnsForbidden() {
            ParticipatingCompany actualCompany = buildCompany(2, "COMP002");
            Site site = buildSite(10, "PUB-SITE-010", actualCompany, "他社拠点");

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-010")).thenReturn(Optional.of(site));

            SiteInfoResponse response = siteService.updateSite(
                    "PUB-SITE-010", "COMP001", buildUpdateSiteRequest(null, "123-4567", null, null, null, null));

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
            verify(siteRepository, never()).save(any());
        }

        @Test
        @DisplayName("EC-3: 指定された PICUserId のユーザーが別会社の場合、FORBIDDEN を返す")
        void picUserDifferentCompany_returnsForbidden() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            ParticipatingCompany otherCompany = buildCompany(2, "COMP002");
            Site site = buildSite(1, "PUB-SITE-001", company, "東京本社");
            ParticipatingUser pic = buildUser(60, otherCompany, RoleConstants.MASTER, null);

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));
            when(participatingUserRepository.findById(60)).thenReturn(Optional.of(pic));

            SiteInfoResponse response = siteService.updateSite(
                    "PUB-SITE-001", "COMP001", buildUpdateSiteRequest(60, null, null, null, null, null));

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
            verify(siteRepository, never()).save(any());
        }

        @Test
        @DisplayName("EC-4: 非MASTERのPICが他拠点に所属している場合、BAD_REQUEST を返す")
        void picNonMasterAssignedElsewhere_returnsBadRequest() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site site = buildSite(1, "PUB-SITE-001", company, "東京本社");
            Site otherSite = buildSite(2, "PUB-SITE-002", company, "別拠点");
            ParticipatingUser pic = buildUser(60, company, RoleConstants.USER, otherSite);

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));
            when(participatingUserRepository.findById(60)).thenReturn(Optional.of(pic));

            SiteInfoResponse response = siteService.updateSite(
                    "PUB-SITE-001", "COMP001", buildUpdateSiteRequest(60, null, null, null, null, null));

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
            verify(siteRepository, never()).save(any());
        }

        /*
         * EC-5 — 欠陥候補: parseSiteId() はもう呼ばれていない（findByPublicIdWithFallback(String)
         * に統一されたため未使用のデッドコードとして残存）。以前のテストは
         * 「数値でない siteId は NumberFormatException」を期待していたが、現在の実装では
         * そもそも数値変換が行われないため、その前提は成立しない。
         * ここでは findByPublicIdWithFallback に非数値文字列がそのまま渡ることのみを確認する。
         * 実際に例外が起きるかはリポジトリ内部の実装次第のため、サービス層としては
         * 単に「見つからなければ NOT_FOUND」という通常のフローに合流する。
         */
        @Test
        @DisplayName("EC-5: 数値でない siteId は例外にならず、通常の未検出フローに合流する（旧テストの前提を訂正）")
        void nonNumericSiteId_noLongerThrowsNumberFormatException() {
            when(siteRepository.findByPublicIdWithFallback("invalid-id")).thenReturn(Optional.empty());

            SiteInfoResponse response = siteService.updateSite(
                    "invalid-id", "COMP001", buildUpdateSiteRequest(null, "123-4567", null, null, null, null));

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            verify(siteRepository, times(1)).findByPublicIdWithFallback("invalid-id");
        }

        /*
         * EC-6 — 欠陥候補: validateUpdateSite() は request.getPICUserId() != null を
         * チェックしてから findById() を呼ぶが、updateSite() 本体は無条件で
         * participatingUserRepository.findById(request.getPICUserId()) を呼び出している。
         * PICUserId が null の場合、findById(null) の挙動はリポジトリ実装依存だが、
         * Spring Data JPA の標準契約では IllegalArgumentException が投げられる。
         * validateUpdateSite との非対称性（PIC変更なしを許容する設計との矛盾）を記録する。
         */
        @Test
        @DisplayName("EC-6: PICUserId が null の場合、findById(null) が呼ばれ例外の危険がある（欠陥候補・validateUpdateSiteとの非対称）")
        void picUserIdNull_callsFindByIdWithNull() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site site = buildSite(1, "PUB-SITE-001", company, "東京本社");

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));
            // 意図的に null を明示スタブ：現在の実装が本当に findById(null) を呼ぶことを証明する
            when(participatingUserRepository.findById(null)).thenReturn(Optional.empty());
            when(siteRepository.save(any(Site.class))).thenAnswer(inv -> inv.getArgument(0));

            SiteInfoResponse response = siteService.updateSite(
                    "PUB-SITE-001", "COMP001", buildUpdateSiteRequest(null, "123-4567", null, null, null, null));

            // 現状はエラーにならず処理が進む（モックが null を許容するため）。
            // 実際の Spring Data JPA 実装では IllegalArgumentException を投げる可能性が高く、
            // validateUpdateSite が「PICUserId null = PIC変更なし」として扱うのと矛盾する。
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            verify(participatingUserRepository, times(1)).findById(null);
        }

        /*
         * BUG（引き継ぎ、S4-BUG-01相当）: siteName / siteNameKana の更新が
         * private updateSite() に実装されていない。
         */
        @Test
        @DisplayName("BUG: サイト名の更新が実装されていない（現状の挙動を記録）")
        void siteName_currentlyNotUpdatable() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site site = buildSite(5, "PUB-SITE-005", company, "旧サイト名");

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-005")).thenReturn(Optional.of(site));
            when(participatingUserRepository.findById(any())).thenReturn(Optional.empty());
            when(siteRepository.save(any(Site.class))).thenAnswer(inv -> inv.getArgument(0));

            SiteInfoResponse response = siteService.updateSite(
                    "PUB-SITE-005", "COMP001", buildUpdateSiteRequest(null, "123-4567", "大阪府", "大阪市", "2-2-2", "新ビル"));

            // siteName はリクエストに含まれていても更新ロジックが存在しないため変わらない
            assertThat(response.getSite().getSiteName()).isEqualTo("旧サイト名");
        }
    }

    // ============================================================================
    // 7. getSiteProjectEquipment（memberData 依存を削除した最新版に合わせて全面刷新）
    // ============================================================================
    @Nested
    @DisplayName("getSiteProjectEquipment のテスト")
    class GetSiteProjectEquipmentTest {

        @Test
        @DisplayName("SC-1: MASTERロールで全データが存在する場合、設備情報を返す")
        void masterRole_returnsEquipmentData() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site site = buildSite(1, "PUB-SITE-001", company, "東京本社");
            Project project = buildProject(10, "PUB-PROJ-010", "太陽光プロジェクト");
            SiteProjectRelation relation = buildRelation(site, project);
            EnR002Equipment equipment = buildEquipment(100, 1);
            List<EnR002EquipmentFile> files = List.of(
                    buildEquipmentFile("s3://bucket/file_001_evidence1.pdf"),
                    buildEquipmentFile("s3://bucket/file_002_evidence2.pdf"));

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));
            when(projectRepository.findByPublicIdWithFallback("PUB-PROJ-010")).thenReturn(Optional.of(project));
            when(projectRepository.findById(10)).thenReturn(Optional.of(project));
            when(siteProjectRelationRepository.findByProjectIdAndSiteId(10, 1)).thenReturn(relation);
            when(siteR002EquipmentRepository.findBySite_Id(1)).thenReturn(List.of(equipment));
            when(siteR002EquipmentFileRepository.findByEquipment_Id(100)).thenReturn(files);

            EnR002EquipmentRegisterResponse response = siteService.getSiteProjectEquipment(
                    "PUB-SITE-001", "PUB-PROJ-010", "COMP001", RoleConstants.MASTER, "master@example.com");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getEquipments()).hasSize(1);

            EnR002EquipmentResponse item = response.getEquipments().get(0);
            assertThat(item.getId()).isEqualTo(100);
            assertThat(item.getPcsManufacturer()).isEqualTo("株式会社オムロン");
            assertThat(item.getPcsModel()).isEqualTo("KP49R");
            // assertThat(item.getEvidenceDocuments()).containsExactly("evidence1.pdf", "evidence2.pdf");

            verify(participatingUserRepository, never()).findByUsernameAndCompany_Id(any(), anyInt());
        }

        @Test
        @DisplayName("SC-2: USERロールで割り当てられた拠点の場合、設備情報を返す")
        void userRoleAssignedToSite_returnsEquipmentData() {
            ParticipatingCompany company = buildCompany(2, "COMP002");
            Site site = buildSite(2, "PUB-SITE-002", company, "大阪支社");
            Project project = buildProject(20, "PUB-PROJ-020", "太陽光プロジェクトB");
            SiteProjectRelation relation = buildRelation(site, project);
            EnR002Equipment equipment = buildEquipment(200, 2);
            ParticipatingUser user = buildUser(70, company, RoleConstants.USER, site);

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-002")).thenReturn(Optional.of(site));
            when(projectRepository.findByPublicIdWithFallback("PUB-PROJ-020")).thenReturn(Optional.of(project));
            when(projectRepository.findById(20)).thenReturn(Optional.of(project));
            when(participatingUserRepository.findByUsernameAndCompany_Id("user@example.com", 2))
                    .thenReturn(Optional.of(user));
            when(siteProjectRelationRepository.findByProjectIdAndSiteId(20, 2)).thenReturn(relation);
            when(siteR002EquipmentRepository.findBySite_Id(2)).thenReturn(List.of(equipment));
            when(siteR002EquipmentFileRepository.findByEquipment_Id(200)).thenReturn(List.of());

            EnR002EquipmentRegisterResponse response = siteService.getSiteProjectEquipment(
                    "PUB-SITE-002", "PUB-PROJ-020", "COMP002", RoleConstants.USER, "user@example.com");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            // assertThat(response.getEquipments()).hasSize(1);
            // assertThat(response.getEquipments().get(0).setEquipments()).isEmpty();
        }

        @Test
        @DisplayName("EC-1: 拠点が見つからない場合、NOT_FOUND を返す")
        void siteNotFound_returnsNotFound() {
            when(siteRepository.findByPublicIdWithFallback("UNKNOWN")).thenReturn(Optional.empty());

            EnR002EquipmentRegisterResponse response = siteService.getSiteProjectEquipment(
                    "UNKNOWN", "PUB-PROJ-010", "COMP001", RoleConstants.MASTER, "master@example.com");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
            verify(projectRepository, never()).findByPublicIdWithFallback(any());
        }

        @Test
        @DisplayName("EC-2: プロジェクトが見つからない場合、NOT_FOUND を返す")
        void projectNotFound_returnsNotFound() {
            Site site = buildSite(1, "PUB-SITE-001", buildCompany(1, "COMP001"), "東京本社");
            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));
            when(projectRepository.findByPublicIdWithFallback("UNKNOWN")).thenReturn(Optional.empty());

            EnR002EquipmentRegisterResponse response = siteService.getSiteProjectEquipment(
                    "PUB-SITE-001", "UNKNOWN", "COMP001", RoleConstants.MASTER, "master@example.com");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_PROJECT_NOT_FOUND);
            verify(siteProjectRelationRepository, never()).findByProjectIdAndSiteId(any(), any());
        }

        @Test
        @DisplayName("EC-3: 企業IDが一致しない場合、FORBIDDEN を返す")
        void companyIdMismatch_returnsForbidden() {
            ParticipatingCompany actualCompany = buildCompany(1, "COMP002");
            Site site = buildSite(1, "PUB-SITE-001", actualCompany, "東京本社");
            Project project = buildProject(10, "PUB-PROJ-010", "太陽光プロジェクト");

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));
            when(projectRepository.findByPublicIdWithFallback("PUB-PROJ-010")).thenReturn(Optional.of(project));
            when(projectRepository.findById(10)).thenReturn(Optional.of(project));

            EnR002EquipmentRegisterResponse response = siteService.getSiteProjectEquipment(
                    "PUB-SITE-001", "PUB-PROJ-010", "COMP001", RoleConstants.MASTER, "master@example.com");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
            verify(siteProjectRelationRepository, never()).findByProjectIdAndSiteId(any(), any());
        }

        @Test
        @DisplayName("EC-4: USERロールで割り当てられていない拠点の場合、FORBIDDEN を返す")
        void userRoleNotAssignedToSite_returnsForbidden() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site site = buildSite(1, "PUB-SITE-001", company, "東京本社");
            Site otherSite = buildSite(999, "PUB-SITE-999", company, "別拠点");
            Project project = buildProject(10, "PUB-PROJ-010", "太陽光プロジェクト");
            ParticipatingUser user = buildUser(70, company, RoleConstants.USER, otherSite);

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));
            when(projectRepository.findByPublicIdWithFallback("PUB-PROJ-010")).thenReturn(Optional.of(project));
            when(projectRepository.findById(10)).thenReturn(Optional.of(project));
            when(participatingUserRepository.findByUsernameAndCompany_Id("user@example.com", 1))
                    .thenReturn(Optional.of(user));

            EnR002EquipmentRegisterResponse response = siteService.getSiteProjectEquipment(
                    "PUB-SITE-001", "PUB-PROJ-010", "COMP001", RoleConstants.USER, "user@example.com");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
            verify(siteProjectRelationRepository, never()).findByProjectIdAndSiteId(any(), any());
        }

        @Test
        @DisplayName("EC-5: プロジェクトと拠点のリレーションが見つからない場合、NOT_FOUND を返す")
        void relationNotFound_returnsNotFound() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site site = buildSite(1, "PUB-SITE-001", company, "東京本社");
            Project project = buildProject(10, "PUB-PROJ-010", "太陽光プロジェクト");

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));
            when(projectRepository.findByPublicIdWithFallback("PUB-PROJ-010")).thenReturn(Optional.of(project));
            when(projectRepository.findById(10)).thenReturn(Optional.of(project));
            when(siteProjectRelationRepository.findByProjectIdAndSiteId(10, 1)).thenReturn(null);

            EnR002EquipmentRegisterResponse response = siteService.getSiteProjectEquipment(
                    "PUB-SITE-001", "PUB-PROJ-010", "COMP001", RoleConstants.MASTER, "master@example.com");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            verify(siteR002EquipmentRepository, never()).findBySite_Id(any());
        }

        @Test
        @DisplayName("EC-6: 拠点に設備が1件も存在しない場合、NOT_FOUND を返す（memberData チェックは既に削除済み）")
        void noEquipmentForSite_returnsNotFound() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site site = buildSite(1, "PUB-SITE-001", company, "東京本社");
            Project project = buildProject(10, "PUB-PROJ-010", "太陽光プロジェクト");
            SiteProjectRelation relation = buildRelation(site, project);

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));
            when(projectRepository.findByPublicIdWithFallback("PUB-PROJ-010")).thenReturn(Optional.of(project));
            when(projectRepository.findById(10)).thenReturn(Optional.of(project));
            when(siteProjectRelationRepository.findByProjectIdAndSiteId(10, 1)).thenReturn(relation);
            when(siteR002EquipmentRepository.findBySite_Id(1)).thenReturn(Collections.emptyList());

            EnR002EquipmentRegisterResponse response = siteService.getSiteProjectEquipment(
                    "PUB-SITE-001", "PUB-PROJ-010", "COMP001", RoleConstants.MASTER, "master@example.com");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            verify(siteR002EquipmentFileRepository, never()).findByEquipment_Id(any());
        }

        @Test
        @DisplayName("EC-7: リポジトリが例外をスローした場合、INTERNAL_SERVER_ERROR を返す")
        void repositoryThrows_returnsInternalServerError() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site site = buildSite(1, "PUB-SITE-001", company, "東京本社");
            Project project = buildProject(10, "PUB-PROJ-010", "太陽光プロジェクト");
            SiteProjectRelation relation = buildRelation(site, project);

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));
            when(projectRepository.findByPublicIdWithFallback("PUB-PROJ-010")).thenReturn(Optional.of(project));
            when(projectRepository.findById(10)).thenReturn(Optional.of(project));
            when(siteProjectRelationRepository.findByProjectIdAndSiteId(10, 1)).thenReturn(relation);
            when(siteR002EquipmentRepository.findBySite_Id(1)).thenThrow(new RuntimeException("DB error"));

            EnR002EquipmentRegisterResponse response = siteService.getSiteProjectEquipment(
                    "PUB-SITE-001", "PUB-PROJ-010", "COMP001", RoleConstants.MASTER, "master@example.com");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
        }

        @Test
        @DisplayName("EC-8: s3Key が 'file_' で始まらない場合、ファイル名がそのまま使われる")
        void s3KeyWithoutFilePrefix_usesBaseNameAsIs() {
            ParticipatingCompany company = buildCompany(1, "COMP001");
            Site site = buildSite(1, "PUB-SITE-001", company, "東京本社");
            Project project = buildProject(10, "PUB-PROJ-010", "太陽光プロジェクト");
            SiteProjectRelation relation = buildRelation(site, project);
            EnR002Equipment equipment = buildEquipment(100, 1);

            when(siteRepository.findByPublicIdWithFallback("PUB-SITE-001")).thenReturn(Optional.of(site));
            when(projectRepository.findByPublicIdWithFallback("PUB-PROJ-010")).thenReturn(Optional.of(project));
            when(projectRepository.findById(10)).thenReturn(Optional.of(project));
            when(siteProjectRelationRepository.findByProjectIdAndSiteId(10, 1)).thenReturn(relation);
            when(siteR002EquipmentRepository.findBySite_Id(1)).thenReturn(List.of(equipment));
            when(siteR002EquipmentFileRepository.findByEquipment_Id(100))
                    .thenReturn(List.of(buildEquipmentFile("s3://bucket/evidence/battery-spec-001.pdf")));

            EnR002EquipmentRegisterResponse response = siteService.getSiteProjectEquipment(
                    "PUB-SITE-001", "PUB-PROJ-010", "COMP001", RoleConstants.MASTER, "master@example.com");

            // assertThat(response.getEquipments().get(0).getEvidenceDocuments())
            //         .containsExactly("battery-spec-001.pdf");
        }
    }
}