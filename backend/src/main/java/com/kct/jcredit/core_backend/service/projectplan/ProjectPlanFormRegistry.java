package com.kct.jcredit.core_backend.service.projectplan;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kct.jcredit.core_backend.dto.request.ApplicableConditionsRequest;
import com.kct.jcredit.core_backend.dto.request.OtherOperatorProjectsRequest;
import com.kct.jcredit.core_backend.dto.request.ProjectApplicationRequest;
import com.kct.jcredit.core_backend.util.AddressFormatter;

import org.springframework.stereotype.Component;

/**
 * The single place that knows which pages the project plan has.
 *
 * <p>Ordering mirrors the wizard steps in the frontend
 * ({@code frontend/src/features/admin/project-registration/forms/projectSteps.ts});
 * step 1 (プロジェクト作成) is the SQL-backed project record and is not a page here.
 *
 * <p>Duplicate slugs or sort keys fail fast at startup.
 */
@Component
public class ProjectPlanFormRegistry {

    private final Map<String, FormDefinition<?>> bySlug = new LinkedHashMap<>();
    private final Map<String, FormDefinition<?>> bySkPrefix = new LinkedHashMap<>();

    public ProjectPlanFormRegistry() {
        register(new FormDefinition<>(
                "application",
                "PROJECT_APPLICATION",
                "プロジェクト申請",
                2,
                false,
                ProjectApplicationRequest.class,
                List.of(
                        sql("projectOperatorKana",
                                (d, c) -> d.setProjectOperatorKana(c.company().getNameFurigana())),
                        sql("projectOperatorNameJa",
                                (d, c) -> d.setProjectOperatorNameJa(c.company().getNameJa())),
                        sql("projectOperatorNameEn",
                                (d, c) -> d.setProjectOperatorNameEn(c.company().getNameEn())),
                        sql("corpNumber", (d, c) -> d.setCorpNumber(c.company().getCorpNumber())),
                        sql("corpPosition", (d, c) -> d.setCorpPosition(c.company().getRepPosition())),
                        sql("corpName", (d, c) -> d.setCorpName(c.company().getNameJa())),
                        sql("prefecture", (d, c) -> d.setPrefecture(c.company().getPrefecture())),
                        sql("city", (d, c) -> d.setCity(c.company().getMunicipality())),
                        // 市区町村以降の住所: the company record splits this across two
                        // columns, the form keeps it as one line.
                        sql("address", (d, c) -> d.setAddress(
                                AddressFormatter.joinAddressAfterCity(c.company().getBlocknum(), c.company().getBuildingName()))),
                        sql("representativePosition",
                                (d, c) -> d.setRepresentativePosition(c.company().getRepPosition())),
                        sql("representativeName",
                                (d, c) -> d.setRepresentativeName(c.repFullName())),
                        sql("picPosition", (d, c) -> d.setPicPosition(c.company().getRepPosition())),
                        // Preserve stored values when the company has no master user,
                        // matching the behaviour of the service this replaces.
                        sql("picName", (d, c) -> {
                            if (c.masterUser() != null) {
                                d.setPicName(c.masterFullName());
                            }
                        }),
                        sql("picEmail", (d, c) -> {
                            if (c.masterUser() != null) {
                                d.setPicEmail(c.masterEmail());
                            }
                        }),
                        sql("picPhoneNumber", (d, c) -> {
                            if (c.masterUser() != null) {
                                d.setPicPhoneNumber(c.masterPhoneNumber());
                            }
                        }),
                        sql("projectNumber", (d, c) -> d.setProjectNumber(c.project().getProjectNumber())),
                        sql("projectName", (d, c) -> d.setProjectName(c.project().getProjectName())),
                        sql("methodologyCategory",
                                (d, c) -> d.setMethodologyCategory(c.project().getMethodCategory())),
                        sql("methodologyNumber",
                                (d, c) -> d.setMethodologyNumber(c.project().getMethodNumber())),
                        sql("methodology", (d, c) -> d.setMethodology(c.project().getMethodName())),
                        sql("regionalCategory",
                                (d, c) -> d.setRegionalCategory(c.project().getRegionalCategory())))));

        register(new FormDefinition<>(
                "other-operator-projects",
                "OTHER_OPERATOR_PROJECTS",
                "代表以外のプログラム型運営・管理者",
                3,
                false,
                OtherOperatorProjectsRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "applicable-conditions",
                "APPLICABLE_CONDITIONS",
                "(参考様式)適用条件",
                4,
                false,
                ApplicableConditionsRequest.class,
                List.of(
                        sql("methodologyCode", (d, c) -> d.setMethodologyCode(c.methodologyCode())),
                        sql("methodologyVersion",
                                (d, c) -> d.setMethodologyVersion(c.project().getMethodologyVersion())),
                        sql("businessName", (d, c) -> d.setBusinessName(c.company().getNameJa())),
                        sql("representativePosition",
                                (d, c) -> d.setRepresentativePosition(c.company().getRepPosition())),
                        sql("representativeName",
                                (d, c) -> d.setRepresentativeName(c.repFullName())))));
    }

    /** Type-inference helper so the lambdas above stay readable. */
    private static <T> SqlField<T> sql(
            String jsonName, java.util.function.BiConsumer<T, ProjectPlanSqlContext> apply) {
        return new SqlField<>(jsonName, apply);
    }


    private void register(FormDefinition<?> definition) {
        if (bySlug.putIfAbsent(definition.slug(), definition) != null) {
            throw new IllegalStateException("Duplicate project-plan form slug: " + definition.slug());
        }
        if (bySkPrefix.putIfAbsent(definition.skPrefix(), definition) != null) {
            throw new IllegalStateException(
                    "Duplicate project-plan form sort key: " + definition.skPrefix());
        }
    }

    public Optional<FormDefinition<?>> bySlug(String slug) {
        return Optional.ofNullable(slug == null ? null : bySlug.get(slug));
    }

    /** Resolves a stored sort key ({@code PREFIX} or {@code PREFIX#M2}) back to its page. */
    public Optional<FormDefinition<?>> bySortKey(String sortKey) {
        if (sortKey == null) {
            return Optional.empty();
        }
        int separator = sortKey.indexOf('#');
        String prefix = separator < 0 ? sortKey : sortKey.substring(0, separator);
        return Optional.ofNullable(bySkPrefix.get(prefix));
    }

    public List<FormDefinition<?>> all() {
        return bySlug.values().stream()
                .sorted(Comparator.comparingInt(FormDefinition::order))
                .toList();
    }
}
