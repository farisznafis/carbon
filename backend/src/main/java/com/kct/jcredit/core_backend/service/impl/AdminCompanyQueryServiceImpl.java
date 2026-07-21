package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.request.CompanyListQuery;
import com.kct.jcredit.core_backend.dto.response.AdminCompanyDetailResponse;
import com.kct.jcredit.core_backend.dto.response.AdminCompanyListResponse;
import com.kct.jcredit.core_backend.dto.response.details.AdminCompanyDetailCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.details.AdminCompanyDetailMasterResponse;
import com.kct.jcredit.core_backend.dto.response.details.AdminCompanyDetailSiteResponse;
import com.kct.jcredit.core_backend.dto.response.details.AdminCompanyDetailUserResponse;
import com.kct.jcredit.core_backend.dto.response.details.AdminCompanyListItemResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Builds bounded database queries for the unified administrator company list.
 */
@Service
@RequiredArgsConstructor
public class AdminCompanyQueryServiceImpl implements AdminCompanyQueryService {

    private final ParticipatingCompanyRepository companyRepository;
    private final ParticipatingUserRepository userRepository;
    private final SiteRepository siteRepository;

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

    @Override
    @Transactional(readOnly = true)
    public AdminCompanyDetailResponse findCompanyDetail(String companyId) {
        AdminCompanyDetailResponse response = new AdminCompanyDetailResponse();
        ParticipatingCompany company = companyRepository.findByCompanyId(companyId).orElse(null);
        if (company == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_COMPANY_NOT_FOUND);
            response.setTimestamp(LocalDateTime.now());
            return response;
        }

        List<ParticipatingUser> users = new ArrayList<>(userRepository.findDetailsByCompanyId(company.getId()));
        users.sort(Comparator
                .comparing((ParticipatingUser user) -> !RoleConstants.MASTER.equals(user.getRole()))
                .thenComparing(user -> safe(user.getUsername()), String.CASE_INSENSITIVE_ORDER));

        List<Site> sites = new ArrayList<>(siteRepository.findDetailsByCompanyId(company.getId()));
        sites.sort(Comparator
                .comparing(Site::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(site -> safe(site.getSiteName()), String.CASE_INSENSITIVE_ORDER));

        response.setMasterAccount(users.stream()
                .filter(user -> RoleConstants.MASTER.equals(user.getRole()))
                .findFirst()
                .map(this::toMasterDetail)
                .orElse(null));
        response.setCompany(toCompanyDetail(company));
        response.setUsers(users.stream().map(this::toUserDetail).toList());
        response.setSites(sites.stream().map(this::toSiteDetail).toList());
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(ResponseConstants.MSG_COMPANY_DETAIL_RETRIEVED);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    private AdminCompanyDetailMasterResponse toMasterDetail(ParticipatingUser user) {
        return AdminCompanyDetailMasterResponse.builder()
                .username(user.getUsername())
                .fullName(buildFullName(user.getLastName(), user.getFirstName()))
                .fullNameKana(buildFullName(user.getLastNameKana(), user.getFirstNameKana()))
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .firstNameKana(user.getFirstNameKana())
                .lastNameKana(user.getLastNameKana())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .userPosition(user.getUserPosition())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .createdBy(user.getCreatedBy())
                .updatedBy(user.getUpdatedBy())
                .build();
    }

    private AdminCompanyDetailCompanyResponse toCompanyDetail(ParticipatingCompany company) {
        return AdminCompanyDetailCompanyResponse.builder()
                .companyId(company.getCompanyId())
                .corpNumber(company.getCorpNumber())
                .nameJA(company.getNameJa())
                .nameFurigana(company.getNameFurigana())
                .nameEN(company.getNameEn())
                .email(company.getEmail())
                .phoneNumber(company.getPhoneNumber())
                .faxNumber(company.getFax())
                .address(buildAddress(company))
                .postalCode(company.getPostalCode())
                .prefecture(company.getPrefecture())
                .municipality(company.getMunicipality())
                .blocknum(company.getBlocknum())
                .buildingName(company.getBuildingName())
                .companyStatus(company.getCompanyStatus())
                .isActive(company.getIsActive())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .createdBy(company.getCreatedBy())
                .updatedBy(company.getUpdatedBy())
                .build();
    }

    private AdminCompanyDetailUserResponse toUserDetail(ParticipatingUser user) {
        return AdminCompanyDetailUserResponse.builder()
                .username(user.getUsername())
                .fullName(buildFullName(user.getLastName(), user.getFirstName()))
                .email(user.getEmail())
                .role(user.getRole())
                .siteName(user.getSite() == null ? null : user.getSite().getSiteName())
                .build();
    }

    private AdminCompanyDetailSiteResponse toSiteDetail(Site site) {
        List<String> projectNames = site.getSiteProjectRelations() == null
                ? Collections.emptyList()
                : site.getSiteProjectRelations().stream()
                .filter(relation -> relation.getProject() != null)
                .map(relation -> relation.getProject().getProjectName())
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        return AdminCompanyDetailSiteResponse.builder()
                .siteName(site.getSiteName())
                .picEmail(site.getPicEmail())
                .createdAt(site.getCreatedAt())
                .address(buildAddress(
                        site.getPostalCode(),
                        site.getPrefecture(),
                        site.getMunicipality(),
                        site.getBlocknum(),
                        site.getBuildingName()
                ))
                .postalCode(site.getPostalCode())
                .prefecture(site.getPrefecture())
                .municipality(site.getMunicipality())
                .blocknum(site.getBlocknum())
                .buildingName(site.getBuildingName())
                .projectNames(projectNames)
                .build();
    }

    private String buildFullName(String lastName, String firstName) {
        return String.join(" ", safe(lastName), safe(firstName)).trim().replaceAll("\\s+", " ");
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
        return buildAddress(
                company.getPostalCode(),
                company.getPrefecture(),
                company.getMunicipality(),
                company.getBlocknum(),
                company.getBuildingName()
        );
    }

    private String buildAddress(
            String postalCode,
            String prefecture,
            String municipality,
            String blocknum,
            String buildingName
    ) {
        return String.join(" ",
                safe(postalCode),
                safe(prefecture),
                safe(municipality),
                safe(blocknum),
                safe(buildingName)
        ).trim().replaceAll("\\s+", " ");
    }

    /**
     * Converts nullable address parts into safe join values.
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }
}
