package com.kct.jcredit.core_backend.dto.projectplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

/**
 * Fields that hold an uploaded file's key rather than text the operator typed.
 *
 * <p>These carried {@code @JapaneseText}, whose alphanumeric pattern allows
 * {@code / - .} but not {@code _}. Every key is
 * {@code tmp/projectplan/{uuid}/{field}_{filename}}, so the separator alone
 * made those fields refuse every upload — the operator saw
 * 「日本語で入力してください。」 against a file they had just attached.
 */
class FileReferenceFieldValidationTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    /** A realistic key: a UUID directory, the field name, and a sanitised file name. */
    private static final String TEMP_KEY =
            "tmp/projectplan/6f1b6a2e-6a1e-4d0e-9f4a-2f6b7c8d9e01/"
                    + "projectConceptDiagram_battery______________-____.pdf";

    private static final String PERMANENT_KEY =
            "projects/PROJ1/plan/project-overview/projectConceptDiagram_diagram.png";

    private record FileField<T>(String name, Supplier<T> create, BiConsumer<T, String> set) {

        void assertAccepts(String key) {
            T target = create.get();
            set.accept(target, key);

            List<String> rejected = VALIDATOR.validate(target).stream()
                    .map(ConstraintViolation::getPropertyPath)
                    .map(Object::toString)
                    .filter(name::equals)
                    .toList();

            assertThat(rejected)
                    .as("%s should accept the key %s", name, key)
                    .isEmpty();
        }
    }

    private static List<FileField<?>> fileFields() {
        return List.of(
                new FileField<>("projectConceptDiagram",
                        ProjectOverviewRequest::new,
                        ProjectOverviewRequest::setProjectConceptDiagram),
                new FileField<>("structureDiagram",
                        ProgramTypeMethodologyRequest::new,
                        ProgramTypeMethodologyRequest::setStructureDiagram),
                new FileField<>("overviewDiagramBeforeAct",
                        ApplicableMethodologyRequest::new,
                        ApplicableMethodologyRequest::setOverviewDiagramBeforeAct),
                new FileField<>("overviewDiagramAfterAct",
                        ApplicableMethodologyRequest::new,
                        ApplicableMethodologyRequest::setOverviewDiagramAfterAct));
    }

    @Test
    void everyFileReferenceFieldAcceptsAProvisionalKey() {
        fileFields().forEach(field -> field.assertAccepts(TEMP_KEY));
    }

    @Test
    void andAcceptsThePermanentKeyItIsPromotedTo() {
        // The stored value is the promoted key, so a rule that only tolerated
        // the temp form would fail on the next read instead of on save.
        fileFields().forEach(field -> field.assertAccepts(PERMANENT_KEY));
    }
}
