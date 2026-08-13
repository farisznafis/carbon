package com.kct.jcredit.core_backend.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.kct.jcredit.core_backend.dto.projectplan.ApplicableConditionsRequest;
import com.kct.jcredit.core_backend.dto.projectplan.OtherOperatorProjectsRequest;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectApplicationRequest;
import com.kct.jcredit.core_backend.service.projectplan.FormDefinition;
import com.kct.jcredit.core_backend.service.projectplan.ProjectPlanFormRegistry;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanFormBase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/**
 * Guards the project-plan form framework's two load-bearing premises:
 * that the shared base class maps correctly through {@code TableSchema.fromBean},
 * and that the 有/無 conditional rules behave in both directions.
 */
class ProjectPlanFormBaseTest {

    private static final Validator VALIDATOR;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        VALIDATOR = factory.getValidator();
    }

    /**
     * Every page the registry serves. Taken from the registry rather than
     * listed here because ProjectPlanFormRepository builds a schema for each of
     * these at startup — an unmappable DTO stops the application booting, so
     * this must cover whatever is registered, not a snapshot of it.
     */
    static Stream<Class<? extends ProjectPlanFormBase>> registeredFormTypes() {
        return new ProjectPlanFormRegistry().all().stream().map(FormDefinition::type);
    }

    /**
     * The whole base-class design rests on {@code fromBean} picking up inherited
     * getters. If Lombok, {@code @DynamoDbBean} placement or the inheritance ever
     * regress, this fails at build time rather than at runtime against AWS.
     */
    @ParameterizedTest
    @MethodSource("registeredFormTypes")
    void everyFormBuildsAValidBeanSchema(Class<? extends ProjectPlanFormBase> type) {
        TableSchema<?> schema = TableSchema.fromBean(type);

        assertThat(schema.tableMetadata().primaryPartitionKey()).isEqualTo("PK");
        assertThat(schema.tableMetadata().primarySortKey()).contains("SK");
        assertThat(schema.attributeNames())
                .contains("status", "updatedAt", "updatedBy", "attachments");
    }

    @ParameterizedTest
    @MethodSource("registeredFormTypes")
    void keysAreSettableThroughTheBaseClass(Class<? extends ProjectPlanFormBase> type)
            throws ReflectiveOperationException {
        ProjectPlanFormBase form = type.getDeclaredConstructor().newInstance();

        form.setPk("PROJ1");
        form.setSk("SECTION");

        assertThat(form.getPk()).isEqualTo("PROJ1");
        assertThat(form.getSk()).isEqualTo("SECTION");
    }

    // --- 有/無 conditional rules (RequiredIf) ---

    @Test
    void emitterCodeIsNotRequiredWhenCompanyIsNotADesignatedEmitter() {
        ProjectApplicationRequest request = new ProjectApplicationRequest();
        request.setIsDesignatedEmitter(false);
        request.setEmitterCode(null);

        assertThat(fieldsWithErrors(request)).doesNotContain("emitterCode");
    }

    @Test
    void emitterCodeIsRequiredWhenCompanyIsADesignatedEmitter() {
        ProjectApplicationRequest request = new ProjectApplicationRequest();
        request.setIsDesignatedEmitter(true);
        request.setEmitterCode(null);

        // Keyed by the field itself, not by the class or a helper method name.
        assertThat(fieldsWithErrors(request)).contains("emitterCode");
    }

    @Test
    void energyActNumbersAreRequiredOnlyWhenSubjectToPeriodicReporting() {
        ProjectApplicationRequest subject = new ProjectApplicationRequest();
        subject.setHasEnergyActPeriodReport(true);

        assertThat(fieldsWithErrors(subject))
                .contains("energyActOperationNumber", "energyManageFactoryNumber");

        ProjectApplicationRequest notSubject = new ProjectApplicationRequest();
        notSubject.setHasEnergyActPeriodReport(false);

        assertThat(fieldsWithErrors(notSubject))
                .doesNotContain("energyActOperationNumber", "energyManageFactoryNumber");
    }

    // --- 地域区分 vocabulary ---

    @Test
    void regionalCategoryAcceptsKyushuOkinawaAsASingleValue() {
        ProjectApplicationRequest request = new ProjectApplicationRequest();
        request.setRegionalCategory("九州・沖縄");

        assertThat(fieldsWithErrors(request)).doesNotContain("regionalCategory");
    }

    @Test
    void regionalCategoryRejectsKyushuAndOkinawaSeparately() {
        for (String invalid : List.of("九州", "沖縄")) {
            ProjectApplicationRequest request = new ProjectApplicationRequest();
            request.setRegionalCategory(invalid);

            assertThat(fieldsWithErrors(request))
                    .as("regionalCategory=%s should be rejected", invalid)
                    .contains("regionalCategory");
        }
    }

    private static Set<String> fieldsWithErrors(Object target) {
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(target);
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
