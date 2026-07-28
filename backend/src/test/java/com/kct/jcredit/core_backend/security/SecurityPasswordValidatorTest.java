package com.kct.jcredit.core_backend.security;

import com.kct.jcredit.core_backend.validation.SecurityPasswordValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityPasswordValidatorTest {
    private final SecurityPasswordValidator validator = new SecurityPasswordValidator();

    @Test
    void acceptsLongPrintablePassphraseWithoutCompositionRule() {
        assertThat(validator.isValid("four calm words 2026", null)).isTrue();
    }

    @Test
    void rejectsShortBlockedAndControlCharacterPasswords() {
        assertThat(validator.isValid("TooShort123", null)).isFalse();
        assertThat(validator.isValid("password123456", null)).isFalse();
        assertThat(validator.isValid("valid length but\nnewline", null)).isFalse();
    }
}
