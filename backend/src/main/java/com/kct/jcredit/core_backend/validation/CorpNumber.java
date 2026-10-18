package com.kct.jcredit.core_backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 法人番号（13桁の数字）のバリデーション。
 * Regex: ^\\d{13}$
 */
@Documented
@Constraint(validatedBy = CorpNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CorpNumber {
    String message() default "法人番号は13桁の数字で入力してください。";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
