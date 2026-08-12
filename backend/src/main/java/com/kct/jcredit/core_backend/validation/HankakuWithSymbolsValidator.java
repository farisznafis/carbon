package com.kct.jcredit.core_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class HankakuWithSymbolsValidator implements ConstraintValidator<HankakuWithSymbols, String> {

    private Pattern pattern;

    /**
     * '-' is included because an unescaped hyphen between two characters in a
     * character class is a range operator. {@code allowedSymbols = "-."} used to
     * build {@code [A-Za-z0-9 @-\.]}, where {@code @-\.} is a reverse range
     * (0x40 down to 0x2E) and throws PatternSyntaxException on initialize.
     */
    private static final String REGEX_META_CHARS = "\\^$.|?*+()[]{}-";

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
