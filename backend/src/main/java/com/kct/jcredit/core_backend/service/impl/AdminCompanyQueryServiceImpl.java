package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.CompanyListQuery;
import com.kct.jcredit.core_backend.dto.response.AdminCompanyListResponse;
import com.kct.jcredit.core_backend.dto.response.details.AdminCompanyListItemResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.service.AdminCompanyQueryService;
import com.kct.jcredit.core_backend.util.ParticipatingCompanySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * Builds bounded database queries for the unified administrator company list.
 */
@Service
@RequiredArgsConstructor
public class AdminCompanyQueryServiceImpl implements AdminCompanyQueryService {

    private final ParticipatingCompanyRepository companyRepository;

    /**
     * Applies only validated filters and adds an ID tie-breaker for stable pagination.
     */
    @Override
    @Transactional(readOnly = true)
    public AdminCompanyListResponse findCompanies(CompanyListQuery query) {
        Sort.Direction direction = Sort.Direction.fromString(query.getSortOrder());
        Sort sort = Sort.by(direction, query.getSort()).and(Sort.by(direction, "id"));
        PageRequest pageRequest = PageRequest.of(query.getPage() - 1, query.getLimit(), sort);

        Specification<ParticipatingCompany> specification = Specification.unrestricted();
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            specification = specification.and(
                    ParticipatingCompanySpecification.hasStatuses(query.getStatus())
            );
        }
        if (StringUtils.hasText(query.getSearch())) {
            specification = specification.and(
                    ParticipatingCompanySpecification.containsKeyword(query.getSearch().trim())
            );
        }
        if (StringUtils.hasText(query.getPrefecture())) {
            specification = specification.and(
                    ParticipatingCompanySpecification.hasPrefecture(query.getPrefecture().trim())
            );
        }
        if (query.getCreatedFrom() != null) {
            specification = specification.and(
                    ParticipatingCompanySpecification.createdAtOrAfter(query.getCreatedFrom().atStartOfDay())
            );
        }
        if (query.getCreatedTo() != null) {
            specification = specification.and(
                    ParticipatingCompanySpecification.createdBefore(query.getCreatedTo().plusDays(1).atStartOfDay())
            );
        }

        Page<ParticipatingCompany> page = companyRepository.findAll(specification, pageRequest);
        List<AdminCompanyListItemResponse> companies = page.getContent().stream()
                .map(this::toListItem)
                .toList();

        AdminCompanyListResponse response = new AdminCompanyListResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(ResponseConstants.MSG_COMPANY_LIST_RETRIEVED);
        response.setCompanies(companies);
        response.setPagination(new PaginationDetailResponse(
                query.getPage(),
                query.getLimit(),
                page.getTotalElements(),
                page.getTotalPages(),
                null,
                page.hasNext()
        ));
        return response;
    }

    /**
     * Maps database fields to the administrator-specific list contract.
     */
    private AdminCompanyListItemResponse toListItem(ParticipatingCompany company) {
        return AdminCompanyListItemResponse.builder()
                .id(company.getId() == null ? null : company.getId().toString())
                .companyId(company.getCompanyId())
                .nameJA(company.getNameJa())
                .nameFurigana(company.getNameFurigana())
                .nameEN(company.getNameEn())
                .email(company.getEmail())
                .phoneNumber(company.getPhoneNumber())
                .fax(company.getFax())
                .corpNumber(company.getCorpNumber())
                .address(buildAddress(company))
                .postalCode(company.getPostalCode())
                .prefecture(company.getPrefecture())
                .municipality(company.getMunicipality())
                .blocknum(company.getBlocknum())
                .buildingName(company.getBuildingName())
                .representativeFirstName(company.getRepFirstName())
                .representativeLastName(company.getRepLastName())
                .representativeFirstNameKana(company.getRepFirstNameKana())
                .representativeLastNameKana(company.getRepLastNameKana())
                .representativePosition(company.getRepPosition())
                .representativePhoneNumber(company.getRepPhoneNumber())
                .requestedMethodologies(company.getRequestedMethodologies() == null
                        ? Collections.emptyList()
                        : List.copyOf(company.getRequestedMethodologies()))
                .activityInformation(company.getActivityInformation())
                .status(company.getCompanyStatus())
                .isActive(company.getCompanyStatus() == CompanyStatus.ACTIVE)
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }

    /**
     * Preserves the existing combined address while also returning split address fields.
     */
    private String buildAddress(ParticipatingCompany company) {
        return String.join(" ",
                safe(company.getPostalCode()),
                safe(company.getPrefecture()),
                safe(company.getMunicipality()),
                safe(company.getBlocknum()),
                safe(company.getBuildingName())
        ).trim().replaceAll("\\s+", " ");
    }

    /**
     * Converts nullable address parts into safe join values.
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }
}
