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
    String message() default "Password must be 15-64 printable characters and must not be commonly used.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
