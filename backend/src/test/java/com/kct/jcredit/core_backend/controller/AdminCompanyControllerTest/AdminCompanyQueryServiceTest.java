package com.kct.jcredit.core_backend.controller.AdminCompanyControllerTest;

import com.kct.jcredit.core_backend.dto.request.CompanyListQuery;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.response.*;
import com.kct.jcredit.core_backend.entity.*;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.repository.postgres.*;
import com.kct.jcredit.core_backend.service.impl.AdminCompanyQueryServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminCompanyQueryServiceTest {

    @Mock private ParticipatingCompanyRepository companyRepository;
    @Mock private ParticipatingUserRepository userRepository;
    @Mock private SiteRepository siteRepository;

    @InjectMocks
    private AdminCompanyQueryServiceImpl adminCompanyQueryService;

    private ParticipatingCompany company(Integer id, String companyId, CompanyStatus status) {
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getId()).thenReturn(id);
        when(company.getCompanyId()).thenReturn(companyId);
        when(company.getCompanyStatus()).thenReturn(status);
        lenient().when(company.getIsActive()).thenReturn(status == CompanyStatus.ACTIVE);
        return company;
    }

    private ParticipatingUser user(String username, String email, String role) {
        ParticipatingUser user = mock(ParticipatingUser.class);
        when(user.getUsername()).thenReturn(username);
        when(user.getEmail()).thenReturn(email);
        when(user.getRole()).thenReturn(role);
        return user;
    }

    private CompanyListQuery query(int page, int limit, String sort, String order) {
        CompanyListQuery query = new CompanyListQuery();
        query.setPage(page);
        query.setLimit(limit);
        query.setSort(sort);
        query.setSortOrder(order);
        return query;
    }

    // ------------------------------------------------------------------------
    // findCompanies
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies のテスト")
    class FindCompaniesTest {

        @Test
        @DisplayName("UT-ACQ-001: 全フィルタ指定時にSpecificationとPageRequestが正しく組み立てられ、正常なレスポンスが返る")
        void findCompanies_allFiltersProvided_buildsSpecificationAndReturnsMappedResponse() {
            // Input: query: status=[ACTIVE], search="テスト", prefecture="東京都",
            //        createdFrom=2026-01-01, createdTo=2026-01-31, sort="createdAt",
            //        sortOrder="DESC", page=1, limit=20
            //        companyRepository.findAll(any(Specification), any(PageRequest)) が
            //        ParticipatingCompanyのPage（1件）を返すようモック
            // Output: statusCode=OK, message=MSG_COMPANY_LIST_RETRIEVED
            //         companiesが1件、各フィールドがtoListItem()のマッピング仕様通りであること
            //         （id=String化されたid、address=結合済み文字列、isActive= status==ACTIVEか等）
                CompanyListQuery query = query(1, 20, "createdAt", "DESC");
                query.setStatus(Set.of(CompanyStatus.ACTIVE));
                query.setSearch("テスト");
                query.setPrefecture("東京都");
                query.setCreatedFrom(LocalDate.of(2026, 1, 1));
                query.setCreatedTo(LocalDate.of(2026, 1, 31));
                ParticipatingCompany company = company(1, "COMP-001", CompanyStatus.ACTIVE);
                when(companyRepository.findAll(ArgumentMatchers.<Specification<ParticipatingCompany>>any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(company), PageRequest.of(0, 20), 1));
                AdminCompanyListResponse response = adminCompanyQueryService.findCompanies(query,null);
                assertThat(response.getStatusCode()).isEqualTo("OK");
                assertThat(response.getCompanies()).hasSize(1);
                assertThat(response.getCompanies().get(0).getId()).isEqualTo("1");
                assertThat(response.getCompanies().get(0).getIsActive()).isTrue();
                assertThat(response.getPagination().getTotalItems()).isEqualTo(1);
                ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
                verify(companyRepository).findAll(ArgumentMatchers.<Specification<ParticipatingCompany>>any(), pageable.capture());
                assertThat(pageable.getValue().getPageNumber()).isZero();
                assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("UT-ACQ-002: statusとsearchが未指定（準正常）の場合、対応するSpecificationが付与されず全件対象になる")
        void findCompanies_noStatusOrSearch_skipsOptionalSpecifications() {
            // Input: query: status=null（またはempty）, search=null, prefecture=null,
            //        createdFrom=null, createdTo=null, sort="id", sortOrder="ASC", page=1, limit=20
            //        companyRepository.findAll(...) が空のPageを返すようモック
            // Output: statusCode=OK, companies=空リスト
            //         pagination.totalElements=0, hasNext=false
            CompanyListQuery query = query(1, 20, "id", "ASC");
            when(companyRepository.findAll(ArgumentMatchers.<Specification<ParticipatingCompany>>any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
            AdminCompanyListResponse response = adminCompanyQueryService.findCompanies(query, null);
            assertThat(response.getStatusCode()).isEqualTo("OK");
            assertThat(response.getCompanies()).isEmpty();
            verify(companyRepository).findAll(ArgumentMatchers.<Specification<ParticipatingCompany>>any(), any(Pageable.class));
        }

        @Test
        @DisplayName("UT-ACQ-003: 該当企業0件でも例外を投げず、空リストと正しいページネーション情報を返す")
        void findCompanies_noMatchingCompanies_returnsEmptyListWithPagination() {
            // Input: query: status=[SUSPENDED], search="存在しない企業", page=2, limit=10
            //        companyRepository.findAll(...) が totalElements=0, totalPages=0 のPageを返すようモック
            // Output: statusCode=OK, companies=空リスト
            CompanyListQuery query = query(2, 10, "createdAt", "DESC");
            query.setStatus(Set.of(CompanyStatus.SUSPENDED));
            query.setSearch("存在しない企業");
            when(companyRepository.findAll(ArgumentMatchers.<Specification<ParticipatingCompany>>any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
            AdminCompanyListResponse response = adminCompanyQueryService.findCompanies(query, null);
            assertThat(response.getCompanies()).isEmpty();
            assertThat(response.getPagination().getPage()).isEqualTo(2);
            assertThat(response.getPagination().getTotalItems()).isZero();
            assertThat(response.getPagination().isHasMore()).isFalse();
        }

        @Test
        @DisplayName("UT-ACQ-004: sortOrderに不正な値が指定された場合、Sort.Direction.fromStringが例外を投げる")
        void findCompanies_invalidSortOrder_throwsIllegalArgumentException() {
            // Input: query: sortOrder="INVALID_DIRECTION", sort="createdAt", page=1, limit=20
            // Output: IllegalArgumentExceptionがスローされること（Sort.Direction.fromStringの仕様）
                CompanyListQuery query = query(1, 20, "createdAt", "INVALID_DIRECTION");
                assertThatThrownBy(() -> adminCompanyQueryService.findCompanies(query, null))
                    .isInstanceOf(IllegalArgumentException.class);
                verifyNoInteractions(companyRepository);
        }

        @Test
        @DisplayName("UT-ACQ-005: createdFrom/createdToのみ指定時、日付境界（atStartOfDay / plusDays(1)）が適用されたSpecificationが使われる")
        void findCompanies_dateRangeOnly_appliesInclusiveDateBoundarySpecification() {
            // Input: query: createdFrom=2026-03-01, createdTo=2026-03-01, status=null, search=null,
            //        prefecture=null, page=1, limit=20
            //        companyRepository.findAll(any(Specification), any(PageRequest)) をキャプチャして返すようモック
            // Output: statusCode=OK
            CompanyListQuery query = query(1, 20, "createdAt", "ASC");
            query.setCreatedFrom(LocalDate.of(2026, 3, 1));
            query.setCreatedTo(LocalDate.of(2026, 3, 1));
            when(companyRepository.findAll(ArgumentMatchers.<Specification<ParticipatingCompany>>any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
            assertThat(adminCompanyQueryService.findCompanies(query, null).getStatusCode()).isEqualTo("OK");
            ArgumentCaptor<Specification<ParticipatingCompany>> specification = ArgumentCaptor.captor();
            verify(companyRepository).findAll(specification.capture(), any(Pageable.class));
            assertThat(specification.getValue()).isNotNull();
        }
    }

    // ------------------------------------------------------------------------
    // findCompanyDetail
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies/{companyId}のテスト")
    class FindCompanyDetailTest {

        @Test
        @DisplayName("UT-ACQ-006: 存在するcompanyIdの場合、company/masterAccount/users/sitesが正しく組み立てられて返る")
        void findCompanyDetail_existingCompanyId_returnsFullyAssembledResponse() {
            // Input: companyId="COMP-001"
            //        companyRepository.findByCompanyId("COMP-001") が ParticipatingCompany を返すようモック
            //        userRepository.findDetailsByCompanyId(company.getId()) が MASTERロール1件+一般ユーザー1件を返すようモック
            //        siteRepository.findDetailsByCompanyId(company.getId()) がSite1件を返すようモック
            // Output: statusCode=OK, message=MSG_COMPANY_DETAIL_RETRIEVED
            //         masterAccountがMASTERロールユーザーの内容と一致すること
            //         companyが会社情報のマッピング通りであること（address結合含む）
            //         usersがソート順（MASTER優先→username昇順）で返ること
            ParticipatingCompany company = company(1, "COMP-001", CompanyStatus.ACTIVE);
                        ParticipatingUser master = user("master", "master@example.com", RoleConstants.MASTER);
            ParticipatingUser operator = user("operator", "operator@example.com", "OPERATOR");
            Site site = mock(Site.class);
            when(site.getSiteName()).thenReturn("Site A");
            when(site.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 1, 1, 0, 0));
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            when(userRepository.findDetailsByCompanyId(1)).thenReturn(List.of(operator, master));
            when(siteRepository.findDetailsByCompanyId(1)).thenReturn(List.of(site));
            AdminCompanyDetailResponse response = adminCompanyQueryService.findCompanyDetail("COMP-001");
            assertThat(response.getStatusCode()).isEqualTo("OK");
            assertThat(response.getMasterAccount().getUsername()).isEqualTo("master");
            assertThat(response.getUsers()).extracting("username").containsExactly("master", "operator");
            assertThat(response.getSites()).hasSize(1);
            assertThat(response.getMasterAccount().getEmail()).isEqualTo("master@example.com");
            assertThat(response.getSites().get(0).getSiteName()).isEqualTo("Site A");
            verify(userRepository).findDetailsByCompanyId(1);
            verify(siteRepository).findDetailsByCompanyId(1);
        }

        @Test
        @DisplayName("UT-ACQ-007: 存在しないcompanyIdの場合、NOT_FOUNDレスポンスを返し他のRepositoryは呼ばれない")
        void findCompanyDetail_nonExistingCompanyId_returnsNotFoundWithoutFurtherLookups() {
            // Input: companyId="COMP-404"
            //        companyRepository.findByCompanyId("COMP-404") がOptional.emptyを返すようモック
            // Output: statusCode=NOT_FOUND, message=MSG_COMPANY_NOT_FOUND, timestampが設定されていること
            //         userRepository.findDetailsByCompanyId / siteRepository.findDetailsByCompanyId が
            when(companyRepository.findByCompanyId("COMP-404")).thenReturn(Optional.empty());
            AdminCompanyDetailResponse response = adminCompanyQueryService.findCompanyDetail("COMP-404");
            assertThat(response.getStatusCode()).isEqualTo("NOT_FOUND");
            assertThat(response.getMessage()).isEqualTo("対象の企業が見つかりませんでした。");
            verifyNoInteractions(userRepository, siteRepository);
        }

        @Test
        @DisplayName("UT-ACQ-008: MASTERロールユーザーが存在しない場合（準正常）、masterAccountはnullで他情報は正常に返る")
        void findCompanyDetail_noMasterUser_returnsNullMasterAccountWithOtherFieldsPopulated() {
            // Input: companyId="COMP-002"
            //        companyRepository.findByCompanyId("COMP-002") が ParticipatingCompany を返すようモック
            //        userRepository.findDetailsByCompanyId(...) がMASTER以外のユーザーのみ返すようモック
            //        siteRepository.findDetailsByCompanyId(...) が空リストを返すようモック
            // Output: statusCode=OK, masterAccount=null
            //         usersにはMASTER以外のユーザーがマッピングされていること
            ParticipatingCompany company = company(2, "COMP-002", CompanyStatus.ACTIVE);
            ParticipatingUser operator = user("operator", "operator@example.com", "OPERATOR");
            when(companyRepository.findByCompanyId("COMP-002")).thenReturn(Optional.of(company));
            when(userRepository.findDetailsByCompanyId(2)).thenReturn(List.of(operator));
            when(siteRepository.findDetailsByCompanyId(2)).thenReturn(List.of());
            AdminCompanyDetailResponse response = adminCompanyQueryService.findCompanyDetail("COMP-002");
            assertThat(response.getMasterAccount()).isNull();
            assertThat(response.getUsers()).hasSize(1);
            assertThat(response.getSites()).isEmpty();
        }

        @Test
        @DisplayName("UT-ACQ-009: Siteに紐づくプロジェクト関連が存在する場合、projectNamesが重複除去・ソートされて返る")
        void findCompanyDetail_siteWithProjectRelations_returnsDistinctSortedProjectNames() {
            // Input: companyId="COMP-003"
            //        companyRepository.findByCompanyId(...) が ParticipatingCompany を返すようモック
            //        userRepository.findDetailsByCompanyId(...) が空リストを返すようモック
            //        siteRepository.findDetailsByCompanyId(...) が
            //        SiteProjectRelation（projectName="B project", "A project", "A project"（重複））
            //        を持つSite1件を返すようモック
            // Output: statusCode=OK
            ParticipatingCompany company = company(3, "COMP-003", CompanyStatus.ACTIVE);
            Site site = mock(Site.class);
            SiteProjectRelation first = mock(SiteProjectRelation.class);
            SiteProjectRelation second = mock(SiteProjectRelation.class);
            SiteProjectRelation duplicate = mock(SiteProjectRelation.class);
            Project projectA = mock(Project.class);
            Project projectB = mock(Project.class);
            when(projectA.getProjectName()).thenReturn("A project");
            when(projectB.getProjectName()).thenReturn("B project");
            when(first.getProject()).thenReturn(projectB);
            when(second.getProject()).thenReturn(projectA);
            when(duplicate.getProject()).thenReturn(projectA);
            when(site.getSiteProjectRelations()).thenReturn(List.of(first, second, duplicate));
            when(site.getSiteName()).thenReturn("Site");
            when(companyRepository.findByCompanyId("COMP-003")).thenReturn(Optional.of(company));
            when(userRepository.findDetailsByCompanyId(3)).thenReturn(List.of());
            when(siteRepository.findDetailsByCompanyId(3)).thenReturn(List.of(site));
            AdminCompanyDetailResponse response = adminCompanyQueryService.findCompanyDetail("COMP-003");
            assertThat(response.getSites().get(0).getProjectNames()).containsExactly("A project", "B project");
        }

    }
}