package com.kct.jcredit.core_backend.service.projectplan.registry;

import java.util.List;

import com.kct.jcredit.core_backend.dto.request.base.ProjectPlanFormBase;
import com.kct.jcredit.core_backend.validation.RequiredIf;

/**
 * Everything the framework needs to serve one page of the project plan.
 *
 * <p>Adding a page is one entry in {@link ProjectPlanFormRegistry} plus its DTO;
 * no new controller, service or repository.
 *
 * @param slug            URL segment, e.g. {@code applicable-conditions}
 * @param skPrefix        DynamoDB sort key, e.g. {@code APPLICABLE_CONDITIONS}
 * @param displayName     Japanese label shown in the wizard
 * @param order           position in the wizard
 * @param perMethodology  whether the page repeats once per 方法論 (①–⑤)
 * @param type            the page's DTO
 * @param sqlFields       fields autofilled from PostgreSQL
 */
public record FormDefinition<T extends ProjectPlanFormBase>(
        String slug,
        String skPrefix,
        String displayName,
        int order,
        boolean perMethodology,
        Class<T> type,
        List<SqlField<T>> sqlFields) {

    /** Singleton pages use the bare prefix; repeated pages append the index. */
    public String sk(Integer methodologyIndex) {
        if (!perMethodology) {
            return skPrefix;
        }
        int index = methodologyIndex == null ? 1 : methodologyIndex;
        return skPrefix + "#M" + index;
    }

    /** Overlays every SQL-sourced field. PostgreSQL is authoritative. */
    public void applySql(ProjectPlanFormBase target, ProjectPlanSqlContext context) {
        if (context == null || context.company() == null) {
            // Nothing to overlay from; leave whatever was stored untouched.
            return;
        }
        T typed = type.cast(target);
        sqlFields.forEach(field -> field.apply().accept(typed, context));
    }

    /** Field names the client must render read-only. */
    public List<String> readOnlyFields() {
        return sqlFields.stream().map(SqlField::jsonName).toList();
    }

    /**
     * The page's 有/無 rules, read straight off the DTO's {@link RequiredIf}
     * annotations so the frontend's show/hide logic cannot drift from the
     * server-side validation that enforces it.
     */
    public List<ConditionalField> conditionalFields() {
        return List.of(type.getAnnotationsByType(RequiredIf.class)).stream()
                .map(rule -> new ConditionalField(rule.field(), rule.when(), rule.is()))
                .toList();
    }

    public T newInstance() {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Form DTO " + type.getName() + " needs a public no-arg constructor", e);
        }
    }

    /** A field that is only required — and only shown — when {@code when} equals {@code is}. */
    public record ConditionalField(String field, String when, boolean is) {
    }
}
