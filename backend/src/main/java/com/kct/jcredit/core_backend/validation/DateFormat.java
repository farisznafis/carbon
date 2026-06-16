package com.kct.jcredit.core_backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 日付形式のバリデーション。
 * デフォルトパターン: "yyyy-MM-dd"
 * サポート: "yyyy/MM/dd", "yyyy-MM-dd"
 */
@Documented
@Constraint(validatedBy = DateFormatValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface DateFormat {
    String message() default "日付の形式が正しくありません。";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /**
     * 日付のパターン。デフォルトは "yyyy-MM-dd"。
     */
    String pattern() default "yyyy-MM-dd";
}
