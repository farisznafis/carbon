package com.kct.jcredit.core_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.CompanyListQuery;
import com.kct.jcredit.core_backend.dto.response.AdminCompanyDetailResponse;
import com.kct.jcredit.core_backend.dto.response.AdminCompanyListResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.impl.AdminCompanyQueryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCompanyQueryServiceImplTest {

    @Mock
    private ParticipatingCompanyRepository companyRepository;

    @Mock
    private ParticipatingUserRepository userRepository;

    @Mock
    private SiteRepository siteRepository;

    @InjectMocks
    private AdminCompanyQueryServiceImpl service;

    @Test
    void findCompaniesReturnsStablePaginatedAdminContract() {
        CompanyListQuery query = new CompanyListQuery();
        query.setStatus(Set.of(CompanyStatus.ACTIVE, CompanyStatus.SUSPENDED));
        query.setSearch("GX");

        ParticipatingCompany company = new ParticipatingCompany();
        company.setId(1);
        company.setCompanyId("COMP00000001");
        company.setNameJa("GX株式会社");
        company.setNameFurigana("ジーエックス");
        company.setNameEn("GX Company");
        company.setCompanyStatus(CompanyStatus.ACTIVE);
        company.setIsActive(true);
        company.setCreatedAt(LocalDateTime.of(2026, 7, 10, 9, 0));

        when(companyRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(company)));

        AdminCompanyListResponse response = service.findCompanies(query);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getCompanies()).hasSize(1);
        assertThat(response.getCompanies().getFirst().getStatus()).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(response.getCompanies().getFirst().getIsActive()).isTrue();
        assertThat(response.getPagination().getPage()).isEqualTo(1);
        assertThat(response.getPagination().getLimit()).isEqualTo(20);
    }

    @Test
    void findCompanyDetailMapsMasterCompanyUsersSitesAndProjectsWithoutNumericIds() throws Exception {
        ParticipatingCompany company = buildCompany(1, "COMP00000001", CompanyStatus.ACTIVE);

        Project projectB = Project.builder().projectName("Project B").build();
        Project projectA = Project.builder().projectName("Project A").build();

        Site siteB = buildSite("Site B", LocalDateTime.of(2026, 2, 1, 9, 0));
        Site siteA = buildSite("Site A", LocalDateTime.of(2026, 1, 1, 9, 0));
        siteA.setSiteProjectRelations(List.of(
                SiteProjectRelation.builder().project(projectB).site(siteA).build(),
                SiteProjectRelation.builder().project(projectA).site(siteA).build(),
                SiteProjectRelation.builder().project(projectA).site(siteA).build()
        ));
        siteB.setSiteProjectRelations(List.of());

        ParticipatingUser regularUser = buildUser("aaa_user", RoleConstants.USER, siteA);
        ParticipatingUser masterUser = buildUser("zzz_master", RoleConstants.MASTER, null);

        when(companyRepository.findByCompanyId(company.getCompanyId())).thenReturn(Optional.of(company));
        when(userRepository.findDetailsByCompanyId(company.getId()))
                .thenReturn(List.of(regularUser, masterUser));
        when(siteRepository.findDetailsByCompanyId(company.getId()))
                .thenReturn(List.of(siteB, siteA));

        AdminCompanyDetailResponse response = service.findCompanyDetail(company.getCompanyId());

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getMasterAccount().getUsername()).isEqualTo("zzz_master");
        assertThat(response.getMasterAccount().getFullName()).isEqualTo("Yamada Taro");
        assertThat(response.getCompany().getCompanyId()).isEqualTo("COMP00000001");
        assertThat(response.getCompany().getFaxNumber()).isEqualTo("0312345679");
        assertThat(response.getCompany().getAddress()).isEqualTo("1000001 Tokyo Chiyoda 1-1 GX Building");
        assertThat(response.getUsers()).extracting("username")
                .containsExactly("zzz_master", "aaa_user");
        assertThat(response.getUsers().get(1).getSiteName()).isEqualTo("Site A");
        assertThat(response.getSites()).extracting("siteName")
                .containsExactly("Site A", "Site B");
        assertThat(response.getSites().getFirst().getProjectNames())
                .containsExactly("Project A", "Project B");

        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(response);
        assertThat(json).doesNotContain("\"id\":");
        assertThat(json).doesNotContain("lastLogin", "decisionMemo", "approvedAt", "subscriptionStatus");
    }

    @Test
    void findCompanyDetailAllowsPendingCompanyWithoutMaster() {
        ParticipatingCompany company = buildCompany(2, "COMP00000002", CompanyStatus.PENDING);
        when(companyRepository.findByCompanyId(company.getCompanyId())).thenReturn(Optional.of(company));
        when(userRepository.findDetailsByCompanyId(company.getId())).thenReturn(List.of());
        when(siteRepository.findDetailsByCompanyId(company.getId())).thenReturn(List.of());

        AdminCompanyDetailResponse response = service.findCompanyDetail(company.getCompanyId());

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getMasterAccount()).isNull();
        assertThat(response.getUsers()).isEmpty();
        assertThat(response.getSites()).isEmpty();
    }

    @Test
    void findCompanyDetailReturnsNotFoundForUnknownCompany() {
        when(companyRepository.findByCompanyId("UNKNOWN")).thenReturn(Optional.empty());

        AdminCompanyDetailResponse response = service.findCompanyDetail("UNKNOWN");

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_COMPANY_NOT_FOUND);
        assertThat(response.getCompany()).isNull();
    }

    private ParticipatingCompany buildCompany(Integer id, String companyId, CompanyStatus status) {
        ParticipatingCompany company = ParticipatingCompany.builder()
                .companyId(companyId)
                .corpNumber("1234567890123")
                .nameJa("GX株式会社")
                .nameFurigana("ジーエックス")
                .nameEn("GX Company")
                .email("company@example.com")
                .phoneNumber("0312345678")
                .fax("0312345679")
                .postalCode("1000001")
                .prefecture("Tokyo")
                .municipality("Chiyoda")
                .blocknum("1-1")
                .buildingName("GX Building")
                .companyStatus(status)
                .build();
        company.setId(id);
        company.setIsActive(status == CompanyStatus.ACTIVE);
        company.setCreatedAt(LocalDateTime.of(2026, 1, 1, 8, 0));
        return company;
    }

    private ParticipatingUser buildUser(String username, String role, Site site) {
        ParticipatingUser user = ParticipatingUser.builder()
                .username(username)
                .firstName("Taro")
                .lastName("Yamada")
                .firstNameKana("タロウ")
                .lastNameKana("ヤマダ")
                .phoneNumber("09012345678")
                .email(username + "@example.com")
                .userPosition("Manager")
                .role(role)
                .site(site)
                .build();
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.of(2026, 1, 1, 8, 0));
        return user;
    }

    private Site buildSite(String siteName, LocalDateTime createdAt) {
        Site site = Site.builder()
                .siteName(siteName)
                .picEmail(siteName.replace(" ", "").toLowerCase() + "@example.com")
                .postalCode("1000001")
                .prefecture("Tokyo")
                .municipality("Chiyoda")
                .blocknum("1-1")
                .buildingName(siteName + " Building")
                .build();
        site.setCreatedAt(createdAt);
        return site;
    }
}
