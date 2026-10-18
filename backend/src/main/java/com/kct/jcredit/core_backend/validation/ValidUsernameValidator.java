package com.kct.jcredit.core_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidUsernameValidator implements ConstraintValidator<ValidUsername, String> {

    private static final int ZERO_WIDTH_JOINER = 0x200D;
    private static final int VARIATION_SELECTOR_16 = 0xFE0F;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        return value.codePoints().allMatch(this::isAllowedCodePoint);
    }

    private boolean isAllowedCodePoint(int codePoint) {
        return isAsciiLetterOrDigit(codePoint)
                || codePoint == ' '
                || codePoint == '@'
                || codePoint == '_'
                || codePoint == ZERO_WIDTH_JOINER
                || codePoint == VARIATION_SELECTOR_16
                || Character.isEmoji(codePoint)
                || Character.isEmojiComponent(codePoint)
                || Character.isEmojiModifier(codePoint);
    }

    private boolean isAsciiLetterOrDigit(int codePoint) {
        return codePoint >= 'A' && codePoint <= 'Z'
                || codePoint >= 'a' && codePoint <= 'z'
                || codePoint >= '0' && codePoint <= '9';
    }
}
