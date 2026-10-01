package com.kct.jcredit.core_backend.sitecontrollertest;

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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    @Mock
    private SiteRepository siteRepository;
    @Mock
    private ParticipatingCompanyRepository participatingCompanyRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private EnR002EquipmentFileRepository siteR002EquipmentFileRepository;
    @Mock
    private EnR002EquipmentRepository siteR002EquipmentRepository;
    @Mock
    private SiteProjectRelationRepository siteProjectRelationRepository;
    @Mock
    private ParticipatingUserRepository participatingUserRepository;

    @InjectMocks
    private SiteServiceImpl siteServiceImpl;

    private static final String COMPANY_ID = "COMP001";
    private static final String OTHER_COMPANY_ID = "COMP999";
    private static final String SITE_ID = "PUB-SITE-001";
    private static final String PROJECT_ID = "PUB-PROJ-001";
    private static final Integer COMPANY_DB_ID = 1;
    private static final Integer SITE_DB_ID = 10;
    private static final Integer PROJECT_DB_ID = 20;
    private static final Integer USER_ID = 5;

    // Helper: a company mock whose companyId/id are stubbed on demand by each test.
    private ParticipatingCompany companyMock() {
        return mock(ParticipatingCompany.class);
    }

    private Site siteMock() {
        return mock(Site.class);
    }

    private ParticipatingUser userMock() {
        return mock(ParticipatingUser.class);
    }

    // ==================================================================
    // 01. getSitesByCompanyId
    // ==================================================================

    @Test
    @DisplayName("UT-STS-01-01: 正常 - 会社と拠点が存在する場合OKと拠点一覧を返す")
    void getSitesByCompanyId_companyAndSitesExist_returnsOk() {
        ParticipatingCompany company = companyMock();

        Project project = mock(Project.class);
        when(project.getProjectName()).thenReturn("PJ-A");
        SiteProjectRelation relation = mock(SiteProjectRelation.class);
        when(relation.getProject()).thenReturn(project);

        Site site = siteMock();
        when(site.getPublicId()).thenReturn(SITE_ID);
        when(site.getSiteName()).thenReturn("熊本第一拠点");
        when(site.getSiteProjectRelations()).thenReturn(List.of(relation));

        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(siteRepository.findByCompany(company)).thenReturn(List.of(site));

        SiteListResponse response = siteServiceImpl.getSitesByCompanyId(COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getSiteList()).hasSize(1);
        assertThat(response.getSiteList().get(0).getSiteId()).isEqualTo(SITE_ID);
        assertThat(response.getSiteList().get(0).getSiteName()).isEqualTo("熊本第一拠点");
        assertThat(response.getSiteList().get(0).getProjects()).containsExactly("PJ-A");
        verify(siteRepository, times(1)).findByCompany(company);
    }

    @Test
    @DisplayName("UT-STS-01-02: 準正常 - siteProjectRelationsがnullの場合projectsは空リストになる")
    void getSitesByCompanyId_siteProjectRelationsNull_returnsEmptyProjects() {
        ParticipatingCompany company = companyMock();
        Site site = siteMock();
        when(site.getSiteProjectRelations()).thenReturn(null);

        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(siteRepository.findByCompany(company)).thenReturn(List.of(site));

        SiteListResponse response = siteServiceImpl.getSitesByCompanyId(COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getSiteList().get(0).getProjects()).isEmpty();
    }

    @Test
    @DisplayName("UT-STS-01-03: 異常 - 会社が存在しない場合NOT_FOUNDを返し拠点検索は行わない")
    void getSitesByCompanyId_companyNotFound_returnsNotFound() {
        when(participatingCompanyRepository.findByCompanyId(OTHER_COMPANY_ID)).thenReturn(Optional.empty());

        SiteListResponse response = siteServiceImpl.getSitesByCompanyId(OTHER_COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
        verifyNoInteractions(siteRepository);
    }

    @Test
    @DisplayName("UT-STS-01-04: 準正常 - 会社は存在するが拠点が0件の場合NOT_FOUNDを返す（空リストではない）")
    void getSitesByCompanyId_noSites_returnsNotFound() {
        ParticipatingCompany company = companyMock();
        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(siteRepository.findByCompany(company)).thenReturn(Collections.emptyList());

        SiteListResponse response = siteServiceImpl.getSitesByCompanyId(COMPANY_ID);

        // NOTE: 拠点0件は空リスト+OKではなくNOT_FOUND扱い。設備一覧(endpoint 10)とは
        // 挙動が異なるため、フロント側の分岐に影響する点を記録しておく。
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getSiteList()).isNull();
    }

    // ==================================================================
    // 02. getSiteByPublicId
    // ==================================================================

    @Test
    @DisplayName("UT-STS-02-01: 正常 - 拠点が存在する場合OKと拠点詳細を返す")
    void getSiteByPublicId_siteExists_returnsOk() {
        Project project = mock(Project.class);
        when(project.getProjectName()).thenReturn("PJ-A");
        SiteProjectRelation relation = mock(SiteProjectRelation.class);
        when(relation.getProject()).thenReturn(project);

        Site site = siteMock();
        when(site.getPublicId()).thenReturn(SITE_ID);
        when(site.getSiteName()).thenReturn("熊本第一拠点");
        when(site.getSiteProjectRelations()).thenReturn(List.of(relation));

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));

        SiteInfoResponse response = siteServiceImpl.getSiteByPublicId(SITE_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getSite().getSiteId()).isEqualTo(SITE_ID);
        assertThat(response.getSite().getSiteName()).isEqualTo("熊本第一拠点");
        assertThat(response.getSite().getProjects()).containsExactly("PJ-A");
    }

    @Test
    @DisplayName("UT-STS-02-02: 準正常 - siteProjectRelationsがnullの場合projectsは空リストになる")
    void getSiteByPublicId_relationsNull_returnsEmptyProjects() {
        Site site = siteMock();
        when(site.getSiteProjectRelations()).thenReturn(null);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));

        SiteInfoResponse response = siteServiceImpl.getSiteByPublicId(SITE_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getSite().getProjects()).isEmpty();
    }

    @Test
    @DisplayName("UT-STS-02-03: 異常 - 拠点が存在しない場合NOT_FOUNDを返す")
    void getSiteByPublicId_siteNotFound_returnsNotFound() {
        when(siteRepository.findByPublicIdWithFallback("UNKNOWN")).thenReturn(Optional.empty());

        SiteInfoResponse response = siteServiceImpl.getSiteByPublicId("UNKNOWN");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
    }

    @Test
    @DisplayName("UT-STS-02-04: 異常 - リポジトリが例外をスローする場合INTERNAL_SERVER_ERRORを返す（try/catchで捕捉）")
    void getSiteByPublicId_repositoryThrows_returnsInternalServerError() {
        when(siteRepository.findByPublicIdWithFallback(SITE_ID))
                .thenThrow(new RuntimeException("DB connection lost"));

        SiteInfoResponse response = siteServiceImpl.getSiteByPublicId(SITE_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
    }

    // ==================================================================
    // 04. createSite
    // ==================================================================

    @Test
    @DisplayName("UT-STS-04-01: 正常 - 全条件を満たす場合拠点が保存されOKを返す（保存内容とPIC情報を検証）")
    void createSite_validRequest_savesSiteAndReturnsOk() {
        CreateSiteRequest request = new CreateSiteRequest();
        request.setUserId(USER_ID);
        request.setSiteName("新拠点");
        request.setPostalCode("860-0001");

        ParticipatingCompany company = companyMock();
        when(company.getId()).thenReturn(COMPANY_DB_ID);
        when(company.getNameJa()).thenReturn("株式会社テスト");

        ParticipatingUser user = userMock();
        when(user.getCompany()).thenReturn(company);
        when(user.getRole()).thenReturn(RoleConstants.MASTER);
        when(user.getFirstName()).thenReturn("太郎");
        when(user.getLastName()).thenReturn("熊本");
        when(user.getEmail()).thenReturn("taro@example.com");

        Site savedSite = siteMock();
        when(savedSite.getPublicId()).thenReturn(SITE_ID);

        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(siteRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(3L);
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(siteRepository.save(any(Site.class))).thenReturn(savedSite);

        CreateSiteResponse response = siteServiceImpl.createSite(request, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getSiteId()).isEqualTo(SITE_ID);

        ArgumentCaptor<Site> siteCaptor = ArgumentCaptor.forClass(Site.class);
        verify(siteRepository, times(1)).save(siteCaptor.capture());
        Site captured = siteCaptor.getValue();
        assertThat(captured.getSiteName()).isEqualTo("新拠点");
        assertThat(captured.getPicFirstName()).isEqualTo("太郎");
        assertThat(captured.getPicLastName()).isEqualTo("熊本");
        assertThat(captured.getPicEmail()).isEqualTo("taro@example.com");
        assertThat(captured.getCreatedBy()).isEqualTo("株式会社テスト");

        // The PIC user must be linked back to the newly created site.
        verify(user, times(1)).setSite(any(Site.class));
        verify(participatingUserRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("UT-STS-04-02: 異常 - 会社が存在しない場合NOT_FOUNDを返し以降の処理は行わない")
    void createSite_companyNotFound_returnsNotFound() {
        CreateSiteRequest request = new CreateSiteRequest();
        when(participatingCompanyRepository.findByCompanyId(OTHER_COMPANY_ID)).thenReturn(Optional.empty());

        CreateSiteResponse response = siteServiceImpl.createSite(request, OTHER_COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        verifyNoInteractions(participatingUserRepository);
        verify(siteRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-STS-04-03: 異常 - 拠点数が上限(20)に達している場合LIMIT_EXCEEDEDを返す（境界値）")
    void createSite_siteCountAtLimit_returnsLimitExceeded() {
        CreateSiteRequest request = new CreateSiteRequest();
        ParticipatingCompany company = companyMock();
        when(company.getId()).thenReturn(COMPANY_DB_ID);

        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        // Boundary: exactly MAX_SITES_PER_COMPANY (20) must already be rejected.
        when(siteRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(20L);

        CreateSiteResponse response = siteServiceImpl.createSite(request, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.LIMIT_EXCEEDED);
        verifyNoInteractions(participatingUserRepository);
        verify(siteRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-STS-04-04: 異常 - ユーザーが存在しない場合NOT_FOUND(USER_NOT_FOUND)を返す")
    void createSite_userNotFound_returnsUserNotFound() {
        CreateSiteRequest request = new CreateSiteRequest();
        request.setUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getId()).thenReturn(COMPANY_DB_ID);

        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(siteRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(1L);
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.empty());

        CreateSiteResponse response = siteServiceImpl.createSite(request, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_USER_NOT_FOUND);
        verify(siteRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-STS-04-05: 異常 - ユーザーが別会社に所属する場合FORBIDDENを返す（会社スコープ検証）")
    void createSite_userFromOtherCompany_returnsForbidden() {
        CreateSiteRequest request = new CreateSiteRequest();
        request.setUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getId()).thenReturn(COMPANY_DB_ID);
        ParticipatingCompany otherCompany = companyMock();
        when(otherCompany.getId()).thenReturn(99);

        ParticipatingUser user = userMock();
        when(user.getCompany()).thenReturn(otherCompany);

        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(siteRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(1L);
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        CreateSiteResponse response = siteServiceImpl.createSite(request, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        verify(siteRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-STS-04-06: 異常 - master以外のユーザーが既に拠点を持つ場合BAD_REQUESTを返す")
    void createSite_nonMasterUserAlreadyHasSite_returnsBadRequest() {
        CreateSiteRequest request = new CreateSiteRequest();
        request.setUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getId()).thenReturn(COMPANY_DB_ID);

        ParticipatingUser user = userMock();
        when(user.getCompany()).thenReturn(company);
        when(user.getRole()).thenReturn(RoleConstants.USER);
        when(user.getSite()).thenReturn(siteMock());

        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(siteRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(1L);
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        CreateSiteResponse response = siteServiceImpl.createSite(request, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
        verify(siteRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-STS-04-07: 正常 - masterユーザーは既に拠点を持っていても作成できる（role分岐の裏側）")
    void createSite_masterUserWithExistingSite_isAllowed() {
        CreateSiteRequest request = new CreateSiteRequest();
        request.setUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getId()).thenReturn(COMPANY_DB_ID);
        when(company.getNameJa()).thenReturn("株式会社テスト");

        ParticipatingUser user = userMock();
        when(user.getCompany()).thenReturn(company);
        when(user.getRole()).thenReturn(RoleConstants.MASTER);

        Site savedSite = siteMock();
        when(savedSite.getPublicId()).thenReturn(SITE_ID);

        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(siteRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(1L);
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(siteRepository.save(any(Site.class))).thenReturn(savedSite);

        CreateSiteResponse response = siteServiceImpl.createSite(request, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        verify(siteRepository, times(1)).save(any(Site.class));
    }

    @Test
    @DisplayName("UT-STS-04-08: 異常 - 保存時に例外が発生した場合INTERNAL_SERVER_ERRORを返す")
    void createSite_saveThrows_returnsInternalServerError() {
        CreateSiteRequest request = new CreateSiteRequest();
        request.setUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getId()).thenReturn(COMPANY_DB_ID);
        when(company.getNameJa()).thenReturn("株式会社テスト");

        ParticipatingUser user = userMock();
        when(user.getCompany()).thenReturn(company);
        when(user.getRole()).thenReturn(RoleConstants.MASTER);

        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(siteRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(1L);
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(siteRepository.save(any(Site.class))).thenThrow(new RuntimeException("DB error"));

        CreateSiteResponse response = siteServiceImpl.createSite(request, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
    }

    @Test
    @DisplayName("UT-STS-04-09: 異常 - ユーザーの所属会社がnullの場合NPEではなく制御されたレスポンスを返すべき【要修正】")
    void createSite_userCompanyIsNull_shouldReturnControlledResponse() {
        // Pattern B: 現状 user.getCompany().getId() はnullガードされておらず、
        // try/catchの外側にあるためNPEがそのまま伝播する。本テストは意図された
        // 正しい挙動(制御されたレスポンス)を表明しており、修正までは失敗する。
        CreateSiteRequest request = new CreateSiteRequest();
        request.setUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getId()).thenReturn(COMPANY_DB_ID);

        ParticipatingUser user = userMock();
        when(user.getCompany()).thenReturn(null);

        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(siteRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(1L);
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        CreateSiteResponse response = assertDoesNotThrow(
                () -> siteServiceImpl.createSite(request, COMPANY_ID),
                "所属会社がnullのユーザーでもNPEを投げず制御されたレスポンスを返すべき");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        verify(siteRepository, never()).save(any());
    }

    // ==================================================================
    // 05. validateCreateSite
    // ==================================================================

    @Test
    @DisplayName("UT-STS-05-01: 正常 - 全条件を満たす場合検証成功を返す")
    void validateCreateSite_allValid_returnsSuccess() {
        CreateSiteRequest request = new CreateSiteRequest();
        request.setUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getId()).thenReturn(COMPANY_DB_ID);

        ParticipatingUser user = userMock();
        when(user.getCompany()).thenReturn(company);
        when(user.getRole()).thenReturn(RoleConstants.MASTER);

        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(siteRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(1L);
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        BaseResponse response = siteServiceImpl.validateCreateSite(request, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @Test
    @DisplayName("UT-STS-05-02: 異常 - 会社が存在しない場合companyIdエラーで即時終了する")
    void validateCreateSite_companyNotFound_returnsValidationError() {
        CreateSiteRequest request = new CreateSiteRequest();
        when(participatingCompanyRepository.findByCompanyId(OTHER_COMPANY_ID)).thenReturn(Optional.empty());

        BaseResponse response = siteServiceImpl.validateCreateSite(request, OTHER_COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.VALIDATION_ERROR);
        assertThat(response.getErrors()).containsEntry("companyId", ResponseConstants.MSG_NOT_FOUND);
        verifyNoInteractions(participatingUserRepository);
    }

    @Test
    @DisplayName("UT-STS-05-03: 異常 - 拠点数上限の場合companyIdにLIMIT_EXCEEDEDが設定される")
    void validateCreateSite_limitExceeded_returnsLimitError() {
        CreateSiteRequest request = new CreateSiteRequest();
        request.setUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getId()).thenReturn(COMPANY_DB_ID);

        ParticipatingUser user = userMock();
        when(user.getCompany()).thenReturn(company);
        when(user.getRole()).thenReturn(RoleConstants.MASTER);

        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(siteRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(20L);
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        BaseResponse response = siteServiceImpl.validateCreateSite(request, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.VALIDATION_ERROR);
        assertThat(response.getErrors()).containsEntry("companyId", ResponseConstants.MSG_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("UT-STS-05-04: 異常 - ユーザーが存在しない場合userIdエラーで即時終了する")
    void validateCreateSite_userNotFound_returnsUserError() {
        CreateSiteRequest request = new CreateSiteRequest();
        request.setUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getId()).thenReturn(COMPANY_DB_ID);

        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(siteRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(1L);
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.empty());

        BaseResponse response = siteServiceImpl.validateCreateSite(request, COMPANY_ID);

        assertThat(response.getErrors()).containsEntry("userId", ResponseConstants.MSG_USER_NOT_FOUND);
    }

    @Test
    @DisplayName("UT-STS-05-05: 異常 - ユーザーが別会社の場合userIdにFORBIDDENが設定される")
    void validateCreateSite_userFromOtherCompany_returnsForbiddenError() {
        CreateSiteRequest request = new CreateSiteRequest();
        request.setUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getId()).thenReturn(COMPANY_DB_ID);
        ParticipatingCompany otherCompany = companyMock();
        when(otherCompany.getId()).thenReturn(99);

        ParticipatingUser user = userMock();
        when(user.getCompany()).thenReturn(otherCompany);
        when(user.getRole()).thenReturn(RoleConstants.MASTER);

        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(siteRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(1L);
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        BaseResponse response = siteServiceImpl.validateCreateSite(request, COMPANY_ID);

        assertThat(response.getErrors()).containsEntry("userId.company", ResponseConstants.MSG_FORBIDDEN);
    }

    @Test
    @DisplayName("UT-STS-05-06: 異常 - master以外で既に拠点を持つ場合userIdにBAD_REQUESTが設定される")
    void validateCreateSite_nonMasterWithSite_returnsBadRequestError() {
        CreateSiteRequest request = new CreateSiteRequest();
        request.setUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getId()).thenReturn(COMPANY_DB_ID);

        ParticipatingUser user = userMock();
        when(user.getCompany()).thenReturn(company);
        when(user.getRole()).thenReturn(RoleConstants.USER);
        when(user.getSite()).thenReturn(siteMock());

        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(siteRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(1L);
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        BaseResponse response = siteServiceImpl.validateCreateSite(request, COMPANY_ID);

        assertThat(response.getErrors()).containsEntry("userId.site", ResponseConstants.MSG_BAD_REQUEST);
    }

    @Test
    @DisplayName("UT-STS-05-07: 異常 - 別会社かつ既に拠点を持つ場合、両方のエラー理由が失われないこと【要修正】")
    void validateCreateSite_twoUserErrors_shouldNotSilentlyOverwrite() {
        // Pattern B: 現状は errors.put("userId", ...) が同一キーを上書きするため、
        // FORBIDDENがBAD_REQUESTに潰されて1件しか残らない。
        // 本テストは「2つの違反理由が利用者に伝わるべき」という意図を表明する。
        CreateSiteRequest request = new CreateSiteRequest();
        request.setUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getId()).thenReturn(COMPANY_DB_ID);
        ParticipatingCompany otherCompany = companyMock();
        when(otherCompany.getId()).thenReturn(99);

        ParticipatingUser user = userMock();
        when(user.getCompany()).thenReturn(otherCompany);
        when(user.getRole()).thenReturn(RoleConstants.USER);
        when(user.getSite()).thenReturn(siteMock());

        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(siteRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(1L);
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        BaseResponse response = siteServiceImpl.validateCreateSite(request, COMPANY_ID);

        assertThat(response.getErrors().values())
                .as("会社違反(FORBIDDEN)と拠点重複(BAD_REQUEST)の両方が報告されるべき")
                .contains(ResponseConstants.MSG_FORBIDDEN, ResponseConstants.MSG_BAD_REQUEST);
    }

    @Test
    @DisplayName("UT-STS-05-08: 準正常 - 上限超過とユーザー不在が同時の場合、異なるキーのエラーは両方保持される")
    void validateCreateSite_limitAndUserNotFound_bothErrorsKept() {
        CreateSiteRequest request = new CreateSiteRequest();
        request.setUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getId()).thenReturn(COMPANY_DB_ID);

        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(siteRepository.countByCompany_Id(COMPANY_DB_ID)).thenReturn(20L);
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.empty());

        BaseResponse response = siteServiceImpl.validateCreateSite(request, COMPANY_ID);

        assertThat(response.getErrors())
                .containsEntry("companyId", ResponseConstants.MSG_LIMIT_EXCEEDED)
                .containsEntry("userId", ResponseConstants.MSG_USER_NOT_FOUND);
    }

    // ==================================================================
    // 06. updateSite
    // ==================================================================

    @Test
    @DisplayName("UT-STS-06-01: 正常 - 有効な更新でOKを返し、拠点情報とPIC情報が更新される")
    void updateSite_validRequest_updatesAndReturnsOk() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        request.setPICUserId(USER_ID);
        request.setSiteName("更新後拠点");

        ParticipatingCompany company = companyMock();
        when(company.getCompanyId()).thenReturn(COMPANY_ID);

        Site site = siteMock();
        when(site.getCompany()).thenReturn(company);

        ParticipatingUser user = userMock();
        when(user.getCompany()).thenReturn(company);
        when(user.getRole()).thenReturn(RoleConstants.MASTER);
        when(user.getEmail()).thenReturn("taro@example.com");
        when(user.getFirstName()).thenReturn("太郎");

        Site updatedSite = siteMock();
        when(updatedSite.getPublicId()).thenReturn(SITE_ID);
        when(updatedSite.getSiteName()).thenReturn("更新後拠点");

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(siteRepository.save(site)).thenReturn(updatedSite);

        SiteInfoResponse response = siteServiceImpl.updateSite(SITE_ID, COMPANY_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getSite().getSiteName()).isEqualTo("更新後拠点");
        verify(site, times(1)).setSiteName("更新後拠点");
        verify(site, times(1)).setPicEmail("taro@example.com");
        verify(site, times(1)).setPicFirstName("太郎");
        verify(site, times(1)).setUpdatedAt(any(LocalDateTime.class));
        verify(siteRepository, times(1)).save(site);
    }

    @Test
    @DisplayName("UT-STS-06-02: 準正常 - 空欄の項目は更新されない（StringUtils.isNotBlank分岐）")
    void updateSite_blankFields_areNotUpdated() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        request.setPICUserId(USER_ID);
        request.setSiteName("");       // blank -> skip
        request.setPostalCode("   ");  // blank -> skip
        request.setPrefecture("熊本県"); // not blank -> update

        ParticipatingCompany company = companyMock();
        when(company.getCompanyId()).thenReturn(COMPANY_ID);

        Site site = siteMock();
        when(site.getCompany()).thenReturn(company);

        ParticipatingUser user = userMock();
        when(user.getCompany()).thenReturn(company);
        when(user.getRole()).thenReturn(RoleConstants.MASTER);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(siteRepository.save(site)).thenReturn(site);

        SiteInfoResponse response = siteServiceImpl.updateSite(SITE_ID, COMPANY_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        verify(site, never()).setSiteName(anyString());
        verify(site, never()).setPostalCode(anyString());
        verify(site, times(1)).setPrefecture("熊本県");
    }

    @Test
    @DisplayName("UT-STS-06-03: 異常 - 拠点が存在しない場合NOT_FOUNDを返す")
    void updateSite_siteNotFound_returnsNotFound() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        when(siteRepository.findByPublicIdWithFallback("UNKNOWN")).thenReturn(Optional.empty());

        SiteInfoResponse response = siteServiceImpl.updateSite("UNKNOWN", COMPANY_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        verifyNoInteractions(participatingUserRepository);
        verify(siteRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-STS-06-04: 異常 - 拠点が他社のものである場合FORBIDDENを返す（会社スコープ検証）")
    void updateSite_siteFromOtherCompany_returnsForbidden() {
        UpdateSiteRequest request = new UpdateSiteRequest();

        ParticipatingCompany otherCompany = companyMock();
        when(otherCompany.getCompanyId()).thenReturn(OTHER_COMPANY_ID);
        Site site = siteMock();
        when(site.getCompany()).thenReturn(otherCompany);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));

        SiteInfoResponse response = siteServiceImpl.updateSite(SITE_ID, COMPANY_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        verifyNoInteractions(participatingUserRepository);
        verify(siteRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-STS-06-05: 異常 - PICユーザーが別会社の場合FORBIDDENを返す")
    void updateSite_picUserFromOtherCompany_returnsForbidden() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        request.setPICUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getCompanyId()).thenReturn(COMPANY_ID);
        ParticipatingCompany otherCompany = companyMock();
        when(otherCompany.getCompanyId()).thenReturn(OTHER_COMPANY_ID);

        Site site = siteMock();
        when(site.getCompany()).thenReturn(company);

        ParticipatingUser user = userMock();
        when(user.getCompany()).thenReturn(otherCompany);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        SiteInfoResponse response = siteServiceImpl.updateSite(SITE_ID, COMPANY_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        verify(siteRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-STS-06-06: 異常 - master以外のPICユーザーが別拠点に所属する場合BAD_REQUESTを返す")
    void updateSite_nonMasterPicFromOtherSite_returnsBadRequest() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        request.setPICUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getCompanyId()).thenReturn(COMPANY_ID);

        Site site = siteMock();
        when(site.getCompany()).thenReturn(company);
        when(site.getId()).thenReturn(SITE_DB_ID);

        Site otherSite = siteMock();
        when(otherSite.getId()).thenReturn(77);

        ParticipatingUser user = userMock();
        when(user.getCompany()).thenReturn(company);
        when(user.getRole()).thenReturn(RoleConstants.USER);
        when(user.getSite()).thenReturn(otherSite);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        SiteInfoResponse response = siteServiceImpl.updateSite(SITE_ID, COMPANY_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
        verify(siteRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-STS-06-07: 準正常 - PICユーザーが見つからない場合はPIC情報を変更せず拠点情報のみ更新する")
    void updateSite_picUserNotFound_updatesSiteFieldsOnly() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        request.setPICUserId(USER_ID);
        request.setSiteName("更新後拠点");

        ParticipatingCompany company = companyMock();
        when(company.getCompanyId()).thenReturn(COMPANY_ID);
        Site site = siteMock();
        when(site.getCompany()).thenReturn(company);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(siteRepository.save(site)).thenReturn(site);

        SiteInfoResponse response = siteServiceImpl.updateSite(SITE_ID, COMPANY_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        verify(site, times(1)).setSiteName("更新後拠点");
        verify(site, never()).setPicEmail(anyString());
        verify(site, never()).setPicFirstName(anyString());
    }

    @Test
    @DisplayName("UT-STS-06-08: 異常 - PICUserIdがnullの場合はユーザー検索をスキップすべき【要修正】")
    void updateSite_nullPicUserId_shouldSkipUserLookup() {
        // Pattern B: validateUpdateSite は request.getPICUserId() != null をガードしているが、
        // updateSite は無条件に findById(null) を呼ぶ。Spring Data では
        // IllegalArgumentException が投げられるため、PIC変更なしの更新が失敗する。
        UpdateSiteRequest request = new UpdateSiteRequest();
        request.setPICUserId(null);
        request.setSiteName("更新後拠点");

        ParticipatingCompany company = companyMock();
        when(company.getCompanyId()).thenReturn(COMPANY_ID);
        Site site = siteMock();
        when(site.getCompany()).thenReturn(company);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        // Reproduce Spring Data's real behaviour for a null id.
        lenient().when(participatingUserRepository.findById(null))
        .thenThrow(new IllegalArgumentException("The given id must not be null!"));
        lenient().when(siteRepository.save(site)).thenReturn(site);

        verify(participatingUserRepository, never()).findById(any());

        SiteInfoResponse response = assertDoesNotThrow(
                () -> siteServiceImpl.updateSite(SITE_ID, COMPANY_ID, request),
                "PICUserIdがnullの場合はユーザー検索を行わず更新を継続すべき");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
    }

    // ==================================================================
    // 07. validateUpdateSite
    // ==================================================================

    @Test
    @DisplayName("UT-STS-07-01: 正常 - PICUserId未指定で拠点が自社のものなら検証成功を返す")
    void validateUpdateSite_noPicUserId_returnsSuccess() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        request.setPICUserId(null);

        ParticipatingCompany company = companyMock();
        when(company.getCompanyId()).thenReturn(COMPANY_ID);
        Site site = siteMock();
        when(site.getCompany()).thenReturn(company);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));

        BaseResponse response = siteServiceImpl.validateUpdateSite(SITE_ID, COMPANY_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        // PICUserIdがnullのときはユーザー検索を行わない（updateSiteとの差分）
        verifyNoInteractions(participatingUserRepository);
    }

    @Test
    @DisplayName("UT-STS-07-02: 正常 - 自社のmasterユーザーをPICに指定した場合検証成功を返す")
    void validateUpdateSite_validMasterPic_returnsSuccess() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        request.setPICUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getCompanyId()).thenReturn(COMPANY_ID);
        Site site = siteMock();
        when(site.getCompany()).thenReturn(company);

        ParticipatingUser user = userMock();
        when(user.getCompany()).thenReturn(company);
        when(user.getRole()).thenReturn(RoleConstants.MASTER);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        BaseResponse response = siteServiceImpl.validateUpdateSite(SITE_ID, COMPANY_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
    }

    @Test
    @DisplayName("UT-STS-07-03: 異常 - 拠点が存在しない場合siteIdエラーで即時終了する")
    void validateUpdateSite_siteNotFound_returnsError() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        when(siteRepository.findByPublicIdWithFallback("UNKNOWN")).thenReturn(Optional.empty());

        BaseResponse response = siteServiceImpl.validateUpdateSite("UNKNOWN", COMPANY_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.VALIDATION_ERROR);
        assertThat(response.getErrors()).containsEntry("siteId", ResponseConstants.MSG_NOT_FOUND);
        verifyNoInteractions(participatingUserRepository);
    }

    @Test
    @DisplayName("UT-STS-07-04: 異常 - 拠点が他社のものである場合siteIdにFORBIDDENが設定される")
    void validateUpdateSite_siteFromOtherCompany_returnsForbidden() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        request.setPICUserId(null);

        ParticipatingCompany otherCompany = companyMock();
        when(otherCompany.getCompanyId()).thenReturn(OTHER_COMPANY_ID);
        Site site = siteMock();
        when(site.getCompany()).thenReturn(otherCompany);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));

        BaseResponse response = siteServiceImpl.validateUpdateSite(SITE_ID, COMPANY_ID, request);

        assertThat(response.getErrors()).containsEntry("siteId", ResponseConstants.MSG_FORBIDDEN);
    }

    @Test
    @DisplayName("UT-STS-07-05: 異常 - PICユーザーが存在しない場合PICUserIdにUSER_NOT_FOUNDが設定される")
    void validateUpdateSite_picUserNotFound_returnsUserNotFound() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        request.setPICUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getCompanyId()).thenReturn(COMPANY_ID);
        Site site = siteMock();
        when(site.getCompany()).thenReturn(company);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.empty());

        BaseResponse response = siteServiceImpl.validateUpdateSite(SITE_ID, COMPANY_ID, request);

        assertThat(response.getErrors()).containsEntry("PICUserId", ResponseConstants.MSG_USER_NOT_FOUND);
    }

    @Test
    @DisplayName("UT-STS-07-06: 異常 - PICユーザーが別会社の場合PICUserIdにFORBIDDENが設定される")
    void validateUpdateSite_picUserFromOtherCompany_returnsForbidden() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        request.setPICUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getCompanyId()).thenReturn(COMPANY_ID);
        ParticipatingCompany otherCompany = companyMock();
        when(otherCompany.getCompanyId()).thenReturn(OTHER_COMPANY_ID);

        Site site = siteMock();
        when(site.getCompany()).thenReturn(company);

        ParticipatingUser user = userMock();
        when(user.getCompany()).thenReturn(otherCompany);
        when(user.getRole()).thenReturn(RoleConstants.MASTER);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        BaseResponse response = siteServiceImpl.validateUpdateSite(SITE_ID, COMPANY_ID, request);

        assertThat(response.getErrors()).containsEntry("PICUserId", ResponseConstants.MSG_FORBIDDEN);
    }

    @Test
    @DisplayName("UT-STS-07-07: 異常 - master以外のPICユーザーが別拠点所属の場合PICUserIdにBAD_REQUESTが設定される")
    void validateUpdateSite_nonMasterPicFromOtherSite_returnsBadRequest() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        request.setPICUserId(USER_ID);

        ParticipatingCompany company = companyMock();
        when(company.getCompanyId()).thenReturn(COMPANY_ID);

        Site site = siteMock();
        when(site.getCompany()).thenReturn(company);
        when(site.getId()).thenReturn(SITE_DB_ID);

        Site otherSite = siteMock();
        when(otherSite.getId()).thenReturn(77);

        ParticipatingUser user = userMock();
        when(user.getCompany()).thenReturn(company);
        when(user.getRole()).thenReturn(RoleConstants.USER);
        when(user.getSite()).thenReturn(otherSite);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        BaseResponse response = siteServiceImpl.validateUpdateSite(SITE_ID, COMPANY_ID, request);

        assertThat(response.getErrors()).containsEntry("PICUserId", ResponseConstants.MSG_BAD_REQUEST);
    }

    @Test
    @DisplayName("UT-STS-07-08: 準正常 - 拠点違反とPIC違反が同時の場合、異なるキーのエラーは両方保持される")
    void validateUpdateSite_siteAndPicErrors_bothKept() {
        UpdateSiteRequest request = new UpdateSiteRequest();
        request.setPICUserId(USER_ID);

        ParticipatingCompany otherCompany = companyMock();
        when(otherCompany.getCompanyId()).thenReturn(OTHER_COMPANY_ID);
        Site site = siteMock();
        when(site.getCompany()).thenReturn(otherCompany);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(participatingUserRepository.findById(USER_ID)).thenReturn(Optional.empty());

        BaseResponse response = siteServiceImpl.validateUpdateSite(SITE_ID, COMPANY_ID, request);

        assertThat(response.getErrors())
                .containsEntry("siteId", ResponseConstants.MSG_FORBIDDEN)
                .containsEntry("PICUserId", ResponseConstants.MSG_USER_NOT_FOUND);
    }

    // ==================================================================
    // 08. getSiteProjectEquipment
    // ==================================================================

    private Site siteWithCompany(String companyId, Integer companyDbId) {
        ParticipatingCompany company = companyMock();
        lenient().when(company.getCompanyId()).thenReturn(companyId);
        lenient().when(company.getId()).thenReturn(companyDbId);
        Site site = siteMock();
        lenient().when(site.getCompany()).thenReturn(company);
        lenient().when(site.getId()).thenReturn(SITE_DB_ID);
        return site;
    }

    private void stubProjectFound() {
        Project project = mock(Project.class);
        lenient().when(project.getId()).thenReturn(PROJECT_DB_ID);
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectRepository.findById(PROJECT_DB_ID)).thenReturn(Optional.of(project));
    }

    @Test
    @DisplayName("UT-STS-08-01: 正常 - masterユーザーで全条件を満たす場合OKと設備情報を返す")
    void getSiteProjectEquipment_masterValid_returnsOk() {
        Site site = siteWithCompany(COMPANY_ID, COMPANY_DB_ID);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        stubProjectFound();

        when(siteProjectRelationRepository.findByProjectIdAndSiteId(PROJECT_DB_ID, SITE_DB_ID))
                .thenReturn(mock(SiteProjectRelation.class));

        EnR002Equipment equipment = mock(EnR002Equipment.class);
        when(equipment.getId()).thenReturn(100);
        when(equipment.getPcsManufacturer()).thenReturn("MakerA");
        when(siteR002EquipmentRepository.findBySite_Id(SITE_DB_ID)).thenReturn(List.of(equipment));

        EnR002EquipmentFile file = mock(EnR002EquipmentFile.class);
        when(file.getS3Key()).thenReturn("equipments/10/file_abc_証憑.pdf");
        when(siteR002EquipmentFileRepository.findByEquipment_Id(100)).thenReturn(List.of(file));

        EnR002EquipmentRegisterResponse response = siteServiceImpl.getSiteProjectEquipment(
                SITE_ID, PROJECT_ID, COMPANY_ID, RoleConstants.MASTER, "master001");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getEquipments()).hasSize(1);
        assertThat(response.getEquipments().get(0).getPcsManufacturer()).isEqualTo("MakerA");
        // masterの場合はユーザー検索を行わない
        verifyNoInteractions(participatingUserRepository);
    }

    @Test
    @DisplayName("UT-STS-08-02: 正常 - userロールで自身の拠点の場合アクセスが許可される")
    void getSiteProjectEquipment_userRoleOwnSite_returnsOk() {
        Site site = siteWithCompany(COMPANY_ID, COMPANY_DB_ID);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        stubProjectFound();

        Site userSite = siteMock();
        when(userSite.getId()).thenReturn(SITE_DB_ID);
        ParticipatingUser user = userMock();
        when(user.getSite()).thenReturn(userSite);
        when(participatingUserRepository.findByUsernameAndCompany_Id("user001", COMPANY_DB_ID))
                .thenReturn(Optional.of(user));

        when(siteProjectRelationRepository.findByProjectIdAndSiteId(PROJECT_DB_ID, SITE_DB_ID))
                .thenReturn(mock(SiteProjectRelation.class));

        EnR002Equipment equipment = mock(EnR002Equipment.class);
        when(equipment.getId()).thenReturn(100);
        when(siteR002EquipmentRepository.findBySite_Id(SITE_DB_ID)).thenReturn(List.of(equipment));
        when(siteR002EquipmentFileRepository.findByEquipment_Id(100)).thenReturn(Collections.emptyList());

        EnR002EquipmentRegisterResponse response = siteServiceImpl.getSiteProjectEquipment(
                SITE_ID, PROJECT_ID, COMPANY_ID, RoleConstants.USER, "user001");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
    }

    @Test
    @DisplayName("UT-STS-08-03: 異常 - 拠点が存在しない場合NOT_FOUND(SITE)を返す")
    void getSiteProjectEquipment_siteNotFound_returnsNotFound() {
        when(siteRepository.findByPublicIdWithFallback("UNKNOWN")).thenReturn(Optional.empty());

        EnR002EquipmentRegisterResponse response = siteServiceImpl.getSiteProjectEquipment(
                "UNKNOWN", PROJECT_ID, COMPANY_ID, RoleConstants.MASTER, "master001");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
        verifyNoInteractions(projectRepository);
    }

    @Test
    @DisplayName("UT-STS-08-04: 異常 - プロジェクトが存在しない場合NOT_FOUND(PROJECT)を返す")
    void getSiteProjectEquipment_projectNotFound_returnsProjectNotFound() {
        Site site = siteWithCompany(COMPANY_ID, COMPANY_DB_ID);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(projectRepository.findByPublicIdWithFallback("UNKNOWN-PJ")).thenReturn(Optional.empty());

        EnR002EquipmentRegisterResponse response = siteServiceImpl.getSiteProjectEquipment(
                SITE_ID, "UNKNOWN-PJ", COMPANY_ID, RoleConstants.MASTER, "master001");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_PROJECT_NOT_FOUND);
    }

    @Test
    @DisplayName("UT-STS-08-05: 異常 - 拠点が他社のものである場合FORBIDDENを返す（会社スコープ検証）")
    void getSiteProjectEquipment_otherCompanySite_returnsForbidden() {
        Site site = siteWithCompany(OTHER_COMPANY_ID, 99);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        stubProjectFound();

        EnR002EquipmentRegisterResponse response = siteServiceImpl.getSiteProjectEquipment(
                SITE_ID, PROJECT_ID, COMPANY_ID, RoleConstants.MASTER, "master001");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        verifyNoInteractions(siteProjectRelationRepository);
    }

    @Test
    @DisplayName("UT-STS-08-06: 異常 - userロールで別拠点に所属する場合FORBIDDENを返す")
    void getSiteProjectEquipment_userRoleOtherSite_returnsForbidden() {
        Site site = siteWithCompany(COMPANY_ID, COMPANY_DB_ID);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        stubProjectFound();

        Site userSite = siteMock();
        when(userSite.getId()).thenReturn(77);
        ParticipatingUser user = userMock();
        when(user.getSite()).thenReturn(userSite);
        when(participatingUserRepository.findByUsernameAndCompany_Id("user001", COMPANY_DB_ID))
                .thenReturn(Optional.of(user));

        EnR002EquipmentRegisterResponse response = siteServiceImpl.getSiteProjectEquipment(
                SITE_ID, PROJECT_ID, COMPANY_ID, RoleConstants.USER, "user001");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
    }

    @Test
    @DisplayName("UT-STS-08-07: 異常 - master/user以外のロール(admin等)はFORBIDDENを返す")
    void getSiteProjectEquipment_otherRole_returnsForbidden() {
        Site site = siteWithCompany(COMPANY_ID, COMPANY_DB_ID);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        stubProjectFound();

        EnR002EquipmentRegisterResponse response = siteServiceImpl.getSiteProjectEquipment(
                SITE_ID, PROJECT_ID, COMPANY_ID, "admin", "admin001");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        verifyNoInteractions(participatingUserRepository);
    }

    @Test
    @DisplayName("UT-STS-08-08: 異常 - 拠点とプロジェクトの関連が存在しない場合NOT_FOUNDを返す")
    void getSiteProjectEquipment_relationNotFound_returnsNotFound() {
        Site site = siteWithCompany(COMPANY_ID, COMPANY_DB_ID);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        stubProjectFound();
        when(siteProjectRelationRepository.findByProjectIdAndSiteId(PROJECT_DB_ID, SITE_DB_ID))
                .thenReturn(null);

        EnR002EquipmentRegisterResponse response = siteServiceImpl.getSiteProjectEquipment(
                SITE_ID, PROJECT_ID, COMPANY_ID, RoleConstants.MASTER, "master001");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        verifyNoInteractions(siteR002EquipmentRepository);
    }

    @Test
    @DisplayName("UT-STS-08-09: 準正常 - 設備が0件の場合NOT_FOUNDを返す（空リストではない）")
    void getSiteProjectEquipment_noEquipments_returnsNotFound() {
        Site site = siteWithCompany(COMPANY_ID, COMPANY_DB_ID);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        stubProjectFound();
        when(siteProjectRelationRepository.findByProjectIdAndSiteId(PROJECT_DB_ID, SITE_DB_ID))
                .thenReturn(mock(SiteProjectRelation.class));
        when(siteR002EquipmentRepository.findBySite_Id(SITE_DB_ID)).thenReturn(Collections.emptyList());

        EnR002EquipmentRegisterResponse response = siteServiceImpl.getSiteProjectEquipment(
                SITE_ID, PROJECT_ID, COMPANY_ID, RoleConstants.MASTER, "master001");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
    }

    @Test
    @DisplayName("UT-STS-08-10: 異常 - リポジトリが例外をスローする場合INTERNAL_SERVER_ERRORを返す")
    void getSiteProjectEquipment_repositoryThrows_returnsInternalServerError() {
        when(siteRepository.findByPublicIdWithFallback(SITE_ID))
                .thenThrow(new RuntimeException("DB connection lost"));

        EnR002EquipmentRegisterResponse response = siteServiceImpl.getSiteProjectEquipment(
                SITE_ID, PROJECT_ID, COMPANY_ID, RoleConstants.MASTER, "master001");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
    }

    @Test
    @DisplayName("UT-STS-08-11: 準正常 - 設備が複数世代ある場合、返されるのは最新世代であるべき【要確認】")
    void getSiteProjectEquipment_multipleVersions_shouldReturnLatest() {
        // Pattern B: 実装は equipments.get(0) を無条件に採用しており、
        // 並び順の指定も「現行世代」の絞り込みもない。設備変更(endpoint 12)により
        // 同一拠点に旧世代レコードが残るため、旧世代が返る可能性がある。
        Site site = siteWithCompany(COMPANY_ID, COMPANY_DB_ID);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        stubProjectFound();
        when(siteProjectRelationRepository.findByProjectIdAndSiteId(PROJECT_DB_ID, SITE_DB_ID))
                .thenReturn(mock(SiteProjectRelation.class));

        EnR002Equipment oldEquipment = mock(EnR002Equipment.class);
        lenient().when(oldEquipment.getId()).thenReturn(100);
        lenient().when(oldEquipment.getPcsManufacturer()).thenReturn("OldMaker");

        EnR002Equipment newEquipment = mock(EnR002Equipment.class);
        lenient().when(newEquipment.getId()).thenReturn(200);
        lenient().when(newEquipment.getPreviousEquipment()).thenReturn(oldEquipment);
        lenient().when(newEquipment.getPcsManufacturer()).thenReturn("NewMaker");

        // Repository returns the old version first.
        when(siteR002EquipmentRepository.findBySite_Id(SITE_DB_ID))
                .thenReturn(List.of(oldEquipment, newEquipment));
        lenient().when(siteR002EquipmentFileRepository.findByEquipment_Id(anyInt()))
                .thenReturn(Collections.emptyList());

        EnR002EquipmentRegisterResponse response = siteServiceImpl.getSiteProjectEquipment(
                SITE_ID, PROJECT_ID, COMPANY_ID, RoleConstants.MASTER, "master001");

        assertThat(response.getEquipments().get(0).getPcsManufacturer())
                .as("設備変更後は最新世代の設備が返されるべき")
                .isEqualTo("NewMaker");
    }
}