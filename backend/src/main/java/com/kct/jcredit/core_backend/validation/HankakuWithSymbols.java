package com.kct.jcredit.core_backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 半角英数字 + 指定された記号を許可するバリデーション。
 * allowedSymbols に指定された文字も追加で許可される。
 * Regex (default): ^[A-Za-z0-9]+$
 */
@Documented
@Constraint(validatedBy = HankakuWithSymbolsValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface HankakuWithSymbols {
    String message() default "半角英数字で入力してください。";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String allowedSymbols() default "";
}
