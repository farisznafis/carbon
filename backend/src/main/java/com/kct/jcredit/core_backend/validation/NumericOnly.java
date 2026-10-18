package com.kct.jcredit.core_backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 数字のみを許可するバリデーション。
 * オプションで +, -, . を許可可能。
 * Regex (default): ^[0-9]+$
 */
@Documented
@Constraint(validatedBy = NumericOnlyValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NumericOnly {
    String message() default "数字のみで入力してください。";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    boolean allowPlus() default false;
    boolean allowMinus() default false;
    boolean allowDot() default false;
}
