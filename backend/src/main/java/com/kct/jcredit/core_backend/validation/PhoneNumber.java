package com.kct.jcredit.core_backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 電話番号形式のバリデーション。
 * Regex: ^\\+?[0-9\\-]{10,20}$
 */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumber {
    String message() default "電話番号の形式が正しくありません。（例: 0312345678）";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
