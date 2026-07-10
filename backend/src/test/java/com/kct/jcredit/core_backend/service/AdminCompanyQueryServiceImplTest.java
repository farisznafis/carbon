package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.CompanyListQuery;
import com.kct.jcredit.core_backend.dto.response.AdminCompanyListResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCompanyQueryServiceImplTest {

    @Mock
    private ParticipatingCompanyRepository companyRepository;

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
}
