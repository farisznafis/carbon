package com.kct.jcredit.core_backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 日本語テキスト（漢字・ひらがな・カタカナ・全角記号）のバリデーション。
 * allowAlphanumeric=true の場合、半角英数字と基本的な句読点（.,&-'）も許可します。
 */
@Documented
@Constraint(validatedBy = JapaneseTextValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface JapaneseText {
    String message() default "日本語で入力してください。";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /**
     * true の場合、半角英数字（A-Za-z0-9）と基本句読点（.,&-'）も許可します。
     */
    boolean allowAlphanumeric() default false;
}
