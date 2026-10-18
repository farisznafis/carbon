package com.kct.jcredit.core_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class ZenkakuValidator implements ConstraintValidator<Zenkaku, String> {

    private static final Pattern ZENKAKU_PATTERN = Pattern.compile("^[^\\x01-\\x7E\\uFF61-\\uFF9F]+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // null check is handled by @NotBlank
        }
        return ZENKAKU_PATTERN.matcher(value).matches();
    }
}
