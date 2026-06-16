package com.kct.jcredit.core_backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 全角文字 + 特定の全角記号（．，'－・　）を許可するバリデーション。
 * Allowed: Full-width chars + ．(FF0E) ，(FF0C) '(2019) －(FF0D) ・(30FB) 　(3000, full-width space)
 */
@Documented
@Constraint(validatedBy = ZenkakuWithSymbolsValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ZenkakuWithSymbols {
    String message() default "全角文字と許可された記号のみ入力してください。";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
