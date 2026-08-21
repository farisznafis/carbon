package com.kct.jcredit.core_backend.service.projectplan;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormBase;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormRow;
import com.kct.jcredit.core_backend.validation.RequiredIf;

/**
 * Everything the framework needs to serve one page of the project plan.
 *
 * <p>Adding a page is one entry in {@link ProjectPlanFormRegistry} plus its DTO;
 * no new controller, service or repository.
 *
 * <p>Pages whose form is a table hold a {@code List<X extends ProjectPlanFormRow>}.
 * Field names addressing such a row are written row-relative — {@code
 * years[].yearStartDate}, with no index, because the rule holds for every row.
 * Bean Validation reports the same field with a concrete index instead
 * ({@code years[3].yearStartDate}); both forms reach the client, which reads
 * either.
 *
 * @param slug            URL segment, e.g. {@code applicable-conditions}
 * @param skPrefix        DynamoDB sort key, e.g. {@code APPLICABLE_CONDITIONS}
 * @param displayName     Japanese label shown in the wizard
 * @param order           position in the wizard
 * @param perMethodology  whether the page repeats once per 方法論 (①–⑤).
 *                        <b>Always false, by decision, not by omission.</b> One
 *                        project applies one methodology on this platform, so
 *                        {@link ProjectPlanFormRegistry} refuses any page that
 *                        sets this. The flag and the key scheme below are kept
 *                        because the official form does provide ①–⑤ — a
 *                        project that uses one fills ① and leaves the rest
 *                        blank, which the form permits — and because the guard
 *                        needs something to guard.
 * @param type            the page's DTO
 * @param sqlFields       fields autofilled from PostgreSQL
 * @param derivation      values the page computes; {@code null} for most pages
 */
public record FormDefinition<T extends ProjectPlanFormBase>(
        String slug,
        String skPrefix,
        String displayName,
        int order,
        boolean perMethodology,
        Class<T> type,
        List<SqlField<T>> sqlFields,
        FormDerivation<T> derivation) {

    /** Separates a table's name from a field inside its rows: {@code years[].startDate}. */
    public static final String ROW_MARKER = "[]";

    /** A page that computes nothing — which is all but a handful of them. */
    public FormDefinition(
            String slug,
            String skPrefix,
            String displayName,
            int order,
            boolean perMethodology,
            Class<T> type,
            List<SqlField<T>> sqlFields) {
        this(slug, skPrefix, displayName, order, perMethodology, type, sqlFields, null);
    }

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
            if (!wasSupplied(supplied, field.jsonName())) {
                continue;
            }
            field.apply().accept(typed, context);
            filled.add(field.jsonName());
        }
        return filled;
    }

    /**
     * Computes this page's derived values, and names them so the client can
     * lock those inputs.
     *
     * <p>Runs after {@link #applySql} on read, and on save both before
     * validation and after upload promotion — so a client that posts its own
     * 合計 has it overwritten rather than trusted.
     *
     * @return the JSON names the derivation owns, empty when it has none
     */
    public List<String> applyDerivation(ProjectPlanFormBase target, ProjectPlanSqlContext context) {
        if (derivation == null) {
            return List.of();
        }
        derivation.derive(type.cast(target), context);
        return derivation.derivedFields();
    }

    /**
     * Whether the probe came back holding a value for this field.
     *
     * <p>A row-relative name is answered by the first row. A binder that seeds
     * a table fills the same column in every row it creates, so a table where
     * only some rows carry the column means the binder is inconsistent — not
     * that the column is read-only in some rows and editable in others.
     */
    private static boolean wasSupplied(ObjectNode supplied, String jsonName) {
        int marker = jsonName.indexOf(ROW_MARKER);
        if (marker < 0) {
            return supplied.hasNonNull(jsonName);
        }

        String table = jsonName.substring(0, marker);
        // Skip the marker and the "." separating it from the column.
        String column = jsonName.substring(marker + ROW_MARKER.length() + 1);
        JsonNode rows = supplied.get(table);
        if (rows == null || !rows.isArray() || rows.isEmpty()) {
            return false;
        }
        JsonNode firstRow = rows.get(0);
        return firstRow != null && firstRow.hasNonNull(column);
    }

    /**
     * The page's 有/無 rules, read straight off the DTO's {@link RequiredIf}
     * annotations so the frontend's show/hide logic cannot drift from the
     * server-side validation that enforces it.
     *
     * <p>Rules declared on a row type are included, named row-relative. The
     * server already enforces those per row — {@code @Valid} on the list runs
     * the class-level constraint on every element — so omitting them here
     * would leave the client marking a column required in rows where the
     * server does not ask for it.
     */
    public List<ConditionalField> conditionalFields() {
        List<ConditionalField> rules = new ArrayList<>(rulesOn(type, ""));
        for (Field table : rowTables(type)) {
            rules.addAll(rulesOn(rowTypeOf(table), jsonNameOf(table) + ROW_MARKER + "."));
        }
        return List.copyOf(rules);
    }

    private static List<ConditionalField> rulesOn(Class<?> declaring, String prefix) {
        if (declaring == null) {
            return List.of();
        }
        return List.of(declaring.getAnnotationsByType(RequiredIf.class)).stream()
                .map(rule -> new ConditionalField(
                        prefix + rule.field(), prefix + rule.when(), rule.is()))
                .toList();
    }

    /** Every {@code List<X extends ProjectPlanFormRow>} a page declares. */
    private static List<Field> rowTables(Class<?> pageType) {
        List<Field> tables = new ArrayList<>();
        Class<?> current = pageType;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (rowTypeOf(field) != null) {
                    tables.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return tables;
    }

    /** The row type a table field holds, or {@code null} when it is not a table. */
    private static Class<?> rowTypeOf(Field field) {
        if (!List.class.isAssignableFrom(field.getType())
                || !(field.getGenericType() instanceof ParameterizedType parameterized)) {
            return null;
        }
        Type[] arguments = parameterized.getActualTypeArguments();
        if (arguments.length != 1 || !(arguments[0] instanceof Class<?> element)) {
            return null;
        }
        return ProjectPlanFormRow.class.isAssignableFrom(element) ? element : null;
    }

    private static String jsonNameOf(Field field) {
        JsonProperty annotation = field.getAnnotation(JsonProperty.class);
        return annotation != null && !annotation.value().isBlank()
                ? annotation.value()
                : field.getName();
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
