package com.kct.jcredit.core_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class KatakanaValidator implements ConstraintValidator<Katakana, String> {

    private static final Pattern KATAKANA_PATTERN = Pattern.compile("^[\\u30A0-\\u30FF\\u30FC]+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        return KATAKANA_PATTERN.matcher(value).matches();
    }
}
