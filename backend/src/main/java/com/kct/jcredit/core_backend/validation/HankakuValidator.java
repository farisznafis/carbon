package com.kct.jcredit.core_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class HankakuValidator implements ConstraintValidator<Hankaku, String> {

    private static final Pattern HANKAKU_PATTERN = Pattern.compile("^[A-Za-z0-9 .,\\-&']+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        return HANKAKU_PATTERN.matcher(value).matches();
    }
}
