package com.kct.jcredit.core_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class KatakanaValidator implements ConstraintValidator<Katakana, String> {

    // A Japanese name is written 姓 名, with a space between the two halves in
    // furigana as much as in kanji, and the SQL columns store it that way.
    // Without the space this rejected every such value — including the ones it
    // autofilled itself. \u30FC already falls inside \u30A0-\u30FF.
    private static final Pattern KATAKANA_PATTERN =
            Pattern.compile("^[\\u30A0-\\u30FF\\u3000\\s]+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        return KATAKANA_PATTERN.matcher(value).matches();
    }
}
