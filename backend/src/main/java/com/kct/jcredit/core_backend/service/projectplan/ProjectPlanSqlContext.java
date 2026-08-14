package com.kct.jcredit.core_backend.service.projectplan;

import java.util.function.Supplier;

import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Project;

/**
 * The PostgreSQL-side data a page may autofill from, resolved once per request.
 *
 * <p>The master {@link OperatingUser} is resolved lazily and memoised: only the
 * application page needs it, and the other 30+ pages should not pay for that
 * query on every read.
 */
public final class ProjectPlanSqlContext {

    private final Project project;
    private final Supplier<OperatingUser> masterUserSupplier;

    private OperatingUser masterUser;
    private boolean masterUserResolved;

    public ProjectPlanSqlContext(Project project, Supplier<OperatingUser> masterUserSupplier) {
        this.project = project;
        this.masterUserSupplier = masterUserSupplier;
    }

    public Project project() {
        return project;
    }

    /** May be {@code null} — callers must guard. */
    public OperatorCompany company() {
        return project == null ? null : project.getOperatorCompany();
    }

    /** May be {@code null} when the company has no master user. */
    public OperatingUser masterUser() {
        if (!masterUserResolved) {
            masterUser = masterUserSupplier == null ? null : masterUserSupplier.get();
            masterUserResolved = true;
        }
        return masterUser;
    }

    /** 代表者氏名 — the company representative's full name. */
    public String repFullName() {
        OperatorCompany company = company();
        return company == null
                ? null
                : fullName(company.getRepLastName(), company.getRepFirstName());
    }

    /** 担当者氏名 — the master user's full name, or {@code null} if there is none. */
    public String masterFullName() {
        OperatingUser user = masterUser();
        return user == null ? null : fullName(user.getLastName(), user.getFirstName());
    }

    public String masterEmail() {
        OperatingUser user = masterUser();
        return user == null ? null : user.getEmail();
    }

    public String masterPhoneNumber() {
        OperatingUser user = masterUser();
        return user == null ? null : user.getPhoneNumber();
    }

    /** 方法論コード, e.g. {@code EN-R-002}. Null-tolerant on both halves. */
    public String methodologyCode() {
        if (project == null) {
            return null;
        }
        String category = project.getMethodCategory();
        String number = project.getMethodNumber();
        if (isBlank(category) && isBlank(number)) {
            return null;
        }
        if (isBlank(category)) {
            return number;
        }
        if (isBlank(number)) {
            return category;
        }
        return category + "-" + number;
    }

    /**
     * Project stores emissions as {@code Double} while the plan DTOs use
     * {@code BigDecimal}. Null-safe so an unset figure stays unset rather than
     * autofilling as zero.
     */
    public static java.math.BigDecimal decimal(Double value) {
        return value == null ? null : java.math.BigDecimal.valueOf(value);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String fullName(String lastName, String firstName) {
        String safeLast = lastName == null ? "" : lastName.trim();
        String safeFirst = firstName == null ? "" : firstName.trim();
        String combined = (safeLast + " " + safeFirst).trim();
        return combined.isEmpty() ? null : combined;
    }
}
