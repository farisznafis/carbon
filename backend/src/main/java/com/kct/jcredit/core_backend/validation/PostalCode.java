package com.kct.jcredit.core_backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 日本の郵便番号形式を検証するバリデーション。
 * Regex: ^[0-9]{3}-?[0-9]{4}$（ハイフンあり・なし両対応）
 */
@Documented
@Constraint(validatedBy = PostalCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PostalCode {
    String message() default "郵便番号の形式が正しくありません。（例: 1000001）";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
