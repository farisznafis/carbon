package com.kct.jcredit.core_backend.service.projectplan;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormBase;
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
 * @param perMethodology  whether the page repeats once per 方法論 (①–⑤).
 *                        Designed but not usable yet: the schema stores one
 *                        方法論 per project, so {@link ProjectPlanFormRegistry}
 *                        rejects any page that sets this. The key scheme below
 *                        is kept because the official form does require ①–⑤,
 *                        so this is a pending gap rather than a dead option.
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

    /**
     * Overlays the SQL-sourced fields PostgreSQL can actually supply, and
     * reports which those were so the client can render exactly those
     * read-only.
     *
     * <p>Fields SQL has no value for are deliberately left alone. Overwriting
     * them with {@code null} would erase what the operator typed on every
     * read, and reporting them read-only anyway would leave a required field
     * both empty and locked — a page in that state can never be saved.
     *
     * @return the JSON names that were filled from PostgreSQL
     */
    public List<String> applySql(
            ProjectPlanFormBase target, ProjectPlanSqlContext context, ObjectMapper mapper) {

        if (context == null || context.company() == null || sqlFields.isEmpty()) {
            // Nothing to overlay from; leave whatever was stored untouched.
            return List.of();
        }
        T typed = type.cast(target);

        // Run the binders against a throwaway instance to see what SQL has.
        // ProjectPlanFormBase is @JsonInclude(NON_NULL) and no DTO initialises
        // its fields, so a field absent from this tree is one SQL cannot fill.
        T probe = newInstance();
        sqlFields.forEach(field -> field.apply().accept(probe, context));
        ObjectNode supplied = mapper.valueToTree(probe);

        List<String> filled = new ArrayList<>(sqlFields.size());
        for (SqlField<T> field : sqlFields) {
            if (!supplied.hasNonNull(field.jsonName())) {
                continue;
            }
            field.apply().accept(typed, context);
            filled.add(field.jsonName());
        }
        return filled;
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
