package com.kct.jcredit.core_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.Set;

public class SecurityPasswordValidator implements ConstraintValidator<ValidSecurityPassword, String> {
    private static final Set<String> BLOCKED = Set.of(
            "passwordpassword", "password123456", "123456789012345",
            "qwertyuiopasdfgh", "gxcarbonpassword", "jcreditpassword");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.length() < 15 || value.length() > 64) {
            return false;
        }
        if (value.chars().anyMatch(character -> character < 0x20 || character > 0x7e)) {
            return false;
        }
        return !BLOCKED.contains(value.toLowerCase(Locale.ROOT));
    }
}
