package com.kct.jcredit.core_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class CorpNumberValidator implements ConstraintValidator<CorpNumber, String> {

    private static final Pattern CORP_NUMBER_PATTERN = Pattern.compile("^\\d{13}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        return CORP_NUMBER_PATTERN.matcher(value).matches();
    }
}
