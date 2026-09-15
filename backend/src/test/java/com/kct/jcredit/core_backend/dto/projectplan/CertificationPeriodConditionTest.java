package com.kct.jcredit.core_backend.dto.projectplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

/**
 * 認証対象期間 is only asked for when the methodology has one.
 *
 * <p>Ticking 「認証対象期間が適用されない方法論を用いたプロジェクト」 hides the two
 * dates in the wizard. While they were unconditionally {@code @NotBlank} the
 * page then demanded values the operator could neither see nor enter, and the
 * only feedback was 入力内容に誤りがあります with nothing marked.
 */
class CertificationPeriodConditionTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    private static final String START = "certTargetPeriodStart";
    private static final String END = "certTargetPeriodEnd";

    private static List<String> violatedFields(Boolean periodDoesNotApply) {
        ProjectOverviewRequest page = new ProjectOverviewRequest();
        page.setIsMethodologyValid(periodDoesNotApply);

        return VALIDATOR.validate(page).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .filter(path -> START.equals(path) || END.equals(path))
                .toList();
    }

    @Test
    void theDatesAreNotAskedForWhenThePeriodDoesNotApply() {
        assertThat(violatedFields(true)).isEmpty();
    }

    @Test
    void theyAreStillRequiredWhenItDoes() {
        assertThat(violatedFields(false)).containsExactlyInAnyOrder(START, END);
    }

    @Test
    void anUnansweredToggleDoesNotDemandThemEither() {
        // isMethodologyValid is @NotNull, so an unanswered page already fails on
        // that. Reporting the dates too would send the operator after fields
        // that may turn out not to apply.
        assertThat(violatedFields(null)).isEmpty();
    }

    @Test
    void theRuleTravelsToTheClientSoItHidesTheSameFields() {
        List<String> conditional = List.of(
                        ProjectOverviewRequest.class.getAnnotationsByType(
                                com.kct.jcredit.core_backend.validation.RequiredIf.class))
                .stream()
                .map(com.kct.jcredit.core_backend.validation.RequiredIf::field)
                .toList();

        // conditionalFields is read straight off these annotations, so the
        // wizard's show/hide cannot drift from the server-side rule.
        assertThat(conditional).contains(START, END);
    }
}
