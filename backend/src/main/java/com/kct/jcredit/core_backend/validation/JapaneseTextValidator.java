package com.kct.jcredit.core_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class JapaneseTextValidator implements ConstraintValidator<JapaneseText, String> {

    private static final Pattern JAPANESE_ONLY = Pattern.compile(
            "^[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\u3000-\\u303F\\u30FC\\u30FB\\uFF01-\\uFF5E\\s]+$"
    );

    private static final Pattern JAPANESE_WITH_ALPHANUM = Pattern.compile(
            "^[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\u3000-\\u303F\\u30FC\\u30FB\\uFF01-\\uFF5EA-Za-z0-9\\s\\-.,&'()\\/:]+$"
    );

    private boolean allowAlphanumeric;

    @Override
    public void initialize(JapaneseText annotation) {
        this.allowAlphanumeric = annotation.allowAlphanumeric();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        Pattern pattern = allowAlphanumeric ? JAPANESE_WITH_ALPHANUM : JAPANESE_ONLY;
        return pattern.matcher(value).matches();
    }
}
