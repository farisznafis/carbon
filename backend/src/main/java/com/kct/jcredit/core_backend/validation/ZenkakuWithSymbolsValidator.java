package com.kct.jcredit.core_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class ZenkakuWithSymbolsValidator implements ConstraintValidator<ZenkakuWithSymbols, String> {

    // Full-width chars + allowed symbols: ．(FF0E) ，(FF0C) '(2019) －(FF0D) ・(30FB) 　(3000)
    private static final Pattern PATTERN = Pattern.compile(
            "^[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\u30FC\\u3000\\s\\uFF0E\\uFF0C\\u2019\\uFF0D\\u30FB]+$"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        return PATTERN.matcher(value).matches();
    }
}
