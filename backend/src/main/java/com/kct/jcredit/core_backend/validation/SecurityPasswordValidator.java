package com.kct.jcredit.core_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.Set;

/**
 * The single password rule for the whole application: 8-64 printable ASCII characters containing
 * at least one lower case letter, one upper case letter and one digit, and not on the block list.
 *
 * <p>The upper bound stays under bcrypt's 72-byte limit, beyond which extra characters are silently
 * ignored and would give a false sense of strength.
 */
public class SecurityPasswordValidator implements ConstraintValidator<ValidSecurityPassword, String> {
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;

    // Short entries matter now that eight characters are allowed; the longer ones are kept for
    // passphrase-style choices.
    private static final Set<String> BLOCKED = Set.of(
            "password", "password1", "password123", "passw0rd", "p@ssw0rd",
            "qwerty123", "abcd1234", "admin123", "welcome1", "iloveyou1",
            "passwordpassword", "password123456", "123456789012345",
            "qwertyuiopasdfgh", "gxcarbonpassword", "jcreditpassword",
            "gxcarbon123", "jcredit123");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            return false;
        }
        if (value.chars().anyMatch(character -> character < 0x20 || character > 0x7e)) {
            return false;
        }
        if (value.chars().noneMatch(Character::isLowerCase)) {
            return false;
        }
        if (value.chars().noneMatch(Character::isUpperCase)) {
            return false;
        }
        if (value.chars().noneMatch(Character::isDigit)) {
            return false;
        }
        return !BLOCKED.contains(value.toLowerCase(Locale.ROOT));
    }
}
