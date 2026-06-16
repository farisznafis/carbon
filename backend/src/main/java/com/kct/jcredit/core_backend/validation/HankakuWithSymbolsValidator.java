package com.kct.jcredit.core_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class HankakuWithSymbolsValidator implements ConstraintValidator<HankakuWithSymbols, String> {

    private Pattern pattern;

    private static final String REGEX_META_CHARS = "\\^$.|?*+()[]{}";

    @Override
    public void initialize(HankakuWithSymbols constraintAnnotation) {
        String allowedSymbols = constraintAnnotation.allowedSymbols();
        StringBuilder escaped = new StringBuilder();
        for (char c : allowedSymbols.toCharArray()) {
            if (REGEX_META_CHARS.indexOf(c) >= 0) {
                escaped.append('\\').append(c);
            } else {
                escaped.append(c);
            }
        }
        pattern = Pattern.compile("^[A-Za-z0-9 @" + escaped + "]+$");
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        return pattern.matcher(value).matches();
    }
}
