package com.kct.jcredit.core_backend.util;

import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

/**
 * Builds parameterized database predicates for administrator company searches.
 */
public final class ParticipatingCompanySpecification {

    private ParticipatingCompanySpecification() {
    }

    /**
     * Filters by one or more canonical company statuses.
     */
    public static Specification<ParticipatingCompany> hasStatuses(Set<CompanyStatus> statuses) {
        return (root, query, builder) -> root.get("companyStatus").in(statuses);
    }

    /**
     * Filters by an exact prefecture after request normalization.
     */
    public static Specification<ParticipatingCompany> hasPrefecture(String prefecture) {
        return (root, query, builder) -> builder.equal(root.get("prefecture"), prefecture);
    }

    /**
     * Includes companies created at or after the supplied timestamp.
     */
    public static Specification<ParticipatingCompany> createdAtOrAfter(LocalDateTime value) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), value);
    }

    /**
     * Includes companies created before the exclusive upper boundary.
     */
    public static Specification<ParticipatingCompany> createdBefore(LocalDateTime value) {
        return (root, query, builder) -> builder.lessThan(root.get("createdAt"), value);
    }

    /**
     * Searches identifiers and company names while escaping SQL wildcard characters.
     */
    public static Specification<ParticipatingCompany> containsKeyword(String keyword) {
        String pattern = "%" + escapeLike(keyword.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("companyId")), pattern, '\\'),
                builder.like(builder.lower(root.get("nameJa")), pattern, '\\'),
                builder.like(builder.lower(root.get("nameFurigana")), pattern, '\\'),
                builder.like(builder.lower(root.get("nameEn")), pattern, '\\'),
                builder.like(builder.lower(root.get("corpNumber")), pattern, '\\'),
                builder.like(builder.lower(root.get("email")), pattern, '\\')
        );
    }

    /**
     * Escapes characters that otherwise change LIKE pattern semantics.
     */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
