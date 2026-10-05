package com.kct.jcredit.core_backend.security;

import com.kct.jcredit.core_backend.validation.SecurityPasswordValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityPasswordValidatorTest {
    private final SecurityPasswordValidator validator = new SecurityPasswordValidator();

    @Test
    void acceptsEightCharacterPasswordWithEachRequiredCharacterClass() {
        assertThat(validator.isValid("Abcd123!", null)).isTrue();
    }

    @Test
    void acceptsPassphraseWithSymbolsTheOldPatternWouldHaveRejected() {
        // The rule this replaced allowed only @$!%*?&, so hyphens, spaces and dots were refused.
        assertThat(validator.isValid("Four calm words 2026", null)).isTrue();
        assertThat(validator.isValid("Rahasia-2026", null)).isTrue();
        assertThat(validator.isValid("my.Secret.9", null)).isTrue();
    }

    @Test
    void rejectsPasswordsShorterThanEightCharacters() {
        assertThat(validator.isValid("Ab1cde", null)).isFalse();
        assertThat(validator.isValid("Abc123", null)).isFalse();
    }

    @Test
    void rejectsPasswordsMissingACharacterClass() {
        assertThat(validator.isValid("alllowercase1", null)).isFalse();
        assertThat(validator.isValid("ALLUPPERCASE1", null)).isFalse();
        assertThat(validator.isValid("NoDigitsAtAll", null)).isFalse();
    }

    @Test
    void rejectsBlockedPasswordsRegardlessOfCase() {
        assertThat(validator.isValid("Password1", null)).isFalse();
        assertThat(validator.isValid("Passw0rd", null)).isFalse();
        assertThat(validator.isValid("password123456", null)).isFalse();
    }

    @Test
    void rejectsControlCharactersAndOverlongValues() {
        assertThat(validator.isValid("Valid length but\nnewline1", null)).isFalse();
        assertThat(validator.isValid("Ab1" + "x".repeat(62), null)).isFalse();
        assertThat(validator.isValid(null, null)).isFalse();
    }
}
