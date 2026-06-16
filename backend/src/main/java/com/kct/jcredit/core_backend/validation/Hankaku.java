package com.kct.jcredit.core_backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 半角英数字（Half-width alphanumeric）のみを許可するバリデーション。
 * Regex: ^[A-Za-z0-9]+$
 */
@Documented
@Constraint(validatedBy = HankakuValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Hankaku {
    String message() default "半角英数字で入力してください。";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
