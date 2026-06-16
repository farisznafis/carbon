package com.kct.jcredit.core_backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 全角文字（Full-width characters）のみを許可するバリデーション。
 * Regex: ^[^\x01-\x7E\uFF61-\uFF9F]+$
 * (Excludes ASCII and half-width katakana)
 */
@Documented
@Constraint(validatedBy = ZenkakuValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Zenkaku {
    String message() default "全角文字で入力してください。";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
