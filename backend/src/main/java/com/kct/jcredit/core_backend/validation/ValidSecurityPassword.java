package com.kct.jcredit.core_backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SecurityPasswordValidator.class)
public @interface ValidSecurityPassword {
    String message() default "パスワードは8〜64文字で、大文字・小文字・数字をそれぞれ1文字以上含めてください。よく使われるパスワードは使用できません。";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
