package com.kct.jcredit.core_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class NumericOnlyValidator implements ConstraintValidator<NumericOnly, String> {

    private Pattern pattern;

    @Override
    public void initialize(NumericOnly constraintAnnotation) {
        StringBuilder allowedChars = new StringBuilder("0-9");
        if (constraintAnnotation.allowPlus()) {
            allowedChars.append("+");
        }
        if (constraintAnnotation.allowMinus()) {
            allowedChars.append("\\-");
        }
        if (constraintAnnotation.allowDot()) {
            allowedChars.append(".");
        }
        pattern = Pattern.compile("^[" + allowedChars + "]+$");
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        return pattern.matcher(value).matches();
    }
}
