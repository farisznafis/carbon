package com.kct.jcredit.core_backend.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Makes one field required only when a boolean toggle on the same object has a
 * given value — the 有/無 pattern of the official J-Credit forms
 * ("if 特定排出者である, then 特定排出者コード is required").
 *
 * <p>Declared at class level and repeatable, so a page lists all of its
 * conditional rules in one place:
 *
 * <pre>{@code
 * @RequiredIf(field = "emitterCode", when = "isDesignatedEmitter", is = true)
 * @RequiredIf(field = "energyActOperationNumber", when = "hasEnergyActPeriodReport", is = true)
 * public class SomeRequest { ... }
 * }</pre>
 *
 * <p>The violation is reported against {@link #field()} rather than the class,
 * so the {@code errors} map stays keyed by field name and matches the shape the
 * frontend already consumes.
 */
@Documented
@Repeatable(RequiredIf.List.class)
@Constraint(validatedBy = RequiredIfValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredIf {

    /** Property that becomes required. */
    String field();

    /** Boolean property that drives the condition. */
    String when();

    /** Value of {@link #when()} that makes {@link #field()} required. */
    boolean is() default true;

    String message() default "この項目は必須です。";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Documented
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        RequiredIf[] value();
    }
}
