package com.kct.jcredit.core_backend.sitecontrollertest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.ProjectListInfoResponse;
import com.kct.jcredit.core_backend.dto.response.details.ProjectDetailResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.impl.ProjectServiceImpl;

// NOTE: クラス名は実際の実装クラスに合わせて調整してください
// (getProjectsBySiteIdAndCompanyId を持つ ProjectService 実装クラス)。
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectServiceImpl のテスト")
class ProjectServiceImplTest {

    @Mock
    private SiteRepository siteRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ParticipatingCompanyRepository participatingCompanyRepository;
    @Mock
    private ParticipatingProjectRepository participatingProjectRepository;
    @Mock
    private SiteProjectRelationRepository siteProjectRelationRepository;

    @InjectMocks
    private ProjectServiceImpl projectServiceImpl;

    private static final String COMPANY_ID = "COMP001";
    private static final String OTHER_COMPANY_ID = "COMP999";
    private static final String SITE_ID = "PUB-SITE-001";
    private static final Integer COMPANY_DB_ID = 1;
    private static final Integer SITE_DB_ID = 10;
    private static final Integer PROJECT_DB_ID = 20;

    private Site siteWithCompany(String companyId) {
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        lenient().when(company.getCompanyId()).thenReturn(companyId);
        Site site = mock(Site.class);
        lenient().when(site.getCompany()).thenReturn(company);
        lenient().when(site.getId()).thenReturn(SITE_DB_ID);
        return site;
    }

    // ==================================================================
    // 03. getProjectsBySiteIdAndCompanyId
    // ==================================================================

    @Test
    @DisplayName("UT-STS-03-01: 正常 - 参加済み・未参加のプロジェクトがそれぞれ正しくマッピングされOKを返す")
    void getProjectsBySiteIdAndCompanyId_validSite_returnsMappedProjects() {
        Site site = siteWithCompany(COMPANY_ID);
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getId()).thenReturn(COMPANY_DB_ID);

        Project joinedProject = mock(Project.class);
        when(joinedProject.getId()).thenReturn(PROJECT_DB_ID);
        when(joinedProject.getPublicId()).thenReturn("PUB-PJ-A");
        when(joinedProject.getProjectName()).thenReturn("太陽光プロジェクトA");
        when(joinedProject.getEmissionReduction()).thenReturn(120.5);

        Project notJoinedProject = mock(Project.class);
        when(notJoinedProject.getId()).thenReturn(21);
        when(notJoinedProject.getPublicId()).thenReturn("PUB-PJ-B");

        ParticipatingProject participation = mock(ParticipatingProject.class);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(projectRepository.findAll()).thenReturn(List.of(joinedProject, notJoinedProject));
        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(participatingProjectRepository.findByCompanyIdAndProject_IdOrderByIdAsc(COMPANY_DB_ID, PROJECT_DB_ID))
                .thenReturn(List.of(participation));
        when(participatingProjectRepository.findByCompanyIdAndProject_IdOrderByIdAsc(COMPANY_DB_ID, 21))
                .thenReturn(Collections.emptyList());
        when(siteProjectRelationRepository.existsBySite_IdAndProject_Id(SITE_DB_ID, PROJECT_DB_ID))
                .thenReturn(true);
        when(siteProjectRelationRepository.existsBySite_IdAndProject_Id(SITE_DB_ID, 21))
                .thenReturn(false);

        ProjectListInfoResponse response =
                projectServiceImpl.getProjectsBySiteIdAndCompanyId(SITE_ID, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getProjects()).hasSize(2);

        ProjectDetailResponse joined = response.getProjects().get(0);
        assertThat(joined.getId()).isEqualTo("PUB-PJ-A");
        assertThat(joined.getProjectName()).isEqualTo("太陽光プロジェクトA");
        assertThat(joined.getEmissionReduction()).isEqualTo("120.5");
        assertThat(joined.getIsCompanyJoined()).isTrue();
        assertThat(joined.getHasSiteRelation()).isTrue();

        ProjectDetailResponse notJoined = response.getProjects().get(1);
        assertThat(notJoined.getIsCompanyJoined()).isFalse();
        assertThat(notJoined.getHasSiteRelation()).isFalse();
    }

    @Test
    @DisplayName("UT-STS-03-02: 準正常 - 排出量系の項目がnullの場合は文字列\"0\"に変換される")
    void getProjectsBySiteIdAndCompanyId_nullEmissionValues_defaultToZeroString() {
        Site site = siteWithCompany(COMPANY_ID);
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getId()).thenReturn(COMPANY_DB_ID);

        Project project = mock(Project.class);
        when(project.getId()).thenReturn(PROJECT_DB_ID);
        when(project.getEmissionReduction()).thenReturn(null);
        when(project.getBaselineEmission()).thenReturn(null);
        when(project.getPostProjectEmission()).thenReturn(null);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(projectRepository.findAll()).thenReturn(List.of(project));
        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(participatingProjectRepository.findByCompanyIdAndProject_IdOrderByIdAsc(COMPANY_DB_ID, PROJECT_DB_ID))
                .thenReturn(Collections.emptyList());
        when(siteProjectRelationRepository.existsBySite_IdAndProject_Id(SITE_DB_ID, PROJECT_DB_ID))
                .thenReturn(false);

        ProjectListInfoResponse response =
                projectServiceImpl.getProjectsBySiteIdAndCompanyId(SITE_ID, COMPANY_ID);

        ProjectDetailResponse detail = response.getProjects().get(0);
        assertThat(detail.getEmissionReduction()).isEqualTo("0");
        assertThat(detail.getBaselineEmission()).isEqualTo("0");
        assertThat(detail.getPostProjectEmission()).isEqualTo("0");
    }

    @Test
    @DisplayName("UT-STS-03-03: 準正常 - プロジェクトが1件もない場合OKと空リストを返す")
    void getProjectsBySiteIdAndCompanyId_noProjects_returnsOkWithEmptyList() {
        Site site = siteWithCompany(COMPANY_ID);
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getId()).thenReturn(COMPANY_DB_ID);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(projectRepository.findAll()).thenReturn(Collections.emptyList());
        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));

        ProjectListInfoResponse response =
                projectServiceImpl.getProjectsBySiteIdAndCompanyId(SITE_ID, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getProjects()).isEmpty();
        verifyNoInteractions(participatingProjectRepository, siteProjectRelationRepository);
    }

    @Test
    @DisplayName("UT-STS-03-04: 異常 - 拠点が存在しない場合NOT_FOUNDを返す")
    void getProjectsBySiteIdAndCompanyId_siteNotFound_returnsNotFound() {
        when(siteRepository.findByPublicIdWithFallback("UNKNOWN")).thenReturn(Optional.empty());

        ProjectListInfoResponse response =
                projectServiceImpl.getProjectsBySiteIdAndCompanyId("UNKNOWN", COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
        assertThat(response.getProjects()).isEmpty();
        verifyNoInteractions(projectRepository, participatingCompanyRepository);
    }

    @Test
    @DisplayName("UT-STS-03-05: 異常 - 拠点が他社のものである場合FORBIDDENを返す（会社スコープ検証）")
    void getProjectsBySiteIdAndCompanyId_otherCompanySite_returnsForbidden() {
        Site site = siteWithCompany(OTHER_COMPANY_ID);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));

        ProjectListInfoResponse response =
                projectServiceImpl.getProjectsBySiteIdAndCompanyId(SITE_ID, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_UNAUTHORIZED);
        verifyNoInteractions(projectRepository, participatingCompanyRepository);
    }

    @Test
    @DisplayName("UT-STS-03-06: 異常 - 会社情報が取得できない場合CustomExceptionがスローされる（他の分岐と異なりレスポンスを返さない）")
    void getProjectsBySiteIdAndCompanyId_companyDbIdNotFound_throwsCustomException() {
        // NOTE: 同一メソッド内で、拠点未検出/権限違反はレスポンス方式なのに対し、
        // 会社未検出だけ例外方式になっている。エラー処理方式が混在している点を記録する。
        Site site = siteWithCompany(COMPANY_ID);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(projectRepository.findAll()).thenReturn(Collections.emptyList());
        when(participatingCompanyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(CustomException.class,
                () -> projectServiceImpl.getProjectsBySiteIdAndCompanyId(SITE_ID, COMPANY_ID));
    }
}