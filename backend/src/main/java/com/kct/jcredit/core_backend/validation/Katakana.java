package com.kct.jcredit.core_backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 全角カタカナ（Full-width Katakana）と「ー」のみを許可するバリデーション。
 * Regex: ^[\u30A0-\u30FF\u30FC]+$
 */
@Documented
@Constraint(validatedBy = KatakanaValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Katakana {
    String message() default "全角カタカナで入力してください。";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
