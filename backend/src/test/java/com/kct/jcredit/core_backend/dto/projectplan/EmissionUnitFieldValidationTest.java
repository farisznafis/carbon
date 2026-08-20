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
 * Units of measure.
 *
 * <p>J-Credit writes these as {@code t-CO2/年}. They were annotated
 * {@code @Hankaku}, whose pattern {@code ^[A-Za-z0-9 .,\-&']+$} rejects both
 * 年 and the slash — and five of these fields are backed by a dropdown offering
 * only {@code t-CO2/年} and {@code kg-CO2/年}, so every available choice failed
 * and the pages could not be saved at all.
 */
class EmissionUnitFieldValidationTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    /** Exactly what the unit dropdown offers. */
    private static final List<String> OFFERED_UNITS = List.of("t-CO2/年", "kg-CO2/年");

    private record UnitField<T>(String name, Supplier<T> create, BiConsumer<T, String> set) {

        void assertAccepts(String unit) {
            T target = create.get();
            set.accept(target, unit);

            List<String> rejected = VALIDATOR.validate(target).stream()
                    .map(ConstraintViolation::getPropertyPath)
                    .map(Object::toString)
                    .filter(name::equals)
                    .toList();

            assertThat(rejected).as("%s should accept %s", name, unit).isEmpty();
        }
    }

    private static List<UnitField<?>> unitFields() {
        return List.of(
                new UnitField<>("emissionReductionUnit",
                        EmissionReductionCalculationMethodRequest::new,
                        EmissionReductionCalculationMethodRequest::setEmissionReductionUnit),
                new UnitField<>("baselineEmissionUnit",
                        EmissionReductionCalculationMethodRequest::new,
                        EmissionReductionCalculationMethodRequest::setBaselineEmissionUnit),
                new UnitField<>("postProjectEmissionUnit",
                        EmissionReductionCalculationMethodRequest::new,
                        EmissionReductionCalculationMethodRequest::setPostProjectEmissionUnit),
                // 4.排出削減計画's two unit fields are gone from this list on
                // purpose. They moved onto EmissionReductionYearRow when the
                // page became a table, and the server now sets them from the
                // page's 単位 rather than accepting them — so there is no
                // character-set rule left to check. The page's own unitEmission
                // is deliberately narrower than this list (t-CO2 / kg-CO2 only),
                // and is covered by EmissionReductionPlanDerivationTest.
                new UnitField<>("unit",
                        BaselineEmissionRequest::new,
                        BaselineEmissionRequest::setUnit),
                new UnitField<>("unit",
                        MainActivityProjectEmissionRequest::new,
                        MainActivityProjectEmissionRequest::setUnit),
                new UnitField<>("unit",
                        MainBaselineEmissionRequest::new,
                        MainBaselineEmissionRequest::setUnit),
                new UnitField<>("unit",
                        IncidentalActivityProjectEmissionRequest::new,
                        IncidentalActivityProjectEmissionRequest::setUnit),
                new UnitField<>("unit",
                        IncidentalBaselineEmissionRequest::new,
                        IncidentalBaselineEmissionRequest::setUnit),
                new UnitField<>("unit",
                        MonitoringPlanRow::new,
                        MonitoringPlanRow::setUnit));
    }

    @Test
    void everyUnitFieldAcceptsEveryUnitTheDropdownOffers() {
        unitFields().forEach(field -> OFFERED_UNITS.forEach(field::assertAccepts));
    }

    @Test
    void aPlainAsciiUnitStillPasses() {
        // Nothing that used to be accepted stops being accepted.
        unitFields().forEach(field -> field.assertAccepts("t-CO2"));
    }
}
