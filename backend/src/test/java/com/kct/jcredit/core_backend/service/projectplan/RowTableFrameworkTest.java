package com.kct.jcredit.core_backend.service.projectplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormBase;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormRow;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.util.JsonFieldNameResolver;
import com.kct.jcredit.core_backend.validation.RequiredIf;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * The framework plumbing a page of repeating rows depends on, proved against a
 * throwaway page rather than a real one.
 *
 * <p>Most of the official form is tables — 4.排出削減計画 is one row per 年度,
 * 11.モニタリング計画 one per モニタリング項目 — and every one of them needs
 * storage, error keys, read-only marking and 有/無 rules to survive the trip
 * through a row index. Proving that here means the first real table page is
 * about the page, not about discovering which of these four is missing.
 */
class RowTableFrameworkTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private final FormDefinition<TableFormPage> definition = new FormDefinition<>(
            "throwaway-table",
            "THROWAWAY_TABLE",
            "使い捨ての表",
            999,
            false,
            TableFormPage.class,
            List.of(new SqlField<>(
                    "rows[].label",
                    (page, context) -> page.setRows(List.of(seededRow(context))))));

    private static TableRow seededRow(ProjectPlanSqlContext context) {
        TableRow row = new TableRow();
        row.setLabel(context.company().getNameJa());
        return row;
    }

    // --- storage ---

    @Test
    void rowsSurviveTheTripThroughDynamoDb() {
        TableSchema<TableFormPage> schema = TableSchema.fromBean(TableFormPage.class);

        TableFormPage page = new TableFormPage();
        page.setPk("project-1");
        page.setSk("THROWAWAY_TABLE");
        page.setRows(List.of(row("A", "first"), row("B", "second")));

        Map<String, AttributeValue> stored = schema.itemToMap(page, true);
        TableFormPage read = schema.mapToItem(stored);

        assertThat(read.getRows())
                .extracting(TableRow::getCode, TableRow::getLabel)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("A", "first"),
                        org.assertj.core.groups.Tuple.tuple("B", "second"));
    }

    @Test
    void rowsSurviveTheTripThroughJackson() throws Exception {
        TableFormPage page = new TableFormPage();
        page.setRows(List.of(row("A", "first"), row("B", "second")));

        TableFormPage read =
                mapper.treeToValue(mapper.valueToTree(page), TableFormPage.class);

        assertThat(read.getRows()).hasSize(2);
        assertThat(read.getRows().get(1).getLabel()).isEqualTo("second");
    }

    // --- error keys ---

    @Test
    void anEmptyRowIsReportedAgainstThatRowsField() {
        TableFormPage page = new TableFormPage();
        page.setRows(List.of(row("A", "first"), row(" ", "second")));

        assertThat(errorKeysOf(page)).containsExactly("rows[1].rowCode");
    }

    @Test
    void aRowFieldKeepsItsJsonNameThroughTheIndex() {
        // The row's Java field is `code` but the client sees `rowCode`.
        // Descending into List rather than the row type loses the rename, and
        // the client is handed a key for a field it does not have.
        assertThat(JsonFieldNameResolver.resolve(TableFormPage.class, "rows[3].code"))
                .isEqualTo("rows[3].rowCode");
    }

    @Test
    void aTableThatIsEntirelyMissingIsStillReportedAgainstTheTable() {
        assertThat(errorKeysOf(new TableFormPage())).containsExactly("rows");
    }

    // --- read-only marking ---

    @Test
    void aSeededColumnIsReportedReadOnlyRowRelative() {
        TableFormPage page = new TableFormPage();

        List<String> readOnly = definition.applySql(page, contextWithCompany("東京電力"), mapper);

        assertThat(readOnly).containsExactly("rows[].label");
        assertThat(page.getRows()).singleElement()
                .extracting(TableRow::getLabel)
                .isEqualTo("東京電力");
    }

    @Test
    void aColumnTheBinderCouldNotFillIsNotLocked() {
        // Locking a required column SQL left empty leaves the page unsaveable:
        // the server keeps asking for it and the operator cannot type it.
        TableFormPage page = new TableFormPage();

        assertThat(definition.applySql(page, contextWithCompany(null), mapper)).isEmpty();
    }

    // --- 有/無 rules ---

    @Test
    void rowLevelConditionalRulesReachTheClientRowRelative() {
        assertThat(definition.conditionalFields())
                .extracting(
                        FormDefinition.ConditionalField::field,
                        FormDefinition.ConditionalField::when,
                        FormDefinition.ConditionalField::is)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("note", "hasNote", true),
                        org.assertj.core.groups.Tuple.tuple(
                                "rows[].detail", "rows[].hasDetail", true));
    }

    @Test
    void aRowsConditionalFieldIsEnforcedPerRow() {
        TableFormPage page = new TableFormPage();
        TableRow needsDetail = row("A", "first");
        needsDetail.setHasDetail(true);
        page.setRows(List.of(row("B", "second"), needsDetail));

        assertThat(errorKeysOf(page)).containsExactly("rows[1].detail");
    }

    // --- helpers ---

    private List<String> errorKeysOf(TableFormPage page) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<TableFormPage> violation : validator.validate(page)) {
            errors.putIfAbsent(
                    JsonFieldNameResolver.resolve(
                            page.getClass(), violation.getPropertyPath().toString()),
                    violation.getMessage());
        }
        return List.copyOf(errors.keySet());
    }

    private static ProjectPlanSqlContext contextWithCompany(String companyName) {
        OperatorCompany company = new OperatorCompany();
        company.setNameJa(companyName);
        Project project = new Project();
        project.setOperatorCompany(company);
        return new ProjectPlanSqlContext(project, () -> null, () -> null);
    }

    private static TableRow row(String code, String label) {
        TableRow row = new TableRow();
        row.setCode(code);
        row.setLabel(label);
        return row;
    }

    // --- the throwaway page ---

    @Getter
    @Setter
    @DynamoDbBean
    @RequiredIf(field = "note", when = "hasNote")
    public static class TableFormPage extends ProjectPlanFormBase {

        private Boolean hasNote;
        private String note;

        @Valid
        @NotEmpty
        private List<TableRow> rows;
    }

    @Getter
    @Setter
    @DynamoDbBean
    @RequiredIf(field = "detail", when = "hasDetail")
    public static class TableRow implements ProjectPlanFormRow {

        /** Deliberately renamed, to prove the index does not lose the mapping. */
        @NotBlank
        @JsonProperty("rowCode")
        private String code;

        private String label;

        private Boolean hasDetail;
        private String detail;
    }
}
