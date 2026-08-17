package com.kct.jcredit.core_backend.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HankakuWithSymbolsValidatorTest {

    /** Builds the annotation by hand so each symbol set can be exercised directly. */
    private static HankakuWithSymbols withSymbols(String allowedSymbols) {
        return new HankakuWithSymbols() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return HankakuWithSymbols.class;
            }

            @Override
            public String allowedSymbols() {
                return allowedSymbols;
            }

            @Override
            public String message() {
                return "";
            }

            @Override
            public Class<?>[] groups() {
                return new Class<?>[0];
            }

            @Override
            public Class<? extends jakarta.validation.Payload>[] payload() {
                return newPayloadArray();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends jakarta.validation.Payload>[] newPayloadArray() {
        return new Class[0];
    }

    private static HankakuWithSymbolsValidator validatorFor(String allowedSymbols) {
        HankakuWithSymbolsValidator validator = new HankakuWithSymbolsValidator();
        validator.initialize(withSymbols(allowedSymbols));
        return validator;
    }

    /**
     * Regression: an unescaped '-' between two characters in the generated
     * character class is a range operator. "-." produced "[A-Za-z0-9 @-\.]",
     * whose "@-\." is a reverse range, so initialize() threw
     * PatternSyntaxException and every request validating such a field failed.
     */
    @ParameterizedTest
    @ValueSource(strings = { "-.", "-", "@._+-", " -.,'&", "&.,' -" })
    void initializesForEverySymbolSetUsedInTheCodebase(String allowedSymbols) {
        assertThatCode(() -> validatorFor(allowedSymbols)).doesNotThrowAnyException();
    }

    @Test
    void hyphenIsTreatedAsALiteralNotARange() {
        HankakuWithSymbolsValidator validator = validatorFor("-.");

        assertThat(validator.isValid("Ver.2.0", null)).isTrue();
        assertThat(validator.isValid("EN-R-002", null)).isTrue();
        // '/' sits inside the accidental '@'..'.' range but must not be accepted.
        assertThat(validator.isValid("a/b", null)).isFalse();
    }

    @Test
    void nullAndEmptyDeferToPresenceConstraints() {
        HankakuWithSymbolsValidator validator = validatorFor("-");

        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
    }
}
